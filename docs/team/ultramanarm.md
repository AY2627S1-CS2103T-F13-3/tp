---
layout: page
title: "Zhu Zhi Yu - Project Portfolio"
---

## Overview

PonHub is a JavaFX desktop application for tuition centre administrators who prefer typing commands.
It brings student, tutor and parent records, shared weekly lessons, enrolments and dated attendance
into one application, reducing the need to maintain separate contact lists and spreadsheets.

My main responsibilities were people-record management, testing and representative sample data,
and the Developer Guide's target user profile, value proposition and prioritised user stories.
I also contributed runtime integration, usability improvements, retrieval extensions and peer review.
The examples below describe merged contributions as of **1 November 2026**.

**Contribution links:** [Code on RepoSense](https://NUS-CS2103-AY2627-S1.github.io/tp-dashboard/?search=ultramanarm&breakdown=true)
| [My pull requests](https://github.com/AY2627S1-CS2103T-F13-3/tp/pulls?q=is%3Apr+author%3Aultramanarm)
| [GitHub profile](https://github.com/ultramanarm)

## Features and implementation

### People records and stable identities

I implemented the immutable Student, Tutor and Parent record models, their shared contact details
and role-prefixed person IDs. I added an ordered people registry with separate allocation counters
for each role, duplicate checks and immutable snapshots. Retaining allocation history means deleting
a record does not allow its ID to be assigned to somebody else. Role-specific duplicate rules allow
siblings to share a parent phone and distinguish same-named people by their identifying phone.
See the [student foundation](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/58),
[tutor and parent models](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/117) and
[ordered registry](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/118).

### Adding, listing and safely deleting people

I built role-aware add parsing, staged person additions, role-filtered listing and a shared resolver
that translates the current displayed position into a stable person identity. This prevents a command
from confusing a filtered list position with an ID suffix or a position in the full list.
I connected the prepared people commands, session, cards and application startup to the team's
canonical data and persistence components. Additions use the existing save-before-publication
transaction boundary so an unsuccessful save does not appear as a successful live change.
See the [index resolver](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/138),
[add/list commands](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/172) and
[runtime integration](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/181).

I implemented guarded person deletion, including checks for lesson, enrolment and retained-attendance
references. I later added a preview showing the person's name and stable ID, followed by explicit
confirmation or cancellation. Confirmation applies to the previewed record; intervening commands
invalidate it, and failed saves preserve the previous data and views. See
[relationship guards](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/212) and
[deletion confirmation](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/483).

### Contact validation and people-card usability

I replaced stack-overflow-prone email matching with a constant-stack scan, added support for Unicode
names and realistic name/address forms, and bounded overlong names before expensive Unicode
normalisation. I also corrected Greek sigma matching for duplicate identities and Tutor selection
while preserving supported saved records. See
[email validation](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/142),
[Unicode names](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/438),
[name/address forms](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/494),
[normalisation bounds](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/584) and
[sigma identity matching](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/595).

I implemented role-aware person cards, added accessible Copy buttons for contact values, and fixed
card sizing at fractional display scales. Copying preserves the displayed value, leading phone zeros
and the unfinished command. I also made active people-search criteria persist above the list so users
can understand why records remain filtered after another command. See
[person cards](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/121),
[contact copying](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/474),
[card sizing](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/504) and
[visible search criteria](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/531).

### Extensions to lesson retrieval and attendance

Building on the team's lesson and retrieval features, I added attendance-history filters by date
and recorded status, exact Tutor-phone filtering, overlapping time-window searches and optional
chronological lesson ordering. These extensions retain stable identities and keep read-only queries
from changing saved data. See
[history filters](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/525),
[Tutor disambiguation](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/527),
[time-window search](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/529) and
[schedule ordering](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/530).

I added feedback when contact edits disconnect Parent–Student phone links, and warnings explaining
when a Lesson edit changes the information displayed alongside past attendance. See
[relationship-change feedback](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/526) and
[attendance presentation guidance](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/528).

## Testing and verification

I contributed automated tests across model, parser, command, storage and JavaFX boundaries. Examples include:

* [Representative people fixtures](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/144)
  covering mixed roles, siblings, same-named people, optional contacts and leading-zero phones.
* [Scheduling and allocation regressions](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/203)
  covering contained overlaps, shared-student conflicts, time boundaries and ID exhaustion.
* [Transaction observer tests](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/208)
  checking save-before-publication, complete callback-time snapshots and silent no-ops.
* [File-boundary regressions](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/209)
  checking atomic-save recovery, symbolic-link bounds and literal logging paths.
* [Hosted Linux JAR verification](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/254)
  exercising the actual packaged application's command box, persistence and restart under Xvfb,
  tied to the supplied artifact's SHA-256. This is hosted verification rather than physical-desktop acceptance.

## Documentation

* **User Guide:** I documented people addition, listing, positions, guarded deletion, confirmation,
  contact rules and copying, together with the syntax and recovery guidance for my later query and
  warning extensions. The corresponding implementation PRs above include these guide changes.
* **Developer Guide:** I wrote the target user profile, value proposition and prioritised user stories,
  then reconciled them with shared lessons and distinguished delivered behaviour from future proposals.
  See [initial requirements](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/38),
  [shared-lesson requirements](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/120) and
  [delivery-status clarification](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/448).
* **Maintainer guidance:** I documented my people APIs and manual checks, improved
  [guide navigation](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/447), corrected
  [people-selection UML notation](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/402), and refreshed
  [setup, testing and logging guidance](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/329).

## Teamwork and peer review

I reviewed teammates' changes with source inspection, focused regressions and follow-up verification.
Examples include [lesson-retrieval integration](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/190),
[bounded JSON nesting](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/473) and
[oversized JSON-number protection](https://github.com/AY2627S1-CS2103T-F13-3/tp/pull/577).

I also coordinated release-readiness work, dependencies and verification evidence in the
[v1.5 delivery tracker](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/327) and
[v1.6 submission tracker](https://github.com/AY2627S1-CS2103T-F13-3/tp/issues/328).

## Attribution

PonHub builds on AddressBook-Level3 and the libraries credited in the
[Developer Guide acknowledgements](../DeveloperGuide.html#acknowledgements).
I used OpenAI Codex across implementation, tests, documentation, review and verification support,
including preparation of this portfolio. I remain responsible for my contributions.
Vincent's underlying persistence work, Benjamin's core retrieval and view features, Yang Shuo's lesson
and attendance features, and Ernest's help and person-edit command work retain their authorship;
my integration and extensions above build on those contributions.
