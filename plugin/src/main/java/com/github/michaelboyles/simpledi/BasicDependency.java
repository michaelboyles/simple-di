package com.github.michaelboyles.simpledi;

import com.squareup.javapoet.CodeBlock;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.function.Function;

/**
 * The most common type of dependency, directly upon another bean.
 */
@AllArgsConstructor
class BasicDependency implements Dependency {
    private final Bean bean;

    @Override
    public List<Bean> allBeans() {
        return List.of(bean);
    }

    @Override
    public List<Bean> directBeans() {
        return allBeans();
    }

    @Override
    public CodeBlock getArgumentExpression(Function<Bean, String> getIdentifier) {
        return CodeBlock.of("$L", getIdentifier.apply(bean));
    }
}
