package com.github.michaelboyles.simpledi.test;

import javax.inject.Singleton;

@Singleton
public class DriversSeat implements Seat {
    @Override
    public String getPosition() {
        return "front right";
    }
}
