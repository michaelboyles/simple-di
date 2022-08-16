package com.github.michaelboyles.simpledi;

import com.squareup.javapoet.CodeBlock;

import javax.annotation.CheckReturnValue;
import java.util.List;

interface SdiDependency {
    /**
     * Get the beans which this dependency requires. May be > 1, e.g. if the dependency is a collection.
     */
    @CheckReturnValue
    List<SdiBean> getBeans();

    /**
     * Get an expression which can be used for auto-wiring this dependency, e.g. in a constructor.
     */
    @CheckReturnValue
    CodeBlock getArgumentExpression();
}
