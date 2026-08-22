# Signed-witness reputation ownership and scoring

## Scope

This specification defines how Bisq 2 requests, authorizes and scores reputation derived from a
Bisq 1 signed account-age witness. It extends the common ownership, transport binding and shared
one-witness/one-profile rules in [account-age.md](account-age.md).

## Authorization protocol

Only signed-witness authorization protocol version `2` may create reputation. It contains the
target profile id, witness hash, salted account-input bytes, owner DSA public key and a signature over
the domain-separated Bisq 1 ownership proof. Requester-supplied account-age and sign dates are not
part of version `2` and are not authoritative.

The Bisq 1 bridge must first verify the account-age hash preimage and owner signature. It must then
find at least one signed-witness leaf whose `witnessOwnerPubKey` equals the independently proven owner
key and which individually satisfies the complete Bisq 1 trust-chain rules. This includes the
deployed witness signature, signer-age, trusted-root, cycle, depth and ban checks. The bridge returns
the earliest stored date among qualifying leaves only. A signature-valid leaf from an incomplete or
untrusted chain must not supply a reputation date.

The deployed Bisq 1 `SignedWitness` signature authenticates the account-age witness hash but does not
commit to the signed-witness date. The date is authoritative for this import only within the trust
model of the current Bisq 1 implementation. The peer broadcast and trade admission paths validate the
hostile date at ingress. The initial synchronization path does not validate the date: it accepts
witness data only from seed nodes and from bundled resources which are audited as part of the release
process, so the stored date additionally rests on that seed-node and release-audit trust. Only the
first admitted record for a witness enters the append-only store, and later consumers, including the
bridge, use that stored date. Cryptographically authenticating the date would require a separate,
versioned Bisq 1 protocol change and is not a requirement of signed-witness authorization protocol
version `2`.

The request is confidential. The oracle must bind the target profile to the authenticated sender,
reject legacy and unsupported versions, obtain the nullifier and date bucket from the bridge,
persist the complete request before publication, and fail closed on every proof, bridge, timeout or
persistence error.

## Authorized data and conflicts

Authorized signed-witness data version `2` contains the profile id, shared witness nullifier and
fixed one-day UTC bucket containing the authoritative sign date. Nullifier construction, bucketing
and conservative scoring are defined by the account-age specification. The data version, nullifier
and date bucket are included in the oracle-authorized data hash, so a relay cannot rewrite a current
record as an unsupported future version or alter its identity or age. Version `1` lacks a verifiable
witness identity, remains parseable for network compatibility and contributes zero score.

Current signed-witness claims participate in the same consumer conflict index as current account-age
claims. The same witness may contribute account-age and signed-witness score to one profile. If any
current claim binds it to another profile, all claims for that witness contribute zero, independent of
arrival order or source.

For a profile with multiple non-conflicting signed witnesses, only the oldest conservative date
bucket contributes. The existing minimum age, maximum age and weight rules remain unchanged, but
the scoring date can be delayed by less than one day. The resulting whole-day age can be at most one
day lower and never increases.

## Privacy and rollout

The proof discloses the salted account-input bytes and owner public key to the controlled oracle and
local Bisq 1 bridge, and the requester retains it for renewal. The raw Bisq 1 witness hash and exact
qualifying sign date are not published; doing so would provide direct public-dataset join keys.
Activation follows the coordinated version-2 rollout in the account-age specification and requires
a Bisq 1 bridge which enforces the ownership proof, nullifier derivation, date bucketing and
qualifying-chain checks, backed by the Bisq 1 witness-admission date rules. Old clients cannot
validate version-2 authorized data because the newly signed nullifier and bucket change the hash
payload.
