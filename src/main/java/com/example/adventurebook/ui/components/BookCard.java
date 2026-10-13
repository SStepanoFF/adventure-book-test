package com.example.adventurebook.ui.components;

import com.example.adventurebook.ui.BaseComponent;
import com.microsoft.playwright.Locator;
import lombok.Getter;

/**
 * One card of the library grid: only the locators of its parts. The card root has data-testid="book-card", but
 * title/author/play button do NOT have test ids, so CSS fallbacks are used (candidates for missing test ids).
 */
@Getter
public class BookCard extends BaseComponent {

    private final Locator header = locator.locator("h2");
    private final Locator author = locator.locator(".author");
    private final Locator description = locator.locator(".description");
    private final Locator tags = locator.locator(".tags .chip");
    private final Locator sectionCount = locator.locator(".section-count");
    // no data-testid and no accessible name (the only content is an aria-hidden arrow) -> structural fallback
    private final Locator playButton = locator.locator(".book-card-footer button");

    public BookCard(Locator root) {
        super(root);
    }
}
