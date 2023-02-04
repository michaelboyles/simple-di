package com.github.michaelboyles.simpledi;

import javax.lang.model.element.ExecutableElement;
import java.util.List;

/**
 * A method found via annotation processing which was annotated with {@link javax.inject.Inject}.
 */
record InjectMethod(ExecutableElement element, List<Dependency> dependencies) {
}
