# Combined candidate and historical media provenance

Reconciled by Vincent Lin with OpenAI Codex assistance, 22 October 2026.

Current application source: c92c689ee8802fb2cb3957832c25dd9407099dbf. Exact frozen ponhub.jar: 12,655,415 bytes; SHA-256 8fbd0e5414d91c006984489b71083445da41d22f76f37c2fbc49a681d79ac9d6; manifest PonHub / 1.4 / ponhub.Main. See [freeze and affected-check handoff](https://github.com/gugrusaurus/tp/blob/5abc52528bdfd608f001b2f0aa26fc60436123a7/validation/final-candidate-20261022/README.md).

The current notes link to this source's guides and include menu readability, compact startup feedback and live heading tooltips. The two older screenshots remain unchanged below as historical workflow evidence. They have been removed from the proposed release body because the feedback layout changed. No old capture is relabelled as the new package. New affected-package media/results remain pending Ernest's bounded check; source/material review and final publication acceptance are separate.

For publication, use source c92c689ee8802fb2cb3957832c25dd9407099dbf and the frozen checksum above; use the actual reviewed materials commit for document/media links. Older application-tag guidance in the historical record below is superseded. Original authorship, runtime limits and image checksums remain valid for those old captures.

---

## Historical record — original text retained

# v1.4 release copy and hosted capture provenance

Materials for [#273](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/273), prepared by Zhu Zhi Yu
with OpenAI Codex assistance. Vincent Lin (`gugrusaurus`) retains accountable ownership.
Milestone **v1.4**; completion due **20 October 2026, 19:00 SGT (UTC+8)**; Target date **2026-10-20**.
The original deadline is unchanged and overdue.

## Application identity and scope

- Application source: [`ed32fe49901ea11070ff16086490d2d72adafcd1`](https://github.com/AY2627S1-CS2103T-F13-3/tp/tree/ed32fe49901ea11070ff16086490d2d72adafcd1).
- JAR: `ponhub.jar`, **12,654,205 bytes**; SHA-256
  `5e24df511c9a5975c1c15e310ab03f2a852837e969359afe71fc8a37fa905456`.
- Manifest: PonHub / 1.4 / `ponhub.Main`.
- [Successful hosted run 39706371977](https://github.com/AY2627S1-CS2103T-F13-3/tp/actions/runs/39706371977),
  dispatched by `ultramanarm`; 22 October 2026, **09:33:28–09:34:59 SGT**.
- Original artifact **12395006972** (`linux-package-evidence`) on the linked run retains source/result/runtime,
  all 32 transcript entries, logs, exact JAR and saved fictitious fixture. Retrieval requires GitHub sign-in.
  Its recorded expiry is 21 November 2026; the two selected images are preserved here independently.
- Ubuntu **24.04.5 LTS x86_64**, kernel 7.0.0-1017-azure; Azul Zulu **25.0.4+7-LTS**,
  with the packaged JavaFX dependencies; X11/Xvfb **1280 × 800 × 24**.
  Actual application captures: **738 × 571 pixels** for shared attendance and **736 × 542** after restart.
  No display-scaling setting was recorded.

These are actual packaged-JAR captures made with the workflow’s ImageMagick window capture,
not generated mockups or attached-scene snapshots. They establish the shown hosted Linux results;
they do not establish physical-display, Windows, Mac or a full display/scaling acceptance matrix.
The notes describe source-checked contracts beyond these two visible flows. Full #271/#272
candidate acceptance, substantive non-author materials review and #274 publication remain separate.

The application identity above remains the packaging/source baseline. This materials-only commit
adds prose and PNGs; it does not require rebuilding the application or changing that identity.

## Preserved files and captions

The files are copied byte-for-byte from the final run, without cropping, resizing or pixel changes.

| File | Bytes | SHA-256 | Visible result |
| --- | --- | --- | --- |
| [shared-attendance.png](media/shared-attendance.png) | 62,818 | `9989a02c2d66522fbc790376e94a6447b3eddfd8b0887848174fed7016d9a6c0` | Edited Alex Lee/P3 retains S1. L1 shows Advanced Math, Mon 1630–1730, room R3, roster 2 and independent present/absent attendance. |
| [restarted-history.png](media/restarted-history.png) | 53,724 | `9c70c49d778e23957226b76dec6e923f3eafe58aada31b1223c6c9f620dd89cd` | After restart, S1’s absence remains with former-enrolment context. Feedback is the subsequent unchanged mark for S2; it is not S1’s history result count. |

The shared-detail context and lower roster area extend beyond the compact viewport. Captions only
claim the readable fields shown; these images do not prove complete long-roster scrolling or
unclipped layout at every size. The path in the status bar belongs to the disposable runner fixture.

## Actual command sequence

The workflow first submitted `help` to prove command-box focus. It then ran:

```text
add r/tutor n/Mei Lim p/92345678 e/mei@example.com
addlesson d/Mon st/1600 et/1730 s/Math tu/Mei Lim rm/R1
addlesson d/Mon st/1730 et/1830 s/Science tu/Mei Lim rm/R2
add r/student n/Alex Tan l/S2 pp/91234567
add r/student n/Jamie Tan l/P1 pp/91234567
list r/student
enrol 1 lid/L1
enrol 2 lid/L1
edit 1 n/Alex Lee l/P3
editlesson lid/L1 st/1630 s/Advanced Math rm/R3
mark 1 lid/L1 d/2026-10-05 s/present
mark 2 lid/L1 d/2026-10-05 s/absent
```

It rejected `edit 1 l/P7`, `edit 1 n/Jamie Tan pp/91234567` and
`editlesson lid/L2 st/1700`, checking unchanged saved bytes. The normalized no-op
`edit 1 n/ Alex   Lee l/p3` also preserved bytes. Then
`showlesson lid/L1 d/2026-10-05` produced **shared-attendance.png**.

Next it ran `mark 1 lid/L1 d/2026-10-05 s/absent`, `unenrol 1 lid/L1`,
`history 1 lid/L1` and `exit`. After reopening the same JAR and store, it submitted:

```text
help
list r/student
lessons
lessons si/1
lessons si/2
showlesson lid/L1 d/2026-10-05
history 1 lid/L1
mark 1 lid/L1 d/2026-10-05 s/absent
mark 2 lid/L1 d/2026-10-05 s/absent
```

Both final marks reported already absent/no changes. It captured **restarted-history.png**,
exited and verified that shutdown/restart/read-only/no-op operations preserved the saved bytes.
The retained transcript and state checks verify Jamie remains enrolled while Alex does not.

## Credits and publication handoff

Yang Shuo authored the v1.4 package scenarios, Vincent Lin corrected bottom-dock focus, and
Zhu Zhi Yu authored the earlier Linux verification foundation. The repository workflow generated
these captures automatically on a GitHub-hosted runner. Zhu dispatched the final run and prepared
this copy/provenance with OpenAI Codex assistance; this is not a claim that a human operated a
physical Linux display. Feature ownership remains with the contributors in the pinned
[About Us](https://github.com/AY2627S1-CS2103T-F13-3/tp/blob/ed32fe49901ea11070ff16086490d2d72adafcd1/docs/AboutUs.md).
Ernest Chua’s original icon/wordmark/theme and Benjamin Ng’s retrieval/view work retain their credit.
Earlier Vincent/Benjamin v1.3 media and its source identities remain in
[the existing manifest](../v1.3/media-plan.md); they have not been relabelled as these captures.

[notes.md](notes.md) is the proposed release body. Its guide/application links are already pinned to
the application source above. Relative image/provenance links work in the repository. Before copying
the body to a GitHub release, resolve its image paths to absolute raw URLs at the actual reviewed
materials commit and its provenance path to that commit’s GitHub blob URL. Preserve the selected
filenames, captions and credits; verify both rendered images and every document link on the release
page. The application tag/JAR source stays `ed32fe49901ea11070ff16086490d2d72adafcd1`; the
reviewed materials commit supplies document/image URLs. Record the real publication time and verify
the public JAR separately under #274.

This increment is **part of #273**. It does not close the issue, publish a release, supply a teammate
review or waive remaining candidate/platform acceptance.
