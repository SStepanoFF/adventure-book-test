package com.example.adventurebook.ui;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Base of page objects. A page object only DESCRIBES a page: where it lives and which elements it has, as plain
 * Playwright {@link Locator}s built straight from the {@link Page} (so every locator variant is available).
 * Behaviour lives in the page's {@code ...Steps} class, checks in its {@code ...Asserts} class.
 * No Browser/Context lifecycle here either (see {@link BrowserManager}): a page just receives a {@code Page}.
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BasePage {

    @Getter
    protected final Page page;

    /** Route of the page, relative to ui.base-url. */
    public abstract String path();

    /** An element that is visible once the page has rendered its static frame. */
    public abstract Locator readyMarker();

    /** An element that proves the page's DATA has been rendered (defaults to the ready marker). */
    public Locator contentMarker() {
        return readyMarker();
    }
}
