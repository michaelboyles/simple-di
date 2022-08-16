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

/**
 * Generates a class which performs dependency injection.
 */
public record InjectorClassGenerator(String className, List<SdiSingleton> sortedSingletons) {
    public JavaFile generateClass() {
        TypeSpec helloWorld = TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addField(getNameToSingletonMapField())
            .addMethod(getMainMethod())
            .addMethod(getSingletonByNameMethod())
            .build();
        return JavaFile.builder("com.example", helloWorld).build();
    }

    private FieldSpec getNameToSingletonMapField() {
        return FieldSpec.builder(ParameterizedTypeName.get(Map.class, String.class, Object.class), "nameToSingleton")
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .initializer("new $T<>()", HashMap.class)
            .build();
    }

    private MethodSpec getMainMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("start")
            .addModifiers(Modifier.PUBLIC)
            .returns(void.class);
        for (SdiSingleton singleton : sortedSingletons) {
            addSingletonInstantiation(builder, singleton);
        }
        for (SdiSingleton singleton : sortedSingletons) {
            addSingletonRegistration(builder, singleton);
        }
        return builder.build();
    }

    private void addSingletonInstantiation(MethodSpec.Builder methodBuilder, SdiSingleton singleton) {
        CodeBlock.Builder builder = CodeBlock.builder()
            .add("$T $L = new $T(", singleton.typeElement(), singleton.getIdentifier(), singleton.typeElement());
        for (int i = 0; i < singleton.dependencies().size(); ++i) {
            builder.add(singleton.dependencies().get(i).getArgumentExpression());
            if (i < (singleton.dependencies().size() - 1)) {
                builder.add(", ");
            }
        }
        methodBuilder.addStatement(builder.add(")").build());
    }

    private void addSingletonRegistration(MethodSpec.Builder methodBuilder, SdiSingleton singleton) {
        String id = singleton.getIdentifier();
        methodBuilder.addStatement("nameToSingleton.put($S, $L)", id, id);
    }

    private MethodSpec getSingletonByNameMethod() {
        return MethodSpec.methodBuilder("getSingletonByName")
            .addModifiers(Modifier.PUBLIC)
            .returns(Object.class)
            .addParameter(String.class, "name")
            .addStatement("return $L.get($L)", "nameToSingleton", "name")
            .build();
    }
}
