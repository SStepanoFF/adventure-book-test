package com.example.adventurebook.ui;

import com.microsoft.playwright.Locator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A reusable widget (card, header, dialog) described relative to its own root locator:
 * children are {@code locator.getByRole(...)}, {@code locator.getByTestId(...)}, {@code locator.locator("h2")}.
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseComponent {

    /** Root element of the widget; every child locator starts from it. */
    @Getter
    protected final Locator locator;

}
