package com.example.adventurebook.data;

import java.util.UUID;

/** Unique test data names: tests never depend on (or clash with) data created by other tests. */
public final class Unique {

    private Unique() {
    }

    public static String name(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String tag() {
        return "qa-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
