---
layout: page
title: Testing guide
---

Use this guide to select automated checks for a PonHub change and find their results.
Complete [setup](SettingUp.html) first. Automated tests check the contracts covered by their assertions;
the DG's [manual-testing instructions](DeveloperGuide.html#appendix-instructions-for-manual-testing)
cover visible workflows, packaged launch and persistence checks that need a running application.

* Table of Contents
{:toc}

## Running tests

Run these commands from the repository root using Java 25 and the included wrapper.
On macOS, select the [prescribed Azul JDK 25 with JavaFX](SettingUp.html#setting-up-the-project-on-your-computer)
in the launching terminal. On Windows, replace `./gradlew` with `gradlew.bat`
(`.\gradlew.bat` in PowerShell).

```sh
./gradlew test
./gradlew check coverage
```

`test` runs the JUnit suite. `check coverage` also runs Checkstyle and generates coverage reports,
matching the Java CI validation command. Use `./gradlew clean test` when a clean rebuild is useful;
cleaning is not necessary for every run. Gradle tests enable assertions by default, and the Gradle `run`
task enables them for development. For native IntelliJ application launches, follow the
[development assertion configuration](DeveloperGuide.html#development-assertions), including the `-ea` VM option.

Some tests initialize JavaFX and need a display. On Linux without a graphical session, install Xvfb
using your system's package manager and run the CI form:

```sh
xvfb-run --auto-servernum ./gradlew check coverage
```

CI also runs `.github/run-checks.sh` on Linux for tracked-file whitespace, line-ending and final-newline
checks. Run it from the repository root in a POSIX shell with Git that supports `git grep -P`:

```sh
.github/run-checks.sh
```

If Git reports that Perl-compatible regular expressions are unavailable, those checks have not succeeded;
use a Git build with that support or inspect the Linux CI result.
The workflows in `.github/workflows/` define the actual platform checks. A local Mac pass does not establish
a Windows/Linux pass, and CI build/test success does not establish packaged GUI acceptance.

### Selecting tests during development

For a focused check, filter by a real test class. This example checks people-list index validation:

```sh
./gradlew test --tests 'ponhub.logic.PersonIndexResolverTest'
```

Repeat `--tests` to select more classes, or select one method with `package.Class.method`.
Gradle fails when a filter matches no tests; check the current source name rather than treating that as a pass.
The [Gradle tutorial](https://se-education.org/guides/tutorials/gradle.html) explains wrapper usage.

In IntelliJ, right-click a test class, method, package or `src/test/java` and choose **Run**.
A Gradle-delegated test run uses the Gradle task; a native JUnit run has its own configuration.
Keep the test JDK at 25 and assertions enabled (`-ea` for a native runner). Use the wrapper checks above
before requesting review, even when the IDE's selected tests pass.

## Representative tests and results

Start with tests near the changed behavior. These examples exercise the current canonical model/session
instead of the inherited AddressBook command path:

| Test | What it checks |
| --- | --- |
| [People index resolver](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/master/src/test/java/ponhub/logic/PersonIndexResolverTest.java) | Unit checks for people-list index resolution, range and role validation. |
| [Canonical people commands](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/master/src/test/java/ponhub/logic/commands/CanonicalPeopleCommandIntegrationTest.java) | People commands with model changes, saving/reloading and failed-save preservation. |
| [Canonical JSON storage](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/master/src/test/java/ponhub/storage/JsonPonHubDataStorageTest.java) | Canonical JSON round trips and rejection of invalid persisted state. |
| [Canonical Logic manager](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/master/src/test/java/ponhub/logic/CanonicalLogicManagerTest.java) | Mixed boundary checks for routing, shared views, preferences and persistence. |

The suite also covers lessons, enrolment, search and attendance. Follow the DG's
[maintainer reading path](DeveloperGuide.html#maintainer-reading-path) and the affected feature's source/tests
to choose relevant cases; these examples are not an acceptance checklist for the whole product.

After a Gradle run, open the local reports:

| Result | Repository-relative location |
| --- | --- |
| JUnit HTML summary and failures | `build/reports/tests/test/index.html` |
| JUnit XML results | `build/test-results/test/TEST-*.xml` |
| Production/test Checkstyle reports | `build/reports/checkstyle/main.html` and `test.html` |
| Coverage HTML from `coverage` | `build/reports/jacoco/coverage/html/index.html` |
| Coverage XML from `coverage` | `build/reports/jacoco/coverage/coverage.xml` |

`test` also finalizes with `jacocoTestReport`, whose HTML is at
`build/reports/jacoco/test/html/index.html`. Coverage describes executed code, not correctness or a required percentage.
After a filtered run, reports cover that selection; rerun `check coverage` to obtain the full suite's result.

When a check fails, record the source commit, command, OS/JDK, failing test or task and report details.
Distinguish a product failure from a missing display/tool or unsupported local runtime, then rerun the relevant
checks after resolving the cause. Preserve actual failures and skips in the PR evidence. For manual checks,
reuse the [DG procedure](DeveloperGuide.html#appendix-instructions-for-manual-testing) and record the exact artifact
and environment tested rather than copying the procedure or repeating another owner's acceptance campaign.

## Recording workflow and package results

Keep dated acceptance results in the relevant issue or a `validation/` evidence directory. For each scenario,
record **PASS**, **FAIL** or **NOT RUN**, the expected and observed behavior, and a link to its transcript,
screenshot or automated report. Include rejected commands, no-ops, failed saves and restart checks where
they affect the feature; record data-file hashes when claiming that operational bytes stayed unchanged.

Identify the tested source commit, JAR size and SHA-256, OS/architecture, Java distribution/version and
actual operator. State whether the result came from source tests, a packaged window, a hosted virtual
display or human interaction. Preserve failures, skips and diagnostic logs even when a retry succeeds;
a clean retry alone does not establish a root cause or a product fix.

When reusing a platform run, retain its original source, artifact, operator and coverage limits. Compare
application source/resources and build inputs before applying it to a later commit, and rerun affected
checks if those inputs change. Documentation-only changes can reuse unchanged application evidence;
their affected rendered pages and links still need their own checks. Follow the
[release procedure](DevOps.html#making-a-release) for final candidate and public-download verification.
