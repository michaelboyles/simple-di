package com.github.michaelboyles.simpledi;

import com.squareup.javapoet.CodeBlock;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A factory method for Collections which provide a constructor that takes a single Collection parameter, e.g.
 * {@link ArrayList#ArrayList(java.util.Collection)}.
 */
@AllArgsConstructor
class CollectionConstructorFactoryMethod implements CollectionFactoryMethod {
    private Class<?> clazz;

    @Override
    public CodeBlock instantiateCollection(CodeBlock elements) {
        return CodeBlock.builder()
            .add("new $T<>($T.asList(", clazz, Arrays.class)
            .add(elements)
            .add("))")
            .build();
    }
}
