# PonHub v1.4 — Changes since v1.3

PonHub v1.4 adds person and shared-Lesson editing and a refreshed Lesson board for tuition centre administrators. Correct existing records while keeping their stable IDs, current enrolments and dated attendance.

## Correct people without recreating records

- Use `edit INDEX` with one or more fields to update a Student, Tutor or Parent. Omitted fields stay unchanged; the role and stable person ID remain fixed.
- Update names, contact details and, for Students, education level and parent phone. Blank `e/` or `a/` clears that optional value; blank `p/` clears only a Student’s own phone. Required names, Student level/parent phone and Tutor/Parent phones cannot be cleared.
- A Tutor rename updates current Lesson labels while preserving the Tutor assignment. Changing a Parent’s phone or a Student’s parent phone recomputes their exact-phone relationship; it does not rewrite another person’s phone.
- The current people filter stays active. An edited person can leave a search result, so recheck displayed positions before the next indexed command.

After the [Quick start fixture](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/ed32fe49901ea11070ff16086490d2d72adafcd1/docs/UserGuide.md#quick-start), these commands update Alex’s level and clear any optional email, then adjust the existing Lesson:

```text
list r/student
edit 1 l/P2 e/
editlesson lid/L1 st/1630 s/Advanced Math
showlesson lid/L1 d/2026-10-05
```

These example positions and `L1` assume that fresh fixture. With existing data, use the current people position and actual Lesson ID. See [person editing](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/ed32fe49901ea11070ff16086490d2d72adafcd1/docs/UserGuide.md#editing-a-person-edit) for the full format and restrictions.

## Edit a shared Lesson while preserving its roster and history

- Use `editlesson lid/LESSON_ID` with any supported weekday, time, subject, Tutor or room changes. Omitted fields stay unchanged; the Lesson keeps its ID, catalogue position, roster and attendance history.
- Change either time endpoint by itself when the complete resulting interval remains valid. Tutor, room and enrolled-Student clashes are rejected; adjacent slots remain valid.
- Select a replacement Tutor with `tu/NAME`; add `tp/PHONE` alongside it when the normalized full name is ambiguous. A phone alone does not select a Tutor.
- A weekday change is rejected if it would invalidate retained attendance, including records for former members. PonHub identifies an incompatible date and keeps the existing history. Remove individual records with `unmark` only if you intend to discard them before retrying.

See [Lesson editing](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/ed32fe49901ea11070ff16086490d2d72adafcd1/docs/UserGuide.md#editing-a-shared-lesson-editlesson) for accepted fields and examples.

## Results follow committed changes

Person and Lesson edits refresh affected searches, Lesson cards, selected details and attendance views after saving succeeds. Invalid input, duplicate records, schedule conflicts and save failures preserve the previous data and results. An identical normalized edit reports no change and does not write the operational file.

The shared-Lesson, relationship-search and attendance workflows introduced in v1.3 remain available. Unenrolling a Student preserves dated history; a missing attendance record still means unrecorded. Editing keeps those records and their stable identities. `help edit` and `help editlesson` now describe the active editing routes.

## A clearer Lesson board and PonHub identity

The orange and black interface now includes a compact PonHub wordmark and a new people-and-book application icon. A full-width command box sits below the people and Lesson results. Resize the feedback area and column widths, scroll the two lists independently, and use wrapping cards and history rows to read longer values.

Optional **Lessons**, **Student lessons** and **Attendance** buttons open the same results as their commands. Student shortcuts use a selected Student in the current people list and preserve unfinished command text. Typed commands continue to work directly from any result view.

![Edited Student and Lesson with independent dated attendance](media/shared-attendance.png)

Alex Lee retains Student ID S1 after the name and level edit. Lesson L1 now shows Advanced Math,
Monday 1630–1730 and room R3, with two Students still enrolled. On 2026-10-05, Alex is present
and Jamie is absent.

![Retained former-member attendance after restart](media/restarted-history.png)

After correcting Alex to absent, unenrolling Alex and restarting, S1’s dated absence remains with
former-enrolment context. The feedback is a later unchanged-mark confirmation for Jamie; the right
panel remains Alex’s selected history.

Both images are unmodified captures of the packaged JAR on a hosted Ubuntu Xvfb virtual display,
not physical Mac or Windows evidence. See [capture provenance](media-provenance.md) for the exact
source, JAR checksum, commands and credits.

## Updated guidance and maintenance

The [User Guide](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/ed32fe49901ea11070ff16086490d2d72adafcd1/docs/UserGuide.md) includes both edit commands, selector rules, relationship refresh and recovery guidance. The [Developer Guide](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/ed32fe49901ea11070ff16086490d2d72adafcd1/docs/DeveloperGuide.md) describes the canonical runtime, edit/persistence boundaries, updated diagrams and manual checks. Public entry pages now describe delivered PonHub features, and obsolete sample content and unused inherited implementation paths have been removed.

## Running this version and current limits

Use Java 25 and launch the downloaded JAR from a stable writable folder. On macOS, use architecture-appropriate Azul JDK with JavaFX `25.0.3.fx-zulu`, following the [installation guide](https://se-education.org/guides/tutorials/javaInstallationMac.html). The default operational file remains `data/ponhub.json` beneath the JAR folder; see the [Quick start](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/ed32fe49901ea11070ff16086490d2d72adafcd1/docs/UserGuide.md#quick-start) and [data protection guidance](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/ed32fe49901ea11070ff16086490d2d72adafcd1/docs/UserGuide.md#data-protection-in-the-current-build).

Protected sessions continue to permit only `help`, `list` and `exit`. There is no automatic import of inherited AB3 data. Undo/redo, fees, capacity/waiting lists, make-ups, occurrence cancellation, exports, archives, saved/guided searches, OR/exclusion search and bulk or clickable attendance remain future scope. The inherited `clear` and `find` routes remain unavailable.

## Credits

PonHub builds on AddressBook-Level3 from the SE-EDU initiative. Ernest Chua owns the person-edit commands and help, with Benjamin Ng credited for the underlying person-edit model support and retrieval/views. Yang Shuo owns Lesson editing, enrolment and attendance; Vincent Lin owns persistence and release-material coordination; Zhu Zhi Yu owns people commands and testing. Ernest created the new application artwork with OpenAI Codex assistance. Earlier retrieval and Lesson-board media retain Benjamin’s credit; Vincent’s earlier attendance media retains its original v1.3 provenance. The two images here were generated by the repository’s hosted package-verification workflow, dispatched by Zhu Zhi Yu with OpenAI Codex assistance; their scenario and tooling authors are credited in the capture provenance.

This release-copy preparation used OpenAI Codex assistance for Zhu Zhi Yu. The [team responsibilities](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/ed32fe49901ea11070ff16086490d2d72adafcd1/docs/AboutUs.md) and [acknowledgements](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/ed32fe49901ea11070ff16086490d2d72adafcd1/docs/DeveloperGuide.md#acknowledgements) retain implementation, documentation and reuse credits.

[Full source changes since v1.3](https://github.com/AY2627S1-CS2103T-F13-3/tp/compare/v1.3...ed32fe49901ea11070ff16086490d2d72adafcd1)
