Compact feedback startup (#363) -- 22 October 2026
Recorded by Ernest Chua (ErnestChuaa), with OpenAI Codex assistance.

Implementation and scope
------------------------
Application source: ec39433b7731b04fc946a33a564d3a7aa4a94c08.
Base: 84f9a2cd4865b452e14fae5748896853febc8c32.
Start the outer divider at its existing 80-logical-pixel feedback minimum.
SplitPane.resizableWithParent=false keeps window growth in the records area;
it does not disable the user's divider. A user-expanded height also survives
window growth, subject to the existing pane minima when space is constrained.
Keep the fixed outer preferred height from #317, navigation, storage, commands,
cards, horizontal divider and original authorship. No new dependencies.
UG already describes dragging the divider and scrolling long guidance without
promising a startup fraction; its wording remains applicable.

Regression and repository checks
--------------------------------
Added startup/short-feedback coverage at fresh 450x330, 853x600, 1920x1020 and
1920x1440 logical layouts, retained compact/expanded height on window resize,
and full real 'help search' scrolling from the compact startup height.
Existing divider-extreme/minima, selection/draft/F1/menu/operational-byte,
long-feedback, navigation, and roster/history drift checks remain in place.
These attached layout sizes are not actual OS scaling tests.

Before the FXML change, the two added startup/height regressions fail: 152 px
feedback at 853x600 and 362 px after increasing window height. See baseline
log/XML. The original four resize checks pass in that run.
Saved XML omits verbose system-out/system-err; log trailing whitespace is
normalized. Complete original outputs remain in the ignored local _temp folder.

Azul Java 25.0.4.1+1-LTS / JavaFX 25.0.4+100, Windows 11 amd64:
gradlew.bat --offline --no-daemon check coverage shadowJar: PASS.
900 tests: 880 passed, zero failures/errors, 20 skipped for unavailable Windows
symbolic-link privileges. Both Checkstyle tasks, coverage and package build
pass. All seven MainWindowResultResizeTest methods pass (fixed-resize.xml).
Repository .github/run-checks.sh and git diff --check pass after staging.

Retained runtime limitation: the first full run using Oracle Java 25.0.4 with
the packaged JavaFX 17.0.7 libraries has one ApplicationStartupTest subprocess
exit failure (-1073740791) after its bootstrap assertions and stop verification
complete. All other executed tests, including the seven resize tests, pass.
See full-check.log and oracle-startup.xml. This run is not reported as passing;
the successful full run uses the installed Azul JDK+FX workflow environment.

Affected packaged acceptance
----------------------------
java -javaagent:<diagnostic-agent>=<evidence-dir> -jar ponhub.jar, launched from
an empty writable folder containing only the copied JAR. The application was
not modified or repackaged for instrumentation. Actual identity is recorded
in package-identity.json: 12,654,353 bytes, SHA-256
9f46603b9e52817e4e8a376389f442df119376d1143ed3308b765bb9932e8940,
manifest PonHub / 1.4 / ponhub.Main.

Observed screen: 2048x1152 logical pixels, output scale 1.25 (125%). No OS or
forced JavaFX scale settings were changed. Real application scenes:
- Fresh startup: 725.6x562.4, feedback 80, records 360.
- Maximized: 2048x1128.8, feedback 80, records 926.4.
- Minimum native window: scene 436x322.4, feedback 80, records 120.8.
- Native mouse drag expands feedback to 555.2, then shrinks to 80.
- 'list' and 'help search' preserve the expanded height.
- All 1,823 characters of search help remain present; scrolling reaches the
  complete end at both maximized and compact sizes without moving the divider.
- Native F1 preserves an unfinished command draft at the compact size.
- Navigation, both list viewports, feedback, command input and footer remain
  reachable; viewing/help/resizing leave the seeded operational bytes intact.

result.txt records measurements. Four native screen captures were visually
inspected: startup, maximized short feedback, expanded search help, and compact
F1/draft. Compact records retain independent scrolling; the full command input
remains below both columns. The long footer path truncates at the compact size
as before. Commands use the production CommandBox ActionEvent handler; divider
drags and F1 use JavaFX Robot. This is automated packaged acceptance, not an
independent human sign-off or evidence for other OS/scaling configurations.

To reproduce: compile CompactFeedbackAgent.java against the built ponhub.jar,
package the class files with Premain-Class: CompactFeedbackAgent, and launch
the command above under Java 25+FX from a fresh JAR directory. Do not reuse the
populated acceptance directory for a fresh-start assertion.

Release handoff and remaining gates
----------------------------------
The #363 15:30 SGT deadline is overdue and has not been moved. This record is
pre-integration evidence for this source/JAR only. Current PR CI and substantive
non-author review are required; Ernest's connected GitHub account has no team
push/merge permission. Do not treat this record as self-review or a merge.

After #363/#364/#369 integration, Vincent must refresh #271's final source,
JAR, size, SHA-256 and manifest and reconcile release materials. Ernest's
affected final-candidate package check remains NOT RUN until those bytes are
available. Retain the original Windows/Linux/Mac evidence under the original
ed32fe49 application inputs / 5e24df51... JAR; do not transfer it to new bytes.
No prior evidence is edited by this increment. #274 publication and #275
closeout retain their owners and overdue original deadlines. The additional
#362 round stays cancelled. No release publication is authorized by this record.
