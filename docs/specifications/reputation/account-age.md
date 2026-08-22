# Account-age reputation ownership and uniqueness

## Scope

This specification defines how Bisq 2 requests, authorizes and scores reputation derived from a
Bisq 1 account-age witness. The Bisq 1 specification is authoritative for witness construction,
ownership-proof encoding and bridge verification.

The additional trust-chain rules for signed-witness reputation are specified in
[signed-witness.md](signed-witness.md).

## Authorization protocol

Only account-age authorization protocol version `2` may create reputation. A version-2 request
contains:

- the target Bisq 2 profile id;
- the 20-byte Bisq 1 account-age witness hash;
- the exact salted account-input bytes used by Bisq 1;
- the encoded Bisq 1 owner DSA public key; and
- the owner's domain-separated signature over the complete proof.

Network-message validation applies structural and resource bounds only. The Bisq 1 bridge performs
the ownership decision: it recomputes the witness hash from the salted account input and owner key,
verifies the signature, resolves the witness, derives the nullifier and returns only the fixed
one-day UTC bucket containing the authoritative date. The oracle must not accept a requester-supplied
date, nullifier or bucket.

The request is an authenticated confidential message. Before calling the bridge, the oracle must
require:

```text
profileId == hex(HASH160(confidentialSenderPublicKey))
```

This transport binding proves control of the target Bisq 2 profile. It is independent of, and
required in addition to, the Bisq 1 witness ownership proof.

Any unsupported or legacy request is rejected before publication. A bridge error, missing witness,
invalid proof, sender mismatch, request timeout or persistence failure must fail closed for new
issuance. Interactive bridge calls have a bounded deadline so an unavailable local bridge cannot
consume every oracle request worker indefinitely.

## Authorized data

Account-age authorized-data version `2` contains the profile id, an unlinkable witness nullifier and
the start of a fixed one-day UTC bucket containing the authoritative witness date. The data version,
nullifier and date bucket are security-critical and are included in the oracle-authorized data hash.
Consequently, a relay cannot rewrite a current record as an unsupported future version or alter its
identity or age bucket without invalidating the oracle signature.

The authorized-data version is also part of internal record equality. Deduplication, pending-data
tracking and removal must not treat records with different semantic versions as the same record,
even when their profile id, bucket and nullifier are byte-identical.

The bridge derives the 32-byte nullifier from the exact verified witness preimage using canonical
four-byte big-endian length framing:

```text
SHA-256(
    LP("BISQ2_WITNESS_REPUTATION_NULLIFIER_V1") ||
    LP(accountInputDataWithSalt || ownerPublicKey)
)
```

Framing the complete historical witness preimage preserves the equivalence relation used by the
Bisq 1 witness hash: byte-identical historical witnesses always produce the same nullifier. The
account-age and signed-witness protocols use the same nullifier domain. Profile id, reputation
source and oracle identity are not inputs, so every honest oracle produces the same nullifier for
the same witness and consumers can detect cross-profile reuse.

The date bucket is:

```text
floor(authoritativeDate / 24 hours) * 24 hours
```

Consumers calculate age from the latest millisecond in the bucket
(`dateBucket + 1 day - 1 millisecond`). The scoring date is less than one day later than the exact
date, so the resulting whole-day age never exceeds the exact age and can be at most one day lower.
This conservative loss is the privacy cost of withholding the exact Bisq 1 timestamp.

Version `1` lacks the witness identity and was issued from a self-referential signature which did not
prove witness ownership. It remains parseable so old network data cannot crash an upgraded client,
but it contributes zero reputation and is never republished by an upgraded requester. Existing
legitimate users must generate and submit a new version-2 proof.

## One witness, one profile

One Bisq 1 account-age witness is one reputation asset. It may contribute to at most one Bisq 2
profile at a time.

Oracles persist accepted version-2 requests and reject a request when the same private witness hash is
already bound to another profile. This prevents ordinary duplicate issuance. Consumers enforce the
same invariant independently because oracle stores are private, more than one oracle may publish,
and network delivery is unordered.

For all currently authorized version-2 account-age and signed-witness data, a consumer builds:

```text
witnessNullifier -> set(profileId)
```

- Repeated authorizations of the same witness for the same profile are one logical claim.
- If the set contains exactly one profile, that profile may receive the applicable account-age and
  signed-witness scores.
- If the set contains more than one profile, every claim for that witness contributes zero until the
  conflict is removed or expires.
- Conflict handling must be independent of message arrival order. First-seen and lexicographic-winner
  rules are forbidden because they enable front-running or profile-id grinding.
- Removal or expiry of authorized witness data must also remove its pending and cached score state,
  even when the associated user profile is temporarily absent. Republishing the shorter-lived user
  profile must not resurrect reputation from authorization data which is no longer active.

A profile with several different non-conflicting witnesses receives only the score of its oldest
account-age witness, preserving the pre-existing source semantics.

An oracle's persisted binding does not expire with the authorized network data. Transferring a
witness to another profile therefore requires a separately specified, authenticated revocation
protocol; network-data expiry alone is not a transfer. Until such a protocol exists, an upgraded
oracle permanently rejects a second profile for a witness it has already authorized. Consumers
still fail closed on conflicts published by different oracles, and there is no implicit
last-writer-wins transfer.

## Shared account witness identity

Account-age and signed-witness reputation are two claims on the same Bisq 1 account-age witness.
The one-witness/one-profile index therefore includes current authorized data from both sources. The
same witness may provide both sources to one profile, but a claim by another profile makes every
account-age and signed-witness claim for that witness contribute zero.

Signed-witness protocol version `2` reuses the account-age hash-preimage proof with the separate
domain `BISQ2_SIGNED_WITNESS_REPUTATION_V2` and is governed by the signed-witness specification.

Signed-witness authorized-data version `2` includes the shared nullifier and its own sign-date bucket
in the oracle-authorized hash. Version `1` remains parseable but contributes zero score.

## Privacy and rollout

The ownership proof reveals salted account-input data to the controlled oracle and its Bisq 1
bridge. It is also retained in the requester's local reputation-request store so the authorization
can be renewed. This is an explicit privacy trade-off required by the current hash construction; a
zero-knowledge design would be a separate protocol.

The raw Bisq 1 witness hash and millisecond-precise witness date must remain confined to the
confidential request, private oracle state and local bridge processing. Publishing either value
would link the Bisq 2 profile to public Bisq 1 witness data. Hashing the public witness hash, even
with a public domain or salt, does not prevent enumeration. Publishing the SHA-256 intermediate of
the Bisq 1 HASH160 construction is also forbidden because applying RIPEMD-160 would recreate the
public witness hash.

The one-day bucket removes exact timestamp equality but provides limited resistance to correlation,
especially when a profile publishes both account-age and signed-witness dates. The nullifier
prevents passive observers who know only public Bisq 1 data from performing the direct
join. A former counterparty who retained the complete salted payment-account payload and owner key
can derive it and remains able to correlate the account. Preventing that stronger linkage requires
an anonymous-credential, OPRF or zero-knowledge protocol outside version `2`.

Deployment order is:

1. deploy the Bisq 1 bridge with ownership verification;
2. deploy upgraded oracles, which reject legacy account-age and signed-witness requests;
3. enforce the Bisq 2 client update which ignores legacy authorized data and understands version `2`;
4. have users regenerate account-age proofs in Bisq 1 for their selected Bisq 2 profile; and
5. enable/use version-2 account-age and signed-witness issuance.

If step 5 occurs before step 1, the new bridge RPC is unavailable and issuance fails closed. If old
Bisq 2 clients remain active, they do not understand the nullifier and date-bucket fields included
in the version-2 authorized-data hash and cannot validate the new data; the enforced client update
is therefore part of the activation boundary. The ownership-response tag formerly used for the
exact date is reserved, while the bucket and nullifier use new tags. Thus an old bridge omits fields
required by a new oracle, and an old oracle sees its exact-date field omitted by a new bridge; both
mismatch directions fail closed.
