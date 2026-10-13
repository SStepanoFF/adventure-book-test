package com.example.adventurebook.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.adventurebook.api.dto.response.ErrorResponseDto;
import com.example.adventurebook.api.dto.request.BookRequestDto;
import com.example.adventurebook.api.payload.BookPayloads;
import com.example.adventurebook.support.BaseTest;
import com.example.adventurebook.support.InvalidBookCases;
import com.example.adventurebook.support.InvalidBookCases.Case;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.example.adventurebook.api.service.BookService;

/**
 * Business rules that make a book invalid: none or several beginnings, no ending, invalid next section id,
 * a non-ending section without options. Each case breaks exactly one rule.
 */
@Tag("api")
@Epic("Add a book")
@Feature("Book validation")
class BookValidationApiTest extends BaseTest {
    private final BookService books = new BookService();


    @ParameterizedTest(name = "{0}")
    @MethodSource("com.example.adventurebook.support.InvalidBookCases#structuralRules")
    @Story("Structural rules")
    @DisplayName("Invalid book is rejected with 400 and a message naming the broken rule")
    void invalidBookIsRejected(Case invalid) {
        ErrorResponseDto error = books.add(invalid.book()).then().statusCode(400).extract().as(ErrorResponseDto.class);

        assertThat(error.detailsOrEmpty())
                .as("error details for '%s'", invalid.name())
                .anyMatch(detail -> detail.toLowerCase().contains(invalid.errorFragment().toLowerCase()));
        assertThat(books.listOk(invalid.book().title(), null))
                .as("rejected book must not appear in the catalog")
                .isEmpty();
    }

    @Test
    @Story("Structural rules")
    @DisplayName("A rejected book can be corrected and then accepted")
    void correctedBookIsAccepted() {
        BookRequestDto broken = BookPayloads.noBeginning();
        books.add(broken).then().statusCode(400);

        books.addOk(BookPayloads.minimal());
    }

    @Test
    @Story("Field validation")
    @DisplayName("Blank title -> 400 naming the field")
    void blankTitle() {
        ErrorResponseDto error = books.add(BookPayloads.blankTitle()).then().statusCode(400).extract().as(ErrorResponseDto.class);
        assertThat(error.detailsOrEmpty()).anyMatch(d -> d.startsWith("title"));
    }

    @Test
    @Story("Field validation")
    @DisplayName("A book without sections -> 400")
    void noSections() {
        ErrorResponseDto error = books.add(BookPayloads.noSections()).then().statusCode(400).extract().as(ErrorResponseDto.class);
        assertThat(error.message()).isNotBlank();
    }

    // ---------------------------------------------------------------------------------------------
    // Defect candidates: found by reading the backend code, to be confirmed by running the suite.
    // They assert the SPECIFIED behaviour, so a failure here is a finding, not a flaky test.
    // Exclude with -DexcludedGroups=defect-candidate
    // ---------------------------------------------------------------------------------------------

    @Test
    @Tag("defect-candidate")
    @Story("Structural rules")
    @DisplayName("[candidate] Non-ending section with \"options\": [] is rejected")
    void nonEndingWithEmptyOptionsListIsRejected() {
        // BookValidator only checks options == null, so an empty list seems to slip through
        books.add(BookPayloads.nonEndingWithEmptyOptions()).then().statusCode(400);
    }

    @Test
    @Tag("defect-candidate")
    @Story("Robustness")
    @DisplayName("[candidate] Malformed JSON -> 400, not 500")
    void malformedJsonIsABadRequest() {
        // HttpMessageNotReadableException has no dedicated handler, so it ends in the generic 500 handler
        books.addRaw("{ this is not json").then().statusCode(400);
    }

    @Test
    @Tag("defect-candidate")
    @Story("Robustness")
    @DisplayName("[candidate] Unknown section type -> 400, not 500")
    void unknownSectionTypeIsABadRequest() {
        String json = """
                {"title":"x","author":"y","sections":[{"id":1,"text":"t","type":"MIDDLE","options":[]}]}""";
        books.addRaw(json).then().statusCode(400);
    }
}
