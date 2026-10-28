---
layout: page
title: Screenshot sources and credits
---

## Guide screenshots

The images illustrate the development interface using fictitious records. They are
not a claim of release or display-matrix acceptance.

Vincent Lin (`gugrusaurus`) created the original guide media. Zhu Zhi Yu
(`ultramanarm`) refreshes the overview and navigation images. Both use OpenAI Codex
assistance. These are native macOS application captures: only rectangular cropping
and image encoding are applied, without redrawing, compositing or AI-generated UI
content. The raw windows measure 1480×1600 pixels.
Crop bounds below are `(left, top, right, bottom)` in those pixels; no resizing was
applied. The overview/navigation images use PNG encoding that preserves the decoded
capture pixels. The attendance JPEGs use quality 95 with no chroma subsampling.

| Image | Pixels | Crop bounds | Illustration |
| --- | --- | --- | --- |
| [Ui.png](Ui.png) | 1480×1600 | `(0, 0, 1480, 1600)` | Two Students sharing one Lesson. |
| [LessonBoardNavigation.png](LessonBoardNavigation.png) | 1480×1600 | `(0, 0, 1480, 1600)` | Student lessons beside an independent people filter. |
| [AttendanceDetails.jpg](AttendanceDetails.jpg) | 816×724 | `(662, 340, 1478, 1064)` | Alex present and Jamie absent in the current roster. |
| [AttendanceHistory.jpg](AttendanceHistory.jpg) | 816×320 | `(662, 340, 1478, 660)` | Jamie's absence retained after unenrolment. |

Overview/navigation captures include the native title bar and persistent people heading.
Attendance crops retain the relevant heading and results so their text remains readable in the guide. The attendance
images show source
[`b22d81569b96ba66b258c94a9ca2b5a7658cdeaa`](https://github.com/AY2627S1-CS2103T-F13-3/tp/tree/b22d81569b96ba66b258c94a9ca2b5a7658cdeaa).
Overview and navigation show source
[`aa81aa445276366cd1baafc1b7cbb09b10cbcf01`](https://github.com/AY2627S1-CS2103T-F13-3/tp/tree/aa81aa445276366cd1baafc1b7cbb09b10cbcf01)
with the persistent Student heading and reset hint. They may differ from public v1.4.

## Other project media

Benjamin Ng (`ben58882`) contributed `LessonResults.png` and the earlier Lesson-board
media, including `LessonBoardShell.png`. Release-specific illustrations retain their
credits in the [v1.3 media note](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/master/release/v1.3/media-plan.md)
and [v1.4 media note](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/master/release/v1.4/media-provenance.md).
Ernest Chua created the PonHub icon/wordmark with OpenAI Codex assistance.
Feature, diagram and portrait authorship remains with the contributors identified in
[About Us](../AboutUs.html); existing upstream licences remain applicable.
