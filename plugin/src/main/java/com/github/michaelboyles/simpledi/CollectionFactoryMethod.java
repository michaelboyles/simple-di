package com.github.michaelboyles.simpledi;

import com.squareup.javapoet.CodeBlock;

/**
 * A static factory method for a Collection, which accepts 1 varargs parameter for the contents of the collection.
 */
interface CollectionFactoryMethod {
    CodeBlock instantiateCollection(CodeBlock elements);
}
