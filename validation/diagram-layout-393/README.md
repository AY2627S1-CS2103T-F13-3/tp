# #393 — actual diagram readability increment

Vincent (@gugrusaurus), with OpenAI Codex assistance, 27 October 2026 SGT.
Base: `c5883e9297354ac9a818e523310bf69e0b6f5cec`, including reviewed/integrated #500.
This is the actual layout increment. #475 remains a separate preparation-only draft; its history is not imported.

## Changes

- Split Membership and Attendance into domain and value-reference views. The original seven/five
  association edges retain endpoint classes, labels, directions and multiplicities across each pair.
  Informal package grouping borders are removed from these compact views; no domain grouping semantics
  are added. Each repeated Lesson/Attendance box denotes the same classifier, as the guide explains.
- Larger local type/arrow/note settings and spaced compass routing keep values and counts readable.
  The shared `style.puml` is unchanged; existing diagram/feature authorship is retained.
- Split both long sequence renders at the save/publication boundary with PlantUML `newpage`.
  All original ordered call/return lines are unchanged. The query success continuation has an explicit
  `opt save succeeded` guard; the Architecture outcome alternatives remain exclusive. The callback
  activation still encloses the synchronous retrieval/return. Both continuations repeat lifeline headers.
- Figure wrappers keep captions and images together (`break-inside` / legacy `page-break-inside: avoid`).
  Scoped print sizing bounds tall panels at 235 mm. All maintained figures now have native-image links.

| Figure (first panel) | Base pixels | New pixels |
| --- | --- | --- |
| LessonMembershipClassDiagram | 942 × 323 | 513 × 306 |
| AttendanceClassDiagram | 897 × 304 | 463 × 323 |
| ArchitectureSequenceDiagram | 908 × 1079 | 868 × 967 |
| LessonQueryRefresh | 844 × 913 | 747 × 586 |

Supplementary value views: Lesson 394×217 and Attendance 277×150. There are fourteen editable
sources / sixteen displayed PNGs: the twelve existing topics, two value-reference supplements,
and two sequence continuations. No quota or universal font-size threshold is introduced.

## Checks performed

- PlantUML 1.2026.2, prescribed Mac Zulu Java/FX 25.0.3, explicit headless rendering and existing
  Smetana layout: changed PNGs regenerate. All fourteen primary embedded raw-source blocks match
  their sources; both continuation PNGs also match their complete source. See inventory.json.
- Frozen Bundler 2.5.11 / actual Jekyll theme build passes. All 302 local DG target occurrences resolve;
  baseline rendered fragments are retained, with no duplicate IDs. Built image bytes match source assets.
- Actual 1280×720 and 760×900 themed context inspected for changed model/sequence figures. Domain
  and value views render at native widths rather than being shrunk to fit. All sixteen images load;
  no horizontal page overflow. Direct full-size access supplements ordinary reading.
- Native PNG/source inspection covers all fourteen figures, including #500's separated PersonRecord
  counts, #499 retrieval/UI interfaces/relationships, Logic keywords/compartments, PeopleSelection's
  underlined static resolver and counts, current guarded-storage model and the qualified state example.
  Compared visible class/interface/multiplicity/dependency and sequence opt/alt/ref notation with the
  live course reference sheets. Architecture retains its explained informal legend. Source checks
  confirm Lesson and Attendance value fields and existing command/query transaction paths.
- An early preview cached the older PersonRecord image. Final integrated image checks use a fresh
  no-store origin and confirm the actual 566×434 #500 PNG; that stale observation is not acceptance.
- Repository text scripts and diff whitespace checks pass. No Java behavior changed; no Java tests run.

## Print evidence and remaining gates

### Accepted A4 diagram export

Vincent's final Chrome PDF was created 27 October 2026 at **08:22:33 SGT**.
It has 99 A4 pages (594.96×841.92 points), 3,235,478 bytes and SHA-256
`5721ab03fed12a65f32431027a85c0eca69eb9bcd0f4ba3017e7e82d1c81ffc9`.
The earlier settings screenshot records portrait, 15 mm margins, Actual size,
headers/footers and background graphics off. Compared candidate: `a2860c6`.
The evidence-only follow-up does not alter guide, style, source or image assets.

All **16/16** diagram images match candidate assets by decoded RGB pixels and
dimensions, including integrated #500's **566×434** PersonRecord and the intact
Logic diagram on page 19. This comparison covers displayed pixels, not PNG encoded
bytes or embedded PlantUML metadata; inventory.json covers source/image provenance.

Rendered all diagram-bearing pages. Pages 5, 7–8, 19, 21–24, 28, 30–31, 34 and 47
are pixel-identical to the earlier visually inspected second export. Inspected final
pages 16–19 directly. The corrected **UI heading, introduction and image stay together
on page 17**, without the orphan on page 16. The membership caption/image stay together
on page 22. Compact domain/value views on pages 22–23 and sequence panels on pages
7–8 and 30–31 remain whole and readable. PersonRecord counts remain visible; no
clipping or broken diagram image was observed. Logic's section heading/API introduction
are on page 18 and its intact figure follows on page 19; the UI-specific grouping does
not claim every section introduction and subsequent illustration share one page.
Selected exact final PDF page renders are retained in `print-pages/`.

**PASS within scope:** diagram readability, corrected figure grouping and current-image
identity in the actual A4 export. This is not an editorial audit of all 99 pages,
a physical-paper test, non-author review or post-merge publication verification.
Frozen actual-theme rebuild, all 302 local targets, preserved `ui-component` anchor,
all sixteen browser-loaded diagram images and no horizontal overflow also pass.

### Superseded print attempts

Preserve identities/results in `print-verification-first-export.json` (07:30:21,
old cached PersonRecord), `print-verification-second-export.json` (08:18:13,
16/16 current images but an orphan UI heading), and `print-verification-third-export.json`
(08:20:34, UI grouping corrected but Logic failed to load into that export).
The final accepted export resolves those findings. The UI correction groups the existing
heading/introduction/image inside `dg-figure`, with Kramdown `markdown="1"` preserving
its original heading ID. No Java or diagram semantics changed for this follow-up.

Obtain substantive non-author rendered/notation review of this increment, current hosted checks and
eligible integration. Then compare actual public PNG bytes, links and guide context with accepted assets.
No post-merge published-byte acceptance is possible before integration; #393 stays open. Existing owner,
v1.5 and overdue 26 October 18:00 SGT completion are unchanged; Monday's cutoff was missed.

Course references: [UML sheets](https://nus-cs2103-ay2627-s1.github.io/website/admin/uml-reference-sheet.html),
[DG guidance](https://nus-cs2103-ay2627-s1.github.io/website/admin/tp-deliverables.html).
