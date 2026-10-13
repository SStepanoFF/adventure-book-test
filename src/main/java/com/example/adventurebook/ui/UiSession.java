package com.example.adventurebook.ui;

import com.example.adventurebook.config.TafConfig;
import com.example.adventurebook.logging.Reports;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Tracing;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Everything one UI test owns: an isolated {@link BrowserContext} (own cookies/localStorage) and its page(s).
 * The expensive {@code Playwright}/{@code Browser} objects are shared per thread by {@link BrowserManager};
 * a context is cheap (~ms) and gives full isolation between tests.
 */
@Slf4j
public final class UiSession implements AutoCloseable {

    private final BrowserContext context;
    @Getter
    private final Page page;
    @Getter
    private final DeviceProfile profile;
    private final List<String> consoleErrors = Collections.synchronizedList(new ArrayList<>());
    private boolean failed;
    private String failedTestName = "test";

    UiSession(Browser browser, DeviceProfile profile) {
        TafConfig config = TafConfig.get();
        this.profile = profile;

        Browser.NewContextOptions options = new Browser.NewContextOptions()
                .setBaseURL(config.uiBaseUrl())
                .setViewportSize(profile.getWidth(), profile.getHeight())
                .setHasTouch(profile.isTouch());
        if (config.video()) {
            options.setRecordVideoDir(Paths.get("target/videos"));
        }
        this.context = browser.newContext(options);
        this.context.setDefaultTimeout(config.uiTimeout().toMillis());
        // trace is always recorded (cheap) but only saved when the test fails
        this.context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));

        this.page = context.newPage();
        hookBrowserDiagnostics(page);
        log.info("UI session opened ({} {}x{}, touch={})", config.browser(), profile.getWidth(), profile.getHeight(),
                profile.isTouch());
    }

    /** Errors printed to the browser console / uncaught page errors seen so far. */
    public List<String> consoleErrors() {
        return List.copyOf(consoleErrors);
    }

    /**
     * Pre-sets localStorage before any page script runs - the UI-side equivalent of "being logged in".
     * (The application has no authentication; when it gets one, this is where a stored login state goes.)
     */
    public void seedLocalStorage(String key, String jsonValue) {
        String script = "window.localStorage.setItem(" + com.example.adventurebook.api.Json.write(key) + ", "
                + com.example.adventurebook.api.Json.write(jsonValue) + ");";
        context.addInitScript(script);
    }

    /** Called by the JUnit extension while the browser is still open. */
    public void onTestFailure(String testName) {
        failed = true;
        this.failedTestName = testName;
        try {
            Reports.binary("Screenshot on failure", page.screenshot(new Page.ScreenshotOptions().setFullPage(true)));
            Reports.text("Page URL on failure", page.url());
            if (!consoleErrors.isEmpty()) {
                Reports.text("Browser console errors", String.join("\n", consoleErrors));
            }
        } catch (RuntimeException e) {
            log.warn("Could not capture failure artefacts: {}", e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            if (failed) {
                Path dir = Paths.get("target", "traces");
                Files.createDirectories(dir);
                Path zip = dir.resolve(failedTestName.replaceAll("[^a-zA-Z0-9._-]", "_") + "-"
                        + System.currentTimeMillis() + ".zip");
                context.tracing().stop(new Tracing.StopOptions().setPath(zip));
                Reports.file("Playwright trace (open with: npx playwright show-trace <file>)", zip);
            } else {
                context.tracing().stop();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            context.close();
            log.info("UI session closed");
        }
    }

    private void hookBrowserDiagnostics(Page p) {
        p.onConsoleMessage(message -> {
            if ("error".equals(message.type())) {
                consoleErrors.add(message.text());
                log.warn("BROWSER console error: {}", message.text());
            } else {
                log.debug("BROWSER console {}: {}", message.type(), message.text());
            }
        });
        p.onPageError(error -> {
            consoleErrors.add(error);
            log.warn("BROWSER page error: {}", error);
        });
        p.onRequestFailed(request -> log.warn("BROWSER request failed: {} {}", request.method(), request.url()));
    }
}
