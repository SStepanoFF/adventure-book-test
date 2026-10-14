package com.example.adventurebook.api;

import com.example.adventurebook.api.dto.request.BookRequestDto;
import com.example.adventurebook.api.dto.response.ErrorResponseDto;
import com.example.adventurebook.api.dto.response.GameStateResponseDto;
import com.example.adventurebook.api.dto.type.GameStatus;
import com.example.adventurebook.api.dto.type.SectionType;
import com.example.adventurebook.api.payload.BookPayloads;
import com.example.adventurebook.api.service.BookService;
import com.example.adventurebook.api.service.GameService;
import com.example.adventurebook.support.BaseApiTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("api")
@Epic("Play")
@Feature("Game API")
class GameApiApiTest extends BaseApiTest {
    private final BookService bookService = new BookService();

    private final GameService gameService = new GameService();


    private GameStateResponseDto newGame(BookRequestDto book) {
        return gameService.startOk(bookService.addOk(book).id());
    }

    // ------------------------------------------------------------------ start / get

    @Test
    @Story("Start a game")
    @DisplayName("A new game starts at the BEGIN section with full health")
    void startGame() {
        BookRequestDto book = BookPayloads.threeStepStory();

        GameStateResponseDto game = gameService.startOk(bookService.addOk(book).id());

        assertThat(UUID.fromString(game.sessionId())).isNotNull();
        assertThat(game.bookTitle()).isEqualTo(book.title());
        assertThat(game.health()).isEqualTo(10);
        assertThat(game.maxHealth()).isEqualTo(10);
        assertThat(game.status()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(game.gameOver()).isFalse();
        assertThat(game.currentSection().id()).isEqualTo(1);
        assertThat(game.currentSection().type()).isEqualTo(SectionType.BEGIN);
        assertThat(game.currentSection().options()).hasSize(1);
        assertThat(game.lastConsequence()).isNull();
    }

    @Test
    @Story("Start a game")
    @DisplayName("Unknown book -> 404")
    void startGameForUnknownBook() {
        gameService.start(999_999L).then().statusCode(404).extract().as(ErrorResponseDto.class);
    }

    @Test
    @Story("Start a game")
    @DisplayName("Missing bookId -> 400")
    void startGameWithoutBookId() {
        gameService.startRaw("{}").then().statusCode(400);
    }

    @Test
    @Story("Resume")
    @DisplayName("GET returns the current state of the session")
    void getGame() {
        GameStateResponseDto started = newGame(BookPayloads.threeStepStory());

        assertThat(gameService.getOk(started.sessionId())).isEqualTo(started);
    }

    @Test
    @Story("Resume")
    @DisplayName("Unknown session -> 404")
    void getUnknownSession() {
        gameService.get(UUID.randomUUID().toString()).then().statusCode(404).extract().as(ErrorResponseDto.class);
    }

    // ------------------------------------------------------------------ choices

    @Test
    @Story("Make a choice")
    @DisplayName("A choice moves the player to the target section")
    void chooseAdvances() {
        GameStateResponseDto game = newGame(BookPayloads.threeStepStory());

        GameStateResponseDto next = gameService.chooseOk(game.sessionId(), 0);

        assertThat(next.currentSection().id()).isEqualTo(2);
        assertThat(next.currentSection().text()).isEqualTo(BookPayloads.SECTION_2);
        assertThat(next.health()).isEqualTo(10);
        assertThat(next.status()).isEqualTo(GameStatus.IN_PROGRESS);
    }

    @Test
    @Story("Make a choice")
    @DisplayName("Reaching an END section finishes the game")
    void reachingEndFinishesTheGame() {
        GameStateResponseDto game = newGame(BookPayloads.minimal());

        GameStateResponseDto finished = gameService.chooseOk(game.sessionId(), 0);

        assertThat(finished.status()).isEqualTo(GameStatus.FINISHED);
        assertThat(finished.gameOver()).isTrue();
        assertThat(finished.currentSection().type()).isEqualTo(SectionType.END);
    }

    @Test
    @Story("Make a choice")
    @DisplayName("A risky option is flagged, but its damage is not revealed before choosing")
    void riskyOptionIsFlagged() {
        GameStateResponseDto game = newGame(BookPayloads.withRiskyChoice(4));

        assertThat(game.currentSection().options().get(0).hasConsequence()).isFalse();
        assertThat(game.currentSection().options().get(1).hasConsequence()).isTrue();
    }

    @ParameterizedTest(name = "option index {0}")
    @ValueSource(ints = {-1, 2, 99})
    @Story("Make a choice")
    @DisplayName("An option index that does not exist -> 400")
    void invalidOptionIndex(int index) {
        GameStateResponseDto game = newGame(BookPayloads.withRiskyChoice(4));

        gameService.choose(game.sessionId(), index).then().statusCode(400);
        assertThat(gameService.getOk(game.sessionId())).as("state must not change").isEqualTo(game);
    }

    @Test
    @Story("Make a choice")
    @DisplayName("Choosing in an unknown session -> 404")
    void chooseInUnknownSession() {
        gameService.choose(UUID.randomUUID().toString(), 0).then().statusCode(404);
    }

    @Test
    @Story("Make a choice")
    @DisplayName("No more choices once the game is finished -> 409, state unchanged")
    void chooseAfterGameEnded() {
        GameStateResponseDto game = newGame(BookPayloads.minimal());
        GameStateResponseDto finished = gameService.chooseOk(game.sessionId(), 0);

        gameService.choose(game.sessionId(), 0).then().statusCode(409).extract().as(ErrorResponseDto.class);

        assertThat(gameService.getOk(game.sessionId())).isEqualTo(finished);
    }

    // ------------------------------------------------------------------ save & resume

    @Test
    @Story("Save & resume")
    @DisplayName("Save returns the persisted state and progress can be resumed")
    void saveAndResume() {
        GameStateResponseDto game = newGame(BookPayloads.threeStepStory());
        GameStateResponseDto progressed = gameService.chooseOk(game.sessionId(), 0);

        GameStateResponseDto saved = gameService.saveOk(game.sessionId());

        assertThat(saved).isEqualTo(progressed);
        assertThat(gameService.getOk(game.sessionId())).isEqualTo(progressed);
    }

    @Test
    @Story("Save & resume")
    @DisplayName("Saving an unknown session -> 404")
    void saveUnknownSession() {
        gameService.save(UUID.randomUUID().toString()).then().statusCode(404);
    }

    // ------------------------------------------------------------------ restart

    @Test
    @Story("Restart")
    @DisplayName("Restart of a game in progress goes back to the beginning")
    void restartInProgress() {
        GameStateResponseDto game = newGame(BookPayloads.withRiskyChoice(4));
        gameService.chooseOk(game.sessionId(), 1);

        GameStateResponseDto restarted = gameService.restartOk(game.sessionId());

        assertThat(restarted.currentSection().id()).isEqualTo(1);
        assertThat(restarted.health()).isEqualTo(10);
        assertThat(restarted.status()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(restarted.lastConsequence()).isNull();
        assertThat(restarted.sessionId()).isEqualTo(game.sessionId());
    }

    @Test
    @Story("Restart")
    @DisplayName("A finished game can be restarted and played again")
    void restartFinishedGame() {
        GameStateResponseDto game = newGame(BookPayloads.minimal());
        gameService.chooseOk(game.sessionId(), 0);

        GameStateResponseDto restarted = gameService.restartOk(game.sessionId());

        assertThat(restarted.status()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(restarted.gameOver()).isFalse();
        gameService.chooseOk(game.sessionId(), 0); // playable again
    }

    @Test
    @Story("Restart")
    @DisplayName("Restarting an unknown session -> 404")
    void restartUnknownSession() {
        gameService.restart(UUID.randomUUID().toString()).then().statusCode(404);
    }
}
