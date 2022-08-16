package com.github.michaelboyles.simpledi;

import com.squareup.javapoet.CodeBlock;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * A standard dependency, directly upon another singleton.
 */
@AllArgsConstructor
class SdiBasicDependency implements SdiDependency {
    private final SdiSingleton singleton;

    @Override
    public List<SdiSingleton> getSingletons() {
        return List.of(singleton);
    }

    @Override
    public CodeBlock getArgumentExpression() {
        return CodeBlock.of("$L", singleton.getIdentifier());
    }
}
