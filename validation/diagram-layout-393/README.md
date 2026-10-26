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

Vincent supplied a 99-page A4 Chrome PDF, `Developer Guide _ PonHub.pdf`, created
27 October 2026 at 07:30:21 SGT. Size: 3,241,031 bytes; SHA-256:
`90d1712a359da68a7e3de48125011626b9f92392764c2eb30c59dfadc7be769e`.
Its A4 dimensions are 594.96×841.92 points. The earlier screenshot records portrait,
15 mm margins, Actual size, headers/footers and background graphics off.

Rendered and visually inspected every diagram-bearing page: 5, 7–8, 17, 19, 21–24,
28, 30–31, 34 and 47. The corrected membership caption and image are together on
page 22. Both compact domain/value pairs retain visible endpoints and counts on
pages 22–23. Both Architecture panels on pages 7–8 and query panels on pages 30–31
fit whole, with readable guards, repeated lifeline headers and the query activation.
No clipping or new caption/image separation was observed in those changed figures.
Selected exact PDF page renders are retained in `print-pages/`; page 21 is explicitly
stale evidence, not an accepted current figure. This is a diagram-page inspection,
not an editorial audit of all 99 pages or a physical-paper reading test.

Decoded RGB pixel hashes and dimensions match **15 of the 16** current candidate diagram
images. These compare displayed pixels, not PNG file bytes or embedded PlantUML source
metadata; source/image provenance remains covered separately by inventory.json.
The exception is page 21: its PersonRecord image is the older cached **473×434** render;
the integrated #500 candidate is **566×434**. The supplied PDF therefore demonstrates
that the changed layouts print correctly, but cannot certify the complete current
candidate. A fresh export from the no-store origin `http://127.0.0.1:9394/DeveloperGuide.html`
has been requested. See `print-verification.json` for reproducible identities and page mapping.
Keep the PR draft until this remaining candidate print check is reconciled.

Obtain substantive non-author rendered/notation review of this increment, current hosted checks and
eligible integration. Then compare actual public PNG bytes, links and guide context with accepted assets.
No post-merge published-byte acceptance is possible before integration; #393 stays open. Existing owner,
v1.5 and overdue 26 October 18:00 SGT completion are unchanged; Monday's cutoff was missed.

Course references: [UML sheets](https://nus-cs2103-ay2627-s1.github.io/website/admin/uml-reference-sheet.html),
[DG guidance](https://nus-cs2103-ay2627-s1.github.io/website/admin/tp-deliverables.html).
