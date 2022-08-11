package com.github.michaelboyles.simpledi;

import com.google.auto.service.AutoService;
import lombok.SneakyThrows;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
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
            .map(singleton -> new SdiSingleton((TypeElement) singleton, getConstructor(singleton)))
            .collect(Collectors.toList());
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
        Map<String, SdiSingleton> fqnToSingleton = singletons.stream()
            .collect(Collectors.toMap(SdiSingleton::getFqn, s -> s));
        for (SdiSingleton singleton : singletons) {
            getNumDependencies(fqnToNumDependents, fqnToSingleton, singleton);
        }
        return singletons.stream()
            .sorted(Comparator.comparing(singleton -> fqnToNumDependents.get(singleton.getFqn())))
            .collect(Collectors.toList());
    }

    private long getNumDependencies(Map<String, Long> fqnToNumDependents,
                                    Map<String, SdiSingleton> fqnToSingleton,
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
            SdiSingleton dependency = fqnToSingleton.get(paramType.toString());
            if (dependency == null) {
                throw new RuntimeException(singleton.getFqn() + " requires a bean of type " + paramType
                    + " which does not exist");
            }
            numDependencies += (1 + getNumDependencies(fqnToNumDependents, fqnToSingleton, dependency));
        }
        fqnToNumDependents.put(singleton.getFqn(), numDependencies);
        return numDependencies;
    }
}
