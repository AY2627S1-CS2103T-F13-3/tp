---
layout: page
title: PonHub
---

[![Java CI](https://github.com/AY2627S1-CS2103T-F13-3/tp/actions/workflows/gradle.yml/badge.svg)](https://github.com/AY2627S1-CS2103T-F13-3/tp/actions/workflows/gradle.yml)
[![codecov](https://codecov.io/gh/AY2627S1-CS2103T-F13-3/tp/branch/master/graph/badge.svg)](https://codecov.io/gh/AY2627S1-CS2103T-F13-3/tp)

PonHub helps tuition centre administrators manage students, tutors, parents, shared recurring lessons and
dated attendance through a keyboard-focused desktop interface.

![PonHub Lesson board with a filtered Student list, shared Lesson results, navigation buttons and bottom command box](images/LessonBoardNavigation.png)

The Lesson board above uses fictitious records. The current source build supports creating, editing and
searching people and shared lessons, enrolment/unenrolment, attendance marking/removal and guarded deletion.
Roster and attendance-history views retain dated records after unenrolment. Person and Lesson edits retain
stable IDs and linked records, with validation and schedule checks protecting changes.

For published JARs and version-specific instructions, visit [Releases](https://github.com/AY2627S1-CS2103T-F13-3/tp/releases).
The v1.3 release predates person/Lesson editing and this Lesson board. To use the features described here,
[build the current source](DevOps.html#build-automation) and follow the [Quick start](UserGuide.html#quick-start).

* [User Guide and command summary](UserGuide.html#current-command-summary) — supported commands and examples;
  use `help` or `help COMMAND` in the app for inline guidance.
* [Developer Guide](DeveloperGuide.html)
* [About Us](AboutUs.html)
* [Report a problem](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues)

**Acknowledgements**

This project is based on the AddressBook-Level3 project created by the [SE-EDU initiative](https://se-education.org).

* Libraries used: [JavaFX](https://openjfx.io/), [Jackson](https://github.com/FasterXML/jackson), [JUnit 5](https://junit.org/junit5/)
