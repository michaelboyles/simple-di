package com.github.michaelboyles.simpledi;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * A singleton that was found by annotation processing.
 */
public record SdiSingleton(TypeElement typeElement, ExecutableElement constructor) {
    public String getFqn() {
        return typeElement.getQualifiedName().toString();
    }
}
