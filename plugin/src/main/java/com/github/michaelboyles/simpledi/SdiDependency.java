package com.github.michaelboyles.simpledi;

import com.squareup.javapoet.CodeBlock;

import javax.annotation.CheckReturnValue;
import java.util.List;
import java.util.function.Function;

interface SdiDependency {
    /**
     * Get the beans which this dependency requires, either directly or indirectly.
     */
    @CheckReturnValue
    List<SdiBean> allBeans();

    /**
     * Get the beans which this dependency *directly* requires. May be > 1, e.g. if the dependency is a collection.
     */
    @CheckReturnValue
    List<SdiBean> directBeans();

    /**
     * Get an expression which can be used for auto-wiring this dependency, e.g. in a constructor.
     *
     * @param getIdentifier A function to get the variable identifier used for a given bean.
     */
    @CheckReturnValue
    CodeBlock getArgumentExpression(Function<SdiBean, String> getIdentifier);
}
