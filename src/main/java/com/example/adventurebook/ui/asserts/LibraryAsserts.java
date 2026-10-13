package com.example.adventurebook.ui.asserts;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.example.adventurebook.ui.pages.LibraryPage;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;

/** Checks on the library page. Web-first assertions: each one retries until the UI catches up or times out. */
public class LibraryAsserts {

    private final LibraryPage library;

    public LibraryAsserts(Page page) {
        this.library = new LibraryPage(page);
    }

    @Step("Library page is loaded")
    public LibraryAsserts shouldBeLoaded() {
        assertThat(library.getHeading()).isVisible();
        return this;
    }

    @Step("Library shows '{title}'")
    public LibraryAsserts shouldShowBook(String title) {
        assertThat(library.bookCard(title).getLocator()).isVisible();
        return this;
    }

    @Step("Library does not show '{title}'")
    public LibraryAsserts shouldNotShowBook(String title) {
        assertThat(library.bookCard(title).getLocator()).hasCount(0);
        return this;
    }

    @Step("Library shows {expected} book(s)")
    public LibraryAsserts shouldShowBookCount(int expected) {
        assertThat(library.getCards()).hasCount(expected);
        return this;
    }

    @Step("Library shows the 'no books' message")
    public LibraryAsserts shouldShowNoResultsMessage() {
        assertThat(library.getEmptyMessage()).isVisible();
        return this;
    }

    @Step("Library offers to continue '{bookTitle}'")
    public LibraryAsserts shouldOfferSavedGame(String bookTitle) {
        assertThat(library.getSavedGamesSection()).isVisible();
        assertThat(library.continueButton(bookTitle)).isVisible();
        return this;
    }

    @Step("Library no longer offers the saved game of '{bookTitle}'")
    public LibraryAsserts shouldNotOfferSavedGame(String bookTitle) {
        assertThat(library.savedGameItem(bookTitle)).hasCount(0);
        return this;
    }

    @Step("Library shows the brand link")
    public LibraryAsserts shouldShowBrandLink() {
        assertThat(library.getBrandLink()).isVisible();
        return this;
    }
}
