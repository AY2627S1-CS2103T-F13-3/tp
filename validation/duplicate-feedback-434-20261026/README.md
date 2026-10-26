# Duplicate-edit guidance verification

Ernest Chua, assisted by OpenAI Codex, 26 October 2026.
Product/UG commit: `0a0f57f3`, based on team `127012c0`.

The edit command now explains the role-specific duplicate fields and the
prefixes to correct. Students use normalized name plus parent phone; Tutors
and Parents use normalized name plus their own phone. The existing duplicate
guard and model are unchanged. Ben retains original model/finder credit.

- 25 focused edit-candidate/model/session tests pass, no failures/errors/skips.
  New coverage exercises all three roles, normalized name conflicts, Students
  with different levels/own phones, unchanged aggregate/view/file bytes, zero
  save/list notifications and retention of unrelated validation messages.
- Both Checkstyle tasks and `shadowJar` pass. Repository text/diff checks pass.
- Frozen Bundler 2.5.11/Jekyll 3.10.0 build passes on WSL Ubuntu Ruby 3.3.8.
  Gemfile and lockfile are unchanged. The build uses cached dependencies under
  the WSL user's cache; Docker's local startup error did not block this render.
- All 496 local link/asset targets across ten generated HTML pages resolve;
  rendered IDs have no duplicates. The served UG bytes match the saved render.
- Actual connected Chrome screenshots inspected at **2034x1024** and
  **760x900**: edited paragraph, syntax, example and recovery text wrap without
  page overflow. Narrow page width is 745px; all images load. Temporary viewport
  override reset. These are local-preview checks, not deployed-site acceptance.
- Exact packaged native identity is in `identity.json`. JavaFX Robot entered
  real additions/edits for Students, Tutors and Parents. Each duplicate gives
  the expected guidance, retains the rejected command and preserves saved bytes.
  Changing `pp/` succeeds; an unchanged self-edit remains a no-op. The actual
  Student and Parent captures were inspected; Tutor text was asserted as well.
  The feedback divider was dragged with native mouse input; no resize outcome
  is inferred from the drag. Window dimensions were set to 1000x700 by the
  diagnostic, then allowed to settle before input.

Runtime: Oracle Java 25.0.4+7, Windows 11 amd64, JavaFX 17.0.7; display
2048x1152 logical at 125% (2560x1440 physical). Disposable fictional fixtures,
separate diagnostic agent, unchanged application JAR, process closed. No user
data/display settings changed. No independent human, other-platform/scaling,
final-candidate or publication acceptance is claimed. The #451 packaging PR is
separate; this branch is independently based on team master.

Run `Run-Native.ps1 -JarPath build\libs\ponhub.jar -EvidenceDirectory PATH`
from the repository root to reproduce. A first local attempt caught a harness
layout-pulse race after resizing and stopped before command execution; the
runner now waits for layout before native input. The successful final evidence
is retained here, with only text whitespace/line endings normalized.
