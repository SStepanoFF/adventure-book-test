package com.example.adventurebook.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.adventurebook.api.dto.response.BookResponseDto;
import com.example.adventurebook.api.dto.response.GameStateResponseDto;
import com.example.adventurebook.api.payload.BookPayloads;
import com.example.adventurebook.api.service.BookService;
import com.example.adventurebook.api.service.GameService;
import com.example.adventurebook.api.ApiClient;
import com.example.adventurebook.support.BaseTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Contract tests: the same calls as the functional tests, but every request and response is validated against
 * openapi.yaml by the contract filter (the call throws on any deviation: status, headers, schema, types).
 * One test per endpoint and per documented response family, so all endpoints are covered.
 */
@Tag("api")
@Tag("contract")
@Epic("API contract")
@Feature("OpenAPI conformance")
class ContractApiTest extends BaseTest {
    private final BookService books = new BookService();

    private final GameService games = new GameService();


    private final ApiClient contractApi = ApiClient.withContract();
    private final BookService contractBooks = new BookService(contractApi);
    private final GameService contractGames = new GameService(contractApi);

    private GameStateResponseDto startedGame() {
        BookResponseDto book = books.addOk(BookPayloads.withRiskyChoice(4));
        return games.startOk(book.id());
    }

    // ---------------------------------------------------------------- books

    @Test
    @Story("GET /api/books")
    @DisplayName("200 list conforms to the contract")
    void listBooks() {
        assertThat(contractBooks.listOk()).isNotEmpty();
    }

    @Test
    @Story("GET /api/books")
    @DisplayName("200 filtered list conforms to the contract")
    void listBooksWithFilters() {
        contractBooks.list("frost", "winter").then().statusCode(200);
    }

    @Test
    @Story("GET /api/books/{bookId}")
    @DisplayName("200 and 404 conform to the contract")
    void getBook() {
        BookResponseDto book = books.addOk(BookPayloads.minimal());
        contractBooks.get(book.id()).then().statusCode(200);
        contractBooks.get(999_999).then().statusCode(404);
    }

    @Test
    @Story("POST /api/books")
    @DisplayName("201 created conforms to the contract")
    void addBookCreated() {
        contractBooks.add(BookPayloads.threeStepStory()).then().statusCode(201);
    }

    @Test
    @Story("POST /api/books")
    @DisplayName("400 validation error conforms to the contract")
    void addBookRejected() {
        contractBooks.add(BookPayloads.noBeginning()).then().statusCode(400);
    }

    // ---------------------------------------------------------------- games

    @Test
    @Story("POST /api/games")
    @DisplayName("201 and 404 conform to the contract")
    void startGame() {
        BookResponseDto book = books.addOk(BookPayloads.minimal());
        contractGames.start(book.id()).then().statusCode(201);
        contractGames.start(999_999L).then().statusCode(404);
    }

    @Test
    @Story("GET /api/games/{sessionId}")
    @DisplayName("200 and 404 conform to the contract")
    void getGame() {
        GameStateResponseDto game = startedGame();
        contractGames.get(game.sessionId()).then().statusCode(200);
        contractGames.get(UUID.randomUUID().toString()).then().statusCode(404);
    }

    @Test
    @Story("POST /api/games/{sessionId}/choices/{optionIndex}")
    @DisplayName("200, 400 and 404 conform to the contract")
    void choose() {
        GameStateResponseDto game = startedGame();
        contractGames.choose(game.sessionId(), 1).then().statusCode(200); // risky option: consequence is present
        contractGames.choose(game.sessionId(), 42).then().statusCode(400);
        contractGames.choose(UUID.randomUUID().toString(), 0).then().statusCode(404);
    }

    @Test
    @Story("POST /api/games/{sessionId}/choices/{optionIndex}")
    @DisplayName("409 (game already ended) conforms to the contract")
    void chooseAfterEnd() {
        GameStateResponseDto game = games.startOk(books.addOk(BookPayloads.minimal()).id());
        contractGames.choose(game.sessionId(), 0).then().statusCode(200);
        contractGames.choose(game.sessionId(), 0).then().statusCode(409);
    }

    @Test
    @Story("POST /api/games/{sessionId}/save")
    @DisplayName("200 and 404 conform to the contract")
    void save() {
        GameStateResponseDto game = startedGame();
        contractGames.save(game.sessionId()).then().statusCode(200);
        contractGames.save(UUID.randomUUID().toString()).then().statusCode(404);
    }

    @Test
    @Story("POST /api/games/{sessionId}/restart")
    @DisplayName("200 and 404 conform to the contract")
    void restart() {
        GameStateResponseDto game = startedGame();
        contractGames.restart(game.sessionId()).then().statusCode(200);
        contractGames.restart(UUID.randomUUID().toString()).then().statusCode(404);
    }
}
