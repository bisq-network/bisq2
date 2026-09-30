---
name: review-feedback
description: "How to handle review feedback. Use it when the user message contains feedback prefixed with `reviewer claims:`."
---

## Review Feedback

When the user provides feedback prefixed with `reviewer claims:`:

* Treat each claim as a hypothesis to verify, not as inherently correct.
* Inspect the relevant code and determine whether each claim is valid.
* Review feedback may be:

  * correct,
  * partially correct,
  * incorrect,
  * obsolete,
  * already fixed,
  * or duplicated by another reviewer.
* Ignore duplicate or obsolete feedback once that has been established.
* If a claim is valid, implement an appropriate fix.
* Prefer fixing the underlying issue rather than mechanically applying the reviewer's proposed solution.
* Create a separate logical commit for each independent valid reviewer issue where practical.
* Reference the substance of the reviewer claim in the commit message so the relationship between feedback and fix is clear.
* If a claim is invalid or not justified, do not change the code merely to satisfy the reviewer. Explain in chat why the claim does not apply.
* Stop after all supplied reviewer claims have been evaluated and all justified fixes completed.
