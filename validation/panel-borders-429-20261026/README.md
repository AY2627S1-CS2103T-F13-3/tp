# Panel boundary polish verification

Ernest Chua, assisted by OpenAI Codex, 26 October 2026.
Product commit `13285af9`, independently based on team `127012c0`.
Original macOS report/finder credit remains Vincent's under #429.

Remove the feedback controller's duplicate frame and record-column frames;
the outer feedback placeholder and record split now own their frames. Feedback
padding is 6 pixels on all sides. Both dividers draw a flat single line in the
existing 8-pixel draggable area, replacing the bevel/doubled centre boundaries.
Command-input focus styling and card/navigation semantics are unchanged.

34 existing layout, ResultDisplay and canonical-window tests pass with zero
failures/errors/skips; both Checkstyle tasks and `shadowJar` pass. No new test
mirrors CSS declarations. Native checks exercise the affected behavior.

## Native comparison

The same separate JavaFX Robot agent, fixtures and native actions ran against
both unchanged JARs, each in a fresh writable folder. Actual screenshots of
the default, dragged, scrolled/focused and compact layouts were inspected.

| Artifact | Product source | Bytes | SHA-256 |
| --- | --- | ---: | --- |
| Baseline UI, with notice-only #451 packaging | `2f8f1f10` | 12,948,786 | `2880efe1a7bddfd9ddc0b707e6b83e949903362d1c1f66001b45df25ebd36aba` |
| Patched boundaries | `13285af9` | 12,895,782 | `5ff5150b8506d236abd0c0d7ea6d71a612f385e7c04aa43e0542b18d722feb35` |

Baseline UI Java/FXML/CSS bytes match team `127012c0`; only its added licence
resources/build differ. Both manifests report 1.4. Neither is a final candidate.

- Both real mouse divider drags pass. Horizontal divider position changes
  0.2232 to 0.3929; vertical divider changes 0.4383 to 0.5158 on the patched build.
  Observed hit areas remain **8 logical pixels** on both axes.
- Native scrollbar thumb drags move each list from 0 to 1 independently while
  the other list's scroll value stays unchanged. The selected person, unfinished
  draft, feedback and exact saved data bytes survive dragging/scrolling.
- The inspected captures show one feedback frame with equal insets, the flat
  dividers, the single centre line, and the retained orange command focus outline.
  The short-card horizontal scrollbar remains a separate open #416 defect;
  this change does not claim to fix it.
- Default window 740x600 (scene approximately 726x562); configured compact
  window 640x480 (scene 626x442). The diagnostic sets compact dimensions;
  native window-resize behavior is not claimed. Native divider/scrollbar input
  is real JavaFX Robot mouse movement/press/release.
- Windows 11 amd64, Oracle Java 25.0.4+7, JavaFX 17.0.7; observed display
  2048x1152 logical at 125% (2560x1440 physical). Processes closed; user data,
  clipboard and display settings unchanged. macOS visual repeat, other scales
  and independent human/final-candidate acceptance are not claimed.

Reproduce using `Run-Native.ps1 -JarPath PATH -EvidenceDirectory PATH`, optionally
supplying `-SourceCommit SHA` for a retained baseline. Scripts, identities,
native results and comparison captures are included; text-log whitespace/line
endings only were normalized. This increment changes no guide or screenshot
used in the public guides; rendered-guide acceptance remains with their owners.
