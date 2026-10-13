package com.example.adventurebook.ui.steps;

import com.example.adventurebook.api.Json;
import com.example.adventurebook.api.dto.request.BookRequestDto;
import com.example.adventurebook.ui.BaseSteps;
import com.example.adventurebook.ui.pages.UploadPage;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import java.util.List;

/** What a user does on the "Add a book" page. {@code @Step} methods become steps of the Allure report. */
public class UploadSteps extends BaseSteps<UploadPage> {

    public UploadSteps(Page page) {
        super(page, new UploadPage(page));
    }

    @Step("Open the 'Add a book' page")
    public UploadSteps open() {
        navigate();
        return this;
    }

    @Step("Enter the book JSON")
    public UploadSteps enterJson(String json) {
        pageObject.getJsonInput().fill(json);
        return this;
    }

    /** Submits a book that is expected to be accepted: the app navigates back to the library. */
    @Step("Submit a valid book")
    public LibrarySteps submitValidBook(BookRequestDto book) {
        pageObject.getJsonInput().fill(Json.write(book));
        pageObject.getSubmitButton().click();
        LibrarySteps library = new LibrarySteps(page);
        library.waitUntilLoaded();
        return library;
    }

    /** Submits raw text and stays on the page (invalid JSON / rejected book). */
    @Step("Submit an invalid book")
    public UploadSteps submitExpectingError(String json) {
        pageObject.getJsonInput().fill(json);
        pageObject.getSubmitButton().click();
        return this;
    }

    public UploadSteps submitExpectingError(BookRequestDto book) {
        return submitExpectingError(Json.write(book));
    }

    @Step("Back to the library")
    public UploadSteps backToLibrary() {
        pageObject.getBackLink().click();
        return this;
    }

    public List<String> validationErrorMessages() {
        return List.of(text(pageObject.getValidationErrors()).split("\\R"));
    }
}
