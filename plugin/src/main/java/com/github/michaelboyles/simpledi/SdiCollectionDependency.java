package com.github.michaelboyles.simpledi;

import com.squareup.javapoet.CodeBlock;

import java.util.Collection;
import java.util.List;

/**
 * A dependency on a collection, e.g. List or Set. This will wire all beans of the corresponding type.
 */
public class SdiCollectionDependency implements SdiDependency {
    private final Class<? extends Collection<?>> concreteCollectionType;
    private final List<SdiSingleton> contents;

    public <T extends Collection<?>, U extends T> SdiCollectionDependency(Class<U> concreteCollectionType,
                                                                          List<SdiSingleton> contents) {
        this.concreteCollectionType = concreteCollectionType;
        this.contents = contents;
    }

    @Override
    public List<SdiSingleton> getSingletons() {
        return contents;
    }

    @Override
    public CodeBlock getArgumentExpression() {
        CodeBlock.Builder builder = CodeBlock.builder().add("$T.of(", concreteCollectionType);
        for (int i = 0; i < contents.size(); ++i) {
            builder.add(contents.get(i).getIdentifier());
            if (i < (contents.size() - 1)) {
                builder.add(", ");
            }
        }
        return builder.add(")").build();
    }
}
