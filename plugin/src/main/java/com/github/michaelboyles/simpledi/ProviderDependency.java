package com.github.michaelboyles.simpledi;

import com.squareup.javapoet.CodeBlock;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;

/**
 * A dependency via the indirection of a {@link javax.inject.Provider}.
 */
@AllArgsConstructor
class ProviderDependency implements Dependency {
    static final String PROVIDER_IDENTIFIER_SUFFIX = "Provider";

    private final Bean bean;

    @Override
    public List<Bean> allBeans() {
        return List.of(bean);
    }

    @Override
    public List<Bean> directBeans() {
        return emptyList();
    }

    @Override
    public CodeBlock getArgumentExpression(Function<Bean, String> getIdentifier) {
        return CodeBlock.of("$L", getIdentifier.apply(bean) + PROVIDER_IDENTIFIER_SUFFIX);
    }
}
