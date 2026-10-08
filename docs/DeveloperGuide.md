---
layout: page
title: Developer Guide
---
* Table of Contents
{:toc}

--------------------------------------------------------------------------------------------------------------------

## **Acknowledgements**

* PonHub is based on [AddressBook-Level3](https://github.com/se-edu/addressbook-level3), created by the [SE-EDU initiative](https://se-education.org).
* Zhu Zhi Yu (`ultramanarm`) used OpenAI Codex to assist with the student, tutor, and parent record models, their common interface, the ordered people registry and its import/export state, automated tests, and design documentation. This acknowledgement covers those bounded contributions.
* Zhu Zhi Yu used OpenAI Codex for the detached role-filtered people-list projection and argument parser, their regression tests, and the integration notes below.
* Zhu Zhi Yu used OpenAI Codex for the dormant current-people index resolver, its role and filtered-view regression tests, and the related integration documentation.
* Zhu Zhi Yu used OpenAI Codex to prepare representative canonical people samples, their sample-integrity and view-selection tests, and the related developer usage notes.
* Zhu Zhi Yu also used OpenAI Codex for PR review, merge-conflict reconciliation, and the integration documentation and Javadoc formatting corrections in the search, inline-help, and atomic-save increments.
* Zhu Zhi Yu used OpenAI Codex to align the assigned product scope and prioritized user stories with the shared-lesson target, preserving the distinction between delivered foundations, planned runtime features, and future extensions. This attribution covers that documentation update.
* Zhu Zhi Yu used OpenAI Codex to document the confirmed person command-index boundary and reconcile shared-lesson command formats, staged search scope, legacy recovery policy, use cases, and manual checks. This acknowledgement covers those documentation changes; feature implementations remain with their owners.
* Zhu Zhi Yu used OpenAI Codex to assist with the file-logging startup fallback, its isolated regression tests, and logging documentation.
* Zhu Zhi Yu used OpenAI Codex for the UI startup error-handling fix and its regression tests.
* Zhu Zhi Yu used OpenAI Codex for dormant role-aware add argument parsing, its immutable ID-free input, regression tests, and integration documentation.
* Zhu Zhi Yu used OpenAI Codex for constant-stack email validation that preserves the inherited contact rules, its parser/file-loading regressions, and the related implementation and manual-testing notes.
* Zhu Zhi Yu used OpenAI Codex to clarify the course-prescribed Java 25 and macOS runtime setup, release verification, and manual-testing documentation.

* PonHub builds on [AddressBook-Level3](https://github.com/se-edu/addressbook-level3) by the SE-EDU initiative. Existing acknowledgements and licences are retained.
* Existing libraries: [JavaFX](https://openjfx.io/), [Jackson](https://github.com/FasterXML/jackson), and [JUnit 5](https://junit.org/junit5/).
* Ernest's Week 8 help increment used OpenAI Codex to inspect the repository, generate and revise the command catalogue, inline-help implementation, regression tests, and Ernest's documentation coordination changes. This attribution covers that increment; it does not claim authorship of teammates' feature implementations or imply teammate review has occurred.
* Ernest used OpenAI Codex for the #129 active UI field-encapsulation and Javadoc standards corrections, caller verification, and repository checks.
* Zhu Zhi Yu used OpenAI Codex to prepare the dormant person-card display projection, FXML renderer, projection and renderer tests, isolated developer preview, Linux CI virtual-display setup, and the related integration and manual-testing documentation. This attribution covers that card increment.

--------------------------------------------------------------------------------------------------------------------

## **Setting up, getting started**

Refer to the guide [_Setting up and getting started_](SettingUp.html).

--------------------------------------------------------------------------------------------------------------------

## **Design**

### Current increment and command integration

This increment implements inline help and command-specific guidance. The runtime still uses inherited AB3 people and storage. Its active commands are `add`, `delete`, `list`, `help`, and `exit`. Role-aware people, stable IDs, shared lessons, enrolment, attendance, history, and search remain integration targets until their owners' code and complete persistence support are merged. Existing domain use cases and model/storage diagrams describe the inherited implementation or proposed product, not a completed shared-lesson runtime.

`CommandCatalog` is the single registration point for active parsers, short descriptions, and usage messages. `AddressBookParser` extracts the command word and delegates to it. `HelpCommandParser` accepts zero or one topic, normalizes topics with `Locale.ROOT`, and rejects unknown topics and excess arguments with actionable guidance. Help reads command-owned usage constants; it does not maintain a second copy of command syntax.

Feature owners supply their parser, usage and runnable example. Register a mutation only when its aggregate, validation, and save/reload behavior work together. Replace the legacy people entries at the canonical runtime cutover; legacy `delete` must be disabled until guarded canonical deletion resolves the current people-list index to a stable person ID. `edit`, `clear`, and `find` are already withdrawn from dispatch and help in this increment. Their inherited implementation classes remain for incremental cleanup and direct regression tests.

When registering or withdrawing a command, update catalogue/router tests, its UG entry and manual tests in the same PR. A summary topic must never advertise an unavailable command. `list` and `exit` now reject unexpected arguments instead of silently ignoring them.

### Shared-lesson target contract

The broader team integration target uses one authoritative people collection, one independent Lesson collection, and separate dated Attendance records. Student composes common contact details and holds education level and parent contact. Student must not own copies of shared lessons. Each Lesson owns `enrolledStudentIds`; derive student lessons, rosters and tutor schedules from that association. Attendance has the unique key student ID + lesson ID + date, with `present` or `absent`; a missing entry is unrecorded.

**Confirmed person-selector boundary:** `INDEX` and `STUDENT_INDEX` arguments use a positive one-based position in the current filtered **people list**, following AB3's command-input convention. This applies to deletion and to the student selector for enrolment, unenrolment, attendance, history, and student-specific lesson retrieval when those routes are integrated. An index selects the card at that position in the current people view, not that position in the unfiltered collection, a lesson roster, or a lesson/history result. Validate the range and required role, then resolve the index once to the selected record's stable `PersonId` before invoking Model or query APIs. Reuse that resolved ID throughout the operation; later view refreshes must not select another record by reinterpreting the index. Lesson creation has the separate named-tutor lookup described below, which also resolves once to a stable ID.

Stable person IDs such as `S1`, `T1` and `P1` remain displayed, stored, and used by internal lookup, queries, relationships and persistence. Cards distinguish the current-view index from the stable ID. Filtering or reordering renumbers the people view from one without changing stored identities. Stable lesson IDs such as `L1` remain displayed and persisted; all command references to an existing lesson use `lid/LESSON_ID`, for example `lid/L1`. Lesson catalogue, roster and history results use separate views and preserve the current people filter, order and indices. Their row positions never replace people-list command selectors.

The planned lesson-catalogue filter is `lessons [si/STUDENT_INDEX]`. Its optional student selector follows the same current people-list index and Student-role validation, then passes the resolved stable ID to the internal student-lesson query. Internal Student-ID query APIs remain ID-based. This planned route is not yet active.

**Tutor selection:** `addlesson` resolves `tu/TUTOR_NAME` as a normalized full name, ignoring letter case and repeated spaces. Optional `tp/TUTOR_PHONE` must match the tutor's exact phone number; never ignore a supplied phone to fall back to the name. Reject zero matches or ambiguity without changing data, and request a disambiguating phone when needed. Search `tu/` instead matches a case-insensitive name fragment. Tutor-category name search uses `n/`. Stable person IDs are not external lookup selectors for these commands.

The planned shared-lesson routes are:

| Action | Format |
| --- | --- |
| Create lesson | `addlesson d/DAY st/HHMM et/HHMM s/SUBJECT tu/TUTOR_NAME [tp/TUTOR_PHONE] rm/ROOM` |
| Enrol / unenrol | `enrol STUDENT_INDEX lid/LESSON_ID` / `unenrol STUDENT_INDEX lid/LESSON_ID` |
| Delete lesson | `deletelesson lid/LESSON_ID` |
| Catalogue | `lessons [si/STUDENT_INDEX]` |
| Lesson detail | `showlesson lid/LESSON_ID [d/YYYY-MM-DD]` |
| Mark / correct | `mark STUDENT_INDEX lid/LESSON_ID d/YYYY-MM-DD s/present\|absent` |
| Unmark | `unmark STUDENT_INDEX lid/LESSON_ID d/YYYY-MM-DD` |
| History | `history STUDENT_INDEX [lid/LESSON_ID]` |

The complete [search matrix](#supported-criteria) remains the planned target. Own-contact searches can be integrated first, followed by the relational families. Each delivered route advertises only its available filters; staging does not silently remove the remaining filters from the target. Documenting a route does not activate it.

Create lessons with empty rosters. Tutor and room clashes are checked once per shared lesson; student timetable clashes are checked on enrolment. Time intervals are half-open so adjacent bookings are allowed. Unenrolment removes membership and retains attendance. New attendance requires membership; existing history can be corrected or unmarked after unenrolment. Block student deletion for enrolment/history, tutor deletion for lesson references, and lesson deletion for enrolment/history. Deleting a separate Parent preserves the student's stored parent phone; parent links derive from matching phone values.

All mutations go through Model APIs using stable identities after command-boundary resolution. Complete aggregate copy/reset/equality and independent snapshots cover people, lessons, memberships, attendance and monotonic allocation state; deleted committed IDs are not reused. Vincent coordinates validated versioned JSON and transactional saves; failed saves must restore data and active views before those commands are enabled. The canonical runtime cutover must connect a compatible people view, index resolution, guarded commands and persistence together. Do not persist displayed indices or introduce a second people store; verify that separate lesson/roster/history results preserve the people view used by the next command. Preferences remain separate. Capacity, waiting lists, make-ups, export, saved searches, archiving, richer attendance statuses or notes, attendance percentages, fees, lesson editing, occurrence cancellation, undo/redo and guided advanced search are future extensions.

**Iteration boundary:** The [Week 8 course instructions](https://nus-cs2103-ay2627-s1.github.io/website/schedule/week8/project.html) call for small first increments towards the simplest MVP. Each member should aim for a meaningful reviewed and merged code PR. An individual feature need not be complete end-to-end, but intermediate versions must remain working; this iteration requires no product release. The broader team target remains visible in [tracker #59](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/59). Carry unfinished work into v1.3 honestly. Compatible loading and rollback remain safety requirements when a mutation is activated, rather than a requirement to finish every planned feature in Week 8.

Zhu owns people/testing; Yang Shuo owns lessons/enrolment/attendance; Ben owns retrieval/views; Vincent owns saving/loading/attendance history; Ernest owns routing/help and documentation coordination. Each owner authors their own feature documentation in the existing UG/DG. Ernest reconciles shared conventions and examples without taking ownership of those implementations.

<div markdown="span" class="alert alert-primary">

:bulb: **Tip:** The `.puml` files used to create diagrams are in `docs/diagrams`. Refer to the [_PlantUML Tutorial_ at se-edu/guides](https://se-education.org/guides/tutorials/plantUml.html) to learn how to create and edit diagrams.
</div>

### Architecture

<img src="images/ArchitectureDiagram.png" width="280" />

The ***Architecture Diagram*** given above explains the high-level design of the App.

The following provides a quick overview of the main components and their interactions.

**Main components of the architecture**

**`Main`** (consisting of classes [`Main`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/Main.java) and [`MainApp`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/MainApp.java)) is in charge of the app launch and shut down.
* At app launch, it initializes the other components in the correct sequence, and connects them up with each other.
* At shut down, it shuts down the other components and invokes cleanup methods where necessary.

The bulk of the app's work is done by the following four components:

* [**`UI`**](#ui-component): The UI of the App.
* [**`Logic`**](#logic-component): The command executor.
* [**`Model`**](#model-component): Holds the data of the App in memory.
* [**`Storage`**](#storage-component): Reads data from, and writes data to, the hard disk.

[**`Commons`**](#common-classes) represents a collection of classes used by multiple other components.

**How the architecture components interact with each other**

The *Sequence Diagram* below shows how the components interact with each other for the scenario where the user issues the command `delete 1`.

<img src="images/ArchitectureSequenceDiagram.png" width="574" />

Each of the four main components (also shown in the diagram above),

* defines its *API* in an `interface` with the same name as the Component.
* provides its functionality through a concrete `{Component Name}Manager` class that implements the corresponding API interface.

For example, the `Logic` component defines its API in `Logic.java` and implements it in `LogicManager.java`. Other components interact with a component through its interface rather than its concrete class, preventing them from coupling to that component's implementation, as illustrated in the following partial class diagram.

<img src="images/ComponentManagers.png" width="300" />

The sections below give more details of each component.

### UI component

The **API** of this component is specified in [`Ui.java`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/ui/Ui.java)

![Structure of the UI Component](images/UiClassDiagram.png)

The UI consists of a `MainWindow` and its parts, such as `CommandBox`, `ResultDisplay`, `PersonListPanel`, and `StatusBarFooter`. All of these, including `MainWindow`, inherit from the abstract `UiPart` class, which captures common behavior among classes that represent visible GUI parts.

The `UI` component uses the JavaFX UI framework. The layouts of these UI parts are defined in matching `.fxml` files in `src/main/resources/view`. For example, [`MainWindow.fxml`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/resources/view/MainWindow.fxml) specifies the layout of [`MainWindow`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/ui/MainWindow.java).

The `UI` component,

* executes user commands using the `Logic` component.
* listens for changes to `Model` data so that the UI can be updated with the modified data.
* keeps a reference to the `Logic` component, because the `UI` relies on the `Logic` to execute commands.
* depends on some classes in the `Model` component because it displays `Person` objects from the model.

At the planned canonical people-view integration, cards show the current filtered people-list index separately from the stable person ID. A separate lesson, roster or history result must not replace or renumber that people view. These requirements describe the integration target; the inherited `Person` UI remains active in this increment.

### Logic component

**API** : [`Logic.java`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/logic/Logic.java)

Here's a (partial) class diagram of the `Logic` component:

<img src="images/LogicClassDiagram.png" width="550"/>

The sequence diagram below illustrates the interactions within the `Logic` component, taking `execute("delete 1")` API call as an example.

![Interactions Inside the Logic Component for the `delete 1` Command](images/DeleteSequenceDiagram.png)

<div markdown="span" class="alert alert-info">:information_source: **Note:** The lifeline for `DeleteCommandParser` should end at the destroy marker (X), but due to a limitation of PlantUML, it continues to the end of the diagram.
</div>

How the `Logic` component works:

1. When `Logic` is called upon to execute a command, the command is passed to an `AddressBookParser` object, which in turn creates a parser that matches the command (e.g., `DeleteCommandParser`) and uses it to parse the command.
1. This results in a `Command` object (more precisely, an object of one of its subclasses e.g., `DeleteCommand`) which is executed by the `LogicManager`.
1. The command can communicate with the `Model` when it is executed (e.g. to delete a person).<br>
   Note that although this is shown as a single step in the diagram above for simplicity, the code can require several interactions between the command object and the `Model` to complete the operation.
1. The result of the command execution is encapsulated as a `CommandResult` object which is returned from `Logic`.

Here are the other classes in `Logic` (omitted from the class diagram above) that are used for parsing a user command:

<img src="images/ParserClasses.png" width="600"/>

How the parsing works:
* `AddressBookParser` delegates to the registered parser in `CommandCatalog`. An `XYZCommandParser` (`XYZ` is a placeholder for the specific command name, e.g., `AddCommandParser`) uses the other classes shown above to parse the arguments and create an `XYZCommand` object. The router returns that object as a `Command`.
* All `XYZCommandParser` classes, such as `AddCommandParser` and `DeleteCommandParser`, implement the `Parser` interface so they can be treated similarly where appropriate, for example during testing.

For planned canonical person commands, parse a positive index and resolve it against the current filtered people list at execution, checking the selected role. Capture the stable person ID once before calling ID-based Model/query APIs. The inherited `delete 1` example selects the first person in the current people view; the planned guarded route retains that input convention while using stable identity internally.

#### Read-only retrieval API contract (#113)

`Logic` now declares the UI-facing retrieval methods below. This increment defines their signatures and read-only
result shapes only. Until the canonical Lesson, Attendance and aggregate stores are integrated, every new method in
`LogicManager` throws `UnsupportedOperationException` with `Shared-lesson retrieval is not implemented yet`. No new
command or view is active, and the inherited people list continues to work.

| `Logic` method | Planned result and ordering |
| --- | --- |
| `getLessonList()` | All shared lessons in creation order, once each, including empty rosters. |
| `getFilteredLessonList()` | Active lesson-filter results in catalogue order, once per lesson; an empty match is valid. |
| `findLessonById(LessonId)` | Lookup in the full catalogue regardless of filter; a valid unknown ID gives `Optional.empty()`. |
| `getStudentLessons(PersonId)` | The student's current lessons, derived from Lesson-owned enrolled student IDs, in creation order. |
| `getLessonRoster(LessonId)` | Current students resolved through the people registry, in people creation order; an empty roster is valid. |
| `getTutorSchedule(PersonId)` | Assigned lessons, including empty ones, by weekday (Monday first), start time, then numeric lesson ID. |
| `getAttendanceHistory(PersonId)` | All recorded dates for a student, newest first, then numeric lesson ID. |
| `getAttendanceHistory(PersonId, LessonId)` | The same retained history restricted to one lesson, regardless of current enrolment. |

Java callers pass `PersonId` or `LessonId`, never a displayed row position. After integration, null IDs are rejected,
an unknown or wrong-role ID for a list query is rejected, and an existing record with no matching results yields an
empty list. `findLessonById` alone uses an empty optional for an unknown lesson. The current declaration stubs throw
before validating arguments. At the command boundary, `lessons si/STUDENT_INDEX` and `history STUDENT_INDEX` resolve
the one-based position in the current people list once, check the Student role, and then pass that record's stable
`PersonId`. Existing lessons are selected by prefixed `lid/LESSON_ID`. These result views preserve the people list,
its filter and its command indices.

The `model.query` interfaces describe projections rather than persisted records. `LessonView` exposes a `LessonId`,
assigned tutor `PersonId` and current name, `LessonTimeSlot`, `Subject`, `Room` and current roster size.
`StudentView` exposes a student `PersonId`, current name and level, required parent phone and optional own contact
fields. `AttendanceHistoryEntry` exposes the student ID, lesson ID, date, recorded present/absent status and a separate
current-enrolment flag. An absent attendance entry is unrecorded. History survives unenrolment because its source is
the attendance store, not the current roster. Each projection describes one model state; changed records replace
entries in the observable lists.

All list results will be unmodifiable observable views. The canonical Model will own query derivation, filtering and
refresh, while `LogicManager` exposes the results. A list object already held by the UI must remain subscribed after
lesson/person changes, enrolment, attendance changes and successful rollback; refresh entries only after a committed
change, or restore the pre-command results on failed save. Retrieval does not save data, alter identity allocation, or
replace the people view. Search still enters through `Logic.execute(String)` and must follow the complete
[`SearchField` matrix](#supported-criteria), including same-student/same-lesson matching when those slices land.
Lesson creation resolves a normalized full tutor name; search `tu/` matches a partial tutor name.

### Model component
**API** : [`Model.java`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/model/Model.java)

<img src="images/ModelClassDiagram.png" width="450" />


The `Model` component,

* stores the address book data i.e., all `Person` objects (which are contained in a `UniquePersonList` object).
* stores the `Person` objects selected by the current filter, such as search results, in a separate _filtered_ list. It exposes this list as an unmodifiable `ObservableList<Person>` that the UI can observe and bind to, so the UI updates when the list changes.
* stores a `UserPrefs` object that represents the user’s preferences (currently, just the GUI settings). This is exposed to the outside as a `ReadOnlyUserPrefs` object.
* does not depend on any of the other three components (as the `Model` represents data entities of the domain, they should make sense on their own without depending on other components)

<div markdown="span" class="alert alert-info">:information_source: **Note:** The alternative, arguably more object-oriented, design below keeps a unique list of tags in `AddressBook`, and each `Person` references tags from that list. This lets `AddressBook` maintain one `Tag` object per unique tag instead of each `Person` holding its own `Tag` objects.<br>

<img src="images/BetterModelClassDiagram.png" width="450" />

</div>


### Storage component

**API** : [`Storage.java`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/storage/Storage.java)

<img src="images/StorageClassDiagram.png" width="550" />

The `Storage` component,
* can save both address book data and user preference data in JSON format, and read them back into corresponding objects.
* is implemented by `StorageManager`, which delegates the actual JSON file access to `JsonAddressBookStorage` and `JsonUserPrefsStorage` (one class per data file).
* depends on some classes in the `Model` component (because the `Storage` component's job is to save/retrieve objects that belong to the `Model`)

#### Safe JSON saves

Address-book data and preferences share a UTF-8 file writer. JSON serialization finishes before writing.
Existing destinations are resolved to their real paths, so saving through a symbolic link updates its
target and preserves the link. Dangling/cyclic links and inaccessible destinations fail before writing.
New files use a resolved parent directory, creating missing directories as needed. Concurrent external
changes to links/files and multiple application instances are not coordinated.

A complete temporary sibling file is written and closed before atomic replacement is attempted.
Only `AtomicMoveNotSupportedException` selects the fallback; permission and other move errors propagate.
The fallback first copies the existing destination into a unique sibling `.ponhub-backup-*.bak` file.
If backup creation/copy fails, replacement is not attempted. After a failed ordinary replacement, the
writer restores the old bytes from that backup (or removes a partial destination for a first save).
If restoration also fails, the complete backup is retained and its path is included in the error;
restore it before continuing or restarting. Never delete such a backup without recovering its contents.

After success, backup cleanup is best effort: a cleanup failure is logged without reporting the committed
save as failed. Other cleanup failures are suppressed on the primary error. Parent directories or
recovery/temporary files can remain after failures or abrupt termination.

The fallback permits saving on filesystems without atomic moves, but is not atomic: concurrent readers
can observe an incomplete destination during replacement/recovery. Its guarantee is a recoverable old
copy, not uninterrupted access at the original path. Neither path promises power-loss durability or
preserves all previous file attributes. Save failures still do not roll back in-memory command changes;
that is separate transaction work. Help skips saving; other read-only commands currently still save,
but unsupported atomic moves alone no longer make those saves fail.

#### JSON version detection foundation

`JsonDataVersionDetector.detect(String)` classifies file contents using the existing Jackson dependency,
without constructing Model objects or writing files. The current loader uses this gate before decoding. The planned
PonHub envelope reserves `schemaVersion` as a positive JSON integer, initially `1`; this is a data-schema
version, independent of the application version. Issue #79 will define and validate the remaining envelope.

| Result | Meaning and later loader action |
| ------ | ------------------------------- |
| `LEGACY_AB3` | An unversioned object containing a `persons` array and, optionally, the textual `_comment` used by AB3 fixtures. No other root fields are accepted. Contact validity is not checked and roles are never inferred; preserve it for manual upgrade. |
| `PONHUB_V1_CANDIDATE` | Explicit integer `schemaVersion: 1`. Continue to complete envelope/domain validation; this result alone never authorizes loading or saving. |
| `INVALID_VERSION` | An explicit version is null, blank, a string, boolean, collection, fractional number, zero or negative. Reject without coercion. |
| `UNSUPPORTED_VERSION` | A positive integer other than 1, including values beyond Java integer range. Reject even if `persons` looks like legacy data. |
| `UNRECOGNIZED_FORMAT` | A non-object root or an unversioned object that does not match the legacy root, including extra unknown fields. Reject rather than ignoring fields. |
| `MALFORMED_JSON` | Empty/invalid JSON, duplicate object keys or trailing content after the document. Reject ambiguous input. |

Missing versions identify legacy data only for the strict legacy root shape. A versioned candidate can
still contain missing or invalid records: the detector is a format gate, not a schema/domain validator.
Callers must handle file absence and read errors separately; neither is equivalent to empty JSON.
The current `JsonAddressBookStorage` classifies and decodes the same string. It accepts only legacy AB3
roots in this runtime and rejects every other classification, including version-1 candidates until their
codecs are integrated. Missing files retain the existing fresh/sample-data behavior. Only a confirmed
missing path is treated as absent; denied access and dangling final symbolic links are loading failures.

#### Current protected startup

On a controlled loading failure, `StorageManager` records recovery guidance and locks operational saves
for the rest of the session. `MainApp` creates an empty display model, not a replacement writable store.
The UI displays a recovery warning; `LogicManager` allows only help/list/exit and bypasses saving for
those commands in the protected session. Other commands are rejected before execution. Storage also
rejects direct operational saves, while preferences retain their separate lifecycle. Deleting or fixing
the file while the session is running does not unlock it: recovery requires a restart.

JSON syntax, root/version errors, invalid contact records and I/O failures become controlled loading
errors. Explicit checks reject null person/tag entries; unrelated programming errors are not swallowed.
Valid legacy contacts still load and save normally. This safety increment does not implement the new
aggregate codec, automatic migration, general command rollback or no-save behavior in normal sessions.

Manual check: in a disposable working folder, place malformed JSON, `null`, or a versioned document in
`data/addressbook.json` and record its bytes. Start the app and verify the recovery warning. Run help,
list, exit and attempt add/delete; restart as necessary. The original bytes must remain unchanged and
mutations must be blocked. Close the app, preserve the original, then restore a known-good compatible
backup or correct JSON/access permissions and restart. Confirm valid human-edited legacy data loads.
`ProtectedStartupTest` exercises the startup/command/storage boundary and original-byte preservation;
`UiManagerTest` verifies the warning without requiring a real window.

#### Planned protected loading and legacy upgrade

These are requirements for the first canonical cutover, not behavior delivered by the inherited loader. Keep the inherited runtime until the compatible canonical Model, codecs, people commands and protected loader are activated together. The new runtime must classify the configured file before decoding: missing data may start a fresh supported root; valid supported versioned data loads only after complete domain, identity and reference validation. Unreadable, corrupt, unsupported-version and unversioned AB3 files produce controlled errors, actionable recovery guidance and no operational writes to the rejected file. Preserve its original bytes through help, list, exit and attempted mutations; saving preferences remains separate.

The initial upgrade policy is deliberate backup and manual re-entry into a separate new supported store. An unversioned AB3 contact does not contain enough information to infer a Student, Tutor or Parent role, and a Student additionally needs its level and parent phone. Do not invent those values, discard invalid records, or convert the original in place. Once the canonical build is available, tell the administrator to close the app, preserve a backup of the original data folder, deliberately start that compatible build in a separate empty folder, and re-enter records with explicitly chosen roles and required fields using the delivered commands. The preserved original and backup are not operational write targets; only the explicitly chosen new supported root is writable. Check the new records before retiring the old working folder. A migration importer and application-level export are future extensions; this policy does not introduce an `import`, `export` or `recover` command.

The contact changes in #61 can reject formerly accepted numeric-only names, long phones or slash-containing addresses, or reveal duplicates after normalization. Diagnose the affected values without overwriting the rejected data. Inspect the local JSON read-only; if the older build is needed to inspect contacts, use an additional working copy, never the preserved original or backup. The administrator supplies any corrections and missing role information. Correctly human-edited supported JSON must load: document the delivered schema, preserve valid stable IDs and allocation state, and validate all relationships without requiring users to avoid editing the file. An invalid edit is rejected as a whole with its bytes preserved. The [planned manual checks](#planned-canonical-loading-and-recovery) cover this boundary.

### Common classes

Classes used by multiple components are in the `seedu.address.commons` package.

--------------------------------------------------------------------------------------------------------------------

## **Implementation**

This section describes some noteworthy details on how certain features are implemented.

### Lesson scheduling value contracts

The lesson scheduling foundation is represented by immutable value objects in `model.lesson`. These objects validate and
normalize scheduling input before later lesson commands or persistence code use it:

* `LessonId` is a stable identifier consisting of `L` followed by a positive `long` sequence number. Lower-case prefixes
  are normalized, leading zeros are rejected, and advancing beyond `Long.MAX_VALUE` fails explicitly instead of wrapping.
  The canonical aggregate introduced in a later increment will own allocation state so committed IDs are not reused.
* `LessonDay` accepts the case-insensitive abbreviations `Mon` through `Sun` and stores their canonical display form.
* `LessonTime` accepts exactly four digits in 24-hour `HHMM` format and retains leading zeros when displayed.
* `LessonTimeSlot` combines one weekday with a start and end time. Its end must be strictly later than its start, so
  overnight lessons are rejected. Slots are treated as half-open ranges: `[start, end)`. As a result, adjacent slots do
  not overlap, and equal time ranges on different weekdays do not clash.
* `Subject` contains 1–50 letters, digits or spaces after trimming and reducing repeated spaces to one.
* `Room` contains 1–10 letters or digits. It is normalized to upper case so room comparisons do not miss clashes because
  of letter case.

This increment establishes domain contracts only. It does not activate or advertise a lesson command; lesson creation,
global tutor/room clash checks, ID allocation and persistence are integrated in later increments.

### Immutable shared lesson records

`Lesson` represents one canonical recurring teaching slot. It stores a stable `LessonId`, one tutor-role `PersonId`,
the validated `LessonTimeSlot`, `Subject` and `Room`, and the set of currently enrolled student-role `PersonId` values.
An empty roster is valid, and several students can refer to the same lesson without duplicating its tutor, room or
schedule. Student records contain no copied `Lesson` values.

The constructor defensively copies the roster and rejects Tutor or Parent IDs in it; the assigned tutor must have a
Tutor ID. `getEnrolledStudentIds()` exposes an unmodifiable snapshot. `withEnrolledStudentIds`,
`withEnrolledStudent`, and `withoutEnrolledStudent` return replacement `Lesson` values, leaving the original lesson
and its stable identity unchanged. Equality, hashing, copying and string representation include every stored field,
including roster membership. The later canonical aggregate owns lesson ordering, ID allocation, clash checks,
persistence and duplicate-enrolment command feedback; this dormant record does not change the active runtime.

### Inline help

`HelpCommand` returns guidance in an ordinary `CommandResult`, with no separate-window or exit flag. It never mutates Model data, filters or preferences. `LogicManager` returns help before the inherited unconditional save path, so help works even when operational storage is unwritable and does not create or rewrite the data file. General change detection and rollback for other commands remain Vincent's integration work.

The Help menu and F1 invoke `MainWindow.executeCommand("help")`. Both display exactly the same guidance as typed help while retaining the command-box draft and person selection. `ResultDisplay` uses a read-only wrapped TextArea with scrolling and resets to the beginning of each new result. Existing F1 handling for focused text controls remains in place. The unused inherited HelpWindow is not constructed or reachable through supported help entry points.

Tests exercise each registered help topic and its example through the actual router, malformed topics, locale-independent matching, filter/data/preference preservation, and help with failing storage. GUI selection, menu/F1 and scroll behavior have a separate manual procedure below.

### Role-filtered people listing foundation

`PersonRecordListData` prepares a read-only snapshot for the future canonical people view.
It takes people in global creation order and an optional `PersonRole` filter, preserves the
relative order and stable IDs of matching records, and numbers the displayed entries from one.
For example, filtering `[T7, S4, P2, S9]` to students yields `(1, S4)` and `(2, S9)`.
These positions are view metadata; this component implements no command selection or ID allocation.

The input collection is copied, entries are immutable, and only the immutable `Student`, `Tutor`,
and `Parent` record types are accepted. Nulls and duplicate IDs are rejected across the entire
source snapshot, including records outside the selected role. A successful empty result has a
count of zero and an explicit `No persons to display.` message. The projection owns no writable
operational store and does not observe subsequent source-list changes. Integration must rebuild
the snapshot from the canonical people collection when data or the selected filter changes,
including after rollback. Each entry supplies the person and positive displayed position needed
by the separately prepared person card component.

`PeopleListParser` parses arguments excluding the command word. Empty arguments select all roles;
otherwise it accepts one lowercase `r/` prefix and a case-insensitive `student`, `tutor`, or
`parent` value. It rejects blank values, unsupported roles, repeated or unknown prefixes, preambles,
extra arguments, and line breaks with `ParseException`. It returns only an optional role and does
not create a command or change a filter.

**Integration boundary:** These components are dormant. The active catalogue still routes bare
`list` to the inherited list command and rejects `list r/student`. Full listing integration in
[#77](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/77) still requires the ordered registry and
canonical aggregate; [#85](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/85) coordinates enabling
the compatible people commands, protected loader, persistence and cards together. This increment
does not complete those issues or activate the proposed User Guide listing syntax.

Automated tests cover mixed-role order, filtered renumbering, stable identity, empty results,
defensive copies, invalid snapshots, parser errors, and the unchanged active command route.
At activation, manually run `list`, `list r/student`, `list r/TUTOR`, and `list r/parent` on a
mixed-role dataset. Confirm displayed positions, stable IDs, creation order, counts and an empty
role result. Run `list r/`, `list r/all`, and `list r/student r/parent`; expect actionable errors
without changing data or the visible list. Repeat after adding a person and after a failed save
rolls back. Those end-to-end checks remain pending runtime integration.

### Current people-index resolution foundation

[Issue #137](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/137) prepares the shared
selection boundary for the canonical people commands.

`PersonIndexResolver.resolve(Index, List<? extends PersonRecord>)` resolves a parsed index against
the supplied current filtered people list and returns the selected immutable record. For a view
containing `[T7, S9, P2]`, index `2` selects `S9`. In a student-filtered view `[S9, S3]`, index `2`
selects `S3`. The resolver does not interpret a position as an ID suffix or select from another
collection. An empty view or out-of-range index produces a checked `CommandException` using the
existing invalid-person-index message. Argument parsing remains the existing `ParserUtil.parseIndex`
responsibility; this helper receives an `Index`, not a command string.

`resolveStudent` resolves the same current people index once, then rejects a selected Tutor or
Parent with specific checked feedback. It never searches a separate student-only list. Callers
must supply the authoritative people view at execution, keep it unchanged during resolution,
and capture the returned record's stable `PersonId` before any Model or query calls. Later view
refreshes must not reinterpret that command's index. The resolver neither owns nor modifies the
view, its filter, records, registry, identity counters, or storage.

**Integration boundary:** This working helper remains dormant alongside the prepared listing and
cards. Canonical commands must share one people-view source at the coordinated #77/#85 cutover.
Guarded deletion under #91 still needs canonical lesson/enrolment/retained-attendance queries,
compatible persistence and save-failure rollback; selecting a record does not establish that it
can be deleted. Existing command routing and the User Guide's active-command status are unchanged.

Tests cover mixed roles, filtered positions that differ from stable-ID suffixes, checked range
and role errors, and retaining the resolved identity after a view refresh. The existing manual
person-index procedure below remains pending actual canonical command integration.

### Role-aware addition parsing foundation

[Issue #135](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/135) prepares argument parsing for [#85](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/85). `RoleAwareAddParser.parse(String)` accepts arguments after the future `add` command word and returns immutable `PersonAdditionInput`. The input contains a role, composed contact details, and optional student education level and parent phone. It contains no ID, person record, registry, command execution or writable operational state.

| Role | Required prefixes | Optional prefixes |
| --- | --- | --- |
| Student | `r/`, `n/`, `l/`, `pp/` | `p/`, `e/`, `a/` |
| Tutor or parent | `r/`, `n/`, `p/` | `e/`, `a/` |

Prefixes are lowercase and may appear once in any order. Role and education-level values are case-insensitive. Omitted optional contacts remain absent; supplied values must be nonblank. Prefix boundaries and surrounding value spaces recognize Unicode whitespace; valid interior field text is preserved. Recognizing Unicode field separators does not expand the permitted characters inside contact values; those still follow the shared validators. Every prefix-shaped token is recognized, so an unknown prefix after an address cannot silently become address text. Repeated prefixes, student-only fields on other roles, preambles, line breaks and control characters produce specific `ParseException` feedback. Controls are rejected before trimming, and phone values retain leading zeros. Both own and parent phones use the existing 3–15-digit role-record bound.

**Contact-validation dependency:** Name, email and address validation delegates to the shared value types. Current master still has inherited contact rules; this parser does not duplicate or deliver the complete planned User Guide policy. [#61 / PR #110](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/110) supplies that policy and remains gated by protected loading. Keep this parsing increment dormant and draft while that dependency remains pending.

**Activation boundary:** The existing `AddCommandParser`, router and supported-command catalogue still handle inherited contacts. The prepared parser does not check duplicate people, allocate even a provisional ID, reset the view, save data or perform rollback. Those behaviors remain #85 work after the canonical aggregate, compatible persistence, protected loading and transaction foundations are ready. At cutover, construct and commit the record through that single canonical root; never use a placeholder ID or a second writable person store.

Automated checks exercise all three roles, absent/present optional contacts, argument order/case, role restrictions, missing/blank/repeated/unknown fields, controls, phone bounds, immutable input invariants and unchanged active routing. Canonical duplicate detection, siblings sharing parent phones, committed ID allocation, save/reload and failed-save restoration remain integration tests under #85.

### Prepared person record cards

[Issue #119](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/119) implements the independent card portion of [#77](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/77). `PersonRecordCard` renders the already merged immutable `Student`, `Tutor`, and `Parent` records. It remains dormant: `MainWindow`, the active list route, and the inherited `PersonCard` still use the existing runtime. Role-filtered listing, counts, aggregate wiring, and the coordinated cutover remain later work under #77 and #85.

`PersonRecordCardData(PersonRecord, int)` creates immutable display text with a positive current-view position. The heading shows that position and the supplied name; a separate identity line shows the role and stable ID. Student details include education level and required parent phone, followed by optional own phone, email, and address. Tutor and parent details include required own phone plus optional email and address. Missing optional fields display `Not provided`. The projection preserves display case, spacing, and phone zeros, accepts only the known record types, and mutates neither the record nor a registry.

`PersonRecordCard(PersonRecord, int)` loads `PersonRecordCard.fxml` and renders the projection. The heading, identity, and detail labels wrap with no fixed card height. The future list-cell host must size the card to the available cell width and provide vertical scrolling; `PersonRecordCardPreview` demonstrates that host with the current theme. Stable IDs never become displayed indices, and no second writable person store is introduced.

Automated projection tests cover all roles, absent/present contacts, required fields, exact display values, long text, index/ID separation, immutable detail lines, and invalid inputs. Renderer regression tests load the actual FXML on the JavaFX application thread and check the displayed values and wrapping. Linux CI runs the Gradle checks under a virtual display; developers on Linux without a display can likewise run `xvfb-run --auto-servernum ./gradlew check coverage` with Xvfb installed. The developer preview exercises the actual FXML separately from the application and uses fixture records without loading or saving operational files. Its layout procedure appears below.

### Representative canonical people samples

[Issue #143](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/143) prepares the people portion of
the acceptance dataset for [#101](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/101).
`PeopleSampleDataUtil.getSamplePeopleRegistry()` returns a fresh `PeopleRegistry` on each call.
The factory uses the registry's addition APIs, enforcing role-specific duplicate rules and
retaining global creation order and complete allocation state. Callers can mutate one sample registry independently of
every other invocation.

The six records have deterministic creation order `T1, S1, P1, S2, T2, S3`:

| IDs | Representative scenario |
| --- | --- |
| `S1`, `S2`, `P1` | Alex Tan and Jamie Tan share parent phone `00987654`, matching Pat Tan's own phone. Alex omits all optional contacts; Jamie supplies them. |
| `T1`, `T2` | Both tutors are named Mei Lim, with distinct phones `00112233` and `00999888` for future name/phone disambiguation checks. `T2` omits email and address. |
| `S1`, `S3` | Two students named Alex Tan have different parent phones, making both valid distinct records. `S3` uses `00888888`. |

Phone zeros and email display case are retained. The interleaved order exercises current-view
positions separately from stable IDs: all-role position `5` selects `T2`, while tutor-filtered
position `2` selects that same record. The factory is dormant and leaves active AB3 sample
loading unchanged. It supplies no lessons, enrolments, attendance or persistence format;
those parts of #101 remain pending their feature owners' integration.

Sample tests validate independent registries, required and optional contacts, the name/phone
cases, registry export/import and continued allocation after removal. They also compose
`PeopleListParser`, `PersonRecordListData`, `PersonIndexResolver` and `PersonRecordCardData`
using the samples, including filtered renumbering after removal. These checks exercise the
prepared APIs; actual canonical command, save/reload and GUI acceptance remain pending.

### Search criteria parsing foundation

Issue #64 adds an independently testable search specification. `SearchCriteriaParser.parse(String)` consumes the
arguments after a future `search` command word and returns immutable `SearchCriteria`. It is a criteria parser rather
than an implementation of `Parser<T extends Command>`, because this increment produces a value object instead of a
command. Runtime routing and help topics will be connected with the later search commands.

`SearchCategory`, `SearchField` and `SearchCriteria` are in `seedu.address.model.search`. The specification has no model,
UI, storage or lesson-record dependency. `SearchField` owns the prefix/category matrix and validation rules;
`SearchCriteria` defensively copies its filters, exposes an unmodifiable map and supplies field-level matching.

#### Supported criteria

The required category is `c/student`, `c/parent`, `c/tutor` or `c/lesson`, ignoring category-value case.
Category-only input is valid and imposes no field restrictions. Prefixes are lowercase, may occur in any order and
may appear only once, including `c/`. This matrix implements the earlier search specification tracked in [issue #64](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/64):

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
| `s/` | Lesson subject | Yes | Yes | Yes | Yes | Partial text |
| `tu/` | Lesson tutor's name | Yes | Yes | — | Yes | Partial text |
| `rm/` | Lesson room | Yes | Yes | Yes | Yes | Partial text |

Text queries are stripped of surrounding whitespace and stored in lowercase using `Locale.ROOT`. Matching uses
case-insensitive substring containment, so `n/al` can match `Sally Tan`, `s/Math` can match `Add Math`, and
`e/@example` can match `mei@example.com`. Text fragments are accepted without requiring a complete valid name,
email address or lesson field. `tu/` remains a partial tutor-name query; tutor-category name searches use `n/`.

Phone filters require 3–15 ASCII digits, retain leading zeros and match the complete number. Levels accept only
`P1`–`P6`, `S1`–`S5`, `JC1` and `JC2`, normalized to uppercase. Weekdays accept only `Mon`, `Tue`, `Wed`, `Thu`, `Fri`,
`Sat` and `Sun`, normalized to lowercase. Times require four digits from `0000` to `2359`, with valid minutes.
Start or end may be supplied independently; when both are present, end must be later than start on the same day.

`SearchCriteria.matches(SearchField, String)` checks one supplied condition. An omitted filter imposes no restriction;
an absent optional field fails a supplied filter. Exact-field comparisons use the same validation/normalization as
the queries. Query integration must combine all supplied filters and preserve the existing same-student/same-lesson
rules. This foundation validates relational criteria without traversing records or building a relationship graph.

#### Errors and integration boundary

The parser throws `ParseException` for missing/unknown categories, unexpected preamble, blank values, repeated
prefixes, unknown prefixes, category-incompatible filters, invalid exact values and non-increasing time ranges.
Slashes in values and line breaks in arguments are rejected. All prefix-shaped tokens at whitespace boundaries are
recognized, including unknown ones: `c/student n/Alex xyz/no` reports unsupported `xyz/` instead of absorbing it into
the name query. Horizontal whitespace can separate prefixes. Programmer-supplied null arguments raise
`NullPointerException`; direct construction of invalid criteria raises `IllegalArgumentException`.

The parser and criteria operate only on their inputs and perform no writes, ID allocation or filtering of live
lists. Tests cover every documented prefix/category combination, normalization, partial/exact matching, retained
phone zeros, day/level/time boundaries, unknown/blank/repeated fields, time ranges and criteria immutability.
The active router still rejects `search`. The full matrix above is the retained planned search contract, including
partial tutor-name queries and relationship filters. Own-field search can be activated as an earlier slice; later
relational slices must retain same-student/same-lesson conjunctions and distinct-ID results. For example, a parent
search combining `sn/Alex` and `s/Math` must find a matching lesson for that same linked Alex, rather than use one
child for the name and another for the subject. A student's combined day/subject filters must match one enrolled
lesson. Lesson results must count each shared lesson once and include empty lessons when the supplied filters allow
them. Feature owners update routing, help and guides for each activated slice without reducing the outstanding target.

### Person record foundation

The person record foundation provides immutable student, tutor, and parent records for subsequent person commands and shared-lesson integration.
The current command, UI, and JSON aggregate still use the inherited AB3 `Person` model; integrating the new types with these components is follow-up work.

| Type | Implemented contract |
| --- | --- |
| `PersonRole` | Represents `STUDENT`, `TUTOR`, or `PARENT`. |
| `PersonId` | Represents a stable role-prefixed identifier, such as `S1`, `T1`, or `P1`, with a positive `long` sequence number and no leading zeros. Constructor input is trimmed and case-normalized; this is an internal identity value, not a displayed command index. |
| `EducationLevel` | Accepts `P1`–`P6`, `S1`–`S5`, `JC1`, or `JC2`, normalizing trimmed input to uppercase. |
| `ContactDetails` | Composes a required `Name` with optional `Phone`, `Email`, and `Address` values. It reuses the existing contact value types and requires any supplied phone number to contain 3–15 digits. |
| `PersonRecord` | Provides common access to a record's stable ID, contact details, name, and role, and declares `isDuplicateOf(PersonRecord)` for role-specific duplicate matching. |
| `Student` | Composes a student-role `PersonId`, `ContactDetails`, `EducationLevel`, and required parent `Phone`. The parent phone must contain 3–15 digits; the student's own phone, email, and address remain optional. |
| `Tutor` | Composes a tutor-role `PersonId` and `ContactDetails`. Its own phone is required; email and address remain optional. |
| `Parent` | Composes a parent-role `PersonId` and `ContactDetails`. Its own phone is required; email and address remain optional. |

`Student`, `Tutor`, and `Parent` implement `PersonRecord` and compose immutable `ContactDetails`; none extends the inherited `Person` class. Each constructor rejects IDs for another role. `Tutor#getPhone()` and `Parent#getPhone()` expose their required own phone, which follows the same 3–15 digit limit as other supplied phone values.

**Validation boundary:** `Name`, `Email`, and `Address` still enforce their inherited AB3 validation rules. This foundation does not yet implement the User Guide's wider name punctuation and field-format or length rules; [issue #61](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/61) tracks that alignment. Contact details preserve the supplied display values; name normalization is used only for duplicate matching.

**Current email validation:** The active contact parser and JSON loader share `Email.isValidEmail`, which scans
ASCII characters in linear time with constant stack space. Long repeated local-part separators, domain hyphens,
or domain labels therefore produce a value or the existing checked boundary error rather than a regex stack
overflow. Its acceptance rules remain unchanged: single-label domains and long emails still load, the local part
allows single `+`, `_`, `.`, or `-` separators, and domain labels allow single hyphens. The inherited final-label
regex requires an adjacent alphanumeric pair, so `a@a-b` is rejected while `a@a-bc` is accepted. The public
`VALIDATION_REGEX` remains the compatibility reference; bounded exhaustive regressions compare it with the scan.
The complete contact-policy alignment in #61/#110 still requires protected loading in #84 before activation;
this safety correction does not impose new field limits or protect the startup fallback.

**Identity, equality, and duplicates:** The stable `PersonId` identifies a record independently of its contact details or position in a displayed list. Value equality compares the ID and all stored fields, including optional contact details and the student's education level and parent phone. `isDuplicateOf(PersonRecord)` instead requires the same role and compares the name, ignoring case and repeated spaces, together with the required identifying phone: the parent phone for students or the record's own phone for tutors and parents. Different IDs, optional contact details, or student education levels do not distinguish otherwise duplicate records. A null or different-role argument is not a duplicate. `PeopleRegistry` now uses this operation to reject duplicate records; connecting that rejection to active commands remains follow-up work.

**Command boundary:** A user-entered person index is resolved once through the current filtered people view to a `PersonId`; it is not passed into the `PersonId` constructor or persisted as a relationship. Internal lookup, student-lesson queries, rosters, attendance keys and storage continue to use stable IDs. Resolving an index and validating its role belongs to the planned command integration, not these immutable record constructors.

**Planned relationships:** The person records do not contain lesson or attendance collections. The planned shared-lesson model will keep canonical lessons and student–lesson membership outside the student record, referring to stable IDs so several students can share one lesson. Separate parent records remain optional; future parent links will use exact equality between a parent's own phone and a student's stored parent phone, without requiring a stored `Parent` object in `Student`. Relationship lookup, dated attendance, and the storage of these relationships are separate follow-up work.

### Ordered people registry foundation

[Issue #70](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/70) adds `PeopleRegistry` and immutable `PeopleRegistryState` for subsequent canonical-model, command, and storage integration. The registry contains the existing immutable `Student`, `Tutor`, and `Parent` records in one global addition order. A stable `PersonId` identifies a record independently of its position in that order. The registry is dormant: active commands, cards, and saved AB3 data continue to use the inherited `Person` runtime.

#### Registry APIs

| API | Contract |
| --- | --- |
| `new PeopleRegistry()` | Creates an empty registry with zero allocated sequence numbers for every role. |
| `new PeopleRegistry(PeopleRegistry)` | Copies records, their order, and all allocation state without sharing mutable collection state. |
| `new PeopleRegistry(PeopleRegistryState)` | Imports a validated snapshot, retaining its record order and allocation state. |
| `addStudent(ContactDetails, EducationLevel, Phone)` | Validates and adds a student using the required parent phone, returning the allocated `Student`. |
| `addTutor(ContactDetails)` | Validates and adds a tutor with a required own phone, returning the allocated `Tutor`. |
| `addParent(ContactDetails)` | Validates and adds a parent with a required own phone, returning the allocated `Parent`. |
| `getPeople()` | Returns an immutable `List<PersonRecord>` snapshot in global addition order. Later registry changes do not alter an earlier snapshot. |
| `getPerson(PersonId)` | Returns an `Optional<PersonRecord>` for the exact stable ID. |
| `remove(PersonId, Predicate<PersonRecord> isReferenced)` | Consults the supplied relationship view before deleting the selected record. |
| `exportState()` | Returns immutable `PeopleRegistryState` containing the ordered records and complete allocation map. |

Each successful addition appends to the same list, regardless of role. Duplicate IDs and same-role business duplicates are rejected with `DuplicatePersonException`; the record types supply their established `isDuplicateOf` rules. A failed addition leaves records and allocation state unchanged.

#### Allocation and state validation

The complete `Map<PersonRole, Long> lastAllocatedSequences` stores the last successfully allocated sequence for `STUDENT`, `TUTOR`, and `PARENT`. Zero means that no ID has been allocated for that role. New IDs use positive sequences independently per role: adding `S1`, then `T1`, then `S2` leaves the global order `[S1, T1, S2]` and allocation values `STUDENT=2`, `TUTOR=1`, `PARENT=0`.

Deletion never reduces an allocation value. For example, removing `S2` from that registry keeps `STUDENT=2`, so the next successful student addition receives `S3`. A value of `Long.MAX_VALUE` marks an exhausted role; another addition for that role is rejected before incrementing, with no overflow or change to registry state. Allocation values must be preserved even when every record of a role has been deleted.

`PeopleRegistryState` defensively copies its ordered records and complete allocation map. It accepts only the known immutable `Student`, `Tutor`, and `Parent` implementations of `PersonRecord`, rejects duplicate IDs or business identities, and requires every allocation value to be nonnegative and at least as large as each retained ID sequence for that role. Missing role entries and invalid state are rejected. Import does not reconstruct counters from the remaining records. Copying, exporting, importing, and equality preserve both people order and allocation values, including exhausted or previously deleted sequences.

The logical persistence fields are:

| Field | Value to preserve |
| --- | --- |
| `people` | All role-specific records in global addition order, including their stable IDs. |
| `lastAllocatedSequences` | A complete map for all three roles, including zero, deleted-record, and exhausted allocation values. |

These are state contracts for [issue #79](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/79); this increment adds no JSON codec or schema change.

#### Deletion and activation boundaries

`remove` obtains the selected record and asks the caller's `isReferenced` predicate whether the canonical relationship view references it. A referenced record raises `ReferencedPersonException`; an unknown ID raises `PersonNotFoundException`. Neither rejection changes records, order, or allocation values. The predicate must not mutate the registry. The registry supplies this integration point without implementing lesson or attendance guards itself; later canonical-model work must pass the complete relationship check rather than maintaining a second writable store.

The dormant registry can support subsequent aggregate and view foundations without replacing the active runtime. Contact validation in [issue #61](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/61) and protected loading in [issue #84](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/84) remain gates before the coordinated person-command activation in [issue #85](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/85). Existing command selectors and shared-lesson/search contracts are unchanged by this foundation.

### Dated attendance records

`Attendance` is a separate immutable association record; it is not nested in a Student or Lesson. Its unique
`AttendanceKey` combines one student-role `PersonId`, one `LessonId`, and one `LocalDate`. The remaining stored field
is `AttendanceStatus`, whose only recorded values are `present` and `absent`. A missing key means unrecorded, so no
`unrecorded` status is stored. Current enrolment also remains outside Attendance, allowing dated history to survive
later unenrolment.

External dates are parsed using strict ASCII `YYYY-MM-DD` syntax and calendar validation, including real leap years.
Status parsing is case-insensitive. Wrong-role person IDs, impossible dates and unknown statuses are rejected.
`withStatus` returns a replacement record for the same key and leaves the original unchanged. Equality and hashing
include all three key components and the status, which lets the later canonical aggregate keep one record per key and
replace its value when correcting attendance. This foundation adds no active command, collection or persistence route.

### Future undo/redo and archiving

Undo/redo and student archiving are future extensions. The inherited AB3 undo diagrams and `VersionedAddressBook` example do not describe an implemented PonHub mechanism. No undo, redo, archive or restore route is registered in the current command catalogue.

Failed-command rollback is part of safe canonical integration and is separate from user-requested undo. A future undo or archive design must preserve shared-lesson and attendance references, stable identity and the rule against reusing committed IDs. Feature owners will document its actual commands and persistence behavior if that extension is implemented.

--------------------------------------------------------------------------------------------------------------------

## **Documentation, logging, testing, dev-ops**

* [Documentation guide](Documentation.html)
* [Testing guide](Testing.html)
* [Logging guide](Logging.html)
* [DevOps guide](DevOps.html)

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Requirements**

### Product scope

**Target user profile**: Tuition centre administrative staff who maintain student, tutor and parent records and coordinate lessons and attendance.

* **Usage context**: During enrolment, timetable planning and daily lesson administration, staff need to find contacts, allocate tutors and rooms, record attendance, and follow up on absences and lesson changes.
* **User characteristics**: Staff understand tuition operations and work repeatedly with names, contact details, student levels and lesson schedules. They need quick record retrieval, clear feedback on invalid changes, and command examples while learning the application.
* **Current problems**: Keeping contact details, schedules and attendance consistent requires repeated checking. Staff need to avoid tutor or room clashes, find the right parent contact promptly, and identify missed lessons that need follow-up.

**Value proposition**: For tuition centre administrative staff who need to keep people, lessons and attendance organised, PonHub provides a single place to manage and search these records, with scheduling checks and attendance tracking to support reliable daily administration.

Compared with maintaining separate contact lists, timetables and attendance records, PonHub connects the information needed for routine tasks and checks conflicting or invalid changes. Planned requirements such as make-up booking extend this support to absence follow-up.

**Shared-lesson MVP target:** Lessons are created independently of student enrolment, including lessons with empty rosters. Each shared `Lesson` owns its currently enrolled student IDs; student lessons, rosters, and tutor schedules are retrieved from that canonical membership rather than copied into each student. Attendance is a separate dated record for one student and one lesson. Unenrolment removes current membership and retains attendance history; an existing historical entry can still be corrected or removed. A missing attendance entry means unrecorded, distinct from present and absent.

**Delivered versus planned:** The [student foundation (#57)](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/57), [tutor and parent foundations (#62)](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/62), [ordered people registry (#70)](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/70), [person-card components (#119)](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/119), [role-filtered listing components (#122)](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/122), [search criteria foundation (#64)](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/64), and [lesson scheduling values (#63)](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/63) provide independently tested APIs. The registry, new cards, and role-filtered listing remain dormant. [Inline help (#68)](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/68) and [safe file replacement (#66)](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/66) are connected to the active inherited contact runtime.

Canonical people and shared-lesson runtime integration remain tracked in [#76](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/76), [#77](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/77), [#85](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/85), and their dependent command, storage, and retrieval issues. Compatible persistence, protected loading, and failed-save rollback remain activation gates. The requirements below describe the intended product; they do not claim that role-aware commands, shared lessons, enrolment, attendance, or history are currently available. Each feature owner updates implementation evidence and feature documentation as those increments are delivered.

### User stories

These stories describe the broader shared-lesson target and future ideas; they do not imply completed functionality or require every High-priority story to be delivered in Week 8.

Priorities: **High** (core shared-lesson target), **Medium** (future extensions), **Low** (future consideration).
The target includes shared rosters and regular enrolment. Capacity, make-up reservations and waiting lists are future work, as are export, saved searches, archive/restore, richer attendance and percentages, fees and guided workflows. Medium and Low priorities remain proposals for later iterations.

Shared lessons, current membership, and retained dated attendance are separate concepts in these stories. The stories specify user goals; command selectors and the complete planned search-filter matrix are confirmed in the [shared-lesson target contract](#shared-lesson-target-contract) and [#60](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/60). Implementation proceeds in working increments, and only the [active command routes](#current-increment-and-command-integration) are available today.

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
| US-37 | Medium | tuition centre administrator | edit an existing lesson directly | update its details without deleting and recreating it. |
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

### Use cases

For the use cases below, the **System** is `PonHub` and the **Actor** is the tuition centre administrator.
The person, shared-lesson, attendance, search, and help operations accept all required details in one keyboard-entered command. These are target use cases; only the routes listed under [Current increment and command integration](#current-increment-and-command-integration) are active today. Canonical mutations validate and save the complete change without requiring a field prompt, preview, or separate confirmation. Read-only retrieval does not save operational data. Guided previews, capacity, make-ups and other explicitly labeled future extensions are outside the core flows.

Person selectors in these target use cases are positive indices from the current filtered people list. Resolve the selected record once to its stable ID and enforce any required Student role; another result view does not supply that index. Existing shared lessons are selected by `lid/LESSON_ID`. Creating a lesson uses a normalized full tutor name and optional exact phone; search uses partial tutor-name text.

#### UC-01: Add a person record

**Related user stories:** US-01, US-02, US-03, US-14, US-15, US-30, US-34

**Preconditions:** PonHub is running. The administrator has the person's role and required contact details.

**Main success scenario**

1. The administrator enters one `add` command containing the selected role and all required person details.
2. PonHub validates the command, normalizes the details, and checks for an exact duplicate.
3. PonHub saves one new person record, refreshes the person list, and reports success.

**Extensions**

* 1a. A required field is missing, invalid, repeated, or unsupported: PonHub explains the problem without changing data. The administrator may correct and resubmit the command at step 1.
* 2a. An exact duplicate exists: PonHub rejects the addition without changing data, and the use case ends.
* 2b. The administrator explicitly requests a planned guided preview: PonHub shows the normalized record and any possible duplicate matches before saving. The administrator cancels with no change, or confirms and resumes at step 3.
* 3a. Saving fails: PonHub rolls back the addition, reports the failure, and the use case ends.

**Postconditions:** On success, one valid, non-duplicate person record is stored and visible. Existing records are unchanged.

#### UC-02: Find and review operational records

**Related user stories:** US-04, US-05, US-06, US-16, US-17

**Preconditions:** PonHub is running with the relevant canonical records and retrieval routes integrated. Available search filters are documented for the delivered slice.

**Main success scenario**

1. The administrator enters `list`, `lessons`, or one `search c/CATEGORY` command with any available filters needed.
2. PonHub validates the request and retrieves the relevant records, combining relational filters according to the same-student/same-lesson rules.
3. PonHub displays each distinct match once in the documented order, with its count and relevant relationship context. People results replace the current people filter and are numbered from one; lesson results preserve that people view.
4. To review a shared lesson, the administrator reads its stable ID from the results and enters `showlesson lid/LESSON_ID`, optionally with `d/YYYY-MM-DD` for dated attendance.
5. PonHub displays the current roster and requested dated attendance without changing operational data or the people list. For a date, missing attendance is shown as unrecorded, separately from present or absent.

**Extensions**

* 1a. The administrator runs `lessons si/STUDENT_INDEX`: PonHub resolves the current people-list index once to a Student ID and shows that student's current lessons. An invalid index or wrong role is rejected without altering records or the people view.
* 2a. A condition is invalid, unavailable in this slice or incompatible with the category: PonHub explains the supported syntax without changing data. The administrator may resubmit at step 1.
* 3a. No record matches: PonHub displays a successful empty result, and the use case ends. Unfiltered catalogue retrieval includes shared lessons with empty rosters.
* 4a. The lesson does not exist or the date is invalid: PonHub reports the error without changing data or the people list.

**Postconditions:** The requested read-only view is displayed. Operational data is unchanged; lesson and roster results preserve the current people filter, order and indices.

**Future extensions:** Sorting/grouping controls, guided search, spelling suggestions, export and saved searches belong to US-22, US-29 and US-38 to US-41. They are not steps required by this core flow and have no promised command syntax here.

#### UC-03: Create or delete an independent shared lesson

**Related user stories:** US-08, US-09, US-10, US-20

**Preconditions:** The tutor exists. PonHub has loaded the shared lesson catalogue and tutor/room bookings; compatible mutation and persistence support is integrated. Creating a lesson does not require a student.

**Main success scenario**

1. The administrator enters `addlesson d/DAY st/HHMM et/HHMM s/SUBJECT tu/TUTOR_NAME [tp/TUTOR_PHONE] rm/ROOM`, supplying a full tutor name and an optional exact phone number.
2. PonHub resolves exactly one tutor by normalized full name and, when supplied, exact phone. It validates the remaining values, same-day time range and proposed slot against that tutor's bookings and room bookings.
3. PonHub saves one independent shared Lesson linked to the tutor's stable ID, allocates its stable Lesson ID, starts an empty roster, refreshes relevant views and reports success.

**Extensions**

* 1a. The administrator instead enters `deletelesson lid/LESSON_ID`. PonHub finds that shared lesson and checks both its current roster and retained attendance references. If neither exists, it removes and saves only the lesson, releases its tutor/room booking and reports success. Surviving lesson IDs do not change; the use case ends.
* 1a1. The lesson has enrolled students or retained attendance: PonHub blocks deletion, identifies the dependency and changes nothing. It does not cascade into student or attendance deletion.
* 2a. A value is missing or invalid, or no tutor matches the full name and optional phone: PonHub explains the problem without changing the schedule. A supplied phone is never ignored to fall back to a name-only match.
* 2b. The tutor or room has an overlapping booking on that weekday: PonHub rejects the addition and identifies the conflict. Adjacent half-open intervals are allowed.
* 2c. Several tutors match the name without a disambiguating phone: PonHub rejects the command and requests `tp/TUTOR_PHONE`. The administrator checks the intended tutor's phone and resubmits the complete command. No student selector is part of lesson creation.
* 3a. Saving an addition or deletion fails: PonHub restores the complete prior data and active views, reports the failure and ends the use case.

**Postconditions:** One clash-free shared lesson with an empty roster is stored, or one unreferenced lesson is removed. Adding a student later does not recreate the lesson or book its tutor/room again. Failed operations preserve the prior schedule and allocation state.

**Future extensions:** Lesson templates, edits, replacement tutors, one-occurrence cancellation and automatic today views are separate later requirements (US-27, US-28, US-36, US-37 and US-47).

#### UC-04: Enrol or unenrol a student in a shared lesson

**Related user stories:** US-17, US-18

**Preconditions:** The Student and shared Lesson exist. The administrator has listed or searched people and checked the intended student's current position. Canonical membership mutation and persistence support is integrated.

**Main success scenario**

1. The administrator enters `enrol STUDENT_INDEX lid/LESSON_ID` using the student's current index in the displayed filtered people list and the lesson's stable ID.
2. PonHub resolves the index once to a Student ID, validates the role and lesson, rejects duplicate membership and checks for overlap with that student's other enrolled lessons.
3. PonHub adds that Student ID to the shared lesson's roster, saves the complete change and refreshes derived student lessons and roster results without replacing the people list.

**Extensions**

* 1a. The administrator instead enters `unenrol STUDENT_INDEX lid/LESSON_ID`. PonHub resolves the Student once, requires current membership, removes only that membership and saves it. Dated attendance remains available; the shared lesson and student remain stored.
* 2a. The people-list index is out of range, selects another role or the lesson does not exist: PonHub explains the problem and changes nothing.
* 2b. Enrolment duplicates current membership or conflicts with the student's timetable, or unenrolment targets a non-member: PonHub rejects the change without modifying data.
* 3a. Saving fails: PonHub restores the previous aggregate and active views, reports the failure and ends the use case.

**Postconditions:** Current membership reflects the successful command; dated attendance and stable identities are unchanged. Tutor and room bookings are still owned by the one shared lesson and are not repeated per student. Failed operations preserve prior data and views.

**Future extensions:** Capacity limits, remaining places, waiting lists, swaps and one-off make-up reservations are outside this core membership flow (US-19, US-24, US-25 and US-48).

#### UC-05: Record, correct or retrieve dated attendance

**Related user stories:** US-11, US-12, US-14, US-21

**Preconditions:** The Student and shared Lesson exist. The administrator has listed or searched people and checked the student's current people-list position. Canonical attendance and persistence support is integrated.

**Main success scenario**

1. The administrator enters `mark STUDENT_INDEX lid/LESSON_ID d/YYYY-MM-DD s/STATUS`, using `present` or `absent` and the student's current filtered people-list index.
2. PonHub resolves the index once to a Student ID, checks the role and validates the lesson, real date, weekday and status. A new attendance key requires current enrolment; an existing key can be corrected after unenrolment.
3. PonHub creates or updates the one record keyed by Student ID, Lesson ID and date, saves the complete change, refreshes dated attendance results and reports success. A missing record is unrecorded, not absent.

**Extensions**

* 1a. The administrator instead enters `unmark STUDENT_INDEX lid/LESSON_ID d/YYYY-MM-DD`. PonHub resolves and validates the Student, lesson and date, removes only the existing dated record and saves the change. Current enrolment and the shared lesson are unchanged. Existing historical attendance can be unmarked after unenrolment.
* 1a1. No entry exists for the specified key: PonHub reports that there is nothing to unmark and changes nothing.
* 1b. The administrator enters `history STUDENT_INDEX [lid/LESSON_ID]`. PonHub resolves the Student once and displays their retained dated attendance, optionally filtered by the shared lesson. History includes former enrolments and preserves the people list. This read-only request does not save operational data.
* 2a. The index, role, lesson, date, weekday or status is invalid, or a new mark has no current membership: PonHub rejects the command and explains the problem without changing data.
* 3a. The same status already exists: PonHub reports no change and performs no operational save.
* 3b. A different status exists: PonHub corrects that key instead of adding a duplicate, including after unenrolment.
* 3c. Saving a mark or unmark fails: PonHub restores the previous aggregate and active views, reports the failure and ends the use case.

**Postconditions:** A successful mutation stores one authoritative status for the key or removes only that key. History remains independent of current membership. Failed operations preserve attendance and views; remaining attendance references still block student and lesson deletion.

**Future extensions:** Feedback, late/excused statuses, notes, bulk/clickable attendance, percentages, make-up links and fee adjustments are not core attendance promises (US-26, US-43 to US-46, US-48 and US-49).

#### UC-06: Delete an unreferenced person

**Related user stories:** US-07

**Preconditions:** The target person exists. The administrator has listed or searched people and checked their current position; canonical guarded deletion and persistence support is integrated.

**Main success scenario**

1. The administrator enters `delete INDEX` for the target in the current filtered people list.
2. PonHub validates the positive index, resolves it once to the stable person ID and checks role-specific dependencies: Student membership or attendance, or Lessons assigned to a Tutor. Deleting a separate Parent does not remove a Student's stored parent phone.
3. PonHub deletes and saves only the eligible person record, preserves the current filter, refreshes displayed positions and reports success. Surviving stable IDs and relationships remain unchanged.

**Extensions**

* 2a. The index is invalid or dependencies prevent deletion: PonHub identifies the problem and changes nothing. Linked records are never deleted automatically.
* 3a. Saving fails: PonHub restores the prior aggregate and people view, reports the failure and ends the use case.

**Postconditions:** One eligible record is deleted without broken references, or the previous state remains unchanged. Recheck displayed positions before selecting another person.

**Future extensions:** Optional previews, student archiving/restoration and undo are separate later requirements (US-23, US-31 and US-35), rather than supported alternatives to this command.

#### UC-07: Get help and recover from a command-entry error

**Related user stories:** US-13, US-32, US-33, US-42

**Preconditions:** PonHub is running and the command interface is available.

**Main success scenario**

1. The administrator enters `help` or `help COMMAND` in one command.
2. PonHub displays the command catalogue or the selected command's syntax and examples.
3. The administrator enters a complete command using the guidance.
4. PonHub validates and executes the command, then displays its result.

**Extensions**

* 2a. The administrator needs only the help catalogue: PonHub leaves the current records and selection unchanged, and the use case ends.
* 2b. Guidance exceeds the Result Display: the administrator scrolls the wrapped text and resumes at step 2.
* 2c. The administrator chooses Help from the menu or presses F1: PonHub executes the same overview help, preserving the command-box draft, filter and person selection.
* 3a. The help topic is unknown or unavailable: PonHub reports the topic and instructs the administrator to type `help` for available commands.
* 3d. More than one help topic is supplied: PonHub reports `Invalid command format!` followed by the help usage. No topic is executed.
* 3b. Required parameters are missing or invalid: PonHub shows guidance and an example without changing data. The administrator may correct and resubmit at step 3.
* 3c. The administrator uses a planned shortcut, completion, or alternative help form: PonHub maps it to the corresponding command and resumes at step 3.
* 4a. The chosen command fails for a domain-specific reason: PonHub reports the error, changes no data, and the use case ends.

**Postconditions:** The requested guidance remains visible, or the selected command has completed with clear feedback.

### Non-Functional Requirements

1.  Should work on Windows, Linux and macOS using Java `25`. On macOS, use the course-prescribed Azul JDK 25 with JavaFX (`25.0.3.fx-zulu`), as described in [Setting up and getting started](SettingUp.html#setting-up-the-project-on-your-computer).
2.  Should be able to hold up to 1,000 persons, 1,000 recurring lessons, and 10,000 attendance records without noticeable sluggishness during typical usage.
3.  Team performance target: common operations should update the GUI within 2 seconds on a reference machine recorded with its hardware, OS, Java version and test dataset. The reference machine and measured results are still to be documented; this guide does not claim the target has been verified.
3.  A user with above average typing speed for regular English text (i.e. not code or system administration commands) should be able to accomplish most recurring tasks faster using commands than using the mouse.
4.  The product should be for a single user and use one local data store, without requiring user accounts, concurrent access, or live synchronisation.
5.  The product should be packaged into a single executable `.jar` file and should not require an installer.
6.  The product file size should remain below 100 MB to ensure efficient storage and distribution.
7.  The GUI should display correctly and without layout issues on screen resolutions of 1920×1080 and higher at 100% and 125% scale.
8.  The GUI should remain usable (i.e. all functions remain accessible even if the layout is suboptimal) at screen resolutions of 1280×720 and higher at 150% scale.
9.  All operational data must be stored locally in a human-readable text file without using a database management system.
10. All successful data changes must be saved automatically and reliably. Failed validation or saving must leave both the in-memory data and stored data file unchanged.
11. All person, lesson, and attendance data must remain on the user's computer and must never be transmitted over the internet.
12. The application should function offline without requiring an internet connection or a team-owned remote server.
13. Invalid inputs must not crash the application or modify existing data. The application should display a specific error message explaining how the user can correct the input.
14. The application should be implemented primarily using object-oriented programming, with clear separation between the UI, Logic, Model, and Storage components.
15. Team testing target: the JUnit-based automated test suite should achieve at least 70% line coverage, and all automated tests should pass before a release. The percentage is the team's own goal, not a course-mandated minimum; coverage does not replace behavior-focused regression and integration checks.

### Glossary

* **Mainstream OS**: Windows, Linux, Unix, or macOS
* **AddressBook-Level3 (AB3)**: The upstream SE-EDU desktop application that is incrementally evolved into PonHub
* **Person record**: A stored record representing one student, tutor, or parent and containing the fields required for that role
* **Role**: The `student`, `tutor`, or `parent` category assigned to a person record, which determines its required fields and applicable operations
* **Shared recurring lesson**: An independent weekly teaching slot with a stable Lesson ID, day, start/end time, subject, tutor and room; its roster may contain zero or more Student IDs
* **Lesson occurrence**: One dated instance of a recurring lesson
* **Attendance record**: A stored record containing the attendance status of one student for one lesson occurrence
* **Attendance status**: The recorded state of a student for a lesson occurrence, such as `present` or `absent`
* **Attendance key**: The combination of student, lesson, and date that uniquely identifies one attendance record
* **Exact duplicate person**: A person record with the same role, normalised name, and identifying contact number as an existing record
* **Displayed person index**: The positive one-based position in the current filtered people list used to select a person in a command; it is distinct from a stable ID and may change when that people view is filtered or reordered. Lesson, roster and history row positions do not supply person command indices
* **Stable person ID**: The role-prefixed identity stored for a person and used in internal lookup, queries, relationships and persistence; commands resolve a displayed person index once to this identity
* **Stable lesson ID**: The `L`-prefixed identity of a shared Lesson, persisted and used in attendance references; existing lessons are selected in commands with `lid/LESSON_ID`, never a per-student lesson index
* **Tutor lookup**: Lesson creation resolves a normalized full tutor name and optional exact phone to one Tutor ID; search uses partial tutor-name text instead
* **Prefix**: A short marker in a command that identifies the type of information represented by the following value
* **Parser**: The Logic component that converts raw command text into validated parameters and a command object
* **Command**: An executable request representing one user operation in PonHub
* **Referential integrity**: The rule that prevents a record from being removed while another lesson or attendance record still refers to it
* **Atomic update**: An all-or-nothing change where either the complete operation is saved successfully or none of it is applied
* **Rollback**: The restoration of the previous valid state after an operation cannot be completed or saved
* **Human-readable data file**: The local text file used to store PonHub data in a form that can be inspected and edited without a database management system
* **Tutor clash**: An overlap between lessons assigned to the same tutor on the same day and during an overlapping time range
* **Room clash**: An overlap between lessons assigned to the same room on the same day and during an overlapping time range
* **Class**: A tuition-centre term for a shared recurring lesson; core membership uses the Lesson-owned roster, with capacity and one-off reservations deferred
* **Enrolment**: A Student's current membership in a shared Lesson; removing membership retains dated attendance
* **Roster**: The current enrolled Student IDs owned by one shared Lesson, from which displayed student records are derived
* **History**: Retained dated attendance keyed by Student ID, Lesson ID and date, independent of current membership
* **Capacity (future)**: A later limit on the number of students in a class
* **Make-up booking (future)**: A one-off reservation in a suitable class for a student who missed a regular lesson, without changing regular membership
* **Waiting list (future)**: An ordered queue of students requesting a place in a class that has reached a later capacity limit
* **Class-swap request (future)**: A pending request to move a student when a transfer cannot be completed immediately
* **Archived student (future)**: A withdrawn student retained for historical reference but hidden from the default active-student list

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Instructions for manual testing**

Use Java 25 and the [runtime prerequisites](SettingUp.html#setting-up-the-project-on-your-computer) for these checks. On macOS, use the course-prescribed Azul JDK 25 with JavaFX (`25.0.3.fx-zulu`) for the Mac's architecture. Confirm the terminal used for `java -jar` selects that installation and record the OS, architecture and JDK distribution/version with the test results. Additional macOS trials with other distributions, including plain Oracle JDK, are optional portability checks.

### Inline help and supported routes

1. Launch the built JAR using Java 25 in a fresh folder. Run `help`, then `help add`, `help delete`, `help list`, `help help`, and `help exit`. Each topic must match its actual active parser and supply a runnable example. Requesting `help exit` must leave the application open.
2. Select a person card, type an unfinished command, and choose Help from the menu. Repeat with F1 while focus is in the command box and in Result Display. Expect identical overview guidance, no separate window, unchanged card selection and unchanged unfinished input.
3. Run `help ADD`; expect the same content as `help add`. Run `help remove`, `help clear`, and `help addlesson`; expect an unknown-topic message directing you to `help`. Run `help add delete`; expect the help usage after `Invalid command format!`.
4. Resize to the minimum width. Run `help add`, scroll to its last example and back to the top. Expect wrapped, readable text and a working vertical scrollbar. Request another topic while scrolled down; it starts at the beginning.
5. Run `edit 1 n/Alex`, `clear`, and `find Alex`. Expect `Unknown command.` and unchanged people/data. Run `list extra` and `exit extra`; expect their usage messages and no list change or exit.
6. Compare the operational file before and after help; it must be byte-for-byte unchanged, and a missing operational file must remain missing. The automated failing-storage test covers help without operational write access. Preference saving at application shutdown remains separate.

Repeat the topic/example checks whenever a feature owner registers another command. Broader shared-lesson workflow and published-site checks remain pending the owners' integration; passing help checks alone does not establish completion of the broader team target. That complete target is separate from the Week 8 first-increment requirement.

### Planned person-index integration checks

Run these checks when the canonical people view and corresponding commands are activated. They are acceptance checks for the confirmed selector boundary, not claims that the current inherited runtime supports shared lessons or stable person IDs. Existing lesson references use `lid/LESSON_ID` throughout these target routes.

1. Prepare records with different stable IDs and current-view positions. Filter the people list to two Students whose IDs are, for example, `S2` and `S5`; verify their cards show positions `1` and `2` separately from those IDs. `delete 1` must target the first current card, subject to its relationship guards, rather than person `S1` or the first unfiltered record. Verify `delete S2`, zero, a negative index and an index beyond the current people-list size are rejected without changing data.
2. Exercise each activated enrolment, unenrolment, mark, unmark and history command using the student's current people-list index. Verify the command resolves that index once to the same stable Student ID used by its internal query or mutation. A stable ID such as `S2` supplied where the student index is required must be rejected. A Tutor or Parent at a valid position must fail a Student-only command without changing data; a valid roster row position is not a substitute for the people-list index.
3. Run `lessons si/1` when that route is available. Verify its student filter resolves position `1` in the current filtered people list to the stable Student ID used by the internal lesson query. Reject an out-of-range index, a non-Student target and the superseded external `sid/S2` form. The internal Student-ID query remains valid and unchanged.
4. Display lesson catalogue, roster and history results, including results whose own row positions differ from the people list. Verify the people filter, order and card indices stay unchanged; the next person command still targets the same current people card. Apply another people filter or ordering, recheck its displayed indices, and verify a command uses that new people view.
5. After a successful person-list change, verify current-view positions are refreshed while surviving stable IDs and stored relationships remain intact. On a failed validation or save, verify the records and prior people view are preserved. Restart with supported data and confirm persisted references use stable IDs rather than previously displayed indices.

### Dormant person-card developer preview

These checks exercise prepared components, not a supported application command. Run `seedu.address.ui.PersonRecordCardPreview.main` from the IDE's test source set with Java 25 and the test runtime classpath. It opens an isolated fixture list and never reads or writes application records or preferences.

1. Launch with the width and height arguments below. The effective logical sizes approximate the listed display conditions; repeat with actual system scaling on an available corresponding display when checking platform-specific fonts.

   | Display condition | Fixture arguments |
   | --- | --- |
   | 1280×720 at 150% | `853 480` |
   | 1920×1080 at 100% | `1920 1080` |
   | 1920×1080 at 125% | `1536 864` |

2. Check the student: `1.` is its current-view position, while `S9223372036854775807` is its stable ID. The level and parent phone remain readable; own phone, email, and address show `Not provided`.
3. Check the tutor's long name, unbroken email, and postal address. Expect every complete value to wrap within the available width, including leading phone zeros, with no overlap or ellipsis hiding content.
4. Launch with `320 480` to stress a narrow people panel. Scroll through the whole tutor card to the parent card. Expect the required parent own phone, separate role/ID line, and missing optional fields to remain readable; no horizontal scrolling is needed.
5. Close the preview. Active application routes and operational files remain unaffected. Repeat these checks in the actual people view when #77/#85 activate its canonical wiring.

Local verification rendered the actual FXML at all four logical sizes and checked wrapped-label widths, complete preferred text height, and access to the last card by scrolling. This verifies effective viewport behavior rather than actual OS scaling on every supported platform.

Given below are instructions to test the app manually.

<div markdown="span" class="alert alert-info">:information_source: **Note:** These instructions only provide a starting point for testers to work on;
testers are expected to do more *exploratory* testing.

</div>

### Launch and shutdown

1. Initial launch

   1. Download the JAR file and copy it into an empty folder.

   1. Double-click the JAR file.<br>
      Expected: The GUI opens with a set of sample contacts. The window size may not be optimal.

1. Saving window preferences

   1. Resize the window to an optimal size. Move the window to a different location. Close the window.

   1. Relaunch the app by double-clicking the JAR file.<br>
       Expected: The most recent window size and location are retained.

### Long emails in the current contact runtime

Use a disposable application folder and the active `add n/NAME p/PHONE e/EMAIL a/ADDRESS` format. These checks
cover validation safety; the planned 254-character email policy remains gated with the full contact alignment.

1. In a text editor, form an email from 5,000 copies of `a.` followed by `a@example.com`. Paste it into
   `add n/Long Email p/123 e/EMAIL a/Somewhere`, replacing `EMAIL` with that text. Expect a successful addition
   without an uncaught error. Restart and verify the stored email is preserved.
2. Repeat in a fresh disposable folder with 5,000 copies of `a-` after `a@`, ending in `ab`, and with 5,000
   copies of `a.` after `a@`, ending in `ab`. Expect the same accepted-value behavior. These long inherited
   values remain loadable even though the planned contact policy will later restrict their length.
3. Replace the first example's suffix with `a!@example.com`, or replace the second example's final `ab` with
   `a`. Expect the existing email constraint feedback; the command must not add a record or crash.
4. Close the app, make a valid manual edit to the disposable JSON file using one of the accepted long emails,
   and restart. Expect the edited email to load unchanged. Automated file-loader regressions additionally
   verify controlled loading errors and byte preservation for invalid emails; safe startup recovery remains #84 work.

### Deleting a person

The following small deletion checks apply to the current inherited contact runtime. Canonical guarded deletion must also pass the planned person-index and shared-lesson checks.

1. Deleting a person while all persons are being shown

   1. Prerequisites: List all persons using the `list` command, with multiple persons in the list.

   1. Test case: `delete 1`<br>
      Expected: The first contact is deleted from the list. The status message shows the deleted contact's details.

   1. Test case: `delete 0`<br>
      Expected: No person is deleted. The status message shows error details.

   1. Other incorrect delete commands to try: `delete`, `delete x`, `...` (where x is larger than the list size)<br>
      Expected: Similar to previous.

### Prepared canonical people sample checks

Use these checks in isolated developer code or a debugger with the prepared classes. The
active app does not load this fixture. The helper can be obtained with
`PeopleSampleDataUtil.getSamplePeopleRegistry()` and its `getPeople()` result supplied to
`PersonRecordListData`; build the selected record list from the projection's entries before
calling `PersonIndexResolver`.

1. Create two sample registries. Expect equal snapshots in order `T1, S1, P1, S2, T2, S3`.
   Remove `P1` from one registry using the registry's unreferenced-record test seam; both
   students' stored parent phones must remain `00987654`, and the second registry must retain
   all six records. This fixture has no lesson or attendance references to query.
2. Project all roles and each individual role. Expect all-role index `5` and tutor-filtered
   index `2` to select `T2`. Pass each entry's person and displayed index to
   `PersonRecordCardData`; expect stable IDs, required phones and `Not provided` for omitted
   contacts, with phone zeros and email case preserved.
3. Remove `S1` from a fresh sample and rebuild the student projection. Expect visible indices
   `1` and `2` to select `S2` and `S3`. Export/import that registry and add a distinct student;
   expect `S4`, with the new record appended. Export/import checks here use validated Java
   snapshots; JSON-file and application-restart checks follow canonical persistence activation.

### Planned shared-lesson workflow checks

Run these as the corresponding routes become available, using an isolated supported dataset. They verify the broader target rather than adding a complete-workflow requirement to every Week 8 PR.

1. Add one Tutor and two Students, read the assigned IDs, and create one lesson with `addlesson d/Mon st/1600 et/1730 s/Math tu/FULL_NAME tp/EXACT_PHONE rm/R1`, replacing the tutor placeholders with the added tutor's values. The catalogue must show the new lesson once with an empty roster. Substitute the actual returned Lesson ID for `L1` in every later `lid/` argument.
2. Run `list r/student`, check both current positions and use them in `enrol STUDENT_INDEX lid/L1`. Both students must refer to the same Lesson ID; the tutor and room booking must still exist only once. Check student clashes separately from global tutor/room clashes, including adjacent times.
3. Keep that people view while running `lessons`, `lessons si/STUDENT_INDEX` and `showlesson lid/L1`. Verify the people filter/order/indices remain unchanged. A roster position must not be reinterpreted as a people-list selector.
4. For each student, mark Monday `2026-10-05` with `mark STUDENT_INDEX lid/L1 d/2026-10-05 s/present` or `s/absent`. Verify one record per student/lesson/date, same-status no-op, correction and explicit unrecorded display for another Monday without a mark.
5. Unenrol one student using `unenrol STUDENT_INDEX lid/L1`. `history STUDENT_INDEX lid/L1` must retain their record. Correction and `unmark STUDENT_INDEX lid/L1 d/2026-10-05` must work for that existing key; a new mark without membership must be rejected. Check Student and Lesson deletion guards before and after removing their remaining dependencies.
6. Exercise each activated search filter from the full matrix, including a parent with two linked children and a student with two differently scheduled lessons. Combined filters must not take the name from one child and the lesson from another, or the day from one lesson and the subject from another. Verify distinct-ID counts, successful empty results and empty-lesson discovery when appropriate.
7. Restart and verify IDs, global creation order, membership and retained attendance. Trigger a save failure in an isolated test seam and verify the aggregate, allocation state and active views are restored. Read-only list/query/history/help operations must not rewrite operational data in the integrated runtime. Record the actual build, platform and results; do not treat unperformed checks as passes.

### Planned canonical loading and recovery

These checks apply when protected loading is activated. The current inherited loader does not yet guarantee rejected-file preservation; never use an original dataset for destructive failure experiments. Use copies in disposable test folders and keep the preserved original and backup outside all configured operational paths.

1. In a deliberately separate empty folder, launch the canonical build with no operational file. Expect a valid fresh supported root. Add delivered records and restart; expect successful restoration. This does not authorize replacing a legacy or rejected file in an existing folder.
2. Close the app and make a valid manual edit to supported versioned JSON using its delivered schema, such as correcting a contact value while preserving valid IDs, allocation state and references. Restart; expect the edited records to load. Repeat with valid memberships and dated history. Human editing must remain usable.
3. In independent copies, test malformed JSON, a null root/record, wrong record shapes, duplicate IDs or normalized duplicate people, dangling references, impossible dates, invalid allocation state, unreadable files and unsupported versions. Even a future-version file resembling legacy `persons` must be rejected before domain decoding or any write. Expect a controlled diagnostic and recovery instructions rather than a crash.
4. Capture each rejected file's bytes, then attempt help/list/exit and a data mutation wherever the failure state permits them. The rejected operational bytes must remain identical, and mutations must be blocked. If startup exits safely instead, verify it reports the reason and performs no operational write. Preferences are checked separately.
5. Test an unversioned AB3 file and formerly accepted contacts rejected by #61. Expect no guessed roles, silent conversion, record dropping or empty writable fallback. Follow the backup/manual re-entry guidance in a separate empty folder with explicitly chosen roles. Verify only the new supported root is writable and that the old file and preserved backup remain unchanged.
6. Correct an invalid supported file on a working copy while the app is closed, then restart and verify complete validation succeeds. If recovery uses an older build for inspection, use another working copy rather than the preserved original or backup. Record actual outcomes and the implemented error/recovery path in the UG/DG when this feature lands; no importer or recovery command is assumed.
