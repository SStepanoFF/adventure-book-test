package com.example.adventurebook.api;

import com.example.adventurebook.api.dto.response.BookResponseDto;
import com.example.adventurebook.api.dto.response.GameStateResponseDto;
import com.example.adventurebook.api.payload.BookPayloads;
import com.example.adventurebook.api.service.BookService;
import com.example.adventurebook.api.service.GameService;
import com.example.adventurebook.api.service.TestSupportService;
import com.example.adventurebook.support.BaseApiTest;
import com.example.adventurebook.support.SeedBooks;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import static org.assertj.core.api.Assertions.assertThat;

/** POST /api/test/reset wipes global state, so this class never runs concurrently with any other test. */
@Isolated
@Tag("api")
@Tag("contract")
@Epic("Test support")
@Feature("Reset endpoint")
class TestSupportApiApiTest extends BaseApiTest {
    private final BookService bookService = new BookService();

    private final GameService gameService = new GameService();


    @Test
    @DisplayName("Reset (204, conforms to the contract) removes added books and all sessions, keeps the seed books")
    void resetRestoresSeedState() {
        BookResponseDto added = bookService.addOk(BookPayloads.minimal());
        GameStateResponseDto game = gameService.startOk(added.id());

        new TestSupportService(ApiClient.withContract())
                .reset().then().statusCode(204);

        bookService.get(added.id()).then().statusCode(404);
        gameService.get(game.sessionId()).then().statusCode(404);
        assertThat(bookService.listOk()).extracting(BookResponseDto::title).containsExactlyInAnyOrderElementsOf(SeedBooks.ALL_TITLES);
    }
}
