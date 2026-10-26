# Licence packaging and scrollbar recheck

Prepared by Ernest Chua with OpenAI Codex assistance on 26 October 2026.

Product source: `2f8f1f1036b2a0d3b93af7ca1a95ffd5c8267ecc`, based on team
`3ba8c0ce845fae52c01d81aa57b21bf82b20ba0f`. The subsequent evidence/attribute
commit changes no product or packaged notice bytes. The `.gitattributes`
entries preserve upstream licence files verbatim, including upstream spaces;
they are treated as binary by Git's text checks rather than altered.

`./gradlew check coverage shadowJar` passes: 996 discovered tests, zero
failures/errors, 22 existing platform/assumption skips. Both Checkstyle tasks
pass. `verifyPackagedLicenses` compares 24 actual JAR entries, including the
original SE-EDU MIT notice, complete Apache terms, all four JavaFX modules'
legal files, each resolved Jackson component's notices, and the audio notice.
Each entry must be unique and byte-identical to its source. JavaFX SDK notices
were also compared with upstream source tag `17.0.7+2`; provenance is in
`licenses/README.txt`. No runtime dependency was added or upgraded.

Exact JAR: **12,948,786 bytes**, SHA-256
`2880efe1a7bddfd9ddc0b707e6b83e949903362d1c1f66001b45df25ebd36aba`.
The manifest still reports **1.4**; candidate version/accepted bytes remain
the release packager's separate responsibility under #327/#328. This is a
local verification artifact, not a published release or legal certification.
Preserve corresponding-source availability alongside the final download.

## Actual native result

The exact unchanged JAR was launched in a fresh writable folder with a separate
diagnostic agent on Oracle Java **25.0.4+7-LTS-189**, Windows 11 amd64. JavaFX
Robot entered all four #416 commands using native mouse/keyboard input.
Actual window captures were inspected. Default window **740x600**, scene
approximately **726x562**; compact window **640x480**, scene **626x442**.
Observed display **2048x1152 logical**, **125%** scale (2560x1440 physical).
The compact dimensions were set by the diagnostic; native resizing is not
claimed. Data bytes survived the layout change and the process was closed.

**#451 packaged smoke launch: PASS. #416 scrollbar defect: REPRODUCED.**
At both sizes the short-card people horizontal scrollbar remains visible.
Default observation: `visible=true; max=1.0; width=275.2`. The screenshot shows
the bar beneath Alex's short card. This supersedes any assumption that merged
#377 or closed #370 established a fix. #416's report recheck is completed, but
the defect stays open; Zhi Yu retains people-card implementation ownership.

The harness records `Diagnostic completed=true` for a completed observation,
not a product pass for #416. Earlier local runs caught a hidden-window launch
problem and then correctly stopped on the reproduced scrollbar assertion;
the final runner keeps the application visible and records the observation.
Original raw attempts remain in `_temp/round1/`; no failed attempt is called
a pass. No other platform/scale or independent human acceptance is claimed.

Reproduce with PowerShell:

```powershell
.\validation\packaging-451-20261026\Run-Native.ps1 `
  -JarPath build\libs\ponhub.jar `
  -EvidenceDirectory _temp\round1\new-evidence
```

Committed text logs have only line endings/trailing whitespace normalized;
raw logs, original JAR and disposable fixtures remain local. Screenshots and
the observed results are unchanged. No user store or display setting changed.
