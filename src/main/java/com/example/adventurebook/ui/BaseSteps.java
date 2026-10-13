package com.example.adventurebook.ui;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.net.URI;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Base of the {@code <Page>Steps} classes: what a user DOES on a page. Shared behaviour only (navigation and
 * timing); fluent methods are written in the concrete class and simply return {@code this}, so no self-referencing
 * generic is needed. Business-level steps are annotated with Allure's {@code @Step} in the concrete classes.
 *
 * @param <P> the page object this steps class works with
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseSteps<P extends BasePage> {

    protected final Page page;
    protected final P pageObject;

    protected void navigate() {
        page.navigate(pageObject.path());
        waitUntilLoaded();
    }

    public void waitUntilLoaded() {
        pageObject.readyMarker().waitFor();
    }

    /**
     * Navigates and returns the time (ms) until the page's DATA is visible. Event-driven wait, so the figure does
     * not include assertion polling intervals.
     */
    public double openAndMeasureContentMs() {
        long start = System.nanoTime();
        page.navigate(pageObject.path());
        pageObject.contentMarker().waitFor();
        return (System.nanoTime() - start) / 1_000_000.0;
    }

    public String currentPath() {
        return URI.create(page.url()).getPath();
    }

    protected static String text(Locator locator) {
        return locator.innerText().trim();
    }
}
