# Dependency verification

## Scope and policy

The root build's `check` must audit all resolvable project and buildscript configurations across the repository's
composite builds, including build logic, applications, desktop, network, and Tor. Gradle must verify the resolved
dependencies against the committed verification metadata and keyring.

Gradle verifies dependencies only against the verification metadata of the build which is the root of the build tree.
The builds in `apps`, `apps/desktop`, and `network` have no metadata of their own, so a build started in one of these
directories verifies no dependency. CI must therefore run their tasks from the repository root, for example
`:apps:build`. The `tor` build has the metadata of the Tor library and verifies its dependencies on its own.

Gradle must verify signatures only with the keys of the committed keyring. The metadata of this repository turns the
key servers off, so that a build never waits for a key server and a key which the keyring lacks fails the verification
instead of being downloaded. A run which downloads keys turns them on for that run only. The policy check fails when
the committed metadata does not turn them off, because Gradle drops the setting when it writes the metadata with the
key servers on.

Plugin resolution must not search MavenLocal. Local build-logic plugins come from the included build; published
dependencies come from the configured remote repositories. This prevents unsigned or incomplete local Maven entries
from shadowing signed publications, including Kotlin dependencies requested during IDE import.

Verification metadata must cover artifacts requested during IDE import, including sources and metadata for libraries
embedded in the pinned Gradle distribution. Compilation and dependency inventory tasks do not necessarily request
those artifacts.

Checksum fallbacks must have exact, reviewed allowlist entries with a rationale. Missing, duplicate, unsorted, or
stale entries must fail the policy check. Resolved executable artifacts must have explicit verification metadata;
metadata for older, unresolved versions of those tools must fail the policy check.

## Reusing dependency inventories

An inventory may be reused only after resolving and verifying the current dependencies. A previous successful audit
must not hide a changed dependency graph or a verification failure. Dependency selection through build properties,
resolution rules, and repository metadata must be reflected in the inventory used by the policy check.

Unchanged inventories may be reused across builds. Changes to resolved modules, direct dependency scopes, artifact
names or contents, verification metadata, or keyring must invalidate the corresponding inventory. Missing or modified
inventory outputs must be regenerated. Application source changes alone do not require inventory regeneration.

Explicit dependency refresh, metadata generation, and task rerun requests must regenerate inventories. Metadata
generation must cover every composite build. Inventory reuse must not suppress checksum or signature verification,
and a failed resolution or verification must fail the audit before a policy result is accepted.

The audit must run with strict dependency verification. Modes that skip or downgrade artifact verification (`off`,
`lenient`) must fail the audit, because a successful audit would otherwise not prove that artifacts were verified.

Each participating build owns its inventory. The policy check must read every expected inventory and fail if one is
missing; unrelated files from previous audits must not affect the result.

Dependency inventories are reusable build state, independent of compiled outputs. `clean` and `cleanAll` must preserve
them so cleanup cannot delete an inventory between its producer and the policy check. Retained inventories remain
subject to the resolution and verification requirements above.

See [the Gradle notes](../../../gradle/README.md) for audit and metadata maintenance commands.
