## Architecture

Treat architectural integrity as part of correctness, including during bug fixes and security work. A change that fixes an example while violating domain ownership or dependency rules is not automatically a sound solution.

### Domain Hierarchy and Dependency Direction

* Follow clean architecture principles and maintain separation of concerns.
* Respect dependency direction between layers.
* Before a non-trivial change, identify the affected domains, their hierarchy, allowed dependencies, and the component that owns each relevant invariant and state transition. Use project architecture and specifications rather than inferring intent solely from existing imports or service access.
* Unless project-specific architecture defines otherwise:

  * domain code may depend only on domain-level concepts,
  * data/infrastructure code may depend on domain and data/infrastructure concepts,
  * presentation code may depend on domain and presentation concepts.
* Avoid leaking infrastructure, persistence, UI, transport, or framework concerns into the domain layer.
* Prefer dependencies pointing toward the domain rather than outward from it.
* Respect boundaries between domains as well as between technical layers. Two components being domain-level does not automatically permit either one to depend on the other.
* A domain's authoritative decisions and data must not depend on a downstream representation or copy maintained for another responsibility. Make the source of truth and the direction of derived data explicit.
* When existing architecture violates these principles, do not blindly reproduce the violation. Determine whether following the existing pattern or correcting the abstraction is more appropriate for the task.

### Responsibilities and Ownership

* Place behavior in the domain or component that owns the rule, data, or lifecycle. Do not add responsibility to a class merely because it is a convenient caller or already has access to the required services.
* Keep the authoritative domain rule in its owning domain. Input validation at an adapter or trust boundary may enforce that rule, but must not become a competing definition of it.
* Put coordination between otherwise independent domains in the appropriate application or integration layer when neither domain should own the other. Follow existing sound project patterns before introducing a new coordinator or abstraction.
* Keep domain decisions separate from presentation updates, transport delivery, and persistence mechanics. Make the boundary and ownership explicit where these concerns must interact.
* Avoid duplicate sources of truth and split ownership of mutable state. When copies or projections are necessary, define how they are derived, when they are valid, and what happens if they disagree with authoritative data.
* Do not make a domain invariant depend accidentally on optional callbacks, optional auxiliary data, UI availability, message arrival order, or another component's initialization side effects. Represent necessary prerequisites and transitions explicitly.

### Indirection and Runtime Coupling

* Review logical and runtime dependencies, not only imports and build-module dependencies.
* Callbacks, generic functions, suppliers, service locators, event listeners, and interfaces can hide reverse dependencies or cycles. Introducing an indirection does not by itself preserve an architectural boundary.
* Use interfaces and events when they express a legitimate boundary with clear ownership and semantics. Do not use them solely to bypass a dependency restriction or leave an operation in the wrong domain.
* When adding a dependency, make its purpose, ownership, required data, and lifecycle clear. Pay special attention to mutable callback registration, hidden startup ordering, reentrant calls, and failure behavior when a collaborator is unavailable.
* Prefer explicit, cohesive coordination over a chain of callbacks that distributes one operation or security invariant across unrelated services. Avoid adding a generic framework when a direct operation at the correct layer is sufficient.

### Assess the Architecture of the Fix

Before implementing a non-trivial fix, assess whether the proposed approach:

* places validation and state changes at the correct domain or trust boundary;
* preserves domain hierarchy, dependency direction, and responsibility ownership;
* introduces circular dependencies, duplicate authority, or new lifecycle and ordering assumptions;
* enforces the invariant across relevant creation, update, restoration, replay, and concurrent paths;
* removes the underlying architectural cause instead of compensating for it elsewhere;
* adds complexity justified by the actual requirements.

If the proposed fix introduces an architectural compromise, point it out explicitly and describe a sound alternative. If the sound alternative requires considerable effort or risk, follow **Effort, Risk, and Alternatives** in the `root-cause-analysis` skill rather than silently choosing either a local workaround or a broad redesign. Explain material architectural effects and any remaining compromise in the final response.
