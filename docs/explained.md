# How it works (for a beginner)

> Everything below describes what is written in the project. None of it has ever been run: not the CI, not Sonar,
> not k6, not OASDiff. All run parameters: `docs/parameters.md`.

## 1. GitHub Actions (CI)

**What it is.** A GitHub service that, following an instruction file (`.github/workflows/*.yml`), runs your commands on a
temporary virtual machine. Nothing needs to be installed: it is part of GitHub. The Jenkins analogue is a `Jenkinsfile`
plus an agent that Jenkins starts for you.

**What it costs** (according to GitHub's documentation): free and unlimited for public repositories (standard runners).
For private repositories the free plan gives 2,000 minutes per month and 500 MB of artifact storage. Artifacts and logs
are kept for up to 90 days by default (configurable from 1 to 90 days for public repositories).

**What goes where (two different things).**

| | What it is | Where it comes from |
|---|---|---|
| A | the **application** repository (`backend/`, `frontend/`, `openapi.yaml`, `.github/workflows/ci.yml`) | the archive you were given |
| B | our **test project** (`pom.xml`, `src/`, `ci/ci.yml`, `docs/` ...) | `adventure-book-taf.zip` |

The tests must live **inside** the application repository, in a folder called `tests/`, and `ci.yml` must be where
GitHub looks for it.

```
adventure-book/                       <- the application repository (A), its root
├── backend/
├── frontend/
├── openapi.yaml
├── .github/workflows/ci.yml          <- REPLACE with the contents of adventure-book-taf/ci/ci.yml
└── tests/                            <- NEW folder: copy the WHOLE content of adventure-book-taf (B) here
    ├── pom.xml
    ├── src/
    ├── ci/ci.yml                     <- this copy just stays as a reference
    └── ...
```

Commands (macOS/Linux; `adventure-book` is the application folder):

```bash
unzip adventure-book-taf.zip                                   # creates the folder adventure-book-taf
mkdir adventure-book/tests
cp -R adventure-book-taf/. adventure-book/tests/               # the trailing "." also copies hidden files (.gitignore)
mkdir -p adventure-book/.github/workflows
cp adventure-book-taf/ci/ci.yml adventure-book/.github/workflows/ci.yml   # replaces the old ci.yml

cd adventure-book
git init                                                       # only if the folder has no .git yet
git add .
git commit -m "Add test automation"
git remote add origin https://github.com/<your-login>/<repo-name>.git
git branch -M main
git push -u origin main
```

Before that, create an **empty** repository on github.com (New repository, without a README). GitHub does not accept a
password for `git push`: use `gh auth login` (GitHub CLI) or a personal access token. The `.github` folder starts with a
dot and is hidden in Finder (Cmd+Shift+. shows it).

**Why it is laid out like this:** GitHub looks for workflows only in `.github/workflows/` at the root of the repository;
the `e2e` job starts `backend/` and `frontend/` from the **same** repository, so the tests have to be in it; `ci.yml`
contains `working-directory: tests` and `../openapi.yaml`, so the folder has to be called `tests` and sit directly in the
root. If you rename the folder, change it in `ci.yml` too.

**If your code is in Bitbucket:** GitHub Actions does not run there (it is a GitHub service). Options: mirror the
repository to GitHub just to run the CI; or move the same commands into a `bitbucket-pipelines.yml` or a `Jenkinsfile`:
the commands in `run:` are plain shell and Maven, only the wrapper changes.

**Then:** after the push, open the **Actions** tab on github.com. It is not a folder but a web page: the workflows are
on the left, the history of runs on the right. Click a run to see its jobs; click a job to see every step with its full
log. At the bottom of a run there is an **Artifacts** block (zip files with reports, traces and logs that you can
download), at the top a **Summary** with our table of results and API coverage. To start a run manually without a push:
Actions -> CI -> **Run workflow** (available thanks to `workflow_dispatch`).

**Where it runs:** on a GitHub virtual machine (Ubuntu) that exists only for the duration of the run. Logs and artifacts
are then stored on GitHub's servers, not in your repository.

**How to check before a push:** the YAML syntax is highlighted by an editor (VS Code with the GitHub Actions extension) or
by the `actionlint` tool. A workflow can be run completely on your machine with the `act` tool (Docker), but the simplest
way is to push to a separate branch and watch the Actions tab. The first run will almost certainly reveal small mistakes:
that is normal.

**Structure of the file, and the Jenkins equivalents:**

| Jenkins | GitHub Actions | In our file |
|---|---|---|
| `Jenkinsfile` | workflow (`ci.yml`) | `ci/ci.yml` |
| triggers / webhook | `on: push / pull_request / workflow_dispatch` | every push to `main`, every PR, a manual button |
| stage | **job** (own machine, own clean folder) | `backend`, `frontend`, `contract`, `quality`, `e2e` |
| step (`sh '...'`) | `steps: - run: ...` | `mvn`, `npm`, `k6` commands |
| agent / label | `runs-on: ubuntu-latest` | a clean GitHub virtual machine |
| plugin / shared library | `uses: actions/checkout@v4` (a ready-made action) | checkout, setup-java, setup-node |
| stage order | `needs: [backend, frontend]` | `e2e` starts only when backend and frontend are green |
| matrix axis | `strategy.matrix` | `parallel: [false, true]`: `e2e` runs twice |
| `archiveArtifacts` | `actions/upload-artifact` | reports, traces, logs |
| credentials | secrets (`${{ secrets.SONAR_TOKEN }}`) | the Sonar token |
| `catchError` / UNSTABLE | `continue-on-error: true` | the "defect candidates" step |

Our jobs: `backend` and `frontend` are the application's original checks (unchanged), `contract` runs OASDiff, `quality`
runs Sonar, `e2e` is the main one: it starts the backend (8080) and the frontend (4200), installs Chromium, runs the full
suite (`mvn test -Denv=ci`), k6, a separate non-blocking step with the "defect candidates", writes a summary (results,
failures, API coverage), builds the Allure report and uploads the artifacts.

## 2. SonarCloud (static analysis)

**What it is.** A service that reads code without running it and finds bugs, vulnerabilities, "code smells" and
duplication. It is now called SonarQube Cloud (the old name SonarCloud is still used).

**It is a separate website, not a settings section of Bitbucket.** Three different parts:
1. **The Sonar website/server** (sonarcloud.io, now SonarQube Cloud) stores the results and shows dashboards.
2. **The scanner**: a Maven plugin that runs in your CI or on your laptop, analyses the code and uploads the result to
   the website. The website does not build your code itself.
3. **The Bitbucket/GitHub integration** is only a convenience: signing in with a Bitbucket account, importing the list of
   repositories, showing the Quality Gate status directly in a pull request. It is not required for the analysis itself.

**Account and price** (according to Sonar's documentation): the account is created by signing in with Bitbucket (or
GitHub/GitLab/Azure DevOps). Free plan: public projects with no size limit, private projects up to 50,000 lines of code.
The Free plan analyses the main branch only. Our project is far below 50,000 lines.

**Setting it up with Bitbucket (according to Sonar's documentation):**
1. Open SonarQube Cloud -> **Sign in with Bitbucket**.
2. After signing in the site offers **Import an organization from Bitbucket**: you connect your Bitbucket workspace and
   create an organization (it corresponds one-to-one to the workspace). Choose the Free plan.
3. **Analyze a new project** -> tick the repository -> **Set up** (each repository corresponds to one Sonar project).
4. The site shows the **organization key** and the **project key**: put them into `pom.xml` instead of `CHANGE_ME`.
5. Create a token: My Account -> Security -> Generate token.
6. Run the analysis in CI. For Bitbucket Sonar recommends **Bitbucket Pipelines**: in Repository settings -> Pipelines ->
   Repository variables add a **secured** variable `SONAR_TOKEN` with the token value. Example `bitbucket-pipelines.yml`
   (not verified):

```yaml
image: maven:3.9-eclipse-temurin-21
pipelines:
  branches:
    main:
      - step:
          name: Static analysis
          script:
            - cd tests
            - mvn -B -DskipTests verify org.sonarsource.scanner.maven:sonar-maven-plugin:5.0.0.4389:sonar
```

With GitHub everything is the same: Sign in with GitHub, import the organization, the `SONAR_TOKEN` secret in
Settings -> Secrets and variables -> Actions, and the analysis is done by the `quality` job of our `ci.yml`. The scanner
command is identical everywhere; only the place where the token is stored differs.

**Run it locally (without CI):**
`mvn -DskipTests verify org.sonarsource.scanner.maven:sonar-maven-plugin:5.0.0.4389:sonar -Dsonar.token=<token>`.
Without an account: `docker run -p 9000:9000 sonarqube:community`, open http://localhost:9000 (admin/admin), create a
token and add `-Dsonar.host.url=http://localhost:9000`.

**How to make sure the setup works:** add a deliberate "smell" to a test class (an empty `catch`, an unused variable,
`String password = "123456";`), run the analysis and find it in the Issues tab. Then remove it.

**Quality Gate** = a set of thresholds (no new bugs, duplication below a limit, and so on). If it is not passed, the job
turns red when `-Dsonar.qualitygate.wait=true` is used. We excluded code coverage (`sonar.coverage.exclusions`): this
project is the tests, coverage is not measured here.

## 3. Contract testing

**The contract** = the file `openapi.yaml`: the list of endpoints (method + address), their parameters, the shapes of
requests and responses, the status codes.

### Level 1: validation of real requests and responses (inside the tests)

- How it works: `ApiClient.withContract()` adds a filter from the swagger-request-validator library. The filter sees every
  request and every response that passes through and compares them with `openapi.yaml`. A mismatch = an exception = a
  red test.
- **Only in these tests?** Yes, on purpose: `ContractApiTest` (one test per operation and response type: 200, 201, 400,
  404, 409) and `TestSupportApiTest`. Any other test can switch the check on too:
  `new BookService(ApiClient.withContract())`. We do not do it everywhere because negative tests deliberately send
  "wrong" requests (broken JSON) and the contract would reject them.
- **What is checked:** whether such an operation exists (method + address template); required path and query parameters
  and their types/formats; the request body against its schema; whether the operation describes such a status code;
  the content type; the response body against its schema (field types, enums, the `uuid` format, fields that must not
  be `null`).
- **What is NOT checked:** business logic (whether `health` really is 10). A missing field is not detected either unless it
  is in the schema's `required` list: `GameState` has no such list, so the contract will not notice a missing field (this
  is a remark about the specification itself). The library does not understand that in the spec `lastConsequence` has
  `nullable` next to `$ref`; if a contract test fails on that, it is a finding about the specification.
- **Where the specification comes from:** the `openapi.spec` parameter. Locally it is a copy in
  `src/test/resources/openapi/`; in CI (`-Denv=ci`) it is the repository's real file `../openapi.yaml`, so the copy cannot
  go stale.

### Level 2: OASDiff (has the contract itself been broken?)

- **What it is.** An open-source utility (Tufin, written in Go) that compares **two versions** of an OpenAPI file and
  tells you what changed and, above all, what **breaks clients** (breaking changes): a removed endpoint or parameter, a
  changed field type, a new required field, a different response code. It is the answer to the task's hint.
- **Which files are compared.** The baseline `contract/baseline/openapi.yaml` (the approved version, stored in git; today it
  is an exact copy of the application's file) against the application's current `openapi.yaml` (`../openapi.yaml` in CI;
  locally pass the path as an argument: `bash scripts/oasdiff.sh /path/to/adventure-book/openapi.yaml`).
- **What to install:** only Docker (the script runs the `tufin/oasdiff` image). Alternative: install the utility itself
  (`brew install oasdiff`, other ways are on the project's page) and run
  `oasdiff breaking <baseline> <current> --fail-on ERR`.
- **How the script works:** it copies the current file into `target/`, prints all changes (`changelog`, informational) and
  runs `breaking --fail-on ERR`: exit code 1 if there are breaking changes, and the CI turns red.
- **When to update the baseline:** if the API was changed deliberately, copy the new `openapi.yaml` over the baseline and
  commit it.
- **How to check it:** make a copy of the specification, delete an endpoint in it or change `type: integer` to
  `type: string`, run `bash scripts/oasdiff.sh <your copy>`. You should see a list of breaking changes and exit code 1.
  With no changes the script ends with code 0.

The levels complement each other: level 1 says "the server does not match the specification", level 2 says "somebody
broke the specification itself".

## 4. k6 (load)

**What it is.** An open-source load-testing tool from Grafana. The scenario is written in JavaScript and executed by an
engine written in Go, so it is fast.

**How it works.** `export default function` is the scenario of one **virtual user** (VU). k6 starts 10 of them at the same
time for 30 seconds and each repeats the scenario in a loop (`perf/k6/api-latency.js`): list the books, start a game, read
the game state. `setup()` runs once before the load (it picks the id of a real book).

**How time is measured.** For every HTTP request k6 measures `http_req_duration` = the time from the start of sending the
request to the complete receipt of the response (sending + the server's processing + downloading the response; without the
time to establish the connection). **p(95) < 500** means: 95% of the requests are faster than 500 ms; the slowest 5% do not
count towards the threshold.

**Thresholds** are the success conditions. If any is not met, k6 ends with exit code 99 and the CI turns red. There are
thresholds for all requests and separately for each endpoint (the `endpoint:...` tags).

**Installation:** `brew install k6`, `choco install k6` or Docker (`grafana/k6`). In CI a ready-made step installs it
(`grafana/setup-k6-action`).

**Run:** `k6 run -e BASE_URL=http://localhost:8080 perf/k6/api-latency.js` (the backend must be running). Result: a table
in the console; with `--summary-export=target/k6-summary.json` also a file.

**How to check that the thresholds really work:** temporarily change a threshold to `p(95)<1` (1 ms): the run should fail
with code 99. Or stop the backend: the `http_req_failed` threshold fails.

**Important:** k6 measures API time, not the time of a page in a browser. Page time is measured by Playwright (next section).

## 5. Page time in Playwright

`PageLoadPerfTest` measures from `page.navigate(...)` to the moment the page shows its data (the first book card or the
section text), so it includes the network, JavaScript execution and rendering. A warm-up pass is not counted; after it the
**median** of `perf.runs` (5) measurements is taken, because a single measurement is noise. The budget is
`perf.page-ready-budget-ms` (500). The test is `@Isolated`: it never runs at the same time as other tests. Run:
`mvn test -Dgroups=perf`. Measurements are meaningful only against a production build of Angular, which is why the `perf`
tag is switched off in CI.

## 6. What `-Dgroups=arch` is

A Maven/JUnit option meaning "run only tests that carry the `arch` tag". `ArchitectureTest` has `@Tag("arch")`, so only it
runs (an architecture check: pages hold only locators, the core does not know about the application, and so on). Nothing
else, and nothing from the application, is needed for it. The complete list of tags and all other parameters:
`docs/parameters.md`.
