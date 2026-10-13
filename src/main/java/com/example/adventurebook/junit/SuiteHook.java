package com.example.adventurebook.junit;

/**
 * Extension point (java.util.ServiceLoader) for once-per-run work that belongs to an application,
 * e.g. resetting its test data. Keeps the generic framework unaware of any concrete app.
 */
public interface SuiteHook {

    default void beforeSuite() {
    }

    default void afterSuite() {
    }
}
