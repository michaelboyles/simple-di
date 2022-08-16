package com.github.michaelboyles.simpledi;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.NONE)
class Const {
    /**
     * The name of the DI injector class which will be generated.
     */
    public static final String INJECTOR_CLASS_NAME = "SimpleDIContext";
    /**
     * The package of the DI injector class which will be generated.
     */
    public static final String INJECTOR_PACKAGE_NAME = "com.example";
}
