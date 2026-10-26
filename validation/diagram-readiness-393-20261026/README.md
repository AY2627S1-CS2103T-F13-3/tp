# Diagram verification preparation — #393

Prepared by Vincent (@gugrusaurus), with OpenAI Codex assistance, on 26 October 2026.
This is a baseline and execution plan, **not final diagram acceptance**. #393 remains open, due
26 October 2026, 18:00 SGT (UTC+8), with the unchanged 20:00 source/material cutoff in #327.
Dock polish (#428) follows correctness work.

Baseline: team `177d4e368f54aec027a927031378f84ee5fcb93a`. No diagram, guide, shared style or
Java source is changed by this preparation. The original authors retain their figures and feature sections.

## Preparation completed

- Inventoried all 12 maintained source/PNG pairs, their SHA-256 hashes, native dimensions, DG source
  references and direct full-size links in `inventory.json`; also recorded guide/shared-style hashes.
  Text hashes normalize line endings to LF so Windows checkout conversion does not change provenance;
  PNG hashes use exact bytes.
- All 12 PNGs' first embedded raw PlantUML blocks match their editable sources. This does not establish
  expanded-style freshness, semantic accuracy, notation compliance or readability.
- All 12 sources render to fresh SVGs in an isolated temporary directory with PlantUML 1.2026.8,
  Oracle Java 25.0.4.1 and headless AWT on Windows. No submitted PNG was replaced. Render success is
  a syntax/tooling check; it is not pixel reproducibility or visual acceptance.
- Three figures currently have direct full-size access; nine do not. Links can help inspection, but
  are not a substitute for readable default-page and print figures.
- Checked the current dependency PR heads and review states below. They remain outside the baseline.

## Integration gates

| Dependency | Observed head | Remaining gate before final #393 pass |
| --- | --- | --- |
| #442 / #390 — retrieval and UI | `9e9fa05c83dca368d256ed376d986a96c44ed14b` | Open. Existing content review identifies an unresolved branch-policy handoff. Owner/coordinator resolution, eligible review/integration, then final rendered inspection. |
| #456 / #396 — query refresh | `e9896fb398ab2d464b9a2dd441fe53248eea0af6` | Open; no submitted review at this check. Substantive non-author review, current checks and integration. |
| #462 / #444 — people selection | `4f515cd459c1adc672b242196f18886e7c298f7a` | Open; no submitted review at this check. Review/integrate the label separation, then inspect the actual combined figure. |
| #466 / #443 — person record | `ed718f639d6dfeaf879ed71529ce1d35a90746aa` | Open, changes requested: ContactDetails/Address/Phone labels still touch connectors/arrowheads. Owner correction and substantive re-review remain. |
| #472 / #413 — storage model | `87b1c6b1a72f2406a89aff117a36933c5ecfff68` | Open. Storage prose/source/PNG and two-window manual test are corrected at this head; the earlier requested-changes review awaits re-review. Integrate before accepting the active storage model. |

These are snapshots, not reserved reviewers or permission to merge. Re-read each PR at pickup.
Historical #385/#391/#392/#394/#395 stay closed; verify their corrections survive the final combination
without treating old approvals as approval of later figure changes.

## Figure-by-figure pass prepared

Dimensions below describe the baseline PNGs, not pending owner corrections. Existing issues' findings
are inspection targets, not new claims that this preparation has independently re-audited their notation.

| Figure | Baseline pixels | Full-size link | Next inspection target |
| --- | --- | --- | --- |
| ArchitectureDiagram | 689 × 579 | No | Preserve the explained informal legend; check reader value and page/print fit. |
| ArchitectureSequenceDiagram | 908 × 1079 | No | Outcome/frame geometry, arrows and activation boundaries; inspect long-figure print breaks and ordinary-width labels. |
| UiClassDiagram | 875 × 393 | No | After #442: retained references versus creation, visible abstract/classifier notation and updated caption. |
| LogicClassDiagram | 554 × 622 | No | Visible interface keywords, compartments, dependency endpoints and standard note treatment. |
| PersonRecordClassDiagram | 473 × 434 | No | After corrected #466: every multiplicity, realization triangle and connector at native and page scale. |
| LessonMembershipClassDiagram | 942 × 323 | No | Ownership/sharing ends and crowded counts; prepare owner-coordinated reflow if default-width text remains small. |
| AttendanceClassDiagram | 897 × 304 | No | Readable counts/notes and supported history relationships; preserve permitted omitted compartments. |
| LessonAttendanceStateDiagram | 618 × 640 | No | Worked-example setup/endpoint qualifications, transition labels and page breaks. |
| LessonRetrieval | 1106 × 583 | Yes | After #442: actual interface targets/keywords, unconnected notes, responsibilities and default-page readability. |
| LessonQueryRefresh | 839 × 839 | Yes | After #456: callback-spanning activation/return geometry, role-header qualification and page/print reading. |
| CanonicalStorageClassDiagram | 965 × 391 | Yes | After #472: active guard/codec/writer path and optional `0..1` reference; inspect the new 737 × 467 asset. |
| PeopleSelectionClassDiagram | 627 × 550 | No | After #462: unchanged `0..*` clear of its curve, sharing counts, visible interface and static resolver notation. |

## Final-pass procedure

1. Record the actual integrated commit after each prerequisite clears. Re-run the inventory and compare
   hashes with this baseline. Reconcile source ownership and the issue/PR map before inspecting changed
   figures. Do not accept a pending branch as integrated evidence.
2. Trace the final figures to relevant implementation and inspect the actual submitted PNGs against the
   live course UML reference/textbook. Check classifier labels, relationship kinds/directions/counts,
   attributes/operations/notes, static/abstract notation, and sequence/state geometry where relevant.
   Preserve allowed simplifications and the explicitly informal architecture legend; do not invent members,
   reverse counts or a complete dependency graph. Source declarations and successful rendering are insufficient.
3. Build the actual guide and inspect all 12 figures in context at 1280 × 720 and 709 × 849, recording the
   real content width, label legibility and asset identities. Inspect print preview, recording paper/orientation,
   scaling and page breaks. These trials are **not performed by this preparation**. If labels are unclear,
   coordinate a focused source/layout/font change with the figure owner and regenerate its matching asset.
   Add full-size access where useful; do not shrink labels to make a dense figure fit.
4. Recheck all affected figures after any shared-style change, not only those whose raw `.puml` text changed.
   Preserve existing DG fragments. Obtain substantive non-author review of Vincent's own figure changes;
   coordinator checks do not supply independent approval.
5. After merge, compare affected public image bytes/links with the accepted source and inspect the deployed
   guide. Record remaining defects honestly; keep owner issues/#393 open for unfinished work. Tuesday's
   candidate/native/release/download checks remain separate.

## Repeatable preflight

From the repository root, using Python 3.10 or later (standard library only):

```text
python validation/diagram-readiness-393-20261026/check_diagrams.py
python validation/diagram-readiness-393-20261026/check_diagrams.py --root PATH_TO_FINAL_CHECKOUT --output fresh-inventory.json
```

The checker fails for a raw-source mismatch or an unreferenced maintained figure. It has no network calls,
font-size threshold, figure-count quota or automatic UML/readability verdict. A changed shared-style hash
requires expanded-style/render checks even when raw source matching passes. Generated SVGs and runtime
dependencies are temporary local artifacts, not committed assets or public-page evidence.

Final evidence must identify the checked commit, affected source/PNG hashes, actual viewports and print
settings, observed result/limitation, non-author review, and post-merge public verification. All final visual,
semantic, print and publication checks are currently pending.

References: [#393](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/393),
[course diagram guidance](https://nus-cs2103-ay2627-s1.github.io/website/admin/tp-deliverables.html#dg-tips),
[UML reference sheet](https://nus-cs2103-ay2627-s1.github.io/website/admin/uml-reference-sheet.html).
