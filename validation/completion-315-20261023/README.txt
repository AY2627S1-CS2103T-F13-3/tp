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
Rendered browser inspection: NOT RUN — browser connection unavailable.
Initial discovery, the user's Chrome-open retry, and post-render discovery
all returned apps=[], browsers=[]. No visual failure is inferred.

These are author checks assisted by OpenAI Codex. Independent teammate review,
non-author acceptance of Ernest's UI work, current PR CI and guide browser
inspection remain separate gates. Other OS/scales were not exercised here.
