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
* `INDEX` is a positive integer identifying a person's position in the currently displayed, filtered people list. `STUDENT_INDEX` uses that same list and must select a student, even when other roles are displayed. Run `list` or a people search and check the current positions before selecting a person.
* Person cards distinguish their numbered command position from their stable ID: `S1` for a student, `T1` for a tutor or `P1` for a parent. Filtering and deletion can change positions; stable IDs and stored relationships survive those changes and restarts. Use a current position for `delete` and student operations.
* Create a lesson by supplying an existing tutor's full name with `tu/TUTOR_NAME` and, optionally, their phone with `tp/TUTOR_PHONE`. Search uses `tu/QUERY` for a fragment of the tutor's name instead.
* Shared lessons have stable IDs such as `L1`. Use `lid/LESSON_ID` wherever a command selects a lesson; there is no per-student lesson index.
* Lesson catalogue, roster and history results preserve the current people list, its filter and its command positions. Select a student from the people list, rather than a position in those other results.
* Type command names and prefixes in lowercase. Supply each prefix at most once, in any order. Omit unknown optional fields; do not supply blank values.

### Planned quick start

After these commands and persistence are integrated, a fresh dataset can use this workflow. Read the actual lesson ID returned by `addlesson` and substitute it for `L1` if necessary. The `list r/student` result below places Alex at position 1 and Jamie at position 2. Recheck those positions when using an existing dataset.

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
```

Expect one shared lesson with two students before unenrolment. Lesson search and history results preserve Alex and Jamie's positions in the people list. Jamie's dated history remains visible and correctable after leaving. Restart and verify the same stable IDs, roster and history; run `list r/student` again before selecting a student by position. No record means unrecorded, rather than absent.

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

The person list resets to show all roles, with the new record at the end. Recheck its displayed positions before selecting a person in your next command. A new student starts with no lessons or attendance records.

#### Things to note

* A separate parent record is optional. You can add a student using their parent's phone number before creating a parent record.
* A duplicate has the same role, normalized name and contact number. For students, the contact number used for this check is `pp/`; for tutors and parents, it is `p/`.
* Duplicate name comparisons ignore letter case and repeated spaces.
* Siblings with different names may share the same parent phone number.
* Changing an optional email, address or student phone number does not make an otherwise duplicate record unique.

A duplicate is rejected with:
`This person already exists: ROLE - NAME.`

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
`Listed 3 person(s) with role: student.`

An empty result is valid. PonHub displays a count of zero and `No persons to display.`

#### Things to note

* Use the singular role names `student`, `tutor` and `parent`. To display everyone, enter `list`; `list r/all` is invalid.
* Listing changes the displayed view without modifying stored records.
* Use positions from the latest people list or people search. A previous position may select someone else after the view changes or a person is deleted.

### 3. Deleting a person: `delete`

Removes one person selected by their current displayed index in the people list.

#### Format

`delete INDEX`

#### Example

1. `list r/parent`
2. Check the intended parent's current position. If Pat Tan is at position 1 in this result, enter `delete 1`.

#### Expected result

PonHub displays a confirmation such as:
`Deleted person: parent - Pat Tan.`

The current filter is preserved. Remaining people keep their IDs and their displayed positions are renumbered.

#### Restrictions

* `INDEX` must be a positive integer within the current people list. A stable person ID such as `P1` is not a delete selector.
* Only one person can be deleted per command.
* Student deletion is blocked while enrolled or referenced by attendance. Tutor deletion is blocked while a lesson references the tutor. Linked records are not deleted automatically; removing a student never removes a shared lesson.
* Deleting a separate parent record does not erase the parent phone number stored on a student's record.

If dependencies prevent deletion, PonHub displays:
`Cannot delete person: linked lesson or attendance records exist. Remove or reassign the links first.`

Check the updated people list before deleting another person.

### 4. Creating and deleting shared lessons

**Create:** `addlesson d/DAY st/START et/END s/SUBJECT tu/TUTOR_NAME [tp/TUTOR_PHONE] rm/ROOM`

Example: `addlesson d/Mon st/1600 et/1730 s/Math tu/Mei Lim rm/R1`.

The tutor must already exist. `tu/` matches the tutor's complete name, ignoring letter case and repeated spaces. If several tutors share that name, supply their exact phone number, for example `tu/Mei Lim tp/92345678`. An ambiguous name without `tp/` is rejected. A supplied phone must match the named tutor exactly, even when the name is unique; an unmatched name or phone is rejected.

Use `Mon` through `Sun` and four-digit 24-hour HHMM times. Start must be earlier than end on the same day. A successful addition returns a stable lesson ID, such as `L1`, with an empty roster. Creation does not select or enrol a student. Tutor and room bookings must not overlap another lesson on that weekday; adjacent intervals are allowed. Adding more students to this lesson does not book the tutor or room again.

**Delete:** `deletelesson lid/LESSON_ID`, for example `deletelesson lid/L1`.

Deletion is blocked while the roster is non-empty or attendance refers to the lesson. It never removes student records. Once an unreferenced lesson is deleted, its tutor and room times become available again. Cancelling one occurrence and editing a lesson are future scope.

**Catalogue:** `lessons [si/STUDENT_INDEX]`.

Enter `lessons` to view each shared lesson once, including lessons with empty rosters. To view a student's currently enrolled lessons, first run `list r/student` and check their current position. If the intended student appears at position 1, enter `lessons si/1`. Catalogue results preserve the current people list and its indices.

**Details:** `showlesson lid/LESSON_ID [d/YYYY-MM-DD]`.

For example, `showlesson lid/L1` displays the lesson's schedule, subject, tutor, room and current roster, with stable IDs identifying the records. `showlesson lid/L1 d/2026-10-05` also shows each current roster student's `present`, `absent` or unrecorded status for that real, matching-weekday date. Attendance retained for former members is labelled separately from the current roster. An empty roster is valid. These results preserve the current people list and its command positions.

### 5. Enrolling and unenrolling students

**Enrol:** `enrol STUDENT_INDEX lid/LESSON_ID`.

Run `list r/student` and check the intended student's current position. If the student appears at position 1 and the lesson ID is `L1`, enter `enrol 1 lid/L1`.

Both records must exist and the person must be a student. Duplicate enrolment and overlap with that student's other enrolled lessons are rejected. A second student can join the same shared lesson without creating another lesson or booking.

**Unenrol:** `unenrol STUDENT_INDEX lid/LESSON_ID`.

Run `list r/student` and recheck the student's position. If the intended student appears at position 1, enter `unenrol 1 lid/L1` for lesson `L1`.

This removes current membership and retains dated attendance. It does not delete the lesson or student. Student lessons are derived from the shared lesson's roster.

### 6. Searching people and shared lessons

**Format:** `search c/CATEGORY [FILTER_PREFIX/VALUE]...`

Use `student`, `parent`, `tutor` or `lesson`; category values are case-insensitive. A category-only search imposes no filters. For example, `search c/lesson` shows each shared lesson once, including empty rosters.

The complete planned filter matrix is below. Search will be integrated in stages; a build must clearly reject a filter whose search functionality is not yet enabled. Check that build's help before using it. Relationship searches in this matrix are part of the integration target.

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

Examples:

* `search c/student n/Alex l/S2 pp/91234567`
* `search c/parent sn/Jamie d/Mon s/Math`
* `search c/tutor n/mei sn/Alex rm/R1`
* `search c/lesson tu/mei d/Mon st/1600 s/Math`

People searches replace the displayed people list and number its results from 1. Use those current positions for subsequent person commands. Lesson searches display separate results and preserve the current people list, its filter and its indices; lesson results do not supply person command indices.

### 7. Recording attendance and retrieving history

**Mark:** `mark STUDENT_INDEX lid/LESSON_ID d/DATE s/STATUS`

Example: Run `list r/student`. If the intended student appears at position 1 and the lesson ID is `L1`, enter `mark 1 lid/L1 d/2026-10-05 s/present`.

Use a real date in `YYYY-MM-DD` format, matching the lesson's weekday, and status `present` or `absent`. New records require current enrolment. Marking an existing student/lesson/date corrects that record; repeating the same status does not create a duplicate. No attendance entry means unrecorded.

**Unmark:** `unmark STUDENT_INDEX lid/LESSON_ID d/DATE`.

Example: After `list r/student`, if the intended student appears at position 1, enter `unmark 1 lid/L1 d/2026-10-05` for lesson `L1`.

Unmark removes only that dated record and does not change enrolment. If no entry exists, there is nothing to remove. Existing historical attendance can be corrected or unmarked after unenrolment. Students must still exist, and historical references continue to block student and lesson deletion. Mark and unmark preserve the people list and its command positions.

**History:** `history STUDENT_INDEX [lid/LESSON_ID]`.

Example: After `list r/student`, if the intended student appears at position 1, enter `history 1 lid/L1` to view their attendance for lesson `L1`.

History lists dated attendance, including former enrolments. Current membership must be distinguishable from historical attendance. History retrieval does not change data or the current people list. Roster and history positions are not student command selectors; use the student's current position in the people list.

### Planned failure and data behavior

The integrated target saves people, shared lessons, memberships, attendance and IDs together in `data/ponhub.json`, with preferences stored separately. Validation or saving failures must leave existing data unchanged and restore active views. Read-only commands do not save operational data. These behaviors depend on the storage owner's implementation.

The supported JSON format remains human-editable. Close PonHub and keep a backup before editing. Preserve the supported schema and version, valid field values, stable IDs, relationships and ID allocation state. Correctly edited supported data must load after validation. Invalid, unreadable, corrupt or unsupported files must produce a controlled error with recovery instructions, preserve their original bytes and remain protected from operational writes.

At the first canonical-format cutover, unversioned AB3 contact data is treated as legacy data. PonHub must preserve it and reject loading it into the new store; it must not guess person roles, automatically migrate it or replace it with an empty dataset. Recovery requires a backup and a separate supported store, followed by manual re-entry. Creating that separate store is available only when the build provides documented protected initialization. This increment supplies no such setup command; an importer remains future work.

The current inherited runtime uses `data/addressbook.json`. Help, list and exit do not create or rewrite this file and remain available when operational saving would fail. Preferences are saved separately on shutdown. Add/delete still save operational data; general rollback and protected loading are not yet delivered. Back up existing files before upgrading or editing them, and do not treat the planned recovery behavior as implemented.

--------------------------------------------------------------------------------------------------------------------

## FAQ

**Q**: How do I transfer my PonHub data to another computer?<br>
**A**: Close PonHub on both computers. Install the same version or one that supports your data format on the new computer, then copy the `data` folder from the folder containing the old JAR to the folder containing the new JAR. Keep a backup of the original folder until you have opened PonHub and checked your records on the new computer. Moving a legacy contact file to a canonical-format build does not migrate it; follow the [planned recovery rules](#planned-failure-and-data-behavior).

**Q**: Should I use a displayed position or an ID?<br>
**A**: The current contact `delete` command and the planned `delete`, `enrol`, `unenrol`, `mark`, `unmark`, `history` and `lessons si/` selectors use the person's current position in the displayed people list. Run `list` or a people search and check that position first. Cards label stable person IDs separately; those IDs preserve stored identity and relationships. Select a lesson with `lid/L1`, for example. Create a lesson using the tutor's full name and optional exact phone; search tutor names using fragments. Check `help` for the commands available in your build.

**Q**: Why is `help addlesson` unavailable?<br>
**A**: Help lists only active commands. Shared lessons and attendance are being integrated by their owners. The planned commands below are available only after their implementations and persistence support are registered.

--------------------------------------------------------------------------------------------------------------------

## Planned command summary

| Action | Format |
| --- | --- |
| Add student | `add r/student n/NAME l/LEVEL pp/PARENT_PHONE [p/PHONE] [e/EMAIL] [a/ADDRESS]` |
| Add tutor or parent | `add r/ROLE n/NAME p/PHONE [e/EMAIL] [a/ADDRESS]` |
| List people | `list [r/ROLE]` |
| Delete person | `delete INDEX` |
| Create shared lesson | `addlesson d/DAY st/START et/END s/SUBJECT tu/TUTOR_NAME [tp/TUTOR_PHONE] rm/ROOM` |
| Delete lesson | `deletelesson lid/LESSON_ID` |
| Lesson catalogue | `lessons [si/STUDENT_INDEX]` |
| Lesson details | `showlesson lid/LESSON_ID [d/YYYY-MM-DD]` |
| Enrol / unenrol | `enrol STUDENT_INDEX lid/LESSON_ID` / `unenrol STUDENT_INDEX lid/LESSON_ID` |
| Search | `search c/CATEGORY [FILTER_PREFIX/VALUE]...` |
| Mark attendance | `mark STUDENT_INDEX lid/LESSON_ID d/DATE s/STATUS` |
| Unmark attendance | `unmark STUDENT_INDEX lid/LESSON_ID d/DATE` |
| History | `history STUDENT_INDEX [lid/LESSON_ID]` |
| Help | `help [COMMAND]` |
| Exit | `exit` |

Capacity, waiting lists, make-ups, fees, lesson editing, occurrence cancellation and undo/redo are future scope. So are exports, saved or guided searches, alternative or exclusion search conditions, archives, late/excused attendance, notes, bulk/clickable attendance and attendance percentages. The full relationship-filter matrix above remains part of the planned integration target. The current build's supported commands are listed in [Current command summary](#current-command-summary).
