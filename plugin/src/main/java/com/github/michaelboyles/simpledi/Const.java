package com.github.michaelboyles.simpledi;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor(access = AccessLevel.NONE)
class Const {
    /**
     * The name of the DI injector class which will be generated.
     */
    static final String INJECTOR_CLASS_NAME = "SimpleDIContext";
    /**
     * The package of the DI injector class which will be generated.
     */
    static final String INJECTOR_PACKAGE_NAME = "com.example";
    /**
     * A map of Collection implementations to a factory method capable of create an instance of that Collection,
     * ordered from most specific to least specific.
     */
    static final Map<Class<?>, CollectionFactoryMethod> COLLECTION_TO_FACTORY_METHOD;

    static {
        COLLECTION_TO_FACTORY_METHOD = new LinkedHashMap<>();
        COLLECTION_TO_FACTORY_METHOD.put(List.class, new CollectionFactoryMethod(List.class, "of"));
        COLLECTION_TO_FACTORY_METHOD.put(Set.class, new CollectionFactoryMethod(Set.class, "of"));
        COLLECTION_TO_FACTORY_METHOD.put(Collection.class, new CollectionFactoryMethod(List.class, "of"));
    }
}
