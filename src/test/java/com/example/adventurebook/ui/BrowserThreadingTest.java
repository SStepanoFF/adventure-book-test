package com.example.adventurebook.ui;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.adventurebook.ui.BrowserManager;
import com.example.adventurebook.ui.DeviceProfile;
import com.example.adventurebook.ui.UiSession;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Verifies the threading model of the framework itself (needs a browser, NOT the application): browsers are open
 * at the same moment on several threads, each thread has its own, they do not mix, and every thread closes its
 * own browser. This is the safety net for running the suite in parallel.
 */
@Tag("ui")
@Tag("parallel-safety")
@Epic("Framework")
@Feature("Parallel execution")
class BrowserThreadingTest {

    @Test
    @DisplayName("Every thread gets its own browser, isolated state, and closes it on its own thread")
    void eachThreadOwnsItsBrowser() throws Exception {
        int threads = 3;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CyclicBarrier allBrowsersOpen = new CyclicBarrier(threads);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                int number = i;
                futures.add(pool.submit(() -> {
                    UiSession session = BrowserManager.open(DeviceProfile.DESKTOP);
                    try {
                        session.getPage().setContent("<title>page-" + number + "</title>");
                        allBrowsersOpen.await(90, TimeUnit.SECONDS); // all browsers exist at the same time
                        return session.getPage().title() + "|"
                                + System.identityHashCode(session.getPage().context().browser());
                    } finally {
                        BrowserManager.closeThreadBrowser(); // same thread that created it
                    }
                }));
            }

            List<String> titles = new ArrayList<>();
            List<String> browsers = new ArrayList<>();
            for (Future<String> future : futures) {
                String[] parts = future.get(120, TimeUnit.SECONDS).split("\\|");
                titles.add(parts[0]);
                browsers.add(parts[1]);
            }

            assertThat(titles).containsExactlyInAnyOrder("page-0", "page-1", "page-2");
            assertThat(browsers).as("one distinct browser per thread").doesNotHaveDuplicates();
            assertThat(BrowserManager.currentIfOpen()).as("nothing leaked into the test thread").isEmpty();
        } finally {
            pool.shutdownNow();
        }
    }
}
