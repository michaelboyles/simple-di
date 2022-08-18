package com.github.michaelboyles.simpledi;

import lombok.Data;
import lombok.experimental.Accessors;
import org.checkerframework.checker.units.qual.A;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A bean that was found by annotation processing.
 */
@Data
@Accessors(fluent = true)
public class SdiBean {
    private final List<SdiDependency> dependencies = new ArrayList<>();
    private final List<InjectMethod> injectMethods = new ArrayList<>();
    private final String name;
    private final TypeElement typeElement;
    private final ExecutableElement constructor;

    public void addDependency(SdiDependency dependency) {
        dependencies.add(dependency);
    }

    public void addInjectMethod(InjectMethod injectMethod) {
        injectMethods.add(injectMethod);
    }

    public String getFqn() {
        return typeElement.getQualifiedName().toString();
    }

    public Collection<String> getAllFqns() {
        return Stream.concat(
                Stream.of(typeElement),
                Stream.concat(
                    getAllInterfaces().stream(),
                    getAllSuperclasses().stream()
                )
            )
            .map(type -> type.getQualifiedName().toString())
            .collect(Collectors.toList());
    }

    private Collection<TypeElement> getAllInterfaces() {
        return typeElement.getInterfaces().stream().map(this::getElement).collect(Collectors.toList());
    }

    private Collection<TypeElement> getAllSuperclasses() {
        List<TypeElement> superClasses = new ArrayList<>();
        TypeElement current = typeElement;
        while (current.getSuperclass().getKind() == TypeKind.DECLARED) {
            current = getElement(current.getSuperclass());
            superClasses.add(current);
        }
        return superClasses;
    }

    private TypeElement getElement(TypeMirror mirror) {
        return (TypeElement) ((DeclaredType) mirror).asElement();
    }

    public String getIdentifier() {
        String fqn = getFqn();
        String shortName = fqn.substring(fqn.lastIndexOf('.') + 1);
        return shortName.substring(0, 1).toLowerCase() + shortName.substring(1);
    }
}
