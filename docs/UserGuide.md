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

1. Download the latest PonHub `.jar` file from the project's Releases page.

1. Put the JAR file in the folder you want to use for PonHub. Keep this folder when moving or backing up your data.

1. Open a terminal in that folder and run `java -jar FILENAME.jar`, replacing `FILENAME.jar` with the downloaded file's name. The PonHub window should open.

1. Enter a command in the command box and press Enter. Start with `help`, then try this example workflow:<br>

   * `add r/tutor n/Mei Lim p/92345678` — adds a tutor.
   * `add r/student n/Alex Tan l/S2 pp/91234567` — adds a student and their parent contact number.
   * `list r/student` — shows students and their displayed indices.
   * `addlesson 1 d/Mon t/1600-1730 s/Math tu/Mei Lim r/R1` — adds a lesson for the student displayed at index `1`.

   The indices in commands refer to the current displayed list, so check the list before using an index. See the [Command summary](#command-summary) for more commands.

--------------------------------------------------------------------------------------------------------------------
## Features

<div markdown="block" class="alert alert-info">

**:information_source: Notes about the command format:**<br>

* Words in `UPPER_CASE` are the parameters to be supplied by the user.<br>
  For example, in `add n/NAME`, replace `NAME` with a value such as `John Doe`.

* Items in square brackets are optional.<br>
  For example, `n/NAME [t/TAG]` can be used as `n/John Doe t/friend` or as `n/John Doe`.

* Items followed by `…`​ can appear zero or more times.<br>
  For example, `[t/TAG]…​` may be omitted, or written as `t/friend` or `t/friend t/family`.

* Parameters can be in any order.<br>
  For example, if the command specifies `n/NAME p/PHONE_NUMBER`, `p/PHONE_NUMBER n/NAME` is also acceptable.

* Extraneous parameters for commands that take no parameters, such as `help`, `list`, `exit`, and `clear`, are ignored.<br>
  For example, `help 123` is interpreted as `help`.

* If you are using a PDF version of this document, be careful when copying and pasting commands that span multiple lines as space characters surrounding line-breaks may be omitted when copied over to the application.
</div>

### Viewing help: `help`

Shows a message explaining how to access the help page.

![help message](images/helpMessage.png)

Format: `help`


### Adding a person: `add`

Adds a person to the address book.

Format: `add n/NAME p/PHONE_NUMBER e/EMAIL a/ADDRESS [t/TAG]…​`

<div markdown="span" class="alert alert-primary">:bulb: **Tip:**
A person can have any number of tags, including zero.
</div>

Examples:
* `add n/John Doe p/98765432 e/johnd@example.com a/John street, block 123, #01-01`
* `add n/Betsy Crowe t/friend e/betsycrowe@example.com a/Newgate Prison p/1234567 t/criminal`

### Listing all persons: `list`

Shows a list of all persons in the address book.

Format: `list`

### Editing a person: `edit`

Edits an existing person in the address book.

Format: `edit INDEX [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]…​`

* Edits the person at the specified `INDEX`. The index refers to the index number shown in the displayed person list. The index **must be a positive integer** 1, 2, 3, …​
* At least one of the optional fields must be provided.
* Existing values will be updated to the input values.
* When editing tags, all of the person's existing tags are removed; adding tags is not cumulative.
* To remove all of a person's tags, enter `t/` without a tag after it.

Examples:
*  `edit 1 p/91234567 e/johndoe@example.com` Edits the phone number and email address of the 1st person to be `91234567` and `johndoe@example.com` respectively.
*  `edit 2 n/Betsy Crower t/` Edits the name of the 2nd person to be `Betsy Crower` and clears all existing tags.

### Locating persons by name: `find`

Finds persons whose names contain any of the given keywords.

Format: `find KEYWORD [MORE_KEYWORDS]`

* The search is case-insensitive; for example, `hans` matches `Hans`.
* Keyword order does not matter; for example, `Hans Bo` matches `Bo Hans`.
* The search considers only names.
* Only full words match; for example, `Han` does not match `Hans`.
* Persons matching at least one keyword are returned (an `OR` search); for example, `Hans Bo` returns `Hans Gruber` and `Bo Yang`.

Examples:
* `find John` returns `john` and `John Doe`
* `find alex david` returns `Alex Yeoh`, `David Li`<br>
  ![result for 'find alex david'](images/findAlexDavidResult.png)

### Deleting a person: `delete`

Deletes the specified person from the address book.

Format: `delete INDEX`

* Deletes the person at the specified `INDEX`.
* The index refers to the index number shown in the displayed person list.
* The index **must be a positive integer** 1, 2, 3, …​

Examples:
* `list` followed by `delete 2` deletes the 2nd person in the address book.
* `find Betsy` followed by `delete 1` deletes the 1st person in the results of the `find` command.

### Clearing all entries: `clear`

Clears all entries from the address book.

Format: `clear`

### Exiting the program: `exit`

Exits the program.

Format: `exit`

### Saving the data

AddressBook automatically saves data after every command. You do not need to save manually.

### Editing the data file

AddressBook data is saved automatically as a JSON file `[JAR file location]/data/addressbook.json`. Advanced users are welcome to update data directly by editing that data file.

<div markdown="span" class="alert alert-warning">:exclamation: **Caution:**
If your changes make the data file invalid, AddressBook starts with an empty address book at the next run. The invalid file remains on disk until you run a command (AddressBook saves after every command). Still, we recommend backing up the file before editing it.<br>
Furthermore, certain edits can cause the AddressBook to behave in unexpected ways (e.g., if a value entered is outside of the acceptable range). Therefore, edit the data file only if you are confident that you can update it correctly.
</div>

### Archiving data files `[coming in v2.0]`

_Details coming soon ..._

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
**Add lesson** | `addlesson INDEX d/DAY t/START-END s/SUBJECT tu/TUTOR_NAME r/ROOM`<br>e.g., `addlesson 1 d/Mon t/1600-1730 s/Math tu/Mei Lim r/R1`
**Delete lesson** | `deletelesson INDEX LESSON_INDEX`<br>e.g., `deletelesson 1 2`
**Search** | `search c/CATEGORY [FILTER_PREFIX/VALUE]...`<br>e.g., `search c/student s/Math d/Mon`
**Mark attendance** | `mark STUDENT_INDEX LESSON_INDEX d/DATE s/STATUS`<br>e.g., `mark 1 2 d/2026-09-18 s/present`
**Remove attendance mark** | `unmark STUDENT_INDEX LESSON_INDEX d/DATE`<br>e.g., `unmark 1 2 d/2026-09-18`
**Help** | `help [COMMAND]`<br>e.g., `help add`
**Exit** | `exit`

`ROLE` is `student`, `tutor`, or `parent`. `CATEGORY` in `search` selects the kind of result; available filters depend on that category. Square brackets mean optional input and are not typed.
