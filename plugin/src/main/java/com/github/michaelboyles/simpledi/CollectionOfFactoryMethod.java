package com.github.michaelboyles.simpledi;

import com.squareup.javapoet.CodeBlock;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * A factory method for the immutable collections added in Java 9: {@link List#of} etc.
 */
@AllArgsConstructor
class CollectionOfFactoryMethod implements CollectionFactoryMethod {
    private final Class<?> clazz;

    @Override
    public CodeBlock instantiateCollection(CodeBlock elements) {
        return CodeBlock.builder()
            .add("$T.$L(", clazz, "of")
            .add(elements)
            .add(")")
            .build();
    }
}
