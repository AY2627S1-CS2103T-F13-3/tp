# Final integrated guide audit — #224

Completed 22 October 2026 (SGT) by Zhu Zhi Yu (`ultramanarm`) with OpenAI Codex
assistance, continuing Ernest Chua's existing audit at the user's request.
Ernest's four original commits, evidence and feature-owner attribution are
preserved. This follow-up uses the existing maintainer-editable #358 branch;
it does not attribute Zhu Zhi Yu's new verification to Ernest.

**Guide audit work is complete and ready for substantive non-author review.**
Issue #224 remains open until that review, applicable current checks and
integration pass. Its owner Ernest, v1.4 milestone, completion due
**20 October 2026, 15:00 SGT (UTC+8)** and Target date **2026-10-20** remain
unchanged and overdue. No release publication or final-candidate acceptance
is claimed.

## Source and coverage

- Audited integrated product source: `f62590fe27fdd0104fa6efea0584e22ab8104ffb`,
  including namespace #357 and version/package-workflow #349.
- Evidence-branch integration: `a1358503f3d13d45af6d3c16f41d86258c910be3`.
  Product code, guides, build configuration and workflows match the audited
  team source exactly. Only validation artifacts differ in this PR.
- The [original coverage map and five-author UML map](../guide-audit-224-20261021/README.md#coverage-map)
  remain applicable. The only guide changes since that baseline are package
  paths/class names in DG, SettingUp, Testing and Logging, plus DevOps's
  Linux candidate-dispatch explanation. These changed sections were rechecked.
  UG, README, index, AboutUs, site templates and all diagram/image bytes are
  unchanged. The original full reader/browser/UML/external-link results retain
  their named authors, sources and limits.

## Final checks

| Check | Actual result |
| --- | --- |
| Locked Jekyll rebuild | PASS: Ruby 3.3.12, Bundler 2.5.11, Jekyll 3.10.0, `BUNDLE_FROZEN=true bundle _2.5.11_ exec jekyll build --baseurl /tp`; lockfiles unchanged. |
| Complete local target check | PASS: 9 pages, 412 local targets, 30 image references, 26 tables, 5 README targets; zero failures. |
| Source inventory | PASS: 52 references, comprising 51 files and one directory, 42 unique targets, 47 namespace occurrences. All targets resolve with the correct kind under `ponhub`. |
| Affected browser sections | PASS: DevOps package procedure, DG Lesson-value/source contract, SettingUp, Testing and Logging at 1280x800 and 390x844. No page-level horizontal overflow or broken local image observed. Viewport override reset. |
| Documented focused example | PASS: `./gradlew test --tests ponhub.logic.PersonIndexResolverTest --no-daemon`, 7 tests, zero failures/errors/skips, prescribed Mac Zulu 25.0.3 with JavaFX. |
| Production-session guide journey | PASS: reused Ernest's original harness with the previously verified namespace-only adaptation. Empty-roster Lesson, two Students, enrolment, attendance correction, former-member history, no-op/rejected-command byte preservation, empty-Lesson deletion and restart all pass. |
| Shared drafting material | PASS for the bounded current readback: historical/current headers, exact cross-tab and repository UG/DG hrefs, shared-Lesson/enrolment/deletion/dated-attendance corrections and active edit guidance are present. No shared-Doc write in this follow-up. |
| DevOps/workflow reconciliation | PASS: manual dispatch requires candidate_ref and expected_sha256 with no remembered defaults; push/PR checks use event SHA and calculated checksum. The guide distinguishes tooling checks from independently identified candidate acceptance. |

The guide journey runs production classes with `-ea` and a fresh disposable
store, without launching a JAR or JavaFX window. Its deletion rejection is for
the remaining Student roster; it does not separately prove the attendance-only
deletion guard. Existing owner regressions cover other unchanged contracts.
No new Java source or assertion quota was introduced.

Reproduction: build the site, then run the original
`validation/guide-audit-224-20261021/check_links.py`; its output paths are in the
original evidence directory, so preserve/restore those historical reports and
copy the new results to a separate directory. For the source-session replay,
use the original `GuideWorkflow.java` after replacing its package/import prefix
`seedu.address` with `ponhub`, compile against this source's actual main runtime
classpath and run `ponhub.logic.GuideWorkflow NEW_DISPOSABLE_JSON_PATH` with `-ea`.
Commands and assertions are unchanged from Ernest's original harness. Recorded
output is `workflow.txt`; test XML, link inventory, browser measurements,
screenshots, sanitized Doc readback and summary hashes accompany this report.

## Original residuals reconciled

1. **Namespace:** #357 is merged. All 52 source references resolve on the final
   integrated source. Setup's `ponhub.Main`, Testing's filter, DG commands and
   Logging's test links agree with the source. Existing guide anchors remain.
2. **Shared draft:** Zhi Yu's [editing-status correction](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/224#issuecomment-6319138833)
   and [shared-Lesson correction](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/224#issuecomment-6319690581)
   are complete. Ben completed the [historical-copy and repository-pointer work](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/224#issuecomment-6320202905)
   on Ernest's assigned track. Current connector readback confirms the saved
   prefixes and bounded feature corrections. Ben's original before/after export
   verification remains his evidence, not a preservation comparison rerun here.
   Templates, dated decisions/proposals and original feature authorship remain
   governed by those owner handoffs. No private meeting/contact content is copied.
3. **Unused legacy Storage figure:** still present but unreferenced in this
   source. #359 is the already reviewed bounded removal; #239 owns its integration
   disposition. The active CanonicalStorageClassDiagram and original five UML
   contributions remain intact. This finding has an explicit owner and does not
   make the unused image part of current architecture.
4. **Release runbook:** #349 is merged and the changed DevOps paragraphs now
   match the actual source/ref/checksum workflow. The guide's `1.3`/`v1.3` pair
   is explicitly an example of the convention; actual source declarations are
   `1.4` and `v1.4`. Published-version/media/download reconciliation remains with
   #273/#274, rather than implying v1.4 has already been released.

## Review, handoff and limits

Any eligible teammate who did not contribute these changes can review the final
head. Earlier COMMENTED reviews cover preparation only. The new author checks
do not replace a substantive non-author teammate review, and the final head's
applicable CI must pass before an eligible teammate with merge access merges.

After that integration, Vincent can consume this completed #224 evidence for
#239 and #271. If #359 merges later, reconcile its two unused-file deletions in
the branding disposition; active guide/source targets are unaffected. Other
new guide or product changes require proportionate affected checks.

#270's bounded owner verification retains its recorded source and review limits.
The later independent #270/#272 exact-candidate acceptance still requires
Vincent's #271 immutable team SHA and accessible JAR, byte size, SHA-256 and
manifest. No such final-candidate handoff is certified here. Native Windows
acceptance cannot be claimed from this Mac source-session/browser run.

Not rerun: full Java suite, packaged/native display or three-platform launch,
new PlantUML rendering, complete external-HTTP campaign, deployed website,
public-download verification or release closeout. Baseline unchanged-artifact
evidence is reused explicitly. No deadline, scope, feature ownership or review
gate was changed.
