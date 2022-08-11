package com.github.michaelboyles.simpledi.test;

import javax.inject.Singleton;

@Singleton
public class Engine {
    private final Turbocharger turbocharger;

    public Engine(Turbocharger turbocharger) {
        this.turbocharger = turbocharger;
    }
}
