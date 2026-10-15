---
layout: page
title: User Guide
---

PonHub is a desktop app for tuition centre administrators to manage student, tutor and parent records and create
shared recurring lessons. Its command box lets you work quickly from the keyboard while the graphical interface
shows separate people and shared-lesson results. Shared-lesson enrolment, dated attendance marking and guarded
person/lesson deletion, dated attendance removal and attendance history are active.

* Table of Contents
{:toc}

--------------------------------------------------------------------------------------------------------------------

## Quick start

1. Ensure that Java `25` is installed on your computer.<br>
   **Mac users:** Install Azul JDK 25 with JavaFX (`25.0.3.fx-zulu`) as prescribed by the [course's Mac advisory](https://nus-cs2103-ay2627-s1.github.io/website/admin/programmingLanguages.html) and [Mac installation guide](https://se-education.org/guides/tutorials/javaInstallationMac.html). Use this installation in the terminal that launches PonHub.

1. Use the JAR built from this increment, or download a matching PonHub build from the [project's Releases page](https://github.com/AY2627S1-CS2103T-F13-3/tp/releases) after publication. Contributors can build `build/libs/ponhub.jar` with `./gradlew shadowJar`; see [DevOps](DevOps.html#build-automation). An older released JAR may have different commands.

1. Put the JAR file in the folder you want to use for PonHub. Keep this folder when moving or backing up your data.

1. Open a terminal in that folder, confirm `java -version` reports Java 25, and run `java -jar FILENAME.jar`, replacing `FILENAME.jar` with the downloaded file's name. The PonHub window should open.

1. A missing `data/ponhub.json` starts an empty people list. Enter a command in the command box and press Enter. Start with `help`, then `help add`:

   * `add r/tutor n/Mei Lim p/92345678 e/mei@example.com` adds a tutor.
   * `addlesson d/Mon st/1600 et/1730 s/Math tu/Mei Lim rm/R1` creates an empty-roster shared lesson.
   * `add r/student n/Alex Tan l/S2 pp/91234567` adds a student.
   * `add r/student n/Jamie Tan l/P1 pp/91234567` adds a second student.
   * `list r/student` shows Alex at position 1 and Jamie at position 2 in this fresh dataset.
   * `enrol 1 lid/L1`, then `enrol 2 lid/L1`, enrols both students in the same lesson.
   * `lessons` shows lesson `L1` once with a roster size of 2; `lessons si/1` shows Alex's current lessons.
   * `mark 1 lid/L1 d/2026-10-05 s/present`, then `mark 2 lid/L1 d/2026-10-05 s/absent`,
     records independent attendance for the same Monday occurrence.
   * `mark 1 lid/L1 d/2026-10-05 s/absent` corrects only Alex's record.
   * `unenrol 1 lid/L1` removes Alex's membership while keeping both attendance records.
     `lessons si/1` now shows no current lessons; `lessons` still shows `L1` with Jamie enrolled.
   * `history 1 lid/L1` shows Alex's retained absence with former-member context after unenrolment.
   * `unmark 1 lid/L1 d/2026-10-05` removes Alex's retained record after unenrolment, keeping Jamie's record and membership.
     Repeating it reports no change.
   * `deletelesson lid/L1` is blocked because Jamie's membership and attendance still reference `L1`.
   * `addlesson d/Tue st/1600 et/1730 s/Math tu/Mei Lim rm/R1` creates an unreferenced lesson `L2`.
     `deletelesson lid/L2` removes it; `L1` and both people records remain.
   * `add r/parent n/Pat Tan p/91234567` adds a parent.
   * `list r/student` displays students; `list` displays all roles.
   * `help ADD` shows the same guidance as `help add`.
   * `exit` closes PonHub.

The first successful mutation creates the data file. Keep the same JAR folder to load the saved records and stable IDs after restart. Reopen it, run `list r/student`, then `lessons`: Alex and Jamie retain their IDs, and `L1` still has Jamie enrolled. `history 2 lid/L1` shows Jamie's retained dated attendance. These example positions and `L1` assume a fresh dataset; with existing data, use the returned Lesson ID and recheck current people positions before each student command.

For a separate store, launch with `java -jar FILENAME.jar --data=data/another-ponhub.json`. Quote an argument containing spaces, for example `java -jar FILENAME.jar --data="separate store/ponhub.json"`. Select a previously unused path when re-entering legacy records; see [Data protection](#data-protection-in-the-current-build).

## Current command summary

The active routes use one saved aggregate with role-specific people, stable IDs and shared lessons whose rosters
can be changed with `enrol` and `unenrol`. Own-contact people search, attendance marking, guarded person and Lesson
deletion, lesson listing, lesson search, lesson details, attendance removal and attendance history are active,
alongside Student, Parent and Tutor relationship search. Their specifications follow under
[Shared-lesson workflow](#planned-shared-lesson-workflow). Type `help` to inspect the commands available
in your session. `edit`, `clear` and `find` are unavailable.

| Action | Format | Example |
| --- | --- | --- |
| Add student | `add r/student n/NAME l/LEVEL pp/PARENT_PHONE [p/PHONE] [e/EMAIL] [a/ADDRESS]` | `add r/student n/Alex Tan l/S2 pp/91234567` |
| Add tutor or parent | `add r/ROLE n/NAME p/PHONE [e/EMAIL] [a/ADDRESS]` | `add r/tutor n/Mei Lim p/92345678` |
| Create shared lesson | `addlesson d/DAY st/HHMM et/HHMM s/SUBJECT tu/TUTOR_NAME [tp/TUTOR_PHONE] rm/ROOM` | `addlesson d/Mon st/1600 et/1730 s/Math tu/Mei Lim rm/R1` |
| Enrol student | `enrol STUDENT_INDEX lid/LESSON_ID` | `enrol 1 lid/L1` |
| Unenrol student | `unenrol STUDENT_INDEX lid/LESSON_ID` | `unenrol 1 lid/L1` |
| Mark attendance | <code>mark STUDENT_INDEX lid/LESSON_ID d/YYYY-MM-DD s/present&#124;absent</code> | `mark 1 lid/L1 d/2026-10-05 s/present` |
| Unmark attendance | `unmark STUDENT_INDEX lid/LESSON_ID d/YYYY-MM-DD` | `unmark 1 lid/L1 d/2026-10-05` |
| Delete person | `delete INDEX` | `delete 1` |
| Delete shared lesson | `deletelesson lid/LESSON_ID` | `deletelesson lid/L1` |
| List people | `list [r/ROLE]` | `list r/student` |
| Search people | `search c/CATEGORY [FILTER/VALUE]...` | `search c/student n/Alex d/Mon s/Math` |
| List shared lessons | `lessons [si/STUDENT_INDEX]` | `lessons` |
| Attendance history | `history STUDENT_INDEX [lid/LESSON_ID]` | `history 1 lid/L1` |
| Show lesson details | `showlesson lid/LESSON_ID [d/YYYY-MM-DD]` | `showlesson lid/L1 d/2026-10-05` |
| Search shared lessons | `search c/lesson [sn/NAME] [d/DAY] [st/HHMM] [et/HHMM] [s/SUBJECT] [tu/TUTOR_NAME] [rm/ROOM]` | `search c/lesson tu/mei d/Mon` |
| Get help | `help [COMMAND]` | `help add` |
| Exit | `exit` | `exit` |

Cards display a current position separately from the stable ID, such as `S1`. Addition allocates that ID; do not supply it as an add argument. Command names and prefixes are lowercase, each prefix may appear only once, and role values ignore case. Optional values must be nonblank. `exit` accepts no arguments.

### Viewing command help: `help`

**Format:** `help [COMMAND]`

`help` shows the current supported-command catalogue in Result Display. In a normal session, `help add`,
`help addlesson`, `help enrol`, `help unenrol`, `help mark`, `help unmark`, `help delete`, `help deletelesson`,
`help list`, `help lessons`, `help showlesson`, `help history`, `help search`, `help help` and `help exit` show usage guidance.
Topic names are case-insensitive, so `help ADD` is
accepted; the command word `help` remains lowercase. Asking for exit guidance does not exit the application.

Choose **Help > Help** or press **F1** for the same catalogue, including when focus is in the command box or Result Display. Menu/F1 preserve unfinished command text. Help preserves records, the active person filter, order, selection and preferences. Typed help clears the submitted command in the usual way. Help needs no internet connection and does not save operational data.

Long guidance wraps within Result Display. Scroll vertically to read the rest; each new result starts at the top.

* `help addlesson`, `help enrol`, `help unenrol`, `help mark`, `help unmark` and `help showlesson` show their active syntax and examples. `help remove` returns
  `Unknown command: 'remove'. Type 'help' to see available commands.`
* `help add delete` returns `Invalid command format!` followed by `Usage: help [COMMAND]` and its explanation. Supply at most one topic.
* `edit`, `clear` and `find` are absent from the catalogue. For example, `edit` returns
  `Unknown command: 'edit'. Type 'help' to see available commands.` No data changes.
* In a protected session, help lists only `help`, `list` and `exit`; `help add`, `help addlesson`, `help enrol`,
  `help unenrol`, `help mark`, `help unmark`, `help history`, `help delete`, `help deletelesson`, `help lessons`, `help showlesson`
  and `help search`
  are unavailable.

### 1. Adding a person: `add`

Creates a student, tutor or parent record.

#### Format

* **Student:** `add r/student n/NAME l/LEVEL pp/PARENT_PHONE [p/PHONE] [e/EMAIL] [a/ADDRESS]`
* **Tutor:** `add r/tutor n/NAME p/PHONE [e/EMAIL] [a/ADDRESS]`
* **Parent:** `add r/parent n/NAME p/PHONE [e/EMAIL] [a/ADDRESS]`

#### Parameters

| Parameter | Description and accepted values |
|-----------|---------------------------------|
| `r/ROLE` | Required. Use `student`, `tutor` or `parent`. Values are case-insensitive. |
| `n/NAME` | Required. Between 1 and 100 characters using English letters (`A`–`Z`, `a`–`z`), spaces, apostrophes, hyphens or periods, with at least one letter. Surrounding spaces are removed and repeated spaces are reduced to one before measuring length. Letter case is preserved. Tabs and line breaks are not allowed. |
| `l/LEVEL` | Required for students; not allowed for tutors or parents. Use `P1`–`P6`, `S1`–`S5`, `JC1` or `JC2`. Values are case-insensitive and displayed in uppercase. |
| `pp/PARENT_PHONE` | Required for students; not allowed for tutors or parents. Use 3–15 digits without spaces or punctuation. |
| `p/PHONE` | Required for tutors and parents; optional for students. Use 3–15 digits without spaces or punctuation. Leading zeros are retained. |
| `e/EMAIL` | Optional. Use an email address such as `mei@example.com`, with no spaces and at most 254 characters. |
| `a/ADDRESS` | Optional. Between 1 and 200 printable ASCII characters. Slashes, tabs and line breaks are not allowed. Surrounding spaces are removed and repeated spaces are reduced to one before measuring length. |

For email addresses, the local part before `@` may contain English letters, digits, periods, underscores, `%`, `+` and `-`. It must not begin or end with a period or contain consecutive periods. Domain labels may contain English letters, digits and hyphens. The domain must contain at least two dot-separated labels, with no leading or trailing hyphens in a label. Its final label must contain 2–63 English letters. Email spelling and letter case are preserved.

**Current behavior:** The role-specific formats and contact rules above are active. Each prefix is allowed once; unknown, repeated, missing or blank fields are rejected. `l/` and `pp/` are student-only. Tags and the inherited contact-only add format are unavailable. Saved records must satisfy the same validation rules when loaded.

**Testing older data:** Back up the data file before testing this increment and use a working copy. If an older file fails validation, the app starts a [protected session](#data-protection-in-the-current-build): the empty view is not your saved data, only `help`, `list` and `exit` are available, and the original file is preserved. Close the app before restoring or deliberately correcting a compatible file, then restart.

#### Examples

* `add r/student n/Alex Tan l/S2 pp/91234567`<br>
  Adds Alex Tan as a Secondary 2 student with parent contact number 91234567.
* `add r/tutor n/Mei Lim p/92345678 e/mei@example.com`<br>
  Adds Mei Lim as a tutor with a phone number and email address.
* `add r/parent n/Pat Tan p/91234567`<br>
  Adds Pat Tan as a parent.

#### Expected result

PonHub saves the new record and displays a confirmation such as:
`New student added: Alex Tan (S1).`

The person list resets to show all roles, with the new record at the end. Recheck its displayed positions before selecting a person in your next command. A new student starts with no lessons or attendance records.

#### Things to note

* A separate parent record is optional. You can add a student using their parent's phone number before creating a parent record.
* A duplicate has the same role, normalized name and contact number. For students, the contact number used for this check is `pp/`; for tutors and parents, it is `p/`.
* Duplicate name comparisons ignore letter case and repeated spaces.
* Siblings with different names may share the same parent phone number.
* Changing an optional email, address or student phone number does not make an otherwise duplicate record unique.

For example, a duplicate student is rejected with:
`A student with the same name and parent phone already exists.`

Validation, duplicate and save failures preserve the existing data, allocation counters, role filter and selected card. Success is reported only after saving; a failed addition consumes no ID.

### 2. Listing people: `list`

Displays all people or only people with a specified role, including each current command index and separately labelled stable ID.

#### Format

`list [r/ROLE]`

#### Examples

| Command | Result |
|---------|--------|
| `list` | Displays everyone and clears previous person filters. |
| `list r/student` | Displays students only. |
| `list r/tutor` | Displays tutors only. |
| `list r/parent` | Displays parents only. |

#### Expected result

People appear in creation order, numbered from 1 within the displayed result. Each card shows that current command index and separately identifies its role and stable ID. For example, a student with ID `S12` may appear at position `1`; student commands then use `1`. Filtering renumbers positions without changing IDs.

Example feedback:
`Showing 3 student(s).`

An empty result is valid. PonHub reports a count of zero and the people panel shows `No persons to display.`

#### Things to note

* Use the singular role names `student`, `tutor` and `parent`. To display everyone, enter `list`; `list r/all` is invalid.
* Listing changes the displayed view without modifying stored records.
* Use positions from the latest people list or people search. A previous position may select someone else after the view changes or a person is deleted.

--------------------------------------------------------------------------------------------------------------------

### Searching people: `search`

**Formats:**

* `search c/student [n/NAME] [l/LEVEL] [p/PHONE] [pp/PARENT_PHONE] [e/EMAIL] [a/ADDRESS] [d/DAY] [st/HHMM] [et/HHMM] [s/SUBJECT] [tu/TUTOR_NAME] [rm/ROOM]`
* `search c/parent [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [sn/STUDENT_NAME] [d/DAY] [st/HHMM] [et/HHMM] [s/SUBJECT] [tu/TUTOR_NAME] [rm/ROOM]`
* `search c/tutor [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [sn/STUDENT_NAME] [d/DAY] [st/HHMM] [et/HHMM] [s/SUBJECT] [rm/ROOM]`

Category values ignore case. Supply each lowercase prefix once with a nonblank value. All supplied filters must
match the same person. Name, email and address use case-insensitive text fragments; name/address queries trim and
collapse repeated spaces. Levels and complete 3–15 digit phones match exactly, including leading zeros.
Missing optional contacts do not match a supplied filter. Category-only searches include unlinked people.

After adding the Quick start records, `search c/student n/alex l/s2 pp/91234567` reports `Found 1 student(s).`
and displays Alex at position 1. `search c/tutor n/mei e/@example` displays Mei; `search c/parent p/91234567`
displays Pat. Results retain creation order, show each stable ID once, and renumber current positions from 1.
`search c/student n/No Match` is a successful empty view with feedback `Found 0 student(s).`

Search reads the complete people catalogue, replacing the previous role/search filter. `list` or `list r/ROLE`
clears search criteria. Use the new displayed positions for subsequent person commands. Search/help do not save
operational data; failed commands preserve the previous view. A successful person addition resets to all roles.

Student, Parent and Tutor relationship filters are active according to the category matrix.
All seven lesson fields remain available for `c/lesson`. Category-incompatible fields, unknown/repeated/blank prefixes
and invalid values fail validation.
The [search matrix](#6-searching-people-and-shared-lessons) describes the complete target.

For example, `search c/student n/Alex d/Mon s/Math tu/mei` requires Alex's own name and **one currently enrolled
Monday Math lesson** taught by a matching tutor. Math on Tuesday and Science on Monday cannot combine into a match.
`tu/` matches the tutor's name, and `st/`/`et/` each match exact HHMM times. All supplied lesson conditions must hold
on that same lesson. A former member's attendance does not imply current enrolment.

Each student appears once by stable ID, with matching lesson IDs, subjects, schedules, tutors and rooms beneath
their own details. Several matching lessons add context lines, never duplicate student cards. Enrolment changes
refresh both matches and context; failed saves preserve them. Own-field-only searches still include students with
no lessons and show no relationship context. `list` clears both the criteria and its matching context.

### Searching parents through their children

A Parent is linked to every Student whose stored parent phone exactly equals that Parent's own phone, including
leading zeros. Names, email and addresses never establish this link, and a separate Parent record is optional.
`search c/parent sn/Alex d/Mon s/Math` requires **one linked child named Alex and one Monday Math lesson in which
that same child is currently enrolled**. Alex in Science and Jamie in Math cannot combine into a match; nor can
Math on Tuesday and Science on Monday. All own-contact conditions still apply to the Parent.

With only `sn/`, a matching child need not have lessons. With only Parent own fields, unlinked Parents remain
eligible. Each stable Parent ID appears once with matching Student IDs/names and, when lesson filters are supplied,
matching lesson details beneath the card. Different Parent records sharing an exact number may legitimately link
to the same children. Empty search results have `Found 0 parent(s).` feedback. Student operations still require a
Student in the **current people list**: run `list r/student` before enrol/unenrol, then repeat the Parent search.

### Searching tutors through assigned lessons

`search c/tutor n/mei sn/Alex d/Mon s/Math rm/r1` requires one lesson assigned to that Tutor's stable ID, matching
all supplied lesson conditions, and a matching Student in that same lesson's current roster. Another assigned
lesson cannot contribute its day, subject or roster. `n/` filters the Tutor's own name; `tu/` is rejected in this
category. Tutors with the same name remain separate results identified by stable IDs.

Without `sn/`, empty assigned lessons can match; own-field-only queries also include Tutors without lessons.
Each Tutor appears once with matching lesson details and, when `sn/` is supplied, matching Student context.
Enrolment changes refresh current roster matching; retained attendance does not make a former member match.
To change a student's membership, first display Students and use their current people-list positions, then
repeat the Tutor query. Listing clears the relationship criteria and context.

### Listing and searching shared lessons

**Formats:**

* `lessons [si/STUDENT_INDEX]`
* `search c/lesson [sn/STUDENT_NAME] [d/DAY] [st/HHMM] [et/HHMM] [s/SUBJECT] [tu/TUTOR_NAME] [rm/ROOM]`

The right-hand lesson panel initially shows the full catalogue. `lessons` restores it after a lesson search or
student selection. Every shared lesson appears once in creation order, including empty rosters. Cards show its
stable Lesson ID, subject, weekday and time, tutor name and ID, room and the number of currently enrolled students.
The lesson heading identifies the selected Student ID or active filters even after another command changes
the feedback. Scroll either panel independently; drag the divider to give long values more room. Labels wrap within their cards.

To show one student's current lessons, find their position in the **current people panel**, then enter
`lessons si/POSITION`. For example, after `list r/student`, use `lessons si/1` only if the intended student is
at position 1. This selects the visible person once. A stable ID such as `S1`, a lesson ID, zero, negative or
out-of-range position, repeated `si/`, or a position occupied by a tutor/parent is rejected. Changing the people
filter later does not change which student's lessons are being shown. Attendance retained after unenrolment
does not make a lesson part of that student's current enrolments; use `history` to inspect retained records.

Lesson searches always read the complete catalogue. All supplied filters must match **one lesson**. `sn/`
checks its current roster; without `sn/`, empty lessons can match. Text filters match fragments ignoring case.
`tu/mei` means a tutor name, not Tutor ID `T1`. `d/` uses Mon–Sun ignoring case; `st/` and `et/` each match an
exact four-digit 24-hour time. Either time may be supplied alone; together, end must be later than start.
The [search matrix](#6-searching-people-and-shared-lessons) describes each field.

Examples, after creating the Math lesson above:

* `search c/lesson tu/MEI d/mon s/ath` finds it even with no students enrolled.
* `search c/lesson st/1600 et/1730 rm/r1` matches its exact times and room fragment.
* `search c/lesson sn/Alex d/Mon s/Math` finds it only when Alex is currently enrolled.
* `search c/lesson sn/No Match` succeeds with `Found 0 lesson(s).` and `No lessons to display.`

`lessons` reports `Showing N lesson(s).`; lesson search reports `Found N lesson(s).`. These commands preserve
people filters, order, selection and command positions, and never save operational data. Invalid commands leave
both views intact. Committed changes refresh the selected results; a failed save leaves the previous cards intact.

![Separate people and shared-lesson results](images/LessonResults.png)

The screenshot uses disposable sample data and shows one shared lesson with two students and an empty lesson.

## Data protection in the current build

The current build stores people, shared lessons, rosters and their allocation state in `data/ponhub.json` inside the folder containing the JAR. `--data=PATH` selects a different file within that folder or its subfolders; relative paths start from the JAR folder. Absolute paths are accepted only within that folder. Blank paths and paths escaping it, including through symbolic links, are rejected at startup. Select a separate data file: `preferences.json` and the root `addressbook.log*` logging files are reserved, including aliases to them. Window preferences are saved separately in `preferences.json` in the JAR folder; selecting a different store does not change the preference file.

A missing selected file starts a normal empty session and remains missing until the first successful mutation.
`help`, `list`, `lessons`, `showlesson`, `history`, `search` and `exit` do not create or rewrite operational data. An invalid, unreadable, corrupt,
unsupported or legacy selected file produces a **Data file protected** warning. The empty list in that session is
unavailable saved data: it does **not** mean the original records were deleted. Only `help`, `list` and `exit` are
available, and operational writes are blocked.

To recover:

1. Close PonHub and preserve a backup of the original file before making changes.
2. For a supported file, restore a known-good compatible copy or deliberately correct its JSON/access permissions after inspecting the error. Keep stable IDs, relationships and allocation counters valid.
3. Restart PonHub and check the loaded records. Repairing or removing the file while the app is running does not unlock that session. Do not delete the original to dismiss the protection.

The default startup leaves an older `data/addressbook.json` untouched. Selecting an unversioned AB3 file with `--data` rejects it and preserves its bytes; PonHub does not guess roles or migrate it automatically. Back it up, choose a separate previously unused store inside the JAR folder, such as `java -jar FILENAME.jar --data="separate store/ponhub.json"`, then manually add each person with an explicit role. There is no importer or setup command. Verify the new records before relying on the new store.

<a id="planned-shared-lesson-workflow"></a>

## Shared-lesson workflow

The following sections record the shared-lesson and guarded-deletion contracts. Role-aware add/list,
`addlesson`, `enrol`, `unenrol`, `mark`, `unmark`, `delete`, `deletelesson`, `lessons`, own-contact people search,
lesson details, history, lesson search and Student, Parent and Tutor relationship search are active.
Use the [current command summary](#current-command-summary) and your build's `help` for active routes.

<a id="reading-the-planned-command-formats"></a>

### Reading the command formats

* Uppercase words are placeholders; replace them with your values. Square brackets indicate optional input and are not typed.
* `INDEX` is a positive integer identifying a person's position in the currently displayed, filtered people list. `STUDENT_INDEX` uses that same list and must select a student, even when other roles are displayed. Run `list` or a people search and check the current positions before selecting a person.
* Person cards distinguish their numbered command position from their stable ID: `S1` for a student, `T1` for a tutor or `P1` for a parent. Filtering and deletion can change positions; stable IDs and stored relationships survive those changes and restarts. Use a current position for `delete` and student operations.
* Create a lesson by supplying an existing tutor's full name with `tu/TUTOR_NAME` and, optionally, their phone with `tp/TUTOR_PHONE`. Search uses `tu/QUERY` for a fragment of the tutor's name instead.
* Shared lessons have stable IDs such as `L1`. Use `lid/LESSON_ID` wherever a command selects a lesson; there is no per-student lesson index.
* Lesson catalogue, roster and history results preserve the current people list, its filter and its command positions. Select a student from the people list, rather than a position in those other results.
* `st/HHMM` and `et/HHMM` use four-digit 24-hour times. `d/` takes a weekday for lesson creation/search and a real `YYYY-MM-DD` date for attendance or dated lesson details. `s/` takes a subject for creation/search and `present` or `absent` for `mark`.
* Type command names and prefixes in lowercase. Supply each prefix at most once, in any order. Omit unknown optional fields; do not supply blank values.

<a id="planned-quick-start"></a>

### Shared-lesson example

All commands in this workflow are active. Start with a fresh dataset. `addlesson` returns the
allocated lesson ID; substitute it for `L1` in later commands if necessary.
The `list r/student` result below places Alex at position 1 and Jamie at position 2. Recheck those positions when
using an existing dataset.

```text
add r/tutor n/Mei Lim p/92345678
add r/student n/Alex Tan l/S2 pp/91234567
add r/student n/Jamie Tan l/S2 pp/91234567
addlesson d/Mon st/1600 et/1730 s/Math tu/Mei Lim rm/R1
lessons
list r/student
enrol 1 lid/L1
enrol 2 lid/L1
search c/lesson
mark 1 lid/L1 d/2026-10-05 s/present
mark 2 lid/L1 d/2026-10-05 s/absent
showlesson lid/L1 d/2026-10-05
unenrol 2 lid/L1
history 2 lid/L1
mark 2 lid/L1 d/2026-10-05 s/present
unmark 2 lid/L1 d/2026-10-05
```

Expect one shared lesson with two students before unenrolment. Lesson search and history results preserve Alex and
Jamie's positions in the people list. Jamie's dated history remains correctable and removable after leaving. Restart
and verify the same stable IDs, roster and remaining history; run `list r/student` again before selecting a student by
position. No record means unrecorded, rather than absent.

### 3. Deleting a person: `delete`

**Active.** Removes one unreferenced person selected by their current displayed index in the people list.

#### Format

`delete INDEX`

#### Example

1. `list r/parent`
2. Check the intended parent's current position. If Pat Tan is at position 1 in this result, enter `delete 1`.

#### Expected result

PonHub displays a confirmation such as:
`Deleted parent: Pat Tan (P1).`

The current filter is preserved. Remaining people keep their IDs and their displayed positions are renumbered.

#### Restrictions

* `INDEX` must be a positive integer within the current people list. A stable person ID such as `P1` is not a delete selector.
* Only one person can be deleted per command.
* Student deletion is blocked while enrolled or referenced by attendance. Tutor deletion is blocked while any lesson references the tutor, including a lesson with an empty roster. Linked records are not deleted automatically; removing a student never removes a shared lesson.
* Deleting a separate parent record does not erase the parent phone number stored on a student's record.

If dependencies prevent deletion, feedback identifies the Student or Tutor and the blocking Lesson ID. Retained
attendance also identifies its date, for example:
`Student S1 cannot be deleted while retained attendance exists for lesson L1 on 2026-10-05.`

Unenrolment removes current membership but keeps dated attendance, which continues to block Student deletion.
Use `unmark STUDENT_INDEX lid/LESSON_ID d/YYYY-MM-DD` to remove a dated Attendance dependency before retrying
Student deletion. Use `deletelesson lid/LESSON_ID` only after clearing the Lesson's roster and attendance.

Successful deletion preserves the role or own-contact search filter and renumbers remaining positions. Surviving
stable IDs, lesson/attendance records and Student parent-phone values are unchanged. Deleted IDs are never reused,
including after restart. Invalid selectors, blocked deletions and failed saves preserve the saved file and current view.
If the lesson panel shows the deleted student's current lessons, it stays empty and identifies that same Student ID
until you run another `lessons` or lesson-search command. Deletion never switches it to a different student.
Check the updated people list before deleting another person.

### 4. Creating shared lessons and other lesson operations

**Create (active):** `addlesson d/DAY st/HHMM et/HHMM s/SUBJECT tu/TUTOR_NAME [tp/TUTOR_PHONE] rm/ROOM`

Example: `addlesson d/Mon st/1600 et/1730 s/Math tu/Mei Lim rm/R1`.

The tutor must already exist. `tu/` matches the tutor's complete name, ignoring letter case and repeated spaces. If several tutors share that name, supply their exact phone number, for example `tu/Mei Lim tp/92345678`. An ambiguous name without `tp/` is rejected. A supplied phone must match the named tutor exactly, even when the name is unique; an unmatched name or phone is rejected.

Use `Mon` through `Sun` and four-digit 24-hour HHMM times. Start must be earlier than end on the same day. A successful addition returns a stable lesson ID, such as `L1`, with an empty roster. Creation does not select or enrol a student. Tutor and room bookings must not overlap another lesson on that weekday; adjacent intervals are allowed. Adding more students to this lesson does not book the tutor or room again.

**Delete (active):** `deletelesson lid/LESSON_ID`, for example `deletelesson lid/L1`.

`LESSON_ID` is the stable ID shown on the lesson card. Do not use a lesson search position or a Student's lesson
position. Deletion is blocked while the roster is non-empty or any attendance refers to the Lesson, including
history retained after unenrolment. Feedback identifies either the roster Student IDs to unenrol or the first
Student/date attendance dependency to unmark. The command never removes Students, memberships or attendance
automatically.

Once every Student has been unenrolled and every dated attendance record has been unmarked, deletion removes only
that Lesson. Its tutor and room times become available again. Surviving Lesson IDs do not change, and a later
`addlesson` receives a new higher ID rather than reusing the deleted ID. A missing Lesson, invalid command, blocked
deletion or failed save preserves the saved file, schedule, allocation counter and current people/lesson views.
Cancelling one occurrence and editing a Lesson are future scope.

**Catalogue (active):** `lessons [si/STUDENT_INDEX]`.

Enter `lessons` to view each shared lesson once, including lessons with empty rosters. To view a student's currently enrolled lessons, first run `list r/student` and check their current position. If the intended student appears at position 1, enter `lessons si/1`. Catalogue results preserve the current people list and its indices.

The catalogue and student lesson results use lesson creation order. Student lessons show current enrolments;
past attendance alone does not add a lesson to that list. Use `history` to inspect attendance retained after unenrolment.

**Details (active):** `showlesson lid/LESSON_ID [d/YYYY-MM-DD]`.

For example, `showlesson lid/L1` displays the lesson's stable ID, subject, schedule, tutor name and ID, room and
current roster, with each Student's stable ID and name. The lesson details use a separate scrolling panel; no
roster row number is a person-command selector. `showlesson lid/L1 d/2026-10-05` also shows each current roster
student's `present`, `absent` or `unrecorded` status for that real, matching-weekday date. `Unrecorded` means no
attendance entry exists; it is never treated as absent. Retained records for former members appear in a separate
section, limited to the supplied date when one is given. Without a date, that section shows all retained
former-member records, newest first. The two prefixes may appear in either order, for example
`showlesson d/2026-10-05 lid/L1`. An empty roster is valid.

An unknown lesson ID, invalid date or date on another weekday leaves the previous lesson result visible. Lesson
details refresh after saved membership and attendance changes, including corrections and removal by `unmark`.
Deleting the selected unreferenced Lesson clears its detail and identifies the unavailable ID. A failed save leaves
the displayed detail unchanged. `lessons` or lesson search restores the catalogue
view. `showlesson` preserves the current people filter, selection and command positions and does not save data.

### 5. Enrolling and unenrolling students

Both commands are active.

**Enrol (active):** `enrol STUDENT_INDEX lid/LESSON_ID`.

Run `list r/student` and check the intended student's current position. If the student appears at position 1 and the lesson ID is `L1`, enter `enrol 1 lid/L1`.

The index is resolved once from the current displayed people list, so it can differ from the number in the Student's
stable ID. Both records must exist and the selected person must be a Student. Duplicate enrolment and overlap with
that Student's other enrolled lessons are rejected. Adjacent times and equal times on different weekdays are allowed.
A second Student can join the same shared lesson without creating another lesson or tutor/room booking. Success
reports both stable IDs, for example `Student S3 enrolled in lesson L1.`

**Unenrol (active):** `unenrol STUDENT_INDEX lid/LESSON_ID`.

Run `list r/student` and recheck the student's position. If the intended student appears at position 1, enter `unenrol 1 lid/L1` for lesson `L1`.

This removes current membership and retains dated attendance. It does not delete the lesson or Student. A Student
who is not currently in that roster is rejected without change. Both commands preserve the current people filter,
order and positions, and refresh derived roster/student-lesson results. Invalid input, validation failure and save
failure leave the aggregate, saved file and active views unchanged.

### 6. Searching people and shared lessons

**Format:** `search c/CATEGORY [FILTER_PREFIX/VALUE]...`

Use `student`, `parent`, `tutor` or `lesson`; category values are case-insensitive. A category-only search imposes no filters. For example, `search c/lesson` shows each shared lesson once, including empty rosters.

The full target filter matrix is below. Own-contact people search and **all seven Lesson-column fields are active**. Student, Parent and Tutor relationship fields are active according to this matrix. Check `help search` for the active formats.

| Prefix | Field | Student | Parent | Tutor | Lesson |
| --- | --- | --- | --- | --- | --- |
| `n/` | Result person's name | Yes | Yes | Yes | — |
| `l/` | Student's education level | Yes | — | — | — |
| `p/` | Result person's own phone | Yes | Yes | Yes | — |
| `pp/` | Student's parent phone | Yes | — | — | — |
| `e/` | Result person's email | Yes | Yes | Yes | — |
| `a/` | Result person's address | Yes | Yes | Yes | — |
| `sn/` | Associated student's name | — | Yes | Yes | Yes |
| `d/` | Lesson weekday | Yes | Yes | Yes | Yes |
| `st/` | Lesson start time | Yes | Yes | Yes | Yes |
| `et/` | Lesson end time | Yes | Yes | Yes | Yes |
| `s/` | Lesson subject | Yes | Yes | Yes | Yes |
| `tu/` | Lesson tutor's name | Yes | Yes | — | Yes |
| `rm/` | Lesson room | Yes | Yes | Yes | Yes |

Name, email, address, associated student name, subject, tutor name and room filters match case-insensitive text fragments. For example, `n/al` can match Sally Tan and `e/@example` can match `mei@example.com`. `tu/mei` searches tutor names, not tutor IDs; use `n/mei` when the result category is `tutor`.

Phone and parent-phone filters match the complete 3–15-digit number, including leading zeros. Level and weekday filters match the complete allowed value, ignoring case. Use `P1`–`P6`, `S1`–`S5`, `JC1` or `JC2` for `l/`, and `Mon` through `Sun` for `d/`. `st/` and `et/` each match an exact four-digit HHMM time. Either time filter may be used alone; when both are supplied, end must be later than start on the same day.

All supplied filters must match together:

* **Student:** Own-field filters apply to that student. All lesson filters must match one lesson in which the student is currently enrolled. Math on Tuesday and Science on Monday do not satisfy `s/Math d/Mon`.
* **Parent:** Own-field filters apply to the parent. Linked students have a parent phone exactly equal to that parent's own phone. One linked student must match `sn/`, if supplied; all lesson filters must match one lesson of that same student. Different siblings or lessons cannot contribute separate parts of a match.
* **Tutor:** Own-field filters apply to the tutor. All lesson filters must match one lesson assigned to that tutor; `sn/`, if supplied, must match a student enrolled in that same lesson. Different assigned lessons cannot contribute separate parts of a match.
* **Lesson:** All filters apply to one shared lesson. `sn/`, if supplied, matches a student in its current roster. Empty lessons remain discoverable when no `sn/` filter is supplied.

People with no matching relationship still qualify for own-field-only searches. Each matching person or lesson appears once, distinguished by stable ID. An absent optional contact field does not match a supplied filter. Unsupported, category-incompatible, blank and repeated filters are rejected, as are slashes or line breaks in values. A valid search with no matches reports an empty result.

Lesson results retain catalogue creation order. For example, `search c/lesson d/Mon s/Math sn/al` requires one Monday
Math lesson with a currently enrolled student whose name contains `al`. A former member's attendance does not satisfy
`sn/`, and several matching students in one lesson still produce one result. After a successful change or rollback,
displayed lesson and roster results must reflect the resulting current data.

Examples:

* `search c/student n/Alex l/S2 pp/91234567`
* `search c/parent sn/Jamie d/Mon s/Math`
* `search c/tutor n/mei sn/Alex rm/R1`
* `search c/lesson tu/mei d/Mon st/1600 s/Math`

People searches replace the displayed people list and number its results from 1. Use those current positions for subsequent person commands. Lesson searches display separate results and preserve the current people list, its filter and its indices; lesson results do not supply person command indices.

### 7. Recording attendance and retrieving history

**Mark (active):** `mark STUDENT_INDEX lid/LESSON_ID d/YYYY-MM-DD s/present|absent`

Example: Run `list r/student`. If the intended student appears at position 1 and the lesson ID is `L1`, enter `mark 1 lid/L1 d/2026-10-05 s/present`.

`STUDENT_INDEX` is the Student's current position in the displayed people list; `lid/` takes the shared Lesson's
stable ID, not a per-student lesson index. Use a real date in `YYYY-MM-DD` format that matches the Lesson's weekday,
and status `present` or `absent`. Past dates are accepted. A new Student/Lesson/date record requires current
enrolment. The same date is independent for different Students, and different dates are independent for one Student.

Marking an existing Student/Lesson/date again with the other status corrects that one record instead of appending a
duplicate. This correction remains allowed after unenrolment, but a new date is rejected until the Student is enrolled
again. Repeating the status already stored reports that nothing changed and does not save. No attendance entry means
unrecorded, not absent. Successful marks and corrections preserve the current people filter, order and command positions.

**Unmark (active):** `unmark STUDENT_INDEX lid/LESSON_ID d/YYYY-MM-DD`

Example: After `list r/student`, if the intended student appears at position 1, enter `unmark 1 lid/L1 d/2026-10-05` for lesson `L1`.

Unmark uses the same current people-list Student position and stable Lesson ID rules as `mark`. Supply a real date
matching the Lesson's weekday; do not supply `s/` or another status field. The Student and Lesson must still exist.
Unmark removes only that Student/Lesson/date record and does not change the Lesson, recurring schedule, roster or
other Students' and dates' attendance.

Existing historical attendance can be corrected with `mark` or removed with `unmark` after unenrolment. If the
selected record does not exist, PonHub reports that nothing changed and does not save. A successful removal makes that
occurrence unrecorded, not absent. Success, no-op and failure preserve the current people filter, order and command positions.

**History (active):** `history STUDENT_INDEX [lid/LESSON_ID]`.

Example: After `list r/student`, if the intended student appears at position 1, enter `history 1 lid/L1` to view their attendance for lesson `L1`.

History shows recorded entries in a separate scrolling panel, newest date first, then by numeric Lesson ID. Each
entry shows the stable Lesson ID, date, `present` or `absent`, and whether the Student is currently enrolled or a
former member. An existing Student with no records, or no records for the selected existing Lesson, gets a successful
empty result. An unrecorded occurrence has no entry; it is never counted as absent. After `mark`, correction, `unmark` or
unenrolment, an open history view refreshes from committed data. History retrieval does not save data or change the
current people filter, order, selection or indices. Use the Student's current position in the people list, not a
roster/history row or stable ID suffix. `lessons`, `showlesson` or a lesson search returns the right panel to lesson results. Invalid requests preserve the prior panel.

Deleting the selected Student or the selected Lesson successfully clears that history selection and returns the
right panel to lesson results. Unrelated deletions and failed deletions preserve a valid history selection.

<a id="planned-failure-and-data-behavior"></a>

### Failure and data behavior

PonHub saves people, shared lessons/rosters, retained attendance and ID allocation state together in the selected file. `addlesson`, `deletelesson`,
`enrol` and `unenrol` use that protected transaction, as do changed `mark` and `unmark` operations. Invalid
attendance commands,
missing-record/identical-status no-ops and failed saves leave the saved file, aggregate and current people view unchanged.
Attendance history retrieval is active and read-only. See
[Data protection in the current build](#data-protection-in-the-current-build) for actual paths and recovery.

The supported JSON format remains human-editable. Close PonHub and keep a backup before editing. Preserve the supported schema and version, valid field values, stable IDs, relationships and ID allocation state. Correctly edited supported data loads after complete validation. Read-only commands do not save operational data; invalid commands and failed saves preserve the previous data and view. Future mutations must retain the same guarantees.

--------------------------------------------------------------------------------------------------------------------

## FAQ

**Q**: How do I transfer my PonHub data to another computer?<br>
**A**: Close PonHub on both computers. Install the same version or one that supports your data format on the new computer, then copy your operational file into the new JAR folder, preserving its relative path, or select its new location inside that folder with `--data=PATH`. If you use the default location, copy the `data` folder. Keep a backup of the original folder until you have opened PonHub and checked your records on the new computer. Moving a legacy contact file to a canonical-format build does not migrate it; follow the [current recovery rules](#data-protection-in-the-current-build).

**Q**: Should I use a displayed position or an ID?<br>
**A**: The active `enrol`, `unenrol`, `mark`, `unmark`, `delete`, `lessons si/` and `history` selectors
use the person's current position in the displayed people list. Run `list` or a people
search and check that position first. Cards label stable person IDs separately; those IDs preserve stored identity
and relationships. Select a lesson with `lid/L1`, for example, including for active `deletelesson`. Create a lesson
using the tutor's full name and optional exact phone; search tutor names using fragments. Check `help` for the
commands available in your build.

**Q**: How do I choose the tutor for `addlesson`?<br>
**A**: Supply the tutor's complete name with `tu/`. Matching ignores letter case and repeated spaces. If more than one
tutor has that name, add their exact phone with `tp/`; leading zeros matter. A supplied phone must match even when the
name is unique. Use `help addlesson` for the complete active syntax.

--------------------------------------------------------------------------------------------------------------------

<a id="planned-command-summary"></a>

## Command format reference

All formats in this table are active in normal sessions. A protected session permits only `help`, `list` and `exit`.
These formats use the [command conventions](#reading-the-planned-command-formats).
Person/student selectors come from the current people list, while every existing-lesson selector uses a stable
`lid/` reference.

| Action | Format |
| --- | --- |
| Add student | `add r/student n/NAME l/LEVEL pp/PARENT_PHONE [p/PHONE] [e/EMAIL] [a/ADDRESS]` |
| Add tutor or parent | `add r/ROLE n/NAME p/PHONE [e/EMAIL] [a/ADDRESS]` |
| List people | `list [r/ROLE]` |
| Delete person | `delete INDEX` |
| Create shared lesson | `addlesson d/DAY st/HHMM et/HHMM s/SUBJECT tu/TUTOR_NAME [tp/TUTOR_PHONE] rm/ROOM` |
| Delete lesson | `deletelesson lid/LESSON_ID` |
| Lesson catalogue | `lessons [si/STUDENT_INDEX]` |
| Lesson details | `showlesson lid/LESSON_ID [d/YYYY-MM-DD]` |
| Enrol / unenrol | `enrol STUDENT_INDEX lid/LESSON_ID` / `unenrol STUDENT_INDEX lid/LESSON_ID` |
| Search | `search c/CATEGORY [FILTER_PREFIX/VALUE]...` |
| Mark attendance | <code>mark STUDENT_INDEX lid/LESSON_ID d/YYYY-MM-DD s/present&#124;absent</code> |
| Unmark attendance | `unmark STUDENT_INDEX lid/LESSON_ID d/YYYY-MM-DD` |
| History | `history STUDENT_INDEX [lid/LESSON_ID]` |
| Help | `help [COMMAND]` |
| Exit | `exit` |

Capacity, waiting lists, make-ups, fees, person and lesson editing, occurrence cancellation and undo/redo are future scope. So are exports, saved or guided searches, alternative or exclusion search conditions, archives, late/excused attendance, notes, bulk/clickable attendance and attendance percentages. The full relationship-filter matrix above is active. The current build's supported commands are listed in [Current command summary](#current-command-summary).


### Checking unsuccessful saves in the current build

On a disposable copy, run `list r/student`, select a card and note the saved people and their IDs. Make the selected operational data location unwritable for the app's account, then attempt a valid role-aware addition. Expect a save error, no success message and unchanged people, IDs, role filter and selection. Restore write access and retry; the successful addition must use the next unconsumed ID, appear last in the all-role view and survive restart. Automated tests inject save failures without changing filesystem permissions.

### Canonical loading and recovery

Use disposable copies and record file bytes before each check. Launch with a missing `--data` path: expect an empty normal session and no file after help/list/exit; a successful add creates supported JSON. Restart with the same path and verify the saved people and IDs.

Select an invalid or unversioned AB3 file: expect a protected empty view, recovery guidance, only help/list/exit and unchanged file bytes. Repair or remove a working copy while the app is open, then attempt add again; the same session stays protected. Close the app, restore a valid supported copy and restart to validate recovery. For legacy records, preserve the original and manually re-enter roles into a separate previously unused path as described in [Data protection](#data-protection-in-the-current-build).

### Editing saved shared lessons (supported format)

The canonical format now supports shared lessons and their student rosters. Each lesson contains its
stable ID, tutor ID, weekday, start/end time, subject, room and enrolled student IDs. Times are strings
such as `"0900"`; an empty student roster is valid. Several students share one lesson record.

Before editing, close the app and retain a backup. Keep referenced tutors/students in the same file,
use each lesson ID once and each student ID once per roster, and preserve allocation counters even
after deleting records. Do not create overlapping bookings for a tutor, room or enrolled student;
back-to-back lessons are allowed. Invalid files are rejected as a whole rather than partially loaded.
Attendance is supported in saved files as described below. `addlesson`, `deletelesson`, `enrol`, `unenrol`, `mark`,
`lessons` and lesson search are active, guarded `delete` removes eligible people, and `unmark` removes a selected
dated attendance record. `history` displays retained records without changing them.

For a disposable-file recovery check, change a saved lesson's tutor ID to an absent
ID and restart. Expect a recovery warning and unchanged file bytes after help/list/exit or an attempted
mutation. Restore the valid backup and restart to verify loading. Codec and protected-storage tests
cover these guarantees; record actual application outcomes when performing this check.


### Editing saved attendance (supported format; mark and unmark active)

Close the app and back up the file before editing. The root `attendance` array contains records such as
`{"studentId":"S1","lessonId":"L2","date":"2026-10-12","status":"present"}`.
Each record requires these four string fields, with no extra fields. The student and lesson must exist.
Use a real `YYYY-MM-DD` date (years 0001–9999) matching the lesson's weekday and status `present` or
`absent` (case and surrounding whitespace are ignored for status).

Keep each student/lesson/date combination unique. Correct a record by changing its status;
remove just that record to make the occurrence unrecorded. Removing a student from a lesson's roster
does not remove attendance: retain those records in the array to preserve their history. A student
can have history for a lesson without being currently enrolled. Do not delete referenced people or
lessons while retaining their attendance records. Keep all allocation counters intact.

Restart to load the edited file. Invalid dates, weekday mismatches, duplicate keys, unknown fields
or missing references protect the entire file from writes. Restore or repair the backup and restart;
repairing the file does not unlock an already protected session. The active `mark` and `unmark` commands apply these
same rules; `history` displays retained records without changing membership.
