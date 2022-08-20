package com.github.michaelboyles.simpledi;

import com.squareup.javapoet.CodeBlock;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.function.Function;

/**
 * A standard dependency, directly upon another bean.
 */
@AllArgsConstructor
class SdiBasicDependency implements SdiDependency {
    private final SdiBean bean;

    @Override
    public List<SdiBean> allBeans() {
        return List.of(bean);
    }

    @Override
    public List<SdiBean> directBeans() {
        return allBeans();
    }

    @Override
    public CodeBlock getArgumentExpression(Function<SdiBean, String> getIdentifier) {
        return CodeBlock.of("$L", getIdentifier.apply(bean));
    }
}
