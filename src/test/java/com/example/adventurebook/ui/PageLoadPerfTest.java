package com.example.adventurebook.ui;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.adventurebook.api.dto.response.GameStateResponseDto;
import com.example.adventurebook.api.payload.BookPayloads;
import com.example.adventurebook.api.service.BookService;
import com.example.adventurebook.api.service.GameService;
import com.example.adventurebook.config.TafConfig;
import com.example.adventurebook.perf.PerformanceAssert;
import com.example.adventurebook.support.UiTest;
import com.example.adventurebook.ui.steps.GameSteps;
import com.example.adventurebook.ui.steps.LibrarySteps;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * Page loading must take 500 ms at most (config: perf.page-ready-budget-ms). "Loaded" means the DATA is rendered
 * (first card / section text), measured as the median of perf.runs warm navigations after one warm-up.
 * Meaningful only against a production-like build; the Angular dev server is slower than a built app.
 */
// timing is only meaningful when nothing else loads the machine/app: never runs concurrently with other tests
@Isolated
@Tag("perf")
@Epic("Performance")
@Feature("Page loading")
class PageLoadPerfTest extends UiTest {

    private final BookService books = new BookService();
    private final GameService games = new GameService();
    private final TafConfig config = TafConfig.get();

    @Test
    @DisplayName("Library: data visible within the budget")
    void libraryLoadsWithinBudget() {
        LibrarySteps library = librarySteps();
        library.openAndMeasureContentMs(); // warm-up (caches, JIT)

        double median = PerformanceAssert.median(config.perfRuns(), library::openAndMeasureContentMs);

        PerformanceAssert.assertWithin(config.pageReadyBudgetMs(), median, "Library page");
    }

    @Test
    @DisplayName("Game page: section text visible within the budget")
    void gamePageLoadsWithinBudget() {
        GameStateResponseDto session = games.startOk(books.addOk(BookPayloads.threeStepStory()).id());
        GameSteps game = gameSteps(session.sessionId());
        game.openAndMeasureContentMs(); // warm-up

        double median = PerformanceAssert.median(config.perfRuns(), game::openAndMeasureContentMs);

        PerformanceAssert.assertWithin(config.pageReadyBudgetMs(), median, "Game page");
    }

    @Test
    @DisplayName("API: listing books answers within the budget")
    void listBooksApiWithinBudget() {
        books.list(); // warm-up
        double median = PerformanceAssert.median(config.perfRuns(), () -> (double) books.list().time());

        assertThat(median).as("GET /api/books median").isLessThanOrEqualTo(config.pageReadyBudgetMs());
    }
}
