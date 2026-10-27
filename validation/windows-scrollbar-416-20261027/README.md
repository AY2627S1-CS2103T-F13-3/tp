# Windows scrollbar recheck — #416 / #370

**Round 1 report recheck: PASS on Windows at the observed 125% display scale.**
Executed on 27 October 2026, 20:03–20:04 SGT, by OpenAI Codex on Ernest Chua's
behalf. All 16 final native screen captures were inspected. This supersedes the
Windows-recheck-waiting status for this integrated build, and the 26 October
still-reproducible result for older bytes. It does not certify the later exact,
versioned candidate under #327.

## Exact application tested

- Integrated source: `2f1b83123808f1b1cf79ce9d22e39d368eeae830`.
- Source tree: `bbca89454aa7d37b132b654cc0683bfaf6d1bbfa`.
- Contains #504's correction `6649977067420928364f137c2dc484b1b5c2c69d`,
  integrated at `bd94cf08f0c22fa09f7441b6bd77cfc27767d7fc`.
- Local `shadowJar verifyPackagedLicenses --no-daemon --max-workers=2` passes;
  24 licence/notice files match their source bytes. See `build.log`.
- `ponhub.jar`: **20,352,584 bytes**.
- SHA-256: `95088f4890034c33b3c46573ceb7567c6ccbdf15c4d09ea9ec10131407a7647a`.
- Manifest: `Main-Class: ponhub.Main`, `Implementation-Title: PonHub`,
  `Implementation-Version: 1.4`. Version delivery is still Round 2; this is
  deliberately identified as an integrated Round 1 test build.
- The actual JAR was copied unchanged into a new writable folder. The agent is a
  separate JAR and neither modifies nor repackages the product. Its hash is
  checked again after the run.

## Native environment and results

Windows 11 amd64; Oracle Java `25.0.4+7-LTS-189`; bundled JavaFX `17.0.7+2`.
Observed physical display **2560×1440**, JavaFX screen **2048×1152 logical**, output
and render scale **1.25**. No display setting or JVM scale override was changed.

| Case | Observation |
| --- | --- |
| Original Alex/Jamie short-card fixture, 740×600 | Horizontal bar `visible=false`; scene approximately 725.6×562.4; cell width/preferred width both 278.4. |
| Native slight divider narrowing, same window | Horizontal bar `visible=false`; actual cell width/preferred width both 274.4. This is the measured current layout, not an assertion that the old 275.2px viewport was recreated exactly. |
| Original fixture, 640×480 | Horizontal bar `visible=false`; scene approximately 625.6×442.4. |
| Tutor, Parent and Student, default and compact | Full long name/email/address strings remain in wrapping labels, with enough allocated height. Native vertical thumb drags reach the last card and the final address line; compact captures include both ends of the list. |
| Filter Jamie | `Position: 1` and `Student · ID: S2` remain distinct and visible. |
| Native divider and vertical scrollbar drags | Divider position changes; vertical bar reaches 0 and 1. No horizontal bar appears after narrowing or scrolling. |
| Actual pointer hover | The live `Copy Parent phone` tooltip appears in `07-native-card-tooltip.png`; no Copy action is invoked. |
| Native F1 | Help appears in feedback; unfinished draft, selected record and people order remain. |
| Read-only layout/shortcut operations | Saved `data/ponhub.json` bytes remain identical. |

Commands, card selection, scrollbar/divider drags, hover and F1 use JavaFX Robot's
native mouse/keyboard input. Window resizing and focus control are supplementary
setup. Layout observations read the real shown window; they do not substitute
for the native interactions or inspected screen pixels.

The launcher keeps its console helper hidden and explicitly shows only the
PonHub window belonging to the launched PID. The diagnostic temporarily keeps
that test window in front. Captures read actual screen pixels bounded to the
application's client area, including the live tooltip, rather than Node snapshots.

## Reproduce

Build the source above with Java 25, then run from PowerShell:

```powershell
.\validation\windows-scrollbar-416-20261027\Run-Native.ps1 `
  -JarPath .\build\libs\ponhub.jar `
  -EvidenceDirectory C:\path\to\new-evidence `
  -RunDirectory C:\path\to\new-empty-runtime `
  -SourceCommit 2f1b83123808f1b1cf79ce9d22e39d368eeae830
```

Use a new run directory. The script discovers the active Java 25 installation,
compiles the diagnostic against that product JAR, records artifact identity and
launches `java -javaagent:<separate-agent>=<evidence> -jar ponhub.jar`.
Keep the test window available while the bounded script runs.

The initial four commands are the original #416 reproduction:

```text
add r/tutor n/mei lim p/92345678
add r/student n/alex tan l/p1 pp/91234567
add r/student n/jamie tan l/s2 pp/91234568
list r/student
```

All additional data is fictitious. The system clipboard is not read or changed.
The application is closed by the agent; the launcher stops its own process if
the three-minute timeout is reached. The user's original checkout and working
data are untouched.

## Evidence and limits

`identity.json`, `runtime.txt`, `result.txt`, `transcript.txt`, `stdout.log` and
`stderr.log` retain the exact final run. `Run-Native.ps1` and
`ScrollbarAgent.java` are its runnable source. The 16 PNG files are the inspected
native client-area captures. `result.txt` ends with
`Windows scrollbar recheck=PASS`.
Published text logs normalize trailing whitespace and final blank lines only;
the raw transcripts remain in the local run folder. Recorded results and
artifact values are unchanged.

Early runner-development probes encountered startup focus, native keystroke
settling and hidden-window launch problems. Those probes are retained locally;
they are not product defect findings. Explicitly showing the target process's
window and waiting for native input completion resolved them with identical
product bytes. Only the completed, visibly inspected final run is delivered here.

Coverage is **Windows at actual 125% scale only**. No native 100%/150% display,
cross-monitor scale transition, macOS/Linux, independent human sign-off or final
versioned-candidate acceptance is claimed. No full test-suite rerun was needed
for this evidence-only increment; the build and licence check above were run.

Ernest retains the original short-scrollbar observation/finder credit. Zhi Yu
retains the people-card correction and #370 ownership; prior feature/UI
authorship remains. The diagnostic adapts the existing team `Round1Agent` runner.
Codex assisted test design, scripting, execution, screenshot inspection and this
report; no unaided discovery or independent human acceptance is asserted.

Owners, milestone v1.5, and the original overdue dates remain unchanged:
#416 **26 October 12:00 SGT**, #370 **24 October 18:00 SGT**. Completion now is
late completion. #327's later exact-candidate/native acceptance and publication
gates remain separate. No release, merge or milestone closeout is performed.
