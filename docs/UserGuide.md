---
layout: page
title: User Guide
---

PonHub is a desktop app for tuition centre administrators to keep student, tutor, and parent details together with lessons and student attendance. Its command box lets you work quickly from the keyboard while the graphical interface shows the records and results.

* Table of Contents
{:toc}

--------------------------------------------------------------------------------------------------------------------

## Quick start

1. Ensure that Java `25` or later is installed on your computer.<br>
   **Mac users:** Follow the JDK installation instructions [here](https://se-education.org/guides/tutorials/javaInstallationMac.html).

1. Use the JAR built from this increment, or download a matching PonHub build from the [project's Releases page](https://github.com/AY2627S1-CS2103T-F13-3/tp/releases) after publication. Contributors can build `build/libs/ponhub.jar` with `./gradlew shadowJar`; see [DevOps](DevOps.html#build-automation). Week 8 does not require a public release, so an older released JAR may have different commands.

1. Put the JAR file in the folder you want to use for PonHub. Keep this folder when moving or backing up your data.

1. Open a terminal in that folder and run `java -jar FILENAME.jar`, replacing `FILENAME.jar` with the downloaded file's name. The PonHub window should open.

1. Enter a command in the command box and press Enter. Start with `help`, then `help add`. The current increment supports inherited contact records:

   * `add n/Mei Lim p/92345678 e/mei@example.com a/10 Clementi Road` adds a contact.
   * `list` displays all contacts.
   * `help delete` explains deletion by the current displayed index.
   * `help ADD` shows the same guidance as `help add`.
   * `exit` closes PonHub.

## Current command summary

This Week 8 increment implements local inline help. The active routes below still use inherited contact data. Role-aware people, stable IDs, lessons, enrolment, attendance, history and search are planned for integration; their specifications follow under [Planned shared-lesson workflow](#planned-shared-lesson-workflow). Type `help` to inspect the commands available in your build. The inherited `edit`, `clear` and `find` routes are withdrawn.

| Action | Format | Example |
| --- | --- | --- |
| Add contact | `add n/NAME p/PHONE e/EMAIL a/ADDRESS [t/TAG]...` | `add n/Mei Lim p/92345678 e/mei@example.com a/10 Clementi Road` |
| List contacts | `list` | `list` |
| Delete contact | `delete INDEX` | `delete 1` |
| Get help | `help [COMMAND]` | `help add` |
| Exit | `exit` | `exit` |

`INDEX` is a positive integer referring to the currently displayed contact list. Contact fields name, phone, email and address are required by the current add parser. Tags may repeat; other add prefixes may appear only once. `list` and `exit` accept no arguments. IDs such as `S1` are part of the planned contract and are not accepted by the current contact parser.

### Viewing command help: `help`

**Format:** `help [COMMAND]`

`help` shows the current supported-command catalogue in Result Display. `help add`, `help delete`, `help list`, `help help` and `help exit` show a command's syntax and example. Topic names are case-insensitive, so `help ADD` is accepted; the command word `help` remains lowercase. Asking for exit guidance does not exit the application.

Choose **Help > Help** or press **F1** for the same catalogue, including when focus is in the command box or Result Display. Menu/F1 preserve unfinished command text. Help preserves records, the active person filter, order, selection and preferences. Typed help clears the submitted command in the usual way. Help needs no internet connection and does not save operational data.

Long guidance wraps within Result Display. Scroll vertically to read the rest; each new result starts at the top.

* `help remove` returns `Unknown command: 'remove'. Type 'help' to see available commands.` Unavailable topics such as `help addlesson` receive the same kind of feedback until their commands are integrated.
* `help add delete` returns `Invalid command format!` followed by `Usage: help [COMMAND]` and its explanation. Supply at most one topic.
* `edit`, `clear` and `find` return `Unknown command.` They are absent from the catalogue.

--------------------------------------------------------------------------------------------------------------------

## Planned shared-lesson workflow

The following sections define the supplied v1.2 integration target. They are not commands delivered by this help increment. Feature owners will update their implementation details and examples when each feature becomes available.

### Reading the planned command formats

* Uppercase words are placeholders; replace them with your values. Square brackets indicate optional input and are not typed.
* Use displayed, stable IDs: `S1` for a student, `T1` for a tutor, `P1` for a parent and `L1` for a shared lesson. Filtering changes display positions, not IDs. Stored references and IDs survive restarts.
* `PERSON_ID` is a student, tutor or parent ID; `STUDENT_ID`, `TUTOR_ID` and `LESSON_ID` must identify the appropriate kind of record.
* Type command names and prefixes in lowercase. Supply each prefix at most once, in any order. Omit unknown optional fields; do not supply blank values.

### Planned quick start

After these commands and persistence are integrated, a fresh dataset can use this workflow. Read the actual IDs returned by additions; existing data may allocate different IDs.

```text
add r/tutor n/Mei Lim p/92345678
add r/student n/Alex Tan l/S2 pp/91234567
add r/student n/Jamie Tan l/S2 pp/91234567
addlesson d/Mon st/1600 et/1730 s/Math tu/T1 rm/R1
enrol S1 L1
enrol S2 L1
search c/lesson
mark S1 L1 d/2026-10-05 s/present
mark S2 L1 d/2026-10-05 s/absent
unenrol S2 L1
history S2 lid/L1
mark S2 L1 d/2026-10-05 s/present
```

Expect one shared lesson with two students before unenrolment. Jamie's dated history remains visible and correctable after leaving. Restart and verify the same IDs, roster and history. No record means unrecorded, rather than absent.

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
| `n/NAME` | Required. Between 1 and 100 characters using English letters, spaces, apostrophes, hyphens or periods, with at least one letter. Repeated spaces are reduced to one. |
| `l/LEVEL` | Required for students; not allowed for tutors or parents. Use `P1`–`P6`, `S1`–`S5`, `JC1` or `JC2`. Values are case-insensitive and displayed in uppercase. |
| `pp/PARENT_PHONE` | Required for students; not allowed for tutors or parents. Use 3–15 digits without spaces or punctuation. |
| `p/PHONE` | Required for tutors and parents; optional for students. Use 3–15 digits without spaces or punctuation. Leading zeros are retained. |
| `e/EMAIL` | Optional. Use an email address such as `mei@example.com`, with no spaces and at most 254 characters. |
| `a/ADDRESS` | Optional. Between 1 and 200 printable ASCII characters. Slashes, tabs and line breaks are not allowed. Repeated spaces are reduced to one. |

For email addresses, the local part before `@` may contain letters, digits, periods, underscores, `%`, `+` and `-`. It must not begin or end with a period or contain consecutive periods. The domain must contain at least two dot-separated labels, with no leading or trailing hyphens in a label. Its final label must contain 2–63 letters.

#### Examples

* `add r/student n/Alex Tan l/S2 pp/91234567`<br>
  Adds Alex Tan as a Secondary 2 student with parent contact number 91234567.
* `add r/tutor n/Mei Lim p/92345678 e/mei@example.com`<br>
  Adds Mei Lim as a tutor with a phone number and email address.
* `add r/parent n/Pat Tan p/91234567`<br>
  Adds Pat Tan as a parent.

#### Expected result

PonHub saves the new record and displays a confirmation such as:
`Added person: student - Alex Tan.`

The person list resets to show all roles, with the new record at the end. A new student starts with no lessons or attendance records.

#### Things to note

* A separate parent record is optional. You can add a student using their parent's phone number before creating a parent record.
* A duplicate has the same role, normalized name and contact number. For students, the contact number used for this check is `pp/`; for tutors and parents, it is `p/`.
* Duplicate name comparisons ignore letter case and repeated spaces.
* Siblings with different names may share the same parent phone number.
* Changing an optional email, address or student phone number does not make an otherwise duplicate record unique.

A duplicate is rejected with:
`This person already exists: ROLE - NAME.`

### 2. Listing people: `list`

Displays all people or only people with a specified role, including each stable ID.

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

People appear in creation order with stable IDs. Display positions are not command selectors.

Example feedback:
`Listed 3 person(s) with role: student.`

An empty result is valid. PonHub displays a count of zero and `No persons to display.`

#### Things to note

* Use the singular role names `student`, `tutor` and `parent`. To display everyone, enter `list`; `list r/all` is invalid.
* Listing changes the displayed view without modifying stored records.

### 3. Deleting a person: `delete`

Removes one person identified by a stable person ID.

#### Format

`delete PERSON_ID`

#### Example

1. `list r/parent`
2. Check the intended parent's ID, then enter `delete P1` if their ID is `P1`.

#### Expected result

PonHub displays a confirmation such as:
`Deleted person: parent - Pat Tan.`

The current filter is preserved. Remaining people keep their IDs.

#### Restrictions

* The ID must identify an existing person.
* Only one person can be deleted per command.
* Student deletion is blocked while enrolled or referenced by attendance. Tutor deletion is blocked while a lesson references the tutor. Linked records are not deleted automatically; removing a student never removes a shared lesson.
* Deleting a separate parent record does not erase the parent phone number stored on a student's record.

If dependencies prevent deletion, PonHub displays:
`Cannot delete person: linked lesson or attendance records exist. Remove or reassign the links first.`

Check the intended ID before deleting another person.

### 4. Creating and deleting shared lessons

**Create:** `addlesson d/DAY st/TIME et/TIME s/SUBJECT tu/TUTOR_ID rm/ROOM`

Example: `addlesson d/Mon st/1600 et/1730 s/Math tu/T1 rm/R1`.

The tutor must already exist. Use `Mon` through `Sun` and four-digit 24-hour HHMM times. Start must be earlier than end on the same day. A successful addition returns a stable lesson ID, such as `L1`, with an empty roster. Tutor and room bookings must not overlap another lesson on that weekday; adjacent intervals are allowed. Adding more students to this lesson does not book the tutor or room again.

**Delete:** `deletelesson LESSON_ID`, for example `deletelesson L1`.

Deletion is blocked while the roster is non-empty or attendance refers to the lesson. It never removes student records. Once an unreferenced lesson is deleted, its tutor and room times become available again. Cancelling one occurrence and editing a lesson are future scope.

### 5. Enrolling and unenrolling students

**Enrol:** `enrol STUDENT_ID LESSON_ID`, for example `enrol S1 L1`.

Both records must exist and the person must be a student. Duplicate enrolment and overlap with that student's other enrolled lessons are rejected. A second student can join the same shared lesson without creating another lesson or booking.

**Unenrol:** `unenrol STUDENT_ID LESSON_ID`, for example `unenrol S1 L1`.

This removes current membership and retains dated attendance. It does not delete the lesson or student. Student lessons are derived from the shared lesson's roster.

### 6. Searching people and shared lessons

**Format:** `search c/CATEGORY [FILTER_PREFIX/VALUE]...`

Use `student`, `parent`, `tutor` or `lesson`. Category-only lesson search, `search c/lesson`, shows each shared lesson once, including empty rosters.

| Category | Initial filters |
| --- | --- |
| People | `n/NAME` and applicable own-contact `p/PHONE`, `pp/PARENT_PHONE`, `e/EMAIL`, `a/ADDRESS` filters |
| Lessons | `tu/TUTOR_ID`, `d/DAY`, `s/SUBJECT` |

Text matching is case-insensitive. Combined lesson filters must match the same lesson. Unsupported, blank and repeated filters are rejected. A valid search with no matches reports an empty result. Relationship searches beyond these initial filters are future scope; feature owners specify their exact matching rules during implementation.

Examples: `search c/student n/Alex`, `search c/tutor p/92345678`, `search c/lesson tu/T1 d/Mon s/Math`.

### 7. Recording attendance and retrieving history

**Mark:** `mark STUDENT_ID LESSON_ID d/DATE s/STATUS`

Example: `mark S1 L1 d/2026-10-05 s/present`.

Use a real date in `YYYY-MM-DD` format, matching the lesson's weekday, and status `present` or `absent`. New records require current enrolment. Marking an existing student/lesson/date corrects that record; repeating the same status does not create a duplicate. No attendance entry means unrecorded.

**Unmark:** `unmark STUDENT_ID LESSON_ID d/DATE`, for example `unmark S1 L1 d/2026-10-05`.

Unmark removes only that dated record and does not change enrolment. Existing historical attendance can be corrected or unmarked after unenrolment. Students must still exist, and historical references continue to block student and lesson deletion.

**History:** `history STUDENT_ID [lid/LESSON_ID]`, for example `history S1 lid/L1`.

History lists dated attendance, including former enrolments. Current membership must be distinguishable from historical attendance. History retrieval does not change data.

### Planned failure and data behavior

The integrated target saves people, shared lessons, memberships, attendance and IDs together in `data/ponhub.json`, with preferences stored separately. Validation or saving failures must leave existing data unchanged and restore active views; corrupt or unsupported files must be preserved with recovery instructions and overwrite protection. These behaviors depend on the storage owner's implementation.

The current inherited runtime uses `data/addressbook.json`. Only help skips operational saving in this increment; general rollback and protected loading are not yet delivered. Back up existing files before upgrading or editing them, and do not treat the planned recovery behavior as implemented.

--------------------------------------------------------------------------------------------------------------------

## FAQ

**Q**: How do I transfer my PonHub data to another computer?<br>
**A**: Close PonHub on both computers. Install the same or a compatible PonHub version on the new computer, then copy the `data` folder from the folder containing the old JAR to the folder containing the new JAR. Keep a backup of the original folder until you have opened PonHub and checked your records on the new computer.

**Q**: Should I use a displayed position or an ID?<br>
**A**: The current contact `delete` command uses a displayed index. The planned shared-lesson commands use stable IDs such as `S1` and `L1`. Check `help delete` in your build; never substitute an index for an ID.

**Q**: Why is `help addlesson` unavailable?<br>
**A**: Help lists only active commands. Shared lessons and attendance are being integrated by their owners. The planned commands below are available only after their implementations and persistence support are registered.

--------------------------------------------------------------------------------------------------------------------

## Planned command summary

| Action | Format |
| --- | --- |
| Add student | `add r/student n/NAME l/LEVEL pp/PARENT_PHONE [p/PHONE] [e/EMAIL] [a/ADDRESS]` |
| Add tutor or parent | `add r/ROLE n/NAME p/PHONE [e/EMAIL] [a/ADDRESS]` |
| List people | `list [r/ROLE]` |
| Delete person | `delete PERSON_ID` |
| Create shared lesson | `addlesson d/DAY st/TIME et/TIME s/SUBJECT tu/TUTOR_ID rm/ROOM` |
| Delete lesson | `deletelesson LESSON_ID` |
| Enrol / unenrol | `enrol STUDENT_ID LESSON_ID` / `unenrol STUDENT_ID LESSON_ID` |
| Search | `search c/CATEGORY [FILTER_PREFIX/VALUE]...` |
| Mark attendance | `mark STUDENT_ID LESSON_ID d/DATE s/STATUS` |
| Unmark attendance | `unmark STUDENT_ID LESSON_ID d/DATE` |
| History | `history STUDENT_ID [lid/LESSON_ID]` |
| Help | `help [COMMAND]` |
| Exit | `exit` |

Capacity, waiting lists, make-ups, fees, lesson editing, occurrence cancellation, undo/redo and advanced search are future scope. The current build's supported commands are listed in [Current command summary](#current-command-summary).
