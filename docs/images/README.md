---
layout: page
title: Guide screenshot provenance
---

## Current #384 captures

These are development-build images, not captures of the public v1.4 release.
Prepared for Vincent (`gugrusaurus`), with OpenAI Codex driving the native
application and capturing it on 25 October 2026. No unaided human checks or
new feature authorship are claimed; existing contributors' credits remain.

- Application source: `b22d81569b96ba66b258c94a9ca2b5a7658cdeaa` (integrated #372/#441 navigation).
- Local `./gradlew --offline --no-daemon shadowJar`, prescribed Azul Zulu Java/JavaFX
  `25.0.3.fx-zulu`, Java `25.0.3+9-LTS`.
- Package: `ponhub.jar`, 12,882,981 bytes; SHA-256
  `d54c2456496404180a5f592749847227027a310d5a93a81b64ea5ee8e91b8c7c`.
- macOS 27.0.1 (26A434), arm64. Window: 740×800 logical points;
  raw native window captures: 1480×1600 pixels (2× capture density).
  No claim of testing a different physical display/scaling combination.
- Disposable folder `/private/tmp/PonHub384`; selected store `data/guide-synthetic.json`.
  A temporary accessibility launcher runs the unchanged JAR with `-ea` and
  `-Xdock:name=PonHub384`. It is not a distributed installer or evidence for #428.
- Public release rechecked during preparation: v1.4 `ponhub.jar`, 12,655,415 bytes,
  SHA-256 `8fbd0e5414d91c006984489b71083445da41d22f76f37c2fbc49a681d79ac9d6`.
  Current release links remain pointed at that public artifact.

### Fixture and sequence

All names, phones and email below are fictitious. Start from an empty selected store:

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

Capture `Ui.png` with All lessons active and both Students enrolled in L1.
Select Alex's card, click Student lessons, then return focus to the command box;
capture `LessonBoardNavigation.png`. The heading identifies S1 independently of
people selection. Then:

```text
mark 1 lid/L1 d/2026-10-05 s/present
mark 2 lid/L1 d/2026-10-05 s/absent
showlesson lid/L1 d/2026-10-05
```

Capture `AttendanceDetails.jpg`: both Students are current members, Alex present
and Jamie absent. Next, without changing either attendance status:

```text
unenrol 2 lid/L1
history 2 lid/L1
```

Capture `AttendanceHistory.jpg`: S2's same absence now has former-enrolment context.
Close normally with `exit`. This sequence is screenshot-fixture evidence, not a
full release acceptance suite.

### Processing and identities

Only rectangular cropping and image encoding were applied. No UI content was
redrawn, composited or AI-generated. Overview/navigation crops remove the native
macOS title bar (including the capture-service indicator) while keeping the app
navigation, feedback, records, bottom command box and status footer. Attendance
crops show only the right result heading and relevant content, avoiding unreadable
full-window scaling. Bounds are `(left, top, right, bottom)` in raw screenshot pixels;
no resizing was applied. JPEGs use quality 95 with no chroma subsampling.

| Current file | Pixels | Crop bounds |
| --- | --- | --- |
| [Ui.png](Ui.png) | 1480×1544 | `[0, 56, 1480, 1600]` |
| [LessonBoardNavigation.png](LessonBoardNavigation.png) | 1480×1544 | `[0, 56, 1480, 1600]` |
| [AttendanceDetails.jpg](AttendanceDetails.jpg) | 816×724 | `[662, 340, 1478, 1064]` |
| [AttendanceHistory.jpg](AttendanceHistory.jpg) | 816×320 | `[662, 340, 1478, 660]` |

Exact SHA-256 values for every current image, raw screenshot and archived original
are retained in [the capture manifest](capture-manifest-384.json), alongside the
crop bounds. Raw captures remain locally with the disposable fixture, outside the
repository, to avoid duplicating full-window evidence in the guide assets.

## Historical images

The prior four guide images are retained byte-for-byte below from baseline
`b22d8156`; these are historical captures, not evidence for the new package.
The three v1.3 Mac captures retain the source/runtime/operator record in
[the original media plan](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/b22d81569b96ba66b258c94a9ca2b5a7658cdeaa/release/v1.3/media-plan.md). The older Lesson-board
image was contributed by Benjamin Ng and last updated in commit `511d1555`; that is an asset-change identity,
not a newly established application/JAR identity. Existing release media and
contributor authorship remain unchanged.

| Archived original |
| --- |
| [Ui.png](archive/pre-384-Ui.png) |
| [LessonBoardNavigation.png](archive/pre-384-LessonBoardNavigation.png) |
| [AttendanceDetails.jpg](archive/pre-384-AttendanceDetails.jpg) |
| [AttendanceHistory.jpg](archive/pre-384-AttendanceHistory.jpg) |
