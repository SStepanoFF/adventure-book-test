package com.example.adventurebook.ui;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;

/** Generic layout checks used by responsive tests. */
public final class LayoutAssert {

    private LayoutAssert() {
    }

    public static void noHorizontalScroll(Page page) {
        Boolean fits = (Boolean) page.evaluate("() => document.documentElement.scrollWidth <= window.innerWidth");
        assertThat(fits).as("page content fits the viewport width (no horizontal scroll) at %s",
                page.viewportSize().width + "x" + page.viewportSize().height).isTrue();
    }

    public static void insideViewportWidth(Page page, Locator element) {
        BoundingBox box = element.boundingBox();
        assertThat(box).as("%s is rendered", element).isNotNull();
        assertThat(box.x).as("%s left edge", element).isGreaterThanOrEqualTo(0);
        assertThat(box.x + box.width).as("%s right edge", element).isLessThanOrEqualTo(page.viewportSize().width);
    }

    /** WCAG 2.2 AA (2.5.8) requires at least 24x24 CSS px; mobile guidelines recommend 44. */
    public static void minTapTarget(Locator element, double minPx) {
        BoundingBox box = element.boundingBox();
        assertThat(box).as("%s is rendered", element).isNotNull();
        assertThat(Math.min(box.width, box.height)).as("%s tap target size", element).isGreaterThanOrEqualTo(minPx);
    }
}
