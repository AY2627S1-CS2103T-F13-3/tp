---
layout: page
title: Developer Guide
---

## Introduction

PonHub is a desktop app for tuition centre administrators. It keeps student, tutor and parent details,
weekly lessons, enrolments and attendance in one place. Most tasks can be done by typing commands.

This guide explains how the app works and how to change it safely. For command syntax and examples,
see the [User Guide](UserGuide.html#current-command-summary).

If you are new to the project, read these sections first:

1. [Setting up](#setting-up-getting-started): prepare your development environment.
2. [Architecture](#architecture): understand the main parts of the app.
3. [How the records fit together](#shared-lesson-target-contract): understand people, lessons and attendance.
4. [How a command changes data](#command-transactions): follow a change from input to saved result.
5. [Manual testing](#appendix-instructions-for-manual-testing): check the app as a user would.

<a id="maintainer-reading-path"></a>

### Terms used in this guide

Some class names use technical terms. Here is what they mean in PonHub:

| Term | Meaning |
| --- | --- |
| Canonical data | The main set of records that the app treats as correct. Other views read from it. |
| Root or aggregate | The object holding all operational data: people, lessons, rosters, attendance and ID counters. |
| Snapshot | A complete description of that data at one point in time. |
| Immutable | Cannot be changed after creation. An edit creates a replacement object. |
| Staged change or candidate | A proposed change prepared separately from the live data. |
| Transaction | The steps used to prepare, save and apply one command's changes together. |
| Publish or commit | Make a successfully saved change available to the live model and its views. |
| Projection or derived view | A list built from the main records, such as a student's lessons. It is not another editable store. |
| Observable list | A list that tells the UI when its contents change. |
| Invariant | A rule that must always hold, such as every lesson referring to an existing tutor. |
| No-op | A valid command that makes no change. |

* Table of Contents
{:toc}

## Acknowledgements

* PonHub evolves [AddressBook-Level3](https://github.com/se-edu/addressbook-level3) by the
  [SE-EDU initiative](https://se-education.org); existing licences and reuse credits are retained.
* Existing libraries: [JavaFX](https://openjfx.io/), [Jackson](https://github.com/FasterXML/jackson),
  and [JUnit 5](https://junit.org/junit5/).
* The optional logo-click percussion cue was synthesized for PonHub at Vincent's direction
  with OpenAI Codex assistance, using oscillators and seeded noise without third-party audio samples.
  The generator, synthesis description and MIT reuse notice are recorded in
  [the audio resource note](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/master/src/main/resources/audio/README.md).
* OpenAI Codex was used by Ernest Chua (`ErnestChuaa`), Benjamin Ng (`ben58882`),
  Yang Shuo (`ys000009`), Vincent Lin (`gugrusaurus`) and Zhu Zhi Yu (`ultramanarm`)
  across implementation and refactoring, test development, documentation, review support,
  integration/conflict reconciliation and verification support. The students remain responsible
  for the submitted work; this acknowledgement preserves each contributor's feature ownership
  and does not claim unaided authorship or human checks that were not performed.

## Setting up, getting started

Follow [Setting up and getting started](SettingUp.html). Use Java 25 for both the project SDK and Gradle JVM.
On macOS, use the course-prescribed Azul JDK with JavaFX (`25.0.3.fx-zulu`) for your Mac's architecture.

### Development assertions

`./gradlew run` and Windows `gradlew.bat run` enable Java assertions. Gradle's `test` task also enables them.
If IntelliJ runs the Gradle `run` task, no extra setting is needed. If it launches `ponhub.Main` through
an **Application** configuration, add `-ea` to **VM options**.

Use assertions to catch programming mistakes. Use normal checks and exceptions to validate user input
and files: those checks must still work when assertions are off. Users launch the packaged app with
`java -jar ponhub.jar`; they do not need `-ea`.

See the [Testing guide](Testing.html#running-tests) for automated checks.

## Design

Diagram source files are in `docs/diagrams`. Edit the `.puml` files when changing a diagram; the
[PlantUML tutorial](https://se-education.org/guides/tutorials/plantUml.html) explains how.

### Architecture

[![Main components and their dependencies](images/ArchitectureDiagram.png)](images/ArchitectureDiagram.png)

PonHub has five main components:

| Component | What it does |
| --- | --- |
| [UI](#ui-component) | Shows records, accepts commands and displays feedback. |
| [Logic](#logic-component) | Reads commands and coordinates their execution and saving. |
| [Model](#model-component) | Holds the records, checks business rules and supplies lists for display. |
| [Storage](#storage-component) | Loads and saves local files. |
| [Commons](#common-classes) | Provides shared helpers for settings, logging, files and errors. |

`Main` starts JavaFX. `MainApp` checks file paths, creates one `CanonicalPeopleSession`, connects it
to `CanonicalLogicManager`, and starts the UI. Preferences are loaded and saved separately from tuition records.

The UI calls the `Logic` interface. It does not edit stored records directly. The session owns the main
model and query helpers, and gives the command executor a writer for complete snapshots. Tests can replace
that writer to check save failures. The architecture arrows show calls and dependencies, not separate data stores.

#### Follow an edit command
{: id="canonical-command-collaboration"}

Consider `edit 1 n/Jamie Tan`, with a valid person at position 1:

1. `MainWindow` passes the text to `Logic.execute`.
2. `CanonicalLogicManager` passes it to `CanonicalPeopleSession`.
3. The session checks whether the command is allowed and parses its arguments.
4. The executor prepares and validates the edit using a separate copy of the data and people view.
5. If data changed, the executor saves the complete new snapshot.
6. Only after saving succeeds does `commit()` update the live data and refresh the views.

[![Edit command: prepare and save](images/ArchitectureSequenceDiagram.png)](images/ArchitectureSequenceDiagram.png)

[![Edit command continued: apply the result or report failure](images/ArchitectureSequenceDiagram_001.png)](images/ArchitectureSequenceDiagram_001.png)

Read the diagrams in order. A parse, validation or save failure stops the command and leaves the live
records, ID counters and views unchanged. Preparing an edit is not success. A no-op skips saving;
a command that only changes a view can apply that view change without writing operational data.

`editlesson` follows the same process through `LessonEditParser` and `CanonicalEditLessonCommand`.
See [Editing records](#v14-editing-contract) for edit rules and
[Command transactions](#command-transactions) for saving and failure handling.

### Command integration
{: id="current-increment-and-command-integration"}

All active commands use the same session and main model. The
[User Guide command summary](UserGuide.html#current-command-summary) lists them. The inherited AB3
command and storage paths have been replaced; `clear` and `find` are unavailable.

`CanonicalCommandDefinition` defines each command's name, help information and availability during
protected startup. `CanonicalCommandCatalog` builds help from those definitions. Normal help follows
definition order; protected help lists only `help`, `list` and `exit`.

`CanonicalPeopleSession.execute()` looks up the command once and routes it through an exhaustive enum
switch. There is no default branch: adding a definition also requires adding its route before compilation
succeeds. Protected sessions reject unavailable commands before parsing their arguments.

When adding or removing a command, update its definition, parser, help, route and related tests.
Update the User Guide and manual checks too. Keep the same model, views and compatible storage format.

<a id="canonical-application-startup"></a>

### Application startup and file locations
{: id="canonical-application-startup-85"}

For a packaged app, **application home** is the folder containing the JAR, even if the terminal is in
another folder. Development launches use the working directory instead.

| File | Location |
| --- | --- |
| Tuition records | `data/ponhub.json` by default, or the path supplied with `--data=PATH`. |
| Preferences | `preferences.json` in application home. |
| Logs | `ponhub.log*` in application home, using the `ponhub` logger. |

Relative data paths start from application home. Absolute paths are allowed only inside it. For example:

```sh
java -jar ponhub.jar --data="separate store/ponhub.json"
```

The footer shows the selected data path. An old address-book path in preferences does not change it.

Before reading data or preferences, startup rejects blank paths, `..` escapes and symbolic links leading
outside application home. Data, preferences and reserved log files must not refer to the same file.
This includes hard links, symbolic links, case variants, rotation/lock names and future log suffixes.
Both `ponhub.log*` and legacy `addressbook.log*` names are reserved at the home root. Old logs are left alone.

File logging begins after these checks. If a `ponhub.log*` file is a symbolic link, or a new log aliases
a legacy log, logging stays on the console.

The session then loads the selected data file once:

| Outcome | What happens |
| --- | --- |
| File is confirmed missing | Start empty, without sample records. Do not create a data file until a successful addition. |
| File is supported and valid | Load the complete snapshot. |
| File cannot be safely loaded | Preserve it and open a protected empty view with recovery guidance. Only `help`, `list` and `exit` work. |

Repairing or removing a rejected file does not unlock the running session. Restart after recovery.
Default startup leaves `data/addressbook.json` untouched. Selecting a legacy AB3 file explicitly rejects
and preserves it; there is no automatic migration or recovery command.

<a id="window-placement-and-active-contact-card-layout"></a>

### Window layout and keyboard input

`WindowPlacement` fits saved window size and position to the available screens. It uses the screen with
the largest overlap, keeps usable secondary-monitor positions, and centres a new or wholly offscreen
window on the primary screen. Invalid dimensions use defaults. JavaFX work-area bounds already account
for display scaling and desktop bars; do not divide them by the scale again.

The window has a minimum size of 450 × 360 logical pixels where the work area allows it. Startup fitting
does not prevent later enlargement. The people sidebar starts at 32% of record width and is adjustable.

The wordmark and menus share a compact header. Viewing buttons, guidance and feedback share a resizable
section above the record lists. Both lists scroll independently. The command box is below both columns,
outside their split panes, so moving a divider does not move command entry into one column.

Startup focuses the command box. Result Display is also focusable so users can select, copy and scroll
feedback. Its orange focus border does not change its size. Menu/F1 help keeps unfinished command text.

#### Command-name completion
{: id="command-completion-design"}

`Logic.getCommandWords()` supplies the available commands in catalogue order. `MainWindow` gives this
list to `CommandBox`; `CommandWordCompleter` uses it when Tab is pressed.

Completion replaces only the first word, using the prefix before the caret. Repeated Tab presses cycle
through the original matches. Editing text, moving the caret, changing selection or changing focus
starts a new cycle. Completion's own updates do not reset it.

Only a successful completion consumes unmodified Tab. With no eligible match, Tab moves focus normally.
Shift+Tab always keeps its normal focus behaviour. Enter still submits commands and retains failed drafts.
Protected sessions offer only their three available commands.

#### Command recall
{: id="command-recall-design"}

`CommandBox` keeps the latest 50 nonblank submissions in memory, including failed commands.
Up and Down browse them without executing anything. The first Up saves the current draft, caret and
selection; Down beyond the newest command restores them. Editing or completing a recalled command
starts a new draft. Submitting resets browsing, and closing the window clears recall history.
This history is separate from saved attendance history.

#### Raw input protection
{: id="raw-command-input-protection-572"}

`CommandBox` uses a `TextFormatter` to reject a whole text replacement containing ISO controls or Unicode
line/paragraph separators before JavaFX can silently remove them. This covers paste shortcuts and the
context menu. After rejection, Enter stays blocked until the text changes; moving the caret is not enough.
Parsers still validate commands separately.

### How the records fit together
{: id="shared-lesson-target-contract"}

PonHub keeps one main set of people, shared lessons and dated attendance. These rules apply to commands,
saved files and restored snapshots.

| Data | Where it belongs |
| --- | --- |
| People | Immutable `Student`, `Tutor` and `Parent` records implementing `PersonRecord`. |
| Lesson | One weekly teaching slot with a Lesson ID, Tutor ID, weekday, times, subject, room and student roster. An empty roster is valid. |
| Enrolment | The lesson's `enrolledStudentIds`. Students do not store their own copies of lessons. |
| Attendance | Separate records, each identified by Student ID + Lesson ID + date. Status is `present` or `absent`. |
| Parent link | Exact equality between a parent's own phone and a student's parent phone. A matching Parent record is optional. Leading zeros matter. |
| Displayed lists | Read-only results derived from these records, not another place to edit membership. |

#### Displayed positions and stable IDs

An `INDEX` or `STUDENT_INDEX` is a positive position in the **current people list**, starting from 1.
Filtering can change it. A stable ID such as `S1`, `T1`, `P1` or `L1` identifies the same record across
filtering, edits and restarts.

For example, if the current student list contains `[S4, S9]`, `edit 2 ...` selects `S9`, not `S2`.
Validate the position and role once, then keep the resolved stable ID for the whole operation.
Lesson, roster and history row numbers are never person-command positions.

Existing lessons are selected with `lid/LESSON_ID`, such as `lid/L1`. `lessons STUDENT_INDEX` uses the
same current-people-list rule. The older `lessons si/STUDENT_INDEX` form remains an alias.
Internal relationships and queries use stable IDs rather than displayed positions.

#### Finding a tutor for a lesson

`addlesson` and `editlesson` match `tu/TUTOR_NAME` against a tutor's full normalized name, ignoring case
including Greek `Σ`/`σ`/`ς`, and repeated spaces. If `tp/TUTOR_PHONE` is supplied, it must match exactly,
even when the name is unique. No match fails; multiple name matches require a phone. If older saved Tutors
match both name and phone, require a name/phone correction rather than selecting the first record.
Never fall back to name-only matching after a phone mismatch.

Search is different: search `tu/` accepts a name fragment. To search tutors by their own name, use `n/`.

#### Scheduling rules

Create a lesson once, initially with no students. On the same weekday, different lessons cannot overlap
if they share a tutor, room or enrolled student. Tutor and room checks also apply to empty lessons.
Enrolling another student in an existing lesson does not book its tutor and room again.

Time ranges include the start and exclude the end: `[start, end)`. Thus `1600–1700` and `1700–1800`
are adjacent and allowed. Equal clock times on different weekdays are also allowed.

#### Attendance and deletion rules

A new attendance entry requires current enrolment and a real date on the lesson's weekday. Unenrolment
removes membership only. Existing attendance can still be corrected or removed afterwards.
No entry means **unrecorded**, not absent. Repeating a status makes no change; `unmark` removes one dated
entry without changing enrolment.

Deletion never removes linked records automatically:

| Record | When deletion is blocked |
| --- | --- |
| Student | A lesson roster or attendance entry still refers to the student. |
| Tutor | A lesson still refers to the tutor, even if its roster is empty. |
| Lesson | Its roster is nonempty or any attendance entry refers to it. |
| Parent | No direct lesson/attendance guard. Removing the Parent leaves students' stored parent phones unchanged. |

#### Capacity and ID allocation
{: id="capacity-policy"}

v1.5 has no lesson-capacity field, enrolment cap or fixed total-person cap. Do not truncate valid records,
silently load a subset, or reject data only because of its count. Physical classroom capacity remains
the administrator's responsibility.

Person IDs use separate positive `long` sequences for each role. Lessons use another sequence.
Counters include deleted IDs, so committed IDs are never reused. `Long.MAX_VALUE` means no more IDs
can be allocated. This numeric bound is not a claim about practical or tested capacity.

Snapshots must preserve all records, relationships and counters. Preferences and UI state are separate.
See [Requirements](#appendix-requirements) for proposed features and
[AboutUs](AboutUs.html#project-team) for feature ownership.

<a id="editing-contract"></a>

### Editing records
{: id="v14-editing-contract"}

<a id="planned-v14-editing-contract"></a>

Both person and lesson editing are implemented. An edit creates an immutable replacement, validates it
in a staged snapshot, and saves it before updating the live model. An identical result is a no-op:
there is no operational save or publication. Protected sessions reject both edit commands before parsing.

#### Editing a person

```text
edit INDEX [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [l/LEVEL] [pp/PARENT_PHONE]
```

Resolve `INDEX` once. Keep the same role, ID, position in the full people order, linked records and counters.
At least one editable prefix is required. A missing field keeps its old value.

| Supplied blank field | Result |
| --- | --- |
| `e/` or `a/` | Clear that optional field for any role. |
| `p/` | Clear a student's optional own phone; reject for a Tutor or Parent. |
| `n/`, `l/` or `pp/` | Reject: these required values cannot be cleared. |

`l/` and `pp/` apply only to Students. Reject role/ID changes, unknown or repeated prefixes, invalid
values and fields for the wrong role. Use the same validators as addition and loading.

Check duplicates against every other person, excluding the edited ID. For Students, compare normalized
name and parent phone; for Tutors and Parents, compare normalized name and own phone within that role.
Name matching lowercases with `Locale.ROOT` and unifies final sigma `ς` with `σ`, without expanding `ß` to
`ss` or removing accents. Existing saved sigma-distinct identities remain loadable and may edit unrelated
fields while retaining their business key; add/edit must not introduce a new collision.
A tutor rename keeps lesson links because they use the Tutor ID.

Changing a Parent's phone or a Student's parent phone changes the links derived from exact phone matches.
It does not edit anyone else's phone, create a Parent or require one to exist. `CanonicalEditCommand`
compares links before and after the edit and lists every removed Parent/Student ID pair in successful
feedback. An unchanged phone or a previously unlinked record produces no warning. Save failure returns
failure feedback instead.

`PersonEditInput` represents each field as `Field.keep()`, `Field.clear()` or `Field.set(value)`.
`PersonEditCandidate.prepare(source, targetId, input)` builds the proposed snapshot without changing
live state, allocating IDs or writing files. Its getters expose the candidate, edited person and change flag.
The model accepts all-kept input as a no-op; the command parser still requires an editable prefix.

`PonHubData.replacePerson` installs the replacement only in the staged root, at the same position,
after validating the people registry and complete snapshot. Equal replacement sends no state notification.
Compare all stored values, including display capitalization: extra spaces in `Alex  Tan` normalize away,
but `ALEX TAN` changes the displayed spelling.

<!-- @@author ErnestChuaa -->
`PersonEditParser.Input` holds the validated position and edit fields. `CanonicalEditCommand` resolves
the position in the staged people view and prepares the replacement from a fresh staged snapshot.
The session exposes `edit` and `help edit` only when the command is allowed. The normal executor saves
before publishing the edited person and its feedback.
<!-- @@author -->

#### Editing a lesson

```text
editlesson lid/LESSON_ID [d/DAY] [st/HHMM] [et/HHMM] [s/SUBJECT] [tu/TUTOR_NAME [tp/TUTOR_PHONE]] [rm/ROOM]
```

Require an existing lesson and at least one editable field. Missing fields keep their old values;
supplied fields cannot be blank. `tp/` requires `tu/` and uses the tutor lookup rule above.
Reject unknown and repeated prefixes.

Keep the Lesson ID, catalogue position, roster order, attendance and counters. Validate the complete
resulting time slot even if only one endpoint was supplied. Check tutor, room and enrolled-student
clashes against every other lesson, excluding the edited lesson itself.

A weekday change must remain compatible with **every retained attendance date**, including former members'
records. Otherwise reject it and explain the explicit `unmark` option. Never move or delete history automatically.

`LessonEditInput` contains optional replacements for day, start/end time, subject, Tutor ID and room.
`LessonEditCandidate.prepare` combines supplied and retained values without changing live data or IDs.
`PonHubData.replaceLesson` validates and replaces the staged record at the same catalogue position.
The model accepts all-omitted input as a no-op; the parser requires an actual field.

Changed edits with retained attendance warn that old attendance is displayed using **current lesson details**.
For changes intended only for future lessons, create a new lesson. PonHub does not store past versions
of lesson details, and editing does not change recorded dates or statuses.

<!-- @@author ys000009 -->
`LessonEditParser` parses values; `TutorResolver` resolves the optional replacement tutor;
`CanonicalEditLessonCommand` prepares the edit; the normal executor saves and publishes it.
<!-- @@author -->

#### What happens to the displayed results

After a successful edit, keep the people filter/search and recheck it against the new data. Keep surviving
matches in their relative order and renumber their displayed positions. Preserve the selected person by
stable ID only while still visible; otherwise clear selection rather than selecting the new occupant
of the old position.

Lesson lists, search results, details and history keep their selected stable IDs and refresh names and
fields after saving. Keep the current right-pane mode. If a permitted weekday edit makes a selected
detail date inappropriate, keep the lesson selected but clear the date filter and date heading.
History keys and statuses stay unchanged. Users must recheck people positions before the next indexed command.

Useful regression tests are `PersonEditParserTest`, `CanonicalEditIntegrationTest`,
`LessonEditParserTest`, `CanonicalLessonEditIntegrationTest` and edit cases in `CanonicalMainWindowTest`.
They cover required/optional fields, duplicates, scheduling, identity, views, no-ops, protected startup,
failed saves, retries and reloads. See the manual edit checks below for packaged-app testing.

### UI component

The UI interface is `src/main/java/ponhub/ui/Ui.java`. Layout files are in `src/main/resources/view`.

[![UI controllers, panels and cards](images/UiClassDiagram.png)](images/UiClassDiagram.png)

`UiManager` creates `MainWindow`. The window connects the people panel, result display and other controls.
Panel cells create cards as needed. `UiPart` is the abstract base for the window, panels, cards and controls.
The diagram leaves out FXML fields, JavaFX containment and some helpers to keep it readable.

Solid arrows show stored references; dashed arrows show dependencies or creation. Hollow triangles show
inheritance or implementation. Cards and panels install JavaFX roots rather than keeping a separate
controller collection for every displayed record.

#### People, lessons and attendance panels

The people panel observes the session's read-only people list. Cards distinguish command positions from
stable IDs and wrap both required and optional fields. Missing contacts show `Not provided`.

`LessonListPanel` observes `Logic.getDisplayedLessons()`. Cards show subject, stable Lesson and Tutor IDs,
schedule, room and roster size. `showlesson` switches to a detailed list with separate current-roster
and former-member attendance sections. Both are expanded immediately. Current students without a dated
entry show `unrecorded`.

Detail rows share a virtualized list: JavaFX builds controls for the visible area as users scroll,
rather than building every row at once. All records remain reachable. The hidden catalogue does not
build duplicate detail graphics.

`HistoryListPanel` shares the right pane. It shows recorded present/absent status and a separate
**Enrolment Status:** label: `currently enrolled` or `not currently enrolled`. That label describes
today's roster membership, not membership on the attendance date. Committed changes refresh it.

#### Viewing buttons and result modes

**All lessons**, **Student lessons** and **Attendance history** use the same `Logic.execute` path as
typed commands. The two Student buttons find the selected stable Person ID in the current people list,
require a Student, and build `lessons CURRENT_INDEX` or `history CURRENT_INDEX`.
No selection, a stale selection or a Tutor/Parent selection gives feedback without changing results.

The buttons preserve unfinished command text and use normal command guards. They add no command grammar,
stored data or separate query implementation. Typed commands use their arguments and need no card selection.
The wrapping button/guidance area shares the resizable feedback section; feedback retains a 50-pixel minimum.

`LessonRetrieval.ResultKind` records `ALL_LESSONS`, `STUDENT_LESSONS`, `SEARCH` or `DETAIL`.
Together with `historyViewActiveProperty()`, it controls button indicators. History takes precedence;
catalogue and Student lesson modes activate their matching button; search and detail activate none.
Use these properties, not heading text, to determine mode. Invalid requests do not change mode.
Later card selection, help or people filtering does not retarget a held lesson/history result.

### Logic component

The public interface is `src/main/java/ponhub/logic/Logic.java`.

[![Command handling classes](images/LogicClassDiagram.png)](images/LogicClassDiagram.png)

`CanonicalLogicManager` adapts one `CanonicalPeopleSession` to `Logic`. The session routes commands;
typed parsers turn arguments into validated inputs. They do not use the old AB3 parser interface/tokenizer.

`CanonicalCommandExecutor` calls `PeopleView.beginTransaction()` and runs a `StagedCommand` on the
transaction's separate data and view. Its `SnapshotWriter` calls `ProtectedPonHubDataStorage.saveData`
in the application. After saving, `CanonicalTransaction.commit()` updates the root before applying
the staged people filter, so query listeners read committed data.

Not every command changes stored data. `list` stages a view-filter change but skips operational saving.
People search validates and replaces its derived filter directly. Lesson, detail and history commands
use their retrieval helpers. `help` returns guidance; `exit` requests shutdown. None creates another model root.

<a id="read-only-retrieval-api-contract"></a>

#### Read-only retrieval
{: id="read-only-retrieval-api-contract-113"}

`LessonQuerySource` defines the lesson-query interface and `LessonQueries` implements it using the same
model as the session. `LessonView`, `StudentView` and `AttendanceHistoryEntry` supply display values;
they do not become new stored records.

| Result | Default order and behaviour |
| --- | --- |
| Lesson catalogue/search | Each matching lesson once, in creation order, including empty lessons. |
| Student lessons | Current membership from lesson rosters. |
| Lesson roster | People creation order. |
| Tutor schedule | Weekday, start time, then numeric Lesson ID. |
| Attendance history | Newest date first, then numeric Lesson ID; includes former enrolments. |
| Lesson lookup by ID | Looks in the full catalogue, regardless of its displayed filter. |

Queries do not save files or allocate IDs. Returned observable lists cannot be modified by callers.
A held list remains the same object while committed changes or restores replace its entries.
Known records with no matches give empty results. Looking up an unknown lesson returns an empty `Optional`;
list queries reject null, missing or wrong-role IDs as specified in `LessonQuerySource`.

`Logic.getDisplayedLessons()` exposes one stable list for the currently selected lesson source.
`AttendanceRetrieval` resolves a Student position once and then retains that Student ID.
Switching the right pane between lessons and history does not replace the people list.

### Model component

[![Person records and contact values](images/PersonRecordClassDiagram.png)](images/PersonRecordClassDiagram.png)

[![Lessons, tutors and roster membership](images/LessonMembershipClassDiagram.png)](images/LessonMembershipClassDiagram.png)

[![Lesson identity and values](images/LessonValuesClassDiagram.png)](images/LessonValuesClassDiagram.png)

[![Attendance is separate from membership](images/AttendanceClassDiagram.png)](images/AttendanceClassDiagram.png)

[![Attendance key and status](images/AttendanceValuesClassDiagram.png)](images/AttendanceValuesClassDiagram.png)

`PonHubData` holds one current immutable `PonHubDataState`. This contains the people registry, ordered
lessons, attendance and allocation counters. Each person has a stable ID and contact details; each lesson
has a Tutor ID and Student-ID roster; each attendance key has Student and Lesson IDs plus a date.
The model resolves those IDs to records. Diagram relationships do not imply extra stored object fields.

The paired diagrams separate relationships from value types. Repeated boxes refer to the same class.
Immutable objects may safely be shared across snapshots; the diagrams' sharing does not create another
writable owner of membership or attendance.

#### Example: enrolment is not attendance

[![Changes to a lesson roster and dated attendance](images/LessonAttendanceStateDiagram.png)](images/LessonAttendanceStateDiagram.png)

Suppose Monday lesson `L1` has Alex (`S1`) and Jamie (`S2`) enrolled:

1. Mark Alex present and Jamie absent for `2026-10-05`: two attendance keys are created.
2. Mark Alex absent for the same date: only Alex's status changes.
3. Unenrol Alex: only `S1` leaves the roster. Alex's absence remains as former-member history.
4. Unmark Alex's entry: that dated key disappears. Jamie and the lesson are unchanged.

Each changed step saves before it updates live views. The diagram starts after setup and stops after
step 4; it does not mean the lesson or app ends there.

<a id="canonical-data-aggregate"></a>

#### Complete model snapshots
{: id="canonical-data-aggregate-76"}

`PonHubDataState` copies incoming collections and validates the **whole** candidate before replacement:
IDs, roles, references, duplicates, schedules, attendance and counters must agree. Invalid or null
replacement leaves the current model unchanged. There is no partially installed snapshot.

Snapshot equality includes order, records, relationships and allocation history. Counters must cover
retained IDs and deleted allocations; never calculate them from surviving records. Zero means no IDs
have been allocated, and `Long.MAX_VALUE` means exhaustion. Discarding an uncommitted candidate can
return to earlier counters, but committed IDs remain reserved.

People validation uses temporary hash sets for IDs and legacy duplicate keys instead of comparing every pair.
The legacy key uses the original lowercase-only name, role and identifying phone so pre-existing sigma-distinct
records and relationships remain valid. New additions use `isDuplicateOf`; `replacePerson` separately checks
changed business identities with sigma equivalence. For people validation alone, expected time and temporary
memory grow linearly with record count.
This does not promise the same cost for scheduling, complete-file saving or UI refresh.

Snapshots and containers may share immutable records safely. Replacing one container cannot alter an older
snapshot. Read-only lookups, collections and `stateProperty()` expose complete validated data. People filters,
selection and preferences stay outside operational snapshots. `PeopleView` supplies the displayed people list.

<a id="shared-lesson-scheduling-model-apis"></a>

#### Creating and changing lessons
{: id="shared-lesson-scheduling-model-apis-78"}

`PonHubData` is the boundary for lesson changes and ID allocation. `addLesson(...)` checks the Tutor ID,
uses the next positive Lesson ID, creates an empty-roster lesson and validates the complete proposed state.
Rejected candidates do not consume an ID. Committed IDs remain reserved after deletion and reload.

`PonHubDataState` applies the same scheduling rules to commands, restored snapshots and loaded files.
Empty lessons still reserve their tutor and room. Student clashes apply when the same Student ID belongs
to overlapping rosters. Different days, adjacent slots, and overlaps with different tutors, rooms and
students are valid.

<a id="shared-lesson-creation-command"></a>

#### Creating a lesson through a command
{: id="shared-lesson-creation-command-86"}

`AddLessonParser` builds `LessonCreationInput`. It checks required fields and typed values without reading
operational data or allocating IDs. `tp/` is optional. Preambles, controls, unknown/repeated prefixes and
missing/blank values fail.

`CanonicalAddLessonCommand` finds one tutor in the staged data using the full-name/phone rule, then
calls `PonHubData.addLesson(...)`. The executor saves the whole snapshot before publishing it.
Any parse, lookup, clash, allocation or save failure leaves the live catalogue, counters, people filter
and query lists unchanged. Success reports the new stable Lesson ID.

<a id="indexed-lesson-query-projections"></a>

#### Lesson queries and indexes
{: id="indexed-lesson-query-projections-80"}

`LessonQueries` derives lists from committed data. The session shares it between lesson retrieval and
people relationship search. The UI owns neither membership records nor indexes.

Indexes are lookup tables built when the model changes: IDs locate records, reverse membership locates
lessons for students, parent phones locate children, and weekday/start/end buckets narrow lesson searches.
They are not rebuilt for every read-only query.

<a id="follow-one-indexed-search"></a>

##### Worked search

Suppose Mei teaches these lessons:

| ID | Schedule | Subject / room | Roster |
| --- | --- | --- | --- |
| L1 | Mon 1600–1730 | Math / R1 | Jamie (S2) |
| L2 | Tue 1600–1730 | Math / R1 | Alex (S1) |
| L3 | Mon 1800–1900 | Science / R1 | Alex (S1) |
| L4 | Mon 1900–2000 | Math / R2 | Empty |

For `search c/lesson d/Mon st/1600 s/Math tu/mei rm/r1 sn/a`:

1. Monday's bucket is `[L1, L3, L4]`; 1600's is `[L1, L2]`. Start with the smaller bucket.
2. Check **all** supplied conditions on each candidate. L2 fails Monday; L1 passes, including Jamie's name
   containing `a`. Return L1 once, in catalogue order.
3. Multiple matching students in L1 still produce only one lesson result.

An index only narrows candidates. It does not replace the other checks. Without exact day/time filters,
search considers the whole catalogue. Text fields use fragments, so an exact-prefix index would not
correctly answer `s/ath`.

Lesson-only `tp/` compares the tutor's current complete phone and can be used alone or with partial `tu/`.
Paired `from/` and `to/` select positive overlap: `lesson.start < to && lesson.end > from`.
Touching an endpoint is not overlap. Exact `st/` and `et/` still apply if supplied.

`sort/schedule` orders matches by Monday–Sunday, start time and numeric Lesson ID. It adds sorting work
but never changes catalogue storage order. Omission or `sort/creation` uses creation order for that request.

For bounded field lengths, an unsorted lesson search costs `O(B + R_B + K)`: candidate lessons `B`,
their roster links `R_B` when `sn/` is used, and returned results `K`. Sorting adds `O(K log K)`.
These costs exclude index rebuilding, writes and UI work.

<a id="select-a-view-without-changing-another-panel"></a>

##### Keeping the two panels independent

[![Lesson retrieval and displayed-result ownership](images/LessonRetrieval.png)](images/LessonRetrieval.png)

`LessonRetrieval` owns one read-only displayed list. The UI binds to it once. Choosing another source
detaches the previous listener and uses the catalogue, a student's lessons, a search or lesson details.
Headings retain the selected stable IDs or filters independently of later people-list changes.

| Request | What it changes |
| --- | --- |
| People search | People criteria and context only. |
| `lessons INDEX` | Right-pane lessons for the resolved Student ID. Later people filtering does not retarget it. |
| Lesson search | Lesson filter and lesson result source only. Empty lessons qualify unless `sn/` requires a roster match. |
| `showlesson lid/ID [d/DATE]` | Details of that lesson from the full catalogue. A supplied date must match its weekday. |
| `history INDEX [lid/ID] [d/DATE] [s/STATUS]` | Student history, optionally narrowed by lesson, exact date and recorded status. All filters must match. |

Read-only requests validate before changing results. Invalid positions, roles, dates or filters leave
the previous panels intact. They do not stage a data copy, save or allocate IDs. A new history request
replaces the previous history filters; omitted filters clear those restrictions.

<a id="refresh-only-after-publication"></a>

##### Refreshing after a save

[![Lesson query refresh: prepare and save](images/LessonQueryRefresh.png)](images/LessonQueryRefresh.png)

[![Lesson query refresh: update the live results](images/LessonQueryRefresh_001.png)](images/LessonQueryRefresh_001.png)

With a Math query selected, consider `editlesson lid/L1 s/English`:

| Step | Saved/live records | Displayed Math results |
| --- | --- | --- |
| Prepare | Candidate is English; file and live lesson are still Math. | Still contains L1. |
| Validation/save fails | Discard the candidate; follow file-recovery rules if needed. | Same filter, source, entries and list objects. |
| Save succeeds, then commit | File and live lesson become English. IDs, roster and attendance remain. | Indexes rebuild; L1 leaves the Math query. A held student-lesson list now shows English. |

`LessonQueries` listens to the root's `stateProperty()`, rebuilds indexes, then refreshes subscribed lists
and the active filter. Held lists keep their identity; their immutable entries are replaced. A previously
returned `searchLessons(...)` snapshot still describes the earlier result.

A held subscription for a deleted record becomes empty and can refill on model restore. A new subscription
for a missing ID fails. Lesson detail refreshes its selected ID and clears an incompatible selected date
after an allowed weekday edit. Deleting history's selected Student or Lesson clears history selection;
unenrolment changes membership labels without removing attendance.

<a id="query-subscription-lifetime-574"></a>
<a id="find-exact-contracts-and-regression-checks"></a>

##### Subscription lifetime

Existing Student, Tutor and Lesson IDs have strongly cached result lists, even if unassigned or empty.
Deleting an identity removes its active cache entry and clears held results. Retired handles use weak keys,
so the query source does not keep unused results and IDs alive indefinitely. A public wrapper keeps its own
result alive; if the model restores the identity, a still-held result rejoins the cache and refreshes.
This applies to student lessons, tutor schedules, rosters and both history query routes.

All model/query access runs on one model thread, or the JavaFX application thread with attached controls.
Refresh copies the subscription entries before notifying listeners, so a callback can request another subscription.
Index rebuilding, schedule sorting and copying subscribed results have costs separate from read-only search.

See `LessonQuerySource` for method signatures, ordering and errors; `LessonQueries` for implementation;
`LessonQueriesTest` for comparisons with a raw-record scan; and `CanonicalLessonRetrievalIntegrationTest`
for routing, panel independence and failed-save checks.

### Storage component

[![Operational storage and separate preferences](images/CanonicalStorageClassDiagram.png)](images/CanonicalStorageClassDiagram.png)

`ProtectedPonHubDataStorage` loads and validates the selected operational file. `JsonPonHubDataCodec`
converts between JSON and complete snapshots. After a valid load or confirmed absence,
`CanonicalDataFileGuard` protects later saves. Before loading or after rejection, no save guard is available.

Preferences use `JsonUserPrefsStorage` and their own file. Both paths reach `AtomicFileWriter` through
shared JSON/file utilities. `JsonPonHubDataStorage` is a standalone convenience wrapper, not the active
session's save path. Do not bypass the protected wrapper with raw codec writes.

#### Preference recovery

Missing fields keep defaults, unknown fields are ignored, and `windowCoordinates: null` means no saved
position. A null JSON root, null `guiSettings`, or explicit null dimensions/coordinate components is invalid.

On preference-loading failure, startup logs the problem, uses defaults and tries to save them.
**Invalid preferences may be replaced; rejected operational data must be preserved.** Preference failures
do not change tuition records. Failure to save initial preferences leaves defaults usable; shutdown
save failure is logged and does not prevent exit. Valid edited sizes/positions survive reload, subject
to fitting the current screens.

#### Safe JSON saves

Serialization finishes before filesystem writing starts. The shared writer uses UTF-8.
For an existing destination, it resolves the real path: saving through a symbolic link updates the
target while preserving the link. Dangling/cyclic links and inaccessible paths fail before writing.
A new destination uses its resolved parent; missing directories are created as needed.

##### Protecting against a second window

After loading, the active storage remembers the exact JSON text and resolved path, or confirmed absence.
Before each save, `CanonicalDataFileGuard`:

1. Takes a nonblocking OS lock on the persistent empty sibling `<data-file-name>.ponhub-lock`.
2. Checks that the destination and text still match the loaded baseline.
3. Keeps the lock while the shared writer replaces or recovers the file.
4. Updates the baseline only after a successful save.

If another window saved first, this window rejects saving before writing or publishing, and needs a restart.
Its read-only commands still show its old in-memory view. Lock contention gives a retryable error instead.
Cleanup errors after a completed replacement are logged without changing success into failure.

The sidecar is created only on a save attempt and remains after exit. Never remove or replace it while
an instance runs. The `.ponhub-lock` suffix is reserved; symbolic-link or nonempty sidecars are rejected
without truncation. Symbolic aliases of the data file use the target's sidecar.

This protects cooperating current-runtime saves on local filesystems with working locks. Preferences,
standalone codec writes, older builds and external editors do not participate. It is not live synchronization
and does not cover lock tampering, concurrent link changes or power-loss durability.

##### Replacing the file and recovering failures

The writer first writes and closes a complete temporary sibling file, then tries an atomic replacement.
Only `AtomicMoveNotSupportedException` triggers the fallback; permission and other move errors propagate.

The fallback works as follows:

1. Copy the existing destination to a unique `.ponhub-backup-*.bak` sibling. If this fails, do not replace it.
2. Try an ordinary replacement.
3. If replacement fails, restore the old bytes from backup. For a first save, remove any partial destination.
4. If restoration also fails, retain the complete backup and include its path in the error.
   Recover it **before continuing or restarting**; do not delete it.

After success, failed backup cleanup is logged without reporting a failed save. On failure, cleanup errors
are attached to the main error. Directories and temporary/recovery files may remain after errors or abrupt exit.

The fallback is not atomic: another reader may temporarily see an incomplete destination. Its protection
is a recoverable old copy, not continuous availability at the original path. Neither path promises
power-loss durability or preservation of every old file attribute.

<a id="prepared-canonical-transaction-handoff-82"></a>
<a id="canonical-transaction-boundary-82"></a>

#### Command transactions

The central rule is **prepare separately → save completely → update live data → report success**.

`PeopleView.beginTransaction()` gives `CanonicalCommandExecutor` a separate model root and view based
on the complete current snapshot and filter/search. The command resolves positions in that staged view
and validates proposed changes. It must not change the live root or run nested commands.

Parse, command or save failure discards the candidate. Live records, counters, filters, selection and
query results remain unchanged. Read-only commands and operational no-ops skip saving. A view-only change
can still apply without writing data. Unchanged views avoid unnecessary refresh, and observable list
objects keep their identity. Publication itself does no new validation or I/O; preferences have a separate lifecycle.

After the callback, a development assertion checks that the live snapshot, role filter and search criteria
still match their starting values. This check happens before saving and has no side effects. It catches
misbehaving code but cannot undo live changes that faulty code already made. It is not user/file validation;
normal validation and save-error handling also work with `-da`.

<a id="example-candidate-saved-file-and-live-state"></a>

##### Example: adding a parent

Suppose the last allocated Parent ID is P1. A valid `add r/parent n/Robin p/91234567` prepares P2:

| Stage | Candidate | File and live views |
| --- | --- | --- |
| Before command | None | Existing records; Parent counter 1. |
| Prepare | P2 appended; Parent counter 2; all other data retained. | Still old records, counters, filter and selection. |
| Save | Complete candidate written. | File is new; live data stays old until commit. |
| Commit | Candidate becomes live. | New person visible; add resets people to all roles; success returned. |

If saving fails, no P2 appears live and no ID is consumed by that attempt. A corrected retry may still
use P2. Follow the file-recovery rules above: unchanged live data does **not** guarantee unchanged disk
bytes after every replacement/recovery failure.

Rejected startup is different from a failed command: no writable snapshot was loaded and the session
stays protected until restart. The old AB3 executor is retired; `ModelTransaction` remains because
`CanonicalTransaction` implements it.

#### JSON version detection
{: id="json-version-detection-foundation"}

`JsonDataVersionDetector.detect(String)` reads JSON using Jackson without constructing model records
or writing files. `schemaVersion` describes the **file format**, not the app release. Current files use
the positive JSON integer `1`.

| Result | Meaning |
| --- | --- |
| `LEGACY_AB3` | Unversioned object with a `persons` array and optional textual `_comment`, with no other root fields. Preserve for manual re-entry; do not guess roles. |
| `PONHUB_V1_CANDIDATE` | Explicit integer version 1. Still needs complete file and business-rule validation. |
| `INVALID_VERSION` | Null, text, boolean, collection, fractional number, zero or negative version. Do not coerce it. |
| `UNSUPPORTED_VERSION` | Another positive integer, even beyond Java integer range. Do not treat it as legacy data. |
| `UNRECOGNIZED_FORMAT` | Non-object root or an unversioned object outside the legacy format. |
| `MALFORMED_JSON` | Empty/invalid JSON, duplicate keys or trailing content. |

Missing files and read errors are separate outcomes, not empty JSON. Version detection alone never
authorizes loading or saving.

#### Protected startup and recovery
{: id="current-protected-startup"}

<a id="prepared-protected-canonical-loading-84"></a>
<a id="protected-canonical-loading-84"></a>

Load storage once before using it. Writes are blocked before initialization. A missing file gives an
empty unwritten model; a supported file supplies data only after complete validation. Loading itself never writes.

Unreadable files, dangling final links, corrupt data, unsupported versions and legacy files produce
path-specific recovery guidance. Unexpected programming errors still propagate. Rejection preserves the
file and blocks all operational writes, including direct saves. The UI clearly labels saved data as
unavailable. Only `help`, `list` and `exit` work, and `list` repeats the diagnostic.

To recover supported data, close the app, keep a backup, then repair a working copy or restore a known-good
file. Fix permissions if needed. Inspect with another compatible build only on a separate copy.
Restart to validate the result; repairing or deleting the file while the session runs does not unlock it.

The recovery dialog shows concise guidance above a read-only wrapped diagnostic TextArea. It fits within
the owner's work area and can be resized. Details get keyboard focus and support scrolling, selection
and copying; Tab reaches OK and Escape closes the dialog. Long paths or field names must not push
the summary or button out of reach.

<a id="canonical-json-schema"></a>

#### JSON file format
{: id="canonical-json-schema-79-81-83"}

<a id="people-only-canonical-json-storage-79"></a>

`JsonPonHubDataCodec` reads and writes one complete UTF-8 version-1 document:

```json
{
  "schemaVersion": 1,
  "people": [
    {"id": "S1", "role": "STUDENT", "name": "Alex", "level": "P1", "parentPhone": "00123456"}
  ],
  "personCounters": {"STUDENT": 1, "TUTOR": 0, "PARENT": 0},
  "lastAllocatedLessonSequence": 0,
  "lessons": [],
  "attendance": []
}
```

People stay in global creation order. Roles are `STUDENT`, `TUTOR` and `PARENT`, matching the ID prefix.
Every person needs `id`, `role` and `name`; Students also need `level` and `parentPhone`, while Tutors
and Parents need `phone`. Absent optional values are omitted, not null. Phones stay strings to preserve
leading zeros. Unknown fields and fields for the wrong role are rejected.

All three person counters and the lesson counter are required nonnegative integers up to `Long.MAX_VALUE`.
They must cover retained and deleted allocations; do not recompute them from the arrays.

A lesson entry has this form (the referenced people must exist in the complete file):

```json
{
  "id": "L2",
  "tutorId": "T1",
  "day": "Mon",
  "startTime": "0900",
  "endTime": "1000",
  "subject": "Mathematics",
  "room": "R1",
  "enrolledStudentIds": ["S1", "S2"]
}
```

All lesson fields are required. Days are Mon–Sun and times are four-digit strings. Reject unknown
fields, wrong types and duplicate roster IDs before making a set. References may point to records
anywhere in the file. Validate all scheduling rules and retain lesson-array order.

<a id="retained-attendance-persistence-83"></a>

Attendance entries are separate:

```json
{"studentId": "S1", "lessonId": "L2", "date": "2026-10-12", "status": "present"}
```

All four fields are required strings. Dates must be real `YYYY-MM-DD` dates in years 0001–9999.
Status accepts present/absent ignoring surrounding whitespace and case, and saves lowercase. Reject
unknown fields, missing references, duplicate keys and dates on the wrong lesson weekday. Former-member
history is valid. Corrections replace a key's status; removal omits that key. Array order affects snapshot equality.

Before editing JSON, close the app and back it up. Whitespace and object-field order do not matter;
array order and counter history do. Adding an optional email string is valid; numeric phones, missing
required fields or counters below retained IDs are not. No partly valid snapshot is loaded.

<a id="protected-loading-and-legacy-upgrade"></a>

#### Legacy files and changed contact rules
{: id="planned-protected-loading-and-legacy-upgrade"}

AB3 contacts have no explicit roles; Students also need a level and parent phone. Keep the original
folder and backup. Choose an unused `--data=PATH` inside application home, or copy the JAR into a separate
empty folder. Re-enter records with `add`, choosing roles and required values explicitly. Verify the new store
before retiring the old working folder. There is no import, export or recovery command.

Current validation may reject old numeric-only names, long phones, invalid slash addresses, or duplicates
revealed by normalization. Inspect read-only or use an older build on another copy. Let the administrator
supply corrections; never guess roles, drop records or convert the original in place.

<a id="preference-json-resource-limits-578"></a>

#### JSON resource limits

Operational JSON is scanned before the codec builds a tree. Maximum nesting is 64 containers.
Non-version numeric tokens have a 64-character limit including signs, decimal points and exponents.
Counters still need to pass their normal range checks. String length and array width are not limited by
this numeric rule. Root `schemaVersion` is checked as text without building a huge `BigInteger`:
positive future integers are unsupported; zero, negative and noninteger versions are invalid.
Duplicate fields and trailing roots fail too. Oversized numbers trigger protected loading and preserve the file.

Preferences have a separate `JsonUtil` scan: 64-character numeric tokens and 64-container nesting,
including unknown fields, before deserialization. Preference errors retain at most 512 characters of
diagnostic detail plus a truncation marker. The warning and `DataLoadingException` use that bounded
detail without retaining Jackson's unbounded exception. Startup then uses reloadable default preferences
and leaves operational data alone.

Resource-limit tests cover boundary lengths/depths and very large numeric values. Run stress probes
with disposable files in a bounded subprocess; do not turn a machine-specific duration into a unit-test promise.

### Common classes

`ponhub.commons` contains shared settings, value helpers, exceptions, JSON/file/string utilities and
`LogsCenter`. Put reusable infrastructure here. Keep business rules in Model and command syntax in Logic.
See [Logging](Logging.html) for logging setup and console fallback.

## Implementation

<a id="lesson-scheduling-value-contracts"></a>
<a id="immutable-shared-lesson-records"></a>

### Lesson values and immutable records

Types in `ponhub.model.lesson` validate the same values for commands and saved files:

| Type | Rule |
| --- | --- |
| `LessonId` | `L` followed by a positive `long` sequence. |
| `LessonDay` | Mon–Sun. |
| `LessonTime` | Valid four-digit 24-hour time. |
| `LessonTimeSlot` | End later than start; includes start but excludes end. |
| `Subject` | 1–50 Unicode letters/digits, with single spaces or hyphens separating nonempty parts, such as `A-Math`. |
| `Room` | 1–10 total characters: letters/digits with optional single hyphens between nonempty parts. Uppercase normalization makes `b-01` and `B-01` equal. |

`Lesson` holds validated values, stable IDs and an unmodifiable copy of its Student-ID roster. It rejects
non-Student member IDs; an empty roster is allowed. Membership changes create a replacement at the same
catalogue position. The full model checks references, clashes, allocation and deletion guards.

### Inline help

`CanonicalCommandCatalog` supplies descriptions, usage and runnable examples. `help` gives one short
description per command and points to `help COMMAND`. Topic parsing accepts zero or one topic,
normalizes case with `Locale.ROOT`, and rejects unknown or extra topics. Protected sessions expose
only `help`, `list` and `exit` guidance.

Help returns normal feedback through `CommandResult`; the result also carries the exit flag.
There is no separate help effect/window. Help menu and F1 call `MainWindow.executeCommand("help")`
while preserving the draft and selected person. `ResultDisplay` is a read-only wrapping TextArea
with vertical scrolling; each response starts at the top. Help does not save operational data.

<a id="person-record-foundation"></a>
<a id="ordered-people-registry-foundation"></a>
<a id="registry-apis"></a>
<a id="allocation-and-state-validation"></a>
<a id="deletion-and-activation-boundaries"></a>

### People records and identity

`Student`, `Tutor` and `Parent` implement `PersonRecord` and contain `ContactDetails`. They do not
extend the old AB3 `Person`, and Students do not store copied lessons or attendance.

| Role | Required fields | Optional fields |
| --- | --- | --- |
| Student | Student ID, name, education level, parent phone | Own phone, email, address |
| Tutor or Parent | ID with the matching role, name, own phone | Email, address |

`EducationLevel` accepts P1–P6, S1–S5, JC1 and JC2, normalized to uppercase. `PersonId` has a role
prefix and positive `long` sequence with no leading zeros; constructor input is trimmed and case-normalized.
The ID stays the same when contacts or displayed positions change.

Record equality compares the ID and every stored field. Duplicate checking instead compares
**role + normalized name + identifying phone**. Students use parent phone; Tutors/Parents use own phone.
Name case, repeated spaces and canonically equivalent Unicode spelling do not distinguish duplicates.
New/changed business identities use `Locale.ROOT` lowercase with `ς` folded to `σ`, without transliteration
or accent removal, so José and Jose differ. Loading uses legacy lowercase-only keys to retain existing
sigma-distinct records and their IDs; an unrelated edit may retain that existing business key.
Another ID, level or optional contact does not bypass the rule. Different roles remain distinct.

`PeopleRegistry` retains all roles in global creation order. Successful addition appends a person and
advances only that role's counter. `PeopleRegistryState` copies and validates the list and all three counters.
It rejects unsupported record types, duplicate IDs/keys, missing/negative counters and counters below retained IDs.
Copying, exporting or restoring retains order and deleted/exhausted allocation history.

Registry deletion checks a caller-supplied relationship predicate without allowing it to mutate records.
Unknown IDs, blocked references or predicate failure leave order and counters unchanged. Active person
deletion goes through `PonHubData.removePerson`, so registry removal does not bypass whole-model guards.

### Contact validation and normalization

Commands and JSON loading construct the same value types. This prevents the two paths accepting
different contact formats. Normalization cleans accepted spelling before comparison or storage.

| Value | Accepted format |
| --- | --- |
| Name | 1–100 Unicode code points after boundary trimming, repeated ASCII-space collapse and NFC normalization. Letters, attached combining marks, ASCII spaces, apostrophes (`'`, `’`), hyphens and periods; must contain a letter. Standalone lowercase `s/o` and `d/o` can separate letter-bearing name text. Preserve display case. Reject digits, emoji, other slashes, unattached marks and internal non-ASCII whitespace. |
| Phone | 3–15 ASCII digits stored as text, including leading zeros. |
| Email | At most 254 characters. Local part allows English letters, digits, `.`, `_`, `'`, `%`, `+`, `-`, without initial/final/repeated periods. Domain has at least two labels using letters/digits/internal hyphens and ends in 2–63 English letters. Preserve spelling and case. |
| Address | 1–200 printable ASCII characters after boundary trimming and repeated ASCII-space collapse. Every slash must have an ASCII digit immediately on both sides. Reject tabs, line breaks and controls. |

Name and Address reject controls, format characters, line/paragraph separators and lone surrogates
before trimming. Thus neither commands nor JSON can hide invalid characters at the ends. Their constructors
perform normalization once; value equality keeps case significant.

Name checks code-point length and combining-mark placement after NFC normalization. Before NFC, it rejects
more than 400 code points after space normalization. On Java 25, canonical decomposition expands a scalar
to at most four code points; this guard bounds work without rejecting a spelling equivalent to a valid
100-code-point name. Constructor and static validation share the guard.

Email checks length first, then uses a linear scan with constant stack use, avoiding stack overflow on
long repeated input. Phone/email parsing still trims surrounding whitespace. Previously accepted files
that fail current rules enter protected startup rather than being silently changed.

`FieldValueTokenizer` lets the four add/edit parsers recognize complete lowercase `s/o` and `d/o` within
`n/` or `tu/`, and digit-surrounded slashes within `a/`. Other prefix-shaped text still triggers normal
unknown/repeated-prefix checks. This adds no quoting grammar and keeps edit keep/clear rules intact.
Search has its own slash restrictions: use fragments such as `n/Ravi`, `a/#01` or `n/Jos`.
Do not assume search performs the same Unicode normalization as stored names.

<a id="role-aware-addition-parsing-foundation"></a>
<a id="staged-person-addition-candidate"></a>
<a id="current-people-index-resolution-foundation"></a>
<a id="prepared-people-commands"></a>
<a id="current-people-selection"></a>

### Adding, listing and selecting people
{: id="canonical-people-commands"}

`RoleAwareAddParser` returns `PersonAdditionInput` without allocating an ID. Lowercase prefixes may
appear once each in any order. Role and level values ignore case.

| Role | Required prefixes | Optional prefixes |
| --- | --- | --- |
| Student | `r/`, `n/`, `l/`, `pp/` | `p/`, `e/`, `a/` |
| Tutor or Parent | `r/`, `n/`, `p/` | `e/`, `a/` |

Supplied optional fields must be nonblank. Reject preambles, controls, line breaks, unknown/repeated
prefixes and Student-only fields on other roles. Unicode boundary whitespace is accepted where the
grammar permits it; interior values still follow their validators.

`PersonAdditionCandidate` prepares a complete proposed snapshot using a temporary registry. It checks
duplicates and ID exhaustion, appends the person, and retains all other data and counters. An abandoned
candidate reserves no ID. It does not save or update the UI.

`CanonicalAddCommand` stages that candidate and an all-role people view. Saving must succeed before
the new person, reset view or success message appears. `CanonicalListCommand` stages only the role filter
and returns a count without writing operational data.

`PeopleListParser` accepts no arguments or one `r/student|tutor|parent` value, with case-insensitive
role names. Invalid arguments leave the view unchanged. `PersonIndexResolver` resolves a current people
position once and rejects wrong roles rather than looking in another list.

[![People positions and stable IDs](images/PeopleSelectionClassDiagram.png)](images/PeopleSelectionClassDiagram.png)

`PeopleView` supplies the list used both by cards and selectors. The resolver receives that list;
it does not own another copy. A displayed position can be reused after filtering/deletion, but a committed
stable ID cannot. See the [User Guide walkthrough](UserGuide.html#people-positions-and-stable-ids).

<a id="prepared-canonical-people-session"></a>
<a id="canonical-people-view-and-cards"></a>
<a id="role-filtered-people-listing-foundation"></a>
<a id="prepared-person-record-cards"></a>
<a id="prepared-people-view-and-cards"></a>

### Session and people cards
{: id="canonical-people-session"}

`CanonicalPeopleSession` loads once and owns a private `PonHubData` root shared by `PeopleView`, the
executor and lifetime `LessonQueries`. Callers receive immutable snapshots and read-only observable lists,
not a writable root. Use the single model/JavaFX application thread. `Logic` exposes these current views;
the old `Person` getter and compatibility defaults have been removed.

`PeopleView` keeps one people list and the selected role/search criteria. Refresh after commit or restore
retains the criteria and global relative order. Successful addition deliberately resets to all roles.

`PersonRecordCardData` separates name, command position and stable ID. Student fields appear as education,
own contacts, parent phone and address; Tutor/Parent cards omit Student-only fields. Captions sit above
wrapping values, with matching search context below. Cards have no fixed height; missing contacts show
`Not provided` while real spelling and phone zeros remain intact.

Long criteria headings scroll separately. They become keyboard-focusable only when overflowing,
with a visible hint and an orange focus border that does not change wrapping. Arrows, Page Up/Down and
Home/End reveal the text. Scope changes reset scrolling; resize updates whether a keyboard stop is needed.

Present contact details have a focusable **Copy** button with a contact/person accessible name.
Missing values and education level do not. Copy writes the exact displayed value only; it does not run
a command or change records, card selection or draft. Mouse activation preserves command focus/caret/selection;
keyboard activation retains normal button focus. Recycled cards rebuild actions for current data.
A real address literally equal to `Not provided` is still present and copyable.

<a id="representative-canonical-people-samples"></a>
<a id="prepared-canonical-people-sample-checks"></a>

### Sample people for tests

`PeopleSampleDataUtil` creates a fresh registry in order `T1, S1, P1, S2, T2, S3` through normal addition:

| Records | What they test |
| --- | --- |
| S1, S2 and P1 | Siblings share parent phone `00987654`, matching Pat Tan. Optional contacts vary. |
| T1 and T2 | Both named Mei Lim, with different phones; tests tutor disambiguation. |
| S1 and S3 | Both named Alex Tan, with different parent phones; valid separate Students. |

The fixture preserves phone zeros and email case. It is test data, not startup sample data, and has no
lessons, enrolments, attendance or file format. See tracker #101 for broader sample/acceptance work.

<a id="search-criteria-parsing"></a>

### Search parsing and matching
{: id="search-criteria-parsing-foundation"}

`SearchCriteriaParser` turns text after `search` into immutable `SearchCriteria`, not a command object.
`SearchField` defines which categories accept each field and how values match. The session routes valid
criteria to people or lesson queries. Parsing never saves data.

#### Supported criteria

Require `c/student`, `c/parent`, `c/tutor` or `c/lesson`; category value ignores case. Category alone
is valid. Prefixes are lowercase, order-independent and unique in one command.

| Prefix | Field | Student | Parent | Tutor | Lesson | Matching |
|--------|-------|---------|--------|-------|--------|----------|
| `n/` | Result person's name | Yes | Yes | Yes | — | Partial text |
| `l/` | Student education level | Yes | — | — | — | Exact level |
| `p/` | Result person's own phone | Yes | Yes | Yes | — | Exact digits |
| `pp/` | Student's parent phone | Yes | — | — | — | Exact digits |
| `e/` | Email address | Yes | Yes | Yes | — | Partial text |
| `a/` | Postal address | Yes | Yes | Yes | — | Partial text |
| `sn/` | Associated student's name | — | Yes | Yes | Yes | Partial text |
| `d/` | Lesson weekday | Yes | Yes | Yes | Yes | Exact weekday |
| `st/` | Lesson start time | Yes | Yes | Yes | Yes | Exact HHMM |
| `et/` | Lesson end time | Yes | Yes | Yes | Yes | Exact HHMM |
| `from/`, `to/` | Paired Lesson time window | — | — | — | Yes | Positive interval overlap |
| `s/` | Lesson subject | Yes | Yes | Yes | Yes | Partial text |
| `tu/` | Lesson tutor's name | Yes | Yes | — | Yes | Partial text |
| `tp/` | Lesson tutor's phone | — | — | — | Yes | Exact digits |
| `rm/` | Lesson room | Yes | Yes | Yes | Yes | Partial text |
| `sort/` | Lesson presentation order | — | — | — | Yes | `creation` or `schedule` |

Text searches trim boundaries, collapse repeated spaces and use case-insensitive substring matching
with `Locale.ROOT`. A fragment need not be a valid complete contact: `n/al` matches Sally Tan and
`s/Math` matches Add Math. Search additionally maps final Greek sigma `ς` to `σ`, so Σ/σ/ς match across
shared text fields. It does not expand ß to ss, remove accents or perform canonical Unicode normalization.
This search rule does not change stored names. Duplicate checking and full-name Tutor lookup also equate
these sigma forms. Existing ς/σ duplicate-key
distinctions remain loadable; search can return both records.

Phones match complete 3–15-digit strings with zeros retained. Levels use the supported uppercase forms.
Days accept Mon–Sun; times use valid four-digit HHMM. Start/end can be supplied separately; together,
end must be later on the same day. Lesson-only `from/` and `to/` must be paired, increasing, and match
positive overlap. Lesson sorting accepts only `creation` or `schedule`.

A missing filter adds no restriction; a supplied filter cannot match an absent optional field.
Every supplied condition must match. The same relationship must supply all related conditions:

* **Student:** one enrolled lesson must match all lesson fields. Monday Science plus Tuesday Math does
  not satisfy Monday Math.
* **Parent:** one linked child must match `sn/`, and one of that same child's lessons must match the
  lesson fields. Do not borrow a name from one sibling and a lesson from another.
* **Tutor:** one assigned lesson must match its lesson fields and any `sn/` roster condition.
* **Lesson:** `sn/` checks current roster members, not former attendance. Without it, empty lessons qualify.

Each stable ID appears once. People results keep creation order. Lessons use creation order unless
`sort/schedule` requests weekday/start/numeric-ID order. A valid no-match query is a successful empty result.

<a id="errors-and-integration-boundary"></a>
<a id="student-relationship-search-checks"></a>
<a id="student-relationship-search-checks-94"></a>
<a id="parent-relationship-search"></a>
<a id="parent-relationship-search-95"></a>
<a id="tutor-relationship-search"></a>
<a id="tutor-relationship-search-96"></a>

#### Search implementation

`PersonContactSearch` handles own fields (`n/l/p/pp/e/a`). `PersonRelationshipSearch` uses the session's
shared `LessonQueries` indexes to find matching lessons and current membership. It filters own fields
first and combines relationships without making another editable relationship store.

Parent links use an exact parent-phone bucket of Students in creation order. Apply `sn/` to the selected
child before checking that child's lesson links; another child or roster member cannot supply the match.
A child-name-only query can match children without lessons. Shared phone numbers may link multiple Parents.
Tutor results match assigned Tutor IDs, so same-name Tutors stay distinct.

`PersonSearchResult` and `PersonSearchMatch` contain immutable result/context data. `PeopleView` validates
the complete result before replacing criteria, context and people. Committed changes re-evaluate retained
criteria. Cards refresh context even if the matching IDs did not change. Listing and successful person
addition clear search context; failed commands preserve it. Lesson filters/results remain independent.
Staged mutation views have separate staged queries, so a failed save cannot leak candidate data.

Exact filters reuse typed validators; text fragments deliberately bypass complete-field constructors.
`SearchCategory.getPersonRole()` maps people categories explicitly and returns no role for lessons.
Reject invalid categories, incompatible/unknown/blank/repeated prefixes and invalid time ranges before
changing views. Unknown prefix text must not be swallowed into the previous field.

For bounded fields, Student/Tutor relationship search costs `O(P + B + R_B + K)`: people checked, lesson
candidates, candidate roster links and emitted results/context. Parent search costs
`O(P + B + C + E + K)`, including children and enrolment links across distinct phone buckets.
Own-only search costs `O(P)`; child-name-only queries skip lesson candidates. The parent-phone index
adds `O(P)` storage/refresh work. These bounds exclude index rebuilding and UI/persistence work.
Relationship tests compare seeded queries with an independent scan of raw records.

#### Unicode spaces at command boundaries

Command routing and parsers accept Unicode horizontal space separators where their grammar already
allows spaces, including U+2003, U+00A0 and U+202F. This does not allow spaces inside IDs/prefixes or
replace disallowed interior contact characters. Command-word/logging patterns use `UNICODE_CHARACTER_CLASS`;
shared boundary helpers retain controls for rejection. Search, history and showlesson reject all `\R`
line breaks before trimming. Other commands retain their existing tab/control rules.

<a id="dated-attendance-records"></a>

### Dated attendance

`Attendance` is immutable and stored separately from enrolment. Its `AttendanceKey` combines Student ID,
Lesson ID and `LocalDate`; the full model permits at most one entry per key. Only present/absent are
stored; unrecorded means no key exists.

Dates must be real dates on the lesson's weekday. New keys require current enrolment. Retained keys
can be corrected with `mark` or removed with `unmark` after unenrolment. Repeating a status or unmarking
a missing key is a no-op. Retained keys still block Student and Lesson deletion.

For new or changed future **present** entries, the marking command uses the session's injected `Clock`
to compare with the local date and requires `ack/future`. This check follows normal model validation
and happens before saving. Future absence and identical-status no-ops need no acknowledgement.
Acknowledgement does not bypass other validation. The model can load and retain valid future records.

<a id="guarded-person-deletion-and-confirmation"></a>

### Deleting a person safely

`delete INDEX` resolves the current position once and tests deletion on a disposable snapshot. It stores
only the immutable target for a prompt showing name and stable ID; it does not write or change views.
`delete confirm` rechecks that target and its dependencies through `CanonicalDeleteCommand` before saving.

Every new command consumes the previous pending request before parsing. Cancel, invalid input, another
command, save failure or restart cannot leave an old confirmation armed. Retry requires a fresh preview.
Confirmation is not persisted. Removing a Parent leaves Student parent-phone values intact. Successful
deletion preserves counters, surviving order, unrelated records and active people criteria.

<a id="future-undoredo-and-archiving"></a>

### Future undo, redo and archiving

No undo, redo, archive or restore command is registered. Discarding a failed transaction is implemented,
but it is different from a user undo command. Future designs must preserve relationships, attendance
history and non-reuse of committed IDs. Old `VersionedAddressBook` examples do not describe a delivered feature.

<a id="documentation-logging-testing-dev-ops"></a>

## Documentation, logging, testing and DevOps

* [Documentation guide](Documentation.html)
* [Testing guide](Testing.html)
* [Logging guide](Logging.html)
* [DevOps guide](DevOps.html)

## Appendix: Requirements

### Product scope

PonHub is for tuition centre administrative staff who manage contacts, timetables and daily attendance.
They know the centre's work, but need quick ways to find records, contact parents, avoid double bookings
and follow up on absences. Clear errors and command examples help them learn the app.

The app connects people, lessons and attendance so staff do not have to keep separate lists consistent
by hand. Person and lesson editing lets them correct details while keeping IDs and linked records.
Lessons can exist before students join. A roster shows current membership; dated attendance records
what was marked for individual occurrences. Unenrolment keeps that history.

See [How the records fit together](#shared-lesson-target-contract) and
[Editing records](#v14-editing-contract) for the rules developers must preserve.

### User stories

The table records user goals, including proposed features. High priority covers the core product;
Medium and Low describe useful extensions and longer-term ideas. Priority alone does not mean a feature
is implemented.

High-priority workflows are available through the [current commands](UserGuide.html#current-command-summary).
Most Medium/Low goals remain proposals. US-33 is partly delivered: Tab completes command names,
but argument/value completion and spelling suggestions are future work. Likewise, delivering one part
of a combined story, such as level filtering in US-22, does not deliver the whole story.

| ID | Priority | As a … | I want to … | So that I can … |
| --- | --- | --- | --- | --- |
| US-01 | High | tuition centre administrator | add students with their level and parent contact number | keep student records and contact a parent when needed. |
| US-02 | High | tuition centre administrator | add tutors with their contact details | maintain the information needed for lesson allocation and communication. |
| US-03 | High | tuition centre administrator | keep optional separate parent contact records | find parent details independently of a student's record. |
| US-04 | High | tuition centre administrator | list all people or only students, parents or tutors | review the relevant records quickly. |
| US-05 | High | tuition centre administrator | search student, parent and tutor records by identifying details and find parents or tutors associated with a student or lesson | retrieve the right contact information promptly. |
| US-06 | High | tuition centre administrator | filter relevant student and lesson records by tutor, subject or lesson day | find the records needed for a specific teaching session. |
| US-07 | High | tuition centre administrator | remove obsolete person records while preventing removal of records still referenced by lessons or attendance | keep the records tidy without breaking existing links. |
| US-08 | High | tuition centre administrator | create a shared recurring lesson independently of student enrolment, with its day, time, subject, tutor and room | prepare teaching slots before students join them. |
| US-09 | High | tuition centre administrator | have shared lessons that overlap existing tutor or room bookings rejected while allowing adjacent time slots | avoid double-booking teaching resources. |
| US-10 | High | tuition centre administrator | remove an unneeded shared lesson only when its roster is empty and it has no linked attendance records | keep the schedule current without breaking membership or history links. |
| US-11 | High | tuition centre administrator | mark an enrolled student present or absent for a particular shared lesson and date | keep each student's attendance independent for the same lesson occurrence. |
| US-12 | High | tuition centre administrator | correct or remove an existing dated attendance entry even after the student leaves the lesson | fix recording mistakes while preserving the lesson and remaining history. |
| US-13 | High | tuition centre administrator | view available commands with their syntax and examples | learn how to complete tasks and recover from command mistakes. |
| US-14 | High | tuition centre administrator | have successful changes saved and available when I reopen PonHub | continue my work without re-entering records. |
| US-15 | High | tuition centre administrator | have attempts to add an exact duplicate person record rejected | avoid storing the same record twice. |
| US-16 | High | tuition centre administrator | retrieve a tutor's weekly timetable from the canonical shared lessons | tell the tutor which lessons they are assigned to teach without counting a shared lesson more than once. |
| US-17 | High | tuition centre administrator | view the current roster of a shared lesson | see which students are currently enrolled, separately from their dated attendance. |
| US-18 | High | tuition centre administrator | enrol a student in a shared lesson or remove their enrolment while preventing duplicate membership and student timetable clashes | keep current membership valid without erasing attendance history. |
| US-19 | Medium | tuition centre administrator | reserve a one-off place in a suitable class for a student who missed a lesson | arrange a make-up lesson without changing the student's regular schedule. |
| US-20 | High | tuition centre administrator | enrol several students in the same existing shared lesson | manage one teaching slot and its roster without duplicating the lesson or its tutor and room bookings. |
| US-21 | Medium | tuition centre administrator | identify students who missed a lesson | follow up with parents and decide whether make-up arrangements or fee adjustments are needed. |
| US-22 | Medium | tuition centre administrator | sort students by name and filter or group them by level | review the relevant records in a clear order. |
| US-23 | Medium | tuition centre administrator | archive withdrawn students, hide them from the default active list and restore them when needed | keep the active list manageable while retaining past records. |
| US-24 | Medium | tuition centre administrator | maintain a waiting list for a full class | offer newly available places in the order students joined the list. |
| US-25 | Medium | tuition centre administrator | track pending class-swap requests | follow up on outstanding transfers. |
| US-26 | Medium | tuition centre administrator | record lesson feedback from students or parents | follow up on reported concerns. |
| US-27 | Medium | tuition centre administrator | assign a replacement tutor for one lesson occurrence | cover a tutor's absence while retaining the regular tutor for other lessons. |
| US-28 | Medium | tuition centre administrator | cancel one lesson occurrence without deleting its recurring schedule | handle a holiday or closure without rebuilding future lessons. |
| US-29 | Medium | tuition centre administrator | export a class list with student names, class details and contact information | share or print the information needed by tutors. |
| US-30 | Medium | tuition centre administrator | review possible duplicate student matches before saving a new record | avoid duplicate records that are similar but not identical. |
| US-31 | Medium | tuition centre administrator | undo my last change | recover from an accidental edit or deletion. |
| US-32 | Medium | tuition centre administrator | use additional command shortcuts or aliases | complete frequent tasks with less typing. |
| US-33 | Medium | tuition centre administrator | receive command completion, inline input guidance and suggestions for misspelled commands | enter valid commands more easily. |
| US-34 | Medium | tuition centre administrator | record names, contact numbers and education levels beyond the MVP formats | represent a wider range of people accurately. |
| US-35 | Medium | tuition centre administrator | preview the person or lesson affected by a deletion | check its consequences before removing it. |
| US-36 | Medium | tuition centre administrator | reuse recurring lesson templates | assign similar lessons without re-entering the same details. |
| US-37 | High | tuition centre administrator | edit an existing lesson directly | update its details without deleting and recreating it. |
| US-38 | Medium | tuition centre administrator | combine alternative search conditions and exclude unwanted matches | express more complex searches. |
| US-39 | Medium | tuition centre administrator | receive spelling suggestions, relevance ordering and highlighted search matches | identify the intended records more easily. |
| US-40 | Medium | tuition centre administrator | save and reuse searches | repeat common lookups without entering every filter again. |
| US-41 | Medium | tuition centre administrator | use a guided advanced-search form and filter completion | construct valid searches without recalling every prefix. |
| US-42 | Medium | tuition centre administrator | browse a long command-help catalogue in manageable pages | read all available help within the application. |
| US-43 | Medium | tuition centre administrator | record late or excused attendance and notes explaining absences | distinguish different attendance circumstances. |
| US-44 | Medium | tuition centre administrator | mark attendance for an entire class in one operation | process attendance efficiently. |
| US-45 | Medium | tuition centre administrator | mark attendance using clickable controls | record attendance without remembering command syntax. |
| US-46 | Medium | tuition centre administrator | view attendance percentage summaries | identify patterns that need follow-up. |
| US-47 | Medium | tuition centre administrator | see today's scheduled lessons automatically | start daily attendance work quickly. |
| US-48 | Medium | tuition centre administrator | start a make-up booking directly from a recorded absence | connect follow-up arrangements to the missed lesson. |
| US-49 | Low | tuition centre administrator | track tuition fees and adjustments associated with student attendance | follow up on payments and fee changes when needed. |
| US-50 | Medium | tuition centre administrator | set a class capacity and check its remaining places before enrolment or make-up booking | decide whether another student can be accommodated. |
| US-51 | High | tuition centre administrator | retrieve a student's dated attendance history, optionally for one lesson, after unenrolment as well as during membership | follow up on past attendance without relying on the current roster. |
| US-52 | High | tuition centre administrator | distinguish an unrecorded lesson occurrence from a present or absent attendance entry | identify attendance that still needs to be recorded. |
| US-53 | High | tuition centre administrator | retrieve the canonical lesson catalogue, including shared lessons with empty rosters, and a student's currently enrolled lessons | discover available teaching slots and review each student's current schedule. |
| US-54 | High | tuition centre administrator | use stable person and lesson identities unchanged by filtering and restart, with deleted IDs never reused | keep record references reliable as displayed lists change. |
| US-55 | High | tuition centre administrator | edit an existing student, tutor or parent while retaining their role, stable identity and linked records | correct their details without deleting and recreating the person. |

### Use cases

In these cases, the actor is the tuition centre administrator and the system is PonHub.
The main flows describe implemented behaviour. Proposed additions are listed separately.
Commands that change data assume the session permits changes.

For all changes: validate first, save before updating live data, and report failure if saving fails.
The live records and views stay unchanged on failure. Disk recovery follows
[Safe JSON saves](#safe-json-saves); it is not safe to assume the destination file always stays unchanged.

#### UC-01: Add a person record

Related stories: US-01, US-02, US-03, US-14, US-15, US-30, US-34.

The administrator knows the person's role and required details.

1. The administrator submits an add request with those details.
2. PonHub saves one new person, reports success and shows the updated people list.

Missing, invalid, repeated or unsupported fields produce feedback so the administrator can correct
the request. A duplicate is rejected. Save failure adds no person to the live session.
Existing records remain unchanged.

See [Adding a person](UserGuide.html#1-adding-a-person-add).
Possible-duplicate previews and broader contact formats are future goals (US-30, US-34).

#### UC-02: Find and review operational records

Related stories: US-04, US-05, US-06, US-16, US-17, US-51, US-52.

PonHub is running and the requested retrieval command is available.

1. The administrator requests people or lessons, optionally with filters or a Student selector.
2. PonHub displays matches and a count. Lesson results may include empty lessons.
3. If needed, the administrator requests details of a lesson, optionally for a date.
4. PonHub shows the current roster, distinguishes unrecorded attendance from recorded statuses,
   and lists retained former-member attendance separately.

The administrator can stop after reviewing any result. Zero matches is a valid empty result.
Invalid filters, unavailable records, wrong roles and invalid/wrong-weekday dates give feedback and
keep previous results. Retrieval does not change stored data; lesson retrieval keeps the people list
and selection. Attendance-history retrieval is described in UC-05.

See [Searching](UserGuide.html#6-searching-people-and-shared-lessons) and
[Lesson details](UserGuide.html#lesson-details). Guided search, saved searches, export, name sorting/grouping
and spelling suggestions remain proposals (US-22, US-29, US-38–US-41).

#### UC-03: Create or delete an independent shared lesson

Related stories: US-08, US-09, US-10, US-20.

The intended tutor must exist. Students are not required to create a lesson.

1. The administrator supplies the lesson's schedule, subject, tutor and room.
2. PonHub saves a lesson with an empty roster, reports its ID and makes it available for enrolment.

Missing/invalid details, no matching tutor and tutor/room clashes are rejected. Ambiguous tutor names
require a phone in the revised request. Adjacent slots are allowed.

Alternatively, the administrator requests deletion of an existing lesson. PonHub saves deletion only
if its roster is empty and no attendance refers to it. Invalid, missing or referenced lessons are not
removed. Deletion releases that slot's tutor and room; linked records are never removed automatically.
Save failure keeps the live schedule unchanged. Other lessons and IDs remain unchanged on success.

See [Lesson operations](UserGuide.html#4-creating-shared-lessons-and-other-lesson-operations).
Direct lesson editing is also implemented under US-37. Templates, one-occurrence tutor replacement,
cancellation and automatic today's-lessons views are future work (US-27, US-28, US-36, US-47).

#### UC-04: Enrol or unenrol a student in a shared lesson

Related stories: US-17, US-18.

The Student and Lesson exist, and the administrator has checked the current people position.

1. The administrator requests enrolment in that lesson.
2. PonHub saves membership and updates the student's current lessons and the lesson roster.

Invalid requests, wrong-role people, missing lessons, duplicate enrolment and Student clashes are rejected.
Alternatively, unenrolment removes existing membership but keeps the person, lesson and attendance.
Trying to unenrol a nonmember is rejected. Save failure keeps live membership unchanged.

Active people-search criteria stay in place, but relationship matches may change after membership changes.
Keep surviving matches in relative order and recheck positions before another person command.
See [Enrolment](UserGuide.html#5-enrolling-and-unenrolling-students).
Capacity, waiting lists, swaps and one-off make-up bookings remain proposals (US-19, US-24, US-25, US-48, US-50).

#### UC-05: Record, correct or retrieve dated attendance

Related stories: US-11, US-12, US-14, US-21; history/unrecorded goals also appear in US-51 and US-52.

The Student exists and has been identified in the current people list. The selected Lesson must exist;
a new attendance key also requires current enrolment.

1. The administrator requests present or absent for one Student, Lesson and date.
2. PonHub saves that entry and confirms the result without changing other Students or dates.

A different status corrects an existing key, even after unenrolment. An identical status reports no change
without saving. Invalid requests, wrong roles, missing records, invalid statuses/dates, wrong weekdays
and new keys for nonmembers are rejected.

A new or changed future present entry needs explicit `ack/future`; otherwise warn and change nothing.
Future absence needs no acknowledgement, and acknowledgement bypasses no other checks.

Alternatively, `unmark` removes one dated entry without changing membership or other history. This works
after unenrolment; a missing key reports no change. `history` displays retained entries, optionally filtered
by lesson, exact date and status. No entries is an empty result, not evidence of absence. Invalid history
requests keep the previous view. All retrieval is read-only.

Save failure keeps live attendance/views unchanged. Retained entries continue to block Student/Lesson
deletion. See [Attendance and history](UserGuide.html#7-recording-attendance-and-retrieving-history).
Late/excused statuses, notes, feedback, bulk/clickable marking, percentages, make-up links and fees are
future goals (US-26, US-43–US-46, US-48, US-49).

#### UC-06: Delete an unreferenced person

Related story: US-07. The administrator has checked the current people position.

1. The administrator requests deletion.
2. PonHub shows the resolved person's name and stable ID and asks for confirmation.
3. The administrator confirms.
4. PonHub rechecks eligibility, saves deletion and updates the people list.

Invalid requests and referenced Students/Tutors are rejected with the blocking dependency identified.
Cancel, another command, invalid input, restart or a new deletion request discards the previous pending
target. If eligibility changes or saving fails, no person is removed live. Retry needs a fresh request
and confirmation.

Successful deletion preserves surviving IDs, relationships and counters. Removing a Parent does not
erase Student parent phones. Recheck displayed positions afterwards.
See [Deleting a person](UserGuide.html#3-deleting-a-person-delete).
Archiving/restoration and undo remain proposals (US-23, US-31).

#### UC-07: Get help and recover from a command-entry error

Related stories: US-13, US-32, US-33, US-42. PonHub is running with command help available.

1. The administrator asks for available commands or one command's help.
2. PonHub shows descriptions, syntax and examples as appropriate.
3. The administrator prepares and submits a command using the guidance.
4. PonHub executes it and shows the result.

The administrator may stop after reading help or scroll to read long guidance. Unknown/unavailable topics
and multiple topics give corrective feedback without executing a topic. Tab completes or cycles command
names without submitting; no match keeps the draft unchanged.

Unknown commands and invalid/missing arguments keep the failed draft available for correction.
Domain/save failures follow the relevant case and recovery rules. Help leaves records and selection alone.
Menu/F1 help also preserves unfinished input. See [Help](UserGuide.html#viewing-command-help-help) and
[Tab completion](UserGuide.html#completing-command-names-with-tab).
Extra aliases, argument/value completion, spelling suggestions and paged help remain future work.

### Non-functional requirements

These are product requirements and team targets. They are not a claim that every test or environment
has already passed.

1. Run on Windows, Linux and macOS using Java 25. On macOS, use the prescribed Azul JavaFX runtime
   `25.0.3.fx-zulu` described in [Setting up](SettingUp.html#setting-up-the-project-on-your-computer).
2. Hold at least 1,000 people, 1,000 recurring lessons and 10,000 attendance records in one local file.
3. At that workload, target common command-to-GUI updates within 2 seconds on the reference machine:
   Apple M4 Pro MacBook Pro, 24 GB RAM, macOS 15.7.7, prescribed Azul Java/JavaFX 25.0.3.
   Report the measured workload and environment. These measurements do not establish performance for
   packaged JARs, other platforms, cold starts or physical displays. See
   [performance evidence](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/101#issuecomment-6194333977).
4. Let a user with above-average English typing speed finish most recurring tasks faster with commands
   than with the mouse; programming/system-administration typing skill is not assumed.
5. Serve one user with one local store. Do not require accounts, concurrent access or live synchronization.
6. Distribute a single executable JAR without an installer.
7. Keep the product file below 100 MB.
8. Display correctly at 1920×1080 and higher, at 100% and 125% scaling.
9. Keep every function accessible at 1280×720 and higher, at 150% scaling, even if layout is less ideal.
10. Store all operational data locally as human-readable text, without a database management system.
11. Save successful operational changes before applying them live. Validation/save failure must leave
    live data unchanged. If fallback replacement fails, the previous complete file must be restored or
    available in the backup named by the error. The limits for first saves, concurrent changes, multiple
    instances and power loss are described in [Safe JSON saves](#safe-json-saves).
12. Keep all people, lesson and attendance data on the user's computer; never transmit it over the internet.
13. Work offline without a team-owned server.
14. Reject invalid input without crashing or changing records, and explain how to correct it.
15. Use mainly object-oriented design and separate UI, Logic, Model and Storage responsibilities.
16. Target at least 70% JUnit line coverage and passing automated tests before release. This is the team's
    target, not a course minimum; coverage does not replace behaviour-focused regression/integration tests.

Large-people benchmarks use 5,000 people across all roles as the primary workload, and 20,000/50,000 as
stress workloads. These are test inputs, not enforced capacities or per-lesson limits. They do not extend
the 2-second target or prove lesson/attendance capacity. State the workload, operation, time and hardware
with each claim; there is no larger-workload latency guarantee.

### Glossary

See [Terms used in this guide](#terms-used-in-this-guide) for software terms.

| Term | Meaning |
| --- | --- |
| AB3 | AddressBook-Level3, the SE-EDU project from which PonHub developed. |
| Mainstream OS | Windows, Linux, Unix or macOS. |
| Person record | One stored Student, Tutor or Parent with a stable ID, contact details and role-specific fields. |
| Role | Student, Tutor or Parent; determines required fields and available operations. |
| Contact details | A required name and any optional own phone, email and address. Role rules can require a phone. |
| Lesson catalogue | Shared lessons in stored creation order. |
| Shared recurring lesson / class | One weekly slot with a stable ID, tutor, day, times, subject, room and a roster of zero or more Students. |
| Lesson occurrence | One dated instance of a recurring lesson. |
| Enrolment / roster | Current membership / the Student IDs owned by a lesson. |
| Attendance record / history | A present/absent entry for one Student, Lesson and date / retained entries independent of current enrolment. |
| Attendance key | Student ID + Lesson ID + date; identifies one entry. |
| Unrecorded | No attendance entry exists for that key. |
| Exact duplicate person | Same role, normalized name and identifying phone as another person. |
| Displayed person index | Positive position in the current people list. Can change; never taken from lesson/history rows. |
| Stable person/lesson ID | Stored identity used in relationships and lookups; unchanged by filtering or editing. |
| Tutor lookup | Full normalized name plus optional exact phone for lesson creation/editing; search uses fragments instead. |
| Prefix | A marker such as `n/` identifying the next command value. |
| Parser | Code that turns command text into validated inputs. |
| Command | One executable user request. |
| Referential integrity | Every stored relationship must point to an appropriate existing record. |
| Atomic live update | Apply the complete saved candidate together; disk recovery has the limits described under storage. |
| Rollback | Discard a failed candidate or restore the prior valid state. |
| Human-readable data file | A local text file that can be inspected without a database system. |
| Tutor/room clash | Same-day overlapping lessons using the same tutor/room. |
| Capacity (proposal) | A future maximum class size; not a current model field. |
| Make-up booking (proposal) | A one-off place without changing regular enrolment. |
| Waiting list (proposal) | An ordered queue for places in a full class. |
| Class-swap request (proposal) | A pending move that cannot yet be completed. |
| Archived student (proposal) | A withdrawn Student hidden from active lists while retaining past records. |

## Appendix: Instructions for manual testing

Use fictitious records in a disposable application folder. These instructions are a checklist,
not a report that the tests have passed. Keep originals/backups outside selected test-data paths.
Record PASS, FAIL or NOT RUN and the actual result; explain unavailable checks. Add exploratory testing
beyond these examples.

Use Java 25. On macOS, use the prescribed Azul JavaFX runtime for the machine's architecture and confirm
that the launch terminal selects it. Other runtimes are extra portability trials, not replacements.

### Record the build and test environment
{: id="common-selector-and-failure-journey"}

Record the source commit/tag, JAR filename, size, SHA-256 and manifest; OS version/architecture; Java
distribution/version; and selected data path. For display trials, also record physical resolution,
scaling, desktop work area, logical window size and output scale. Attach screenshots or command transcripts.

For every change, check the intended record and unrelated records. For rejection, check the previous
live records, counters, filter, selection and right-pane context. For a successful save, close and
restart with the same path to check the saved state and next IDs. Session filters/selection are not
required to survive restart.

When checking save failure, use the [User Guide procedure](UserGuide.html#checking-unsuccessful-saves-in-the-current-build)
in the disposable folder. Try an otherwise valid change while a people filter and selection are active.
Expect failure feedback, no success and no visible candidate change. Restore access and retry once;
only that retry should apply the change and consume any new ID.

Do not assume every failed save leaves the destination bytes intact. Preparation/backup failures preserve
the old destination; replacement/recovery failures may require restoring the named backup before continuing
or restarting. Follow [Safe JSON saves](#safe-json-saves). An injected failing writer tests application
handling, not the behaviour of a real filesystem.

<a id="compact-windows-and-active-contact-cards"></a>

### Display and window checks
{: id="common-window-and-layout-checks"}

Use one row for each actual physical-display trial. Automated FXML bounds tests and logical-size
simulations supplement these checks; they do not prove another platform or scale works.

<!-- @@author ErnestChuaa -->
| OS | Physical display | Scale | Result / evidence |
| --- | --- | --- | --- |
| Windows | 1280×720 | 150% | NOT RUN |
| Windows | 1920×1080 | 100% | NOT RUN |
| Windows | 1920×1080 | 125% | NOT RUN |
| Linux | 1280×720 | 150% | NOT RUN |
| Linux | 1920×1080 | 100% | NOT RUN |
| Linux | 1920×1080 | 125% | NOT RUN |
| macOS | 1280×720 | 150% | NOT RUN |
| macOS | 1920×1080 | 100% | NOT RUN |
| macOS | 1920×1080 | 125% | NOT RUN |
<!-- @@author -->

Record extra configurations separately rather than relabelling them as a matrix row.

1. Launch, shrink to minimum size, widen and narrow again. Move both dividers. Keep menus, viewing
   controls, feedback, command entry and both lists reachable.
2. Use all roles, missing optional contacts, leading-zero phones and long valid values. Scroll both
   lists independently to the final record and to the bottom of a tall card. Values must wrap without
   horizontal overflow. Run `help add` and reach the final example.
3. Check empty/large rosters, dated details, history and former-member sections. All entries must be
   reachable; current membership, recorded status and unrecorded attendance must stay distinct.
4. Refresh/filter people and check positions, counts, empty placeholders and recycled cards. Preserve
   selection by stable ID if still visible; clear it if hidden. A deliberate filter changes results.
5. Keep an unfinished draft while using viewing buttons and menu/F1 help. Check preserved draft/caret/
   selection. Valid viewing chooses the requested result; rejected viewing keeps the previous one.
6. Hover all context/summary labels after long queries and result changes. Tooltips must show the full
   current text with wrapping, without expanding headings or losing draft/list space.
7. Restart with saved preferences. Check a valid position, an oversized window, a removed monitor and
   a connected monitor left/above the primary one. Fit unusable windows to a work area and keep valid
   secondary-screen positions.

For a long-contact check, use:

```text
add r/tutor n/Alexandra Catherine Elizabeth Montgomery Wellington p/001234567890123 e/alexandracatherineelizabethmontgomerywellington@example.com a/123 Long Residential Avenue Clementi District Block 456 Unit 12-345 Singapore 123456
```

For a long people-search heading, use enough valid long filters to overflow it, including a zero-match
query. Tab/Shift+Tab must reach it only when overflowing; its hint, scrollbar and focus outline remain
visible. Arrows, Page Up/Down and Home/End reveal all text. Scrolling changes no draft, filter, selection
or saved data. A new scope resets it to the top; once it fits, it adds no keyboard stop. Normal TextField
focus traversal may reset text selection, so assess that separately from scrolling itself.

### Shared-lesson and attendance workflow
{: id="active-shared-lesson-and-attendance-workflow"}

Start with a previously unused file in a disposable JAR folder, for example:

```sh
java -jar ponhub.jar --data="manual/lesson-workflow.json"
```

Use a fresh store: the expected IDs below depend on it.

#### 1. Add people

```text
add r/tutor n/Mei Lim p/92345678
add r/tutor n/Ravi Kumar p/93456789
add r/student n/Alex Tan l/P1 pp/91234567
add r/student n/Jamie Tan l/P2 pp/91234567
```

Expect T1, T2, S1 and S2 in that creation order. In `list`, Alex/Jamie are positions 3/4;
in `list r/student`, they are 1/2.

#### 2. Create lessons and check clashes

```text
addlesson d/Mon st/1600 et/1730 s/Math tu/Mei Lim tp/92345678 rm/R1
addlesson d/Mon st/1700 et/1800 s/English tu/Mei Lim tp/92345678 rm/R2
addlesson d/Mon st/1700 et/1800 s/English tu/Ravi Kumar tp/93456789 rm/R1
addlesson d/Mon st/1630 et/1700 s/Science tu/Ravi Kumar tp/93456789 rm/R2
addlesson d/Mon st/1730 et/1830 s/English tu/Ravi Kumar tp/93456789 rm/R2
```

The first command creates L1. The next two fail for tutor and room clashes without consuming IDs.
The final two create L2 and L3: L2 uses another tutor/room; L3 starts exactly when L1 ends.

#### 3. Enrol and retrieve

```text
list r/student
enrol 1 lid/L1
enrol 2 lid/L1
enrol 1 lid/L2
enrol 1 lid/L3
lessons
lessons 1
search c/lesson sn/Alex d/Mon s/Math
search c/lesson sn/Nobody
```

Both Students share L1. Alex's L2 enrolment fails because it overlaps L1; adjacent L3 succeeds.
The first search finds L1 and the second finds nothing. Lesson retrieval leaves people positions alone.

#### 4. Mark and correct dated attendance

```text
mark 1 lid/L1 d/2026-10-05 s/present
mark 2 lid/L1 d/2026-10-05 s/absent
mark 1 lid/L1 d/2026-10-05 s/present
mark 1 lid/L1 d/2026-10-05 s/absent
mark 1 lid/L1 d/2026-10-06 s/present
showlesson lid/L1 d/2026-10-05
showlesson lid/L1 d/2026-10-12
history 1 lid/L1
```

Each Student gets a separate key. Repeating Alex's present status is a no-op; absent corrects that key.
Tuesday 6 October fails. The first dated detail shows recorded statuses; the next Monday shows both
Students as unrecorded. Alex's history contains the recorded 5 October absence only.

#### 5. Unenrol without losing history

```text
list r/student
unenrol 1 lid/L1
unenrol 1 lid/L3
mark 1 lid/L1 d/2026-10-05 s/present
mark 1 lid/L1 d/2026-10-12 s/present
showlesson lid/L1 d/2026-10-05
lessons 1
delete 1
exit
```

Alex's existing key can still be corrected to present, but the new 12 October key fails after unenrolment.
Details show Alex as a former member; Alex's current lessons are empty. Retained attendance blocks deletion.
Inspect the closed file, then restart with the same data path.

#### 6. Remove dependencies and check IDs after restart

```text
list r/student
showlesson lid/L1 d/2026-10-05
lessons 1
delete 1
unmark 1 lid/L1 d/2026-10-05
unmark 1 lid/L1 d/2026-10-05
deletelesson lid/L1
unenrol 2 lid/L1
deletelesson lid/L1
unmark 2 lid/L1 d/2026-10-05
deletelesson lid/L1
addlesson d/Mon st/1600 et/1730 s/Math tu/Mei Lim tp/92345678 rm/R1
exit
```

Before unmarking, Alex's history still blocks deletion after restart. Repeated unmark is a no-op.
Jamie first blocks L1 deletion through enrolment, then through attendance. Only after both are removed
does deletion succeed. Recreating the slot gives **L4**, never L1.

Restart, run `list r/student`, and check L2, L3, L4 in catalogue order. Math search finds L4;
Alex's current lessons remain empty.

#### 7. Verify read-only commands do not save

After the successful changes, close and hash the operational file. Restart with the same path and run
`list r/student`, `lessons`, `lessons 1`, `search c/lesson s/Math`,
`showlesson lid/L4 d/2026-10-05`, `help lessons` and `help showlesson`, then exit.
The operational hash must stay the same. Preferences may change separately.

```powershell
Get-FileHash "manual/lesson-workflow.json" -Algorithm SHA256
```

On macOS/Linux use `shasum -a 256 "manual/lesson-workflow.json"`.

#### 8. Verify injected save failures

From the repository root, run the delivered failure tests:

```bash
./gradlew test \
    --tests "ponhub.logic.CanonicalLessonMembershipIntegrationTest.execute_enrolSaveFailureRestoresAllStateAndRetryCommitsOnce" \
    --tests "ponhub.logic.CanonicalAttendanceIntegrationTest.execute_markSaveFailureRestoresStateViewsAndRetryCommits" \
    --tests "ponhub.logic.CanonicalAttendanceIntegrationTest.execute_unmarkSaveFailureRestoresStateViewsAndRetryCommits" \
    --tests "ponhub.logic.CanonicalDeleteLessonIntegrationTest.execute_failedSavePreservesStateCountersAndViewsBeforeSuccessfulRetry" \
    --tests "ponhub.logic.CanonicalLessonRetrievalIntegrationTest.addLesson_saveFailureThenCommitAndReload_refreshOnlyMatchingSelectedResults"
```

Use `gradlew.bat` on Windows, with your shell's line-continuation syntax or a single line.
Expect all selected tests to pass: candidates/counters/views stay hidden on failed writes, and a retry
applies one change that reloads correctly. This is not a packaged-UI command for deliberately failing a save.

<a id="planned-person-index-integration-checks"></a>
<a id="canonical-people-commands-and-unsuccessful-saves"></a>

### People positions, addition and listing
{: id="person-index-integration-checks"}

1. In the workflow fixture, `list` shows Alex/Jamie at positions 3/4 with IDs S1/S2.
   `lessons` and `history 4 lid/L1` must not renumber people.
2. Run `list r/student`, then `search c/student n/Jamie`. Jamie S2 is now position 1.
   `mark 1 lid/L1 d/2026-10-05 s/present` and `history 1 lid/L1` must act on S2, not S1.
   `lessons`, details and history keep the Jamie search. Enrol/unenrol use the same current-position rule.
3. Reject `history 0`, out-of-range `history 2`, `history S2`, `lessons sid/S2`, unknown lessons
   and wrong-weekday attendance. List Tutors and reject Student-only commands on a Tutor position.
   Keep previous data/results after each failure. Restore the intended Student filter before continuing.
4. Check `list`, each role and `list r/TUTOR`: preserve global relative order and correct counts;
   an empty role succeeds. Reject `list r/`, `list r/all`, repeated roles and extra arguments without changes.
5. Add all roles with required/optional contacts. Check sibling Students with a shared parent phone,
   leading zeros, readable cards and global order. Success resets people to all roles and appends the new person.
6. Reject duplicates, missing/blank required fields, repeated/unknown prefixes and Student-only Tutor/Parent
   fields. Use Student parent phone for duplicate keys and Tutor/Parent own phone for theirs.
7. Repeat addition with a failed save while a search/selection is active. Preserve records, next ID and
   view. Retry successfully: use that same next ID, reset the view once and confirm the saved state on restart.

`CanonicalPeopleCommandIntegrationTest`, `CanonicalPeopleSessionTest` and boundary tests check the
parser/session/writer path, including allocation/exhaustion. Component tests do not certify native layout.

<!-- @@author ErnestChuaa -->
### Person editing

Use a fresh copy of the [User Guide quick start](UserGuide.html#quick-start) through the first
`history 1 lid/L1`, before `unmark`. Keep Alex's former-member history and add
`add r/parent n/Pat Tan p/91234567`.

1. Run `list r/student`, select Alex, then `edit 1 l/P2 p/ e/ a/`. Check level P2, cleared optional
   contacts, the same selected S1 and Student filter. `edit 1 n/Alex  Tan` is a no-op with no write;
   `edit 1 n/ALEX TAN` changes display case.
2. Reject missing fields, stable IDs used as positions, zero/out-of-range positions, blank `n/`/`pp/`,
   invalid phones, role edits, repeated prefixes and unknown prefix-shaped values. Check unchanged file/view.
3. Reject `edit 1 n/Jamie Tan pp/91234567` as a duplicate. On Tutors/Parents, reject clearing required
   `p/` and adding `l/`/`pp/`.
4. With Tutor listing active, show a dated L1 detail and rename its tutor. Keep Lesson/Tutor IDs, date,
   roster and history; refresh the tutor label.
5. Search Parents by Alex, then change Pat's own phone. The Parent leaves the retained search; Student
   parent phones remain unchanged, and success lists removed Parent/Student pairs. Update Alex's parent
   phone to match again, then change it away and check the warning. Restore phones. Name-only edits,
   unchanged phones and previously unlinked phone changes must not warn or create extra Parents.
6. Show Alex's history, search Students by Alex, select Alex and rename to Changed Student. Clear the
   now-hidden people selection while keeping S1's history in the right pane.
7. Repeat a valid edit with a failed save, retry and restart. Check old cards/selection/detail/history
   before retry, and stable identities/relationships after reload. Protected startup blocks `edit`
   and `help edit` without touching rejected data.

<!-- @@author -->

<!-- @@author ys000009 -->
### Lesson editing

Use the shared workflow fixture before removing attendance. Record Monday attendance for both Students.

1. `editlesson lid/L1 st/1630 s/Advanced Math` changes only supplied fields. Keep ID, roster, Tutor,
   room, end time, counters and attendance. Expect the retained-attendance warning and current-information
   note in dated details; verify both after restart. An edit without attendance has no such warning.
2. Test start-only and end-only changes. Reject equal/reversed times. Identical normalized edits report
   no change, with no warning or write.
3. Add same-name Tutors with different phones. A name-only replacement is ambiguous; matching `tu/` + `tp/`
   succeeds. Orphan `tp/` and phone mismatches fail.
4. Test tutor, room and enrolled-Student clashes against other lessons. Keep old cards/file/counters on
   rejection. The lesson's own unchanged slot and adjacent boundaries must not count as clashes.
5. Unenrol Jamie but keep Jamie's Monday record. Reject a Tuesday edit while either Student's Monday
   attendance remains; identify an incompatible date and explicit unmark option. Remove those entries
   only for this test, select a Monday detail, and retry. Keep the lesson selected but clear the date/context.
6. Check failed save, retry and reload with old detail/roster/history preserved until success. Protected
   startup blocks both the command and its help topic while preserving rejected bytes.

Run `./gradlew test --tests ponhub.logic.CanonicalLessonEditIntegrationTest --no-daemon` for focused
automated coverage. Record the manual outcomes separately, including empty/populated, clash, history,
no-op, invalid-selector, weekday and save-failure cases.

<!-- @@author -->

### Searching people and lessons
{: id="active-own-contact-search-checks"}

Use copies of the worked search dataset and additional unassigned people/empty lessons.

| Check | Expected result |
| --- | --- |
| Category-only people search | All records of that role in creation order, once per stable ID. |
| Combined own fields | Every field matches; phones are exact with zeros retained; absent optional values do not match supplied filters. |
| Change search, then `list r/student`, then `list` | Each listing clears search; headings show the new scope. |
| Help/menu/F1 with a search active | Keep people criteria, context, draft as promised and saved bytes. |
| Student has Tuesday Math and Monday Science | Monday Math search must not match. |
| Parent has two children | Do not combine one child's name with the other's lesson; child-name-only search can match an unenrolled child. |
| Tutor has multiple assignments | All lesson/name conditions must match one assigned lesson; empty lessons qualify when no roster name is required. |
| Multiple matching lessons/children/students | One people/lesson result per stable ID, with only matching context. |
| Unknown/repeated/blank/incompatible filter | Error and previous results retained. |
| Valid zero matches | Successful empty list and count 0. |

Test Parents with phones `00987654` and `0987654`, same-name Parents with different phones,
Parents sharing a phone and unlinked Parents. Same-name Tutors with different phones/assignments stay
distinct. Tutor own-name search uses `n/`, not `tu/`. Use partial Unicode text and all accepted exact fields.

With searches active, enrol/unenrol, edit people/lessons, fail a save, retry and restart. Re-evaluate
relationships/context only on successful changes, even when matching people IDs stay the same.
Switch to Student listing to perform membership operations, then repeat the Parent/Tutor search.
Do not use attendance as evidence of current membership. Read-only requests leave operational bytes
and counters unchanged and do not change the other panel's selected query.

For the shared workflow's lessons:

* `search c/lesson d/Mon from/1700 to/1730` finds only L1: L2 ends at the start and L3 starts at the end.
* `from/1630 to/1800` finds L1, L2, L3. Add `s/Science` for L2 or `st/1600` for L1.
* Missing/equal/reversed endpoints, `2400`, people-category windows and invalid sorts fail without view/write changes.

For ordering, use another fresh store with Tuesday 1600, Monday 0900 and Monday 1400 lessons created
as L1/L2/L3. Default lists show L1/L2/L3; `sort/schedule` shows L2/L3/L1; `sort/creation` restores creation
order. Repeat for Student lessons and search. None renumbers people or changes stored catalogue order.

For tutor phone filtering, create same-name Tutors with phones `0012345` and `93456789`, each with a lesson.
Partial name search finds both; adding `tp/0012345` finds only the first. `tp/12345` finds none;
`tp/12` is invalid and keeps the previous view.

Automated relationship-search tests compare 1,000 seeded queries with an independent raw-record scan.
Keep those results separate from native card/context wrapping checks.

### Lesson details, history and navigation
{: id="lesson-cards-and-result-navigation-320-371"}

Use current and former members, empty lessons, long values and enough records to scroll.

1. From history, type `lessons`, `lessons STUDENT_INDEX`, lesson search, undated details and dated details.
   Each command uses its arguments, regardless of the selected card. Lesson retrieval preserves people
   filtering/positions. A people search works from any right-pane mode; history switches that pane back.
2. Select a Student and use each viewing button with an unfinished draft and forward/reversed text selections.
   Mouse activation preserves command focus, text, caret/selection and people selection/filter/order.
   Dragging off a button before release does not activate it. Tab/Shift+Tab and Space work with normal
   keyboard button focus. All lessons needs no selected person.
3. Clear selection; select Tutor/Parent; hide/delete a selected Student. Student shortcuts must explain
   the missing/wrong target without falling back to another person at its old position or changing results.
4. In protected startup, All lessons gets the normal rejection. Student buttons explain the absent
   selection. No button bypasses protection, saves data or changes the draft/results.
5. Check subject/date headings, separate stable IDs, labelled schedule/Tutor/room/counts/status/membership.
   Current roster and former-member sections are expanded. Missing dated entries say unrecorded.
   At narrow widths, scroll all rows and retain independently usable lists/controls.
6. Check indicators for typing and buttons: catalogue → All lessons; Student lessons → Student lessons;
   history → Attendance history; lesson search/details → none. Card selection, people filtering, help
   and committed refresh do not retarget the held result. Invalid commands/failed saves keep the indicator.
7. Delete history's selected unreferenced Student/Lesson: clear history and reveal retained lesson mode.
   Delete the selected empty lesson detail: keep unavailable-detail context with no shortcut active.
   Unrelated deletions keep valid history. Rejection/save failure keeps old context and results.

#### Dated lesson detail
{: id="active-shared-lesson-details-and-dated-attendance-99"}

Show a Monday lesson with one present and one unrecorded Student, then mark the second absent.
Unenrol the second Student: their record moves to the former-member section. Correct/remove the retained
entry and check immediate refresh, then repeat after restart. Undated details show all former-member
history; dated details show that occurrence only. Former-member entries are newest first; current
rosters follow people creation order.

Reject missing/repeated/unknown/blank fields, malformed IDs/dates, a wrong weekday and unknown Lesson ID
without replacing results or writing. When relationship filtering hides the former member, run Student
listing or an own-name search and recheck the current position before correcting attendance.

#### Student attendance history
{: id="active-student-attendance-history-checks-100"}

1. Choose a Student whose displayed position differs from the ID suffix. Request history, then combine
   `lid/`, `d/` and `s/` in different orders. All filters must match; ordering stays newest date then
   numeric Lesson ID. Headings show active criteria. An existing Student/Lesson with no entries succeeds empty.
2. Omit filters in a new request to clear them. Reject invalid dates/statuses, repeated/unknown prefixes,
   bad positions, stable person IDs and wrong roles without changing previous views or files.
3. Correct status while history is open: an `s/absent` result loses a corrected present entry and gains
   a corrected absence. Unenrol/re-enrol changes Enrolment Status without changing date/status.
   Unmark removes the key; repeating is a no-op. Check saved results after restart.
4. Later people filtering must not retarget held Student history. Switch between history and lessons/details;
   valid retrieval changes the right pane, invalid retrieval preserves it. A failed history-affecting save
   leaves entries/membership unchanged; one retry applies the change.
5. Scroll long results at narrow widths; check current tooltips for all four context/summary labels.
   Protected startup rejects history and its help. Read-only history/help/errors leave bytes unchanged
   and a missing file missing; history row numbers never select people.

`CanonicalHistoryIntegrationTest`, `ResultHeadingTooltipTest` and `CanonicalMainWindowTest` cover the
automated paths; physical platform/scaling checks still need actual observations.

### Guarded person deletion
{: id="active-guarded-person-deletion-91"}

1. Add two Parents and a Student linked by phone to the first. Filter Parents and preview deletion of
   position 1. Check correct name/ID and no write/view change; cancel. Preview and confirm again:
   only that Parent disappears, with Student parent phone unchanged. Delete the other, restart and
   add another: its ID must exceed both deleted IDs.
2. Search for one unreferenced Student whose ID suffix differs from position 1. Preview/confirm removes
   that Student only, leaves search active and empty, and keeps other order/counters. In separate trials,
   run help, list, invalid input or another operation between preview and confirmation: old confirmation fails.
3. With an enrolled Student and assigned Tutor, deletion must name the blocking lesson. Unenrol with no
   history: Student deletion is now allowed, but the Tutor stays blocked by the empty lesson.
4. With retained attendance, unenrolment still does not allow Student deletion, including after restart.
   Unmark removes that dependency. Remove an empty history-free lesson to release its Tutor.
5. Reject missing/zero/negative/out-of-range positions, `delete S1` and extra arguments. Control-character
   cases also have automated coverage. Fail a confirmed save: old data/views remain, and repeating
   `delete confirm` cannot write. A fresh preview/confirmation after restoring access succeeds once.

`CanonicalDeleteIntegrationTest` covers mixed/filtered selectors, all roles, references, controls,
counter history, search continuity, failed saves and retries.

### Future attendance acknowledgement

Use an enrolled Student in a Monday lesson and choose a Monday **after the computer's current date**.
For example, `2036-10-06` is suitable only while it remains future.

1. Mark future present without acknowledgement: warn with supplied date/today, and keep data unchanged.
   Future absent succeeds without acknowledgement.
2. Correct that absence to present: require `ack/future`. Repeating the identical stored present entry
   needs no acknowledgement and performs no write.
3. Acknowledgement must not bypass wrong weekdays, missing membership for a new key or any other guard.
   Reject invalid/repeated `ack/` values too.

<a id="unicode-and-slash-contact-checks"></a>
<a id="unicode-person-name-checks-383-first-increment"></a>

### Contacts, Unicode and slash values
{: id="contact-validation-in-the-current-increment"}

1. In a fresh disposable store, add this Tutor. Enter each example as one line; the spaces before
   the next prefix deliberately exercise boundary-space trimming.

   ```text
   add r/tutor n/  Anne-Marie   O'Neil a/  Blk 10,   #01-02  p/00123456 e/anne.o'neil%school@example.com
   ```

   Expect normalized name/address spacing with punctuation, case and phone zeros kept after restart.
   The Tutor is position 1 in this fresh store. Edit the email, then reload and check its exact spelling:

   ```text
   edit 1 e/Anne.O'Neil@Example.COM
   ```
2. Reject a 16-digit phone, `test@localhost`, digits in a name and an address such as `Blk 10/Unit 2`.
   A very long repeated email must give constraints without crashing or adding a record.
3. In a fresh store, add José Santos (Tutor), 李明 (Student) and தமிழ் (Parent). Reload all spellings;
   create a lesson with full-name `tu/JOSÉ SANTOS`. Rename the Tutor and retain T1/lesson links.
4. Restore José, then edit with decomposed `e` + combining acute. It is the same normalized name and a
   no-op. A same-role/same-phone addition with that equivalent spelling is a duplicate. Jose without
   an accent is distinct; same-name Tutors with different phones still need disambiguation.
5. Add `add r/student n/Ravi s/o Kumar l/P1 pp/91234567 a/Blk 10, #01/02`, then edit to
   `n/Anita d/o Kumar a/Block A12/34B`. Reload and keep identity/links. A Tutor with `s/o` in their name
   works with `tu/` in lesson add/edit. Reject `n/s/o`, `n/Ravi s/other Kumar`, `a/A/B`, `a/#01//02`
   and unknown/repeated prefixes. Search with fragments such as `n/Anita a/A12`.
6. Edit supported JSON copies to contain invalid contacts or normalized duplicates. Protected startup
   must preserve original bytes through help/list/exit and attempted changes. Recover on a working copy.

Use `NameTest`, `CanonicalUnicodeNameIntegrationTest`, `CanonicalSlashContactIntegrationTest` and
`JsonPonHubDataStorageTest` for focused validation/reload coverage.

<a id="planned-people-card-display-checks"></a>
<a id="dormant-person-card-developer-preview"></a>

### People cards and copying contacts
{: id="people-card-display-checks"}

1. Check all roles and optional values. Show prominent names, separate Position/ID labels, leading zeros,
   correct field order and Not provided for missing contacts. Repeat selected/unselected and after recycling.
2. Filtering/refresh must update positions/counts/placeholders and matching context. Preserve selected
   identity only if visible. Long context and contacts remain fully reachable at all tested display sizes.
3. Save clipboard contents you need. Add
   `add r/student n/Copy Test l/P1 pp/00912345 p/00123456 e/Copy.Test@example.com a/12 Main Road`.
   Select another Student and leave a partly selected draft. Click each Copy button on Copy Test and
   paste into a disposable editor: only the exact full contact value is copied, retaining zeros/case.
   Check unchanged person selection, draft/caret/selection, feedback, filters and operational bytes.
4. Tab/Shift+Tab and Space activate Copy with visible focus/accessibility labels. Ctrl+C on Windows/Linux
   or Cmd+C on macOS works while a Copy button is focused. Normal copying in command/feedback text still works.
   Missing values have no button; required Tutor/Parent phones do. Edit/clear a value and recycle the
   card: copy actions must show current data or disappear. Restore your clipboard after testing.

<a id="keyboard-menu-access-on-macos"></a>

### Help and keyboard menus
{: id="inline-help-and-supported-routes"}

1. `help` lists one short description per available command and points to topic help. Check every active
   topic against its parser and runnable examples. `help exit` must not exit. Uppercase topic names work;
   unknown topics and `help add delete` give corrective guidance without execution.
2. With a selected card and unfinished draft, use Help menu and F1 from command/result focus. Expect
   the same inline help, retained draft/selection and no new window. Protected help lists only
   help/list/exit; edit/retrieval help must not bypass protection.
3. At narrow/wide sizes, focus Result Display and scroll to the final line with arrows/Page Up/Down,
   then mouse/trackpad. Its orange border must not move layout. Shift+Tab returns through controls.
   Each new result starts at the top; replacing long help with short feedback must work.
4. Unknown `clear`/`find`, invalid selectors, malformed lesson/attendance commands and `exit extra`
   give errors without saving, changing results or exiting. Help works with unwritable operational storage
   and changes no operational bytes; missing data stays missing. Preferences are separate.

On the prescribed macOS runtime, use **Control+F10** (with Fn/Globe if needed), release keys and press
Down for the in-window File menu. Right switches to Help; Down/Enter opens inline help.
Control+F2 selects the macOS system menu instead, and ordinary Tab skips the in-window menus.
Check Left/Right, Escape, F1 and File > Exit in the disposable store. Repeat from a viewing-button focus;
keep draft text and return focus to that button. Assess normal TextField selection changes on later Tab
separately. Record physical-key and automated results separately.

### Command-name completion

1. Type `sho`, press Tab: expect showlesson without submitting, feedback or data changes.
   Type `ad`, press Tab repeatedly: add → addlesson → add. Shift+Tab moves focus backwards.
2. Add leading/repeated/trailing spaces and arguments, place the caret after the first-word prefix,
   then complete. Preserve all other text/spacing and put the caret after the completed word.
   Caret/focus/selection/text changes reset the cycle.
3. Blank/unmatched input, a selection and Tab in arguments keep text unchanged and use normal traversal.
   Enter alone submits. Correct a failed draft with completion and retry.
4. Protected startup completes only help/list/exit. Menu/F1 help still keeps the unfinished draft and
   selection; rejected operational bytes remain unchanged.

### Command recall

1. Up in a fresh window changes nothing. Submit help and a role listing, then type/select an unfinished
   draft. Up browses newest-to-oldest; Down returns through commands and restores draft/caret/selection.
   Neither key executes or saves.
2. Edit a recalled command and submit once. Failed submissions are editable and recallable. Tab still
   completes a restored prefix. Modified arrows retain normal editing; browsing keeps command focus.
3. Close/reopen: no earlier submissions remain. `CommandBoxTest` checks 50-entry eviction separately.
   Recheck current people positions before rerunning recalled changes.

### Raw command input

Paste a name containing a newline, then `edit 1 e/` followed by a newline. Reject the complete replacement
and block Enter without executing or changing the contact. Repeat with tab, CRLF, context-menu paste and
a reversed selection. Moving the caret alone must not unlock submission. Correcting text permits a normal
command; deliberately entered blank `e/` must still clear an email.

### Optional logo sound

1. Launch with working sound output and check no automatic cue. Keep an unfinished draft with selected
   text, feedback and views; primary-click the wordmark. Expect one short cue without changing focus,
   draft/selection, feedback, results or data. Secondary-click does not request sound.
2. Rapid clicks must not layer playback. Replay after it finishes; commands stay responsive.
   Missing/muted audio must not block normal work; record inaudible playback as unverified.
3. Close during playback: exit cleanly without continuing audio or extra windows.

The [audio resource note](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/master/src/main/resources/audio/README.md)
records provenance. `LogoSoundPlayerTest` and `MainWindowTest` use controlled audio and do not prove physical playback.

### Launch, paths and shutdown
{: id="launch-and-shutdown"}

1. Copy the matching JAR into a fresh disposable folder. Launch `java -jar ponhub.jar`. Check an empty
   people view and window inside the work area. Help/list/exit must leave `data/ponhub.json` missing;
   preferences may save separately.
2. Make a successful change and restart. Check complete records and counters in the selected file.
   Select another missing path with `--data=data/another-ponhub.json`: expect a separate empty dataset
   and unchanged preference location/old data. Launch from another terminal directory: files still use JAR home.
3. Try `--data="separate store/ponhub.json"`; check footer and save/reload path. Default startup must
   leave legacy `data/addressbook.json` unchanged.
4. Reject blank data paths, `../outside.json`, outside absolute paths, symbolic escapes and data/preferences/
   reserved-log aliases before unsafe access. Check logs/preferences remain separate from operational data.
5. Test saved placement and preference failures as below; distinguish packaged path checks from automated
   initialization and logical-size checks.

### Preference recovery
{: id="preference-recovery-checks"}

Keep a supported operational-file copy for comparison after each disposable trial.

1. With the app closed, set preferences to `null`, then try null guiSettings and null dimensions.
   Startup should use/save usable defaults, with operational bytes unchanged.
2. Try `{}` and `{"guiSettings":{"windowWidth":910,"windowHeight":710,"windowCoordinates":null}}`.
   Missing fields use defaults; valid edited dimensions are kept if the screen allows them.
3. Replace preferences with a nonempty directory before launch. Read/initial-save failures must not
   prevent startup, help/list/exit or shutdown; directory contents and operational bytes remain unchanged.
4. Repeat replacing preferences with a directory while running. Shutdown logs its save error without
   blocking exit or changing records. Repeat with missing operational data: it must stay missing.
5. In bounded subprocess stress probes, check oversized numbers/nesting, long error strings, nonfinite
   dimensions and extreme coordinates. Defaults must remain reloadable, diagnostics bounded and
   operational data intact. Keep automated stress results separate from physical window observations.

### Two windows saving the same file
{: id="accidental-second-window-save-protection"}

1. In a disposable folder, launch two windows A/B **before either saves**, both with
   `java -jar ponhub.jar --data=manual/two-windows.json`. Both start empty without creating data.
2. In A, add Alex as S1 and hash the saved file. In B, try adding Jamie: expect a changed-file save error
   requiring restart, no live Jamie and no consumed Student ID. A and the saved hash remain unchanged.
3. Retry in B: still reject. Help/list remain usable but show B's stale data, not an updated file view.
   Exit both and compare hashes. Keep the empty `.ponhub-lock` sidecar; it is normal after exit.
4. Restart one window: Alex S1 loads. Add Jamie successfully as S2 and restart to verify both.
5. Repeat with an existing supported file loaded in both before A changes it. B's later change must
   preserve A's bytes and B's previous live view.

Lock contention may first give a busy-save error. After the other save finishes, a retry must detect
the changed file and require restart. Never delete the sidecar, overwrite newer data from a stale window
or edit the data while instances run. This is a bounded safeguard, not synchronized multiuser editing.

### Loading and recovery
{: id="canonical-loading-and-recovery"}

<a id="planned-canonical-loading-and-recovery"></a>

1. Check the missing-file lifecycle and normal save/reload above. Never use a rejected/legacy file as a new store.
2. Close the app and edit a supported copy while retaining IDs/counters and valid former-member history.
   A fully valid edit must load as a complete snapshot.
3. Use separate copies with malformed JSON, null roots/records, wrong shapes, duplicate IDs/normalized
   people, dangling/wrong-role references, invalid counters and unsupported versions. Include duplicate
   attendance keys, invalid dates/statuses and wrong weekdays. A future-version file with `persons`
   must not be mistaken for legacy data.
4. Save each rejected file's bytes. Expect a labelled protected empty view, recovery guidance and only
   help/list/exit. Attempt changes and retrieval: no writes and identical rejected bytes. List repeats
   the diagnostic. Repair/remove a working copy while running: the session stays blocked. Restart with
   a corrected compatible copy to recover.
5. For legacy/rejected contact formats, keep originals/backups and manually re-enter explicit roles and
   required fields in an unused store. Do not silently convert, drop records or guess roles.

<a id="numeric-json-resource-boundary-573"></a>

#### Numeric JSON boundary

Use separate disposable stores. Automated tests check 64/65-character numeric boundaries, huge version
integers and a 500,000-digit counter. For a packaged check, replace a supported empty store's STUDENT
counter with an unquoted 500,000-digit integer, then launch. Expect controlled protected startup,
preserved bytes and blocked saves. Do not modify real records.

<a id="readable-protected-load-diagnostics-575"></a>

#### Readable load errors

Test one supported empty store with a 5,000-character unknown root field, then a separate `{bad` file.
Check concise visible recovery guidance and complete scrollable/copyable details. Reach the end by keyboard,
Tab to OK and dismiss with Escape. Summary/buttons must remain accessible. Both rejected files must stay
byte-identical after dismissal and shutdown.
