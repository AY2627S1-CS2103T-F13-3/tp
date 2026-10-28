# Optional packaged UI checks

These standalone JavaFX agents exercise a supplied PonHub JAR in disposable folders.
They supplement the [Developer Guide's manual procedures](../../docs/DeveloperGuide.md#appendix-instructions-for-manual-testing).
They are optional diagnostics, not additional release gates or a record of acceptance.
Ernest Chua created the agents and Windows launchers with OpenAI Codex assistance.

Use a graphical Windows session with Java 25 available as `java.exe`, `javac.exe` and
`jar.exe`. Keep PonHub in the foreground while a Robot agent controls the mouse/keyboard.
Use only fictitious data, and choose a fresh absolute output directory **outside the
repository**. The launchers copy the supplied JAR into a new disposable application folder;
they do not use your operational data. The command-completion launcher additionally needs
`-JavaHome` pointing to the selected Java 25 installation.

From the repository root in PowerShell:

```powershell
.\tools\manual-checks\command-recall\Run-Native.ps1 -JarPath C:\candidate\ponhub.jar -EvidenceDirectory C:\Temp\ponhub-recall
.\tools\manual-checks\duplicate-feedback\Run-Native.ps1 -JarPath C:\candidate\ponhub.jar -EvidenceDirectory C:\Temp\ponhub-duplicates
.\tools\manual-checks\panel-borders\Run-Native.ps1 -JarPath C:\candidate\ponhub.jar -EvidenceDirectory C:\Temp\ponhub-panels
.\tools\manual-checks\command-completion\Run-NativeCheck.ps1 -JarPath C:\candidate\ponhub.jar -JavaHome C:\Java\jdk-25 -EvidenceDirectory C:\Temp\ponhub-completion
```

For command completion, repeat with a different output directory and `-Protected` to
exercise a malformed disposable store. The panel runner accepts `-SourceCommit SHA` when
the supplied JAR comes from a different checkout. Other launchers record the current
checkout SHA; verify the supplied JAR's source separately instead of assuming they match.

| Agent | Scenario |
| --- | --- |
| `command-recall/Round1Agent.java` | Up/Down order and boundaries, draft/selection restoration, failed submissions, Tab, Enter, and empty recall after restart. |
| `command-completion/CompletionAgent.java` | Tab completion and cycling, draft/caret changes, keyboard traversal, Help/F1, and protected-session command availability. |
| `duplicate-feedback/Round1Agent.java` | Duplicate Student, Tutor and Parent feedback and unchanged stored data. |
| `panel-borders/Round1Agent.java` | Divider dragging, independent scrolling, focus/draft preservation and unchanged stored data. |
| `compact-feedback/CompactFeedbackAgent.java` | Compact startup feedback, resizing, long help, divider persistence and Help/F1 draft preservation. |

The compact-feedback agent can be compiled and packaged as a Java agent using the same
`javac -cp CANDIDATE_JAR` and `jar cfm` steps as the supplied launchers, with a manifest
containing `Premain-Class: CompactFeedbackAgent`. Launch it from a fresh writable folder
with `java -javaagent:AGENT_JAR=ABSOLUTE_OUTPUT_DIRECTORY -jar CANDIDATE_JAR`.
Compile each agent separately; several intentionally share the `Round1Agent` class name.

Inspect reported failures, screenshots and runtime details before drawing conclusions.
Programmatic resizing, a Robot action or a successful assertion does not establish human
acceptance or another platform/display combination. Store necessary results in the relevant
issue description/comments; do not commit generated logs, screenshots or result files.
