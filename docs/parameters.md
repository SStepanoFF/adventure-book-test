# All run parameters

## How to pass a value (highest priority first)

1. A Maven parameter: `mvn test -Dui.headless=false`
2. An environment variable: the key in upper case, every non-alphanumeric character becomes `_`
   (`api.base-url` -> `API_BASE_URL`). The environment itself can also be set with the `TAF_ENV` variable.
3. The environment file `src/main/resources/env/<env>.properties` (chosen with `-Denv=<name>`, default `local`).
4. `src/main/resources/env/default.properties`.

## A. Which tests to run (Maven/JUnit, not our own parameters)

| Parameter | Example | What it does |
|---|---|---|
| `-Dgroups` | `-Dgroups=arch`, `-Dgroups=api,ui`, `-Dgroups="ui & !responsive"` | Runs only tests that carry the given **tag** (`@Tag("...")` on a class or method). Comma = "or", `&` = "and", `!` = "not". |
| `-DexcludedGroups` | `-DexcludedGroups=perf,defect-candidate` | The opposite: skips tests that carry these tags. |
| `-Dtest` | `-Dtest=GameApiTest`, `-Dtest=GameApiTest#startGame` | One class or one method. |
| (nothing) | `mvn test` | Runs **everything**, including `perf`, `defect-candidate` and `arch`. |

Tags used in the project:

| Tag | Contains |
|---|---|
| `api` | functional API tests |
| `contract` | validation of responses against `openapi.yaml` |
| `ui` | tests that drive the browser |
| `responsive` | tablet, both orientations, touch |
| `e2e` | hybrid API + UI scenarios |
| `perf` | timing measurements (need a production build) |
| `arch` | architecture checks (ArchUnit); the application is not needed |
| `parallel-safety` | checks the browser threading model; needs a browser only, not the application |
| `concurrency` | concurrent calls to the backend |
| `defect-candidate` | suspected defects; a failure is a finding, not a flaky test |

## B. Project parameters

| Key (`-D...`) | Default | Meaning |
|---|---|---|
| `env` | `local` | Which environment: file `env/<env>.properties` (`local`, `ci` and a `staging` placeholder exist). |
| `api.base-url` | `http://localhost:8080` | Backend address. |
| `ui.base-url` | `http://localhost:4200` | Frontend address. |
| `openapi.spec` | `openapi/openapi.yaml` | Specification used by contract checks: a classpath resource, a file path or a URL. In `ci` it is `../openapi.yaml` (the real file of the repository). |
| `api.log` | `all` | REST Assured's standard log: `all`, `failures` (only when a `then()` check fails) or `none`. |
| `ui.browser` | `chromium` | `chromium`, `firefox`, `webkit`. |
| `ui.headless` | `true` | `false` = show the browser window. |
| `ui.slow-mo-ms` | `0` | Delay between browser actions (for debugging). |
| `ui.timeout-ms` | `10000` | How long Playwright waits for an element or an action. |
| `ui.video` | `false` | Record video into `target/videos` (it is not attached to the report). |
| `ui.auto-install-browser` | `true` | Download the selected browser if it is missing. |
| `ui.browser-scope` | `auto` | When to close the browser: `run` (at the end of the run), `class` (after each class), `auto` (`class` when parallel execution is on). |
| `test.reset-on-start` | `true` | Call `POST /api/test/reset` once before the run. Use `false` on environments where it is not allowed. |
| `perf.page-ready-budget-ms` | `500` | Time budget for data to appear on a page. |
| `perf.runs` | `5` | Number of measurements used for the median. |
| `coverage.enforce` | `false` | `true` = fail the run when an operation of `openapi.yaml` has no happy-path call or no contract-validated call. Only for a **full** run. |

## C. Parallel execution (JUnit)

| Parameter | Meaning |
|---|---|
| `-Djunit.jupiter.execution.parallel.enabled=true` | Test classes run at the same time (default `false`). |
| `-Djunit.jupiter.execution.parallel.config.fixed.parallelism=4` | Number of threads. |

## D. Reports and logs (where things are written)

| What | Where |
|---|---|
| Allure results | `target/allure-results` (`allure.results.directory`) |
| Run log | `target/logs/test-run.log` (the folder is changed with the `LOG_DIR` variable/parameter) |
| Playwright trace of a failed UI test | `target/traces/*.zip` |
| API operation coverage | `target/api-coverage.md` |
| Surefire report (XML) | `target/surefire-reports` |
| k6 summary | `target/k6-summary.json` |

Report commands: `mvn allure:serve` (build and open), `mvn allure:report` (build only),
`mvn test surefire-report:report` (a plain HTML report without Allure).

## E. Other tools

| Tool | Command / parameters |
|---|---|
| OASDiff | `bash scripts/oasdiff.sh [path to the current openapi.yaml]` (default `../openapi.yaml`); needs Docker |
| k6 | `k6 run [--summary-export=target/k6-summary.json] -e BASE_URL=http://localhost:8080 perf/k6/api-latency.js` |
| Sonar | `mvn -DskipTests verify org.sonarsource.scanner.maven:sonar-maven-plugin:5.0.0.4389:sonar -Dsonar.token=<token>`; set `sonar.organization` and `sonar.projectKey` in `pom.xml`; `-Dsonar.qualitygate.wait=true` fails the build when the Quality Gate fails |
| Install a browser manually | `mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"` |
| Trace Viewer | `mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="show-trace target/traces/<file>.zip"` |
