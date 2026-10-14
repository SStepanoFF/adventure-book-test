package com.example.adventurebook.api;

import com.example.adventurebook.api.dto.response.BookResponseDto;
import com.example.adventurebook.api.dto.response.ErrorResponseDto;
import com.example.adventurebook.api.service.BookService;
import com.example.adventurebook.api.service.GameService;
import com.example.adventurebook.data.Unique;
import com.example.adventurebook.support.BaseApiTest;
import com.example.adventurebook.support.BookWalker;
import com.example.adventurebook.support.SeedBooks;
import com.example.adventurebook.support.TestResources;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The books that ship with the application are real content, not synthetic test data: they must pass the same
 * validation as an uploaded book, be completable, and the fixture the app ships for an invalid book must be rejected.
 */
@Tag("api")
@Epic("Library")
@Feature("Built-in books")
class BuiltInBooksApiApiTest extends BaseApiTest {

    private final BookService bookService = new BookService();
    private final GameService gameService = new GameService();

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"01-the-prisoner", "02-the-sunken-temple", "03-the-wasteland-convoy", "04-frost-hollow"})
    @Story("Add a book")
    @DisplayName("A built-in book passes the same validation when it is uploaded (under a unique title)")
    void builtInBookIsAcceptedOnUpload(String file) throws IOException {
        ObjectNode book = (ObjectNode) Json.mapper()
                .readTree(TestResources.read("books/" + file + ".json"));
        book.put("title", Unique.name(book.get("title").asText())); // the catalog does not forbid duplicates, we avoid them

        bookService.addRaw(book.toString()).then().statusCode(201);
    }

    @Test
    @Story("Add a book")
    @DisplayName("The invalid-book fixture shipped with the app is rejected, naming the broken rule")
    void invalidFixtureIsRejected() {
        ErrorResponseDto error = bookService.addRaw(TestResources.read("invalid-books/missing-begin.json"))
                .then().statusCode(400).extract().as(ErrorResponseDto.class);

        assertThat(error.detailsOrEmpty()).anyMatch(detail -> detail.contains("BEGIN"));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {SeedBooks.PRISONER, SeedBooks.SUNKEN_TEMPLE, SeedBooks.WASTELAND_CONVOY,
            SeedBooks.FROST_HOLLOW})
    @Story("Play")
    @DisplayName("Every built-in book can be played from the beginning to a real ending")
    void everyBuiltInBookCanBeCompleted(String title) {
        BookResponseDto book = bookService.listOk(title, null).stream()
                .filter(candidate -> candidate.title().equals(title)).findFirst().orElseThrow();

        List<Integer> path = new BookWalker(gameService).findPathToEnding(book.id());

        assertThat(path).as("option indexes of a walk that reaches an ending").isNotEmpty();
    }
}
