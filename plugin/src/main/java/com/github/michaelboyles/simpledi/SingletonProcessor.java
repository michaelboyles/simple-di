package com.github.michaelboyles.simpledi;

import com.google.auto.service.AutoService;
import lombok.SneakyThrows;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

/**
 * An annotation processor which scans for classes annotated with {@link javax.inject.Singleton} and creates a
 * dependency injection context.
 */
@SupportedAnnotationTypes("javax.inject.Singleton")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class SingletonProcessor extends AbstractProcessor {
    private static final String INJECTOR_CLASS_NAME = "SimpleDIContext";

    @SneakyThrows
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        List<SdiSingleton> sortedSingletons = sortSingletonsByNumDependencies(
            getSingletons(roundEnv)
        );

        if (!sortedSingletons.isEmpty()) {
            JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(INJECTOR_CLASS_NAME);
            try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
                InjectorClassGenerator generator = new InjectorClassGenerator(INJECTOR_CLASS_NAME, sortedSingletons);
                generator.generateClass().writeTo(out);
            }
            return true;
        }
        return false;
    }

    private List<SdiSingleton> getSingletons(RoundEnvironment roundEnv) {
        return roundEnv.getElementsAnnotatedWith(Singleton.class).stream()
            .filter(singleton -> singleton.asType().getKind() == TypeKind.DECLARED)
            .map(singleton -> new SdiSingleton(getName(singleton), (TypeElement) singleton, getConstructor(singleton)))
            .collect(Collectors.toList());
    }

    private String getName(Element singleton) {
        Named named = singleton.getAnnotation(Named.class);
        if (named == null) return singleton.getSimpleName().toString();
        return named.value();
    }

    private ExecutableElement getConstructor(Element singleton) {
        List<ExecutableElement> possibleConstructors = new ArrayList<>();
        for (Element enclosedElement : singleton.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.CONSTRUCTOR) {
                possibleConstructors.add((ExecutableElement) enclosedElement);
            }
        }
        if (possibleConstructors.size() == 1) {
            return possibleConstructors.get(0);
        }
        List<ExecutableElement> annotatedConstructors = new ArrayList<>();
        for (ExecutableElement constructor : possibleConstructors) {
            Inject injectAnnotation = constructor.getAnnotation(Inject.class);
            if (injectAnnotation != null) {
                annotatedConstructors.add(constructor);
            }
        }
        if (annotatedConstructors.size() == 1) {
            return annotatedConstructors.get(0);
        }
        else if (annotatedConstructors.size() > 1) {
            throw new RuntimeException(
                "Only 1 constructor can be annotated with @Inject in " + singleton.getSimpleName()
                    + ", found " + annotatedConstructors.size()
            );
        }
        throw new RuntimeException("There are " + possibleConstructors.size() + " constructors in "
            + singleton.getSimpleName() + ". Either annotate one with @Inject, or only provide 1 constructor"
        );
    }

    private List<SdiSingleton> sortSingletonsByNumDependencies(List<SdiSingleton> singletons) {
        Map<String, Long> fqnToNumDependents = new HashMap<>();
        Map<String, List<SdiSingleton>> fqnToSingletons = new HashMap<>();
        for (SdiSingleton singleton : singletons) {
            for (String fqn : singleton.getAllFqns()) {
                fqnToSingletons.computeIfAbsent(fqn, k -> new ArrayList<>()).add(singleton);
            }
        }
        for (SdiSingleton singleton : singletons) {
            getNumDependencies(fqnToNumDependents, fqnToSingletons, singleton);
        }
        return singletons.stream()
            .sorted(Comparator.comparing(singleton -> fqnToNumDependents.get(singleton.getFqn())))
            .collect(Collectors.toList());
    }

    private long getNumDependencies(Map<String, Long> fqnToNumDependents,
                                    Map<String, List<SdiSingleton>> fqnToSingletons,
                                    SdiSingleton singleton) {
        final Long SENTINEL = -123L;

        Long prevNumDeps = fqnToNumDependents.get(singleton.getFqn());
        if (SENTINEL.equals(prevNumDeps)) throw new RuntimeException("Circular dependency!");
        if (prevNumDeps != null) return prevNumDeps;

        fqnToNumDependents.put(singleton.getFqn(), SENTINEL);
        long numDependencies = 0;
        for (VariableElement parameter : singleton.constructor().getParameters()) {
            TypeMirror paramType = parameter.asType();
            if (paramType.getKind() != TypeKind.DECLARED) {
                throw new RuntimeException("Unsupported type in constructor: " + paramType);
            }
            SdiDependency dependency = findDependenciesForParam(fqnToSingletons, singleton, parameter);
            singleton.addDependency(dependency);
            for (SdiSingleton dependentSingleton : dependency.getSingletons()) {
                numDependencies += (1 + getNumDependencies(fqnToNumDependents, fqnToSingletons, dependentSingleton));
            }
        }
        fqnToNumDependents.put(singleton.getFqn(), numDependencies);
        return numDependencies;
    }

    private SdiDependency findDependenciesForParam(Map<String, List<SdiSingleton>> fqnToSingletons,
                                                   SdiSingleton singleton, VariableElement parameter) {
        String paramTypeFqn = parameter.asType().toString();
        List<SdiSingleton> candidates = new ArrayList<>(fqnToSingletons.getOrDefault(paramTypeFqn, emptyList()));
        if (candidates.isEmpty()) {
            if (paramTypeFqn.startsWith(List.class.getName())) {
                DeclaredType contentsType = getCollectionContentsType(parameter);
                List<SdiSingleton> contents = fqnToSingletons.getOrDefault(contentsType.toString(), emptyList());
                return new SdiCollectionDependency(List.class, contents);
            }
            throw new RuntimeException(
                "%s requires a bean of type %s which does not exist".formatted(singleton.getFqn(), paramTypeFqn)
            );
        }
        Named named = parameter.getAnnotation(Named.class);
        if (named != null) {
            candidates.removeIf(candidate -> !candidate.name().equals(named.value()));
        }
        if (candidates.size() > 1) {
            throw new RuntimeException(
                "Ambiguous dependency. %s requires a bean of type %s and there are %d candidates: %s".formatted(
                    singleton.getFqn(), paramTypeFqn, candidates.size(),
                    candidates.stream().map(SdiSingleton::getFqn).collect(Collectors.joining(", "))
                )
            );
        }
        return new SdiBasicDependency(candidates.get(0));
    }

    private DeclaredType getCollectionContentsType(VariableElement collectionParameter) {
        List<? extends TypeMirror> typeArguments = ((DeclaredType) collectionParameter.asType()).getTypeArguments();
        if (typeArguments.size() != 1) {
            throw new RuntimeException("Collection has wrong number of type params: " + typeArguments.size());
        }
        return (DeclaredType) typeArguments.get(0);
    }
}
