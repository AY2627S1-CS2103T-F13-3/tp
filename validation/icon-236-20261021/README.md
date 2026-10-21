# PonHub application icon — issue #236

Prepared by Ernest Chua with OpenAI Codex assistance on 21 October 2026.
Base: team master `b9cfd2d92ae603ce0e79ab17f64695ba2b86afb7`, including #350.
The 19 October 2026, 20:00 SGT deadline remains overdue and unchanged.

## Asset and scope

The original vector depicts two people reading an open book, using the existing
`#FF9900` accent and `#101010` surface. The neutral people/lesson cue is readable
at small sizes. Editable source: `docs/images/ponhub-icon.svg`; JavaFX resource:
`src/main/resources/images/ponhub_icon.png`, 64x64 RGBA with transparent corners.
The vector was rasterized with resvg-py as development tooling only; no runtime
library/dependency is added. Re-export with `resvg_py.svg_to_bytes(svg_string=...)`.

Both UiManager's initial icon and MainWindow.fxml now use that resource. Owned
application alerts retain the main Stage as owner. No layout, window dimension,
command behavior, shared image or theme setting changes.

The old brown contact-book asset is removed after all resource references move.
copyright.txt credits the new original artwork and removes the obsolete entries
for address_book_32.png, AddressApp.ico and edit.png, all absent from this tree.
The genuine Marco Jakob/calendar credits and upstream MIT licence are retained.
Final screenshot/public-page changes stay with #102/#152/#223 and #239.

## Automated verification

- Windows 11 Pro x64 build 26200, Oracle JDK 25.0.4, project JavaFX 17.0.7.
- Full `gradlew.bat check coverage shadowJar`: 896 tests, zero failures/errors,
  20 platform-assumption skips. Checkstyle, coverage and packaging pass.
- Existing AppUtilTest now checks successful icon decoding and actual dimensions.
  Existing real-stage CanonicalMainWindowTest covers production initialization,
  FXML and command focus. UiManager startup/fatal/protected regressions also pass.
- Packaged PNG equals the source PNG byte-for-byte; the packaged FXML references
  the new path, and the old PNG is absent. Source audit finds no stale resource
  reference outside the historical issue evidence and accurate deletion record.
- Tested JAR: 12,658,434 bytes, SHA-256
  `a4fb3c9164f4ca4e68330a9e357fb12bb2aff24d1999850c96a575af26c294c3`.
  This is this increment's development package, not the final v1.4 candidate.
- Repository formatting scripts and `git diff --check` pass. Locked Bundler
  2.5.11/Jekyll 3.10.0 builds the documentation tree; 410 local targets, 35
  image references, 16 DG tables and ten shared anchors pass. Connected Chrome
  at 2052x986 inspects the exact runtime PNG at 16/32/64/128 CSS pixels on light
  and dark surfaces. All eight images load and the intended people/book cue
  remains visible at the small sizes. This is asset review, not native evidence.

## Native gate and recovery forecast

NOT RUN — native window/error-dialog icon inspection. CUA's initial inventory
exposes connected Chrome and no native apps; native control is disabled in this
session. Automated Stage/FXML checks and image inspection do not establish
visible OS title-bar/taskbar or dialog acceptance.

A local manual runner is prepared at `_temp/check-icon236-native.ps1`. It launches
the exact JAR from an isolated empty writable folder with `java -jar`, then a
disposable probe invoking the real packaged UiManager main-window/error-dialog
methods. It uses no production data. The user has been asked for the observed
result while independent work continues. No manual pass is claimed yet.

Implementation and automated checks are complete. Native inspection is a short
remaining check, followed by first-available non-author review, head CI and merge.
Forecast: ready for native check/review pickup immediately; integration timing
depends on that evidence and teammate availability. Keep #236 open until the
gates pass. No deadline, scope or acceptance requirement is waived.
