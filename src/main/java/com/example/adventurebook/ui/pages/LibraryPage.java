package com.example.adventurebook.ui.pages;

import com.example.adventurebook.ui.BasePage;
import com.example.adventurebook.ui.components.BookCard;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import java.util.List;
import lombok.Getter;

/** Library page ("/"): locators only. Actions: {@code LibrarySteps}; checks: {@code LibraryAsserts}. */
@Getter
public class LibraryPage extends BasePage {

    private final Locator heading = page.getByRole(AriaRole.HEADING,
            new Page.GetByRoleOptions().setName("Adventure Book Library"));
    private final Locator uploadLink = page.getByTestId("upload-book-link");
    private final Locator searchInput = page.getByTestId("book-search-input");
    private final Locator tagInput = page.getByTestId("book-tag-filter");
    private final Locator searchButton = page.getByTestId("book-filter-submit");
    private final Locator grid = page.getByTestId("book-grid");
    private final Locator emptyMessage = page.getByTestId("book-grid-empty");
    private final Locator errorMessage = page.getByTestId("books-error");
    private final Locator cards = page.getByTestId("book-card");
    private final Locator savedGamesSection = page.getByTestId("saved-games-section");
    private final Locator continueButtons = page.getByTestId("continue-game-button");
    private final Locator savedGameItems = page.getByTestId("saved-game-item");
    // brand link in the app shell has no test id
    private final Locator brandLink = page.locator("a.app-brand");

    public LibraryPage(Page page) {
        super(page);
    }

    @Override
    public String path() {
        return "/";
    }

    @Override
    public Locator readyMarker() {
        return heading;
    }

    @Override
    public Locator contentMarker() {
        return cards.first();
    }

    /** Snapshot of the cards rendered right now. */
    public List<BookCard> bookCards() {
        return cards.all().stream().map(BookCard::new).toList();
    }

    /** Card of the book with exactly this title (the heading inside the card is matched exactly). */
    public BookCard bookCard(String title) {
        return new BookCard(cards.filter(new Locator.FilterOptions().setHas(
                page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(title).setExact(true)))));
    }

    /** The list entry of the saved game of this book. */
    public Locator savedGameItem(String bookTitle) {
        return savedGameItems.filter(new Locator.FilterOptions().setHasText(bookTitle));
    }

    /** "Remove" button of the saved game of this book. */
    public Locator removeButton(String bookTitle) {
        return savedGameItem(bookTitle).getByTestId("remove-saved-game-button");
    }

    /** "Continue" button of the saved game of this book. */
    public Locator continueButton(String bookTitle) {
        return continueButtons.filter(new Locator.FilterOptions().setHasText(bookTitle));
    }
}
