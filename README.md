[![Java CI](https://github.com/AY2627S1-CS2103T-F13-3/tp/actions/workflows/gradle.yml/badge.svg)](https://github.com/AY2627S1-CS2103T-F13-3/tp/actions/workflows/gradle.yml)

# PonHub

Many students fail to live up to their potential. The culprit? An unhealthy amount of pon.

PonHub helps tuition centres keep track of students, manage lesson schedules, and see who actually turns up.
Because some habits need monitoring.

**All your students. All their sessions. One hub.**

![Student lessons active beside a filtered Student list, with the bottom command box](docs/images/LessonBoardNavigation.png)

*Fictitious records. Familiar admin problems. [Screenshot sources and credits](docs/images/README.md).*

## A little less tuition centre chaos

* **Keep your people straight.** Create, edit and search students, tutors and parents.
  Your memory has other things to do.
* **Make room for actual lessons.** Create, edit and search shared recurring lessons, and enrol or unenrol
  students without losing track of who's taking what.
* **Keep receipts for “I was there.”** Mark and remove dated attendance records. Roster and attendance-history
  views retain dated records after unenrolment, because leaving a lesson doesn't rewrite history.
* **Give typos some supervision.** Validation, schedule checks and guarded deletion help protect your records.
  Person and Lesson edits retain stable IDs and linked records, even when the details change.

## Show up for the quick start

1. Download **`ponhub.jar`** from the latest published release,
   [v1.5](https://github.com/AY2627S1-CS2103T-F13-3/tp/releases/tag/v1.5).
2. Make sure you have **Java 25**, and put the JAR in a writable application folder.
3. Open a terminal in that folder and run:

   ```sh
   java -jar ponhub.jar
   ```

Follow the [v1.5 User Guide](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/f76be6739317dc07dad7bb60f621703d67c113a0/docs/UserGuide.md#quick-start)
for the full quick start. Once you're in, `help` or `help COMMAND` provides inline guidance.
Asking for help is allowed. There is no participation grade.

## The reading material

No surprise quiz. Just the links you might need.

* [Current source User Guide and command summary](docs/UserGuide.md#current-command-summary) — commands and examples.
  When running the released JAR, use the version-matched v1.5 guide above.
* [Developer Guide](docs/DeveloperGuide.md) — for those volunteering for more homework.
* [Build the current source](docs/DevOps.md#build-automation) — try later development changes.
* [Project website](https://ay2627s1-cs2103t-f13-3.github.io/tp/) — the front desk.
* [Report a problem](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues) — bugs don't get an excused absence.

## Credit where it's due

PonHub is based on the AddressBook-Level3 project created by the [SE-EDU initiative](https://se-education.org).
They laid the groundwork. We added tuition centre admin problems.
