[![Java CI](https://github.com/AY2627S1-CS2103T-F13-3/tp/actions/workflows/gradle.yml/badge.svg)](https://github.com/AY2627S1-CS2103T-F13-3/tp/actions/workflows/gradle.yml)

# PonHub

PonHub helps tuition centre administrators manage students, tutors, parents, shared recurring lessons and
dated attendance through a keyboard-focused desktop interface.

![Student lessons active beside a filtered Student list, with the bottom command box](docs/images/LessonBoardNavigation.png)

*[Screenshot sources and credits](docs/images/README.md).*

The Lesson board above uses fictitious records. Released v1.5 supports creating, editing and
searching people and shared lessons, enrolment/unenrolment, attendance marking/removal and guarded deletion.
Roster and attendance-history views retain dated records after unenrolment. Person and Lesson edits retain
stable IDs and linked records, with validation and schedule checks protecting changes.

Download **`ponhub.jar`** from the latest published release,
[v1.5](https://github.com/AY2627S1-CS2103T-F13-3/tp/releases/tag/v1.5), and follow its
[version-matched User Guide](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/v1.5/docs/UserGuide.md#quick-start).
Use Java 25 and launch it with `java -jar ponhub.jar` in a writable application folder.
Contributors can [build the current source](docs/DevOps.md#build-automation) to try later development changes.
Use the version-matched guide above when running the released JAR.

* [Current source User Guide and command summary](docs/UserGuide.md#current-command-summary) — commands and examples;
  use `help` or `help COMMAND` in the app for inline guidance.
* [Developer Guide](docs/DeveloperGuide.md)
* [Project website](https://ay2627s1-cs2103t-f13-3.github.io/tp/)
* [Report a problem](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues)

##### Acknowledgements

This project is based on the AddressBook-Level3 project created by the [SE-EDU initiative](https://se-education.org).
