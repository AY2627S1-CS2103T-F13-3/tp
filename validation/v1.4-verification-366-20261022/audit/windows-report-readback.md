# Final-candidate Windows native run — 22 October 2026

Manual execution by Vincent (`gugrusaurus`), guided by OpenAI Codex. All 29 supplied screenshots were visually inspected in the conversation and preserved unchanged in screenshots/. This is Vincent's actual Windows run, not Ernest's personal run or an additional independent human reviewer. Vincent did not author the person/Lesson editing implementations, but contributed persistence, shell and package-focus work; retain that component-level authorship distinction.

## Identity

- Reviewed application source: ed32fe49901ea11070ff16086490d2d72adafcd1.
- Exact JAR from final Linux artifact 12395006972 / workflow 39706371977: 12,654,205 bytes; SHA-256 5e24df511c9a5975c1c15e310ab03f2a852837e969359afe71fc8a37fa905456. Manifest PonHub / 1.4 / ponhub.Main. Hash verified before/after manual run.
- Isolated writable folder: candidate-v14-windows-20261022. No operational file existed initially. Launch-Candidate.cmd explicitly uses Oracle JDK 25.0.4.1+1-LTS-5 with -ea; runtime.txt records Windows 10 / amd64. Project-packaged JavaFX dependencies, no runtime replacement or rendering-scale override.
- Native app launched and accepted typing immediately, user-confirmed. Initial screenshot predates submitted help; user subsequently confirmed help appeared and setup commands were accepted.

## Results

| Scenario | Observed result |
| --- | --- |
| Create Tutor/two Lessons/two Students; enrol both in L1 | PASS: stable T1/S1/S2 and L1/L2, roster two, initially unrecorded. |
| Person and Lesson edits | PASS: Alex Tan/S2 level becomes Alex Lee/P3 while retaining S1; L1 becomes Advanced Math/1630–1730/R3 with both members. |
| Independent attendance/correction | PASS: Alex present/Jamie absent, then Alex corrected to absent while Jamie remains absent. |
| Rejected edits | PASS: P7 invalid; duplicate Jamie; L2 start 1700 overlaps Tutor; L1 Tuesday conflicts with retained Monday attendance. Screenshots show recoverable errors, retained input and unchanged view. |
| Normalized person no-op | PASS: `edit 1 n/ Alex   Lee l/p3` reports no changes. Combined rejection/no-op checkpoint file hash unchanged. |
| Unenrol/history | PASS: S1 removed from roster but absence retained as former-enrolment history. |
| First restart | PASS: Alex has zero current Lessons; Jamie has L1; edited Alex/P3 and L1 fields persist; former absence persists. |
| Attendance no-ops | PASS: both S1/S2 already-absent confirmations; file hash unchanged. One extra attempt targeted Tutor position 1 in an all-person view and correctly rejected it. Restored Student filter before the two successful no-ops; do not label the selector rejection a product failure. |
| Contact/Tutor edit and query refresh | PASS: Alex phone/email updated; T1 renamed Mei Chen; both L1/L2 labels and Tutor-name search refresh, IDs stable. |
| Parent relationship search refresh | PASS: P1 Pat Tan initially matches Alex; Parent phone change removes P1 from active Alex relationship search; Alex parent-phone update restores P1. Jamie's original parent phone remains unchanged. |
| Lesson search refresh | PASS: active Advanced Math search retains Advanced Mathematics/R4; renaming to Algebra removes L1; lessons restores L1 with same ID/time/Tutor/S2 roster. |
| Real F1 and Help menu | PASS: help catalogue opens while preserving unfinished help editlesson draft, selected Alex, Student filter and former-history context. User confirms draft preserved; both screenshots show retained state. |
| Final restart/exit | PASS: contact/Tutor/Parent/Lesson edits, relationship and former history persist. User confirms normal exit. Saved bytes remain identical to final checkpoint. |

## Integrity checkpoints

- Before/after rejected edits plus normalized no-op: 2fc5f624da362404d281259fe2d3646307b86132e3382d1418d7f5a88ffd9469.
- Before/after first restart, read-only queries, selector rejection and both attendance no-ops: 0f9ffebcb21eaa0d9121b17903215f9edd08dec57b1ea562af8f893ade9a7682.
- Before/after final restart, queries and normal exit: 3cfa711f4b8de63546f8feabee746276620bdc10449f5d4669750ce5a44c8360.
- Final saved state: T1 Mei Chen; S1 Alex Lee/P3/contact changes/parentPhone 91234568; S2 Jamie unchanged; P1 Pat/91234568; L1 Algebra/Mon/1630–1730/R4 with S2; L2 Science unchanged; S1 and S2 absent 2026-10-05 in L1; counters Student 2/Tutor 1/Parent 1 and Lesson sequence 2. Final JSON and original checkpoints retained beside report.

## Limits and handoff

This completes the guided representative Windows packaged/native scenario on the exact candidate. No fresh display-scaling matrix, Mac run, protected-session native flow or injected save-failure scenario was performed. Reuse #317 display evidence and #270/owner regressions only under their original identities/scope. Normal file hashes prove byte preservation at the checkpoints, not absence of every possible filesystem write. No product changes, release publication, final cross-platform acceptance or issue closure are implied. #272 owner reconciliation and remaining Mac/platform evidence stay separate.
