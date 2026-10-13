package com.example.adventurebook.junit;

import com.example.adventurebook.ui.BrowserManager;
import lombok.extern.slf4j.Slf4j;
import com.example.adventurebook.config.TafConfig;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.slf4j.MDC;

/**
 * Cross-cutting behaviour for every test, in one place:
 * once-per-run setup/teardown, per-test log context (MDC), failure artefacts (screenshot, trace)
 * and guaranteed browser cleanup.
 */
@Slf4j
public class TafExtension
        implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback,
                   TestExecutionExceptionHandler {

    private static final Namespace NS = Namespace.create(TafExtension.class);

    @Override
    public void beforeAll(ExtensionContext context) {
        context.getRoot().getStore(NS)
                .getOrComputeIfAbsent(SuiteLifecycle.class, key -> new SuiteLifecycle(), SuiteLifecycle.class);
    }

    /** Parallel-safe cleanup: the browser is closed by the thread that created it, right after its class. */
    @Override
    public void afterAll(ExtensionContext context) {
        if (closeBrowserAfterClass(context)) {
            BrowserManager.closeThreadBrowser();
        }
    }

    private static boolean closeBrowserAfterClass(ExtensionContext context) {
        return switch (TafConfig.get().browserScope()) {
            case "class" -> true;
            case "run" -> false;
            default -> context.getConfigurationParameter("junit.jupiter.execution.parallel.enabled")
                    .map(Boolean::parseBoolean).orElse(false);
        };
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        MDC.put("test", context.getDisplayName());
        context.getStore(NS).put("start", System.nanoTime());
        log.info("START {}.{}", context.getRequiredTestClass().getSimpleName(), context.getDisplayName());
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        log.error("FAILED {}: {}", context.getDisplayName(), throwable.toString());
        // browser is still open here (AfterEach has not run yet) -> screenshot/trace can be taken
        BrowserManager.currentIfOpen().ifPresent(session -> session.onTestFailure(context.getDisplayName()));
        throw throwable;
    }

    @Override
    public void afterEach(ExtensionContext context) {
        try {
            BrowserManager.closeSession();
        } finally {
            Long start = context.getStore(NS).remove("start", Long.class);
            long ms = start == null ? -1 : (System.nanoTime() - start) / 1_000_000;
            log.info("END   {} ({} ms)", context.getDisplayName(), ms);
            MDC.remove("test");
        }
    }
}
