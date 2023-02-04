package com.github.michaelboyles.simpledi.test;

import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("driver")
public class DriversSeat implements Seat {
    @Override
    public String getPosition() {
        return "front right";
    }
}
