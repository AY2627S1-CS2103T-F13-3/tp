# Canonical command UML — issue #220

Prepared by Ernest Chua with OpenAI Codex assistance on 21 October 2026.
Baseline: team master `a828eef7366d3d2b7eff3000c1d8f324396603f5`.
The original 19 October 2026, 23:00 SGT deadline remains overdue and unchanged.

## Current integrated result

#350 merged into team master as `b9cfd2d9` during this task. A normal merge
integrated it into this branch at `1ff33856`. Full `check coverage shadowJar`
then passed: 896 tests, zero failures/errors, 20 platform-assumption skips,
both Checkstyle tasks, coverage and packaging. The earlier failure below is
historical and resolved. No exception or threshold change was used.

Jekyll was rebuilt after integration, all three figures loaded, and connected
Chrome rechecked the sequence and Logic guidance at 2052x1042 without overflow.
The guide sources/assets are identical to the previously inspected 800x900 tree.

## Source reconciliation

- Replaced ArchitectureDiagram, ArchitectureSequenceDiagram and LogicClassDiagram
  with current responsibilities, boundary collaborators and staged edit execution.
- Removed ComponentManagers, DeleteSequenceDiagram and ParserClasses source/render
  pairs after confirming their only consumers were the obsolete DG passages replaced here.
- Checked `CanonicalPeopleSession.execute`, `CanonicalEditCommand`,
  `CanonicalCommandExecutor.execute`, `PeopleView.beginTransaction`,
  `CanonicalTransaction.commit` and `ProtectedPonHubDataStorage.saveData`.
  The session rejects protected mutations before argument parsing; the executor
  saves a changed candidate before publication. Read-only/no-op routes do not
  write operational storage. `editlesson` shares the executor.
- Logic.java already has current people/lesson/history API wording. No stale
  activation comment remained to change. No Java source or behavior is changed.
- Model/storage/query owners' contracts and assets are preserved and linked.
  Namespace/source-link finalization remains #238.

## Checks

- PlantUML 1.2026.2 renders all three editable sources to PNG without errors.
- Repository workflow: Ruby 3.3 container, locked Bundler 2.5.11, Jekyll 3.10.0;
  `bundle _2.5.11_ exec jekyll build --baseurl ''` passes.
- 412 local page/fragment/image targets across nine rendered HTML pages pass;
  all 32 image references, 16 DG tables and ten shared anchors pass.
  No tracked consumer refers to a removed asset.
- Connected Chrome screenshots and DOM checks inspect Architecture, Canonical
  command collaboration and Logic component at 2052x1042, plus sequence/Logic
  and figure bounds at 800x900. Figures load, labels and prose are readable,
  and no horizontal page overflow occurs. Viewport override restored.
  Exact local render: docs/_site, served at http://127.0.0.1:8769/.
- Oracle JDK 25.0.4, project JavaFX 17.0.7, Windows 11 build 26200:
  CanonicalCommandExecutorTest, CanonicalEditIntegrationTest,
  CanonicalLessonEditIntegrationTest and CanonicalPeopleSessionTest pass:
  33 tests, zero failures/errors/skips.
  Both Checkstyle tasks pass.
- Full exact-tree `check coverage shadowJar`: 896 tests, one failure, 20
  platform-assumption skips. The sole failure is the unchanged compact
  HistoryListPanelTest logical-bounds assertion already reproduced and corrected
  in #350. This is a failed check, not a successful full build or a waiver.
  #350 is ready with four green head checks; its merge remains separate.
- Repository formatting scripts and `git diff --check` pass.

## Remaining gates and forecast

This increment is ready for first-available non-author review and CI. Integrated
completion needs review/merge and #224's final guide verification. Forecast:
review/CI pickup can start immediately; no teammate availability or merge time
is assumed. #220 stays open until those gates pass. No deadline or scope change.
