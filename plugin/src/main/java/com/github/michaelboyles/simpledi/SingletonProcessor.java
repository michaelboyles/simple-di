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

import static com.github.michaelboyles.simpledi.Const.COLLECTION_TO_FACTORY_METHOD;
import static com.github.michaelboyles.simpledi.Const.INJECTOR_CLASS_NAME;
import static java.util.Collections.emptyList;

/**
 * An annotation processor which scans for classes annotated with {@link javax.inject.Singleton} and creates a
 * dependency injection context.
 */
@SupportedAnnotationTypes("javax.inject.Singleton")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class SingletonProcessor extends AbstractProcessor {
    @SneakyThrows
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        List<SdiBean> sortedBeans = sortBeansByNumDependencies(getBeans(roundEnv));

        if (!sortedBeans.isEmpty()) {
            JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(INJECTOR_CLASS_NAME);
            try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
                InjectorClassGenerator generator = new InjectorClassGenerator(INJECTOR_CLASS_NAME, sortedBeans);
                generator.generateClass().writeTo(out);
            }
            return true;
        }
        return false;
    }

    private List<SdiBean> getBeans(RoundEnvironment roundEnv) {
        return roundEnv.getElementsAnnotatedWith(Singleton.class).stream()
            .filter(singleton -> singleton.asType().getKind() == TypeKind.DECLARED)
            .map(singleton -> new SdiBean(getName(singleton), (TypeElement) singleton, getConstructor(singleton)))
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

    private List<SdiBean> sortBeansByNumDependencies(List<SdiBean> beans) {
        Map<String, Long> fqnToNumDependents = new HashMap<>();
        Map<String, List<SdiBean>> fqnToBeans = new HashMap<>();
        for (SdiBean bean : beans) {
            for (String fqn : bean.getAllFqns()) {
                fqnToBeans.computeIfAbsent(fqn, k -> new ArrayList<>()).add(bean);
            }
        }
        for (SdiBean bean : beans) {
            getNumDependencies(fqnToNumDependents, fqnToBeans, bean);
        }
        for (SdiBean bean : beans) {
            addInjectMethods(fqnToBeans, bean);
        }
        return beans.stream()
            .sorted(Comparator.comparing(bean -> fqnToNumDependents.get(bean.getFqn())))
            .collect(Collectors.toList());
    }

    private long getNumDependencies(Map<String, Long> fqnToNumDependents,
                                    Map<String, List<SdiBean>> fqnToBeans,
                                    SdiBean bean) {
        final Long SENTINEL = -123L;

        Long prevNumDeps = fqnToNumDependents.get(bean.getFqn());
        if (SENTINEL.equals(prevNumDeps)) throw new RuntimeException("Circular dependency!");
        if (prevNumDeps != null) return prevNumDeps;

        fqnToNumDependents.put(bean.getFqn(), SENTINEL);
        long numDependencies = 0;
        for (VariableElement parameter : bean.constructor().getParameters()) {
            TypeMirror paramType = parameter.asType();
            if (paramType.getKind() != TypeKind.DECLARED) {
                throw new RuntimeException("Unsupported type in constructor: " + paramType);
            }
            SdiDependency dependency = findDependenciesForParam(fqnToBeans, bean, parameter);
            bean.addDependency(dependency);
            for (SdiBean dependentBean : dependency.getBeans()) {
                numDependencies += (1 + getNumDependencies(fqnToNumDependents, fqnToBeans, dependentBean));
            }
        }
        fqnToNumDependents.put(bean.getFqn(), numDependencies);
        return numDependencies;
    }

    private SdiDependency findDependenciesForParam(Map<String, List<SdiBean>> fqnToBeans, SdiBean bean,
                                                   VariableElement parameter) {
        String paramTypeFqn = parameter.asType().toString();
        List<SdiBean> candidates = new ArrayList<>(fqnToBeans.getOrDefault(paramTypeFqn, emptyList()));
        if (candidates.isEmpty()) {
            for (Map.Entry<Class<?>, CollectionFactoryMethod> entry : COLLECTION_TO_FACTORY_METHOD.entrySet()) {
                if (paramTypeFqn.startsWith(entry.getKey().getName())) {
                    DeclaredType contentsType = getCollectionContentsType(parameter);
                    List<SdiBean> contents = fqnToBeans.getOrDefault(contentsType.toString(), emptyList());
                    return new SdiCollectionDependency(entry.getValue(), contents);
                }
            }
            throw new RuntimeException(
                "%s requires a bean of type %s which does not exist".formatted(bean.getFqn(), paramTypeFqn)
            );
        }
        Named named = parameter.getAnnotation(Named.class);
        if (named != null) {
            candidates.removeIf(candidate -> !candidate.name().equals(named.value()));
        }
        if (candidates.size() > 1) {
            throw new RuntimeException(
                "Ambiguous dependency. %s requires a bean of type %s and there are %d candidates: %s".formatted(
                    bean.getFqn(), paramTypeFqn, candidates.size(),
                    candidates.stream().map(SdiBean::getFqn).collect(Collectors.joining(", "))
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

    private void addInjectMethods(Map<String, List<SdiBean>> fqnToBeans, SdiBean bean) {
        for (ExecutableElement method : getInjectAnnotatedMethods(bean)) {
            List<SdiDependency> dependencies = method.getParameters().stream()
                .map(param -> findDependenciesForParam(fqnToBeans, bean, param))
                .toList();
            bean.addInjectMethod(new InjectMethod(method, dependencies));
        }
    }

    private List<ExecutableElement> getInjectAnnotatedMethods(SdiBean bean) {
        return bean.typeElement().getEnclosedElements().stream()
            .filter(element -> element.getKind() == ElementKind.METHOD)
            .filter(element -> element.getAnnotation(Inject.class) != null)
            .map(ExecutableElement.class::cast)
            .toList();
    }
}
