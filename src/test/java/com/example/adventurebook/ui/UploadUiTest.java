package com.example.adventurebook.ui;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.adventurebook.api.dto.request.BookRequestDto;
import com.example.adventurebook.api.payload.BookPayloads;
import com.example.adventurebook.api.service.BookService;
import com.example.adventurebook.support.InvalidBookCases.Case;
import com.example.adventurebook.support.UiTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Tag("ui")
@Epic("Add a book")
@Feature("Upload page")
class UploadUiTest extends UiTest {

    private final BookService books = new BookService();

    @Test
    @Story("Add a book")
    @DisplayName("A valid book is accepted and listed in the library")
    void validBookIsAdded() {
        BookRequestDto book = BookPayloads.threeStepStory();

        uploadSteps().open().submitValidBook(book).search(book.title());

        libraryAsserts().shouldShowBook(book.title());
    }

    @Test
    @Story("Validation")
    @DisplayName("Text that is not JSON is reported without calling the backend")
    void notJson() {
        uploadSteps().open().submitExpectingError("{ definitely not json");

        uploadAsserts().shouldShowParseError();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("com.example.adventurebook.support.InvalidBookCases#structuralRules")
    @Story("Validation")
    @DisplayName("A structurally invalid book is rejected and the broken rule is shown to the user")
    void invalidBookShowsTheBrokenRule(Case invalid) {
        uploadSteps().open().submitExpectingError(invalid.book());

        uploadAsserts().shouldShowValidationError(invalid.errorFragment());
        assertThat(books.listOk(invalid.book().title(), null))
                .as("rejected book must not be in the catalog").isEmpty();
    }
}
