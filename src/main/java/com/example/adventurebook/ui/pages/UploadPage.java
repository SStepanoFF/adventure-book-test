package com.example.adventurebook.ui.pages;

import com.example.adventurebook.ui.BasePage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import lombok.Getter;

/** "Add a book" page ("/upload"): locators only. Actions: {@code UploadSteps}; checks: {@code UploadAsserts}. */
@Getter
public class UploadPage extends BasePage {

    private final Locator heading = page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Add a book"));
    private final Locator jsonInput = page.getByTestId("book-json-input");
    private final Locator submitButton = page.getByTestId("submit-book-button");
    private final Locator parseError = page.getByTestId("book-json-parse-error");
    private final Locator validationErrors = page.getByTestId("book-validation-errors");
    private final Locator backLink = page.getByTestId("back-to-library-link");

    public UploadPage(Page page) {
        super(page);
    }

    @Override
    public String path() {
        return "/upload";
    }

    @Override
    public Locator readyMarker() {
        return heading;
    }
}
