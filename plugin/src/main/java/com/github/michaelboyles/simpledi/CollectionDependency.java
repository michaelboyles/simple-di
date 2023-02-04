package com.github.michaelboyles.simpledi;

import com.squareup.javapoet.CodeBlock;

import java.util.List;
import java.util.function.Function;

/**
 * A dependency on a collection, e.g. List or Set. This will wire all beans of the corresponding type.
 */
class CollectionDependency implements Dependency {
    private final CollectionFactoryMethod factoryMethod;
    private final List<Bean> contents;

    public CollectionDependency(CollectionFactoryMethod factoryMethod, List<Bean> contents) {
        this.factoryMethod = factoryMethod;
        this.contents = contents;
    }

    @Override
    public List<Bean> allBeans() {
        return contents;
    }

    @Override
    public List<Bean> directBeans() {
        return contents;
    }

    @Override
    public CodeBlock getArgumentExpression(Function<Bean, String> getIdentifier) {
        CodeBlock.Builder arguments = CodeBlock.builder();
        for (int i = 0; i < contents.size(); ++i) {
            arguments.add(getIdentifier.apply(contents.get(i)));
            if (i < (contents.size() - 1)) {
                arguments.add(", ");
            }
        }
        return factoryMethod.instantiateCollection(arguments.build());
    }
}
