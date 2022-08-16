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
        for (SdiBean bean : sortedBeans) {
            addBeanInstantiation(builder, bean);
        }
        for (SdiBean bean : sortedBeans) {
            addBeanRegistration(builder, bean);
        }
        return builder.build();
    }

    private void addBeanInstantiation(MethodSpec.Builder methodBuilder, SdiBean bean) {
        CodeBlock.Builder builder = CodeBlock.builder()
            .add("$T $L = new $T(", bean.typeElement(), bean.getIdentifier(), bean.typeElement());
        for (int i = 0; i < bean.dependencies().size(); ++i) {
            builder.add(bean.dependencies().get(i).getArgumentExpression());
            if (i < (bean.dependencies().size() - 1)) {
                builder.add(", ");
            }
        }
        methodBuilder.addStatement(builder.add(")").build());
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
}
