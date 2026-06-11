# AGENTS.md — Bisq 2

## Initialization
Read ~/.codex/AGENTS.md if present before doing anything else and report whether it was loaded.


## Scope

This file contains agent-specific operating rules.
Project coding conventions and architecture rules are defined in the developer docs.

## Source of Truth (Developer Docs)

Start with:

- [Developer Guide](docs/dev/dev-guide.md)

Primary referenced docs:

- [Code Guidelines](docs/dev/code-guidelines.md)
- [Nullability and Optional](docs/dev/nullability-and-optional.md)
- [Architecture](docs/dev/architecture.md)
- [MVC Pattern](docs/dev/mvc-pattern.md)
- [Observable Framework](docs/dev/observable-framework.md)
- [Testing](docs/dev/testing.md)
- [Protobuf Notes](docs/dev/protobuf-notes.md)
- [P2P Network](docs/dev/network.md)
- [Build](docs/dev/build.md)
- [Contributing](docs/dev/contributing.md)


## Tech and Runtime Context

- Java (JDK 21)
- Gradle (Kotlin DSL)
- JavaFX desktop app

---

## Agent Behavior

- Do not guess missing requirements
- Do not introduce new dependencies without strong justification
- Prefer existing utilities and established patterns over new implementations
- Keep changes minimal and consistent with surrounding code

### Correctness Over Compliance
- Do not optimize for agreement with the developer
- If a request conflicts with architecture, conventions, or correctness, **point it out explicitly**
- Do not silently work around problems or inconsistencies

### No Hidden Assumptions
- Do not infer unstated requirements
- Do not change behavior beyond the requested scope
- If multiple interpretations are possible, **stop and ask for clarification**

### Consistency Enforcement
- Match the style, structure, and patterns of the surrounding code
- Do not introduce new abstractions, patterns, or naming schemes without clear need
- Avoid unrelated refactoring or “improvements”

### Quality Gate (Self-Check Before Completion)
Ensure that:
- The change is minimal and scoped to the request
- The solution follows existing project conventions
- No unnecessary complexity or abstraction was introduced
- No hidden assumptions were made
- The result would pass a strict code review

### Failure Handling
- If the task cannot be completed safely or clearly, **do not proceed with a speculative or partial solution**
- Clearly state what is missing and what is required

### Rule of Last Resort
- If the requested change would not pass a strict senior code review, **do not implement it—explain why instead**

When uncertain: **stop and ask**

---

## Authority

Human contributors have final authority.
Agents are assistants, not decision-makers.

---

## License

Bisq 2 is licensed under AGPL-3.0.
