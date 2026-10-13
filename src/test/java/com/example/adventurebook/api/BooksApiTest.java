package com.example.adventurebook.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.adventurebook.api.dto.response.ErrorResponseDto;
import com.example.adventurebook.api.dto.request.BookRequestDto;
import com.example.adventurebook.api.dto.response.BookResponseDto;
import com.example.adventurebook.api.payload.BookPayloads;
import com.example.adventurebook.support.BaseTest;
import com.example.adventurebook.support.SeedBooks;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.example.adventurebook.api.service.BookService;

@Tag("api")
@Epic("Library")
@Feature("Books API")
class BooksApiTest extends BaseTest {
    private final BookService books = new BookService();


    private List<String> titles(List<BookResponseDto> list) {
        return list.stream().map(BookResponseDto::title).toList();
    }

    @Test
    @Story("List books")
    @DisplayName("GET /api/books returns the seed catalog")
    void listContainsSeedBooks() {
        assertThat(titles(books.listOk())).containsAll(SeedBooks.ALL_TITLES);
    }

    @Test
    @Story("Search")
    @DisplayName("Search matches the title, case-insensitively")
    void searchByTitle() {
        assertThat(titles(books.listOk("SUNKEN", null)))
                .containsExactly(SeedBooks.SUNKEN_TEMPLE);
    }

    @Test
    @Story("Search")
    @DisplayName("Search matches the author")
    void searchByAuthor() {
        assertThat(titles(books.listOk("okafor", null)))
                .containsExactly(SeedBooks.WASTELAND_CONVOY);
    }

    @Test
    @Story("Filter")
    @DisplayName("Tag filter matches exactly and ignores case")
    void filterByTag() {
        assertThat(titles(books.listOk(null, "WINTER")))
                .containsExactly(SeedBooks.FROST_HOLLOW);
    }

    @Test
    @Story("Filter")
    @DisplayName("A partial tag does not match")
    void partialTagDoesNotMatch() {
        assertThat(books.listOk(null, "wint")).isEmpty();
    }

    @Test
    @Story("Filter")
    @DisplayName("Search and tag filter are combined (AND)")
    void searchAndTagAreCombined() {
        assertThat(titles(books.listOk("frost", "survival")))
                .containsExactly(SeedBooks.FROST_HOLLOW);
        assertThat(books.listOk("prisoner", "winter")).isEmpty();
    }

    @Test
    @Story("Search")
    @DisplayName("No match returns 200 and an empty list, not an error")
    void noMatchReturnsEmptyList() {
        assertThat(books.listOk("zzz-no-such-book-zzz", null)).isEmpty();
    }

    @Test
    @Story("Search")
    @DisplayName("A blank search is ignored")
    void blankSearchIsIgnored() {
        assertThat(titles(books.listOk("   ", null))).containsAll(SeedBooks.ALL_TITLES);
    }

    @Test
    @Story("Get a book")
    @DisplayName("GET /api/books/{id} returns the summary of an existing book")
    void getBook() {
        BookResponseDto created = books.addOk(BookPayloads.minimal());

        BookResponseDto fetched = books.getOk(created.id());

        assertThat(fetched).isEqualTo(created);
    }

    @Test
    @Story("Get a book")
    @DisplayName("Unknown book id -> 404 with the standard error body")
    void getUnknownBook() {
        ErrorResponseDto error = books.get(999_999).then().statusCode(404).extract().as(ErrorResponseDto.class);
        assertThat(error.message()).isNotBlank();
    }

    @Test
    @Story("Get a book")
    @DisplayName("Non-numeric book id -> 400, not a server error")
    void getBookWithNonNumericId() {
        books.get("not-a-number").then().statusCode(400);
    }

    @Test
    @Story("Add a book")
    @DisplayName("A valid book is accepted (201) and shows up in the catalog")
    void addValidBook() {
        BookRequestDto input = BookPayloads.threeStepStory();

        BookResponseDto created = books.addOk(input);

        assertThat(created.id()).isPositive();
        assertThat(created.title()).isEqualTo(input.title());
        assertThat(created.author()).isEqualTo(input.author());
        assertThat(created.tags()).isEqualTo(input.tags());
        assertThat(created.sectionCount()).isEqualTo(4);
        assertThat(titles(books.listOk(input.title(), null))).containsExactly(input.title());
    }
}
