## Security and Compatibility

* Always consider the security implications of code changes.
* Pay particular attention to:

  * trust boundaries,
  * externally supplied input,
  * validation,
  * serialization and deserialization,
  * networking,
  * persistence,
  * authentication,
  * authorization,
  * cryptographic operations,
  * signatures,
  * protocol messages,
  * concurrency,
  * resource exhaustion,
  * and error handling.
* Prefer secure-by-design solutions rather than isolated checks around an otherwise unsafe design.
* Consider how validation may be bypassed rather than assuming normal call paths are the only call paths.
* Be proactive in reporting security issues discovered while working on a task, including issues outside the immediate task scope.
* Do not silently make substantial unrelated security changes. Raise them with the developer and explain their impact.
* Consider backward compatibility whenever changing:

  * existing behavior,
  * public or internal APIs used across modules,
  * network protocols,
  * persisted data,
  * serialized formats,
  * database schemas,
  * configuration formats,
  * or other externally observable behavior.
* Explicitly raise compatibility or migration risks when a sound solution requires a breaking change.

### Baseline for Backward Compatibility

* The baseline is the last released version: the newest release tag, for example `v2.1.13`. A change must stay compatible with the network messages and the persisted data which that version writes and reads.
* Unreleased development work is not a baseline. A message, field, format, or rule which only commits after the last release introduced can be changed, renumbered, or removed without a compatibility measure, also when development builds have already written data with it.
* Before using compatibility as an argument, check whether the last release contains the affected definition: compare the file with its version at the release tag (`git show v2.1.13:<path>`), or check the commit with `git tag --contains <commit>`.
* Protobuf: a field number which the last release writes or reads must keep its type and its meaning. A removed field of the last release keeps its number and its name as `reserved`. A new field gets a number above every number which the last release uses, including the reserved numbers. A field which only unreleased commits added may get another number, type, or meaning. The rules and the reasons are in `docs/dev/backward-compatibility.md`.

### Completeness of Integrity Protection

When a change adds or reviews a signature, MAC, hash commitment, or another integrity protection:

* List every field of the protected message. For each field, record who sets it, whether a relay or another party can change it, and what depends on it: acceptance, storage key, store or route selection, expiry, ordering, authorization, and the interpretation of other fields.
* Cover every field which a third party must not change, directly or through a covered hash. This includes version and format markers, metadata which selects stores, limits, or expiry, and other signatures in the same message, such as a legacy signature kept during a migration. If one signature does not cover the other, a relay can create variants which some node versions accept and others reject.
* Derive the required coverage from the field list and the threat model. Do not use the coverage of an older protection version as the reference.
* Exclude a field only for a stated reason, for example forward compatibility for fields added in later versions, or reuse of the signature for another message. Record the reason and the consequence of the exclusion in the specification.
