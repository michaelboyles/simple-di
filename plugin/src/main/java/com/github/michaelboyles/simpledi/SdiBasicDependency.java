package com.github.michaelboyles.simpledi;

import com.squareup.javapoet.CodeBlock;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * A standard dependency, directly upon another bean.
 */
@AllArgsConstructor
class SdiBasicDependency implements SdiDependency {
    private final SdiBean bean;

    @Override
    public List<SdiBean> getBeans() {
        return List.of(bean);
    }

    @Override
    public CodeBlock getArgumentExpression() {
        return CodeBlock.of("$L", bean.getIdentifier());
    }
}
