---
layout: page
title: Logging guide
---

Use logs to diagnose PonHub startup and runtime failures. For data recovery, follow the
[DG's protected-startup guidance](DeveloperGuide.html#current-protected-startup) and preserve the original files.

* Table of Contents
{:toc}

## Finding startup diagnostics

Launch from a terminal so you can read console output, including errors that occur before a file logger opens.
Use `./gradlew run` for development or `java -jar ponhub.jar` for a built JAR; select the
[required Java 25 setup](SettingUp.html#setting-up-the-project-on-your-computer) first.

| Launch | Log location |
| --- | --- |
| Packaged JAR | The JAR's containing folder, even when the terminal's working directory is elsewhere. |
| Development classes / Gradle `run` | The application's working directory (the repository root for the normal Gradle launch). |

[`LogsCenter`](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/master/src/main/java/ponhub/commons/core/LogsCenter.java)
uses `java.util.logging` under the `ponhub` logger. Console logging is available immediately.
File logging appends to `ponhub.log*`, normally `ponhub.log.0`, rotating at about 5 MiB per file with up to
five files. Java's `FileHandler` may add uniqueness or lock suffixes, so inspect the matching files rather
than relying on one fixed name. Existing `addressbook.log*` files are legacy logs and are left untouched.

`MainApp` checks the selected operational and preferences paths before enabling file logging.
An invalid or overlapping path can therefore leave only console diagnostics. Root `ponhub.log*` and
legacy `addressbook.log*` names are reserved, including case variants, rotation/lock/future suffixes and aliases;
do not select them as data or preferences files. See [startup path rules](DeveloperGuide.html#canonical-application-startup-85).

If opening a file log fails, startup retains console logging and emits
`Unable to initialize file logging; continuing with console logging.` with the cause.
Inspect that cause and the application folder's permissions and existing log paths.
Symbolic `ponhub.log*` paths or new log files aliased to legacy logs also trigger this fallback to preserve
their targets. Do not delete or overwrite an unknown file to silence the warning; resolve the path problem
on a backed-up working copy and restart. Missing file output alone does not mean no diagnostic was recorded.

When reporting a problem, include the source commit or packaged artifact, OS/JDK, launch command,
resolved application/data paths and relevant warning/exception. Logs and paths may contain personal information;
inspect and redact private details before sharing them. Do not attach a real tuition centre's records.

## Adding useful log messages

Obtain one logger with `LogsCenter.getLogger(YourClass.class)` and reuse it; handler configuration belongs
in `LogsCenter`. The default threshold is `Level.INFO`. Developers can temporarily lower its `LOG_LEVEL`
constant (for example, to `Level.FINE`) when diagnosing an issue; it is not a user preference.
Follow the [SE-EDU logging-level conventions](https://se-education.org/guides/conventions/java/logging.html).
Log useful context and exception causes without dumping contact records, complete data files or sensitive
command arguments. Avoid duplicate messages at multiple layers for the same failure.

For focused regression checks, inspect
[`LogsCenterTest`](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/master/src/test/java/ponhub/commons/core/LogsCenterTest.java)
for file output, legacy preservation and console fallback, and
[`ProtectedStartupTest`](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/master/src/test/java/ponhub/ProtectedStartupTest.java)
for data/preferences/log separation. Use the [Testing guide](Testing.html#selecting-tests-during-development)
to run those classes; the [DG manual procedure](DeveloperGuide.html#appendix-instructions-for-manual-testing)
owns packaged launch and recovery checks.
