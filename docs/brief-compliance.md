# Compliance with the testing brief

Honest status: nothing below has been executed yet (see "Not verified"). "Met" means the artefact exists and was
reviewed against the brief and the application sources, not that a green run was observed.

| Requirement of the brief | Evidence | Status |
|---|---|---|
| Strategy covering all six capabilities, not only one happy path | `docs/test-strategy.md`; negative paths for every endpoint; all four real books completed through the API, one in the browser | Met |
| Mix of levels (API, UI/E2E) | `*ApiTest`, `ContractApiTest`, `*UiTest`, `HybridE2eTest`, `ResponsiveUiTest`, `PageLoadPerfTest` | Met |
| API tests against `openapi.yaml` | `ContractApiTest` (every operation and response family) + coverage report `target/api-coverage.md`, enforceable with `-Dcoverage.enforce=true`; `scripts/oasdiff.sh` for spec changes | Met |
| Independently runnable and repeatable | reset once per run, unique data per test, no ordering, fresh browser context per test | Met |
| Use of the reset endpoint | `AdventureBookSuiteHook` (`SuiteHook` SPI) | Met |
| `data-testid` first, graceful when missing | page objects; table in the strategy; selectors cross-checked with the templates | Met |
| CI: extend `ci.yml` with a pipeline stage | `ci/ci.yml` = baseline jobs + job `e2e` (backend + frontend started, suite run sequentially and in parallel, defect candidates, summary, report, artefacts) | Met on paper - never run on GitHub |
| Clear reporting of what passed/failed and why | Allure steps (`@Step`), request/response attachments, screenshot + trace + console errors on UI failure, API log, job summary with failures and coverage | Partly: report generation not verified |
| Judgement: what to automate vs manual and why | "What is automated and what is not" in the strategy | Met |
| Framework choice explained | `README.md` (Playwright for Java, REST Assured, JUnit 5, Allure) | Met |

## Objectives and hints of the task

| Objective / hint | Evidence | Status |
|---|---|---|
| 1 Framework is app-independent, reusable | generic core: `config`, `logging`, `junit`, `perf`, `data`, `api` (client, filters, contract, coverage), `ui` (base classes, browser lifecycle, layout checks); application layers separated and guarded by `ArchitectureTest`; app hooks through the `SuiteHook` SPI | Partly: boundary is enforced but it is not packaged as a separate library (one module, one package root by decision) |
| 2 Easy to maintain | layers (DTO / payload / service, page / steps / asserts), one place per concern, per-environment config, ArchUnit | Met |
| 3 Easy to use for developers | tests declare only what they use; builders; services with `Ok` helpers; run parameters (`-Denv`, `-Dgroups`); developer recipes in README | Met |
| 4 All rules covered by automated tests | beginnings, ending, invalid next id, section without options -> `InvalidBookCases` (API + UI); life 0 -> `HealthAndDeathApiTest`, UI, E2E; page load 500 ms -> `PageLoadPerfTest` + k6; tablet -> `ResponsiveUiTest` | Met (one rule - empty `options` list - is a defect candidate; perf needs a production build) |
| 5 Every endpoint: happy path + contract test | 9 operations; functional tests + `ContractApiTest`/`TestSupportApiTest`; `target/api-coverage.md`, `-Dcoverage.enforce=true` | Met |
| 6 At least one front-end E2E scenario | `HybridE2eTest` (6+ scenarios), `GameplayUiTest` | Met |
| 7 (extra) Business-oriented, standardized, automated report | Allure (epic/feature/story labels, `@Step`), JUnit XML, produced in CI with a job summary | Partly: report never generated, see below |
| 8 (extra) Test data mocked live or searched in the DB | data is created live through the API per test (`BookBuilder`, `BookPayloads`) and found through the API; NO direct DB access | Partly. A DB check is possible (H2 URL has `AUTO_SERVER=TRUE`, table `game_sessions`) but needs the same H2 version as the backend; not implemented |
| Hint: contract with OASDiff | `scripts/oasdiff.sh` + CI job `contract`; runtime validation as the second level | Met (not run) |
| Hint: responsive with a simulator | Playwright viewport + touch emulation (`ResponsiveUiTest`) | Met |
| Hint: performance with k6 | `perf/k6/api-latency.js` + CI step | Met (not run) |
| Hint: front end with Playwright | Playwright for Java | Met |
| Hint: static analysis with SonarCloud | `sonar.*` in `pom.xml` + CI job `quality` (skipped without `SONAR_TOKEN`) | Configured only: needs your SonarCloud organization, project key and token |
| GenAI allowed, author must explain everything | `docs/explained-uk.md` explains CI, contract, performance and static analysis; design decisions in `README.md` | Met |

## Not verified

* The code has never been compiled or run: sources were only checked for syntax. Library versions are pinned in `pom.xml`.
* `ci/ci.yml` has never run on GitHub Actions.
* Allure: the report could not be built on the author's machine because Maven Central was unreachable (network), so
  that the report and the `@Step` AspectJ agent are unverified.
* The `defect-candidate` tests are hypotheses from code reading until they are run.
