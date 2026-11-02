---
layout: page
title: Screenshot sources and credits
---

## Guide screenshots

The images illustrate the development interface using fictitious records.

Vincent Lin (`gugrusaurus`) created the original guide media. Zhu Zhi Yu
(`ultramanarm`) later refreshed the overview, navigation and attendance-history images.
Both used OpenAI Codex assistance. Their earlier feature and media authorship is retained.

### Current overview, navigation and attendance details

Vincent recaptured these three images on 2 November 2026 with OpenAI Codex assistance,
using the unchanged application at source
[`399faedefe40978fcdfedce92bcea9d1ed53405a`](https://github.com/AY2627S1-CS2103T-F13-3/tp/tree/399faedefe40978fcdfedce92bcea9d1ed53405a).
The local `ponhub.jar` SHA-256 is
`2114bd4e9601e7a65557216ae52ed8d79493f816e9ddb6a62254b1e5715e7f50`.

These are JavaFX scene captures from the packaged application on Windows amd64 with
Oracle Java 25.0.4.1. A temporary capture agent submits commands through the real command
box and snapshots the production scene root at 2× resolution. The captures exclude the
operating-system title bar. No production source, UI text, colours or layout styles were
modified. No UI redrawing, compositing or AI-generated image content was used.

The window is 740×1000 logical pixels; each complete scene capture is 1448×1922 pixels.
Only rectangular cropping and image encoding are applied after capture. Crop bounds below
are `(left, top, right, bottom)` in capture pixels. The two PNG files preserve the captured
pixels exactly; the detail JPEG uses quality 95 with no chroma subsampling.

| Image | Pixels | Crop bounds | Illustration |
| --- | --- | --- | --- |
| [Ui.png](Ui.png) | 1448×1922 | `(0, 0, 1448, 1922)` | Alex and Jamie share L1; the people panel shows Students. |
| [LessonBoardNavigation.png](LessonBoardNavigation.png) | 1448×1922 | `(0, 0, 1448, 1922)` | Alex's current lessons remain selected while the people panel shows Tutors. |
| [AttendanceDetails.jpg](AttendanceDetails.jpg) | 816×760 | `(632, 276, 1448, 1036)` | Current Lesson information, Alex present and Jamie absent on 5 October 2026. |

The fictitious fixture contains Tutor T1 (Mei Lim), Students S1/S2 (Alex/Jamie Tan),
and Monday lesson L1 (Math, 1600–1730, R1), with both Students enrolled. The dated records
mark S1 present and S2 absent on `2026-10-05`. The application uses
`C:\Apps\PonHub\data\ponhub.json`.

The overview follows `list r/student` and `lessons`. Navigation follows `lessons 1` and
`list r/tutor`. Details follow `list r/student` and `showlesson lid/L1 d/2026-10-05`.
All capture commands are read-only; the operational file bytes were checked unchanged.

### Retained attendance-history image

[AttendanceHistory.png](AttendanceHistory.png) remains Zhu Zhi Yu's native macOS crop:
816×340 pixels, cropped from `(662, 340, 1478, 680)` in a 1480×1600 capture.
It uses the [attendance-history card](https://github.com/ultramanarm/tp/blob/545-clarify-attendance-enrolment-status/src/main/resources/view/AttendanceHistoryCard.fxml)
and a fictitious fixture with S2 outside L1's current roster while the 5 October 2026
absence remains recorded. Its capture method and feature credit are unchanged.

The previous overview/navigation captures used source
[`aa81aa445276366cd1baafc1b7cbb09b10cbcf01`](https://github.com/AY2627S1-CS2103T-F13-3/tp/tree/aa81aa445276366cd1baafc1b7cbb09b10cbcf01);
the previous detail capture used
[`b22d81569b96ba66b258c94a9ca2b5a7658cdeaa`](https://github.com/AY2627S1-CS2103T-F13-3/tp/tree/b22d81569b96ba66b258c94a9ca2b5a7658cdeaa).

## Other project media

Benjamin Ng (`ben58882`) contributed `LessonResults.png` and the earlier Lesson-board
media, including `LessonBoardShell.png`. Release-specific illustrations retain their
credits in the [v1.3 media note](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/master/release/v1.3/media-plan.md)
and [v1.4 media note](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/master/release/v1.4/media-provenance.md).
Ernest Chua created the PonHub icon/wordmark with OpenAI Codex assistance.
The website's [PonHub logo](ponhub-logo.svg) combines Ernest's original
[people and lessons icon](ponhub-icon.svg) and wordmark styling. Benjamin Ng
adapted it for the website with OpenAI Codex assistance. Both SVG assets retain
their proportions and are original project artwork under the repository's MIT licence.
Feature, diagram and portrait authorship remains with the contributors identified in
[About Us](../AboutUs.html); existing upstream licences remain applicable.
