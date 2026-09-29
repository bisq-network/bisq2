## Specifications

Specifications describe the intended domain behavior of the system. They are the authoritative written source for intended behavior and take precedence over behavior inferred solely from the current implementation or tests.

### Authority and Conflicts

Use the following order when determining intended behavior:

1. Explicit developer decisions
2. Domain specifications
3. Current implementation and tests as evidence of existing behavior

The current implementation and tests show what the system currently does or expects, but neither by itself is authoritative evidence of developer intent.

When specifications, implementation, and tests disagree:

* Do not resolve the discrepancy by majority.
* Do not assume the current implementation is correct merely because it exists.
* Do not assume a test represents intended behavior merely because it passes.
* Identify and clearly describe the conflicting behaviors.
* Determine whether the intended behavior can be established from explicit developer decisions or specifications.
* If intended behavior cannot be established confidently, raise the discrepancy with the developer.
* Suggest a resolution, including which specification, implementation, or tests should change and why.
* Do not silently change important domain behavior merely to make code conform to a specification or to make a test pass.

### Reading Specifications

Before making changes that affect domain behavior:

* Check `docs/` and `docs/specifications/` for documentation and specifications relevant to the affected domain.
* Check the affected source packages and modules for relevant `*.md` files.
* Read all relevant specifications before modifying domain behavior.
* Consider specifications from related domains when a change crosses domain boundaries.

Package-local Markdown files may contain useful implementation details, historical context, or older specifications. Treat `docs/specifications/` as the canonical location for authoritative domain specifications unless project-specific instructions state otherwise.

If existing documentation conflicts with `docs/specifications/`, implementation, or tests, surface the conflict rather than silently choosing one.

### Tests Are Not Specifications

Do not treat behavior encoded only in tests as authoritative evidence of developer intent.

Tests may be low quality, AI-generated, outdated, overly coupled to implementation details, or insufficiently reviewed.

Therefore:

* Treat tests as evidence of expected or existing behavior, not as specifications.
* Do not infer a domain requirement solely because a test asserts it.
* Be especially careful when preserving unusual or questionable behavior only because an existing test expects it.
* If a test enforces behavior that appears suspicious, inconsistent, unnecessarily restrictive, or contrary to specifications or surrounding domain logic, raise it with the developer.
* Do not change important production behavior merely to make an existing test pass without determining whether the tested behavior is actually intended.
* Once intended behavior is established, update incorrect or obsolete tests accordingly.

Tests should verify the specification rather than define it.

### Writing and Updating Specifications

When a change introduces or modifies important domain behavior, invariants, validation rules, protocol rules, security assumptions, state transitions, externally observable semantics, or other significant behavior, create or update the corresponding specification.

Specifications and code should evolve together. A code change affecting specified domain behavior should normally include the corresponding specification update in the same change set.

Use the `write-specification` skill for the location, structure, content, and scope of specifications.
