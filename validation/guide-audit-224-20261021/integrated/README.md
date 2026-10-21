# Integrated guide audit — #224

Audited product/guide source: team master
`f62590fe27fdd0104fa6efea0584e22ab8104ffb`, including merged namespace #357
and package/runbook #349. Prepared on **21 October 2026 (SGT)** with OpenAI
Codex assistance for Ernest's assigned #224 handoff. Original guide, feature,
UML and audit authorship is retained. This evidence does not certify the
independent final package/native acceptance in #270/#271/#272.

The parent directory preserves the earlier `9a2f8cd9` preparation and owner
handoffs unchanged. Old pending-action wording there describes that dated
baseline; the reconciliation below supersedes those remaining-action references.
The report integrates the audited team SHA through a conflict-free ordinary
merge. Its guide/product files match that team commit exactly;
`links.json` distinguishes the audit-report HEAD from the audited product SHA.

## Completed dependencies

- **Namespace/source references:** #357 is merged at `033c0dd7`. All current
  repository source links and documented application/test names use `ponhub`.
  No old `seedu/address` or `seedu.address` reference remains in README/current
  top-level Markdown guides. Historical baseline evidence retains its old names.
- **Release runbook:** #349 is merged in audited `f62590fe`. DevOps correctly
  distinguishes manual exact-source/checksum candidate verification from
  push/PR event/tooling checks. The workflow has required manual inputs without
  remembered defaults; event runs use their event SHA and calculated checksum.
  The paragraph includes person/Lesson edits, rejection, manifest/entry point,
  startup version and restart coverage, agreeing with the integrated workflow
  and verification script. Both product-version declarations now identify 1.4.
  This source reconciliation does not itself accept a final release candidate.
- **Shared Lesson correction:** Zhi Yu's
  [saved correction and handoff](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/224#issuecomment-6319690581)
  reconciles the working Add/Delete Lesson and Mark/Unmark sections against the
  merged UG/DG. The earlier active-editing status and Ernest's Help enumeration
  corrections remain saved. Original authorship and historical material remain.
- **Historical copy and repository pointers:** the
  [saved #224 owner handoff](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/224#issuecomment-6320202905)
  records the historical banner on `t.5ag24r8ilwns` and the current working
  specification header on `t.jjia1q9gvtk9`. Both point to the repository UG/DG;
  the historical header points directly to the working tab. The coordinating
  agent inspected actual hrefs, screenshots, Saved to Drive and text exports.
  This audit independently compared the retained before/after exports: each
  complete original body remains an exact suffix after its new header.
  `shared-draft-handoff.json` retains hashes/status without personal content.

## Checks rerun on the integrated source

| Check | Actual result and retained evidence |
| --- | --- |
| Locked Jekyll build | PASS; Ruby 3.3.8, Bundler 2.5.11 and Jekyll 3.10.0 in the existing Lima Linux VM; `BUNDLE_FROZEN=true bundle _2.5.11_ exec jekyll build --baseurl /tp`; `jekyll-build.log`. Project Gemfile/lockfile unchanged; retained log trailing whitespace normalized. |
| Full local render/link/source audit | PASS; existing `check_links.py`, Python 3.14.6: 9 pages, 412 local targets, 30 image references, 26 tables, 5 README targets, 52 source references (51 files + 1 directory), 42 unique source paths and 47 namespace references; `links.json`, `source-links.json`, `namespace-links.tsv`. |
| Negative source-directory probes | PASS; a nonexistent directory and a Java file used as a directory both fail with the expected missing-tree error; source and baseline reports restored byte-for-byte; `negative-probes.json`. |
| UML source/render agreement | PASS; all 13 PNGs' embedded PlantUML source matches the paired `.puml` exactly; `uml.json` includes hashes. Namespace/package integration leaves diagram bytes unchanged. No fresh PlantUML rendering was repeated; earlier fresh-render evidence remains dated. |
| Documented focused test command | PASS; prescribed Mac Azul `25.0.3.fx-zulu`, `./gradlew --offline --no-daemon classes test --tests ponhub.logic.PersonIndexResolverTest`: 7 tests, 0 failures/errors/skips; `focused-test.log` (trailing whitespace normalized), `focused-test.xml`. |
| Representative cross-guide workflow | PASS; actual production canonical session/storage on the integrated classes follows the UG quick start and DG attendance state path, including former-member history, no-op/rejected-write byte preservation, empty-Lesson deletion and restart/stable IDs; `workflow.txt`. |
| Formatting | Repository check scripts, ordinary/staged whitespace checks pass. No production/guide behavior edit is introduced by this audit. |

The source workflow was generated locally from the original parent
`GuideWorkflow.java` by replacing all three `seedu.address` occurrences with
`ponhub`, then compiled against the integrated main runtime classpath and run
as `ponhub.logic.GuideWorkflow` with a new disposable JSON path. The original
helper and historical transcript remain unchanged; the generated helper is not
an additional tracked Java implementation. Its guarded L1 deletion is rejection
while Jamie's roster membership remains, rather than an independent proof of
the attendance-only deletion guard.

The first helper attempt used macOS system Python 3.9, which does not support
`Path.write_text(newline=...)`; it failed before producing a new report.
The complete helper and probes then passed with installed Python 3.14.6.
The initial system Ruby 2.6 likewise was not used to claim a locked build.
The build used disposable VM folders and exact locked dependencies.

## Reader, browser and handoff gates

The coordinating agent inspected the exact newly built preview at
`http://127.0.0.1:8776/tp/`: DG `#lesson-scheduling-value-contracts`,
SettingUp `#setting-up-the-project-on-your-computer`, Testing
`#selecting-tests-during-development`, Logging `#finding-startup-diagnostics`
and DevOps `#verify-the-packaged-application`, at **1470x800** and **390x844**.
All ten screenshots were visually inspected. Intended anchors and `ponhub`
source hrefs were checked; no page overflow or broken image was observed.
The narrow Testing command scrolls inside its code block (358px client,
540px content). Viewport override was reset. `browser-checks.json` retains
measurements and observation attribution; two DevOps screenshots are retained.
The retained measurements identify the coordinating audit session as the
browser observer; they do not claim a second independent browser run. The coordinator also opened the deployed UG/DG successfully:
DG's `CanonicalPeopleSession` href uses `ponhub`, and UG's current command
summary heading is visible. Final release/media/download verification remains
#273/#274's separate work.

The earlier all-page, external support-HTTP and reader journey checks remain
source-specific evidence in the parent report. Unchanged guide workflow
behavior was independently replayed above; external support HTTP was not
repeated for namespace-only links or the changed DevOps paragraph.

The old, unembedded `StorageClassDiagram` pair still exists in this audited
source; the active DG uses `CanonicalStorageClassDiagram`. The bounded removal
is separately reviewed in #359 under Vincent's #239 ownership. If it merges
before final #224 handoff, record the resulting source SHA, verify only that
unused pair disappeared and rerun the affected inventory. Do not relabel the
obsolete pair as active architecture or hold unrelated unchanged diagrams to
an invented new quota.

Final technical evidence and the coordinator's rendered check are complete;
substantive non-author review of the final audit update remains required. #224 remains open
until the reviewed handoff/integration is saved. Ben cannot push to Ernest's
fork; publish an additive complete follow-up through the authorized fresh
fork-branch route, preserving Ernest's existing report commits and identifying
#358 as its predecessor. No access change or pushed-history rewrite is needed.
The original **20 October 2026, 15:00 SGT** deadline remains unchanged and overdue.
Vincent receives this guide/source disposition for #239/#271; #270/#272 still
require the exact candidate handoff and their actual platform/acceptance gates.
