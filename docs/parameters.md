# Усі параметри запуску

## Як передати значення (пріоритет від найвищого)

1. Параметр Maven: `mvn test -Dui.headless=false`
2. Змінна оточення: ключ у верхньому регістрі, будь-який не-літерно-цифровий символ стає `_` (`api.base-url` -> `API_BASE_URL`).
   Середовище також можна задати змінною `TAF_ENV`.
3. Файл середовища `src/main/resources/env/<env>.properties` (вибирається `-Denv=<ім'я>`, за замовчуванням `local`).
4. `src/main/resources/env/default.properties`.

## A. Який набір тестів запускати (Maven/JUnit, не наші параметри)

| Параметр | Приклад | Що робить |
|---|---|---|
| `-Dgroups` | `-Dgroups=arch`, `-Dgroups=api,ui`, `-Dgroups="ui & !responsive"` | Запускає лише тести з відповідним **тегом** (`@Tag("...")` на класі чи методі). Кома = «або», `&` = «і», `!` = «не». |
| `-DexcludedGroups` | `-DexcludedGroups=perf,defect-candidate` | Навпаки: пропускає тести з цими тегами. |
| `-Dtest` | `-Dtest=GameApiTest`, `-Dtest=GameApiTest#startGame` | Один клас або один метод. |
| (нічого) | `mvn test` | Запускає **все**, включно з `perf`, `defect-candidate` і `arch`. |

Теги, які є в проєкті:

| Тег | Що містить |
|---|---|
| `api` | функціональні API-тести |
| `contract` | перевірка відповідей за `openapi.yaml` |
| `ui` | тести через браузер |
| `responsive` | планшет, обидві орієнтації, дотик |
| `e2e` | гібридні сценарії API + UI |
| `perf` | заміри часу (потрібна production-збірка) |
| `arch` | перевірка архітектури (ArchUnit); це не потребує запущеного додатка |
| `parallel-safety` | перевірка потокової моделі браузерів; потребує лише браузера, не додатка |
| `concurrency` | паралельні виклики бекенда |
| `defect-candidate` | підозрювані дефекти; падіння означає знахідку, а не флейк |

## B. Параметри проєкту

| Ключ (`-D...`) | За замовчуванням | Значення |
|---|---|---|
| `env` | `local` | Яке середовище: файл `env/<env>.properties` (є `local`, `ci`, `staging`-заглушка). |
| `api.base-url` | `http://localhost:8080` | Адреса бекенда. |
| `ui.base-url` | `http://localhost:4200` | Адреса фронтенда. |
| `openapi.spec` | `openapi/openapi.yaml` | Специфікація для контрактних перевірок: ресурс classpath, шлях до файлу або URL. У `ci` це `../openapi.yaml` (справжній файл репозиторію). |
| `api.log` | `all` | Стандартний лог REST Assured: `all`, `failures` (лише коли перевірка `then()` не пройшла) або `none`. |
| `ui.browser` | `chromium` | `chromium`, `firefox`, `webkit`. |
| `ui.headless` | `true` | `false` = бачити вікно браузера. |
| `ui.slow-mo-ms` | `0` | Затримка між діями браузера (для налагодження). |
| `ui.timeout-ms` | `10000` | Скільки Playwright чекає на елемент/дію. |
| `ui.video` | `false` | Записувати відео в `target/videos` (у звіт не додається). |
| `ui.auto-install-browser` | `true` | Скачати обраний браузер, якщо його немає. |
| `ui.browser-scope` | `auto` | Коли закривати браузер: `run` (в кінці прогону), `class` (після кожного класу), `auto` (`class`, якщо увімкнена паралель). |
| `test.reset-on-start` | `true` | Викликати `POST /api/test/reset` один раз перед прогоном. `false` для середовищ, де це заборонено. |
| `perf.page-ready-budget-ms` | `500` | Бюджет часу появи даних на сторінці. |
| `perf.runs` | `5` | Скільки замірів для медіани. |
| `coverage.enforce` | `false` | `true` = провалити прогін, якщо якась операція з `openapi.yaml` не має happy path або контрактного виклику. Лише для **повного** прогону. |

## C. Паралельний запуск (JUnit)

| Параметр | Значення |
|---|---|
| `-Djunit.jupiter.execution.parallel.enabled=true` | Класи тестів виконуються одночасно (за замовчуванням `false`). |
| `-Djunit.jupiter.execution.parallel.config.fixed.parallelism=4` | Скільки потоків. |

## D. Звіти й логи (куди писати)

| Що | Де |
|---|---|
| Результати Allure | `target/allure-results` (`allure.results.directory`) |
| Лог прогону | `target/logs/test-run.log` (папка змінюється змінною/параметром `LOG_DIR`) |
| Trace Playwright при падінні UI-тесту | `target/traces/*.zip` |
| Покриття операцій API | `target/api-coverage.md` |
| Звіт Surefire (XML) | `target/surefire-reports` |
| Підсумок k6 | `target/k6-summary.json` |

Команди звітів: `mvn allure:serve` (відкрити), `mvn allure:report` (зібрати), `mvn test surefire-report:report` (простий HTML без Allure).

## E. Інші інструменти

| Інструмент | Команда / параметри |
|---|---|
| OASDiff | `bash scripts/oasdiff.sh [шлях до поточного openapi.yaml]` (за замовчуванням `../openapi.yaml`); потрібен Docker |
| k6 | `k6 run [--summary-export=target/k6-summary.json] -e BASE_URL=http://localhost:8080 perf/k6/api-latency.js` |
| Sonar | `mvn -DskipTests verify org.sonarsource.scanner.maven:sonar-maven-plugin:5.0.0.4389:sonar -Dsonar.token=<токен>`; значення `sonar.organization` і `sonar.projectKey` у `pom.xml`; `-Dsonar.qualitygate.wait=true` червонить збірку при провалі Quality Gate |
| Встановлення браузера вручну | `mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"` |
| Trace Viewer | `mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="show-trace target/traces/<файл>.zip"` |
