---
layout: page
title: PonHub
---

[![Java CI](https://github.com/AY2627S1-CS2103T-F13-3/tp/actions/workflows/gradle.yml/badge.svg)](https://github.com/AY2627S1-CS2103T-F13-3/tp/actions/workflows/gradle.yml)
[![codecov](https://codecov.io/gh/AY2627S1-CS2103T-F13-3/tp/branch/master/graph/badge.svg)](https://codecov.io/gh/AY2627S1-CS2103T-F13-3/tp)

![Ui](images/LessonResults.png)



PonHub is being developed for tuition centre administrators to manage people, shared lessons, enrolment and dated attendance through a keyboard-focused desktop interface. The shared-lesson target lets two students join one lesson and retains attendance history after unenrolment.

The current build supports role-aware people addition/listing, own-contact people search and empty-roster
shared-lesson creation, enrolment/unenrolment and dated attendance marking. Type `help` or `help COMMAND` for inline
guidance, also available through the Help menu and F1. `lessons` and all seven lesson-search filters show
separate shared-lesson results. Attendance removal/history remain planned; consult the
[current command summary](UserGuide.html#current-command-summary) before trying the wider workflow.
The UI image shows the current people and lesson panels using disposable sample data.

* [User Guide](UserGuide.html#current-command-summary)
* [Developer Guide](DeveloperGuide.html)
* [About Us](AboutUs.html)

**Acknowledgements**

This project is based on the AddressBook-Level3 project created by the [SE-EDU initiative](https://se-education.org).

* Libraries used: [JavaFX](https://openjfx.io/), [Jackson](https://github.com/FasterXML/jackson), [JUnit5](https://github.com/junit-team/junit5)
