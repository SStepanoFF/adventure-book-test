# Adventure Book - test automation framework + tests

One Maven module (Java 21), one package root: `com.example.adventurebook`.

## Run

All run parameters (tags, environment keys, tools): `docs/parameters.md`. Beginner explanations of CI, contract, performance and static analysis: `docs/explained-uk.md`.

Start the application first (`backend: ./mvnw spring-boot:run`, `frontend: npm start`), then:

```bash
mvn test                                   # everything, env=local
mvn test -Denv=ci                          # environment = run parameter (env/<name>.properties)
mvn test -Dgroups=api                      # tags: api | contract | ui | responsive | e2e | perf | arch | defect-candidate
mvn test -DexcludedGroups=perf,defect-candidate
mvn test -Dui.headless=false -Dui.browser=firefox -Dui.slow-mo-ms=200     # any key can be overridden with -D
mvn test -Djunit.jupiter.execution.parallel.enabled=true                    # parallel run is supported
mvn allure:serve                           # report (Allure Report 3); `mvn allure:report` for a static one
```

Any config key can also be given as an environment variable (`api.base-url` -> `API_BASE_URL`).
Adding an environment = adding `src/main/resources/env/<name>.properties`.

All run parameters, tags and config keys: `docs/parameters.md`.

## Developer guide (adding things)

* **A new API endpoint:** add a method to the matching service (or a new `<Group>Service`) with the path constant next to
  it: `client.request()...get(PATH)` returns a plain `Response`; add an `...Ok` variant if tests need the DTO. Put the DTOs in
  `api/dto/request` / `api/dto/response` (suffix `Dto`) and a body builder in `api/payload` if needed.
* **A new page:** `<Name>Page` (locators only, `@Getter`), `<Name>Steps` (actions, `@Step`), `<Name>Asserts` (checks, `@Step`).
  A missing `data-testid` is a CSS/role fallback inside the page object, with a comment.
* **A new test:** extend `BaseTest` (API) or `UiTest` (browser), declare only the services you use, create your own data
  (`Unique`, `BookBuilder`), tag it (`api`, `ui`, `e2e`, `contract`, `perf`).
* **Run:** `mvn test -Dgroups=api`, `-Denv=staging`, `-Dui.headless=false`; other tools: `bash scripts/oasdiff.sh`, `k6 run perf/k6/api-latency.js`,
  Sonar (`docs/explained-uk.md`).

## CI

`ci/ci.yml` is the application's baseline workflow plus a third job `e2e` (backend and frontend started, API/contract/UI
suite run sequentially and in parallel, defect candidates as a non-blocking step, job summary with failures and API
coverage, Allure report and traces as artefacts). Copy this project to `tests/` of the application repository and replace
`.github/workflows/ci.yml` with `ci/ci.yml`. See also `docs/test-strategy.md` and `docs/brief-compliance.md`.

## Layers

```
com.example.adventurebook
  config/   TafConfig                 env selection (-Denv) and typed config
  logging/  Reports                   the only place that attaches evidence to the Allure report
  junit/    TafExtension, SuiteHook   per-test log context, failure artefacts, once-per-run setup/cleanup
  perf/, data/                        median-of-N timing asserts, unique test data
  api/      ApiClient, ApiCoverage,   CORE: ONE client = a pre-configured REST Assured RequestSpecification
            ContractValidator, Json   (any path, any verb); logging, Allure, coverage, contract = filters
  api/dto/{request,response,type}     what we send | what we read | shared enums
  api/payload                         BookBuilder / BookPayloads: request-body construction
  api/service                         one service per endpoint group; the group's paths live in the service
  ui/       BasePage, BaseComponent,  CORE: base classes, browser lifecycle, installer, layout checks
            BaseSteps, BrowserManager,
            UiSession, ...
  ui/pages, ui/components             page objects: ONLY locators (plain Playwright Locator) + Lombok getters
  ui/steps                            <Page>Steps   - what a user does (actions, reads, navigation)
  ui/asserts                          <Page>Asserts - what must be true (web-first Playwright assertions)
src/test/.../                         api | contract | ui | responsive | perf | e2e | arch
```

Flow of an API call: `test -> Service method -> client.request().pathParam(..).body(dto).post(PATH)` -> plain
`io.restassured.response.Response` -> `then().statusCode(201)` / `as(Foo.class)` / `as(Foo[].class)`.
Service convention: no suffix = returns the `Response` (any status); `...Ok` = asserts the success status and returns the DTO.
Bodies come from `BookBuilder`/`BookPayloads`. A test declares only the services it uses
(`private final BookService books = new BookService();`) - there is no do-everything base class.
`ArchitectureTest` keeps the layering: the core packages (`api`, `ui`) never depend on the application layers, UI classes
never call API services, and page objects hold nothing but locators.

## Design decisions

**Browser / Context / Page.** `Playwright` and `Browser` are expensive and not thread-safe: one per thread, created
lazily (`BrowserManager`). A `BrowserContext` is cheap and gives full isolation (cookies, localStorage - the saved
games live there): one per test (`UiSession`), closed by the extension. `Page` comes from the test's context.
Page objects only receive a `Page`; they know nothing about how the browser was started.

**Locators: plain Playwright `Locator`, no wrapper.** A Locator is already lazy, auto-waiting, strict and every action
shows up in the Playwright trace; Playwright's own assertions (`assertThat(locator)`) are richer than any wrapper we
would write. Wrapping it (by composition, or by a runtime proxy that makes an `Element` IS-A `Locator`) adds a layer
to maintain and has a real cost: Playwright casts to its own implementation inside its assertions and option objects,
so a wrapper must be unwrapped there. Logging and Allure steps therefore live at the business level, in the
`...Steps` / `...Asserts` classes, and the low-level detail comes from the trace attached on failure.
(`locator.describe("...")` exists in newer Playwright versions to give a locator a readable name in traces/errors.)

**Pages / Steps / Asserts.** A page object is `@Getter` + locators + `path()`; it extends `BasePage` (no self-referencing
generic: it was needed only while pages returned `this` for fluent chains). `<Page>Steps` extend `BaseSteps<P>` (a plain
generic for the page type), `<Page>Asserts` are independent; both are created from a `Page`, so a test composes what it needs.

**Locators.** Preference order: `data-testid` -> role + accessible name -> CSS/text fallback. Locators are lazy and
auto-wait, so elements are plain fields. Every CSS fallback is a candidate for "ask the developers for a test id".

**Steps: Allure `@Step` annotations.** No custom step helper: business-level methods of the `...Steps` and `...Asserts`
classes carry `@Step("Search books for '{text}'")`, which shows up as a (nested) step in the Allure report, with parameters
substituted into the name. `@Step` is applied by an AspectJ agent, configured in the Surefire `argLine` of `pom.xml`
(IDE runs need the same `-javaagent:` VM option). There is no `@Steps` annotation in Allure (that one belongs to
Serenity BDD); the "steps classes" are ordinary classes. HTTP calls need no steps: REST Assured logs them itself (`api.log`)
and Allure attaches each request/response.

**Browsers.** Playwright would download every browser (chromium, firefox, webkit) on first start. `BrowserManager` turns
that off and `BrowserInstaller` installs only the selected browser (`-Dui.browser=`), only if it is missing
(`ui.auto-install-browser=false` to forbid).

**Waiting.** No sleeps. Assertions are web-first (`should*` retry until the UI catches up); timing measurements
use event-driven waits.

**Test independence.** The backend is reset once per run (`SuiteHook`); every test creates its own uniquely named
book/session, so tests do not depend on order or on each other and can run in parallel. The only test that calls
reset is `@Isolated`.

**API + UI together.** Preconditions and verification through the API, the journey through the UI
(`HybridE2eTest`). The app has no login: the equivalent is the game *session* (created via API, opened directly
in the browser) and browser state pre-set with `seedLocalStorage`. If authentication is added, a stored login
state belongs in `UiSession`.

**API coverage.** `ApiClient` records every call (REST verb + path template); after the run `target/api-coverage.md`
lists each operation of `openapi.yaml` with its number of calls, 2xx calls and contract-validated calls - the
objective "every endpoint: one happy path and one contract test" as a report instead of a claim. Gaps are warnings;
`-Dcoverage.enforce=true` fails the run (use it for the full CI run, not for filtered runs).

**Parallel execution.** `-Djunit.jupiter.execution.parallel.enabled=true` runs test *classes* concurrently (4 by
default); the methods of one class run on one thread (`@Execution(SAME_THREAD)` on `UiTest`). Playwright is driven
by the thread that calls it, so every thread creates AND closes its own `Playwright`/`Browser`
(`BrowserManager.closeThreadBrowser()`, called by `TafExtension.afterAll`; `ui.browser-scope=auto|class|run`).
Safety nets: `BrowserThreadingTest` (several browsers open at once, no mixing, closed by their owners),
`ConcurrencyApiTest` (the backend under concurrent load), `@Isolated` on tests that must run alone (reset, perf),
unique data per test, and a CI matrix that runs the suite both sequentially and in parallel.

**Logging.** API: REST Assured's own standard request/response log (`-Dapi.log=all|failures|none`, default `all`; `failures`
prints only when a `then()` check fails). UI/business steps: SLF4J + Logback (console = INFO, `target/logs/test-run.log` =
DEBUG, test name in every line via MDC); UI steps live in the Allure report (`@Step`), not in the log. On a UI failure:
screenshot, URL, browser console errors and the Playwright trace are attached to the report.

## Reporter: Allure Report 3 (results written by Allure Java 3)

| Option | Verdict |
|---|---|
| **Allure Report 3** (OSS, static HTML, plugin based; Allure 2 stays available) | **Chosen.** Business-oriented structure (epic/feature/story), steps, attachments (HTTP exchanges, screenshots, traces), history/trends, flaky detection, quality gate, GitHub Actions integration; works from `mvn allure:report`, no server. |
| ReportPortal (OSS, server) | Best when many teams/projects need a shared live dashboard and ML failure triage; needs a server + DB, overkill for this scope. The log/step abstraction (`Steps`, `Reports`) keeps a later switch cheap. |
| ExtentReports, Serenity BDD | Single-file HTML / opinionated BDD style; weaker ecosystem momentum and no history story out of the box. |

### And Playwright's own reporter?

The reporters you know (`html`, `list`, `line`, `json`, `junit`, `blob`) belong to the **Playwright Test runner**
(Node.js, configured in `playwright.config`). Playwright for Java is a library: the runner here is JUnit, so those
reporters are not available. What Java does get is the **Trace Viewer** (DOM snapshots, network, console, timeline of
every action) - it is attached to the report on every UI failure:
`mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="show-trace target/traces/<file>.zip"`
(or drag the zip into trace.playwright.dev). They complement each other: the report (Allure) answers *what failed,
how often, since when*; the trace answers *why it failed on this page*. Allure also publishes an official
`allure-playwright` module for Playwright Java (AspectJ based action steps + screenshots); it is not wired in here.
If Allure is unavailable, `mvn test surefire-report:report` still produces a plain HTML summary (`target/site`).

## Defect candidates (found by reading the backend, to be confirmed by running)

Tests tagged `defect-candidate` assert the *specified* behaviour; a failure is a finding to report.

1. A non-END section with `"options": []` is accepted (validator only checks `null`) - rule 4.
2. A lethal choice that leads into an END section ends as FINISHED, not DEAD.
3. Health can go negative (6 + 6 = -2).
4. A consequence value of `"-5"` heals above `maxHealth`.
5. Malformed JSON / unknown enum value -> 500 instead of 400.
6. The play button on a book card has no accessible name (and no test id).

## Not verified

The code was written without being able to download dependencies or run the application: sources were checked for
syntax only. Library versions are pinned in `pom.xml` properties - run `mvn -q versions:display-property-updates`.
Expect to touch: Allure attachment calls (isolated in `Reports`), and the contract filter if the OpenAPI document
has quirks (e.g. `nullable` next to `$ref` on `lastConsequence`) - a contract failure there is a spec finding.
