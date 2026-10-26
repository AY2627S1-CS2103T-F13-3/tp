# Command recall verification

Ernest Chua, with OpenAI Codex assistance, 26 October 2026.
Product/help/guide commit `35003250`, independently based on team `127012c0`.
The follow-up adds modifier/blank-submission regressions and this evidence;
it changes no product bytes. Original arrow-key idea credit remains with
Ernest, with Yang Shuo's broader command-reuse suggestion retained in #414.

- 18 focused CommandBox/completion/catalogue tests pass, no failures/errors/skips.
  They cover chronological traversal, oldest/newest boundaries, exact unfinished
  draft and selection restoration, no execution on recall, unsuccessful commands,
  editing a recall, Enter, Tab, all modifier guards, blank submissions, 50-entry
  eviction and isolation between CommandBox instances. Both Checkstyle tasks
  and `shadowJar` pass. Repository text/diff checks pass.
- Exact native JAR: 12,897,800 bytes, SHA-256
  `a94db434b1967c077c4d6555f8d1c35fa0803d16cb062f00b90431caf36559f5`, manifest
  1.4. It was copied unchanged into a fresh writable folder and launched using
  the retained separate JavaFX Robot diagnostic. Real mouse/keyboard input
  verifies Up/Down ordering and boundaries, preserved text/caret/selection,
  unchanged feedback/no operational data write, Tab completion, failure recall,
  edited Enter submission and retained command focus. Actual draft-selection
  screenshot inspected. A second launch from the same app folder verifies
  that Up cannot recall the previous process's history.
- Native runtime: Windows 11 amd64, Oracle Java 25.0.4+7, JavaFX 17.0.7;
  740x600 window, approximately 726x562 scene; 2048x1152 logical display at
  125% (2560x1440 physical). Both processes closed. User data, clipboard and
  display settings were untouched. No other platform/scale or independent
  human/final-candidate acceptance is claimed.
- Frozen Jekyll build passes on WSL Ubuntu Ruby 3.3.8/Bundler 2.5.11 with the
  unchanged Gemfile/lockfile. All 498 local link/asset targets across ten HTML
  pages resolve; no duplicate IDs; served UG bytes match the saved render.
  Actual Chrome inspection at 2034x1024 and 760x900 covers recall wording,
  nearby command summary and DG manual steps; text wraps without page overflow.
  The viewport override was reset. Screenshots are local-preview evidence.

Reproduce using `Run-Native.ps1 -JarPath build\libs\ponhub.jar
-EvidenceDirectory PATH` from the repository root. Text logs have only
line endings/trailing whitespace normalized. The first local harness expected
the list-help text to begin with `Usage: list`; the actual catalogue uses
`Use list or list r/`. That mistaken assertion was corrected before the final
successful run. The original attempt remains under `_temp/round1/native-414`.

The #451 packaging fix and final release identity remain separate. No merge
or release publication is claimed by these local checks.

## Current-master reconciliation

After #438 merged, `bd1f2d53` merges team `2de4faa7` and resolves the shared
manual-test insertion point by retaining both complete sections. The Unicode
procedure is byte-equivalent to master's section; the PR still changes only
its own recall behavior/guidance plus validation artifacts.

44 current recall/completion/catalogue/Name/Unicode-session/JSON tests, both
Checkstyle tasks and shadowJar pass, zero failures/errors/skips. The frozen
render passes; all 499 local targets across ten HTML pages resolve, with no
duplicate IDs or conflict markers. Actual 760x900 Chrome inspection confirms
both adjacent manual sections and the UG recall guidance; override reset.
The native recall and same-folder restart checks pass again on the new exact
JAR; updated identity, logs and captures are in `current-master/`. Earlier
evidence above remains specific to its original source, rather than being
silently relabelled as current-master acceptance.
