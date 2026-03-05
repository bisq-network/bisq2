# Account Timestamp Attestation Specification

## Overview

Bisq requires a mechanism to attest the *age* of a payment account in order to apply trade limits and reduce fraud risk, especially for newly created or potentially stolen payment accounts.

Bisq 1 already supports an account age concept based on deterministic hashing of payment account identifiers and an append-only timestamp database. The Bisq 1 model trusts seed nodes not to backdate data added to the database.

Bisq 2 introduces an oracle-based attestation mechanism that supports two account flavors:

1. **Imported Bisq 1 accounts**, where account age is externally verifiable via the Bisq 1 DAO/account-age dataset. Trust is based on Bisq 1 seed nodes.
2. **New Bisq 2 accounts**, where no external history exists and trust in oracle issuance is required.

To avoid this trust requirement, a solution using OpenTimestamps was explored, but it was dropped for reasons discussed in:
[https://github.com/bisq-network/bisq2/discussions/4325](https://github.com/bisq-network/bisq2/discussions/4325)

This document specifies the protocol, security goals, threat model, limitations, and possible future improvements.

---

## Goals

The account timestamp mechanism aims to provide:

* **Account age signaling**
* **Protection against backdating**
* **Privacy preservation**
* **Determinism**
* **Minimal P2P network footprint**
* **Compatibility with the Bisq 1 account age witness**
* **Operational feasibility with a small oracle set**

---

## Non-Goals

This mechanism does **not** provide:

* Fully trustless timestamp issuance for Bisq 2–native accounts
* Public verifiability of payment account data without user disclosure
* Blockchain-level immutability (unless future anchoring is added)

---

## Entities

### User

Creates or imports a payment account and requests timestamp attestation via an HTTP request over Tor.

### Oracle Node

A bonded role responsible for:

* verifying requests
* looking up the account age witness from Bisq 1
* enforcing issuance rules
* publishing `AuthorizedAccountTimestamp` into the Bisq network
* re-publishing persisted attestations upon user request

### Bisq P2P Network

Carries oracle attestations with TTL-limited propagation.

### Peers (Traders)

Verify timestamp attestations during trade protocol execution.

---

## Data Model

### Payment Account Fingerprint

Each account payload exposes a deterministic fingerprint derived from identifying payment account data.

Examples of identifying data include:

* IBAN
* Account number
* Bank name
* Account holder name

The fingerprint is salted with the per-account `salt` from `AccountPayload` to prevent public correlation.

Two fingerprint variants are used:

* **Bisq 1 compatible fingerprint (V1)**: raw bytes from `AccountPayload.getBisq1CompatibleFingerprint()`.
* **Bisq 2 fingerprint hash (V2)**: 20-byte hash from `AccountPayload.getBisq2FingerprintHash()`.

---

## Hash Construction

### Inputs

`hash()` denotes `RIPEMD160(sha256(x))` (see `DigestUtil.hash`).

#### V1 (BISQ1_IMPORTED)

* `fingerprint`: `AccountPayload.getBisq1CompatibleFingerprint()`
* `salt`: `AccountPayload.getSalt()` (32 bytes)
* `publicKeyBytes`: public key bound to the account

### Computation

```
saltedFingerprint = concat(fingerprint, salt)

preimage = concat(saltedFingerprint, publicKeyBytes)

hash = hash(preimage)
```

This `hash` serves as the unique key for the payment account.

#### V2 (BISQ2_NEW)

* `fingerprintHash`: `AccountPayload.getBisq2FingerprintHash()` (20 bytes)
* `salt`: `AccountPayload.getSalt()` (32 bytes)
* `publicKeyBytes`: public key bound to the account

```
saltedFingerprintHash = hash(concat(fingerprintHash, salt))

preimage = concat(saltedFingerprintHash, publicKeyBytes)

hash = hash(preimage)
```

---

## AccountTimestamp Structure

An `AccountTimestamp` contains:

* `hash`
* `date`: user-defined account creation timestamp (milliseconds since epoch)

The user signs the serialized authorization payload (`AuthorizeAccountTimestampV1Payload` or
`AuthorizeAccountTimestampV2Payload`). The signature is carried in the corresponding request message.
The date is defined by the user and not the oracle to allow deterministic attestation by multiple oracle nodes.

---

## Protocol Modes

Bisq supports two timestamp origins:

### 1. Imported Bisq 1 account

The user imports from Bisq 1:

* payment account data
* account age timestamp
* private signing key

This mode is externally auditable. Trust is based on Bisq 1 seed nodes not having backdated data.

Request details:

* **Key type**: DSA
* **Payload**: raw `fingerprint` and `saltedFingerprint`

---

### 2. Newly created account in Bisq 2


The user creates a new account and requests timestamp issuance.

The oracle verifies:

* signature validity
* that the user-provided timestamp is close to the oracle’s current time

Acceptance rule:

```
abs(requestedDate - oracleNow) <= toleranceWindow
```

Default tolerance:

* ±2 hours

If the timestamp is in the future, it is treated as the processing node’s current time.

Request details:

* **Key type**: EC
* **Payload**: `fingerprintHash` and `saltedFingerprintHash` (20 bytes each)

---

## Request Message

The user sends a request to all available oracle nodes as an HTTP request over Tor:

```proto
message AuthorizeAccountTimestampV1Payload {
  uint64 date = 1;
  bytes hash = 2;
  bytes fingerprint = 3;
  bytes saltedFingerprint = 4;
  bytes publicKey = 5;
}

message AuthorizeAccountTimestampV1Request {
  AuthorizeAccountTimestampV1Payload payload = 1;
  bytes signature = 2;
}

message AuthorizeAccountTimestampV2Payload {
  uint64 date = 1;
  bytes hash = 2;
  bytes fingerprintHash = 3;
  bytes saltedFingerprintHash = 4;
  bytes publicKey = 5;
}

message AuthorizeAccountTimestampV2Request {
  AuthorizeAccountTimestampV2Payload payload = 1;
  bytes signature = 2;
}
```

---

## Oracle Request Verification

The oracle verifies:

* that the hash in the payload matches the hash derived from the request data:

For V1:

```
preimage = concat(
  payload.saltedFingerprint,
  payload.publicKey
)

hash = hash(preimage)
```

For V2:

```
preimage = concat(
  payload.saltedFingerprintHash,
  payload.publicKey
)

hash = hash(preimage)
```

* that the signature proves key ownership

If the oracle node has a persisted entry with that hash, it publishes an `AuthorizedAccountTimestamp` message using the persisted date.

If request is `AuthorizeAccountTimestampV1Request`:

* the oracle requests the date via gRPC from the Bisq 1 bridge node using the hash
* the returned date must equal the date in the request payload

If request is `AuthorizeAccountTimestampV2Request`:

* The oracle node publishes an `AuthorizedAccountTimestamp` with the date from the request.

---

## Oracle Attestation Publication

If validation succeeds, the oracle publishes:

* `AuthorizedAccountTimestamp` containing the `AccountTimestamp`
* `staticPublicKeysProvided` and `version` metadata

Authorization is expressed through the Bisq network’s `AuthorizedDistributedData` mechanism using bonded oracle public keys, rather than an explicit oracle signature inside the payload.

---

## Network TTL and Renewal

* `AuthorizeAccountTimestampV1Request` uses a TTL of 10 days.
* `AuthorizedAccountTimestamp` uses a TTL of 20 days. This must be longer than the TTL of the `UserProfile`, which is 15 days.

To ensure reliable availability of `AuthorizedAccountTimestamp`, the user requests re-publication from oracle nodes after half of its TTL has elapsed.

---

## Peer Verification in Trades

During the trade protocol, the peer verifies:

1. The user shares the `salt` from `AccountPayload`
2. The peer computes (V1/BISQ1_IMPORTED):

```
saltedFingerprint = concat(AccountPayload.fingerprint, AccountPayload.salt)
preimage = concat(saltedFingerprint, publicKeyBytes)
hash = hash(preimage)
```

For V2/BISQ2_NEW:

```
fingerprintHash = AccountPayload.getBisq2FingerprintHash()
saltedFingerprintHash = hash(concat(fingerprintHash, AccountPayload.salt))
preimage = concat(saltedFingerprintHash, publicKeyBytes)
hash = hash(preimage)
```

3. The peer checks:

* the hash matches the `AuthorizedAccountTimestamp` hash
* the date aligns with protocol rules (e.g., trade amount limits based on account age)

The authorized-data infrastructure verifies the oracle signature at the P2P network layer.

---

## Security Properties

### Privacy

* Payment data is not revealed publicly
* The hash cannot be reversed without payment data plus the secret

### Binding

The hash binds together:

* payment account identifiers
* per-account secret
* account keypair (one key for all imported Bisq 1 accounts, per-account key for new accounts)

### Replay Resistance

Replays do not help attackers:

* only the oldest timestamp is valuable
* newer timestamps provide no advantage

### Determinism

Deterministic hash construction enables:

* Bisq 1 auditing
* multi-oracle issuance at the same timestamps

---

## Threat Model

### Oracle Backdating (Core Risk)

An oracle could be bribed or compromised to issue:

* an artificially old timestamp for a new account

Mitigation:

* oracle is a bonded role
* multi-oracle redundancy
* future auditing mechanisms

This is not fully preventable without stronger anchoring.

---

### Long-Term Precomputation Attack

A malicious user may attempt:

1. Generate many `(hash, date)` attestations early
2. Later acquire stolen payment account data
3. Attempt to match old timestamps

Constraint:

* the hash depends on payment data
* the attacker would need to brute-force the secret

Mitigation:

* the secret must be high entropy (≥32 bytes)
* attacker-controlled variable-length secrets must not be allowed

Since a 32-byte secret is used, this attack is not feasible.

---

### Multiple Timestamp Spam

A user may request repeated attestations.

Mitigation:

* TTL-limited propagation
* the oracle ignores requests with a newer date if an older timestamp already exists for the same hash

---

## Oracle Storage Rules

The oracle maintains an append-only set of issued timestamps.

To support re-issuance after an `AuthorizedAccountTimestamp` has expired, the oracle stores:

```
commitmentKey = H(hash, date)
```

Re-publication rule:

* if commitment exists → return the stored date
* if not → reject

---

## Known Limitations

### Bisq 2–Created Accounts Are Not Publicly Auditable

Unlike Bisq 1 imports, no external dataset exists.

Trust is placed in oracle correctness.

### No Blockchain-Level Immutability

Oracle statements are not anchored unless future improvements are added.

### Small Oracle Set Limits Quorum Options

Currently, only a small number of oracle nodes exist, so threshold signing is not yet feasible.

### Bisq 1 Bank Account Fingerprint Drift

Bisq 1 bank account fingerprints are not fully stable across versions and locales. In Bisq 1:

* **Bank account type uses translated strings**, so fingerprints depend on the UI language at creation time.
* **Holder ID is prefixed with a localized description** and includes a newline, so translations affect the hash.
* **Required-field rules differ by country and have evolved**, which can change whether bank name, bank ID,
  branch ID, account type, holder ID, or national account ID are included.
* **Missing required branch IDs were encoded as the literal string `"null"`**, which can cause mismatches if
  older clients handled this differently.

Result: imported Bisq 1 bank accounts may fail to match their historical account-age witness if the
original Bisq 1 version or language does not align with current Bisq 2 fingerprint construction.


#### Mitigation:

We will show a warning popup if the user has imported an payment account of type `bankAccount` as only those seem 
to have those issues. The user is advised to create a new account and switch to that new account once account has aged enough.

---

## Future Improvements

### OpenTimestamps (OTS) Anchoring

Oracles can batch issued attestations into Merkle trees:

* daily or weekly batches
* publish Merkle roots externally

Possible anchors:

* Bitcoin transaction OP_RETURN
* public timestamping services
* multi-oracle cross-signing

OTS provides:

* immutable ordering guarantees
* strong prevention of retroactive backdating

### Multi-Oracle Quorum

Require `k-of-n` oracle signatures for Bisq 2–native accounts.

This reduces single-oracle corruption risk.

### Cross-Oracle Witnessing

Oracles periodically sign each other’s issuance roots.

This reduces isolated backdating.

### Improved Audit Tooling

Enable users and monitors to detect:

* abnormal issuance rates
* suspicious backdating patterns

---

## Conclusion

The account timestamp mechanism provides a practical, privacy-preserving approach to account age authorization in Bisq 2.

* Imported Bisq 1 accounts remain auditable and deterministic. Bisq 1 data is based on trust in seed nodes.
* New Bisq 2 accounts require oracle trust but can be strengthened with future anchoring (OTS) and quorum models.

This design balances:

* scalability
* usability
* minimal P2P network footprint
* strong fraud resistance
