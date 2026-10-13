package com.example.adventurebook.junit;

import com.example.adventurebook.api.ApiCoverage;
import com.example.adventurebook.api.OpenApiOperations;
import com.example.adventurebook.config.TafConfig;
import com.example.adventurebook.ui.BrowserManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Runs exactly once per JVM: banner, Allure environment info, app hooks; and cleans up at the very end. */
@Slf4j
final class SuiteLifecycle implements ExtensionContext.Store.CloseableResource {

    private final List<SuiteHook> hooks = new ArrayList<>();

    SuiteLifecycle() {
        TafConfig config = TafConfig.get();
        log.info("================ TEST RUN ================");
        log.info("{}", config);
        writeAllureEnvironment(config);
        ServiceLoader.load(SuiteHook.class).forEach(hooks::add);
        hooks.forEach(SuiteHook::beforeSuite);
    }

    @Override
    public void close() {
        try {
            hooks.forEach(SuiteHook::afterSuite);
            reportApiCoverage();
        } finally {
            BrowserManager.closeAll();
            log.info("================ END OF RUN ================");
        }
    }

    /**
     * Which operations of the OpenAPI document were called, with a 2xx response and through the contract client.
     * Only meaningful after a FULL run: a filtered run (-Dgroups=ui) legitimately leaves gaps, so by default they
     * are warnings; -Dcoverage.enforce=true turns them into a failure (for the full CI run).
     */
    private void reportApiCoverage() {
        TafConfig config = TafConfig.get();
        ApiCoverage.Report report;
        try {
            report = ApiCoverage.evaluate(OpenApiOperations.load(config.openApiSpec()));
        } catch (RuntimeException e) {
            log.warn("API coverage not evaluated: {}", e.getMessage());
            return;
        }
        log.info("API coverage of this run:\n{}", report.markdown());
        try {
            Files.createDirectories(Path.of("target"));
            Files.writeString(Path.of("target", "api-coverage.md"), report.markdown());
        } catch (IOException e) {
            log.warn("Could not write target/api-coverage.md: {}", e.getMessage());
        }
        List<String> gaps = report.runGaps();
        gaps.forEach(gap -> log.warn("API coverage gap - {}", gap));
        if (config.enforceApiCoverage() && !gaps.isEmpty()) {
            throw new IllegalStateException("API coverage objective not met: " + gaps);
        }
    }

    /** Shown in the "Environment" block of the Allure report. */
    private static void writeAllureEnvironment(TafConfig config) {
        try {
            Path dir = Path.of(System.getProperty("allure.results.directory", "target/allure-results"));
            Files.createDirectories(dir);
            String content = "environment=" + config.envName() + "\n"
                    + "api.base-url=" + config.apiBaseUrl() + "\n"
                    + "ui.base-url=" + config.uiBaseUrl() + "\n"
                    + "browser=" + config.browser() + "\n"
                    + "headless=" + config.headless() + "\n";
            Files.writeString(dir.resolve("environment.properties"), content);
        } catch (IOException e) {
            log.warn("Could not write Allure environment.properties: {}", e.getMessage());
        }
    }
}
