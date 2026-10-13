package com.example.adventurebook.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Records which operations (REST verb + path template) the run actually called - {@link ApiClient} does it for
 * every call, thread-safe - and compares that with the operations of the OpenAPI document.
 *
 * <p>"Covered" follows the objective: every operation needs at least one happy-path call (2xx) and at least one
 * call that went through the contract-validating client. Nothing has to be declared anywhere: the verb is known
 * at call time, so a new endpoint in the spec simply shows up as "never called".
 */
public final class ApiCoverage {

    private static final Map<String, Stats> STATS = new ConcurrentHashMap<>();

    private ApiCoverage() {
    }

    /** Identity of an operation; path parameter names are ignored, so {id} and {bookId} are the same. */
    public static String key(String method, String path) {
        return method.toUpperCase() + " " + path.replaceAll("\\{[^}/]+}", "{}");
    }

    static void record(String method, String path, int status, boolean contractValidated) {
        Stats stats = STATS.computeIfAbsent(key(method, path), k -> new Stats());
        stats.calls.incrementAndGet();
        if (status >= 200 && status < 300) {
            stats.happy.incrementAndGet();
        }
        if (contractValidated) {
            stats.contract.incrementAndGet();
        }
    }

    public static Report evaluate(Set<String> specOperations) {
        List<String> neverCalled = new ArrayList<>();
        List<String> noHappyPath = new ArrayList<>();
        List<String> noContract = new ArrayList<>();
        Map<String, Stats> rows = new TreeMap<>();
        for (String key : new TreeSet<>(specOperations)) {
            Stats stats = STATS.getOrDefault(key, new Stats());
            rows.put(key, stats);
            if (stats.calls.get() == 0) {
                neverCalled.add(key);
            } else if (stats.happy.get() == 0) {
                noHappyPath.add(key);
            }
            if (stats.contract.get() == 0) {
                noContract.add(key);
            }
        }
        List<String> notInSpec = new ArrayList<>(new TreeSet<>(STATS.keySet()));
        notInSpec.removeAll(specOperations);
        return new Report(notInSpec, neverCalled, noHappyPath, noContract, markdown(rows));
    }

    private static String markdown(Map<String, Stats> rows) {
        StringBuilder sb = new StringBuilder("| Operation | calls | 2xx (happy path) | contract-validated |\n|---|---|---|---|\n");
        rows.forEach((key, s) -> sb.append("| ").append(key).append(" | ").append(s.calls.get()).append(" | ")
                .append(s.happy.get()).append(" | ").append(s.contract.get()).append(" |\n"));
        return sb.toString();
    }

    private static final class Stats {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicInteger happy = new AtomicInteger();
        final AtomicInteger contract = new AtomicInteger();
    }

    /**
     * @param notInSpec   called during the run, but not described by the OpenAPI document
     * @param neverCalled in the document, never called during this run
     * @param noHappyPath called, but never with a 2xx response
     * @param noContract  never called through the contract-validating client
     */
    public record Report(List<String> notInSpec, List<String> neverCalled, List<String> noHappyPath,
                         List<String> noContract, String markdown) {

        /** Gaps that only make sense to judge after a FULL run. */
        public List<String> runGaps() {
            List<String> gaps = new ArrayList<>();
            notInSpec.forEach(k -> gaps.add("called but not in the spec: " + k));
            neverCalled.forEach(k -> gaps.add("never called: " + k));
            noHappyPath.forEach(k -> gaps.add("no happy path (2xx): " + k));
            noContract.forEach(k -> gaps.add("no contract check: " + k));
            return gaps;
        }
    }
}
