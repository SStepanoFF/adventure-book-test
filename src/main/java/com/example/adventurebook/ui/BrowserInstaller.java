package com.example.adventurebook.ui;

import com.example.adventurebook.config.TafConfig;
import com.microsoft.playwright.BrowserType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Playwright downloads ALL browsers (chromium, firefox, webkit) the first time it starts. We switch that off
 * (PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD, see BrowserManager) and install only the browser that is actually selected,
 * and only when it is missing - nothing is downloaded when it is already there.
 */
@Slf4j
final class BrowserInstaller {

    private static final Set<String> VERIFIED = new HashSet<>();

    private BrowserInstaller() {
    }

    static synchronized void ensureInstalled(BrowserType type, String browserName) {
        if (VERIFIED.contains(browserName)) {
            return;
        }
        if (Files.exists(Path.of(type.executablePath()))) {
            log.info("Browser '{}' is installed", browserName);
        } else if (TafConfig.get().autoInstallBrowser()) {
            install(browserName);
        } else {
            throw new IllegalStateException("Browser '" + browserName + "' is not installed and "
                    + "ui.auto-install-browser=false. Install it: mvn exec:java -Dexec.mainClass="
                    + "com.microsoft.playwright.CLI -Dexec.args=\"install " + browserName + "\"");
        }
        VERIFIED.add(browserName);
    }

    /** Runs Playwright's own CLI in a separate JVM (the CLI ends with System.exit, which must not hit the tests). */
    private static void install(String browserName) {
        log.info("Browser '{}' is not installed - installing it now (one-time download)", browserName);
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        ProcessBuilder builder = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
                "com.microsoft.playwright.CLI", "install", browserName).redirectErrorStream(true);
        try {
            Process process = builder.start();
            try (BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = out.readLine()) != null) {
                    log.info("playwright install: {}", line);
                }
            }
            int exit = process.waitFor();
            if (exit != 0) {
                throw new IllegalStateException("'playwright install " + browserName + "' failed with exit code " + exit);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot run 'playwright install " + browserName + "'", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while installing " + browserName, e);
        }
    }
}
