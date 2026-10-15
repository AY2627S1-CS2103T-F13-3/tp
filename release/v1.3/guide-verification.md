# Integrated guide and media verification — 15 October 2026

Vincent's Codex-assisted #102 verification, following [completed #101 acceptance](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/101#issuecomment-6194333977). This records completed checks and the remaining integration gate; it is not final release-JAR/platform acceptance.

Current release follow-up: #249/#251 are merged and #102 is complete. [candidate-handoff.md](candidate-handoff.md) reconciles the release copy and original captures against the v1.3 artifact; Vincent confirms #103 is closed. The source/runtime identities and historical deployment observations below remain unchanged.

## Source and artifact

- Accepted application: `c5b8233e9bfaf5b8c7eb7f6af8932345c17c4c63`, containing merged #250's history contrast fix.
- Application built on media branch `003a79d8260187d51f3448ca7310e16c7a1b7d7c`; production source/resources and build.gradle are identical to that accepted application. Subsequent changes are guide/release prose and images.
- Local `shadowJar` passes. JAR SHA-256: `170a22ed680dc687b7e9c5fc79846d246368372cab9ec8995302a6ad4a5e43c3`.
- Actual runtime: macOS 27.0.1 arm64, prescribed Azul Java/JavaFX 25.0.3. Window 740x600 logical pixels; screenshots 1480x1200 pixels. An isolated macOS launcher executes the unmodified JAR to expose its window to the accessibility tool. The JAR still logs application version 1.2; release version/artifact reconciliation remains #103/#153 work.

## Actual guide workflow

The visible command box executed 33 recorded submissions across two launches, plus normal exit commands. Checks cover Student/Tutor/Parent creation; two Students enrolled in one Lesson; present/absent attendance; correction to absent/absent; unenrolment and readable former-member history; all four relationship-search categories; history help; complete exit/relaunch; stable IDs, retained history and roster after restart; selective unmark; missing-record no-op; blocked L1 deletion and successful unreferenced L2 deletion.

One submission accidentally appended a new command to the previous rejected input, which the app deliberately retains. It was rejected without changing data. Replacing the complete input and rerunning the command passed. The unsuccessful attempt remains in the transcript. This is not a claim of 33 independent test cases.

The operational JSON is byte-identical before exit and after relaunch plus read-only list/lessons/history checks. New media is linked from the [release notes](notes.md); the [media manifest](media-plan.md) records authorship, captions, source and limits. Ben's original LessonResults.png is retained, with its authorship credited. README/index/UG now consistently use the refreshed Ui.png; UG additionally demonstrates dated attendance and retained history. The old caption's empty-Lesson claim is replaced with what the new capture actually shows.

## Source and documentation checks

- UG and DG each contain the same 13 search-prefix/category combinations as SearchField. All 15 catalogue commands appear in the UG summary. Current people indices, stable Lesson IDs, own-field/relationship search and future-scope distinctions remain consistent.
- All 20 advertised master source paths in DG resolve. Person/lesson/attendance diagram types resolve to implementation; their described ownership matches immutable PersonRecord roles, Lesson-owned rosters and independent AttendanceKey records. Inherited AB3 diagrams remain explicitly labelled historical.
- Owner manual checks, source/reuse/Codex acknowledgements, all accepted headings/explicit aliases and the Requirements appendix are retained. Unsupported legacy contact wording is already absent; no replacement email was invented.
- Full frozen-lockfile builds of accepted and proposed documentation pass: Ruby 3.3.12, Bundler 2.5.11, Jekyll 3.10.0, github-pages 232. Dependencies are unchanged. The expected unauthenticated GitHub-metadata warning is recorded.
- Proposed render: 10 HTML pages, 291 local href/src references, zero missing targets/fragments or duplicate IDs; all 16 UG/DG tables have uniform columns. Local release-document links were checked separately.
- In-app browser: UG, DG, AboutUs and index checked at 1280x720 and 800x720; no page/table horizontal overflow or failed images after page load. The new history capture and its caption were visually inspected in the compact guide. All 19 guide image elements load. Viewport overrides were reset.
- Repository EOF, line-ending, trailing-whitespace and diff checks pass. No additional Java tests were introduced or claimed; earlier independently reviewed #101/#249 evidence retains its original source identity.

## Deployment and remaining integration

GitHub Pages reported a successful build from accepted `c5b8233e` (updated 2026-10-15 14:19:45 UTC). The published UG, DG, AboutUs and index main text matches a fresh render of that commit after whitespace normalization. Published diagrams, portraits, badges and existing screenshots load. This establishes accepted-source/site parity, not deployment of this media follow-up.

PR #249 contains independently approved quick-start observations. PR #251 contains the ownership correction, final guide images and this evidence. Their new text/media must be reviewed and merged by Zhi Yu, then confirmed in the actual Pages deployment before #102 is closed. Final #103 candidate identity, release publication and release-context media/link verification remain separate.

Local reproducible evidence in Vincent's workspace: `_temp/issue102-final/` contains `native-transcript.json`, the isolated app/JAR and launch logs, pre-restart JSON, accepted/proposed Jekyll builds and logs, `audit.py`/`audit.json`, `contract-audit.py`, `parity.py`/`parity.json`, published DOM text, browser measurements and screenshots. Only disposable fixture data was used.
