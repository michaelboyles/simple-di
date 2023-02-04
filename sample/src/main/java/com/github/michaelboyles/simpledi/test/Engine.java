package com.github.michaelboyles.simpledi.test;

import javax.inject.Singleton;

@Singleton
public record Engine(Turbocharger turbocharger) {
}
