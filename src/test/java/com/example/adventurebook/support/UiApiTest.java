package com.example.adventurebook.support;

import com.example.adventurebook.ui.BrowserManager;
import com.example.adventurebook.ui.DeviceProfile;
import com.example.adventurebook.ui.UiSession;
import com.example.adventurebook.ui.asserts.GameAsserts;
import com.example.adventurebook.ui.asserts.LibraryAsserts;
import com.example.adventurebook.ui.asserts.UploadAsserts;
import com.example.adventurebook.ui.pages.GamePage;
import com.example.adventurebook.ui.pages.LibraryPage;
import com.example.adventurebook.ui.steps.GameSteps;
import com.example.adventurebook.ui.steps.LibrarySteps;
import com.example.adventurebook.ui.steps.UploadSteps;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Base of tests that drive the browser. The browser is opened lazily on first use (so API-only tests that
 * extend BaseTest never start one) and closed by the extension after every test. Steps/Asserts/Page objects are
 * cheap wrappers around the current page, created on demand.
 */
@Execution(ExecutionMode.SAME_THREAD) // a browser belongs to one thread: never spread the methods of a UI class
public abstract class UiApiTest extends BaseUiTest {

    /** Override (or call {@link #openBrowser}) to change the emulated device. */
    protected DeviceProfile defaultProfile() {
        return DeviceProfile.DESKTOP;
    }

    protected UiSession openBrowser(DeviceProfile profile) {
        return BrowserManager.open(profile);
    }

    protected UiSession ui() {
        return BrowserManager.currentOrOpen(defaultProfile());
    }

    protected Page page() {
        return ui().getPage();
    }

    // ---- steps: what the user does ----
    protected LibrarySteps librarySteps() {
        return new LibrarySteps(page());
    }

    protected UploadSteps uploadSteps() {
        return new UploadSteps(page());
    }

    /** Game reached through the UI (session id taken from the URL when needed). */
    protected GameSteps gameSteps() {
        return new GameSteps(page());
    }

    /** Game of a session created through the API. */
    protected GameSteps gameSteps(String sessionId) {
        return new GameSteps(page(), sessionId);
    }

    // ---- asserts: what must be true ----
    protected LibraryAsserts libraryAsserts() {
        return new LibraryAsserts(page());
    }

    protected UploadAsserts uploadAsserts() {
        return new UploadAsserts(page());
    }

    protected GameAsserts gameAsserts() {
        return new GameAsserts(page());
    }

    // ---- page objects: only for tests that need raw locators (layout checks, accessibility) ----
    protected LibraryPage libraryPage() {
        return new LibraryPage(page());
    }

    protected GamePage gamePage(String sessionId) {
        return new GamePage(page(), sessionId);
    }
}
