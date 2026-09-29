---
layout: page
title: Developer Guide
---
* Table of Contents
{:toc}

--------------------------------------------------------------------------------------------------------------------

## **Acknowledgements**

* _{List the sources of reused or adapted ideas, code, documentation, and third-party libraries here, with links to the originals.}_

--------------------------------------------------------------------------------------------------------------------

## **Setting up, getting started**

Refer to the guide [_Setting up and getting started_](SettingUp.md).

--------------------------------------------------------------------------------------------------------------------

## **Design**

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
* When called upon to parse a user command, the `AddressBookParser` class creates an `XYZCommandParser` (`XYZ` is a placeholder for the specific command name, e.g., `AddCommandParser`). The parser uses the other classes shown above to parse the user command and create an `XYZCommand` object (e.g., `AddCommand`). The `AddressBookParser` returns that object as a `Command` object.
* All `XYZCommandParser` classes, such as `AddCommandParser` and `DeleteCommandParser`, implement the `Parser` interface so they can be treated similarly where appropriate, for example during testing.

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

### Common classes

Classes used by multiple components are in the `seedu.address.commons` package.

--------------------------------------------------------------------------------------------------------------------

## **Implementation**

This section describes some noteworthy details on how certain features are implemented.

### \[Proposed\] Undo/redo feature

#### Proposed Implementation

The proposed undo/redo mechanism is facilitated by `VersionedAddressBook`. It extends `AddressBook` with an undo/redo history, stored internally as an `addressBookStateList` and `currentStatePointer`. Additionally, it implements the following operations:

* `VersionedAddressBook#commit()` — Saves the current address book state in its history.
* `VersionedAddressBook#undo()` — Restores the previous address book state from its history.
* `VersionedAddressBook#redo()` — Restores a previously undone address book state from its history.

These operations are exposed in the `Model` interface as `Model#commitAddressBook()`, `Model#undoAddressBook()` and `Model#redoAddressBook()` respectively.

Given below is an example usage scenario and how the undo/redo mechanism behaves at each step.

Step 1. The user launches the application for the first time. The `VersionedAddressBook` will be initialized with the initial address book state, and the `currentStatePointer` pointing to that single address book state.

![UndoRedoState0](images/UndoRedoState0.png)

Step 2. The user executes `delete 5` command to delete the 5th person in the address book. The `delete` command calls `Model#commitAddressBook()`, causing the modified state of the address book after the `delete 5` command executes to be saved in the `addressBookStateList`, and the `currentStatePointer` is shifted to the newly inserted address book state.

![UndoRedoState1](images/UndoRedoState1.png)

Step 3. The user executes `add n/David …​` to add a new person. The `add` command also calls `Model#commitAddressBook()`, causing another modified address book state to be saved into the `addressBookStateList`.

![UndoRedoState2](images/UndoRedoState2.png)

<div markdown="span" class="alert alert-info">:information_source: **Note:** If a command fails its execution, it will not call `Model#commitAddressBook()`, so the address book state will not be saved into the `addressBookStateList`.

</div>

Step 4. The user now decides that adding the person was a mistake, and decides to undo that action by executing the `undo` command. The `undo` command will call `Model#undoAddressBook()`, which will shift the `currentStatePointer` once to the left, pointing it to the previous address book state, and restores the address book to that state.

![UndoRedoState3](images/UndoRedoState3.png)

<div markdown="span" class="alert alert-info">:information_source: **Note:** If the `currentStatePointer` is at index 0, pointing to the initial AddressBook state, then there are no previous AddressBook states to restore. The `undo` command uses `Model#canUndoAddressBook()` to check if this is the case. If so, it will return an error to the user rather
than attempting to perform the undo.

</div>

The following sequence diagram shows how an undo operation goes through the `Logic` component:

![UndoSequenceDiagram](images/UndoSequenceDiagram-Logic.png)

<div markdown="span" class="alert alert-info">:information_source: **Note:** The lifeline for `UndoCommand` should end at the destroy marker (X), but due to a limitation of PlantUML, it continues to the end of the diagram.

</div>

Similarly, how an undo operation goes through the `Model` component is shown below:

![UndoSequenceDiagram](images/UndoSequenceDiagram-Model.png)

The `redo` command does the opposite — it calls `Model#redoAddressBook()`, which shifts the `currentStatePointer` once to the right, pointing to the previously undone state, and restores the address book to that state.

<div markdown="span" class="alert alert-info">:information_source: **Note:** If the `currentStatePointer` is at index `addressBookStateList.size() - 1`, pointing to the latest address book state, then there are no undone AddressBook states to restore. The `redo` command uses `Model#canRedoAddressBook()` to check if this is the case. If so, it will return an error to the user rather than attempting to perform the redo.

</div>

Step 5. The user then decides to execute the command `list`. Commands that do not modify the address book, such as `list`, will usually not call `Model#commitAddressBook()`, `Model#undoAddressBook()` or `Model#redoAddressBook()`. Thus, the `addressBookStateList` remains unchanged.

![UndoRedoState4](images/UndoRedoState4.png)

Step 6. The user executes `clear`, which calls `Model#commitAddressBook()`. Since the `currentStatePointer` is not pointing at the end of the `addressBookStateList`, all address book states after the `currentStatePointer` will be purged. Reason: It no longer makes sense to redo the `add n/David …​` command. This is the behavior that most modern desktop applications follow.

![UndoRedoState5](images/UndoRedoState5.png)

The following activity diagram summarizes what happens when a user executes a new command:

<img src="images/CommitActivityDiagram.png" width="250" />

#### Design considerations:

**Aspect: How undo & redo execute:**

* **Alternative 1 (current choice):** Saves the entire address book.
  * Pros: Easy to implement.
  * Cons: May have performance issues in terms of memory usage.

* **Alternative 2:** Individual command knows how to undo/redo by
  itself.
  * Pros: Will use less memory (e.g. for `delete`, just save the person being deleted).
  * Cons: We must ensure that the implementation of each individual command is correct.

_{more aspects and alternatives to be added}_

### \[Proposed\] Data archiving

_{Explain here how the data archiving feature will be implemented}_


--------------------------------------------------------------------------------------------------------------------

## **Documentation, logging, testing, dev-ops**

* [Documentation guide](Documentation.md)
* [Testing guide](Testing.md)
* [Logging guide](Logging.md)
* [DevOps guide](DevOps.md)

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Requirements**

### Product scope

**Target user profile**:

* has a need to manage a significant number of contacts
* prefers desktop apps over other types of applications
* can type fast
* prefers typing to mouse interactions
* is reasonably comfortable using CLI apps

**Value proposition**: Manage contacts faster than with a typical mouse-driven GUI application.


### User stories

Priorities: High (must have) - `* * *`, Medium (nice to have) - `* *`, Low (unlikely to have) - `*`

| Priority | As a …​                                    | I want to …​                     | So that I can…​                                                        |
| -------- | ------------------------------------------ | ------------------------------ | ---------------------------------------------------------------------- |
| `* * *`  | new user                                   | see usage instructions         | refer to instructions when I forget how to use the App                 |
| `* * *`  | user                                       | add a new person               |                                                                        |
| `* * *`  | user                                       | delete a person                | remove entries that I no longer need                                   |
| `* * *`  | user                                       | find a person by name          | locate details of persons without having to go through the entire list |
| `* *`    | user                                       | hide private contact details   | minimize chance of someone else seeing them by accident                |
| `*`      | user with many persons in the address book | sort persons by name           | locate a person easily                                                 |

*{More to be added}*

### Use cases

For the use cases below, the **System** is `PonHub` and the **Actor** is the tuition centre administrator.

#### UC-01: Add a person record

**Related user stories:** US-01, US-02, US-03, US-14, US-15, US-30, US-34

**Preconditions:** PonHub is running. The administrator has the person's role and required contact details.

**Main success scenario**

1. The administrator requests to add a student, tutor or parent.
2. PonHub displays the fields required for the selected role.
3. The administrator enters the person's details.
4. PonHub validates the fields and checks for exact and possible duplicates.
5. PonHub shows the normalized record for confirmation.
6. The administrator confirms the addition.
7. PonHub creates and saves the record, refreshes the person list and reports success.

**Extensions**

* 3a. A required field is missing, invalid, repeated or unsupported: PonHub explains the problem, keeps valid input and resumes at step 3.
* 4a. An exact duplicate exists: PonHub rejects the addition and the use case ends.
* 4b. Possible duplicates exist: PonHub shows the matches. The administrator cancels, ending the use case, or chooses to continue at step 5.
* 6a. The administrator cancels: no record is created and the use case ends.
* 7a. Saving fails: PonHub rolls back the addition, reports the failure and the use case ends.

**Postconditions:** One valid, non-duplicate person record is stored and visible. Existing records are unchanged.

#### UC-02: Find and review operational records

**Related user stories:** US-04, US-05, US-06, US-22, US-29, US-38, US-39, US-40, US-41

**Preconditions:** PonHub is running and has loaded the stored person, lesson and class records.

**Main success scenario**

1. The administrator requests a list or search of people, lessons or classes.
2. PonHub displays the available categories, filters, sorting options and saved searches.
3. The administrator supplies search conditions such as role, name, level, tutor, subject or lesson day.
4. PonHub validates the conditions and searches the relevant records.
5. PonHub displays each distinct match once, ordered or grouped as requested, with relevant relationship context and highlighted matches.
6. The administrator opens a result or requests a class-list export.
7. PonHub displays the selected details or generates the requested export.

**Extensions**

* 3a. The administrator uses a guided advanced-search form: PonHub supplies valid filters and completion, then resumes at step 3.
* 4a. A condition is invalid or incompatible with the selected category: PonHub explains the valid syntax and resumes at step 3.
* 4b. A command may be misspelled: PonHub suggests the closest valid command. If accepted, resume at step 4.
* 5a. No record matches: PonHub displays a successful empty result and the use case ends.
* 5b. The administrator asks to save the search: PonHub stores the named search and resumes at step 5.
* 7a. The export cannot be generated: PonHub reports the failure without changing stored records and the use case ends.

**Postconditions:** The requested view is displayed. Stored operational data is unchanged; a saved search or export exists only if requested.

#### UC-03: Schedule and maintain a recurring lesson

**Related user stories:** US-08, US-09, US-10, US-16, US-27, US-28, US-36, US-37, US-47

**Preconditions:** The student and regular tutor records exist. PonHub has loaded current tutor and room bookings.

**Main success scenario**

1. The administrator selects a student and requests a recurring lesson.
2. PonHub displays the student's lessons and the relevant tutor and room schedules.
3. The administrator enters the day, start and end times, subject, tutor and room, or selects a reusable lesson template.
4. PonHub validates the student, tutor, values and time range.
5. PonHub checks the proposed slot for tutor and room clashes.
6. PonHub displays the proposed lesson for confirmation.
7. The administrator confirms the lesson.
8. PonHub saves it, updates the student and tutor timetables, and reports success.

**Extensions**

* 3a. The administrator edits an existing lesson: PonHub pre-fills its details and resumes at step 3.
* 4a. A value is missing or invalid, or the tutor does not exist: PonHub explains the problem and resumes at step 3.
* 5a. The tutor or room is already booked: PonHub identifies the conflict and resumes at step 3.
* 6a. The administrator cancels: no schedule is changed and the use case ends.
* 8a. The administrator assigns a replacement tutor or cancels one occurrence: PonHub changes only that occurrence, saves it and the use case ends.
* 8b. The administrator requests deletion but attendance exists: PonHub blocks deletion and the use case ends.
* 8c. Saving fails: PonHub restores the previous schedule, reports the failure and the use case ends.

**Postconditions:** A clash-free recurring lesson or occurrence-level change is stored, and affected timetable views are current.

#### UC-04: Manage class membership and a make-up booking

**Related user stories:** US-17, US-18, US-19, US-20, US-24, US-25, US-48

**Preconditions:** The student and relevant tutor records exist. For enrolment or make-up booking, the class exists.

**Main success scenario**

1. The administrator opens class management and selects a class.
2. PonHub displays its tutor, schedule, room, capacity, roster, remaining places and waiting list.
3. The administrator selects a student and requests regular enrolment or a one-off make-up place.
4. PonHub validates the records, checks for duplicate membership, timetable conflicts and available capacity.
5. PonHub previews the change and its effect on remaining places.
6. The administrator confirms the change.
7. PonHub records and saves the enrolment or reservation, updates the roster and capacity, and reports success.

**Extensions**

* 1a. The class does not exist: the administrator enters its name, tutor, day, time, room and capacity. PonHub checks conflicts, creates the class, then resumes at step 2.
* 3a. The administrator starts from a recorded absence: PonHub pre-fills the student and missed lesson, then resumes at step 3.
* 3b. The administrator requests removal from a class: PonHub previews the removal and resumes at step 6.
* 4a. The class is full: PonHub offers to add the student to the waiting list. If accepted, it saves the waiting-list position and the use case ends.
* 4b. No suitable place is available during a requested swap: PonHub records a pending swap request and the use case ends.
* 4c. The student is already enrolled or has a timetable conflict: PonHub rejects the change and resumes at step 3.
* 7a. Saving fails: PonHub restores the previous roster and capacity, reports the failure and the use case ends.

**Postconditions:** The class roster, remaining capacity and any related wait-list, swap or make-up record are consistent and saved.

#### UC-05: Record attendance and follow up

**Related user stories:** US-11, US-12, US-21, US-26, US-43, US-44, US-45, US-46, US-49

**Preconditions:** The student or class, recurring lesson and selected lesson occurrence exist.

**Main success scenario**

1. The administrator opens today's lessons or finds a lesson and date.
2. PonHub displays the roster and any attendance already recorded for that occurrence.
3. The administrator marks students present, absent, late or excused, individually or in bulk, and optionally adds absence notes.
4. PonHub validates the date, occurrence and statuses.
5. PonHub shows which attendance entries will be created or updated.
6. The administrator confirms the entries.
7. PonHub saves one authoritative entry per student and occurrence, updates the display, attendance summaries and absence list, and reports success.
8. The administrator selects any follow-up needed for an absence.
9. PonHub records the feedback, make-up link or fee adjustment selected by the administrator.

**Extensions**

* 3a. The administrator uses clickable controls instead of a command: PonHub records the same pending statuses and resumes at step 4.
* 4a. The date is invalid or does not match the lesson's weekday: PonHub rejects the input and resumes at step 1.
* 5a. The same status is already recorded: PonHub reports that no change is needed and resumes at step 8.
* 5b. A different status exists: PonHub previews a correction and resumes at step 6.
* 5c. The administrator requests removal of an erroneous entry: PonHub previews the removal and resumes at step 6.
* 5d. There is no attendance record to remove: PonHub reports this and resumes at step 2.
* 7a. Saving fails: PonHub restores the previous attendance state, reports the failure and the use case ends.
* 8a. No follow-up is needed: the use case ends.

**Postconditions:** Attendance is saved without duplicate entries, summaries are current and any selected follow-up is linked to the absence.

#### UC-06: Remove, archive or restore a record

**Related user stories:** US-07, US-23, US-31, US-35

**Preconditions:** The target person record exists and PonHub has loaded its lesson and attendance links.

**Main success scenario**

1. The administrator selects a person and requests deletion or archiving.
2. PonHub previews the target and any linked lessons or attendance, and asks for confirmation.
3. The administrator confirms the requested action.
4. PonHub verifies that the action will not break existing references.
5. PonHub deletes an eligible obsolete record or archives a withdrawn student, saves the change and updates the active list.
6. PonHub reports success and makes the last change available for undo.

**Extensions**

* 3a. The administrator cancels: PonHub changes nothing and the use case ends.
* 4a. Linked lesson or attendance records prevent deletion: PonHub blocks deletion, identifies the dependencies and resumes at step 1.
* 4b. The administrator chooses archiving instead: PonHub retains history, hides the student from the active list and resumes at step 6.
* 5a. Saving fails: PonHub restores the prior state, reports the failure and the use case ends.
* 6a. The administrator requests undo: PonHub restores the immediately preceding state, saves it and reports success.
* 6b. No change is available to undo: PonHub reports the error without changing data and the use case ends.
* 6c. The administrator selects an archived student and requests restoration: PonHub returns the student to the active list, saves the change and the use case ends.

**Postconditions:** The record is safely deleted, archived or restored without broken references; the resulting state is saved.

#### UC-07: Get help and recover from a command-entry error

**Related user stories:** US-13, US-32, US-33, US-42

**Preconditions:** PonHub is running and the command interface is available.

**Main success scenario**

1. The administrator requests help, focuses the command field or begins entering a command.
2. PonHub displays command categories, completion options, syntax, examples and relevant shortcuts.
3. The administrator chooses a help topic or accepts a completed command.
4. PonHub displays detailed guidance or validates the completed command.
5. The administrator enters any required parameters and submits the command.
6. PonHub executes the valid command and displays its result.

**Extensions**

* 1a. Help is requested without a topic: PonHub shows the first page of the full command catalogue and resumes at step 3.
* 3a. The catalogue spans multiple pages: the administrator requests the next or previous page; PonHub displays it and resumes at step 3.
* 4a. The command name is unknown or misspelled: PonHub suggests a likely command and resumes at step 3.
* 4b. Required parameters are missing or invalid: PonHub shows inline guidance and an example, preserves valid input and resumes at step 5.
* 4c. The administrator uses a shortcut or alternative help form: PonHub maps it to the corresponding command and resumes at step 4.
* 6a. The chosen command fails for a domain-specific reason: PonHub reports the error, changes no data and the use case ends.
* 6b. Help is requested repeatedly: PonHub refreshes the same guidance without changing data and the use case ends.

**Postconditions:** The requested guidance remains visible, or the selected command has completed with clear feedback.

### Non-Functional Requirements

1.  Should work on any _mainstream OS_ as long as it has Java `25` or above installed.
2.  Should be able to hold up to 1000 persons without noticeable sluggishness in performance for typical usage.
3.  A user with above average typing speed for regular English text (i.e. not code, not system admin commands) should be able to accomplish most of the tasks faster using commands than using the mouse.

*{More to be added}*

### Glossary

* **Mainstream OS**: Windows, Linux, Unix, or macOS
* **Private contact detail**: A contact detail that is not meant to be shared with others

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Instructions for manual testing**

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

1. _{ more test cases …​ }_

### Deleting a person

1. Deleting a person while all persons are being shown

   1. Prerequisites: List all persons using the `list` command, with multiple persons in the list.

   1. Test case: `delete 1`<br>
      Expected: The first contact is deleted from the list. The status message shows the deleted contact's details.

   1. Test case: `delete 0`<br>
      Expected: No person is deleted. The status message shows error details.

   1. Other incorrect delete commands to try: `delete`, `delete x`, `...` (where x is larger than the list size)<br>
      Expected: Similar to previous.

1. _{ more test cases …​ }_

### Saving data

1. Dealing with missing/corrupted data files

   1. _{Explain how to simulate missing or corrupted data files and state the expected behavior.}_

1. _{ more test cases …​ }_
