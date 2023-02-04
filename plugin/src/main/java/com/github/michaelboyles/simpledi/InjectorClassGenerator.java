package com.github.michaelboyles.simpledi;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.michaelboyles.simpledi.Const.INJECTOR_PACKAGE_NAME;
import static com.github.michaelboyles.simpledi.ProviderDependency.PROVIDER_IDENTIFIER_SUFFIX;

/**
 * Generates a class which performs dependency injection.
 */
@RequiredArgsConstructor
class InjectorClassGenerator {
    private static final String MAP_FIELD_NAME = "nameToBean";

    private final Map<Bean, String> beanToIdentifier = new HashMap<>();
    private final String className;
    private final List<Bean> sortedBeans;

    public JavaFile generateClass() {
        TypeSpec helloWorld = TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addField(getNameToBeanMapField())
            .addMethod(getConstructor())
            .addMethod(getBeanByNameMethod())
            .build();
        return JavaFile.builder(INJECTOR_PACKAGE_NAME, helloWorld).build();
    }

    private FieldSpec getNameToBeanMapField() {
        return FieldSpec.builder(ParameterizedTypeName.get(Map.class, String.class, Object.class), MAP_FIELD_NAME)
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .initializer("new $T<>()", HashMap.class)
            .build();
    }

    private MethodSpec getConstructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC);
        List<Bean> providedBeans = getProvidedBeans(sortedBeans);
        for (Bean bean : providedBeans) {
            addProviderInstantiation(builder, bean);
        }
        for (Bean bean : sortedBeans) {
            addBeanInstantiation(builder, bean, providedBeans.contains(bean));
        }
        for (Bean bean : sortedBeans) {
            addInjectMethodInvocations(builder, bean);
        }
        for (Bean bean : sortedBeans) {
            addBeanRegistration(builder, bean);
        }
        return builder.build();
    }

    private List<Bean> getProvidedBeans(List<Bean> beans) {
        return beans.stream()
            .map(Bean::getProvidedBeans)
            .flatMap(List::stream)
            .distinct()
            .toList();
    }

    private void addProviderInstantiation(MethodSpec.Builder methodBuilder, Bean bean) {
        methodBuilder.addStatement(
            "$T<$T> $L$L = new $T<>()", MutableProvider.class, bean.typeElement(), getIdentifier(bean),
            PROVIDER_IDENTIFIER_SUFFIX, MutableProvider.class
        );
    }

    private void addBeanInstantiation(MethodSpec.Builder methodBuilder, Bean bean, boolean isProvided) {
        methodBuilder.addStatement(
            "$T $L = new $T($L)",
            bean.typeElement(), getIdentifier(bean), bean.typeElement(),
            getArgumentList(bean.dependencies())
        );
        if (isProvided) {
            methodBuilder.addStatement(
                "$L$L.set($L)", getIdentifier(bean), PROVIDER_IDENTIFIER_SUFFIX, getIdentifier(bean)
            );
        }
    }

    private void addBeanRegistration(MethodSpec.Builder methodBuilder, Bean bean) {
        String id = getIdentifier(bean);
        methodBuilder.addStatement("$L.put($S, $L)", MAP_FIELD_NAME, id, id);
    }

    private MethodSpec getBeanByNameMethod() {
        return MethodSpec.methodBuilder("getBeanByName")
            .addModifiers(Modifier.PUBLIC)
            .returns(Object.class)
            .addParameter(String.class, "name")
            .addStatement("return $L.get($L)", MAP_FIELD_NAME, "name")
            .build();
    }

    private void addInjectMethodInvocations(MethodSpec.Builder methodBuilder, Bean bean) {
        for (InjectMethod method : bean.injectMethods()) {
            methodBuilder.addStatement(
                "$L.$L($L)",
                getIdentifier(bean),
                method.element().getSimpleName().toString(),
                getArgumentList(method.dependencies())
            );
        }
    }

    private CodeBlock getArgumentList(List<Dependency> dependencies) {
        CodeBlock.Builder builder = CodeBlock.builder();
        for (int i = 0; i < dependencies.size(); ++i) {
            builder.add(dependencies.get(i).getArgumentExpression(this::getIdentifier));
            if (i < (dependencies.size() - 1)) {
                builder.add(", ");
            }
        }
        return builder.build();
    }

    // The same class name might exist in different packages, so this guarantees uniqueness of the identifier used for
    // each bean
    private String getIdentifier(Bean bean) {
        return beanToIdentifier.computeIfAbsent(bean, k -> {
            String fqn = bean.getFqn();
            String shortName = fqn.substring(fqn.lastIndexOf('.') + 1);
            String camelCase = shortName.substring(0, 1).toLowerCase() + shortName.substring(1);
            String possibleName = camelCase;
            int attempt = 0;
            while (beanToIdentifier.containsValue(possibleName)) {
                possibleName = camelCase + (++attempt);
            }
            return possibleName;
        });
    }
}
