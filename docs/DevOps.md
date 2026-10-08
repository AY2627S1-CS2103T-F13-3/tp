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

The Week 8 increment uses Gradle version `1.2` and `MainApp.VERSION` `v1.2`. This identifies the increment, not completion of all planned shared-lesson features. For a packaged check, copy `ponhub.jar` into a fresh folder, confirm `java -version` reports Java 25, and run `java -jar ponhub.jar`. Verify Help/menu/F1 and the current supported commands there. Yang owns the final integrated product launch check; v1.2 publication is optional.

The single JAR stores JavaFX 17.0.7 native libraries in separate platform directories for Windows x64, Linux x64,
macOS x64 and macOS ARM64. `Main` selects and extracts only the current directory before JavaFX starts. Native files
are deliberately absent from the JAR root, preventing Shadow from silently resolving the same-named macOS x64 and
ARM64 libraries by dependency order. `./gradlew check` also builds the JAR and verifies the four platform directories,
the expected Glass library in each, the absence of root native files and unique archive paths.

Packaged launch evidence on 8 October 2026 used macOS 26.6.2 ARM64 and `ponhub.jar` built from this change
(12,920,771 bytes; SHA-256 `36ae74862907a0375fb15e17a4f0b10a09b9549d3499f1d4bbedd6c38705aaba`). The JAR was
copied into a new writable directory and launched with an OpenJDK 25.0.3 Zulu runtime containing `java.se` and
`jdk.unsupported`; its module list contained no JavaFX modules. The application initialized preferences and data and
logged `Starting UI...` without an incompatible-architecture or missing-toolkit error before it was closed manually.
The two packaged macOS Glass libraries were also inspected as distinct Mach-O ARM64 and x86_64 files. Actual launches
on Windows x64, Linux x64, macOS x64 and Oracle Java 25.0.1 were not performed locally; the automated archive check
and the existing three-platform CI matrix cover build-time regressions, while those platform launches remain release
verification steps.

* **`run`**: Builds and runs the application.<br>
  **`runShadow`**: Builds the application as a fat JAR, then runs it.

* **`checkstyleMain`**: Runs the code style check for the main code base.<br>
  **`checkstyleTest`**: Runs the code style check for the test code base.

* **`test`**
  * `./gradlew test`: Runs all tests.
  * `./gradlew clean test`: Cleans the project before running all tests

--------------------------------------------------------------------------------------------------------------------

## Continuous integration (CI)

This project uses GitHub Actions for CI. The necessary workflow configuration files are in `.github/workflows`. No further setup is required.

### Code coverage

As part of CI, Gradle generates JaCoCo coverage reports from the tests, and CI uploads the coverage data to Codecov. Codecov then provides information about test coverage.

However, because Codecov is known to run into intermittent problems (e.g., report upload fails) due to issues on the Codecov service side, the CI is configured to pass even if the Codecov task failed. Therefore, developers are advised to check the code coverage levels periodically and take corrective actions if the coverage level falls below desired levels.

To enable Codecov for forks of this project, follow the steps given in [this se-edu guide](https://se-education.org/guides/tutorials/codecov.html).

### Repository-wide checks

In addition to Gradle checks, CI runs repository-wide checks. Unlike Gradle checks, which cover files used in the build, these checks cover every repository file and enforce rules that are hard to apply on development machines, such as line-ending requirements.

These checks are implemented as POSIX shell scripts, and thus can only be run on POSIX-compliant operating systems such as macOS and Linux. To run all checks locally on these operating systems, execute the following in the repository root directory:

`./.github/run-checks.sh`

Any warnings or errors will be printed out to the console.

**If adding new checks:**

* Checks are implemented as executable `check-*` scripts within the `.github` directory. The `run-checks.sh` script will automatically pick up and run files named as such. That is, you can add more such files if you need and the CI will do the rest.

* Check scripts should print out errors in the format `SEVERITY:FILENAME:LINE: MESSAGE`
  * SEVERITY is either ERROR or WARN.
  * FILENAME is the path to the file relative to the current directory.
  * LINE is the line of the file where the error occurred and MESSAGE is the message explaining the error.

* Check scripts must exit with a non-zero exit code if any errors occur.

--------------------------------------------------------------------------------------------------------------------

## Making a release

Here are the steps to create a new release.

1. Update the version number in [`MainApp.java`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/MainApp.java).
1. Generate a fat JAR file using Gradle (i.e., `./gradlew shadowJar`).
1. Tag the repo with the version number. e.g. `v0.1`
1. [Create a new release using GitHub](https://help.github.com/articles/creating-releases/). Upload the JAR file you created.
