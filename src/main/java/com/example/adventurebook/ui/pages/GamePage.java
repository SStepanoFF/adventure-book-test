package com.example.adventurebook.ui.pages;

import com.example.adventurebook.ui.BasePage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import lombok.Getter;

/** Game page ("/game/{id}"): locators only. Actions: {@code GameSteps}; checks: {@code GameAsserts}. */
@Getter
public class GamePage extends BasePage {

    private final String sessionId;

    private final Locator header = page.getByTestId("game-header");
    private final Locator bookTitle = page.locator("[data-testid='game-header'] h1");
    private final Locator status = page.getByTestId("game-status");
    private final Locator health = page.getByTestId("game-health");
    private final Locator healthBar = page.getByRole(AriaRole.PROGRESSBAR);
    private final Locator saveButton = page.getByTestId("save-game-button");
    private final Locator savedConfirmation = page.getByTestId("game-saved-confirmation");
    private final Locator restartButton = page.getByTestId("restart-game-button");
    private final Locator sectionText = page.getByTestId("section-text");
    private final Locator lastConsequence = page.getByTestId("last-consequence-text");
    private final Locator finishedMessage = page.getByTestId("game-finished-message");
    private final Locator deadMessage = page.getByTestId("game-dead-message");
    private final Locator optionsList = page.getByTestId("options-list");
    private final Locator options = page.getByTestId("option-button");
    private final Locator noOptionsMessage = page.getByTestId("options-empty");
    private final Locator backButton = page.getByTestId("back-to-library-button");
    private final Locator error = page.getByTestId("game-error");

    /** Page reached by navigating in the UI (the session id is not known up front). */
    public GamePage(Page page) {
        this(page, null);
    }

    /** Page of an existing session, e.g. one created through the API (hybrid tests). */
    public GamePage(Page page, String sessionId) {
        super(page);
        this.sessionId = sessionId;
    }

    @Override
    public String path() {
        if (sessionId == null) {
            throw new IllegalStateException("Session id is unknown: create the page with new GamePage(page, id)");
        }
        return "/game/" + sessionId;
    }

    @Override
    public Locator readyMarker() {
        return header;
    }

    @Override
    public Locator contentMarker() {
        return sectionText;
    }

    public Locator option(int index) {
        return options.nth(index);
    }

    public Locator optionContaining(String text) {
        return options.filter(new Locator.FilterOptions().setHasText(text));
    }
}
