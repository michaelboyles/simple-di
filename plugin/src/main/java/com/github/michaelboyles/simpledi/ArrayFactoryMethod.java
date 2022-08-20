package com.github.michaelboyles.simpledi;

import com.squareup.javapoet.CodeBlock;
import lombok.AllArgsConstructor;

import javax.lang.model.type.TypeMirror;

/**
 * A factory method for an array.
 */
@AllArgsConstructor
class ArrayFactoryMethod implements CollectionFactoryMethod {
    private final TypeMirror arrayType;

    @Override
    public CodeBlock instantiateCollection(CodeBlock elements) {
        return CodeBlock.builder()
            .add("new $T[] {", arrayType)
            .add(elements)
            .add("}")
            .build();
    }
}
