package com.example.adventurebook;

import com.example.adventurebook.api.service.TestSupportService;
import com.example.adventurebook.config.TafConfig;
import com.example.adventurebook.junit.SuiteHook;
import lombok.extern.slf4j.Slf4j;

/**
 * Registered through META-INF/services. Resets the backend ONCE before the run, so the suite starts from the
 * seed catalog; tests themselves are self-contained (unique data) and never need a reset between them.
 */
@Slf4j
public class AdventureBookSuiteHook implements SuiteHook {

    @Override
    public void beforeSuite() {
        TafConfig config = TafConfig.get();
        if (!config.resetOnStart()) {
            log.info("test.reset-on-start=false: backend state is left untouched");
            return;
        }
        try {
            int status = new TestSupportService().reset().statusCode();
            if (status != 204) {
                throw new IllegalStateException("POST /api/test/reset returned " + status
                        + " (set -Dtest.reset-on-start=false if the endpoint is disabled on this environment)");
            }
        } catch (RuntimeException e) {
            throw new IllegalStateException("Backend is not ready at " + config.apiBaseUrl()
                    + " - start it (cd backend && ./mvnw spring-boot:run). Cause: " + e.getMessage(), e);
        }
    }
}
