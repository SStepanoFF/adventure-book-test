package com.example.adventurebook.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.example.adventurebook.api.dto.response.GameStateResponseDto;
import com.example.adventurebook.api.payload.BookPayloads;
import com.example.adventurebook.api.service.BookService;
import com.example.adventurebook.api.service.GameService;
import com.example.adventurebook.support.UiTest;
import com.example.adventurebook.ui.asserts.GameAsserts;
import com.example.adventurebook.ui.pages.GamePage;
import com.example.adventurebook.ui.steps.GameSteps;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** The application must be responsive and playable on a tablet (both orientations, touch input). */
@Tag("ui")
@Tag("responsive")
@Epic("Responsive")
@Feature("Tablet")
class ResponsiveUiTest extends UiTest {

    private static final double MIN_TAP_TARGET_PX = 24; // WCAG 2.2 AA; 44 is the stricter mobile recommendation

    private final BookService books = new BookService();
    private final GameService games = new GameService();

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = DeviceProfile.class, names = {"TABLET_LANDSCAPE", "TABLET_PORTRAIT"})
    @Story("Library")
    @DisplayName("Library fits the screen without horizontal scrolling")
    void libraryFitsTheScreen(DeviceProfile profile) {
        openBrowser(profile);

        librarySteps().open();

        LayoutAssert.noHorizontalScroll(page());
        libraryAsserts().shouldShowBrandLink();
        LayoutAssert.insideViewportWidth(page(), libraryPage().getGrid());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = DeviceProfile.class, names = {"TABLET_LANDSCAPE", "TABLET_PORTRAIT"})
    @Story("Upload")
    @DisplayName("Upload page fits the screen")
    void uploadFitsTheScreen(DeviceProfile profile) {
        openBrowser(profile);

        uploadSteps().open();

        LayoutAssert.noHorizontalScroll(page());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = DeviceProfile.class, names = {"TABLET_LANDSCAPE", "TABLET_PORTRAIT"})
    @Story("Play")
    @DisplayName("A whole adventure can be played with touch input")
    void adventureIsPlayableOnTablet(DeviceProfile profile) {
        GameStateResponseDto session = games.startOk(books.addOk(BookPayloads.threeStepStory()).id()); // API precondition
        openBrowser(profile);

        GameSteps game = gameSteps(session.sessionId()).open();
        GameAsserts check = gameAsserts();
        GamePage locators = gamePage(session.sessionId());

        LayoutAssert.noHorizontalScroll(page());
        assertThat(locators.getHeader()).isVisible();
        LayoutAssert.insideViewportWidth(page(), locators.option(0));
        LayoutAssert.minTapTarget(locators.option(0), MIN_TAP_TARGET_PX);
        LayoutAssert.minTapTarget(locators.getSaveButton(), MIN_TAP_TARGET_PX);

        game.tapOption(0);
        check.shouldShowSectionContaining(BookPayloads.SECTION_2);
        game.tapOption(0);
        check.shouldShowSectionContaining(BookPayloads.SECTION_3);
        game.tapOption(0);
        check.shouldBeFinished();
        LayoutAssert.noHorizontalScroll(page());
    }
}
