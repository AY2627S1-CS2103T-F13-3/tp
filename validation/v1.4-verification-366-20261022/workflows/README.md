# Independent command/session verification for #366

On 22 October 2026, OpenAI Codex ran this assistant-operated verification for Ben (`ben58882`).
It passed **11 scenario groups, 178 command submissions and 644 assertions**, including **80 expected
rejections**. No product defect was observed within this campaign. This is executable command/session evidence;
it does not establish native-window appearance, physical-display acceptance or an independent human pass.

## Identity

- Team source: `84f9a2cd4865b452e14fae5748896853febc8c32`.
- Accepted application source: `ed32fe49901ea11070ff16086490d2d72adafcd1`; subsequent changes are documentation/materials.
- JAR: `ponhub.jar`, 12,654,205 bytes.
- SHA-256: `5e24df511c9a5975c1c15e310ab03f2a852837e969359afe71fc8a37fa905456`.
- Runtime: architecture-appropriate prescribed Mac Azul Zulu `25.0.3.fx-zulu`, Java `25.0.3+9-LTS`, arm64.
- Input: new isolated fictitious dataset in `/private/tmp/ponhub-366-workflows/final-reviewed-names`; no user data was used.
- Contract sources: UG person/Lesson editing, search, attendance and protection sections; DG manual-testing
  procedures and transactional persistence contracts. The existing guide replay informed the harness structure.

## Executed scope

| Group | Verified behavior |
| --- | --- |
| 1 | Missing-file startup and read commands leave operational data unwritten. |
| 2 | All roles and creation order; distinct duplicate keys; leading-zero phones; ambiguous Tutor lookup; Tutor/room overlap rejection; no consumed Lesson IDs on rejection. |
| 3 | UG compound search examples; same-child and same-Lesson matching; exact phone relationships; empty results; distinct people/Lesson panels and duplicate suppression. |
| 4 | A filtered position 1 edits S2, preserving stable identity; malformed/current-index/role/unknown-field/help/newline rejection preserves aggregate, bytes, visible results and context. |
| 5 | Student optional clears; normalized person no-op bytes; true duplicate rejection; Tutor-label refresh; Parent-phone edits and Student parent-phone edits recompute relationships independently. |
| 6 | Lesson subject changes empty and restore active people/Lesson searches; partial time endpoints; normalized no-op bytes; invalid/unknown Tutor and conflicting replacements; stable allocation counters. |
| 7 | Separate Student attendance keys; status correction; identical-status no-op; invalid dates; unrecorded current members; former-member history; correction after unenrolment; rejection of new former-member dates and incompatible weekday changes. |
| 8 | Reloaded aggregate and former history; retained-attendance deletion guards; targeted unmark/no-op; allowed weekday change clears the obsolete selected date; deleted Lesson ID is not reused. |
| 9 | Read-only restart/query byte stability and all-role position reset. |
| 10 | Corrupt, unversioned legacy and unsupported-version data remain protected; only recovery routes are exposed; writes and unavailable help are rejected; replacing the file does not unlock the running session; restart with restored valid data succeeds. |
| 11 | Four injected failed saves (person edit, Lesson edit, addition and enrolment) preserve active projections, snapshots and bytes; one retry saves exactly once and reloads; normalized no-op performs no writer call. |

All rejected operations assert the saved bytes, aggregate, current people, Lesson results, history and both
result-context labels remain unchanged. No-op commands assert snapshot and byte stability. The failure seam
also counts persistence calls; it simulates an IO error through the existing `SnapshotWriter` contract and
does not claim a native OS-permissions failure. Windows/Linux were not executed by this probe.

## Reproduce

From the repository root, select Java 25 using the prescribed Mac runtime for a Mac check. First obtain or build
the JAR for the exact source, then verify its hash. The parent campaign runs the full Gradle suite separately.

```sh
verification_dir="$(mktemp -d)"
javac -cp build/libs/ponhub.jar -d "$verification_dir/classes" \
    validation/v1.4-verification-366-20261022/workflows/WorkflowVerification.java
java -ea -cp "$verification_dir/classes:build/libs/ponhub.jar" \
    ponhub.logic.WorkflowVerification "$verification_dir/data"
```

The class is deliberately in `ponhub.logic` to use the existing package-visible snapshot-writer seam, without
changing production or checked-in test code. JavaFX observable data structures are used without starting a
toolkit/window. All checks run serially on the calling thread; no native display is required.

## Retained evidence

- [WorkflowVerification.java](WorkflowVerification.java): runnable harness, expected outcomes and assertions.
- [transcript.txt](transcript.txt): actual final-run command/feedback/rejection record and result totals.
- [summary.json](summary.json): source, artifact identity, counts and explicit limits.
- [final-operational.json.gz](final-operational.json.gz): exact final fictitious saved bytes after the successful retry;
  gzip preserves the original lack of a final newline without weakening repository text checks.
- [checkstyle.txt](checkstyle.txt): Checkstyle 14.1.0 directly checked this validation Java against
  `config/checkstyle/checkstyle.xml`; exit 0, no violations. The broad cached CLI classpath emits an SLF4J
  multiple-provider warning. Manual inspection also checked naming, 4-space indentation/8-space continuations,
  imports, braces, local scope, visibility, public/class Javadoc and the 120-character hard line limit.

An initial harness run used the wrong exact expected wording for repeated `mark` feedback ("already present"
instead of the implementation's documented no-op behavior, "already marked present"). The expectation was
corrected; no production change was made. An independent agent inspected the harness, then helper names and
Javadoc layout were corrected to SE-EDU conventions. The retained transcript is the final complete passing
run after those corrections, with the same 178 submissions and 644 assertions.
