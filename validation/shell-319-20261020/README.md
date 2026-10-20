# #319 native display checks — 20 October 2026

Manual execution by the user, guided in Codex; supplied screenshots visually inspected in the conversation. Initial automated capture failed; manual checks supersede the initial blocked attempt. This is bounded shell evidence, not complete release or independent #317/#321 acceptance.

Build: PR #334, commit 88701bf2d3a18112990d9d3b30770189f1aff646.
Windows Azul Java/JavaFX 25.0.4.1; Gradle wrapper --no-daemon shadowJar passed.
JAR SHA-256 before/after: 06C576BF8A9EE577C752D898259B6C2B723C36DE620F698ED522C07F113D542C.

## Configuration and observations

| Configuration | Observed checks |
| --- | --- |
| 1920x1080, 100% | User confirmed startup command focus and Enter help submission. Help divider shortened; final guidance reached and copied. F1/menu draft preservation confirmed. Missing-prefix error readable and failed input retained. Long Tutor contacts fully wrap at narrow People width. Final Student card including parent phone/missing contacts reached. Lesson subject, schedule, Tutor, room and count wrap at narrow Lesson width. Screenshots show maximised/restored layouts and both column-width extremes with bottom dock accessible. |
| 1920x1080, 125% | Settings proof supplied. User confirmed startup typing and divider dragging. Screenshot shows final help guidance and complete final Student card. Both help routes preserve draft; subsequent screenshot shows readable missing-prefix error and retained input. Bottom command dock accessible. |
| 1280x720, 150% | Completed after the user applied custom scaling. Settings screenshots show 1280x720 and 150% custom scaling; PonHub screenshots show the maximised app with the dock visible. Help opened with Enter and was scrolled to its final guidance; F1 preserved an unfinished command. In the narrow People column, the Tutor's email and full address wrap across scroll screenshots. In the narrow Lesson column, subject, schedule, Tutor, room and student count are readable across scroll screenshots. A missing required `p/` error is readable, the rejected command remains in the input, and the People count remains 3. |
| 2560x1440, 150% | Supplementary supported setting, with Settings proof. User confirmed startup typing. Screenshots show both column extremes, long-contact/Lesson wrapping, final help guidance and final Student address line. Both help routes preserve draft; rejection screenshot shows readable error and retained input. Bottom dock accessible. |
| 2560x1440, 100% restored | Settings screenshot confirms original resolution/scale and extended displays restored. Search c/tutor a/Wrapping returns only Tutor T1 with complete matching address visible at narrow width. Separate protected fixture startup dialog and blocked mutation feedback are fully readable. Zero people added. |

45 supplied PNGs are preserved beside this report with their original attachment names. Settings proofs: 8cdf35c0 (1080/100), af409e70 (1080/125), c4604d4c (earlier 720 scale-selector state), 8cabf23c and 0f74ba8b (1280x720 with 150% custom scaling), bbc83075 (1440/150), acba4b11 (2560x1440/100% restored); identify each by its filename prefix after codex-clipboard-.

## Synthetic fixture

Original empty isolated app populated manually with Tutor T1 Native Display Test Tutor, Parent P1 Native Test Parent, Student S1 Native Test Student, and Lesson L1 Mathematics Native Display Wrapping Check. Parent phone 91234567; Tutor phone 92345678 and native.display.test@example.com; long address ends Wrapping Check. Room is ROOM123: the initial suggested Room 123 was rejected because spaces are invalid, then corrected by the user. These are intentional fixture mutations, not a claim that the original fixture bytes remained unchanged.

## Protected fixture integrity

Separate protected/ponhub.jar uses protected/data/ponhub.json with unsupported schemaVersion 999. User observed startup protection dialog, dismissed it and submitted add r/tutor n/Blocked Test p/93456789. Screenshot shows protection guidance, retained failed input and zero people. User then closed normally.

Protected data SHA-256 before/after matches:
58A73E30A2829BC5E5209341574D5BF555564C147FEF23B7C8B998E810631D52.

## Remaining limits

- Search context and protected feedback were checked only at restored 2560x1440/100%, not throughout the required matrix.
- Copying was confirmed at 1080/100%; not repeated at other scales.
- One Lesson card does not demonstrate multi-Lesson final-card scrolling or populated roster/attendance/history views.
- This evidence was published to PR #334's branch. No claim of full #319 closure, independent review, rendered-guide validation, #320 handoff or merge is made.

## Configuration follow-up — 20 October 2026

The user later applied Windows custom scaling at 150% while the display was set to 1280x720. The supplied Settings screenshots show that configuration and a separate screenshot confirms restoration to 2560x1440/100% with extended displays. The exact 1280x720/150% setup was then exercised in PonHub as itemized above; this supersedes the earlier NOT RUN note. It does not close the remaining matrix and content-coverage items above or substitute for independent #317/#321 acceptance.


## Documentation reconciliation

The UG now includes an actual populated shell screenshot and explains both adjustable columns and the fixed bottom command dock. The DG describes startup focus and links this evidence. Existing Ui.png and its separate shared-roster caption are retained.

Rendered-guide validation remains BLOCKED. The repository frozen Windows Gemfile adds wdm, absent from Gemfile.lock. A temporary Gemfile excluding only the Windows watcher got past that mismatch, but installing the remaining locked native extensions failed because MSYS2 is unavailable. Repository Gemfile/lockfile were not changed. No successful Jekyll build or rendered-guide acceptance is claimed.

Prepared as Vincent with OpenAI Codex assistance. User supplied and executed the manual native checks; these are author checks, not independent non-author acceptance.
