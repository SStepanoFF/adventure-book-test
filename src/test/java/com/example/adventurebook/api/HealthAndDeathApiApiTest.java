package com.example.adventurebook.api;

import com.example.adventurebook.api.dto.response.GameStateResponseDto;
import com.example.adventurebook.api.dto.type.ConsequenceType;
import com.example.adventurebook.api.dto.type.GameStatus;
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

import static org.assertj.core.api.Assertions.assertThat;

/** Rule: the player starts with 10 health; when it reaches 0 the adventure is over. */
@Tag("api")
@Epic("Play")
@Feature("Consequences & health")
class HealthAndDeathApiApiTest extends BaseApiTest {
    private final BookService bookService = new BookService();

    private final GameService gameService = new GameService();


    private GameStateResponseDto newGame(com.example.adventurebook.api.dto.request.BookRequestDto book) {
        return gameService.startOk(bookService.addOk(book).id());
    }

    @Test
    @Story("Damage")
    @DisplayName("Non-lethal damage reduces health, the game continues")
    void nonLethalDamage() {
        GameStateResponseDto game = newGame(BookPayloads.withRiskyChoice(4));

        GameStateResponseDto after = gameService.chooseOk(game.sessionId(), 1);

        assertThat(after.health()).isEqualTo(6);
        assertThat(after.status()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(after.gameOver()).isFalse();
        assertThat(after.lastConsequence().type()).isEqualTo(ConsequenceType.LOSE_HEALTH);
        assertThat(after.lastConsequence().value()).isEqualTo(4);
        assertThat(after.lastConsequence().text()).isEqualTo("You hit the rocks hard.");
    }

    @Test
    @Story("Death")
    @DisplayName("Health reaching exactly 0 ends the adventure (DEAD)")
    void healthReachingZeroKills() {
        GameStateResponseDto game = newGame(BookPayloads.withRiskyChoice(10));

        GameStateResponseDto after = gameService.chooseOk(game.sessionId(), 1);

        assertThat(after.health()).isZero();
        assertThat(after.status()).isEqualTo(GameStatus.DEAD);
        assertThat(after.gameOver()).isTrue();
    }

    @Test
    @Story("Death")
    @DisplayName("Damage accumulates over several choices until the player dies")
    void cumulativeDamageKills() {
        GameStateResponseDto game = newGame(BookPayloads.twoHitsOfSix());

        GameStateResponseDto afterFirst = gameService.chooseOk(game.sessionId(), 0);
        assertThat(afterFirst.health()).isEqualTo(4);
        assertThat(afterFirst.status()).isEqualTo(GameStatus.IN_PROGRESS);

        GameStateResponseDto afterSecond = gameService.chooseOk(game.sessionId(), 0);
        assertThat(afterSecond.status()).isEqualTo(GameStatus.DEAD);
        assertThat(afterSecond.gameOver()).isTrue();
    }

    @Test
    @Story("Death")
    @DisplayName("A dead player cannot make further choices (409)")
    void deadPlayerCannotContinue() {
        GameStateResponseDto game = newGame(BookPayloads.withRiskyChoice(10));
        gameService.chooseOk(game.sessionId(), 1);

        gameService.choose(game.sessionId(), 0).then().statusCode(409);
    }

    @Test
    @Story("Death")
    @DisplayName("A dead player can restart with full health")
    void deadPlayerCanRestart() {
        GameStateResponseDto game = newGame(BookPayloads.withRiskyChoice(10));
        gameService.chooseOk(game.sessionId(), 1);

        GameStateResponseDto restarted = gameService.restartOk(game.sessionId());

        assertThat(restarted.status()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(restarted.health()).isEqualTo(10);
        assertThat(restarted.currentSection().id()).isEqualTo(1);
    }

    // ---------------------------------------------------------------------------------------------
    // Defect candidates (see BookValidationApiTest): assert the specified behaviour.
    // ---------------------------------------------------------------------------------------------

    @Test
    @Tag("defect-candidate")
    @Story("Death")
    @DisplayName("[candidate] A lethal choice that leads into an END section is DEAD, not FINISHED")
    void lethalChoiceIntoEndingKills() {
        // GameService.resolveStatus() checks "destination is END" before "health <= 0"
        GameStateResponseDto game = newGame(BookPayloads.lethalChoiceIntoEnding());

        GameStateResponseDto after = gameService.chooseOk(game.sessionId(), 0);

        assertThat(after.health()).isLessThanOrEqualTo(0);
        assertThat(after.status()).isEqualTo(GameStatus.DEAD);
    }

    @Test
    @Tag("defect-candidate")
    @Story("Damage")
    @DisplayName("[candidate] Health never drops below 0")
    void healthIsNeverNegative() {
        GameStateResponseDto game = newGame(BookPayloads.twoHitsOfSix()); // 6 + 6 = 12 > 10
        gameService.chooseOk(game.sessionId(), 0);

        GameStateResponseDto after = gameService.chooseOk(game.sessionId(), 0);

        assertThat(after.health()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @Tag("defect-candidate")
    @Story("Damage")
    @DisplayName("[candidate] A \"negative damage\" consequence cannot heal above the maximum")
    void healthNeverExceedsMaximum() {
        GameStateResponseDto game = newGame(BookPayloads.negativeDamage());

        GameStateResponseDto after = gameService.chooseOk(game.sessionId(), 0);

        assertThat(after.health()).isLessThanOrEqualTo(after.maxHealth());
    }
}
