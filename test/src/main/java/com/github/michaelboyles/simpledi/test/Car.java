package com.github.michaelboyles.simpledi.test;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;

@Singleton
public class Car {
    private final Engine engine;
    private final Seat driversSeat;
    private final List<? extends Seat> seats;

    @Inject
    public Car(Engine engine, @Named("driver") Seat driversSeat, List<? extends Seat> seats, Provider<Car> self) {
        this.engine = engine;
        this.driversSeat = driversSeat;
        this.seats = seats;
    }

    // Just an example of a constructor that's ignored due to @Inject on the other one
    public Car() {
        this(null, null, emptyList(), null);
    }

    @Inject
    public void addDriver(Driver driver) {
        System.out.println("Added driver "  + driver);
    }

    @Inject
    public void addSeats(Seat[] seats) {
        System.out.println("Added seats " + Arrays.toString(seats));
    }
}
