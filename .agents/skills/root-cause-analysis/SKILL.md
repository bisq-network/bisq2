---
name: root-cause-analysis
description: "Detailed root-cause procedure for non-trivial bugs, unexpected behavior, domain inconsistencies, security-sensitive changes, and problems with several edge cases. Use it before choosing between a root-cause fix and a mitigation, when presenting options to the developer, when a defect may be more severe than reported, when several explanations are possible, and when verifying a root-cause fix."
---

# Root Cause Analysis: Detailed Procedure

This skill extends the section "Root Cause Analysis and Problem Framing" of the project agent instructions (`AGENTS.md`).

### Effort, Risk, and Alternatives

When correcting the root cause requires considerable effort or introduces material risk, make the tradeoff explicit before proceeding with expanded work.

* Describe the concrete changes required and why they are necessary. Identify affected domains, responsibilities, dependencies, public APIs, protocols, persisted data, and operational behavior where relevant.
* Explain the sources of effort and uncertainty, including investigation, implementation, migration, testing, and rollout. Label estimates as estimates; do not substitute vague statements such as "too complex" or "too risky" for analysis.
* Compare feasible alternatives, including direct root-cause correction, a staged correction, and a narrower mitigation where applicable. Do not invent alternatives merely to fill a list.
* For each meaningful alternative, explain its pros and cons: which causes it eliminates, which risks remain, architectural effects, implementation and verification effort, compatibility implications, and reversibility. Consider the risk of leaving the defect in place as well as the risk of changing the code.
* Recommend one approach and explain why its benefits outweigh its costs and risks under the current constraints. Identify any unresolved decision that materially affects the recommendation.
* If a narrower mitigation is selected, label it accurately. Explain the remaining defect, why the mitigation is appropriate, and what follow-up would remove the cause. Do not describe symptom suppression as a complete root-cause fix.
* When useful, propose a staged path that reduces immediate risk while preserving a clear route to the sound design. Do not make the mitigation depend on the invalid behavior or introduce coupling that makes the eventual correction harder.

Neither the smallest diff nor the broadest redesign is automatically preferable. Choose the smallest sound solution with an explicit account of architectural integrity, residual risk, and practical feasibility.

### Alternatives and Developer Decisions

* Complete the root-cause analysis before asking the developer to choose. The options must include the correction at the origin of the problem, even if it is larger or changes an unreleased format. If all options only change downstream handling, the set of options is incomplete.
* For each option, state whether it removes the cause, mitigates consequences, or only documents a risk.
* Treat a developer decision as a set of required properties (for example "one signature verification per request" or "the legacy signature may be empty"), not as a restriction to the options offered. If a design found later meets those properties and removes the risk, present it.

### Challenge the Initial Problem Statement

Treat the reported issue, failing test, reviewer comment, exception, or requested change as evidence of a problem, not necessarily as an accurate description of its cause.

Before implementing the obvious requested fix:

* Check whether the reported issue is a consequence of a more fundamental defect.
* Check whether multiple reported edge cases share one cause.
* Check whether existing behavior that appears intentional is actually compensating for a historical bug.
* Check whether fixing the immediate problem would conceal a more severe domain, security, accounting, consensus, or state-integrity issue.
* Do not assume the requested scope corresponds to the true scope of the defect.

If investigation reveals a more fundamental issue, prioritize understanding and correcting that issue before adding handling for its consequences.

### Severity and Fundamental Defects

During investigation, actively look for defects with larger impact than the originally reported problem.

Treat potential violations of core domain invariants, security properties, consensus rules, accounting correctness, ownership, authorization, state integrity, or irreversible state transitions as potentially severe even when the immediate observed symptom appears minor.

When such a defect is found:

* Stop treating the original symptom as the primary problem.
* Determine the scope and consequences of the fundamental defect.
* Identify all behavior and edge-case handling derived from it.
* Raise the issue clearly to the developer before proceeding with a narrow patch if the required correction materially changes the task.
* Recommend fixing the fundamental defect first when downstream fixes would otherwise encode or preserve incorrect behavior.
* After fixing the fundamental defect, re-evaluate whether the original requested change is still necessary.

Do not judge severity primarily by:

* how often the bug is triggered,
* how large the failing diff is,
* whether tests already cover it,
* or whether the current task explicitly mentions it.

Judge severity by the violated invariant and the possible consequences.

### Evaluate Competing Explanations

Do not settle prematurely on the first plausible explanation.

For non-trivial problems:

* Form at least one alternative hypothesis for the cause when practical.
* Look for evidence that would disprove the leading hypothesis.
* Prefer explanations that account for all observed behavior with fewer special cases.
* If the proposed fix handles one example but does not explain why the invalid state exists, continue investigating.
* If new branches or exceptions keep accumulating during implementation, treat that as a signal that the abstraction or root-cause analysis may be wrong.

Increasing conditional complexity during a bug fix should trigger reconsideration of the underlying model.

### Verify the Root-Cause Fix

Verification must demonstrate more than that the originally observed symptom is gone.

Where practical:

* Add or update tests around the restored invariant rather than only around the specific reported example.
* Verify that alternative paths cannot recreate the invalid state.
* Check whether downstream workarounds can now be removed.
* Search for similar assumptions or duplicated handling elsewhere.
* Re-run tests for related domain behavior, not only the originally failing test.
* Confirm that the fix does not merely move the failure to another layer.

For important domain or security issues, summarize the causal chain in the discussion or commit context:

**root cause → invalid state → downstream consequences → observed symptom → fix**

The final implementation should make this chain shorter by eliminating the earliest practical cause, not longer by adding another downstream workaround.
