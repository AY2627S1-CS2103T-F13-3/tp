# Lesson cards and navigation validation

Issue: [#320](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/320).
Base: merged shell `93f0162f40b6d8e56a9e6c5df9534bfde9f283bd` on team `master`.
Date: 20 October 2026. Environment: macOS Darwin 25.6.0, arm64,
Azul Zulu Java/JavaFX 25.0.3.

## Automated results

`./gradlew --no-daemon check coverage shadowJar` passed: 893 tests in
130 suites, zero failures, errors or skips, including both Checkstyle tasks.
Repository formatting scripts, diff whitespace, FXML XML parsing and guide
same-page anchors/image targets passed.

Local JaCoCo inspection covers all 32 changed executable Java lines and
all eight branches on those lines. `LessonCard` also retains complete class
line/branch coverage. Existing uncovered `MainWindow` help-error/accelerator
paths outside this contribution remain visible in the full report; this
record does not claim repository-wide 100% coverage. Codecov on the pushed
PR head remains a separate gate recorded in the PR.

`LessonBoardNavigationTest` adds nine regressions for typed routes from
opposing views, conflicting selection, mixed/filtered people positions,
empty results, dated unrecorded/current/former attendance, committed edits,
optional-button equivalence, draft/caret preservation, absent/stale/non-Student
selection and protected-session behavior. Read-only cases check saved bytes
and relevant canonical snapshots.

The displayed-Stage regression sizes a decorated native window to 450 by
360, then inspects its actual Scene dimensions on subsequent JavaFX turns.
Navigation, command entry, status and both scrolling viewports fit. Existing
330-pixel content-height and divider tests remain; navigation shares the
existing feedback section's minimum height instead of increasing the total
minimum layout budget. The feedback section retains an 80-pixel minimum,
including navigation and at least 50 pixels for scrollable feedback.

## Visual inspection and limits

The [updated guide image](../../docs/images/LessonBoardNavigation.png) is an
attached-scene snapshot of production FXML using disposable test data at
1000 by 720 logical pixels. It was visually inspected for the navigation,
single subject heading, stable Lesson ID/schedule line, readable values,
independent people/Lesson context and full-width bottom input.

This is bounded automated/native-window evidence, not an independent manual
application acceptance run or a display/scaling matrix. Full redesign
acceptance remains #321; #317 help acceptance and #268/#270 owner checks
retain their scope. A complete Jekyll site build was not run locally because
the installed system Ruby/Bundler lacks the repository's dependencies.
No release or milestone completion is claimed.
