---
name: write-specification
description: "Rules for creating or updating a domain specification in docs/specifications/: location, hierarchy, content, package-local documentation, and scope. Use it whenever a change introduces or modifies important domain behavior, invariants, validation rules, protocol rules, security assumptions, state transitions, or externally observable semantics."
---

# Writing and Updating Specifications

This skill extends the section "Specifications" of the project agent instructions (`AGENTS.md`).

### Writing and Updating Specifications

Use `docs/specifications/` as the canonical location for domain specifications.

Organize specifications hierarchically by domain and subdomain. Where practical, use domain terminology that corresponds naturally to the codebase, while omitting unnecessary structural package prefixes such as `bisq`.

Example:

```text
docs/
└── specifications/
    ├── trade/
    │   ├── protocol-validation.md
    │   ├── dispute-opening.md
    │   └── account-age.md
    ├── dao/
    │   ├── block-validation.md
    │   ├── voting.md
    │   └── merit.md
    └── network/
        ├── message-authentication.md
        └── data-propagation.md
```

Prefer small, focused specification files covering one coherent aspect of a domain rather than large documents covering an entire subsystem.

A specification should primarily describe:

* Required behavior
* Domain rules and invariants
* Important validation rules
* Relevant state transitions
* Exceptional and boundary cases
* Security requirements where applicable
* Protocol requirements where applicable
* Backward-compatibility requirements where applicable
* The rationale behind important or non-obvious requirements

Keep specifications independent of implementation details where practical.

Specifications should primarily describe **what** the system must do and **why**, rather than how particular classes, methods, or packages implement it.

If an existing specification already covers the affected behavior, update it rather than creating overlapping or duplicate documentation.

### Package-Local Documentation

Use package-local `*.md` files primarily for implementation-oriented documentation.

As a general rule:

* `docs/specifications/` describes **what the system must do and why**.
* Package-local documentation describes **how a particular part of the implementation works**.

Authoritative domain specifications should not depend unnecessarily on the current package, class, or module structure. Domain concepts often outlive implementation structures and should remain stable when code is refactored.

Package-local documentation should reference the corresponding canonical specification when useful rather than duplicating it.

### Specification Scope and Structure

Keep specifications narrowly scoped and easy to discover.

Prefer several focused documents over one large specification.

Each specification should cover one coherent domain concept, rule set, protocol aspect, or behavior.

When a specification directory becomes large enough that discoverability suffers, add a small `README.md` that acts as an index and briefly describes the contained specifications and their relationships.

Avoid defining the same rule in multiple specification files. Define it in the most appropriate specification and reference it elsewhere when necessary.
