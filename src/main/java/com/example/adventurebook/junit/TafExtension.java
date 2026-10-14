package com.example.adventurebook.junit;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.slf4j.MDC;

/**
 * Cross-cutting behaviour for every test, in one place:
 * once-per-run setup/teardown, per-test log context (MDC), failure artefacts (screenshot, trace)
 * and guaranteed browser cleanup.
 */
@Slf4j
public class TafExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback {

    private static final Namespace NS = Namespace.create(TafExtension.class);

    @Override
    public void beforeAll(ExtensionContext context) {
        context.getRoot().getStore(NS)
                .getOrComputeIfAbsent(SuiteLifecycle.class, key -> new SuiteLifecycle(), SuiteLifecycle.class);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        MDC.put("test", context.getDisplayName());
        context.getStore(NS).put("start", System.nanoTime());
        log.info("START {}.{}", context.getRequiredTestClass().getSimpleName(), context.getDisplayName());
    }

    @Override
    public void afterEach(ExtensionContext context) {
        try {
            Long start = context.getStore(NS).remove("start", Long.class);
            long ms = start == null ? -1 : (System.nanoTime() - start) / 1_000_000;
            log.info("END   {} ({} ms)", context.getDisplayName(), ms);
        } finally {
            MDC.remove("test");
        }
    }
}