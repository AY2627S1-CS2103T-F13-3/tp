---
layout: page
title: Setting up and getting started
---

This guide helps a new contributor build PonHub, launch a development session and find the active code.
PonHub manages people, shared lessons and attendance; the [User Guide](UserGuide.html#current-command-summary)
describes the commands available in the current build.

* Table of Contents
{:toc}

## Setting up the project on your computer

1. **Fork and clone** the [team repository](https://github.com/AY2627S1-CS2103T-F13-3/tp), then open a terminal
   in the cloned repository's root, where `gradlew` and `build.gradle` are located.
2. **Select JDK 25.** On macOS, follow the [course's Mac advisory](https://nus-cs2103-ay2627-s1.github.io/website/admin/programmingLanguages.html)
   and [Mac installation guide](https://se-education.org/guides/tutorials/javaInstallationMac.html): use the
   prescribed **Azul JDK 25 with JavaFX (`25.0.3.fx-zulu`)** for your Mac's architecture. Confirm `java -version`
   in the launching terminal. With SDKMAN, `sdk current java` identifies the selected distribution and
   `sdk use java 25.0.3.fx-zulu` selects that installed Mac JDK for the current terminal.
   Windows and Linux also require Java 25. PonHub must remain compatible with all three platforms.
   Plain Oracle Java without JavaFX on Mac is an optional portability diagnostic; required Mac checks use
   the prescribed Azul setup above.
3. **Verify the build environment** using the included Gradle wrapper; a separate Gradle installation is unnecessary.

   ```sh
   ./gradlew --version
   ./gradlew check coverage
   ```

   On Windows PowerShell or Command Prompt, use `gradlew.bat` in place of `./gradlew`
   (`.\gradlew.bat` in PowerShell). Check that Gradle reports Java 25. See the
   [Testing guide](Testing.html#running-tests) for Linux virtual-display requirements, reports and failure diagnosis.
4. **Launch the development application** with `./gradlew run` (`gradlew.bat run` on Windows).
   This launch enables assertions. In a fresh clone, PonHub starts with an empty people/lesson view;
   try `help` and `list`, then `exit`. Development data and preferences are relative to the working directory,
   so use a disposable clone when trying commands that change records.

### Using IntelliJ IDEA

Import the root as a [Gradle project](https://se-education.org/guides/tutorials/intellijImportGradleProject.html).
Set both the [project SDK and Gradle JVM](https://se-education.org/guides/tutorials/intellijJdk.html) to JDK 25,
including the prescribed Azul JDK with JavaFX on macOS. The IDE can use a different JVM from your terminal.

Prefer the Gradle `run` task for the same development launch as above. For a native **Application** run
configuration, select `seedu.address.Main` and add `-ea` to **VM options**. Follow the
[development assertions check](DeveloperGuide.html#development-assertions) for the configuration you actually use;
remove its temporary probe before committing. The packaged user launch does not require `-ea`.

## Before writing code

1. **Follow the current design.** Read [PonHub's architecture](DeveloperGuide.html#architecture), then trace
   [canonical people commands](DeveloperGuide.html#canonical-people-commands) through routing, staged changes,
   persistence and view publication. The DG's [maintainer reading path](DeveloperGuide.html#maintainer-reading-path)
   links the source and representative tests.
2. **Configure code style.** Follow the [IDEA code-style guide](https://se-education.org/guides/tutorials/intellijCodeStyle.html).
   The optional [Checkstyle integration](https://se-education.org/guides/tutorials/checkstyle.html) gives feedback
   while editing; `./gradlew check coverage` remains the repository validation path.
3. **Run focused checks, then repository checks.** Use the [Testing guide](Testing.html) for selection and reports,
   [Logging](Logging.html) for startup diagnosis, and [Documentation](Documentation.html#build-and-preview-locally)
   when changing the website. GitHub Actions runs the configured checks on pushes and pull requests;
   inspect the actual run, including any fork Actions settings, before reporting CI as passed.

PonHub evolved from AddressBook-Level3. Its upstream [tracing](https://se-education.org/guides/tutorials/ab3TracingCode.html),
[adding a command](https://se-education.org/guides/tutorials/ab3AddRemark.html) and
[removing fields](https://se-education.org/guides/tutorials/ab3RemovingFields.html) tutorials are optional background
on that design. Their AddressBook classes and command examples are not the active PonHub command path.
