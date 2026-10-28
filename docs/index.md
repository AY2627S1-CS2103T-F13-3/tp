---
layout: page
title: PonHub
---

[![Java CI](https://github.com/AY2627S1-CS2103T-F13-3/tp/actions/workflows/gradle.yml/badge.svg)](https://github.com/AY2627S1-CS2103T-F13-3/tp/actions/workflows/gradle.yml)
[![codecov](https://codecov.io/gh/AY2627S1-CS2103T-F13-3/tp/branch/master/graph/badge.svg)](https://codecov.io/gh/AY2627S1-CS2103T-F13-3/tp)

PonHub helps tuition centre administrators manage students, tutors, parents, shared recurring lessons and
dated attendance through a keyboard-focused desktop interface.

![Development preview: Student lessons active beside a filtered Student list, with the bottom command box](images/LessonBoardNavigation.png)

*Current development-build preview; public v1.4 has an earlier layout.
[Screenshot sources and credits](images/README.html).*

The Lesson board above uses fictitious records. Released v1.4 supports creating, editing and
searching people and shared lessons, enrolment/unenrolment, attendance marking/removal and guarded deletion.
Roster and attendance-history views retain dated records after unenrolment. Person and Lesson edits retain
stable IDs and linked records, with validation and schedule checks protecting changes.

Download **`ponhub.jar`** from the latest published release,
[v1.4](https://github.com/AY2627S1-CS2103T-F13-3/tp/releases/tag/v1.4), and follow its
[version-matched User Guide](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/v1.4/docs/UserGuide.md#quick-start).
Use Java 25 and launch it with `java -jar ponhub.jar` in a writable application folder.
Contributors can [build the current source](DevOps.html#build-automation) to preview unreleased changes;
the current source guides may describe features absent from the published JAR.

* [Current source User Guide and command summary](UserGuide.html#current-command-summary) — commands and examples;
  use `help` or `help COMMAND` in the app for inline guidance.
* [Developer Guide](DeveloperGuide.html)
* [About Us](AboutUs.html)
* [Report a problem](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues)

**Acknowledgements**

This project is based on the AddressBook-Level3 project created by the [SE-EDU initiative](https://se-education.org).

* Libraries used: [JavaFX](https://openjfx.io/), [Jackson](https://github.com/FasterXML/jackson), [JUnit 5](https://junit.org/junit5/)
