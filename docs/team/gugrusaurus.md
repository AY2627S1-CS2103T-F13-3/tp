---
layout: page
title: "Vincent Lin - Project Portfolio"
---

## Overview

PonHub is a JavaFX desktop application for tuition centre administrators who prefer typing commands.
It brings people records, shared weekly lessons, enrolments and dated attendance into one application,
reducing the need to maintain separate contact lists and spreadsheets.

My main responsibilities were saving and validated loading, complete data snapshots, transaction
coordination and recovery guidance. I also contributed UI improvements, testing, documentation,
release materials and deliverable coordination. The examples below describe merged contributions
as of **2 November 2026**.

**Contribution links:** [Code on RepoSense](https://NUS-CS2103-AY2627-S1.github.io/tp-dashboard/?search=gugrusaurus&breakdown=true)
| [My pull requests](https://github.com/AY2627S1-CS2103T-F13-3/tp/pulls?q=is%3Apr+author%3Agugrusaurus)
| [GitHub profile](https://github.com/gugrusaurus)

## Features and implementation

### Complete snapshots and versioned persistence

I introduced `PonHubData` and immutable `PonHubDataState` snapshots to hold ordered people, shared
lessons, lesson-owned rosters, dated attendance and allocation history together. Snapshot equality
includes ordering and counters, so deleting a record does not discard the information needed to
prevent ID reuse. This aggregate builds on the team's person, Lesson and attendance record models.
See the [snapshot foundation](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/158).

I built the versioned JSON codec and extended it as shared lessons and attendance became available.
Loading validates the complete candidate, including field types, duplicate identities, role-specific
references, scheduling conflicts and allocation bounds, before it can become live. Saving preserves
people order, optional contacts, leading-zero phone numbers, empty rosters and attendance retained
after unenrolment. See [people persistence](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/160),
[shared-Lesson persistence](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/174) and
[retained attendance persistence](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/186).

### Save-before-publication transactions

I implemented the boundary that prepares a command against isolated data and view state, saves a
changed operational snapshot, and publishes it only after saving succeeds. A failed command or save
therefore does not leave a rejected change in memory for a later command to persist. Operational
no-ops and view-only changes skip the file write. Zhu Zhi Yu subsequently connected the canonical
boundary to the active runtime, shared view and people commands. See
[staged execution](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/170).

I later added a programmer invariant that detects a staged callback changing live records, counters
or filters before the writer runs. It detects misuse without claiming to undo a faulty callback's
own changes. See the [live-state invariant](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/409).

### Safe file replacement and protected startup

I replaced direct JSON writes with a complete temporary sibling file followed by atomic replacement
where supported. The fallback retains a recovery backup before ordinary replacement and attempts
restoration after failure. Symbolic links are resolved without replacing the link itself; unsafe
or inaccessible destinations are rejected. These measures reduce partial-write risk without claiming
power-loss durability. See [safe JSON replacement](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/111).

I implemented `ProtectedPonHubDataStorage` so an unreadable, invalid, unsupported or legacy file
cannot silently become an empty writable store. A rejected session preserves the original and blocks
operational saves until a new instance restarts and validates the repaired data. Preferences retain
their separate lifecycle. See [protected loading](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/171).

I also added protection against an accidental second window overwriting newer changes. Storage
checks the resolved destination and loaded file baseline under an operating-system lock before
replacement. A stale instance rejects saving before publication and asks the user to restart.
This coordinates cooperating application instances; it is not live synchronisation with external
editors. See [stale-save protection](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/472).

### Input, diagnostics and resource safeguards

I bounded deeply nested JSON and oversized numeric tokens before recursive tree construction or
expensive number conversion. I applied corresponding limits to preferences and bounded their
failure diagnostics, preserving normal recovery to defaults. See
[nesting protection](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/473),
[operational numeric bounds](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/577) and
[preference limits](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/579).

I prevented pasted control characters from silently changing command data. Rejected replacements
preserve the draft and selection, and Enter remains blocked until the text changes. I also removed
raw command arguments and result bodies from routine logs while retaining bounded command outcomes
and cause classes. See [paste protection](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/576) and
[command logging](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/408).

### UI and shared command infrastructure

I built the themed application shell with a full-width command dock beneath the resizable record
columns, then fixed divider drift between roster and history views. I also unified command words,
help text and protected-session eligibility in `CanonicalCommandDefinition`, with exhaustive routing
to detect missing handlers when developers add commands. See the
[Lesson board shell](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/334),
[divider correction](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/354) and
[command definitions](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/333).

I added optional logo percussion with background playback, replay guards and cleanup, using an
original procedural sound and a committed generator and licence. I also implemented the logo-triggered
developer credits, with animation cleanup and preservation of command drafts and operational data.
See [logo audio](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/431) and
[developer credits](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/501).

### Query lifetime and scheduling efficiency

Building on Benjamin's query views, I stopped deleted identities from remaining permanently owned
by strong result caches. Externally held read-only lists retain their identity and restore behaviour;
unused retired handles can be collected. See
[query-cache lifetime](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/581).

I indexed schedule validation by weekday and resource, avoiding comparisons between unrelated
bookings while retaining conflict precedence and diagnostics. Independent tests compare results
against a pairwise oracle over 4,000 seeded catalogues. Performance measurements in the PR are local
observations, rather than response-time guarantees. See
[schedule indexing](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/589).

## Testing and verification

I contributed automated tests alongside the features above, including:

* Complete persistence round-trips and rejection cases for people, shared lessons, retained attendance,
  ordering and allocation history, including failure cases that preserve both live state and file bytes.
* [Path and preference-isolation tests](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/176)
  covering alternate paths with spaces, rejected directory destinations and preference saving after
  an operational load failure.
* Assertions-enabled and assertions-disabled JVM checks for transaction invariants, failed saves,
  no-ops and view-only commands, plus separate-process save races and lock-release tests.
* JSON depth and numeric boundaries, protected startup and preference recovery, rejected paste
  behaviour, optional audio failures, and developer-credits animation and focus restoration.

I also adjusted the Linux packaged verifier after privacy-safe logging removed its old payload-based
synchronisation, retaining checks against actual result-control feedback. I later made packaged
verification use the triggering candidate's version. See the verifier changes in
[command logging](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/408) and
[candidate-version verification](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/539).
The linked records distinguish automated, hosted and native checks and identify the candidates tested;
they do not establish acceptance of every later integrated build.

## Documentation

* **[User Guide](https://ay2627s1-cs2103t-f13-3.github.io/tp/UserGuide.html):** I documented saving,
  protected startup, recovery and retained history, and improved task-based navigation and the quick
  start. I added the expandable contents at the beginning of the guide. See
  [recovery guidance](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/296),
  [task-based organisation](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/604) and
  [User Guide contents](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/637).
* **[Developer Guide](https://ay2627s1-cs2103t-f13-3.github.io/tp/DeveloperGuide.html):** I documented
  snapshots, storage and publication boundaries, proposed a simpler guide structure, and refined
  component responsibilities. The team's later guide integration retains its own authorship. See
  [storage documentation](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/228),
  [guide rewrite](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/598),
  [expandable contents](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/631) and
  [component boundaries](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/647).
* **Diagrams and examples:** I simplified the UI class diagram, explained lesson retrieval through
  a sequence diagram, and corrected the fixture prerequisite for the people manual tests. See
  [UI layout](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/616),
  [retrieval sequence](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/618) and
  [test-fixture checkpoint](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/614).

## Teamwork and peer review

I coordinated deliverable and deadline follow-up, prepared release materials, and reconciled guide
media and release notes with integrated candidates. Examples include
[integrated release media](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/251) and
[combined-candidate materials](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/376).
I worked with feature owners on the handoff from prepared storage and transactions to the active
runtime, keeping persistence responsibilities distinct from command and UI integration.

I reviewed [Zhu Zhi Yu's portfolio](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/613#pullrequestreview-5727612384),
checking the full change, contribution attribution, cited merged PRs and published review evidence,
then checking browser readability and navigation before approval.

## Attribution

PonHub builds on AddressBook-Level3 and the libraries credited in the
[Developer Guide acknowledgements](https://ay2627s1-cs2103t-f13-3.github.io/tp/DeveloperGuide.html#acknowledgements).
I used OpenAI Codex for implementation, testing, documentation, review and verification support,
including preparation of this portfolio. I remain responsible for my contributions.
Zhi Yu's people models and runtime integration, Benjamin's retrieval and view features, Yang Shuo's
Lesson and attendance features, and Ernest's help, person-edit commands and guide integration retain
their authorship. My persistence and shared infrastructure build on and support those components.
