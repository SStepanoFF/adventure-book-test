package com.example.adventurebook.perf;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/** Timing helpers: a single measurement is noise, so budgets are asserted on the median of N runs. */
public final class PerformanceAssert {

    private PerformanceAssert() {
    }

    public static double measureMs(Runnable action) {
        long start = System.nanoTime();
        action.run();
        return (System.nanoTime() - start) / 1_000_000.0;
    }

    public static double median(int runs, Supplier<Double> measurementMs) {
        List<Double> samples = new ArrayList<>();
        for (int i = 0; i < runs; i++) {
            samples.add(measurementMs.get());
        }
        Collections.sort(samples);
        return samples.get(samples.size() / 2);
    }

    public static void assertWithin(double budgetMs, double actualMs, String what) {
        assertThat(actualMs)
                .as("%s: %.0f ms (budget %.0f ms)", what, actualMs, budgetMs)
                .isLessThanOrEqualTo(budgetMs);
    }
}
