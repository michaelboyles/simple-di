package com.github.michaelboyles.simpledi;

import javax.inject.Provider;

public class MutableProvider<T> implements Provider<T> {
    private T value;

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
