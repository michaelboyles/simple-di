package com.github.michaelboyles.simpledi;

import com.squareup.javapoet.CodeBlock;
import lombok.AllArgsConstructor;

/**
 * A static factory method for a Collection, which accepts 1 varargs parameter for the contents of the collection.
 */
@AllArgsConstructor
class CollectionFactoryMethod {
    private final Class<?> clazz;
    private final String methodName;

    CodeBlock instantiateCollection(CodeBlock elements) {
        return CodeBlock.builder()
            .add("$T.$L(", clazz, methodName)
            .add(elements)
            .add(")")
            .build();
    }
}
