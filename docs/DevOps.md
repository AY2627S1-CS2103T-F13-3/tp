---
layout: page
title: DevOps guide
---

* Table of Contents
{:toc}

--------------------------------------------------------------------------------------------------------------------

## Build automation

This project uses Gradle for **build automation and dependency management**. **We recommend reading [this Gradle tutorial from se-edu/guides](https://se-education.org/guides/tutorials/gradle.html).**


The following commands perform common Gradle tasks.


* **`clean`**: Deletes the files created during the previous build tasks (e.g. files in the `build` folder).<br>
  For example: `./gradlew clean`

* **`shadowJar`**: Uses the Shadow plugin to create the fat JAR file `build/libs/ponhub.jar`.<br>
  For example: `./gradlew shadowJar`

Use the checked-in Gradle wrapper, which selects the Gradle distribution in
[`gradle-wrapper.properties`](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/master/gradle/wrapper/gradle-wrapper.properties).
The product version is separate: [`build.gradle`](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/master/build.gradle)
sets `version` for the JAR's `Implementation-Version` manifest attribute;
[`MainApp.java`](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/master/src/main/java/ponhub/MainApp.java)
sets `MainApp.VERSION` for the version reported in the startup log. Update both for a release, preserving their
existing convention (`1.3` and `v1.3`, for example). These values do not select the Gradle version.
See [Making a release](#making-a-release) for candidate, package and publication checks.

* **`run`**: Builds and runs the application.<br>
  **`runShadow`**: Builds the application as a fat JAR, then runs it.

* **`checkstyleMain`**: Runs the code style check for the main code base.<br>
  **`checkstyleTest`**: Runs the code style check for the test code base.

* **`test`**
  * `./gradlew test`: Runs all tests.
  * `./gradlew clean test`: Cleans the project before running all tests

--------------------------------------------------------------------------------------------------------------------

## Continuous integration (CI)

This project uses GitHub Actions for CI. The
[Java workflow](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/master/.github/workflows/gradle.yml)
runs `check coverage` on Windows, Linux and macOS with Java 25, using a virtual display on Linux.
Verify the checks on the PR's current head; results from an earlier commit are historical evidence.
Unit/integration CI, a packaged launch, and a rendered website check establish different things;
record their outcomes separately. Website preview instructions are in the [Documentation guide](Documentation.html#build-and-preview-locally).

### Code coverage

As part of CI, Gradle generates JaCoCo coverage reports from the tests, and CI uploads the coverage data to Codecov. Codecov then provides information about test coverage.

However, because Codecov is known to run into intermittent problems (e.g., report upload fails) due to issues on the Codecov service side, the CI is configured to pass even if the Codecov task failed. Therefore, developers are advised to check the code coverage levels periodically and take corrective actions if the coverage level falls below desired levels.

To enable Codecov for forks of this project, follow the steps given in [this se-edu guide](https://se-education.org/guides/tutorials/codecov.html).

### Repository-wide checks

In addition to Gradle checks, CI runs repository-wide checks. Unlike Gradle checks, which cover files used in the build, these checks cover every repository file and enforce rules that are hard to apply on development machines, such as line-ending requirements.

These checks are POSIX shell scripts. Run them from the repository root in a compatible shell on macOS,
Linux or Windows with Git Bash:

`./.github/run-checks.sh`

Any warnings or errors will be printed out to the console. The scripts inspect Git's index, so stage the
intended files with `git add` before running them. Review the staged diff before committing.

**If adding new checks:**

* Checks are implemented as executable `check-*` scripts within the `.github` directory. The `run-checks.sh` script will automatically pick up and run files named as such. That is, you can add more such files if you need and the CI will do the rest.

* Check scripts should print out errors in the format `SEVERITY:FILENAME:LINE: MESSAGE`
  * SEVERITY is either ERROR or WARN.
  * FILENAME is the path to the file relative to the current directory.
  * LINE is the line of the file where the error occurred and MESSAGE is the message explaining the error.

* Check scripts must exit with a non-zero exit code if any errors occur.

--------------------------------------------------------------------------------------------------------------------

## Making a release

This procedure is for the release maintainer. Prepare notes and a demonstration plan while features are
being implemented, then validate them against the reviewed candidate before publication. Keep dated
coordination and acceptance records in the relevant issues rather than copying them into this runbook.

### Freeze and identify the candidate

1. Confirm the intended feature scope, known limitations and owner checks. Update both product-version
   declarations described under [Build automation](#build-automation), then obtain review and integration.
2. From a clean checkout of the exact reviewed team commit, run `git rev-parse HEAD`, `java -version`,
   `./gradlew check coverage` and `./gradlew shadowJar` (use `gradlew.bat` in PowerShell).
   Run the [repository-wide checks](#repository-wide-checks) and verify current-head CI.
3. Record the source SHA, runtime, commands/results, JAR size and SHA-256 of `build/libs/ponhub.jar`.
   On macOS use `shasum -a 256 build/libs/ponhub.jar`, on Linux `sha256sum build/libs/ponhub.jar`,
   or in PowerShell `Get-FileHash build/libs/ponhub.jar -Algorithm SHA256`.
   The source, tag and uploaded asset must identify the same accepted candidate; recheck affected evidence
   if source or artifact changes.

### Verify the packaged application

Copy the candidate JAR into a fresh disposable folder, confirm Java 25 in the launch terminal, and run
`java -jar ponhub.jar`. Follow the [runtime prerequisites](SettingUp.html#setting-up-the-project-on-your-computer).
On macOS use the course-prescribed Azul JDK 25 with JavaFX (`25.0.3.fx-zulu`) for the Mac's architecture.
Record the OS, architecture, JDK distribution/version, source SHA, JAR hash and actual launch result.
Other runtime trials are optional portability evidence, not substitutes for the prescribed Mac check.
Compatibility with Windows, Linux and macOS remains required; use the existing JAR distribution.

Use the [User Guide](UserGuide.html) and [developer manual checks](DeveloperGuide.html#appendix-instructions-for-manual-testing)
for the candidate's supported commands and acceptance cases. Verify Help/menu/F1, the shared-lesson workflow,
retained attendance/history, and save/restart persistence. Include newly delivered mutations and their
failure cases without claiming proposed features work. In a fresh folder, missing operational data should
remain unwritten until the first successful mutation; preferences and logs are separate. Exercise rejected
data and recovery on disposable copies, preserving original bytes. Also launch from another working
directory and confirm packaged files remain anchored to the JAR folder. Record limitations and failures.
Building a JAR or passing Java CI alone does not establish these packaged outcomes.

For hosted Linux verification, manually dispatch **Verify Linux release package** in GitHub Actions using
the [workflow](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/master/.github/workflows/linux-package.yml).
Set `candidate_ref` to the reviewed **team source SHA**, `expected_sha256` to the intended JAR's
**matching SHA-256**, and `expected_version` to the **product version recorded in the reviewed handoff**
(for example, `1.5`, without the startup log's `v` prefix). Manual dispatch deliberately has no remembered
candidate defaults. The expected version must match the candidate source's `build.gradle` declaration;
it is then checked against the JAR manifest and runtime startup version. Push and pull-request runs use
the event SHA, its source-declared product version and freshly calculated checksum, so they check the
verification tooling but do not establish the independently identified release handoff. Track final candidate
verification in the [delivery tracker](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/328).

The hosted Ubuntu job rebuilds that source, rejects a different checksum and drives the real JAR's command
box on a virtual display through person and Lesson edits, rejected conflicting edits, attendance correction,
retained history and restart. It also checks the distribution filename, size, manifest version and entry point,
and confirms that the runtime startup version matches. Check the
[verification script](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/master/.github/scripts/verify_linux_package.py)
for its actual coverage; arrange additional checks for new features it does not exercise. Download
`linux-package-evidence` promptly (the workflow retains it for 30 days): it contains the exact JAR, runtime,
logs, disposable data and screenshots. Record the run URL and result. Automated X11 results do not establish
manual physical-display acceptance; a failed or unavailable check remains unverified.

### Publish and verify the release

1. Reconcile release notes, guide examples and media against the accepted candidate. Describe changes since
   the previous release and retain known limitations and author/AI/reuse credits. Capture fictitious data;
   reuse existing media only when it still matches and its provenance is retained.
2. Verify that the intended version tag points to the accepted source commit. Have the designated release
   owner [create the GitHub release](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository)
   with the accepted notes, exact verified JAR and working media links.
3. Download the public JAR and compare its SHA-256 with the accepted hash. Check the public tag/source,
   release copy and media, and verify the deployed guides via the
   [website checks](Documentation.html#maintain-and-publish-the-website). Record the release URL and outcomes.
4. Reconcile issue acceptance and the iteration's release records before milestone closure. An uploaded
   asset or closed issue alone does not prove a working release; retain explicit unresolved limitations.
