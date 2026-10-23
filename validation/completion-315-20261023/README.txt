Completion #315 verification — 23 October 2026

Implementation source: 130e84d0f1d93da83697e39918f2a3d5bfde6a8b, based on
team master 8ce4440e. This record and diagnostics do not change the product.

Zulu Java/JavaFX 25.0.4.1: gradlew.bat check coverage shadowJar PASS.
913 tests, 0 failures/errors, 20 platform-assumption skips. Checkstyle,
.github/run-checks.sh and git diff --check PASS. Focused completion, input
and Logic checks also passed with Oracle Java 25.0.4 and packaged JavaFX 17.
The first full check caught test indentation; the corrected full check passes.

Native packaged acceptance used Oracle Java 25.0.4+7, bundled JavaFX 17.0.7,
Windows 11 amd64, observed 2048x1152 logical screen and 1.25 output scale
(2560x1440 physical). No display settings were changed. Scene: 725.6x562.4.
JAR: 12,659,587 bytes, SHA-256
 d255e450644df651ba7b2386e781e0f439fb70ec7505706edccc2b6b8a7c31b2
Manifest remains PonHub 1.4: this focused feature build is not a release.

Run-NativeCheck.ps1 compiles a separate diagnostic agent, copies the exact
JAR to each fresh writable app folder and runs java -javaagent:... -jar ... .
The product JAR is not changed/repackaged. Completion drafts, Tab/cycling,
caret movement, selection, Shift+Tab/ordinary traversal, Enter, failed-draft
recovery, F1 and menu actions use real JavaFX Robot keyboard/mouse input.
Programmatic reads assert the resulting text/caret/focus/feedback/file state.
The protected fixture contains deliberately malformed fictitious data; the
native script dismisses the expected protection warning using a mouse click.
Only help/list/exit complete; unmatched mutation prefixes traverse normally.
No operational data is created by normal completion/help, and the rejected
file is unchanged. Both success transcripts and screenshots are retained.
Screenshots were inspected, including the actual Help popup.

An initial diagnostic launch lacked foreground focus and stopped on typing
verification before any command submission; a separate protected run stopped
on the expected modal before input. The runner now checks foreground before
input and dismisses the known modal natively. Reruns pass with unchanged
product source/JAR. Failed diagnostic runs remain local under the task's
_temp evidence, not as successful product evidence.

Guides: frozen-lockfile Jekyll build using Ruby 3.3/Bundler 2.5.11 PASS.
Changed UG/DG anchors and links checked against the render. Exact render is
served locally at http://127.0.0.1:8878/tp/UserGuide.html and DeveloperGuide.html.
Rendered browser inspection: PASS — completed on 23 October 2026.
Guide source commit: 29b500dc74b157fc8029f3db727b419e58aa054d.
The earlier NOT RUN entry is superseded: browser discovery now lists Chrome
and the in-app browser. The user-selected @Browser preview was inspected
using the in-app browser at the exact localhost render above.

Observed viewports: default 1230x947 CSS pixels (DPR 1.25), 768x947 and
390x844 (override DPR approximately 1). Both guides have no page-level
horizontal overflow: client/scroll widths 1215/1215, 753/753, 375/375.
UG completion wording, command summary/table, Lesson board image and TOC;
DG completion design, four manual checks, architecture diagram and table;
DG-to-UG and manual-check links, TOC and compact navigation were inspected.
All 4 UG and 15 DG images load; all 7 UG and 16 DG tables have consistent
columns. At 390px the summary table scrolls within its own viewport; actual
horizontal input reveals the Example column (scrollLeft 88.8), without
widening the page. Temporary viewport overrides were reset.

The first rendered check found Kramdown misparsing the space-padded command
examples in manual step 2. Commit 29b500dc describes the extra spaces
explicitly around ordinary code spans; the rebuilt desktop/narrow/compact
views have six correct code spans and no stray backticks in that step.
Before/after screenshots, DOM measurements, normalized build log and the
served-byte/local-target verification script/results are in browser/.
The local preview path is an existing junction to docs/_site, so rebuilding
updates it directly. Build command (PowerShell, repository root):
 docker run --rm -e BUNDLE_FROZEN=true -v "${PWD}/docs:/docs" -v ponhub-jekyll-gems:/usr/local/bundle -w /docs ruby:3.3 bundle _2.5.11_ exec jekyll build --baseurl /tp
Then run: python validation/completion-315-20261023/browser/verify_render.py

These are author checks assisted by OpenAI Codex. Independent teammate review,
non-author acceptance of Ernest's UI work and current PR CI remain separate
gates. These browser checks do not extend the native application/OS/scaling
coverage above. No product code, tests or packaged JAR changed in this
guide-rendering follow-up, so native acceptance was not rerun.

CI follow-up: the first PR run rejected trailing spaces in captured Windows
command echoes and Jekyll output. Those two transcript copies have trailing
whitespace normalized only; the original raw logs remain local. No product,
test, diagnostic outcome or JAR byte changed. Repository scripts pass again.
