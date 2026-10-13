package com.example.adventurebook.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.adventurebook.api.dto.response.BookResponseDto;
import com.example.adventurebook.api.dto.response.GameStateResponseDto;
import com.example.adventurebook.api.payload.BookPayloads;
import com.example.adventurebook.api.service.TestSupportService;
import com.example.adventurebook.support.BaseTest;
import com.example.adventurebook.support.SeedBooks;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import com.example.adventurebook.api.service.BookService;
import com.example.adventurebook.api.service.GameService;

/** POST /api/test/reset wipes global state, so this class never runs concurrently with any other test. */
@Isolated
@Tag("api")
@Tag("contract")
@Epic("Test support")
@Feature("Reset endpoint")
class TestSupportApiTest extends BaseTest {
    private final BookService books = new BookService();

    private final GameService games = new GameService();


    @Test
    @DisplayName("Reset (204, conforms to the contract) removes added books and all sessions, keeps the seed books")
    void resetRestoresSeedState() {
        BookResponseDto added = books.addOk(BookPayloads.minimal());
        GameStateResponseDto game = games.startOk(added.id());

        new TestSupportService(ApiClient.withContract())
                .reset().then().statusCode(204);

        books.get(added.id()).then().statusCode(404);
        games.get(game.sessionId()).then().statusCode(404);
        assertThat(books.listOk()).extracting(BookResponseDto::title).containsExactlyInAnyOrderElementsOf(SeedBooks.ALL_TITLES);
    }
}
