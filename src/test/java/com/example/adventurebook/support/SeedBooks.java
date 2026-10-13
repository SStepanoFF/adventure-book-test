package com.example.adventurebook.support;

import java.util.List;

/** The books shipped with the backend (backend/src/main/resources/books). Used by catalog tests only. */
public final class SeedBooks {

    public static final String PRISONER = "The Prisoner";
    public static final String SUNKEN_TEMPLE = "The Sunken Temple";
    public static final String WASTELAND_CONVOY = "The Wasteland Convoy";
    public static final String FROST_HOLLOW = "Frost Hollow";

    public static final List<String> ALL_TITLES = List.of(PRISONER, SUNKEN_TEMPLE, WASTELAND_CONVOY, FROST_HOLLOW);

    private SeedBooks() {
    }
}
