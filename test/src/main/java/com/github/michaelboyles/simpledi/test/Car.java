package com.github.michaelboyles.simpledi.test;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class Car {
    private final Engine engine;
    private final Seat driversSeat;

    @Inject
    public Car(Engine engine, @Named("driver") Seat driversSeat) {
        this.engine = engine;
        this.driversSeat = driversSeat;
    }

    // Just an example of a constructor that's ignored due to @Inject on the other one
    public Car() {
        this(null, null);
    }
}
