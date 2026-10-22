# File/Help popup contrast — #364

Vincent (`gugrusaurus`), OpenAI Codex assisted, 22 October 2026. Base team source `84f9a2cd4865b452e14fae5748896853febc8c32`; the accepted `ed32fe49` candidate has the same menu stylesheet. This report accompanies the scoped CSS/test change, not final replacement-candidate acceptance.

## Reproduction and cause

The old rule `.menu-button:hover .label, .menu-button:showing .label` selects every matching descendant label. JavaFX popup CSS follows its owner as the styleable parent, so an open File/Help menu causes the rule to reach the popup's ordinary item labels. Black text overrides `.context-menu .label`'s light foreground without changing the unhighlighted dark popup background.

The added regression creates an attached JavaFX `MenuBar` with File/Help menus, opens the real popup through `Menu.show()`, applies the production DarkTheme/Extensions styles and measures the effective foreground/background contrast. Before the fix, **Exit fails at 1.2322449066582624:1**, versus the existing 4.5:1 minimum. See `baseline.log` and `baseline.xml`. This is an actual rendered JavaFX control/CSS reproduction on Mac, not a human-operated Windows screenshot or a final-JAR native popup inspection.

The old test uses a ContextMenu owned by a TextField. It still passes because there is no open MenuButton in that popup's owner ancestry; it did not cover the reported menu path.

## Scoped correction

Use `.menu-button:hover > .label` and `.menu-button:showing > .label`. The orange menu button keeps its black title label, while unrelated popup labels retain their own normal/focused/disabled colors. The focused-item background/text and disabled-item rules are unchanged. No command handler, window layout, Help/F1 binding or domain behavior changes.

The regression checks both File and Help titles before hover, during hover/showing and after hiding; ordinary, focused and disabled popup items; and inherited opacity. The pre-existing generic-popup regression remains.

## Checks actually run

- Prescribed Mac arm64 Azul `25.0.3.fx-zulu`, Java 25.0.3+9-LTS / JavaFX 25.0.3.
- Focused `ThemeMenuContrastTest`: 2 tests pass after the correction; original stylesheet failed the added menu-popup check while the old generic-popup check passed.
- Full wrapper `--offline --no-daemon check coverage shadowJar`: **898 tests, zero failures/errors/skips**, both Checkstyle tasks, coverage and package build pass (`checks.log`). Existing production Help/F1/draft/state regressions are included in this unfiltered run.
- Whitespace and staged repository checks are recorded with the PR. No new human-operated Windows or Mac popup acceptance is claimed here. Native screenshots and supported-platform rechecks remain part of the issue/replacement-candidate handoff.

## Package/release consequence

Local fix build: `ponhub.jar`, **12,654,259 bytes**, SHA-256 **`fb24845c2a3bc02f445c8a16ed34d8ddc040515e67d233e06fa8586b38bd18f7`**, manifest PonHub / 1.4 / `ponhub.Main`. This is a pre-integration build, not the immutable final accepted candidate. See `package.json`.

The stylesheet is inside the JAR. Therefore the old accepted hash `5e24df51…` and its complete platform acceptance cannot be relabelled as acceptance of these new bytes. After review/current CI/integration, #271 must identify the resulting source/JAR/hash/manifest and reconcile affected packaged File/Help normal/hover/open/focused states and Help/F1/draft preservation before #274 publication. Retain the old evidence under its original identity. Any final-source/input change must be compared explicitly.

This verification establishes a concrete product defect and a focused correction. It does not close #364, waive missing native observations, merge/publish a release or change deadlines. Original v1.4 ownership/authorship remains.
