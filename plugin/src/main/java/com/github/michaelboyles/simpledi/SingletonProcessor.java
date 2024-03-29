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
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.tools.JavaFileObject;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.michaelboyles.simpledi.Const.COLLECTION_TO_FACTORY_METHOD;
import static com.github.michaelboyles.simpledi.Const.INJECTOR_CLASS_NAME;

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
        DiscoveredBeans discoveredBeans = findBeans(roundEnv);
        if (discoveredBeans.all().isEmpty()) return false;

        addDependenciesToBeans(discoveredBeans);
        for (Bean bean : discoveredBeans.all()) {
            addInjectMethods(discoveredBeans, bean);
        }
        List<Bean> sortedBeans = discoveredBeans.byNumDependencies();

        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(INJECTOR_CLASS_NAME);
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            InjectorClassGenerator generator = new InjectorClassGenerator(INJECTOR_CLASS_NAME, sortedBeans);
            generator.generateClass().writeTo(out);
        }
        return true;
    }

    private DiscoveredBeans findBeans(RoundEnvironment roundEnv) {
        return new DiscoveredBeans(
            roundEnv.getElementsAnnotatedWith(Singleton.class).stream()
                .filter(singleton -> singleton.asType().getKind() == TypeKind.DECLARED)
                .map(singleton -> new Bean(getName(singleton), (TypeElement) singleton, getConstructor(singleton)))
                .toList()
        );
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

    private void addDependenciesToBeans(DiscoveredBeans discoveredBeans) {
        for (Bean bean : discoveredBeans.all()) {
            for (VariableElement parameter : bean.constructor().getParameters()) {
                Dependency dependency = findDependenciesForParam(discoveredBeans, bean, parameter);
                bean.addDependency(dependency);
            }
        }
    }

    private Dependency findDependenciesForParam(DiscoveredBeans discoveredBeans, Bean bean, VariableElement parameter) {
        TypeMirror paramType = parameter.asType();
        if (paramType.getKind() == TypeKind.ARRAY) {
            TypeMirror arrayType = ((ArrayType) paramType).getComponentType();
            return new CollectionDependency(
                new ArrayFactoryMethod(arrayType),
                discoveredBeans.beansExtending(arrayType.toString())
            );
        }

        String paramTypeFqn = parameter.asType().toString();
        boolean isProvider = paramTypeFqn.startsWith(Provider.class.getName());
        if (isProvider) {
            return new ProviderDependency(
                getProviderContents(bean, parameter, discoveredBeans)
            );
        }
        List<Bean> candidates = discoveredBeans.beansExtending(paramTypeFqn);
        if (candidates.isEmpty()) {
            for (Map.Entry<Class<?>, CollectionFactoryMethod> entry : COLLECTION_TO_FACTORY_METHOD.entrySet()) {
                if (paramTypeFqn.startsWith(entry.getKey().getName())) {
                    List<Bean> contents = getCollectionContents(parameter, discoveredBeans);
                    return new CollectionDependency(entry.getValue(), contents);
                }
            }
            throw new RuntimeException(
                "%s requires a bean of type %s which does not exist".formatted(bean.getFqn(), paramTypeFqn)
            );
        }
        return new BasicDependency(
            tryToDisambiguateWithNamedAnnotation(candidates, parameter)
        );
    }

    private List<Bean> getCollectionContents(VariableElement collectionParameter, DiscoveredBeans discoveredBeans) {
        TypeMirror typeArgument = getSingleGenericTypeParam(collectionParameter);
        if (typeArgument.getKind() == TypeKind.DECLARED) {
            return discoveredBeans.beansExtending(typeArgument.toString());
        }
        if (typeArgument.getKind() == TypeKind.WILDCARD) {
            WildcardType wildcardType = (WildcardType) typeArgument;
            if (wildcardType.getSuperBound() != null) {
                // This is a bit counterintuitive. We can't provide every item that's a superclass, since that would
                // include every single bean, which would produce circular dependencies. Instead, a super wildcard
                // includes just beans with that exact FQN, and not child classes. This is the same way Spring handles
                // this problem.
                return discoveredBeans.beansWithExactFqn(wildcardType.getSuperBound().toString());
            }
            if (wildcardType.getExtendsBound() != null) {
                return discoveredBeans.beansExtending(wildcardType.getExtendsBound().toString());
            }
        }
        throw new RuntimeException(
            "Unsupported type %s in parameter '%s %s'".formatted(
                typeArgument.getKind(), collectionParameter.asType(), collectionParameter
            )
        );
    }

    private Bean tryToDisambiguateWithNamedAnnotation(List<Bean> candidates, VariableElement parameter) {
        candidates = new ArrayList<>(candidates);
        Named named = parameter.getAnnotation(Named.class);
        if (named != null) {
            candidates.removeIf(candidate -> !candidate.name().equals(named.value()));
        }
        if (candidates.size() > 1) {
            throw new RuntimeException(
                "Ambiguous dependency. Parameter '%s %s' has %d candidates: %s".formatted(
                    parameter.asType(), parameter, candidates.size(),
                    candidates.stream().map(Bean::getFqn).collect(Collectors.joining(", "))
                )
            );
        }
        return candidates.get(0);
    }

    private TypeMirror getSingleGenericTypeParam(VariableElement variable) {
        List<? extends TypeMirror> typeArguments = ((DeclaredType) variable.asType()).getTypeArguments();
        if (typeArguments == null || typeArguments.isEmpty()) {
            throw new RuntimeException(
                "Parameter '%s %s' uses raw type".formatted(variable.asType(), variable.getSimpleName())
            );
        }
        if (typeArguments.size() != 1) {
            throw new RuntimeException(
                "Parameter '%s %s' has unexpected number of type params: %s".formatted(
                    variable.asType(), variable.getSimpleName(), typeArguments.size()
                )
            );
        }
        return typeArguments.get(0);
    }

    private Bean getProviderContents(Bean sourceBean, VariableElement provider, DiscoveredBeans discoveredBeans) {
        List<Bean> providerContents = getCollectionContents(provider, discoveredBeans);
        if (providerContents.isEmpty()) {
            throw new RuntimeException(
                "%s requires a bean of type %s which does not exist".formatted(sourceBean.getFqn(), provider.asType())
            );
        }
        return tryToDisambiguateWithNamedAnnotation(providerContents, provider);
    }

    private void addInjectMethods(DiscoveredBeans discoveredBeans, Bean bean) {
        for (ExecutableElement method : getInjectAnnotatedMethods(bean)) {
            List<Dependency> dependencies = method.getParameters().stream()
                .map(param -> findDependenciesForParam(discoveredBeans, bean, param))
                .toList();
            bean.addInjectMethod(new InjectMethod(method, dependencies));
        }
    }

    private List<ExecutableElement> getInjectAnnotatedMethods(Bean bean) {
        return bean.typeElement().getEnclosedElements().stream()
            .filter(element -> element.getKind() == ElementKind.METHOD)
            .filter(element -> element.getAnnotation(Inject.class) != null)
            .map(ExecutableElement.class::cast)
            .toList();
    }
}
