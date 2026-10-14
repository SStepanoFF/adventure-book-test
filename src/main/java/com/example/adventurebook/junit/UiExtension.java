package com.example.adventurebook.junit;

import com.example.adventurebook.config.TafConfig;
import com.example.adventurebook.ui.BrowserManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

@Slf4j
public class UiExtension implements AfterAllCallback, AfterEachCallback, TestExecutionExceptionHandler {

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        log.error("FAILED {}: {}", context.getDisplayName(), throwable.toString());
        BrowserManager.currentIfOpen().ifPresent(session -> session.onTestFailure(context.getDisplayName()));
        throw throwable;
    }

    @Override
    public void afterEach(ExtensionContext context) {
        BrowserManager.closeSession();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (closeBrowserAfterClass(context)) {
            BrowserManager.closeThreadBrowser();
        }
    }

    private boolean closeBrowserAfterClass(ExtensionContext context) {
        return switch (TafConfig.get().browserScope()) {
            case "class" -> true;
            case "run" -> false;
            default -> context.getConfigurationParameter("junit.jupiter.execution.parallel.enabled")
                    .map(Boolean::parseBoolean).orElse(false);
        };
    }
}
