## Root Cause Analysis and Problem Framing

For non-trivial bugs, unexpected behavior, domain inconsistencies, security-sensitive changes, and problems involving multiple edge cases, do not start by patching the observed symptom.

Before implementing a fix, first determine what underlying condition makes the observed behavior possible.

### Analyze Before Fixing

Before changing production code:

* Reconstruct the relevant execution and data flow far enough upstream to understand where the problematic state or behavior originates.
* Identify the domain invariants that should hold and determine where the first invariant is violated.
* Distinguish between:

  * the observed symptom,
  * intermediate consequences,
  * defensive or edge-case handling triggered by those consequences,
  * and the underlying defect that created the invalid state.
* Ask repeatedly: **"Why can this state exist?"** until reaching the earliest actionable cause rather than stopping at the first location where the problem becomes visible.
* Inspect callers, state creation, validation, persistence, serialization, and downstream consumers when relevant. Do not assume the defect is located where the exception, failing test, or incorrect result appears.
* Examine whether existing edge-case handling, compensating logic, retries, special cases, or validation exists because of an earlier defect.
* Search for other code paths affected by the same underlying condition.

Do not implement a fix until you can explain the following, and communicate the relevant conclusions to the developer:

1. What is the incorrect behavior?
2. What invariant or intended behavior is violated?
3. Where is the invalid state or decision first introduced?
4. Why is it possible for that to happen?
5. What other behaviors are consequences of the same cause?
6. What code becomes unnecessary if the root cause is corrected?
7. Could the proposed fix leave another path to the same invalid state?

### Explain the Root Cause Explicitly

* For every issue investigated, reviewed, or fixed, explicitly state the identified root cause and explain how it produces the observed behavior. Scale the explanation and investigation to the complexity and impact of the issue.
* Distinguish confirmed causes from hypotheses. Support the explanation with relevant evidence, such as the execution path, state transition, violated invariant, or ownership decision.
* If the root cause remains unknown or only partially understood, say so explicitly. State what is established, what remains uncertain, and what investigation would resolve that uncertainty. Never invent a cause to satisfy the reporting requirement.
* Do not describe only the exception, missing check, or failing operation when these are consequences of an earlier defect. Explain why the invalid state was allowed to exist and which component should have prevented it.
* State whether the proposed or implemented change removes the cause, mitigates its consequences, or does both. Identify any remaining paths to the same failure.
* Include the causal explanation in the findings or final response, even when the underlying correction is outside the authorized implementation scope. Do not leave it only in internal reasoning, code comments, or a commit message.

### Prefer Removal of Causes Over Addition of Handling

Prefer fixes that restore the correct invariant and make invalid states impossible or substantially harder to create.

When a root-cause correction is feasible and architecturally sound, explicitly recommend it as the preferred solution. Do not default to a symptom-level patch merely because it is easier, changes fewer lines, or matches the initial suggestion. Implement within the authorized scope; use the effort and risk assessment below when the sound correction materially expands that scope.

When correcting the root cause:

* Re-evaluate downstream edge-case handling that existed only because of the bug.
* Remove obsolete workarounds, compensating logic, special cases, and defensive branches when they are no longer needed.
* Do not preserve complexity merely because it already exists.
* Do not add another conditional branch if correcting an earlier invariant makes the branch unnecessary.
* Prefer reducing the number of possible invalid states over adding handling for each invalid state separately.
* Prefer enforcing correctness at the earliest appropriate trust or state-transition boundary.
* Remove defensive handling only after establishing that it is obsolete. Preserve checks needed for independent trust boundaries, previously persisted invalid data, backward compatibility, or other valid failure modes.

A good fix often makes code **simpler**, because behavior that was previously treated as an edge case becomes impossible.

### Fix the Step That Creates the Problem, Not the Step That Detects It

* The code which detects a problem (a validator, a verification rule, a failing test, an exception) is usually not the code which creates it. Before changing detection or handling code, identify the earlier step which makes the invalid state or the unwanted capability possible, and correct that step.
* Ask for every finding: "Why can this happen at all?" Examples:
  * A third party can change a field of a signed message without invalidating the signature. Cause: the signed data does not include the field. Fix: include the field in the signed data. A consistency check against another field, an additional verification rule, or a documented risk only compensates for the gap.
  * Two representations of one value can disagree. Cause: one is not derived from the other when it is created. Fix: derive one from the other, or remove one. Use a consistency check only when this is not possible.
  * An invalid state can be persisted or relayed. Cause: the state transition which creates it accepts it. Fix: the transition, not every reader.
* Do not redefine the root cause to fit a convenient fix. The root cause is the earliest step at which the problem becomes possible. A check, rule, or document at a later step is a mitigation and must be labeled as a mitigation.

### Designs Introduced by the Change Are Not Constraints

* A format, message, field, schema, protocol rule, or API which the change under review introduces, or which unreleased commits of the current branch introduce, can be corrected without compatibility cost. Before using compatibility as an argument, verify whether the affected design is released or persisted in production.
* Do not limit fixes of an unreleased design to changes which keep its output identical. If the design is incomplete, complete it.
* When a review finds a gap in a new design, correct the design first. Then remove downstream checks, rules, test changes, and documentation which the corrected design makes unnecessary.

### Residual Risks

* A documented residual risk is not a fix. Before reporting a residual risk, state why its cause cannot be removed now, what would remove it, and who has to decide.
* Do not describe a removable weakness as an accepted limitation in a specification. A specification records only limitations whose cause cannot be removed at this time, together with the reason.

### Self-Check Before Reporting Fixes

Answer these questions for every fix before reporting it:

1. Which earlier step made the problem possible, and does the fix change that step?
2. Is the fix a check, rule, test change, or document which compensates for a gap that could be closed at its origin?
3. Were similar gaps (other fields, other message types, similar code paths) fixed in the same way? If the same kind of gap received different kinds of fixes, the analysis is incomplete.
4. Would each added check or rule still be needed if the cause were corrected? If not, correct the cause instead.
5. Can a design change remove any residual risk in the report?

If most fixes are mitigations, test adaptations, or documented risks, repeat the root-cause analysis before reporting.

---

For the detailed procedure (effort, risk, and alternatives; developer decisions; challenging the problem statement; severity; competing explanations; verification of the fix), use the `root-cause-analysis` skill.
