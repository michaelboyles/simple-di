package com.github.michaelboyles.simpledi;

import javax.inject.Provider;

/**
 * A simple holder for a value which can be provided in place of a direct dependency. The indirection
 * allows for the avoidance of circular dependencies. This class is only expected to be used by the
 * generated DI context.
 *
 * @param <T> The type of the mutable object reference.
 */
public class MutableProvider<T> implements Provider<T> {
    private T value;

    @SuppressWarnings("unused") // Called by generated code only
    public void set(T value) {
        if (value == null) throw new NullPointerException();
        if (this.value != null) throw new RuntimeException("Already set to " + value);
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }
}
