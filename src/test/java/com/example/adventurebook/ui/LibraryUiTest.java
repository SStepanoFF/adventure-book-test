package com.example.adventurebook.ui;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.adventurebook.api.dto.request.BookRequestDto;
import com.example.adventurebook.api.payload.BookPayloads;
import com.example.adventurebook.api.service.BookService;
import com.example.adventurebook.data.Unique;
import com.example.adventurebook.support.SeedBooks;
import com.example.adventurebook.support.UiTest;
import com.example.adventurebook.ui.steps.LibrarySteps;
import com.example.adventurebook.ui.steps.LibrarySteps.BookCardInfo;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ui")
@Epic("Library")
@Feature("Library page")
class LibraryUiTest extends UiTest {

    private final BookService books = new BookService();

    @Test
    @Story("List books")
    @DisplayName("Every seed book is shown as a card with author, tags and section count")
    void showsSeedBooks() {
        LibrarySteps library = librarySteps().open();

        SeedBooks.ALL_TITLES.forEach(libraryAsserts()::shouldShowBook);

        BookCardInfo prisoner = library.bookInfo(SeedBooks.PRISONER);
        assertThat(prisoner.author()).isEqualTo("by Daniel El Fuego");
        assertThat(prisoner.tags()).containsExactly("escape", "short");
        assertThat(prisoner.sectionCount()).isEqualTo("7 sections");
    }

    @Test
    @Story("Search")
    @DisplayName("Searching by title narrows the list")
    void searchByTitle() {
        librarySteps().open().search("sunken");

        libraryAsserts().shouldShowBook(SeedBooks.SUNKEN_TEMPLE).shouldNotShowBook(SeedBooks.PRISONER);
    }

    @Test
    @Story("Search")
    @DisplayName("Searching by author finds the book")
    void searchByAuthor() {
        librarySteps().open().search("Okafor");

        libraryAsserts().shouldShowBook(SeedBooks.WASTELAND_CONVOY)
                .shouldNotShowBook(SeedBooks.PRISONER)
                .shouldNotShowBook(SeedBooks.SUNKEN_TEMPLE)
                .shouldNotShowBook(SeedBooks.FROST_HOLLOW);
    }

    @Test
    @Story("Filter")
    @DisplayName("Filtering by tag shows only the matching books")
    void filterByTag() {
        librarySteps().open().filterByTag("winter");

        libraryAsserts().shouldShowBook(SeedBooks.FROST_HOLLOW)
                .shouldNotShowBook(SeedBooks.PRISONER)
                .shouldNotShowBook(SeedBooks.SUNKEN_TEMPLE)
                .shouldNotShowBook(SeedBooks.WASTELAND_CONVOY);
    }

    @Test
    @Story("Search")
    @DisplayName("A search without matches shows the empty state")
    void noResults() {
        librarySteps().open().search(Unique.name("no-such-book"));

        libraryAsserts().shouldShowNoResultsMessage();
    }

    @Test
    @Story("List books")
    @DisplayName("A book added through the API appears in the library (hybrid precondition)")
    void bookAddedThroughApiIsListed() {
        BookRequestDto book = BookPayloads.minimal();
        books.addOk(book);

        librarySteps().open().search(book.title());

        libraryAsserts().shouldShowBook(book.title()).shouldShowBookCount(1);
    }

    @Test
    @Story("Navigation")
    @DisplayName("'Add a book' opens the upload page")
    void navigatesToUpload() {
        var upload = librarySteps().open().openUploadPage();

        assertThat(upload.currentPath()).isEqualTo("/upload");
        upload.backToLibrary();
        libraryAsserts().shouldBeLoaded();
    }

    @Test
    @Tag("defect-candidate")
    @Story("Accessibility")
    @DisplayName("[candidate] The play button of a card has an accessible name")
    void playButtonHasAccessibleName() {
        List<String> titles = librarySteps().open().visibleBookTitles();
        assertThat(titles).isNotEmpty();

        // the only content of the button is an aria-hidden arrow -> a screen reader announces just "button"
        Locator named = libraryPage().bookCard(titles.get(0)).getLocator()
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(Pattern.compile(".+")));

        assertThat(named.count()).as("play button with an accessible name").isEqualTo(1);
    }
}
