package com.example.adventurebook.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Properties;

/**
 * Typed access to configuration. The environment is chosen by the run parameter {@code -Denv=<name>}
 * (or the TAF_ENV variable) and loaded from {@code env/<name>.properties}; default is {@code local}.
 *
 * <p>Resolution order for every key: {@code -Dkey} > environment variable (api.base-url -> API_BASE_URL)
 * > {@code env/<name>.properties} > {@code env/default.properties}.
 */
public final class TafConfig {

    private static final TafConfig INSTANCE = load();

    private final String envName;
    private final Properties defaults;
    private final Properties environment;

    private TafConfig(String envName, Properties defaults, Properties environment) {
        this.envName = envName;
        this.defaults = defaults;
        this.environment = environment;
    }

    public static TafConfig get() {
        return INSTANCE;
    }

    // ---------- typed accessors ----------

    public String envName() {
        return envName;
    }

    public String apiBaseUrl() {
        return require("api.base-url");
    }

    public String uiBaseUrl() {
        return require("ui.base-url");
    }

    public String openApiSpec() {
        return require("openapi.spec");
    }

    public String browser() {
        return get("ui.browser", "chromium").toLowerCase();
    }

    /** REST Assured's own request/response log: all (default) | failures (only when a then() check fails) | none. */
    public String apiLog() {
        return get("api.log", "all").toLowerCase();
    }

    public boolean headless() {
        return Boolean.parseBoolean(get("ui.headless", "true"));
    }

    /**
     * run   - browsers live until the end of the run (closed once, by the thread that ends the run);
     * class - the browser of a thread is closed right after the test class, ON THE SAME THREAD that created it;
     * auto  - class when JUnit parallel execution is enabled, run otherwise (default).
     */
    public String browserScope() {
        return get("ui.browser-scope", "auto").toLowerCase();
    }

    /** Install the selected browser on first use when it is missing (turn off where browsers are pre-installed). */
    public boolean autoInstallBrowser() {
        return Boolean.parseBoolean(get("ui.auto-install-browser", "true"));
    }

    public boolean video() {
        return Boolean.parseBoolean(get("ui.video", "false"));
    }

    public double slowMoMs() {
        return Double.parseDouble(get("ui.slow-mo-ms", "0"));
    }

    public Duration uiTimeout() {
        return Duration.ofMillis(Long.parseLong(get("ui.timeout-ms", "10000")));
    }

    public boolean resetOnStart() {
        return Boolean.parseBoolean(get("test.reset-on-start", "true"));
    }

    /** Fail the run when an endpoint lacks a happy-path or contract call. Only for FULL runs (not -Dgroups=...). */
    public boolean enforceApiCoverage() {
        return Boolean.parseBoolean(get("coverage.enforce", "false"));
    }

    public double pageReadyBudgetMs() {
        return Double.parseDouble(get("perf.page-ready-budget-ms", "500"));
    }

    public int perfRuns() {
        return Integer.parseInt(get("perf.runs", "5"));
    }

    // ---------- generic ----------

    public String require(String key) {
        String value = get(key, null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing configuration value '" + key + "' for env '" + envName + "'");
        }
        return value;
    }

    public String get(String key, String fallback) {
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(key.toUpperCase().replaceAll("[^A-Z0-9]", "_"));
        }
        if (value == null) {
            value = environment.getProperty(key);
        }
        if (value == null) {
            value = defaults.getProperty(key);
        }
        return value != null ? value : fallback;
    }

    @Override
    public String toString() {
        return "env=" + envName + ", api=" + apiBaseUrl() + ", ui=" + uiBaseUrl()
                + ", browser=" + browser() + ", headless=" + headless();
    }

    // ---------- loading ----------

    private static TafConfig load() {
        String env = System.getProperty("env");
        if (env == null || env.isBlank()) {
            env = System.getenv("TAF_ENV");
        }
        if (env == null || env.isBlank()) {
            env = "local";
        }
        Properties defaults = read("env/default.properties", true);
        Properties specific = read("env/" + env + ".properties", false);
        if (specific == null) {
            throw new IllegalArgumentException("Unknown environment '" + env
                    + "': file env/" + env + ".properties not found on the classpath");
        }
        return new TafConfig(env, defaults, specific);
    }

    private static Properties read(String resource, boolean mustExist) {
        try (InputStream in = TafConfig.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                if (mustExist) {
                    throw new IllegalStateException("Missing classpath resource " + resource);
                }
                return null;
            }
            Properties props = new Properties();
            props.load(in);
            return props;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
