Result heading inspection (#369) -- 22 October 2026
Ernest Chua (ErnestChuaa), with OpenAI Codex assistance.

Implementation eda3fdd8; source with guide wording 6e61fde0cd6ea4a34b59f35608a28d15116d4584.
Base e4bed90a49387359ac15b5c0fa30ecded1986e28 includes #363/#364's merged fixes.
Ben retains original result/navigation feature ownership. This bounded fix
adds wrapping, live-bound tooltips to the two contexts and two summaries.
Content width is capped at 320 logical pixels; the tooltip remains available
while hovered. It does not expand the heading or consume list space.

Checks actually performed
-------------------------
Windows 11 amd64; Oracle Java 25.0.4+7-LTS-189; packaged JavaFX 17.0.7+2.
gradlew.bat check coverage shadowJar: PASS; 903 tests, zero failures/errors,
20 skips for unavailable Windows symbolic-link privileges. Both Checkstyle
tasks and repository .github/run-checks.sh pass; git diff --check passes.
The focused panel/production-window run passes all 29 tests.

ResultHeadingTooltipTest inspects the actual tooltip skin, complete text
and scene bounds at 198/399-pixel panel widths, then follows live context,
count and list/detail changes. CanonicalMainWindowTest checks all four
tooltips through its existing production command routes and result refreshes,
including history/student/detail/date changes, errors and help.

The four PNGs are JavaFX node snapshots from the production panels loaded
from the built JAR, with the production stylesheet. The independent local
render probe programmatically shows each tooltip at both panel widths;
rendering.txt records actual text and dimensions. All four compact snapshots
were visually inspected: the long query and history IDs are complete and
wrapped; both summaries are complete. The normal-width snapshots were also
generated locally. These are rendered control checks, not native pointer
hover, OS display-scaling checks or independent human acceptance.

Frozen-lockfile Jekyll build using ruby:3.3 and Bundler 2.5.11: PASS.
Chrome inspected the exact local render of UG 'Moving between results' and
DG 'Active Student attendance history checks (#100)' at desktop/853x600
and 450x600 viewports. Screenshots and DOM measurements show the added
paragraphs wrap without overflow. The temporary viewport override was reset.

Pre-integration build identity
------------------------------
ponhub.jar: 12,655,415 bytes
SHA-256: 8fbd0e5414d91c006984489b71083445da41d22f76f37c2fbc49a681d79ac9d6
Manifest: Main-Class ponhub.Main; Implementation-Title PonHub; Version 1.4.
This is a local branch build, not the frozen final release package. No new
java -jar native/scaling acceptance or public-download verification is claimed.

Remaining gates
---------------
Current PR CI and substantive review by an eligible non-author teammate;
eligible merge pickup after those gates pass. Retain the PR branch. The
18:30 SGT #369 checkpoint remains overdue and has not been moved.
After integration, refresh the final source/JAR/hash/manifest and affected
packaged heading/menu/F1/draft checks and materials before #274 publication.
Old 5e24df51... evidence stays attached to its original bytes. #362 remains
cancelled; v1.5 and publication/closeout remain outside this implementation.
