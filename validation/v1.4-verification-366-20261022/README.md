# Comprehensive v1.4 verification — #366

On 22 October 2026, OpenAI Codex and three delegated agents performed this
verification for Benjamin Ng (`ben58882`) following his request to test the
current build comprehensively and update the documentation. All new execution
is assistant-operated contributor evidence. Original platform operators retain
their authorship; no new human or independent teammate acceptance is claimed.

**PASS for the executed scope below. No additional product defect was observed.**
The known startup-height and menu-readability tasks #363/#364 remain with their
owners. This record covers the identified baseline; changes to application
inputs require affected checks on a refreshed candidate. Publication and
iteration closeout remain separate owner decisions.

## Candidate and environment

| Item | Verified identity |
| --- | --- |
| Tested team baseline | `84f9a2cd4865b452e14fae5748896853febc8c32` |
| Previously accepted application source | `ed32fe49901ea11070ff16086490d2d72adafcd1` |
| Relationship | Only four release-material files differ; application, resources, tests, build inputs and workflows are identical. |
| Freshly rebuilt artifact | `ponhub.jar`, 12,654,205 bytes |
| SHA-256 | `5e24df511c9a5975c1c15e310ab03f2a852837e969359afe71fc8a37fa905456` |
| Manifest / startup | PonHub / 1.4 / `ponhub.Main`; startup `v1.4` |
| New application execution | macOS 26.6.2, build 25G83, arm64; prescribed Azul `25.0.3.fx-zulu`, Java 25.0.3+9-LTS with JavaFX |
| Test data | Fresh, isolated fictitious records under `/private/tmp/ponhub-366-*`; user operational data was not used. |

The parent inspected the clean working tree, fetched both remotes and
fast-forwarded local/fork master to this baseline before creating the fresh
`codex/366-verify-v14-workflows` branch. No open teammate PR existed at initial
inventory. This contribution changes testing guidance and validation evidence;
it does not change production code, checked-in JUnit tests or application bytes.

## Results

| Result | Executed check | Evidence and practical limit |
| --- | --- | --- |
| PASS | Clean complete `check coverage shadowJar` | [Build log](automated/gradle.log), [summary](automated/summary.json), [JUnit summary](automated/test-results.xml): 897 tests in 130 suites, zero failures/errors/skips; production and test Checkstyle pass. |
| PASS | Independent command/session campaign | [Runnable probe and matrix](workflows/README.md): 11 groups, 178 commands, 644 assertions, including 80 expected rejections. Covers live search/index refresh, both edits, conflicts, no-ops, former attendance, guards, restart, three protected formats and four injected failed saves with retry. |
| PASS | Exact packaged Mac workflows | [Native report](native/README.md): 70 command-box submissions plus three Help/navigation logic actions, five normal launches and five clean exits. Includes a real filesystem save obstacle/retry, protected startup/repair/restart and missing-file read-only checks. |
| PASS | Operational-byte integrity | Native rejection/no-op, restart/query, save-failure and protected-file records preserve their expected hashes. The session probe separately verifies snapshots, projections, counters and persistence calls. |
| PASS | Bounded native layout and input checks | Default 740×600 and compact 457×367 logical windows; feedback divider dragging, long help, long contact wrapping/scroll access, selection, Help/F1 and navigation draft preservation. Popup appearance and a full scaling/platform matrix remain outside this new run. |
| PASS | Existing Windows/Mac/Linux evidence reconciliation | [Source-pinned matrix](audit/README.md#reused-platform-evidence) preserves actual operators, runtime, artifact, virtual-display limits and the earlier shutdown diagnostic disposition. These are reused runs. |
| PASS | UG/DG/release-copy contract audit | [Guide audit](audit/README.md#guide-and-release-copy-checks): inspected current examples and procedures against the source and executed workflows; no new mismatch found. |
| PASS | Fresh locked documentation build | [Site summary](automated/site-summary.json): Ruby 3.3.8, Bundler 2.5.11, Jekyll 3.10.0; current Testing update rendered with unchanged Gemfile/lockfile. Task-local dependencies in the existing guest; guest restored to stopped. |
| PASS | Fresh rendered targets and source references | [Link report](audit/links.json): nine pages, 414 local targets, 30 image references, 26 tables and 52 source references; zero failures. |
| PASS | Changed-page browser preview | [Measurements](audit/browser-checks.json), [desktop](audit/testing-desktop.png), [narrow](audit/testing-narrow.png): new section readable at 1280×800 and 390×844, no page-level horizontal overflow or broken images; temporary viewport override reset. |

Whole unchanged production coverage is **3389/3460 lines (97.9480%)** and
**1321/1351 branches (97.7794%)**. This describes the complete existing suite;
it is not a claim of 100% product coverage. There is no changed production
code or Codecov production patch in this documentation/evidence contribution.
The standalone verification Java also passed repository Checkstyle and a
manual SE-EDU inspection after an independent agent review.

## Documentation outcome

`docs/Testing.md` now explains how to record scenario outcomes, candidate and
operator identity, unchanged-byte claims, diagnostics and reused evidence.
Dated outcomes and transcripts remain here. The tested UG/DG command
contracts remain consistent with the observed implementation, so no command
syntax or behavior description was changed merely to record a passing run.

## Retained failures and limits

- A first packaged attempt inside the restricted runner failed before a
  usable UI with an empty JavaFX screen list. The same verified bytes started
  after native display access was enabled. [Original log](native/launch.log)
  and [host launch log](native/launch-host.log) remain retained. Five later
  normal wrapper sessions closed without an unexpected shutdown diagnostic;
  this does not establish a root cause or product fix for historical failures.
- Native AX `setValue`, an offscreen-row click and a stale element mapping
  did not perform their intended actions. The successful retry used native
  paste/current screenshot coordinates. Case-sensitive or diff-only
  readback matchers were corrected against the actual observed response.
  [Raw interaction records](native/commands.json) preserve those attempts;
  they are not product failures or additional successful submissions.
- The save-failure probe uses the existing injected IO seam. The new native
  check separately used a regular-file obstacle at the disposable data
  directory, preserving the relocated original file before one successful
  retry. Neither is a new physical Windows/Linux permissions test.
- **NOT RUN here:** new Windows/Linux native interaction, complete display
  scaling matrix, separate human pass, complete popup readability states,
  deployed website, public release/download verification or release closeout.
  Those are not inferred from source tests, this Mac run or reused evidence.
- #362's cancelled human remainder stays NOT RUN. Its cancellation does not
  cancel Ben's separately requested #366 campaign, grant publication authority
  or turn unperformed checks into passes.

Teammate review and current PR checks remain required before integration.
The detailed original platform scope, diagnostic disposition and owner
dependencies are preserved in the [audit](audit/README.md).
