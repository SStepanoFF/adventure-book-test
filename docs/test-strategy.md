# Test strategy - Adventure Book

## Approach

Test the application at the cheapest level that can show the defect, and use the browser only for what only a
browser can show.

| Level | Purpose | Where |
|---|---|---|
| API (functional) | every rule and status code, exhaustively and fast | `*ApiTest` |
| Contract | every operation and response family matches `openapi.yaml` (real traffic validated) | `ContractApiTest`, `TestSupportApiTest` |
| UI | what a user sees and does; one scenario per behaviour, not per data variant | `*UiTest` |
| E2E (hybrid) | journeys that cross levels: API prepares state, UI acts, API verifies | `HybridE2eTest` |
| Non-functional | page load budget, tablet layout/touch, concurrency | `PageLoadPerfTest`, `ResponsiveUiTest`, `ConcurrencyApiTest` |

## The six capabilities

| Capability | API | UI / E2E |
|---|---|---|
| 1 Library: list, search, filter | `BooksApiTest` (seed books, title/author/tag, case, combined, blank, no match, get 200/400/404) | `LibraryUiTest` (cards content, search by title/author, tag filter, empty state, new book appears) |
| 2 Play: start, navigate by choices | `GameApiTest` (start, advance, invalid index, unknown session, after end), `BuiltInBooksApiTest` (every real book completable) | `GameplayUiTest` (synthetic book to the end, **a real built-in book to the end**), `HybridE2eTest` |
| 3 Consequences & health | `HealthAndDeathApiTest` (partial/exact/cumulative damage, death, no choices after death) | `GameplayUiTest` (risky option marked, health, consequence text, death screen) |
| 4 Save & resume | `GameApiTest` (save, resume by GET, unknown session) | `HybridE2eTest` (save -> Continue, returning player from browser state, **remove a saved game**) |
| 5 Restart | `GameApiTest` (in progress, finished), `HealthAndDeathApiTest` (dead) | `GameplayUiTest`, `HybridE2eTest` (restart in UI, verify in API) |
| 6 Add a book | `BookValidationApiTest` (each invalid rule one by one, field validation), `BuiltInBooksApiTest` (**built-in books accepted, shipped invalid fixture rejected**) | `UploadUiTest` (valid book listed, not JSON, every invalid rule shown to the user) |

Business rules of the book format (none/several beginnings, no ending, invalid next id, non-ending section without
options) are one shared catalogue (`InvalidBookCases`) run on API and UI.

## Independence and repeatability

* The backend is reset ONCE per run through `POST /api/test/reset` (`SuiteHook`), never between tests.
* Every test creates its own uniquely named books/sessions and never reads data it did not create (the seed books
  are read-only for tests). No order dependency, no leftover state; a second run starts from the same state.
* Each browser test gets a fresh browser context (own localStorage - saved games live there).
* Tests that must run alone (`reset`, performance) are `@Isolated`; the rest can run in parallel (class level).

## Elements without a `data-testid`

The brief warns that not every control has a hook. Used `data-testid` first, then role, then CSS as a visible,
named fallback in ONE place (the page object), so a missing hook is a one-line fix:

| Element | Fallback | Note |
|---|---|---|
| Play button of a book card | `.book-card-footer button` | no hook, **no accessible name** (aria-hidden arrow) - a11y finding |
| Card title / author / description / tags / section count | `h2`, `.author`, `.description`, `.tags .chip`, `.section-count` | inside `book-card` |
| Brand link | `a.app-brand` | |
| Game book title | `[data-testid=game-header] h1` | |
| Health bar | role `progressbar` | |

Every `data-testid` used by the page objects was cross-checked against the Angular templates.

## What is automated and what is not

Automated: everything above - deterministic rules, validations, state transitions, contracts and the user journeys
that make the product work. Reasons: cheap to run on every push, failures point at one cause.

Deliberately NOT automated (better manual or exploratory) and why:

* **Persistence across a backend restart** (H2 file): needs control over the backend process; high cost for one rule.
  Checked manually: save a game, restart the backend, continue it.
* **Story content and wording, pacing, whether a book is "good"**: needs human judgement. Automation only proves that
  every book is valid and completable.
* **Visual design and look and feel, animations**: pixel comparison is brittle here and low value for this UI.
* **Cross-browser and real devices**: Chromium + tablet emulation (viewport, touch) automated; a real iPad/Android
  pass stays an exploratory check before a release.
* **Full accessibility audit**: one automated finding exists (button without name); a screen-reader pass is manual.
* **Transient loading states** (`books-loading`, `game-loading`): timing-dependent, flaky to assert, little value.
* **Load beyond a smoke profile**: `perf/k6/api-latency.js` is a smoke check, capacity testing needs a target environment.

## Findings

Tests tagged `defect-candidate` assert the specified behaviour for suspected defects found by reading the backend;
they run as a separate, non-blocking CI step so the build is not red for a known problem, and the report still shows them.
