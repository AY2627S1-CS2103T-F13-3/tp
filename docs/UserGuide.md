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

1. Download the latest PonHub `.jar` file from the [project's Releases page](https://github.com/AY2627S1-CS2103T-F13-3/tp/releases).

1. Put the JAR file in the folder you want to use for PonHub. Keep this folder when moving or backing up your data.

1. Open a terminal in that folder and run `java -jar FILENAME.jar`, replacing `FILENAME.jar` with the downloaded file's name. The PonHub window should open.

1. Enter a command in the command box and press Enter. Start with `help`, then try this example workflow:<br>

   * `add r/tutor n/Mei Lim p/92345678` — adds a tutor.
   * `add r/student n/Alex Tan l/S2 pp/91234567` — adds a student and their parent contact number.
   * `list r/student` — shows students and their displayed indices.
   * `addlesson 1 d/Mon st/1600 et/1730 s/Math tu/Mei Lim rm/R1` — adds a lesson for the student displayed at index `1`.

   The indices in commands refer to the current displayed list, so check the list before using an index. See the [Command summary](#command-summary) for more commands.

--------------------------------------------------------------------------------------------------------------------
## Features

PonHub helps tuition centre administrators manage student, tutor and parent records, schedule recurring lessons, search for relevant information, and record attendance.

### Reading the command formats

* Words in uppercase, such as `NAME`, are placeholders. Replace them with your own values.
* Square brackets indicate optional parameters. Do not type the brackets.
* Type command names and prefixes in lowercase. Some parameter values, such as roles and weekdays, accept either uppercase or lowercase.
* Supply each prefix at most once. For commands with prefixed parameters, the prefixes may appear in any order.
* `INDEX` refers to a person's number in the currently displayed person list. Filtering, searching, adding or deleting records can change these numbers. Check the displayed list before using an index.
* `LESSON_INDEX` refers to a lesson's number within the selected student's lesson list.
* Indices must be positive integers without leading zeros. For example, `1` is valid; `0`, `01`, `-1` and `1.5` are invalid.
* Omit an optional parameter when its value is unknown. Supplying its prefix with an empty value is invalid.

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

Displays all people or only people with a specified role.

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

People appear in creation order, with displayed indices starting at 1.

Example feedback:
`Listed 3 person(s) with role: student.`

An empty result is valid. PonHub displays a count of zero and `No persons to display.`

#### Things to note

* Use the singular role names `student`, `tutor` and `parent`. To display everyone, enter `list`; `list r/all` is invalid.
* Listing changes the displayed view without modifying stored records.

### 3. Deleting a person: `delete`

Removes one person from the currently displayed person list.

#### Format

`delete INDEX`

#### Example

1. `list r/parent`
2. Check the displayed parent list, then enter:
   `delete 1`
   This removes the parent currently displayed at index 1.

#### Expected result

PonHub displays a confirmation such as:
`Deleted person: parent - Pat Tan.`

The current filter is preserved, and the remaining people are renumbered from 1.

#### Restrictions

* The index must exist in the current person list.
* Only one person can be deleted per command.
* Deletion is blocked if the person contains or is referenced by lesson or attendance records. Linked records are not deleted automatically.
* Deleting a separate parent record does not erase the parent phone number stored on a student's record.

If dependencies prevent deletion, PonHub displays:
`Cannot delete person: linked lesson or attendance records exist. Remove or reassign the links first.`

Check the displayed indices again after each deletion before deleting another person.

### 4. Adding a recurring lesson: `addlesson`

Assigns a weekly recurring lesson to an existing student.

#### Format

`addlesson INDEX d/DAY st/START_TIME et/END_TIME s/SUBJECT tu/TUTOR_NAME rm/ROOM`

#### Parameters

| Parameter | Description and accepted values |
|-----------|---------------------------------|
| `INDEX` | The student's index in the currently displayed person list. It must refer to a student. |
| `d/DAY` | `Mon`, `Tue`, `Wed`, `Thu`, `Fri`, `Sat` or `Sun`. Values are case-insensitive. |
| `st/START_TIME` | Start time in four-digit, 24-hour HHMM format, such as `0900` or `1600`. |
| `et/END_TIME` | End time in four-digit, 24-hour HHMM format. It must be later than the start time on the same day. |
| `s/SUBJECT` | Between 1 and 50 characters using letters, digits and spaces. Repeated spaces are reduced to one. |
| `tu/TUTOR_NAME` | The name of an existing tutor. Matching ignores letter case and repeated spaces. |
| `rm/ROOM` | Between 1 and 10 letters or digits, without spaces or punctuation. |

All parameters are required. Valid times range from `0000` to `2359`, with minutes from `00` to `59`. Overnight lessons are not supported.

#### Example

First, use `list r/student` and check the student's index. Assuming Alex Tan is displayed at index 1 and Mei Lim already exists as a tutor:
`addlesson 1 d/Mon st/1600 et/1730 s/Math tu/Mei Lim rm/R1`

#### Expected result

PonHub adds the lesson to Alex's lesson list and displays:
`Added lesson to Alex Tan: Mon 1600-1730 Math with Mei Lim in R1.`

The student's card shows the lesson's index, day, time, subject, tutor and room.

#### Scheduling checks

PonHub rejects a lesson if its time overlaps with an existing lesson on the same day involving either:
* The same tutor.
* The same room.

A rejected addition does not create a partial lesson record.

#### Common errors

| Problem | How to correct it |
|---------|-------------------|
| The selected person is a tutor or parent. | Run `list r/student` and use the intended student's displayed index. |
| The tutor cannot be found. | Add the tutor first, then use their recorded name. |
| The day is written as `Monday`. | Use `Mon`. |
| A time is written as `4pm`, `16:00` or `930`. | Use four digits, such as `1600` or `0930`. |
| The end time is equal to or earlier than the start time. | Choose a later end time on the same day. |
| The tutor or room is already booked. | Choose a non-overlapping time or an available tutor or room. |

### 5. Deleting a recurring lesson: `deletelesson`

Removes one recurring lesson from a student's record.

#### Format

`deletelesson INDEX LESSON_INDEX`

#### Example

`deletelesson 1 2`
Removes lesson 2 from the student currently displayed at person index 1.

#### Expected result

PonHub displays a confirmation such as:
`Deleted lesson from Alex Tan: Wed 1800-1930 Science.`

The student's remaining lessons are renumbered from 1.

#### Restrictions

* The person index must refer to a student.
* The lesson index must exist within that student's lesson list.
* A lesson with attendance records cannot be deleted until those records are removed.

If attendance records exist, PonHub displays:
`Cannot delete lesson: attendance records exist for this lesson. Remove them first.`

This command removes the recurring lesson entry. It does not cancel only one dated occurrence. Removing attendance history to enable deletion also removes that historical information, so check the intended record carefully.

### 6. Searching people and lessons: `search`

Finds students, parents, tutors or lessons using one or more filters.

#### Format

`search c/CATEGORY [FILTER_PREFIX/VALUE]...`

Use `student`, `parent`, `tutor` or `lesson` as the category. The category is required and is case-insensitive.

The `...` indicates that you may supply additional supported filters; do not type it. Omitting all filters displays every record in the selected category.

#### Available filters

| Prefix | Searches by | Supported categories |
|--------|-------------|----------------------|
| `n/` | Person's name | `student`, `parent`, `tutor` |
| `l/` | Student's education level | `student` |
| `p/` | Person's own phone number | `student`, `parent`, `tutor` |
| `pp/` | Student's parent phone number | `student` |
| `e/` | Email address | `student`, `parent`, `tutor` |
| `a/` | Postal address | `student`, `parent`, `tutor` |
| `sn/` | Associated student's name | `parent`, `tutor`, `lesson` |
| `d/` | Lesson weekday | `student`, `parent`, `tutor`, `lesson` |
| `st/` | Lesson start time | `student`, `parent`, `tutor`, `lesson` |
| `et/` | Lesson end time | `student`, `parent`, `tutor`, `lesson` |
| `s/` | Lesson subject | `student`, `parent`, `tutor`, `lesson` |
| `tu/` | Lesson tutor's name | `student`, `parent`, `lesson` |
| `rm/` | Lesson room | `student`, `parent`, `tutor`, `lesson` |

#### How matching works

* Text filters use case-insensitive partial matching. For example, `n/tan` matches `Alex Tan`, while `s/Math` can match both `Math` and `Add Math`.
* Phone numbers, education levels, weekdays and times use exact matching. Phone queries must contain the complete number, using 3–15 digits. Levels and weekdays accept the same values as the corresponding add commands. Times use four-digit HHMM format; if both start and end times are supplied, the end time must be later.
* Every supplied filter must match. For example:
  `search c/student s/Math d/Mon`
  Finds students with a lesson whose subject contains “Math” and whose day is Monday. A student with Math on Tuesday and Science on Monday does not match: both lesson conditions must be satisfied by the same lesson.
* For parent searches, PonHub links a parent to a student when the parent's `p/` exactly matches the student's `pp/`. It does not infer relationships from shared names or addresses. When several relationship filters are supplied, they must apply to the same linked student and, where relevant, the same lesson.

#### Examples

| Command | Finds |
|---------|-------|
| `search c/student` | All students. |
| `search c/student n/Alex l/S2` | Secondary 2 students whose names contain “Alex”. |
| `search c/student pp/91234567` | Students with parent contact number 91234567. |
| `search c/parent n/Tan` | Parents whose names contain “Tan”. |
| `search c/parent s/Math d/Mon` | Parents linked to students with a Monday lesson whose subject contains “Math”. |
| `search c/tutor sn/Alex s/Science` | Tutors assigned a Science-matching lesson belonging to a student whose name contains “Alex”. |
| `search c/lesson sn/Alex tu/Mei` | Lessons belonging to students whose names contain “Alex”, taught by tutors whose names contain “Mei”. |
| `search c/lesson d/Mon st/1600 rm/R1` | Monday lessons starting exactly at 1600 whose room contains “R1”. |

#### Expected result

PonHub replaces the current view with matching records and displays:
`Found N matching CATEGORY record(s).`

Each person appears once, even if several of their lessons match. Matching lesson context is shown beneath the person's card.

Lesson results are grouped by student and retain their existing per-student lesson indices. Their position in the search results does not create a new lesson index.

If no records match, the search still succeeds and displays:
`Found 0 matching CATEGORY record(s).`

#### Things to note

* Searches do not change stored records.
* Use `n/` to search student names in the `student` category; use `sn/` to search associated student names in other supported categories.
* Use `n/` to search tutor names in the `tutor` category; `tu/` is not accepted there.
* Blank filters and repeated prefixes are rejected.
* Text queries must not contain slashes or line breaks.
* Use `list` to return to the complete person list. Before an index-based operation following a lesson search, display the relevant person list and check the indices.

### 7. Recording and removing attendance: `mark` / `unmark`

Records attendance for one dated occurrence of a student's recurring lesson.

#### Marking attendance

**Format:** `mark INDEX LESSON_INDEX d/DATE s/STATUS`

**Parameters:**

| Parameter | Description and accepted values |
|-----------|---------------------------------|
| `INDEX` | The student's index in the currently displayed person list. |
| `LESSON_INDEX` | The lesson's index within that student's lesson list. |
| `d/DATE` | A real calendar date in YYYY-MM-DD format. Its weekday must match the recurring lesson's scheduled day. |
| `s/STATUS` | `present` or `absent`, case-insensitive. The stored status is lowercase. |

**Example:**
Assuming student 1's lesson 1 is scheduled on Mondays:
`mark 1 1 d/2026-09-21 s/present`

**Expected result:**
PonHub displays a confirmation such as:
`Marked attendance for Alex Tan: 2026-09-21 - present.`

The attendance date and status appear under the student's lesson.

**Correcting an existing status:**
Run `mark` again for the same student, lesson and date with the corrected status:
`mark 1 1 d/2026-09-21 s/absent`

PonHub updates the existing entry instead of creating another one:
`Updated attendance for Alex Tan: 2026-09-21 - absent.`

If the same status is already recorded, PonHub leaves it unchanged and reports:
`Attendance already marked for Alex Tan: 2026-09-21 - absent.`

#### Removing attendance

**Format:** `unmark INDEX LESSON_INDEX d/DATE`

**Example:**
`unmark 1 1 d/2026-09-21`

**Expected result:**
PonHub removes the attendance entry for that occurrence and displays:
`Unmarked attendance for Alex Tan: 2026-09-21.`

The recurring lesson remains unchanged. If no attendance entry exists for that date, PonHub reports:
`No attendance record found for Alex Tan on 2026-09-21.`

#### Things to note

* A missing attendance record means attendance has not been recorded. It does not mean the student was absent.
* Both `mark` and `unmark` require a date matching the lesson's scheduled weekday.
* Invalid dates such as `2026-02-30` are rejected.
* Only `present` and `absent` are supported. Values such as `late`, `excused` and `yes` are invalid.
* Do not supply `s/STATUS` with `unmark`.
* Attendance commands preserve the current person filter and list order.

### 8. Viewing command help: `help`

Displays command guidance directly in PonHub's Result Display.

#### Format

`help [COMMAND]`

#### Examples

| Command | Result |
|---------|--------|
| `help` | Displays the command summary. |
| `help add` | Displays the syntax, parameters and examples for adding people. |
| `help addlesson` | Displays guidance for adding recurring lessons. |
| `help search` | Displays search syntax and supported filters. |
| `help mark` | Displays attendance-marking guidance. |

The optional command keyword is case-insensitive. For example, `help MARK` requests the same guidance as `help mark`.

#### Expected result

Help appears inside the application without opening a browser or another window. Your current person and lesson views, filters, ordering and selection remain unchanged.

#### Common errors

* An unknown command, such as `help remove`, produces:
  `Unknown command: 'remove'. Type 'help' to see available commands.`
* Supplying more than one keyword, such as `help mark attendance`, produces:
  `Invalid command format. Usage: help [COMMAND]`

### Handling unsuccessful commands

When a command fails, PonHub displays an error explaining the problem. Failed commands leave stored records unchanged. If saving a change fails, PonHub restores the previous state and does not report success.

Correct the reported problem and try again. If several inputs are invalid, PonHub reports the first detected error.

--------------------------------------------------------------------------------------------------------------------

## FAQ

**Q**: How do I transfer my PonHub data to another computer?<br>
**A**: Close PonHub on both computers. Install the same or a compatible PonHub version on the new computer, then copy the `data` folder from the folder containing the old JAR to the folder containing the new JAR. Keep a backup of the original folder until you have opened PonHub and checked your records on the new computer.

**Q**: Which index should I use for a student or lesson?<br>
**A**: Use the number shown in the current list. Filtering or searching can change displayed indices. For attendance, provide both the student index and the lesson index shown for that student.

--------------------------------------------------------------------------------------------------------------------

## Known issues

PonHub-specific known issues have not yet been documented in this guide.

--------------------------------------------------------------------------------------------------------------------

## Command summary

Action | Format and example
-------|-------------------
**Add student** | `add r/student n/NAME l/LEVEL pp/PARENT_PHONE [p/PHONE] [e/EMAIL] [a/ADDRESS]`<br>e.g., `add r/student n/Alex Tan l/S2 pp/91234567`
**Add tutor** | `add r/tutor n/NAME p/PHONE [e/EMAIL] [a/ADDRESS]`<br>e.g., `add r/tutor n/Mei Lim p/92345678`
**Add parent** | `add r/parent n/NAME p/PHONE [e/EMAIL] [a/ADDRESS]`<br>e.g., `add r/parent n/Pat Tan p/91234567`
**List people** | `list [r/ROLE]`<br>e.g., `list r/student`
**Delete person** | `delete INDEX`<br>e.g., `delete 2`
**Add lesson** | `addlesson INDEX d/DAY st/START_TIME et/END_TIME s/SUBJECT tu/TUTOR_NAME rm/ROOM`
**Delete lesson** | `deletelesson INDEX LESSON_INDEX`<br>e.g., `deletelesson 1 2`
**Search** | `search c/CATEGORY [FILTER_PREFIX/VALUE]...`<br>e.g., `search c/student s/Math d/Mon`
**Mark attendance** | `mark STUDENT_INDEX LESSON_INDEX d/DATE s/STATUS`<br>e.g., `mark 1 2 d/2026-09-18 s/present`
**Remove attendance mark** | `unmark STUDENT_INDEX LESSON_INDEX d/DATE`<br>e.g., `unmark 1 2 d/2026-09-18`
**Help** | `help [COMMAND]`<br>e.g., `help add`
**Exit** | `exit`

`ROLE` is `student`, `tutor`, or `parent`. `CATEGORY` in `search` selects the kind of result; available filters depend on that category. Square brackets mean optional input and are not typed.
