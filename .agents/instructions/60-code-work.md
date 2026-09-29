## Code Work

### General Approach

* Before making code changes, check for relevant specifications and documentation and follow the rules in the **Specifications** section.
* Inspect the relevant code and its surrounding context before changing it.
* Understand the existing design, domain model, data flow, abstractions, call sites, and project patterns rather than modifying code based only on the immediate symptom.
* Prefer a conceptually and architecturally sound solution over a minimal patch.
* Do not work around an underlying design problem merely to minimize the diff or make a failing test pass.
* Fix problems at the appropriate abstraction level and, where practical, address the root cause rather than its symptoms.
* A small diff is not inherently better than a larger change when the larger change produces a substantially cleaner, safer, or more maintainable solution.
* The goal is the **smallest sound solution**, not the smallest possible diff.
* Do not sacrifice correctness, architecture, security, or maintainability solely to remain within the apparent initial scope.
* At the same time, avoid unnecessary complexity, speculative abstractions, unrelated refactoring, and scope expansion that does not contribute materially to solving the problem.
* If the sound solution materially exceeds the initial scope, raise this with the developer. Explain:

  * why the additional scope is necessary,
  * what additional changes are required,
  * what architectural or domain issue is being addressed,
  * and what risks or compromises a narrower patch would introduce.
* Do not silently expand a task when the expansion substantially changes architecture, domain behavior, public APIs, protocols, persistence, compatibility, or other important design decisions.

### Code Quality and Scope

* Follow existing project patterns and conventions unless there is a concrete reason to improve or deviate from them.
* Keep changes cohesive and centered on the problem being solved.
* Avoid unrelated refactoring, formatting churn, dependency changes, renaming, or cleanup.
* Related refactoring is appropriate when necessary to implement the change cleanly, fix the problem at the correct abstraction level, or avoid introducing architectural debt.
* Do not preserve a poor abstraction merely because improving it increases the size of the diff.
* Avoid duplication, ad-hoc special cases, and local workarounds when a clear general solution exists.
* Prefer explicit domain concepts and meaningful abstractions over clever or unnecessarily generic solutions.
* Prefer simple solutions when they are equally sound.
* Do not introduce abstractions merely because they might be useful in the future.

### Pure Refactoring

When performing purely technical refactoring that does not intentionally change behavior:

* Isolate the refactoring in separate commits where practical.
* Prefix the commit message with `Refactoring: `.
* Examples include:

  * renaming variables, fields, methods, or classes,
  * moving fields or methods,
  * extracting methods,
  * reorganizing code without behavioral changes.
* Do not mix significant behavioral changes into a commit presented as pure refactoring.

### Documentation and Comments

* Prefer expressive naming, clear types, and understandable structure over explanatory comments.
* Add comments or code documentation primarily when the code itself cannot adequately explain **why** something is done.
* Document important invariants, assumptions, constraints, compatibility requirements, security reasoning, and non-obvious design decisions where losing that context could lead to future mistakes.
* Avoid comments that merely restate what the code does.
* Important domain behavior belongs in specifications rather than only in code comments.

### Java

* Use modern Java syntax and language features appropriate for the Java version used by the project.
* Prefer modern constructs such as pattern matching for `instanceof` where they improve clarity.
* Use Lombok annotations such as `@Getter`, `@Setter`, `@Slf4j`, `@EqualsAndHashCode`, and `@ToString` when appropriate and consistent with surrounding code.
* Avoid introducing other Lombok annotations unless project-specific conventions explicitly support them.
* Use `@Nullable`, `Optional`, or other null-handling patterns according to the conventions and guidelines of the affected project.
* Do not introduce a different null-handling style without a good reason.
* Prefer existing project conventions over introducing new stylistic patterns.
