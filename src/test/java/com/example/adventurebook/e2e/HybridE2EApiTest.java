package com.example.adventurebook.e2e;

import com.example.adventurebook.api.Json;
import com.example.adventurebook.api.dto.request.BookRequestDto;
import com.example.adventurebook.api.dto.response.BookResponseDto;
import com.example.adventurebook.api.dto.response.GameStateResponseDto;
import com.example.adventurebook.api.dto.type.GameStatus;
import com.example.adventurebook.api.payload.BookPayloads;
import com.example.adventurebook.api.service.BookService;
import com.example.adventurebook.api.service.GameService;
import com.example.adventurebook.support.UiApiTest;
import com.example.adventurebook.ui.asserts.GameAsserts;
import com.example.adventurebook.ui.steps.GameSteps;
import com.example.adventurebook.ui.steps.LibrarySteps;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end scenarios that mix levels: the API prepares state quickly and deterministically (the "preconditions"),
 * the UI performs the user journey, and the API verifies what really happened in the backend.
 *
 * <p>The application has no login. The equivalent of "log in through the API, then continue in the UI" is the
 * game SESSION: it is created through the API and the browser opens it directly. Browser-side state (the list
 * of saved games lives in localStorage) is pre-set with {@code ui().seedLocalStorage(...)}.
 */
@Tag("e2e")
@Epic("End to end")
@Feature("API + UI")
class HybridE2EApiTest extends UiApiTest {

    private static final String SAVED_GAMES_KEY = "adventureBook.savedGames";

    private final BookService bookService = new BookService();
    private final GameService gameService = new GameService();

    @Test
    @Story("Play")
    @DisplayName("API: create the book and play to section 2 -> UI: finish the adventure -> API: status is FINISHED")
    void finishInUiWhatWasStartedInApi() {
        // preconditions through the API
        BookResponseDto book = bookService.addOk(BookPayloads.threeStepStory());
        GameStateResponseDto session = gameService.startOk(book.id());
        gameService.chooseOk(session.sessionId(), 0);

        // journey through the UI, continuing exactly where the API left off
        GameSteps game = gameSteps(session.sessionId()).open();
        GameAsserts check = gameAsserts();
        check.shouldShowSectionContaining(BookPayloads.SECTION_2).shouldBeInProgress();
        game.choose(0);
        check.shouldShowSectionContaining(BookPayloads.SECTION_3);
        game.choose(0);
        check.shouldBeFinished();

        // verification through the API
        GameStateResponseDto backend = gameService.getOk(session.sessionId());
        assertThat(backend.status()).isEqualTo(GameStatus.FINISHED);
        assertThat(backend.currentSection().text()).isEqualTo(BookPayloads.SECTION_END);
    }

    @Test
    @Story("Death")
    @DisplayName("UI: a fatal choice kills the player -> API confirms DEAD and refuses further choices")
    void dieInUiAndVerifyInApi() {
        GameStateResponseDto session = gameService.startOk(bookService.addOk(BookPayloads.withRiskyChoice(10)).id());

        gameSteps(session.sessionId()).open().choose(1);
        gameAsserts().shouldBeDead();

        GameStateResponseDto backend = gameService.getOk(session.sessionId());
        assertThat(backend.status()).isEqualTo(GameStatus.DEAD);
        assertThat(backend.health()).isZero();
        gameService.choose(session.sessionId(), 0).then().statusCode(409);
    }

    @Test
    @Story("Save & resume")
    @DisplayName("Save in the UI -> the library offers 'Continue' -> the game resumes with the same progress")
    void saveAndContinueFromLibrary() {
        BookRequestDto input = BookPayloads.withRiskyChoice(4);
        GameStateResponseDto session = gameService.startOk(bookService.addOk(input).id());
        gameService.chooseOk(session.sessionId(), 1); // API: take the risky path, health 6

        GameSteps game = gameSteps(session.sessionId()).open();
        gameAsserts().shouldShowHealth(6);
        game.save();
        gameAsserts().shouldConfirmSaved();

        LibrarySteps library = game.backToLibrary();
        libraryAsserts().shouldOfferSavedGame(input.title());
        library.continueSavedGame(input.title());

        gameAsserts().shouldShowHealth(6).shouldShowSectionContaining(BookPayloads.SECTION_2).shouldBeInProgress();
        assertThat(gameService.getOk(session.sessionId()).health()).isEqualTo(6);
    }

    @Test
    @Story("Save & resume")
    @DisplayName("Browser state pre-set for a returning player -> 'Continue' resumes a session created in the API")
    void returningPlayerWithSeededBrowserState() {
        BookRequestDto input = BookPayloads.threeStepStory();
        GameStateResponseDto session = gameService.startOk(bookService.addOk(input).id());
        gameService.playOk(session.sessionId(), 0, 0); // API: now at section 3

        // what the UI itself stores when the player presses "Save"
        String saved = Json.write(List.of(Map.of(
                "sessionId", session.sessionId(),
                "bookTitle", input.title(),
                "savedAt", Instant.now().toString())));
        ui().seedLocalStorage(SAVED_GAMES_KEY, saved);

        LibrarySteps library = librarySteps().open();
        libraryAsserts().shouldOfferSavedGame(input.title());
        library.continueSavedGame(input.title());

        gameAsserts().shouldShowSectionContaining(BookPayloads.SECTION_3);
    }

    @Test
    @Story("Save & resume")
    @DisplayName("Removing a saved game hides it in the library; the session itself stays in the backend")
    void removeSavedGame() {
        BookRequestDto input = BookPayloads.threeStepStory();
        GameStateResponseDto session = gameService.startOk(bookService.addOk(input).id());

        GameSteps game = gameSteps(session.sessionId()).open();
        game.save();
        gameAsserts().shouldConfirmSaved();
        LibrarySteps library = game.backToLibrary();
        libraryAsserts().shouldOfferSavedGame(input.title());

        library.removeSavedGame(input.title());

        libraryAsserts().shouldNotOfferSavedGame(input.title());
        // "Remove" only makes this browser forget the session id (there is no delete endpoint)
        gameService.getOk(session.sessionId());
    }

    @Test
    @Story("Restart")
    @DisplayName("API: player is dead -> UI: restart -> API: session is back at the beginning with full health")
    void restartInUiIsVisibleInApi() {
        GameStateResponseDto session = gameService.startOk(bookService.addOk(BookPayloads.withRiskyChoice(10)).id());
        gameService.chooseOk(session.sessionId(), 1); // dead

        GameSteps game = gameSteps(session.sessionId()).open();
        gameAsserts().shouldBeDead();
        game.restart();
        gameAsserts().shouldBeInProgress();

        GameStateResponseDto backend = gameService.getOk(session.sessionId());
        assertThat(backend.status()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(backend.health()).isEqualTo(10);
        assertThat(backend.currentSection().id()).isEqualTo(1);
    }

    @Test
    @Story("Play")
    @DisplayName("UI: start a book from the library -> API: move the session on -> UI reload shows the new state")
    void startInUiThenProgressThroughApi() {
        BookRequestDto input = BookPayloads.threeStepStory();
        bookService.addOk(input);

        GameSteps game = librarySteps().open().search(input.title()).startBook(input.title());
        String sessionId = game.sessionIdFromUrl();

        gameService.chooseOk(sessionId, 0); // another "device" moves the same session on
        page().reload();

        gameAsserts().shouldBeLoaded().shouldShowSectionContaining(BookPayloads.SECTION_2);
    }
}
