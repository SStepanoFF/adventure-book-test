# Rules of the task -> tests

| Rule | Level | Test (class # method) |
|---|---|---|
| Book has none, or more than one beginning | API | `BookValidationApiTest#invalidBookIsRejected` cases "no beginning", "two beginnings" |
| | UI | `UploadUiTest#invalidBookShowsTheBrokenRule` cases "no beginning", "two beginnings" |
| Book has no ending (but can have several) | API | `BookValidationApiTest#invalidBookIsRejected` case "no ending"; `BookValidationApiTest#severalEndingsAreAllowed` |
| | UI | `UploadUiTest#invalidBookShowsTheBrokenRule` case "no ending"; `UploadUiTest#bookWithSeveralEndingsIsAdded` |
| Book has an invalid next section id | API | `BookValidationApiTest#invalidBookIsRejected` case "invalid next section id" |
| | UI | `UploadUiTest#invalidBookShowsTheBrokenRule` case "invalid next section id" |
| A non-ending section has no options | API | `BookValidationApiTest#invalidBookIsRejected` case "non-ending section without options field"; `BookValidationApiTest#nonEndingWithEmptyOptionsListIsRejected` (defect candidate: `"options": []`) |
| | UI | `UploadUiTest#invalidBookShowsTheBrokenRule` case "non-ending section without options field" |
| | Real data | `BuiltInBooksApiTest#builtInBookIsAcceptedOnUpload`, `#invalidFixtureIsRejected` |
| If player life reaches 0, the adventure is over | API | `HealthAndDeathApiTest#healthReachingZeroKills`, `#cumulativeDamageKills`, `#deadPlayerCannotContinue`, `#deadPlayerCanRestart`; defect candidates `#lethalChoiceIntoEndingKills`, `#healthIsNeverNegative` |
| | UI | `GameplayUiTest#deathEndsTheAdventure`, `#restartAfterDeath` |
| | E2E | `HybridE2eTest#dieInUiAndVerifyInApi` |
| Page loading takes 500 ms at most | UI | `PageLoadPerfTest#libraryLoadsWithinBudget`, `#gamePageLoadsWithinBudget` |
| | API | `PageLoadPerfTest#listBooksApiWithinBudget`; load profile `perf/k6/api-latency.js` |
| Responsive and playable on a tablet | UI | `ResponsiveUiTest#libraryFitsTheScreen`, `#uploadFitsTheScreen`, `#adventureIsPlayableOnTablet` (each for TABLET_LANDSCAPE and TABLET_PORTRAIT) |
