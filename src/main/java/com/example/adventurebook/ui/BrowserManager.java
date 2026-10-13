package com.example.adventurebook.ui;

import com.example.adventurebook.config.TafConfig;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.extern.slf4j.Slf4j;

/**
 * Lifecycle policy for Playwright objects (see also {@link UiSession}):
 *
 * <pre>
 *   Playwright  - one per THREAD, created lazily (Playwright objects are not thread-safe; starting one spawns a driver process)
 *   Browser     - one per THREAD, reused by all tests on that thread (launching is the slow part)
 *   Context     - one per TEST (isolation: cookies, localStorage, cache; ~ms to create)
 *   Page        - created from the context of the test (extra tabs: session.getPage().context())
 * </pre>
 *
 * Tests never touch this class directly; the JUnit extension closes the session after each test and everything
 * else at the end of the run.
 */
@Slf4j
public final class BrowserManager {

    private static final ThreadLocal<Playwright> PLAYWRIGHT = new ThreadLocal<>();
    private static final ThreadLocal<Browser> BROWSER = new ThreadLocal<>();
    private static final ThreadLocal<UiSession> SESSION = new ThreadLocal<>();
    private static final Queue<Playwright> ALL_PLAYWRIGHTS = new ConcurrentLinkedQueue<>();

    private BrowserManager() {
    }

    /** Opens a fresh isolated session for the current test (replacing a previous one, if any). */
    public static UiSession open(DeviceProfile profile) {
        closeSession();
        UiSession session = new UiSession(browser(), profile);
        SESSION.set(session);
        return session;
    }

    /** Current session, opened with {@code profile} if the test has none yet. */
    public static UiSession currentOrOpen(DeviceProfile profile) {
        UiSession session = SESSION.get();
        return session != null ? session : open(profile);
    }

    public static Optional<UiSession> currentIfOpen() {
        return Optional.ofNullable(SESSION.get());
    }

    public static void closeSession() {
        UiSession session = SESSION.get();
        if (session != null) {
            SESSION.remove();
            session.close();
        }
    }

    /**
     * Closes the Playwright/Browser owned by the CURRENT thread. Playwright Java is driven by the calling thread,
     * so an instance must be closed by the thread that created it - this is what makes parallel runs safe.
     */
    public static void closeThreadBrowser() {
        closeSession();
        Playwright playwright = PLAYWRIGHT.get();
        BROWSER.remove();
        PLAYWRIGHT.remove();
        if (playwright != null) {
            ALL_PLAYWRIGHTS.remove(playwright);
            try {
                playwright.close(); // closes its browsers too
            } catch (RuntimeException e) {
                log.warn("Error while closing Playwright of thread {}: {}", Thread.currentThread().getName(), e.getMessage());
            }
        }
    }

    /** End of the run: close whatever is left (sequential runs; in class scope nothing is left). */
    public static void closeAll() {
        closeSession();
        Playwright playwright;
        while ((playwright = ALL_PLAYWRIGHTS.poll()) != null) {
            try {
                playwright.close(); // closes its browsers too
            } catch (RuntimeException e) {
                log.warn("Error while closing Playwright: {}", e.getMessage());
            }
        }
    }

    private static Browser browser() {
        Browser browser = BROWSER.get();
        if (browser != null && browser.isConnected()) {
            return browser;
        }
        TafConfig config = TafConfig.get();
        Playwright playwright = PLAYWRIGHT.get();
        if (playwright == null) {
            // do not let Playwright download every browser: BrowserInstaller fetches only the selected one
            playwright = Playwright.create(new Playwright.CreateOptions()
                    .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
            PLAYWRIGHT.set(playwright);
            ALL_PLAYWRIGHTS.add(playwright);
        }
        String name = switch (config.browser()) {
            case "firefox", "webkit" -> config.browser();
            default -> "chromium";
        };
        BrowserType type = switch (name) {
            case "firefox" -> playwright.firefox();
            case "webkit" -> playwright.webkit();
            default -> playwright.chromium();
        };
        BrowserInstaller.ensureInstalled(type, name);
        browser = type.launch(new BrowserType.LaunchOptions()
                .setHeadless(config.headless())
                .setSlowMo(config.slowMoMs()));
        BROWSER.set(browser);
        log.info("Browser launched: {} (headless={})", name, config.headless());
        return browser;
    }
}
