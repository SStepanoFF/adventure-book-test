package com.example.adventurebook.ui;

import com.example.adventurebook.api.dto.request.BookRequestDto;
import com.example.adventurebook.api.dto.response.BookResponseDto;
import com.example.adventurebook.api.dto.type.GameStatus;
import com.example.adventurebook.api.payload.BookPayloads;
import com.example.adventurebook.api.service.BookService;
import com.example.adventurebook.api.service.GameService;
import com.example.adventurebook.support.BookWalker;
import com.example.adventurebook.support.SeedBooks;
import com.example.adventurebook.support.UiApiTest;
import com.example.adventurebook.ui.asserts.GameAsserts;
import com.example.adventurebook.ui.steps.GameSteps;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Playing through the UI only; the books are created through the API so every scenario is deterministic. */
@Tag("ui")
@Epic("Play")
@Feature("Game page")
class GameplayUiApiTest extends UiApiTest {

    private final BookService bookService = new BookService();
    private final GameService gameService = new GameService();

    private GameSteps startInUi(BookRequestDto book) {
        bookService.addOk(book);
        return librarySteps().open().search(book.title()).startBook(book.title());
    }

    @Test
    @Story("Play to the end")
    @DisplayName("Following the choices to an END section finishes the adventure")
    void playToTheEnd() {
        BookRequestDto book = BookPayloads.threeStepStory();
        GameSteps game = startInUi(book);
        GameAsserts check = gameAsserts();

        check.shouldShowBookTitle(book.title())
                .shouldShowSectionContaining(BookPayloads.SECTION_1)
                .shouldShowHealth(10)
                .shouldBeInProgress();

        game.choose(0);
        check.shouldShowSectionContaining(BookPayloads.SECTION_2);
        game.choose(0);
        check.shouldShowSectionContaining(BookPayloads.SECTION_3);
        game.choose(0);
        check.shouldShowSectionContaining(BookPayloads.SECTION_END).shouldBeFinished();
    }

    @Test
    @Story("Play to the end")
    @DisplayName("A REAL built-in book is played to its ending in the browser (path found through the API)")
    void builtInBookIsPlayedToTheEnd() {
        BookResponseDto book = bookService.listOk(SeedBooks.PRISONER, null).stream()
                .filter(candidate -> candidate.title().equals(SeedBooks.PRISONER)).findFirst().orElseThrow();
        List<Integer> path = new BookWalker(gameService).findPathToEnding(book.id());

        GameSteps game = librarySteps().open().search(SeedBooks.PRISONER).startBook(SeedBooks.PRISONER);
        path.forEach(game::chooseAndWait);

        gameAsserts().shouldBeFinished();
    }

    @Test
    @Story("Consequences")
    @DisplayName("A risky option is marked, costs health and shows its consequence text")
    void riskyChoiceCostsHealth() {
        GameSteps game = startInUi(BookPayloads.withRiskyChoice(4));

        assertThat(game.optionTexts().get(0)).doesNotContain("⚠");
        assertThat(game.optionTexts().get(1)).contains("⚠");

        game.choose(1);
        gameAsserts().shouldShowHealth(6)
                .shouldShowConsequence("You hit the rocks hard.")
                .shouldBeInProgress();
    }

    @Test
    @Story("Death")
    @DisplayName("Health reaching 0 ends the adventure: dead message, no options, saving disabled")
    void deathEndsTheAdventure() {
        GameSteps game = startInUi(BookPayloads.withRiskyChoice(10));

        game.choose(1);

        gameAsserts().shouldShowHealth(0).shouldBeDead().shouldHaveSaveDisabled().shouldHaveRestartEnabled();
        assertThat(game.optionCount()).isZero();
    }

    @Test
    @Story("Restart")
    @DisplayName("Restart after death brings the player back to the beginning with full health")
    void restartAfterDeath() {
        GameSteps game = startInUi(BookPayloads.withRiskyChoice(10));
        game.choose(1);
        gameAsserts().shouldBeDead();

        game.restart();

        gameAsserts().shouldBeInProgress()
                .shouldHaveStatus(GameStatus.IN_PROGRESS)
                .shouldShowHealth(10)
                .shouldShowSectionContaining(BookPayloads.SECTION_1);
    }

    @Test
    @Story("Restart")
    @DisplayName("A finished adventure can be restarted")
    void restartAfterFinish() {
        GameSteps game = startInUi(BookPayloads.minimal());
        game.choose(0);
        gameAsserts().shouldBeFinished();

        game.restart();

        gameAsserts().shouldBeInProgress().shouldShowSectionContaining(BookPayloads.SECTION_1);
    }

    @Test
    @Story("Save")
    @DisplayName("Saving shows a confirmation")
    void saveShowsConfirmation() {
        GameSteps game = startInUi(BookPayloads.threeStepStory());

        game.save();

        gameAsserts().shouldConfirmSaved();
    }

    @Test
    @Story("Navigation")
    @DisplayName("Back to the library")
    void backToLibrary() {
        GameSteps game = startInUi(BookPayloads.minimal());

        game.backToLibrary();

        libraryAsserts().shouldBeLoaded();
    }
}
