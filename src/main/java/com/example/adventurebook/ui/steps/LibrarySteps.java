package com.example.adventurebook.ui.steps;

import com.example.adventurebook.ui.BaseSteps;
import com.example.adventurebook.ui.components.BookCard;
import com.example.adventurebook.ui.pages.LibraryPage;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import java.util.List;

/** What a user does on the library page. {@code @Step} methods become steps of the Allure report. */
public class LibrarySteps extends BaseSteps<LibraryPage> {

    /** Texts of one card as displayed, e.g. author "by Daniel El Fuego", sectionCount "7 sections". */
    public record BookCardInfo(String title, String author, String description, List<String> tags,
                               String sectionCount) {
    }

    public LibrarySteps(Page page) {
        super(page, new LibraryPage(page));
    }

    @Step("Open the library page")
    public LibrarySteps open() {
        navigate();
        return this;
    }

    @Step("Search books for '{text}'")
    public LibrarySteps search(String text) {
        pageObject.getSearchInput().fill(text);
        pageObject.getSearchButton().click();
        return this;
    }

    @Step("Filter books by tag '{tag}'")
    public LibrarySteps filterByTag(String tag) {
        pageObject.getTagInput().fill(tag);
        pageObject.getSearchButton().click();
        return this;
    }

    @Step("Open the 'Add a book' page")
    public UploadSteps openUploadPage() {
        pageObject.getUploadLink().click();
        UploadSteps upload = new UploadSteps(page);
        upload.waitUntilLoaded();
        return upload;
    }

    @Step("Start the book '{title}' from the library")
    public GameSteps startBook(String title) {
        pageObject.bookCard(title).getPlayButton().click();
        GameSteps game = new GameSteps(page);
        game.waitUntilLoaded();
        return game;
    }

    /** Continues the saved game of the given book (button text: Continue "Title"). */
    @Step("Continue the saved game of '{bookTitle}'")
    public GameSteps continueSavedGame(String bookTitle) {
        pageObject.continueButton(bookTitle).click();
        GameSteps game = new GameSteps(page);
        game.waitUntilLoaded();
        return game;
    }

    @Step("Remove the saved game of '{bookTitle}'")
    public LibrarySteps removeSavedGame(String bookTitle) {
        pageObject.removeButton(bookTitle).click();
        return this;
    }

    // ---------- reads (snapshots; use LibraryAsserts to wait for a state) ----------

    public List<String> visibleBookTitles() {
        return pageObject.bookCards().stream().map(card -> text(card.getHeader())).toList();
    }

    public BookCardInfo bookInfo(String title) {
        BookCard card = pageObject.bookCard(title);
        return new BookCardInfo(
                text(card.getHeader()),
                text(card.getAuthor()),
                text(card.getDescription()),
                card.getTags().allInnerTexts().stream().map(String::trim).toList(),
                text(card.getSectionCount()));
    }
}
