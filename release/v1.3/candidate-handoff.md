# v1.3 candidate reconciliation and publication handoff

Prepared by Vincent (`gugrusaurus`) with OpenAI Codex assistance for #152 on 15 October 2026. Yang (`ys000009`) owns #103 acceptance and #153 publication. Feature authors retain their implementation and documentation credit; Ben's search facts and existing media remain credited in the notes and media manifest.

## Candidate identity and readiness

- Application source: `b513d313d1ba219df662d401bc0949e5afb49309`, merged #252.
- Artifact: `ponhub.jar`, **12,712,759 bytes**; SHA-256 `f932224736b8a9ea9c87228ef7ce65e5a6f31313e9a4be2a1013c16263354c05`.
- Manifest: `Implementation-Title: PonHub`, `Implementation-Version: 1.3`, `Main-Class: seedu.address.Main`; application version `v1.3`.
- [Windows exact-candidate report](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/103#issuecomment-6196023254): native launch and user-operated workflow/restart checks, with Codex inspecting screenshots/logs. Windows 10.0.19045 x64, installed Oracle Java 25.0.4.1+1-LTS-5. Separate automated source checks used Azul Java/JavaFX 25.0.4.1: 877 passed, 15 skipped, zero failures/errors; style and coverage passed.
- [Linux exact-candidate run](https://github.com/ultramanarm/tp/actions/runs/38761553866): automated production-JAR GUI execution on Ubuntu 24.04.5 x86_64, Azul Java 25.0.4+7-LTS, JAR-supplied JavaFX, Xvfb 1280x800. This is a hosted virtual display check, not a manual physical-display check; #254's review/integration remains separate.
- [Mac author report and independent build review](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/252): author-submitted native Apple Silicon workflow/restart results; independent reviewer reproduced the artifact and checks but explicitly did not independently exercise the Mac GUI. The [final #103 artifact report](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/103#issuecomment-6195673149) records this source/hash; Vincent confirms #103 is now closed. The connector still returned an older open snapshot during this preparation, so closure is attributed to that user confirmation.

**Release materials are reconciled to the #103 candidate; Vincent confirms #103 is closed. This documentation increment requires teammate review/merge before its final URLs are handed off for publication.** No release, tag or Canvas submission is created by this handoff. If the accepted application source or JAR changes, refresh this identity and the affected claim/media checks before publication.

## Release-note claim reconciliation

Checked every feature bullet, command example, caption and limitation in [notes.md](notes.md) against this candidate's command catalogue/session, implementation and User Guide. Source inspection establishes supported contracts; manual screenshots establish only the observed flows. The Windows source-test run covers additional behavior but is not a packaged interaction test.

| Notes claim group | Candidate evidence and limits |
| --- | --- |
| Student/Tutor/Parent fields; role lists; stable IDs versus current positions | `CanonicalCommandCatalog`, `PeopleListParser`, role records and `PeopleView`; UG Adding/Listing people. Windows captures show S1/S2 stable across restart and current list positions 1/2. |
| Shared weekly Lesson; Tutor disambiguation; timetable conflict guards; adjacent slots | `AddLessonParser`, lesson creation command, `PonHubDataState`, `LessonTimeSlot`; UG shared-lesson creation and enrolment. Windows captures show two Students in one L1. Conflict/disambiguation claims are source/test-backed, not additional manual trials in this run. |
| `lessons`, `lessons si/INDEX`, `showlesson lid/L1` | Catalogue/session and `LessonRetrieval`; UG command summary. Windows exercises catalogue and dated details; optional Student-specific listing is source/test-backed. |
| Independent dated present/absent; correction; missing means unrecorded | `PonHubData.markAttendance`, attendance parser and query projections; Windows initially shows Alex present/Jamie absent, then Alex absent in former-member history. Linux report also checks both corrected statuses. |
| Selective `unmark`; no-op marks/removal; retention after unenrolment; only existing records correctable after departure | `PonHubData.markAttendance`, `unmarkAttendance`, `unenrolStudent` and executor no-change handling. Windows shows retained former history, successful removal and repeated removal reporting no change; new-date restriction is source/test-backed. |
| `showlesson lid/L1 d/2026-10-05`; `history INDEX [lid/LESSON_ID]`, newest-first/current-former context | Catalogue, `LessonQueries`/`LessonQuerySource`, history parser and retrieval. Date is a Monday matching fixture L1. Windows details/history screenshots and restart log corroborate the examples. |
| Four search categories, own fields, current Lesson relationships, Parent phone links, Tutor assignment, same-child/same-Lesson matching | `SearchCriteriaParser`, `SearchField`, `PersonContactSearch`, `PersonRelationshipSearch`, `LessonQueries`; UG search matrix. Candidate search implementation is unchanged from the original source-qualified Mac replay of all four categories and Ben's facts. These search commands were not manually replayed in this Windows session. |
| Case-insensitive text fragments; exact phones/HHMM; no duplicate identities; retained attendance is not current membership | Search criteria and relationship/query implementation; UG matching contracts. Source/test-backed, with original Mac replay retaining its own identity. |
| Aggregate persistence, failed-save preservation and read-only commands | `CanonicalCommandExecutor`, protected storage and immutable aggregate publication; UG data protection. Windows log confirms exit/relaunch and retained IDs, membership and Jamie's record. Failure injection is automated source-test evidence, not a manual packaged trial. |
| Guarded Student/Tutor/Lesson deletion; no cascade; IDs not reused | `PonHubData` deletion blockers and allocation counters. Windows L1 deletion rejected for S2; unused L2 creation/deletion succeeds. Person guards and ID non-reuse are source/test-backed. |
| Protected invalid/unsupported/legacy files; no automatic AB3 import | `ProtectedPonHubDataStorage`, startup/session restrictions and catalogue protected entries (`help`, `list`, `exit`); UG recovery/manual re-entry. No destructive migration or legacy-file experiment was performed in this manual run. |
| Inline/topic help, Help menu and F1 | Catalogue and MainWindow bindings; Windows supplied captures plus user's explicit menu confirmation. F1 preserves unfinished `lessons` and current history. |
| Java 25; prescribed Mac Azul JavaFX runtime; stable folder and `data/ponhub.json` | Candidate DevOps/SettingUp/UserGuide and actual launch report. Windows screenshots show the disposable store path. No additional Java vendor/architecture compatibility is inferred. |
| Unsupported/future scope | `edit`, `clear`, `find` absent from active router/catalogue. Editing, undo/redo, fees, capacity/waiting lists, make-ups, cancellation, export, saved/OR/exclusion search and bulk attendance remain explicitly future scope. |

The earlier media source `c5b8233e9bfaf5b8c7eb7f6af8932345c17c4c63` and this candidate have identical production behavior/resources except `MainApp.VERSION`; build changes are the project version and manifest metadata. Checked with `git diff c5b8233e b513d313 -- src/main build.gradle`. This supports the existing captions' feature accuracy, but does not relabel those earlier Mac images as exact-v1.3-JAR verification.

## Exact-candidate supplementary Windows media

User-operated screenshots supplied by Vincent in this chat; saved unmodified, fictitious fixture data only. The visible local Windows path identifies the disposable workspace store. Original compact captures are approximately 726x590 pixels; no display-scaling value was recorded. Do not infer a DPI/scaling acceptance claim.

| Image | Caption / observed content |
| --- | --- |
| [Shared Lesson](media/windows-shared-lesson.png) | Alex S1 and Jamie S2 share recurring Math Lesson L1; one card, roster count 2. |
| [Independent attendance](media/windows-attendance.png) | On 2026-10-05, Alex is present and Jamie absent in the same Lesson. |
| [Former-member history](media/windows-former-history.png) | After correcting Alex to absent and unenrolling him, his dated absence remains with former-enrolment context. |

These three images meet the minimum A/B/D still-image sequence in [media-plan.md](media-plan.md) on the exact candidate. Additional local Windows captures/logs support unmark/no-op, guarded/valid deletion, Help, restart and maximized display; the self-contained #103 comment records their outcomes. The compact empty-history explanatory label is ellipsized, while its zero-record count remains visible. No overlapping/clipped populated history row was observed in compact/maximized supplied captures.

Original Mac release-note images retain the authorship, platform, source and JAR identity in the media manifest. Ben's `LessonResults.png` remains credited and unchanged.

## Handoff to Yang for #153

Release copy: [notes.md](notes.md). Media/captions/authorship: [media-plan.md](media-plan.md) and the table above. Guide evidence: [guide-verification.md](guide-verification.md). Application candidate and acceptance evidence: identity and links above.

For the GitHub release body, relative paths from `notes.md` must be replaced with working absolute links to the reviewed, merged documentation commit. In particular, `../../docs/images/...`, `../../docs/UserGuide.md`, `../../docs/AboutUs.md` and sibling Markdown links are repository-file links, not release-page-relative links. Use `https://raw.githubusercontent.com/AY2627S1-CS2103T-F13-3/tp/DOCS_SHA/docs/images/Ui.png` (and the two attendance images) for embedded original media, or the corresponding `DOCS_SHA/release/v1.3/media/windows-*.png` paths for the candidate stills. Replace `DOCS_SHA` with the actual merged documentation SHA; it is not an application-source identity. Use GitHub blob links for Markdown guides and this handoff. Preserve captions and credits.

Before publication, Yang retains the recorded #103 acceptance evidence and confirms the accessible location of the exact JAR to upload. The local verified copy is `issue103-evidence/clean/ponhub.jar` in Vincent's workspace; the hosted Linux run also retains a checksum-qualified evidence artifact. A workspace path is not a downloadable public asset. Verify bytes after retrieval and upload; tag the accepted application commit, retaining the separately reviewed documentation commit for media URLs.

After publication, verify the downloaded release asset's size/hash, actual launch, every displayed media/guide link and the final release URL. Give Vincent that URL and evidence for #154. These publication/download checks belong to #153 and remain pending; this document does not claim they passed.
