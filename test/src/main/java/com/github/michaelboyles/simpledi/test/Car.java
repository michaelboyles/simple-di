package com.github.michaelboyles.simpledi.test;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Car {
    private final Engine engine;
    private final Seat seat;

    @Inject
    public Car(Engine engine, Seat seat) {
        this.engine = engine;
        this.seat = seat;
    }

    // Just an example of a constructor that's ignored due to @Inject on the other one
    public Car() {
        this(null, null);
    }
}
