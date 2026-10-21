# Roster-driven divider drift (#317 / #319)

Recorded by Vincent (`gugrusaurus`) with OpenAI Codex assistance on 21 October 2026.

## Reproduction and cause

The [reported native finding](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/317#issuecomment-6306841139)
reproduced using source `a828eef7366d3d2b7eff3000c1d8f324396603f5` and the supplied
30-student fixture (SHA-256 `11e5406b985eee361161274d4b653db4ac341eeaeceb92f06fdf9c099b8031dc`).
The shown production Stage stayed at 1200×800 points, Scene 1200×772, with an initial split of 0.5.

| Command | Original divider centre | Original footer height | Fixed divider centre | Fixed footer height |
| --- | ---: | ---: | ---: | ---: |
| `help` | 364 | 30 | 364 | 30 |
| `showlesson lid/L1 d/2026-10-19` | 366 | 25 | 364 | 30 |
| `history 1` | 363 | 30 | 364 | 30 |
| `help` | 363 | 30 | 364 | 30 |

Positions are relative to the Scene. Original final-help positions over three cycles were
363, 362, 361; fixed positions stayed 364 throughout. See `before.csv` and `after.csv` for all samples.

`LessonListPanel` switches from a list to a ScrollPane containing the full roster. The ScrollPane
inherits its content's preferred height (794 points including its insets), which propagates through
the records pane to the outer result SplitPane. Its preferred height jumps 626→1020 while the
root's preferred height jumps 738→1132, exceeding the fixed 772-point Scene.

VBox shrinks children toward their minima to fit. `VBox.vgrow="NEVER"` governs expansion, and does
not stop the footer shrinking from its preferred 30 to its computed minimum 25. The split therefore
grows 660→665, then returns to 660 when history is shown. JavaFX SplitPaneSkin's integer allocation
of this odd resize delta gives the upper pane +2 on growth and -3 on shrink, changing the retained
divider position. The runtime's ScrollPaneSkin, VBox and SplitPaneSkin source confirms this path.

Independent temporary controls (pin footer minimum to preferred size, bound the detail viewport's
preferred height, or bound the outer split's preferred height) each eliminated the movement.

## Fix and checks

Give the outer result SplitPane a content-independent preferred height of 210, matching its existing
minimum. `VBox.vgrow="ALWAYS"` still allocates the available height and the user can still drag the
divider. This removes the roster-length dependency at the shell boundary.

`MainWindowResultResizeTest.rosterAndHistoryCommands_preserveWorkspaceFooterAndChosenSplit`
uses a generated 30-student roster and repeats real commands through the CommandBox handler three
times at logical Scene sizes 1200×772, 853×600 and 450×360. It verifies an actual oversized roster
and history view, unchanged split/feedback/footer geometry, divider fraction, command position and
operational bytes. The regression fails without the FXML fix (split height expected 660, actual 665)
and passes with it.

Local `./gradlew --offline --no-daemon check coverage shadowJar` passed: 897 tests, zero
failures/errors/skips, both Checkstyle tasks, coverage and packaging. Existing UI resizing tests
also cover divider extremes, pane minima, help/F1/menu behavior and complete long-feedback scrolling.

Environment: macOS 27.0.1 (26A434), arm64, Azul 25.0.3.fx-zulu / OpenJDK 25.0.3+9-LTS,
bundled JavaFX 25.0.3. The diagnostic submits commands programmatically in a shown production window;
these measurements are not native mouse/keyboard or packaged-JAR acceptance. Logical test sizes are
not evidence of actual display scaling. CI, non-author review and #317's affected native acceptance
remain separate gates. The Windows history-text geometry report in #319 item 6 is outside this fix.
