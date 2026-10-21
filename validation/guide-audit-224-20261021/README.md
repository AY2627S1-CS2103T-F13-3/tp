# Round 1 guide audit — #224

Prepared by Ernest Chua with OpenAI Codex assistance on 21 October 2026 (SGT).
Audited team master: `9a2f8cd905ae6d51857e5d84661e0e91f8e7e265`.
Scope: independent Round 1 preparation for [#224](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/224).
Its **20 October 2026, 15:00 SGT** completion deadline remains overdue and unchanged.

**Round 1 preparation is complete. Final #224 acceptance remains pending #238,
the residual owner actions below, and substantive non-author review.** This is
preparatory evidence, not an issue closeout, merge, release publication or final-candidate approval.
The application, guide text, author contributions and shared Google document were
not changed by this audit.

## Course and completion checklist

The live [course schedule](https://nus-cs2103-ay2627-s1.github.io/website/) and
[Week 10 project page](https://nus-cs2103-ay2627-s1.github.io/website/schedule/week10/project.html)
were retrieved on 21 October. This is Week 10, starting 19 October: v1.4 alpha is
due **22 October 2026, 23:59 SGT**. The individual DG UML update is recommended;
its separate weekly checkpoint is **23 October, 16:00 SGT**. These course dates
do not replace #224's missed team deadline. The live
[GitHub workflow requirements](https://nus-cs2103-ay2627-s1.github.io/website/admin/appendixE-gitHub.html)
were also checked. Search-service fetches missed its cache; direct HTTPS retrieval succeeded.

- [x] Map all current guide/site/support content and original owner evidence.
- [x] Check reader paths, active/future labels, shared contracts and attribution.
- [x] Inventory current source links and the five individual UML contributions.
- [x] Build the locked Jekyll site and inspect desktop/narrow rendered content.
- [x] Replay one integrated guide journey, reusing other owners' existing checks.
- [x] Inspect the shared drafting material and record bounded owner actions.
- [ ] Reconcile final namespace/source/runtime references after merged #238.
- [ ] Recheck affected render/examples on the final integrated documentation SHA.
- [ ] Obtain non-author review and record the final handoff to #239/#271.

## Coverage map

All paths below are relative to the repository root. Historical release records
and dated meeting notes are evidence, not current product instructions.

| File or section | Existing owner/task | Result at audited SHA |
| --- | --- | --- |
| `README.md`, `docs/index.md` | Ben #223, baseline #102/#152 | Product/audience, fictitious Lesson-board media, Releases, source-build and UG/DG routes are clear. Current editing/board features are distinguished from published v1.3. |
| `docs/AboutUs.md` | Ben #223, original feature authors | Five real members/roles retained; person-edit activation and underlying model authorship are distinguished. Portraits and shared-contract link resolve. |
| `docs/team/johndoe.md`, `_data/projects.yml` | Ben #223, Yang #237 | Removed from the integrated source; no sample portfolio is rendered. Current templates have no remaining sample reference. |
| UG conventions, selectors, summary, help, planned-scope reference | Ernest #216/#265 | Early selector rules, one authoritative command-summary table, retained old anchors, active edit routes and protected-session limits agree. Future proposals remain labelled. |
| UG people and position/ID example; DG introduction, reading path, people APIs/requirements | Zhi Yu #215; original people/edit authors | Overview leads to architecture, active command, source and tests. Displayed positions are distinguished from stable IDs. Existing editing/authorship contracts are preserved. |
| UG search/retrieval; DG indexed queries/view selection/refresh and use cases | Ben #218 | Same-child/same-Lesson conjunction, independent panels and save-before-refresh explanation agree with current query/session APIs. Worked example is shared through links. |
| UG Lesson/enrolment/attendance; DG domain/glossary/NFRs/manual checks | Yang #219/#264/#269 | Shared roster ownership and retained dated history agree with source. New Lesson edits are active; cancellation, make-ups and richer attendance remain proposals. |
| UG saved data/recovery; DG Storage, transactions, schema and recovery tests | Vincent #217/#267, logging #235 | Safe retry/backup guidance, protected loading, separate preferences and retained history remain intact. Operational save and live publication are distinguished. |
| DG architecture/command sequence/Logic API/help | Ernest #220/#234/#280 | Current canonical components and staged execution replace retired execution diagrams; parsers, executor and transaction source links resolve. |
| `docs/SettingUp.md`, `Testing.md`, `Logging.md` | Zhi Yu #221, Ernest #260, Vincent #235 | Current Main class/test examples exist; Java 25/assertion/report instructions agree with build/source. Log reservations and console fallback remain documented. |
| `docs/Documentation.md`, `DevOps.md` | Vincent #222/#271 | Locked preview instructions work. Product/wrapper versions, source links, package evidence and release steps are distinguished. Recheck Linux workflow wording after #349. |
| `_config.yml`, `_includes/*`, `_layouts/*`, `_sass/*`, `assets/css/style.scss` | Ben #223, baseline site/branding owners | PonHub title/navigation/print branding, responsive figures and scrollable tables retained. All nine generated pages checked. No configuration/dependency edit. |
| DG acknowledgements, current API comments and provenance | Ernest #277, feature authors | One DG Codex acknowledgement records tool, all five users and assistance extent. Upstream/library credits remain. Representative session/parser/query/transaction comments describe active APIs. |
| Referenced images and editable diagram sources | Five authors below, #237/#236 | All local image targets load; all 13 source/render metadata pairs agree. Twelve diagram pairs are embedded; one obsolete unembedded pair remains for reconciliation. |
| Shared collaboration draft | #224 coordinates; feature owners retain corrections | Four text-tab exports checked; two other tabs show empty-document UI. Residual live-spec drift is recorded below; dated notes/proposals were preserved. |

## Five individual UML contributions

Every source below is in `docs/diagrams/`, with its same-named PNG in
`docs/images/`, embedded in `docs/DeveloperGuide.md`. Authorship comes from
the existing commits/PRs; this audit does not claim those contributions.

| Author/task | Source/render contribution | Integrated evidence |
| --- | --- | --- |
| Zhi Yu #215 | `PeopleSelectionClassDiagram` | [#309](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/309), implementation `b8cde80f`, merge `48c61071` |
| Vincent #217 | `CanonicalStorageClassDiagram` | [#299](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/299), implementation `bd60bd36`, merge `ad38e10a`; #296 provides preceding recovery consolidation |
| Ben #218 | `LessonRetrieval`, `LessonQueryRefresh` | [#352](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/352), implementation `506ca031`, integration `a828eef7` |
| Yang #219 | `LessonAttendanceStateDiagram`; existing person/membership/attendance class figures | [#340](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/340), `a239ae39`/`43a493b4`, merge `87cb7755`; earlier domain work `fa076a71`/#232 |
| Ernest #220 | `ArchitectureDiagram`, `ArchitectureSequenceDiagram`, `LogicClassDiagram` | [#355](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/355), implementation `3a40bdcc`, merge `1e6c76fe` |

Original owner/reviewer checks are retained in those PRs and their issue records.
For example, #217 records the failed-save retry example and focused regressions;
#220 records its integrated Windows suite and browser checks; #269 records Lesson
edit source-session checks. They are prior evidence with their original SHA/runtime,
not tests rerun here. Independent teammate review of this audit remains outstanding.

## Checks actually performed

- **Locked site build: PASS.** Existing Ruby 3.3 container, cached gems,
  Bundler 2.5.11, Jekyll 3.10.0; from `docs`,
  `BUNDLE_FROZEN=true bundle _2.5.11_ exec jekyll build --baseurl /tp`.
  The first cached bundle volume lacked Bundler; the existing gem volume succeeded.
  Gemfile/lockfile were unchanged. Non-blocking Faraday retry notice only.
- **Local links/tables/source paths: PASS.** `python validation/guide-audit-224-20261021/check_links.py`:
  nine HTML pages, 412 local targets, 30 image-target references, 26 tables,
  five README targets and 52 source-link occurrences (42 unique paths):
  51 `blob/master` file references and one `tree/master` directory reference.
  No missing file/fragment or inconsistent table columns. See `links.json`.
  Source references resolve against the audited checkout; this is not a remote
  HTTP check of every GitHub blob URL or a Java semantic proof.
- **External entry/support links: PASS.** All 37 distinct non-issue/non-blob
  HTTPS links selected from the nine pages returned HTTP 200 after redirects.
  See `external-links.json`; external fragments are not validated by HTTP status.
- **UML: PASS.** PlantUML 1.2026.2 freshly rendered all 13 diagram sources
  into a temporary directory; `style.puml` is include-only. Original source
  extracted from every tracked PNG matches its paired `.puml` exactly.
  See `uml.json`. Rendered bytes may differ by platform/fonts; tracked images
  were preserved. The obsolete pair below is not current-design acceptance.
- **Rendered Chrome checks: PASS at this SHA.** Exact `/tp` build served locally
  at `http://127.0.0.1:8775/tp/`. All nine pages measured at 2052x986,
  800x900 and 390x844: no page-level horizontal overflow or broken local image.
  At 390px the UG summary scrolls inside its table (344px client, 433px content),
  without widening the page. Keyboard Space opens/closes mobile navigation and
  exposes guide links. Viewport override restored to 2052x986.
  Screenshots were visually inspected for the entry page, architecture/component
  table, canonical sequence, people guidance/selection figure, storage, query
  refresh, domain figures/attendance state and mobile UG summary. Screenshots
  are in the browser session, not separately retained files. A Codecov badge
  was initially still loading; no missing local asset was observed.
- **Reader path: PASS for the current source.** Visited site →
  `DevOps.html#build-automation` → `UserGuide.html#quick-start` and
  `#current-command-summary`; DG `#maintainer-reading-path` → `#architecture`
  → `#canonical-command-collaboration`/`#canonical-people-commands`
  → `#command-transactions` and the existing routing/integration-test links.
  Ordinary operation does not require reading coordination issues.
- **Representative guide replay: PASS.** `GuideWorkflow.java` runs the actual
  canonical session with a fresh disposable store and asserts the UG journey:
  tutor/two Students/shared L1 → enrol both → mark/correct → unenrol → retained
  former-member history → unmark/no-op → guarded deletion → add/delete empty L2
  → restart with stable IDs and Jamie's current-member absence retained.
  Rejected deletion/no-op preserve file bytes. This also follows Yang's DG
  attendance-state example. Exact output: `workflow.txt`.
- **Documented focused test command: PASS.** Oracle JDK 25.0.4 on Windows,
  `gradlew.bat test --tests 'seedu.address.logic.PersonIndexResolverTest'`:
  see `focused-test.xml`. Compilation and the selected tests pass. Full suite,
  package/native display/scaling and final-candidate acceptance were not rerun.
- **Repository formatting: PASS.** Staged `.github/run-checks.sh` and
  `git diff --cached --check`. No product code or guide behavior change.

To replay the evidence harness, compile the project's `classes`, obtain its
`sourceSets.main.runtimeClasspath.asPath` using a temporary Gradle init task,
then compile `GuideWorkflow.java` with that classpath into a disposable output
directory. Run `seedu.address.logic.GuideWorkflow` with that output directory
plus the runtime classpath, supplying a previously nonexistent JSON path.
This exercises production routing/storage without launching JavaFX or a JAR.

## Residual findings and handoff

1. **Final source references — Zhi Yu #238, then Ernest #224.**
   `namespace-links.tsv` lists all 47 guide source-link occurrences containing
   `seedu/address`; they are valid now. Also update plain class names in
   SettingUp's Application configuration, DG assertion/test commands and
   Testing's `--tests` filter when the namespace actually moves. Preserve
   `-ea`, guide anchors, JSON/legacy markers and the retained dated evidence.
   This dated `GuideWorkflow.java` also needs new imports/package if rerun
   after #238; its baseline transcript remains historical.
2. **Shared working draft — original feature owners; #224 tracks reconciliation.**
   [Document](https://docs.google.com/document/d/1B47n0kNPYl-F-5dwM7dXYSN4ukbJ3SCnR2uNC0wkuAo/edit):
   `PonHub Feature Specification` (`t.jjia1q9gvtk9`) is the named working spec;
   the `Copy of` tab is a separate competing copy, not an authoritative guide.
   Ask owners to mark that copy historical and point the working spec directly
   to the repository UG/DG. Yang's working Add/Delete Lesson and Mark/Unmark
   sections still select per-student lessons, and have no explicit enrol/unenrol
   feature flow. Reconcile shared Lesson IDs, roster/history and active Lesson
   editing. Ernest's Help topic enumeration omits active `edit`/`editlesson`;
   its COMMAND placeholder cleanup is already present. Zhi Yu's earlier shared
   editing-status paragraph is still conditional on activation and should now
   reflect the merged routes. Ben's current retrieval note/reference is present
   in the meeting-note working-guide block; keep its same-Lesson/view rules.
   Generic feature templates remain intentional templates. Dated notes and AB3
   extension proposals stay historical. `shared-draft.json` identifies four
   text exports without copying personal meeting/contact information. Embedded
   object contents were not audited; no shared-document edits were made.
3. **Unused retired figure — Vincent #239 coordinates with original #217/#237 owners.**
   `docs/diagrams/StorageClassDiagram.puml` and `docs/images/StorageClassDiagram.png`
   remain tracked and describe removed legacy classes. They have no tracked
   guide/template/style/config consumer and are not embedded in the built site.
   The active figure is `CanonicalStorageClassDiagram`. Record a bounded
   cleanup decision; do not relabel the old figure as the new architecture.
4. **Release runbook — Vincent #271/#349, then #224 checks the changed paragraph.**
   DevOps accurately describes frozen v1.3 workflow defaults at this baseline.
   When #349 changes trigger/ref/hash handling, reconcile that paragraph with
   the merged workflow. Keep current-source/published-version wording accurate
   when v1.4 is actually published; #273/#274 own final media/download checks.

## Round 2 finish forecast

Preparation can be handed over now. After reviewed #238 integration and receipt
of the bounded owner corrections, allow approximately **30–45 minutes of focused
auditing** to rebuild, rerun the link inventory and changed examples, inspect the
affected anchors/source links, record the final SHA and reconcile findings.
This is a recovery estimate, not a moved deadline or a promise of reviewer/CI
availability. Wall-clock completion remains dependent on those inputs and
non-author review. Keep #224 open; hand its final evidence to #239/#271 only
after the remaining gates pass. #317/#321 and later #270/#272 acceptance remain
separate workstreams.

## Round 1 inventory correction — 21 October 2026

Addressed [the non-author directory-link finding](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/358#discussion_r4338085605).
The first inventory counted 51 blob links / 46 namespace occurrences but omitted
the DG `model.lesson` directory URL. The helper now collects both `blob/master`
and `tree/master`, requires files/directories respectively, and records both
old `seedu/address` and future `ponhub` Java paths. The regenerated inventory has
**52 source references, 42 unique paths and 47 namespace occurrences**, including
`src/main/java/seedu/address/model/lesson`. The product/guide source remains the
audited `9a2f8cd9`; `links.json` identifies the report commit used for this rerun.

The locked Jekyll build and complete local link/table/target pass were repeated.
Two temporary negative probes correctly rejected a missing directory and a file
used as a tree target; the DG bytes were restored exactly before the final pass.
This corrects audit coverage; it does not implement or accept #357's namespace move.

Connected Chrome inspected the rebuilt DG `#lesson-scheduling-value-contracts`
section and its actual `model.lesson` href at **2052x986** and **390x844**.
Screenshots show readable prose/code/link wrapping; no page-level horizontal
overflow or broken local images was observed. The narrow anchor was activated
with Enter and reached the intended heading. Temporary viewport override was reset.
These follow-up checks apply to unchanged guide content at `3b7dd8f8` (product
baseline `9a2f8cd9`), not a rendered post-#357 acceptance.
