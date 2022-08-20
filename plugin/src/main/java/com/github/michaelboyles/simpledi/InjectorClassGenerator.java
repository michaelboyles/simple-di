package com.github.michaelboyles.simpledi;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.michaelboyles.simpledi.Const.INJECTOR_PACKAGE_NAME;
import static com.github.michaelboyles.simpledi.SdiProviderDependency.PROVIDER_IDENTIFIER_SUFFIX;

/**
 * Generates a class which performs dependency injection.
 */
public record InjectorClassGenerator(String className, List<SdiBean> sortedBeans) {
    private static final String MAP_FIELD_NAME = "nameToBean";

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
        List<SdiBean> providedBeans = getProvidedBeans(sortedBeans);
        for (SdiBean bean : providedBeans) {
            addProviderInstantiation(builder, bean);
        }
        for (SdiBean bean : sortedBeans) {
            addBeanInstantiation(builder, bean, providedBeans.contains(bean));
        }
        for (SdiBean bean : sortedBeans) {
            addInjectMethodInvocations(builder, bean);
        }
        for (SdiBean bean : sortedBeans) {
            addBeanRegistration(builder, bean);
        }
        return builder.build();
    }

    private List<SdiBean> getProvidedBeans(List<SdiBean> beans) {
        return beans.stream()
            .map(SdiBean::getProvidedBeans)
            .flatMap(List::stream)
            .distinct()
            .toList();
    }

    private void addProviderInstantiation(MethodSpec.Builder methodBuilder, SdiBean bean) {
        methodBuilder.addStatement(
            "$T<$T> $L$L = new $T()", MutableProvider.class, bean.typeElement(), bean.getIdentifier(),
            PROVIDER_IDENTIFIER_SUFFIX, MutableProvider.class
        );
    }

    private void addBeanInstantiation(MethodSpec.Builder methodBuilder, SdiBean bean, boolean isProvided) {
        methodBuilder.addStatement(
            "$T $L = new $T($L)",
            bean.typeElement(), bean.getIdentifier(), bean.typeElement(),
            getArgumentList(bean.dependencies())
        );
        if (isProvided) {
            methodBuilder.addStatement(
                "$L$L.set($L)", bean.getIdentifier(), PROVIDER_IDENTIFIER_SUFFIX, bean.getIdentifier()
            );
        }
    }

    private void addBeanRegistration(MethodSpec.Builder methodBuilder, SdiBean bean) {
        String id = bean.getIdentifier();
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

    private void addInjectMethodInvocations(MethodSpec.Builder methodBuilder, SdiBean bean) {
        for (InjectMethod method : bean.injectMethods()) {
            methodBuilder.addStatement(
                "$L.$L($L)",
                bean.getIdentifier(),
                method.element().getSimpleName().toString(),
                getArgumentList(method.dependencies())
            );
        }
    }

    private CodeBlock getArgumentList(List<SdiDependency> dependencies) {
        CodeBlock.Builder builder = CodeBlock.builder();
        for (int i = 0; i < dependencies.size(); ++i) {
            builder.add(dependencies.get(i).getArgumentExpression());
            if (i < (dependencies.size() - 1)) {
                builder.add(", ");
            }
        }
        return builder.build();
    }
}
