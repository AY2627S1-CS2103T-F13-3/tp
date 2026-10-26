# macOS Dock icon — #428

## Candidate and environment

- Date: 26 October 2026, approximately 14:10–14:14 SGT.
- Base source: `4896886e273a82a11583b7ef27ad384032aa7166` (team master).
- Patched source: this PR's `DockIcon.java` addition and `UiManager` call; no other production changes.
- macOS 27.0.1 (26A434), arm64.
- Runtime: Zulu Java/JavaFX 25.0.3, `25.0.3+9-LTS`, installed at
  `/Users/gugrusaurus/.sdkman/candidates/java/25.0.3.fx-zulu`.
- Both JARs identify themselves as v1.4 development builds; neither is claimed to be the released v1.4 artifact.

| Candidate | SHA-256 | Size |
| --- | --- | --- |
| Unmodified base JAR | `13a63896354dd58e2c4cb7270ebaea4e049ebf52e256aaef328f6b2241833dc3` | Not recorded |
| Patched JAR | `67a23668ff452cf713dd7e48827ab82d4b20d48804a9fc5bc87d724bd42837e3` | 12,889,102 bytes |

## Launch and observations

Codex built each JAR with the runtime above and launched it in a separate empty application
folder using the absolute runtime path followed by `-jar ponhub.jar`. No `.app` wrapper,
external icon, `-Xdock` argument or display-name override was used. Both processes logged
successful UI startup.

1. Baseline: Vincent reported “it shows the generic icon” and supplied
   [the Dock screenshot](baseline-user.png), showing the black tile with green `exec` text.
2. Patched: Vincent reported “it shows the patched icon” after the second JAR launched.
   [The after screenshot](patched-user.png) shows the bundled orange PonHub artwork;
   the tooltip still reads `java`, consistent with the icon-only scope. Both screenshots
   were supplied by Vincent, rather than captured independently by Codex.

The native Dock capture tool timed out. The screenshot and operator confirmation above
are distinguished from automated evidence. Finder double-click launching has not been checked.
The Dock display name is outside #428's scope.

## Implementation and checks

`UiManager` retains the JavaFX stage icon and schedules the same bundled PNG for the
macOS Dock on AWT's event thread. Non-Mac platforms return before AWT initialization.
Taskbar and ICON_IMAGE support are checked; missing/undecodable artwork and runtime,
linkage, AWT or I/O failures are nonfatal and logged at FINE level.

API reference: [Java 25 Taskbar](https://docs.oracle.com/en/java/javase/25/docs/api/java.desktop/java/awt/Taskbar.html).
No platform-specific library or new dependency was added.

Local command: `./gradlew --offline --no-daemon shadowJar checkstyleMain` with the prescribed
Zulu Java/FX runtime: **passed**. No local unit tests were added or run in this increment.
Headless/unsupported/failure branches and Windows/Linux rendered behavior have not been
manually exercised. Hosted checks and substantive non-author review remain integration gates.

Prepared by Vincent with OpenAI Codex assistance. Existing artwork authorship is unchanged.

## Coverage follow-up — 26 October

The original reviewed head `d8b4ce81` passed platform builds but failed Codecov patch coverage
at 12%. The follow-up adds package-local native-operation/scheduler/resource seams, with production
bindings remaining direct standard-library method references. It adds ten committed `DockIconTest`
regressions without an audio/desktop mocking dependency or changes to coverage settings.

The tests exercise non-Mac early return, deferred Mac execution, taskbar/ICON_IMAGE guards,
pixel-identical bundled PNG decoding and delivery, missing/undecodable/unreadable resources,
scheduling/support/acquisition/set failures (unsupported/security/native-linkage/AWT), and a
nonfatal default-binding missing-resource request. Controlled operations establish call order and
failure containment; they do not establish native Dock rendering on Windows/Linux or Finder launching.

On macOS 27.0.1 arm64 / Zulu Java/FX 25.0.3:

- `./gradlew --offline --no-daemon check coverage shadowJar`: PASS.
- 980 tests, zero failures/errors/skips; ten are the added Dock tests.
- Both Checkstyle tasks pass.
- JaCoCo: DockIcon 27/27 lines, 10/10 branches; DesktopAccess record 1/1 line.
- Follow-up JAR: 12,891,287 bytes; SHA-256
  `78ae9828c4015d576fa5f710a6f73037422a4b04104602ee969b6dbeafa1a77d`.

The screenshots above remain evidence for the earlier patched JAR hash, not a new visual replay of
this follow-up JAR. Current hosted patch checks and substantive non-author re-review must clear
before integration. No coverage threshold, test assertion or platform gate was weakened.
