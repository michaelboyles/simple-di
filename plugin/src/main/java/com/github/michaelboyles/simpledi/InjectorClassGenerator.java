package com.github.michaelboyles.simpledi;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        String args = singleton.dependencies().stream()
            .map(this::getIdentifier)
            .collect(Collectors.joining(", "));
        methodBuilder.addStatement(
            "$T $L = new $T($L)", singleton.typeElement(), getIdentifier(singleton),
            singleton.typeElement(), args
        );
    }

    private void addSingletonRegistration(MethodSpec.Builder methodBuilder, SdiSingleton singleton) {
        String id = getIdentifier(singleton);
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

    private String getIdentifier(SdiSingleton singleton) {
        return getIdentifier(singleton.getFqn());
    }

    private String getIdentifier(String fqn) {
        String shortName = fqn.substring(fqn.lastIndexOf('.') + 1);
        return shortName.substring(0, 1).toLowerCase() + shortName.substring(1);
    }
}
