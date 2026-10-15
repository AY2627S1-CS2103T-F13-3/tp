# PonHub v1.3 — What's New

Release copy reconciled against v1.3 candidate source `b513d313d1ba219df662d401bc0949e5afb49309`. The candidate has recorded #103 verification; Vincent confirms that issue is closed. Publication is handled separately under #153. The original Mac demonstration media below retains its `c5b8233e` source identity; exact-candidate Windows captures and the claim-by-claim check are in [the candidate handoff](candidate-handoff.md). See also [the media manifest](media-plan.md).

PonHub adapts AddressBook-Level3 into a desktop workspace for tuition centre administrators. Manage Students, Tutors and Parents, enrol multiple Students in one recurring Lesson, and keep dated attendance even after a Student leaves. Commands remain keyboard-driven, with separate people and lesson/history views.

## People suited to tuition administration

- Add explicit Student, Tutor and Parent records. Students include an education level and parent phone; their own contact details are optional. Tutors and Parents require their own phone number.
- Use `list r/student`, `list r/tutor` or `list r/parent` to focus the people list. Cards distinguish the current command position from a persistent identity such as `S1` or `T1`.
- Select people using their current displayed positions, and Lessons using stable `lid/` references. Filtering may change person positions; it does not change saved identities.

## One Lesson shared by several Students

- Create a weekly Lesson with a Tutor, weekday, start/end times, subject and room. Tutor names are checked against existing records; an exact phone can disambiguate Tutors with the same name.
- Enrol two or more Students in the same Lesson without duplicating its schedule or Tutor assignment. Overlapping Tutor/room bookings and Student timetable clashes are rejected; adjacent time slots are allowed.
- Browse the catalogue with `lessons`, find a Student's current enrolments with `lessons si/INDEX`, or inspect a roster with `showlesson lid/L1`.

## Attendance that survives membership changes

- Record `present` or `absent` for one Student, Lesson and matching-weekday date. Students sharing a Lesson have independent attendance records. A missing record means unrecorded, not absent.
- Correct a record by marking it again with the other status. Remove only that dated record with `unmark`; repeating an unchanged mark or removing a missing record does not save a change.
- Unenrolment removes current membership while retaining dated attendance. Existing retained records remain correctable and removable after departure; new attendance dates require current enrolment.
- Use `showlesson lid/L1 d/2026-10-05` to inspect dated attendance, including former-member records separately. Use `history INDEX [lid/LESSON_ID]` for a Student's recorded history, newest first, with current/former membership context.

## Search people and their relationships

- Search Students, Parents, Tutors and Lessons, including category-only searches such as `search c/lesson`.
- Combine a Student's own details with their current Lesson's weekday, exact start/end times, subject, Tutor name or room.
- Find Parents through linked children and those children's current Lessons. A Parent's own phone links to a Student's parent phone by exact equality, including leading zeros.
- Find Tutors through their assigned Lessons and currently enrolled Students. Search a Tutor's own name with `n/`; Lesson-category Tutor-name searches use `tu/`.
- Every supplied condition must match the same relevant child and Lesson. Different siblings or Lessons cannot contribute separate parts of one match. Results appear once per stable identity, with matching relationship context.
- Text searches use case-insensitive fragments; phone filters match complete values. Start/end filters match exact HHMM values, not overlapping time windows. Retained attendance does not count as current enrolment.

## Safer changes and clearer recovery

- People, Lessons, rosters, attendance and ID allocation state are saved together. Failed saves preserve the previous committed data and views; read-only commands do not write operational data.
- Dependency guards prevent deleting a Student with membership/history, a Tutor with assigned Lessons, or a Lesson with a roster/history. Deletion does not silently cascade through related records, and deleted IDs are not reused.
- Invalid, unsupported or legacy operational files start a protected session with recovery guidance and only `help`, `list` and `exit`. The original file is preserved. Existing AB3 data is not automatically imported: back it up and follow the User Guide's manual re-entry instructions.
- Inline `help`, `help COMMAND`, the Help menu and F1 describe the routes available in the current session.

## PonHub in action

![Two Students sharing one recurring Lesson](../../docs/images/Ui.png)

Alex and Jamie share one Math Lesson with one Tutor and schedule.

![Independent attendance for the same occurrence](../../docs/images/AttendanceDetails.jpg)

Alex is present while Jamie is absent on the same date.

![Retained attendance after unenrolment](../../docs/images/AttendanceHistory.jpg)

After correcting Alex's record and unenrolling Alex, the dated absence remains visible as former-enrolment history.

## Before you start

Use Java 25. On macOS, use the course-prescribed Azul JDK with JavaFX (`25.0.3.fx-zulu`) for your architecture. Keep the JAR in a stable folder; the default operational file is `data/ponhub.json` beneath it. Consult the [User Guide](../../docs/UserGuide.md) for installation, accepted formats, recovery and a complete shared-Lesson example.

This version does not provide the inherited `edit`, `clear` or `find` routes. Person/Lesson editing, undo/redo, fees, capacity/waiting lists, make-ups, occurrence cancellation, exports, saved searches, OR/exclusion search and bulk attendance remain future scope. Use `search` for supported retrieval.

## Credits

PonHub builds on AddressBook-Level3 from the SE-EDU initiative. Feature authors retain credit for their implementation and documentation. Benjamin Ng supplied the search/retrieval facts and existing Lesson-results media; Vincent Lin coordinated these release notes and the final media plan with OpenAI Codex assistance. See [About Us](../../docs/AboutUs.md) for team responsibilities.
