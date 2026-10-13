package com.example.adventurebook.ui.asserts;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.example.adventurebook.api.dto.type.GameStatus;
import com.example.adventurebook.ui.pages.GamePage;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;

/** Checks on the game page. Web-first assertions: each one retries until the UI catches up or times out. */
public class GameAsserts {

    private final GamePage game;

    public GameAsserts(Page page) {
        this.game = new GamePage(page);
    }

    @Step("Game page is loaded")
    public GameAsserts shouldBeLoaded() {
        assertThat(game.getHeader()).isVisible();
        return this;
    }

    @Step("Game shows the book title '{expected}'")
    public GameAsserts shouldShowBookTitle(String expected) {
        assertThat(game.getBookTitle()).hasText(expected);
        return this;
    }

    @Step("Section text contains '{text}'")
    public GameAsserts shouldShowSectionContaining(String text) {
        assertThat(game.getSectionText()).containsText(text);
        return this;
    }

    @Step("Health is {expected}")
    public GameAsserts shouldShowHealth(int expected) {
        assertThat(game.getHealth()).hasText("Health: " + expected);
        return this;
    }

    @Step("Status is {expected}")
    public GameAsserts shouldHaveStatus(GameStatus expected) {
        assertThat(game.getStatus()).hasText(expected.name());
        return this;
    }

    @Step("Adventure is in progress")
    public GameAsserts shouldBeInProgress() {
        assertThat(game.getStatus()).hasText(GameStatus.IN_PROGRESS.name());
        assertThat(game.getOptionsList()).isVisible();
        return this;
    }

    @Step("Adventure is finished")
    public GameAsserts shouldBeFinished() {
        assertThat(game.getFinishedMessage()).isVisible();
        assertThat(game.getOptionsList()).isHidden();
        assertThat(game.getStatus()).hasText(GameStatus.FINISHED.name());
        return this;
    }

    @Step("Player is dead, adventure over")
    public GameAsserts shouldBeDead() {
        assertThat(game.getDeadMessage()).isVisible();
        assertThat(game.getOptionsList()).isHidden();
        assertThat(game.getStatus()).hasText(GameStatus.DEAD.name());
        return this;
    }

    @Step("Consequence text contains '{text}'")
    public GameAsserts shouldShowConsequence(String text) {
        assertThat(game.getLastConsequence()).containsText(text);
        return this;
    }

    @Step("Game confirms 'Saved'")
    public GameAsserts shouldConfirmSaved() {
        assertThat(game.getSavedConfirmation()).isVisible();
        return this;
    }

    @Step("Save is disabled")
    public GameAsserts shouldHaveSaveDisabled() {
        assertThat(game.getSaveButton()).isDisabled();
        return this;
    }

    @Step("Restart is enabled")
    public GameAsserts shouldHaveRestartEnabled() {
        assertThat(game.getRestartButton()).isEnabled();
        return this;
    }

    @Step("'No options' message is shown")
    public GameAsserts shouldShowNoOptionsMessage() {
        assertThat(game.getNoOptionsMessage()).isVisible();
        return this;
    }

    @Step("Game shows an error")
    public GameAsserts shouldShowError() {
        assertThat(game.getError()).isVisible();
        return this;
    }
}
