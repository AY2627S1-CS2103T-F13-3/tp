# Packaged Mac checks — #366

OpenAI Codex operated the exact accepted JAR for Ben on 22 October 2026 using
macOS 26.6.2/25G83 arm64 and the prescribed Azul `25.0.3.fx-zulu` runtime,
Java 25.0.3+9-LTS with JavaFX. These are assistant interactions, not an
additional human acceptance run. JAR source/size/hash are recorded in the
[parent report](../README.md#candidate-and-environment).

## Launch and identity

The rebuilt JAR was copied unchanged to
`/private/tmp/ponhub-366-native/package/ponhub.jar`. The initial restricted
launch failed with an empty JavaFX screen list before a usable window;
`launch.log` preserves it. The direct host-context launch then logged normal
startup from another working directory, using data/preferences beside the
JAR. `launch-host.log` retains that result. That setup process was terminated
before the normal interaction campaign; it is not counted as a clean exit.

The desktop controls could not select the unbundled Java executable. A
temporary `/private/tmp/PonHub366.app` shell launcher identified that same
process to the native controls. It executed:

```sh
cd /private/tmp/ponhub-366-native/other-cwd
exec /Users/ben/.sdkman/candidates/java/25.0.3.fx-zulu/bin/java \
    -jar /private/tmp/ponhub-366-native/package/ponhub.jar
```

No JAR entry, application resource or runtime option was changed. The last
missing-store session added `--data=fresh/read-only.json` to this command;
the temporary launcher was restored afterward. The actual log and status
bar show all operational paths anchored to the JAR folder, despite launch
from `other-cwd`. The normal wrapper campaign is in `launch-wrapper.log`.

## Observed results

| Result | Expected behavior and actual observation |
| --- | --- |
| PASS | All roles, a shared Lesson and two Student enrolments persisted with stable T1/P1/S1/S2/L1 identities. Independent present/absent keys and corrections appeared in dated detail. |
| PASS | Student level edits with an explicit empty email, Parent phone, Student parent-phone and Lesson subject/time edits refreshed their active relationship/search/detail views while preserving identities. Alex originally had no email; clearing a populated contact is covered by the source-session campaign. |
| PASS | Normalized person/Lesson no-ops, zero index, invalid phone, incompatible weekday/date, Tutor overlap and guarded deletion left the saved file byte-identical. |
| PASS | Unenrolment retained former-member attendance; existing history could be corrected, new former-member attendance was rejected, and an unrecorded date remained distinct from absence. |
| PASS | Restart retained the saved aggregate, person/Lesson IDs and former history. Read queries preserved the pre-restart file hash. Compact 457×367 logical geometry also reloaded from preferences. |
| PASS | Removing S1's last attendance allowed person deletion and renumbered Jamie to position 1. L1 deletion remained blocked first by S2's roster and then attendance alone. Explicit unenrol/unmark allowed deletion; the replacement received L2. |
| PASS | With the disposable `data` directory moved aside and replaced by a regular file, an edit reported `Could not save data`. Original saved bytes stayed intact. Restoring the directory and retrying once saved the edit, which survived restart. |
| PASS | Corrupt disposable JSON displayed the protected warning, rejected edits/additions and unavailable detailed help, and preserved the corrupt bytes. Restoring valid bytes while running did not unlock the session. Restart loaded the valid records and L2. |
| PASS | `help list`, `list`, `lessons` and exit on a separately selected missing file left `fresh/read-only.json` absent. Preferences/logs remained separate. |
| PASS | Five normal wrapper launches produced five stopping logs. Four command-box exits and one File-menu exit closed normally; no unexpected exception or shutdown diagnostic occurred in this campaign. |

`commands.json` contains 70 actual command-box submissions and two unsuccessful
tool attempts that submitted no command. Three additional logged logic calls
come from F1, the Lessons button and Help-menu selection: 73 actual logic calls
in `launch-wrapper.log`. The File-menu exit stops the app without a logic call.
Manually inspected expected rejections have no automated substring assertion
in the raw file; their actual messages and the log remain retained.

## Layout and input

The default screenshot is 1480×1200 device pixels (740×600 logical). The
resized screenshots are 914×734 (457×367 logical); this is a bounded compact
check, not a complete minimum-size/scaling matrix. Navigation, input and
status remained visible. The feedback divider moved from y=544 to y=320 in
the initial device-pixel screenshot, leaving more room for lists. Long help
retained a separate scrollbar, and long name/email/address fields wrapped.
Wheel scrolling and then keyboard End reached the final long card and its
bottom address. An offscreen AX click failed; a visible coordinate click
and End succeeded. The tool failure is not reported as a product defect.

F1 and Help-menu selection displayed the same overview while retaining an
unfinished `edit 1 n/Draft Preserved` command. The Lessons button retained
that draft as it restored the catalogue. Top-level File/Help labels were
visible in the inspected normal/hover/focus states. Popup windows were not
captured with the main-window screenshot, so popup text/disabled/hover
contrast is **NOT RUN** here; #364 retains its separate verification scope.
The large initial feedback area remains the known #363 follow-up.

Screenshots, accessibility readbacks, fictitious JSON checkpoints and
the four `*-integrity.json` records accompany this report. Tool limitations
and corrected readback matchers are retained in `commands.json`. No changed
production bytes, independent human approval, new Windows/Linux run,
public-download verification or release publication is claimed.

The `before-*.json.gz` and `final-operational.json.gz` checkpoints preserve
the exact original operational bytes, including their lack of a final
newline. Decompress them before comparing with recorded hashes. Text logs
and accessibility exports have repository-required final newlines; incidental
trailing log whitespace was removed. Original test datasets remain unchanged.
