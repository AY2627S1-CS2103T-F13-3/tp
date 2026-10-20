# Windows history bounds investigation — issue #319

Recorded on 20 October 2026 by Ernest Chua with OpenAI Codex assistance.
Item 6 remains unchecked pending review and owner reconciliation. This is
automated Windows fixture evidence; whether this PC is different from the
original reporter's machine has not been confirmed.

## Environment and source

- Machine: `ERNEST-PC`, Windows 11 Pro x64, version `10.0.26200`, build `26200`.
- Oracle JDK `25.0.4`, runtime/HotSpot `25.0.4+7-LTS-189`,
  `JAVA_HOME=C:\Program Files\Java\jdk-25.0.4`.
  The original report names `25.0.4.1`; this run does not claim that exact JDK.
- Project JavaFX `17.0.7`; the test worker loaded
  `javafx-graphics-17.0.7-win.jar` from the Gradle dependency cache.
- Rendered font: Segoe UI Regular, 12 px.
- Unmodified baseline: `93f0162f40b6d8e56a9e6c5df9534bfde9f283bd`.
- Fix parent: `493380a444e82709af8c77297482aea5ed33946a` (team master).
  `HistoryListPanel.java`, its FXML, the existing test, `FxTestUtil` and
  `build.gradle` are identical between these two source commits. The later
  theme change only adds view-navigation button rules, unused by this fixture.

## Reproduction and diagnosis

At the exact baseline, this command failed: **1 test, 1 failure**.

```powershell
.\gradlew.bat test --tests "seedu.address.ui.HistoryListPanelTest.layout_wrapsCompleteRowsWithinCompactAndNormalViewports" --rerun-tasks --console=plain
```

The failure occurs on the final record at the 198 px panel width. The text is
`2026-08-07 · L9223372036854775807 · absent · former enrolment`.
Its wrapping width is 180 px; the viewport spans scene x=0 through x=183 px.

| Measurement | Right bound, scene px |
| --- | ---: |
| Text logical bounds | 185.291015625 |
| Visible glyph bounds | 181.37109375 |
| Viewport | 183 |
| Space at string index 44, after `former` | 185.291015625 |

The space's advance spans x=182.00390625 through x=185.291015625. It is the
only character advance exceeding the viewport plus the existing 1 px tolerance.
The visible glyphs remain 1.62890625 px inside the viewport. The excess logical
width therefore does not demonstrate clipped visible text.

[JavaFX's TextBoundsType documentation](https://openjfx.io/javadoc/17/javafx.graphics/javafx/scene/text/TextBoundsType.html)
explains that logical bounds include leading and trailing spaces. In this
fixture, `Shape.union(text, text).getBoundsInLocal()` obtains the actual glyph
outlines in scene coordinates, without changing the attached text node.
[Shape.union documentation](https://openjfx.io/javadoc/17/javafx.graphics/javafx/scene/shape/Shape.html#union(javafx.scene.shape.Shape,javafx.scene.shape.Shape))
specifies transformation into the parent coordinate space of the topmost parent.

Temporary diagnostic instrumentation recorded logical/visual bounds and
`text.rangeShape(index, index + 1)` for character advances, then captured JavaFX
snapshots. It bypassed the original horizontal assertion to inspect all widths;
that diagnostic run is not a passing acceptance result. The instrumentation
was removed before the final checks. The snapshots show the actual attached
Scene fixture, not an interactive application or OS display-scaling check.

- [198 px panel, final record](row-198-last.png): complete final row readable.
- [160 px panel, first record](row-160-first.png): compact wrapping readable.

## Change and validation

The horizontal assertion now measures visible glyph outlines, preserving the
1 px tolerance. The production layout and dependencies are unchanged. The
test still checks the complete text, wrapping, row height, scrollbars, selection,
first/last records at widths 198 → 399 → 160 → 399 px, refresh and clearing.
A negative control moves the refreshed text outside the viewport and verifies
that the assertion rejects it, then restores the original position.

Focused validation passed: **2 tests, 0 failures/errors/skips**, plus
`checkstyleMain` and `checkstyleTest`:

```powershell
.\gradlew.bat test --tests "seedu.address.ui.HistoryListPanelTest" checkstyleMain checkstyleTest --console=plain
```

The full `check coverage shadowJar --continue --console=plain` retry passed:
**895 tests, 0 failures/errors, 20 skipped**, including both history tests.
Checkstyle, coverage report generation and JAR packaging completed successfully.
The initial full run failed the unchanged
`LessonBoardNavigationTest.shownWindow_minimumStageSize_keepsNavigationCommandStatusAndBothViewportsAccessible`
at line 78 (`contentHeight <= stage.getHeight()`). It passed in isolation and
on the full retry. This records a separate intermittent native-window observation;
no navigation code or assertion was changed or excluded.
The repository `.github/run-checks.sh` checks passed against the staged change.

This evidence explains the reported Windows geometry failure; it does not
establish independent cross-machine verification, complete #317/#321 native
acceptance, or close #319. Its owner and existing milestone/deadline are unchanged.
