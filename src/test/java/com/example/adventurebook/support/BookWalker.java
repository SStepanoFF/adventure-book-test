package com.example.adventurebook.support;

import com.example.adventurebook.api.dto.response.GameStateResponseDto;
import com.example.adventurebook.api.dto.type.GameStatus;
import com.example.adventurebook.api.service.GameService;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Explores a REAL book through the API (random walks with a fixed seed, so the result is repeatable) and returns
 * the option indexes of a walk that reaches an ending. Used to prove that shipped content is completable, and to
 * replay such a walk in the browser.
 */
public final class BookWalker {

    private static final int MAX_ATTEMPTS = 2000;
    private static final int MAX_STEPS = 200;

    private final GameService games;

    public BookWalker(GameService games) {
        this.games = games;
    }

    public List<Integer> findPathToEnding(long bookId) {
        Random random = new Random(42);
        GameStateResponseDto first = games.startOk(bookId);
        String sessionId = first.sessionId();
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            GameStateResponseDto state = attempt == 0 ? first : games.restartOk(sessionId);
            List<Integer> path = new ArrayList<>();
            for (int step = 0; step < MAX_STEPS && !state.gameOver(); step++) {
                int options = state.currentSection().options().size();
                if (options == 0) {
                    break; // dead end that is not an ending
                }
                int index = random.nextInt(options);
                state = games.chooseOk(sessionId, index);
                path.add(index);
            }
            if (state.status() == GameStatus.FINISHED) {
                return path;
            }
        }
        throw new AssertionError("No path to an ending found for book " + bookId + " in " + MAX_ATTEMPTS
                + " random walks - the book may not be completable");
    }
}
