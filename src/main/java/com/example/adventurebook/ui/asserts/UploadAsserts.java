package com.example.adventurebook.ui.asserts;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.example.adventurebook.ui.pages.UploadPage;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;

/** Checks on the "Add a book" page. */
public class UploadAsserts {

    private final UploadPage upload;

    public UploadAsserts(Page page) {
        this.upload = new UploadPage(page);
    }

    @Step("Upload page is loaded")
    public UploadAsserts shouldBeLoaded() {
        assertThat(upload.getHeading()).isVisible();
        return this;
    }

    @Step("Upload page shows the JSON parse error")
    public UploadAsserts shouldShowParseError() {
        assertThat(upload.getParseError()).isVisible();
        return this;
    }

    @Step("Upload page shows the validation error '{fragment}'")
    public UploadAsserts shouldShowValidationError(String fragment) {
        assertThat(upload.getValidationErrors()).isVisible();
        assertThat(upload.getValidationErrors()).containsText(fragment);
        return this;
    }
}
