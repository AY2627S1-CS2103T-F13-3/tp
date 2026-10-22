# Guide and existing-platform audit — #366

Audited on 22 October 2026 by OpenAI Codex for Benjamin Ng (`ben58882`).
This report records fresh evidence inspection and local link checks. The
platform runs below keep their original operators; they were not repeated by
this auditor. Parent coordination records the new suite and workflow checks
separately.

## Candidate reconciliation

The inspected baseline is team master
`84f9a2cd4865b452e14fae5748896853febc8c32`. Comparing it with accepted
application source `ed32fe49901ea11070ff16086490d2d72adafcd1` yields exactly:

```text
release/v1.4/media-provenance.md
release/v1.4/media/restarted-history.png
release/v1.4/media/shared-attendance.png
release/v1.4/notes.md
```

No production source/resources, tests, build/dependency configuration,
wrapper or verification workflow differs. Existing application evidence can
therefore be reused under its original source identity. A later application
change, including #363 or #364, requires affected candidate checks.

Common recorded artifact: `ponhub.jar`, 12,654,205 bytes; SHA-256
`5e24df511c9a5975c1c15e310ab03f2a852837e969359afe71fc8a37fa905456`;
manifest PonHub / 1.4 / `ponhub.Main`, startup `v1.4`.
The new local rebuild/hash verification belongs to this run's automated
report; this audit does not substitute a historical checksum for that result.

## Reused platform evidence

| Evidence | Original operator and environment | Recorded result and limits |
| --- | --- | --- |
| [Windows report](https://github.com/gugrusaurus/tp/blob/93be585a5d2318d153df4646969638907ed63bd9/validation/windows-candidate-272-20261022/README.md) | Vincent manually operated the application with Codex guidance. Windows 10 amd64, Oracle 25.0.4.1+1-LTS-5, packaged JavaFX, assertions enabled. | PASS for create/enrol, both edits, relationships/search, validation/no-ops, independent/corrected attendance, former history, F1/menu and two restarts. 29 screenshots and saved checkpoints retained. No protected-session, injected save-failure or new scaling run. |
| [Mac report](https://github.com/gugrusaurus/tp/blob/be8e2a2326fe7a42677b74036940d5b609f3d3bb/validation/mac-candidate-272-20261022/README.md) and [retry summary](https://github.com/gugrusaurus/tp/blob/be8e2a2326fe7a42677b74036940d5b609f3d3bb/validation/mac-candidate-272-20261022/retry/summary.json) | Codex operated native workflows for Vincent; Vincent manually clicked Help. macOS 27.0.1/26A434 arm64, Azul 25.0.3.fx-zulu, Java 25.0.3+9-LTS with bundled JavaFX, assertions enabled. | PASS for representative workflows, edits/search/history, rejection/no-op bytes, restart, permission-failed save/retry, protected restore/restart and Help draft/context. Default 740×600 logical window and native zoom inspected. No new complete scaling matrix or separate selected-row preservation claim. |
| [Linux package run 39706371977](https://github.com/AY2627S1-CS2103T-F13-3/tp/actions/runs/39706371977) | Repository workflow, dispatched by Zhu Zhi Yu (`ultramanarm`); Ubuntu 24.04.5 x86_64, Azul 25.0.4+7, Xvfb 1280×800. | PASS for the retained 32-command packaged sequence, both edits, rejected/clashing edits, no-op byte preservation, attendance correction, former history and restart. Automated virtual-display execution; no physical Linux desktop or independent human interaction claim. |
| [Application-source Java CI](https://github.com/AY2627S1-CS2103T-F13-3/tp/actions/runs/39706233849) and [materials-master Java CI](https://github.com/AY2627S1-CS2103T-F13-3/tp/actions/runs/39708755853) | GitHub-hosted three-platform Java 25 with JavaFX jobs. | Both runs were read back as completed/success at their exact ed32fe49 and 84f9a2cd heads. Source-suite checks do not establish packaged GUI acceptance. |

All platform rows refer to the common exact artifact above. The Windows
report and Mac retry summary were read directly at their immutable fork
commits; the readbacks are retained beside this report. The Linux run's live
API record was read back as completed/success at `ed32fe49`. Its retained
[artifact 12395006972](https://github.com/AY2627S1-CS2103T-F13-3/tp/actions/runs/39706371977/artifacts/12395006972)
and committed release provenance supply the existing detailed transcript and
runtime scope. No fresh artifact download or physical platform run is
claimed by this audit.

## Shutdown diagnostic and acceptance history

The original Mac exit reported JavaFX `InvokeLaterDispatcher` /
`Main Java thread is detached` after stopping. Original logs remain retained;
the process stopped and saved data remained intact. The immutable retry
summary records two normal and two protected/recovered shutdown cycles with
no recurrence. The [final #272 consolidation](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/272#issuecomment-6333007859)
also records two clean supplemental exits. That consolidation dispositioned
the diagnostic within its original bounded acceptance scope. No product fix
or proven root cause is established by clean retries. The earlier empty
JavaFX screen-list launch failure remains a failed attempt in its original
context, followed by successful native-context execution of unchanged bytes.

#271/#272/#273 are completed original-scope acceptance/handoff records.
Their historical comments still describe #362 as a future gate. The newer
[final #362 disposition](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/362)
supersedes that status: its remaining guided-human checks were cancelled and
remain NOT RUN. This audit does not reopen that task or turn the cancelled
checks into passes. Ben separately requested #366's present testing.

## Guide and release-copy checks

The UG quick-start/edit/help examples and DG manual edit, shared-Lesson,
attendance, selector, failed-save and recovery procedures were inspected
against the integrated command contracts and existing evidence. Release
notes retain the same syntax, Java requirement, source-pinned guide links,
protected-session limits and future-feature distinctions. The two release
images are correctly described as hosted Linux captures. Their provenance
retains the compact viewport and S2-feedback/S1-history distinction. No
unsupported feature claim or new guide defect was found in this inspection.
New command execution belongs to the workflow and native reports.

A **fresh locked Jekyll build passed** in the existing Lima Linux guest,
using Ruby 3.3.8 (`aarch64-linux-gnu`), Bundler 2.5.11 and Jekyll 3.10.0.
The automated-validation agent built a disposable copy of the current docs,
including this contribution's Testing section, with:

```sh
BUNDLE_FROZEN=true BUNDLE_PATH=/tmp/ponhub366-bundle \
    bundle _2.5.11_ exec jekyll build --baseurl /tp \
    --destination /tmp/ponhub366-site/rendered
```

The actual build log, runtime and input hashes are in
`../automated/site-build.log` and `../automated/site-summary.json`.
Testing.md SHA-256 is
`1b963dc1666691c307433eb4011ed4b106a1a6c09de494109073f6f6d7f3dfd6`;
the Gemfile, lockfile and Testing inputs matched the host before/after
building. No shared dependency file changed. The existing Faraday retry
middleware notice was non-blocking and remains in the log.

A fresh local-target/source-existence check used the existing #224 checker
against the new rendered site at `/private/tmp/ponhub366-site-preview/tp`.
Result: **PASS within this rendered-link/source-link scope**: 9 HTML pages,
414 local targets, 30 image references, 26 tables, 5 README targets and
52 source references (51 files, one directory; 42 unique paths, 47 namespace
references); zero failures. All 12 active diagram pairs remain embedded with
their rendered image present. See `links.json` and `namespace-links.tsv`.

The historical checker was executed with ROOT set to the current checkout,
SITE set to the fresh render, and OUT set to this audit directory. Its
`Path.write_text(newline=...)` argument was removed only in the in-memory
adapter because host Python 3.9 does not support it. The initial adapter
failure was a tooling error before report output; the adjusted check passed.
Historical reports and checker source were not changed.

The earlier provisional check against retained #224 render output passed
412 targets after establishing equality of all 28 compared baseline guide/
template/configuration inputs. Its files remain as `cached-links.json`,
`cached-namespace-links.tsv` and `cached-render-inputs.json`; the fresh build
and 414-target check supersede that provisional result for the new section.
The host Ruby 2.6/Bundler 1.17 limitation was resolved through the guest's
task-local frozen dependencies, without changing global gems or lockfiles.

`docs/Testing.md` now explains scenario outcomes, exact candidate/runtime/
operator identity, byte-preservation claims, retained diagnostics and reuse
of unchanged evidence. Its new DevOps release-procedure link was checked
against the fresh `making-a-release` target. The new
`recording-workflow-and-package-results` heading and its TOC link are present
in rendered Testing.html. This is durable recording
guidance; dated outcomes remain in validation/issues.

The parent's fresh browser preview passed at 1280×800 and 390×844, with the
new section readable, zero broken images and no page-level horizontal
overflow. See `browser-checks.json`, `testing-desktop.png` and
`testing-narrow.png`. This audit independently inspected the narrow capture;
the preview is separate from the build/link check. External HTTP, deployed
site, public-release/download and a new full display matrix are separate
checks.

Teammate-owned #363 compact startup and #364 menu-readability verification
remain separate work. This audit does not assume those tasks are complete,
publish a release, change owners/deadlines or replace teammate review.
