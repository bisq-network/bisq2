## Commits

* During implementation, make small, logically coherent commits so the change history is easy to understand and review.
* Prefer commits representing meaningful conceptual steps rather than arbitrary amounts of changed code.
* Each commit should leave the codebase in a reasonable state where practical.
* Keep pure refactoring separate from behavioral changes where practical.
* Commit messages must describe the implementation in enough detail for a reviewer to understand the change without inspecting the entire diff. For non-trivial changes, use a concise subject plus a body covering:
  * the problem or violated invariant;
  * the root cause;
  * the behavior or code paths changed;
  * relevant security, compatibility, persistence, or protocol implications;
  * tests or verification performed.
* Do not use vague one-line messages such as "Fix issue" or "Update validation" for substantive changes. The subject should summarize the outcome, while the body should explain the implementation and rationale.
* Do not add AI-agent authorship, attribution, `Co-authored-by` entries, or similar AI metadata to commit messages.
* Before committing, inspect the working tree.
* Include only changes belonging to the current task.
* Never accidentally commit unrelated pre-existing user changes.
* After completing and verifying a code task, commit the completed changes unless the developer explicitly says not to commit.
* Use only ASCII characters unless the task explicitly requires Unicode. Use ordinary spaces (U+0020), straight quotes, and regular hyphens. Do not introduce non-breaking spaces (U+00A0), zero-width characters, smart quotes, or typographic dashes.
