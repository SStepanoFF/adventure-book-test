package com.example.adventurebook.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.adventurebook.api.dto.request.BookRequestDto;
import com.example.adventurebook.api.dto.response.BookResponseDto;
import com.example.adventurebook.api.dto.response.GameStateResponseDto;
import com.example.adventurebook.api.payload.BookPayloads;
import com.example.adventurebook.support.BaseTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.example.adventurebook.api.service.BookService;
import com.example.adventurebook.api.service.GameService;

/**
 * A parallel test run hits the backend concurrently, so the backend has to survive that. These tests check it
 * directly: no lost updates, no duplicated ids, sessions do not leak into each other.
 */
@Tag("api")
@Tag("concurrency")
@Epic("Robustness")
@Feature("Concurrent access")
class ConcurrencyApiTest extends BaseTest {
    private final BookService books = new BookService();

    private final GameService games = new GameService();


    private static final int THREADS = 8;
    private static final int COUNT = 24;

    @Test
    @DisplayName("Concurrent uploads get unique ids and none is lost")
    void concurrentUploads() throws Exception {
        List<BookRequestDto> inputs = IntStream.range(0, COUNT).mapToObj(i -> BookPayloads.minimal()).toList();

        List<BookResponseDto> created = inParallel(inputs, books::addOk);

        assertThat(created).extracting(BookResponseDto::id).doesNotHaveDuplicates();
        assertThat(books.listOk()).extracting(BookResponseDto::title)
                .containsAll(inputs.stream().map(BookRequestDto::title).toList());
    }

    @Test
    @DisplayName("Concurrent sessions of one book stay independent")
    void concurrentSessionsAreIndependent() throws Exception {
        BookResponseDto book = books.addOk(BookPayloads.withRiskyChoice(4));
        List<Integer> numbers = IntStream.range(0, COUNT).boxed().toList();

        List<GameStateResponseDto> started = inParallel(numbers, i -> games.startOk(book.id()));
        assertThat(started).extracting(GameStateResponseDto::sessionId).doesNotHaveDuplicates();

        List<GameStateResponseDto> afterHit = inParallel(started, s -> games.chooseOk(s.sessionId(), 1));
        assertThat(afterHit).allSatisfy(state -> {
            assertThat(state.health()).isEqualTo(6);
            assertThat(state.currentSection().id()).isEqualTo(2);
        });
    }

    private static <I, O> List<O> inParallel(List<I> inputs, Function<I, O> call) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<O>> futures = new ArrayList<>();
            for (I item : inputs) {
                futures.add(pool.submit(() -> call.apply(item)));
            }
            List<O> results = new ArrayList<>();
            for (Future<O> future : futures) {
                results.add(future.get(60, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}
