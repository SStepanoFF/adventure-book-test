package com.example.adventurebook.ui.steps;

import com.example.adventurebook.api.dto.type.GameStatus;
import com.example.adventurebook.ui.BaseSteps;
import com.example.adventurebook.ui.pages.GamePage;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** What a user does on the game page. {@code @Step} methods become steps of the Allure report. */
public class GameSteps extends BaseSteps<GamePage> {

    private static final Pattern SESSION_IN_URL = Pattern.compile("/game/([^/?#]+)");

    /** Game reached by navigating in the UI (session id unknown up front). */
    public GameSteps(Page page) {
        super(page, new GamePage(page));
    }

    /** Game of an existing session, e.g. one created through the API (hybrid tests). */
    public GameSteps(Page page, String sessionId) {
        super(page, new GamePage(page, sessionId));
    }

    @Step("Open the game page")
    public GameSteps open() {
        navigate();
        return this;
    }

    @Step("Choose option #{optionIndex}")
    public GameSteps choose(int optionIndex) {
        pageObject.option(optionIndex).click();
        return this;
    }

    /** Choose and wait until the backend has answered, so that the next click never hits a stale screen. */
    @Step("Choose option #{optionIndex} and wait for the response")
    public GameSteps chooseAndWait(int optionIndex) {
        page.waitForResponse(
                response -> response.url().contains("/choices/") && "POST".equals(response.request().method()),
                () -> pageObject.option(optionIndex).click());
        return this;
    }

    /** Touch variant, for tablet tests. */
    @Step("Tap option #{optionIndex}")
    public GameSteps tapOption(int optionIndex) {
        pageObject.option(optionIndex).tap();
        return this;
    }

    @Step("Choose the option containing '{text}'")
    public GameSteps chooseOptionContaining(String text) {
        pageObject.optionContaining(text).click();
        return this;
    }

    @Step("Save the game")
    public GameSteps save() {
        pageObject.getSaveButton().click();
        return this;
    }

    @Step("Restart the book")
    public GameSteps restart() {
        pageObject.getRestartButton().click();
        return this;
    }

    @Step("Go back to the library")
    public LibrarySteps backToLibrary() {
        pageObject.getBackButton().click();
        LibrarySteps library = new LibrarySteps(page);
        library.waitUntilLoaded();
        return library;
    }

    // ---------- reads (snapshots; use GameAsserts to wait for a state) ----------

    /** Session id taken from the current URL (after the game was started through the UI). */
    public String sessionIdFromUrl() {
        Matcher matcher = SESSION_IN_URL.matcher(page.url());
        if (!matcher.find()) {
            throw new IllegalStateException("Not a game URL: " + page.url());
        }
        return matcher.group(1);
    }

    public String bookTitle() {
        return text(pageObject.getBookTitle());
    }

    public GameStatus status() {
        return GameStatus.valueOf(text(pageObject.getStatus()));
    }

    /** Parsed from the label "Health: N". */
    public int health() {
        Matcher matcher = Pattern.compile("(-?\\d+)").matcher(text(pageObject.getHealth()));
        if (!matcher.find()) {
            throw new IllegalStateException("No number in the health label");
        }
        return Integer.parseInt(matcher.group(1));
    }

    public String sectionText() {
        return text(pageObject.getSectionText());
    }

    public List<String> optionTexts() {
        return pageObject.getOptions().allInnerTexts().stream().map(String::trim).toList();
    }

    public int optionCount() {
        return pageObject.getOptions().count();
    }
}
