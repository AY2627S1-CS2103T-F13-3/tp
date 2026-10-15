# v1.3 media and publication handoff

Owner: Vincent (`gugrusaurus`), #152 / #102. Publisher: Yang (`ys000009`), #153. Zhi Yu handles merges and #101 acceptance.

## Source and readiness

The draft [release notes](notes.md) were checked against accepted master `4e3619fc50e74a5b186631b64ff15e2a7496d414`: the canonical command catalogue/session, User Guide and delivered search/history contracts. This is not the final release-candidate identity. PR #249 adds visible quick-start attendance checks; PR #250 addresses #248's native history readability finding. Neither was merged when the initial draft was prepared. #250 has since merged at c5b8233e; #101 and #248 are closed.

Final media and publication remain pending accepted #101/#102 evidence and the exact packaged candidate verified under #103. Do not label baseline checks or existing images as final-candidate evidence.

## Existing material and attribution

- [Ben's search/retrieval handoff](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/152#issuecomment-6192204036) supplies checked facts and a capture fixture. Its source is `3534f24b`; its statement that history was pending is historical. History is delivered in the accepted baseline above.
- [Ben's Lesson-results image](../../docs/images/LessonResults.png) can illustrate the existing split-pane catalogue where it matches the accepted candidate. It does not demonstrate relationship-search context or final attendance-history readability. Preserve Ben's authorship.
- The final integrated screenshot/demo is Vincent's responsibility. Update the actual consumers in README/index/UG consistently if replacing a referenced image; do not assume they all use `Ui.png`.

## Capture fixture

Launch the verified candidate in a disposable JAR folder with an initially empty operational file. Record source SHA, JAR filename/SHA-256, OS/architecture, Java/JavaFX distribution/version, window dimensions and display scaling before capture. Use only these fictitious records.

```text
add r/tutor n/Mei Lim p/92345678 e/mei@example.com
addlesson d/Mon st/1600 et/1730 s/Math tu/Mei Lim rm/R1
add r/student n/Alex Tan l/S2 pp/91234567
add r/student n/Jamie Tan l/P1 pp/91234567
add r/parent n/Pat Tan p/91234567
list r/student
enrol 1 lid/L1
enrol 2 lid/L1
lessons
```

Use the allocated Lesson ID if it is not L1. In this fresh fixture, `list r/student` places Alex at 1 and Jamie at 2. Recheck positions after every people-filter change.

## Demonstration sequence and expected visible results

| Shot | Commands after fixture | Expected view and suggested caption |
| --- | --- | --- |
| A: shared Lesson | `lessons`, then `showlesson lid/L1` | One Lesson with Alex and Jamie in its current roster. “Two Students share one recurring Lesson.” |
| B: independent attendance | `mark 1 lid/L1 d/2026-10-05 s/present`, `mark 2 lid/L1 d/2026-10-05 s/absent`, `showlesson lid/L1 d/2026-10-05` | Alex present, Jamie absent for the same occurrence. “Attendance is recorded independently for each Student.” |
| C: correction | `mark 1 lid/L1 d/2026-10-05 s/absent`, `showlesson lid/L1 d/2026-10-05` | Both absent; only Alex's record changed. “Correct one dated record without duplicating it.” |
| D: retained history | `unenrol 1 lid/L1`, `history 1 lid/L1` | Alex's absence remains visible with former-member context and readable text. “Leaving a Lesson preserves attendance history.” |
| E: relationship search | `search c/parent sn/Jamie d/Mon s/Math` | Pat with Jamie and matching Math Lesson context; Jamie remains enrolled. “Find Parents through their children's current Lessons.” |
| F: restart | `exit`; relaunch the same JAR/store; `list r/student`, `lessons`, `history 1 lid/L1` | Stable IDs, Jamie's current enrolment and Alex's retained absence survive restart. “Saved relationships and attendance persist between sessions.” |

Capture A, B and D as the minimum still-image set; include C, E and F in a short recording or additional stills if needed. Capture the actual application window with readable text, without unrelated desktop content. Confirm readable history at both normal and compact available widths after #248 is accepted. Do not use a generated mockup as product evidence.

Optional removal/guard demonstration, after the main captures: `list r/student`, `unmark 1 lid/L1 d/2026-10-05`, `history 1 lid/L1` (empty); `deletelesson lid/L1` remains blocked by Jamie's membership/attendance. This intentionally changes the disposable fixture; do not reuse its empty history as Shot D.

## Final handoff checklist

- [ ] Record accepted source SHA and the exact #103-verified JAR identity/runtime.
- [ ] Recheck every release-note claim and command against that candidate; record actual demo outcomes, including failures.
- [x] Capture the integrated images/recording; record relative paths, captions, authors and source identity here.
- [x] Confirm images show readable current UI and no real operational data. Preserve credits for reused Ben media.
- [ ] Complete #102's guide/source/published-site consistency checks against accepted changes.
- [ ] Replace the draft notice in notes.md only when release copy and media are accepted; record any remaining user-facing limitation.
- [ ] Hand Yang the accepted notes path, media paths, captions and candidate identity for #153; verify GitHub-rendered media and links after publication.

Capture application source: `c5b8233e9bfaf5b8c7eb7f6af8932345c17c4c63`. The media branch at `003a79d8` has identical production source/resources/build configuration; its additional changes are prose. Local `shadowJar` output SHA-256: `170a22ed680dc687b7e9c5fc79846d246368372cab9ec8995302a6ad4a5e43c3`.

Vincent captured the actual app on macOS 27.0.1 arm64, Azul Java/JavaFX 25.0.3 (`25.0.3.fx-zulu`), in a 740x600 logical window (1480x1200 screenshot pixels). A temporary macOS launcher identified the app to the accessibility tool; it executes the same JAR, without changing application classes/resources. Disposable data only. Captures are full-window images; Ui.png is a format conversion of the original JPEG capture, with no content alteration.

| Image | Author | Verified content |
| --- | --- | --- |
| `docs/images/Ui.png` | Vincent, Codex-assisted native capture | Alex and Jamie share L1; roster count 2. |
| `docs/images/AttendanceDetails.jpg` | Vincent, Codex-assisted native capture | Alex present / Jamie absent for 2026-10-05. |
| `docs/images/AttendanceHistory.jpg` | Vincent, Codex-assisted native capture | Alex absent after correction and unenrolment; former-enrolment label readable. |

Native command-box replay also verified all four relationship-search categories, correction to absent/absent, complete exit/relaunch, retained IDs/roster/history, selective unmark, repeated unmark no-op and guarded/valid Lesson deletion. The stored file was byte-identical across exit/relaunch and subsequent read-only checks. One automation entry accidentally appended to a rejected command retained by the UI; replacing the full input and rerunning passed. The failed attempt remains in the transcript.

Evidence: `_temp/issue102-final/native-transcript.json`, build/launch logs, isolated JAR and pre-restart JSON in Vincent's Mac workspace. These are source-qualified guide/media checks, not Yang's final #103 artifact or all-platform acceptance. Final release source/JAR reconciliation remains pending; #152 stays open until final handoff. No release is published.

Prepared by Vincent with OpenAI Codex assistance. Ben's feature facts and media remain credited to Ben; feature owners retain implementation/documentation authorship.

See [the integrated guide verification report](guide-verification.md) for completed native, source, render and accepted-site parity checks and the remaining deployment gate.
