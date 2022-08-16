package com.github.michaelboyles.simpledi.test;

import javax.inject.Singleton;

@Singleton
public class PassengerSeat implements Seat {
    @Override
    public String getPosition() {
        return "front left";
    }
}
