# Ownership of stored data

This specification defines which key may publish data in the network storage. It covers the stores of authenticated
data and mailbox data. Append-only data has no owner.

## Terms

- **Owner**: the key which may publish the data. In the authenticated data store, only the owner can also refresh and
  remove it. The data names its owner, or, for authorized data, the authorized key is the owner.
- **Publisher**: the key which signs an add request. The add request contains the hash of this key
  (`pubKeyHash` of `AuthenticatedSequentialData`, `senderPublicKeyHash` of `MailboxSequentialData`). The storage checks
  that the signing key matches this hash.
- **Owner hash**: the hash of the encoded public key of the owner, `RIPEMD160(SHA256(publicKey.getEncoded()))`, as
  `PubKey.getHash()` computes it.

## Problem

Storage key V2 contains the public key of the publisher (see [`storage-keys.md`](storage-keys.md)). Entries of different
publishers therefore do not replace each other. This does not decide who may publish the data.

Without an owner rule, Mallory can copy the data of Alice (for example an offer, the ban of a moderator, or a bonded
role which an oracle authorized) into an add request which Mallory signs. The store accepts it as a separate entry.
Mallory then removes her entry. The store reports the removal of the data, and the application removes the data by
equality. The entry of Alice stays in the store, but the application no longer shows the data of Alice. Mallory can
also publish a copy of data which its owner removed, and the application shows it again.

The storage knows the publisher; only the data knows its owner. So the data must name its owner, and the storage must
compare both before it accepts the data.

## Rules

### The data names its owner

1. A `DistributedData` type whose data names its owner in its own fields implements `OwnershipVerifiableData`. Its
   method `getOwnerPubKeyHash()` returns the owner hash. Types whose data names no owner do not implement it.
   The methods of `NetworkService` which publish, refresh and remove default authenticated data accept only
   `OwnershipVerifiableData`, so the compiler rejects a type which cannot have an owner there.

   | Data | Owner |
   | --- | --- |
   | `UserProfile` | the key of its network ID |
   | `PublicChatMessage` (`BisqEasyOfferbookMessage`, `CommonPublicChatMessage`) | the author: `authorUserProfileId` |
   | `CommonPublicChatMessageReaction`, `BisqEasyOfferbookMessageReaction` | the reacting user: `userProfileId` |
   | `MuSigOfferMessage` | the maker: the key of the maker's network ID in the offer |
   | `ConfidentialMessage` (in mailbox data) | the sender: `senderPublicKey` of the confidential data |
   | `AuthorizedOracleNode` | the key of its network ID |
   | other authorized data | none: the authorized key is the owner (rule 4) |
   | `AccountTimestamp` | none: it is published only inside `AuthorizedAccountTimestamp` |
   | append-only data | none: it is not signed and cannot be removed |

2. A profile ID names the owner hash only if it is the lower-case hex encoding of the hash, as `PubKey.getId()` creates
   it: 40 characters for the 20 bytes of the hash. Data with another profile ID is invalid (`isDataInvalid()`).

   Reason: one hash must have only one ID. Earlier versions compared the ID with the hex encoding of the publisher's
   hash as strings, so they also accept only this encoding.

### The owner of authenticated data

3. The owner of default authenticated data is the owner which the data names. The store rejects default authenticated
   data whose data names no owner.

   Reason: authorized data names no owner. Wrapped as default authenticated data, it would otherwise skip the check of
   its authorization.

4. The owner of authorized data is the owner which the data names. If the data names no owner, its authorized key
   (`authorizedPublicKeyBytes`) is the owner.

   Reason: the authorization signature covers the data, but not the publisher. Anyone can copy authorized data with a
   valid authorization signature into their own add request. Only the authority can decide who publishes and removes
   its data.

### Admission

5. The authenticated data store accepts an add request only if the publisher is the owner of the data. Otherwise the
   result is `ownerInvalid`. The only exception is the transition rule below. The store checks the content of the data
   (`isDataInvalid()`, result `dataInvalid`) before the owner, because the owner can only be read from valid data, for
   example from a valid profile ID.
6. The mailbox store accepts an add request only if the sender is the owner of the confidential message. Otherwise the
   result is `ownerInvalid`. The receiver removes mailbox data; storage key V2 of mailbox data contains the key of the
   receiver (see [`storage-keys.md`](storage-keys.md)). The receiver hash in the sequential data must equal the hash of
   its receiver public key bytes. Reject inconsistent sequential data when it is constructed or decoded, including
   persisted entries. Otherwise the key which finds the entry cannot pass its removal authorization check, and the
   key which passes that check looks up another entry. No key could remove the message. Released nodes always derive
   the receiver hash from the receiver public key, so valid existing entries already satisfy this rule.
7. Remove requests need no own owner check. Their storage key contains the key which signs them, so they find only the
   entry which the same key added (see [`storage-keys.md`](storage-keys.md)). The same holds for refresh requests, but
   a refresh replaces the creation date of the entry, on which the transition rule depends. So the store checks the
   owner of the refreshed entry again.
8. When a node loads an authenticated data store, it removes the add requests whose publisher is not the owner of the
   data.

   Reason: earlier versions did not check the owner. An entry which an attacker added before the update could
   otherwise be removed after the update, with the effect described in [Problem](#problem).

### Publishers of authorized data

9. Oracle and seed nodes publish and remove authorized data with their authorized key pair: the key pair whose public
   key is `authorizedPublicKeyBytes`. `AuthorizedOracleNode` is the exception: it names the key of the network ID of the
   oracle node as its owner, so the oracle node publishes it with that key pair.
10. Security managers, moderators and release managers use the key pair of their user identity both as authorized key
    pair and as publisher. They need no change.

### Use of stored data

11. Applications get only data whose publisher is its owner. The storage notifies them only of such entries, and its
    queries of stored data return only such entries. The stores still hold and relay all entries which they accept
    (rule 12).

    Reason: during the transition, the stores of authorized data also hold entries of other publishers: the entries
    which oracle and seed nodes of earlier versions published with the key pair of their network identity, and copies
    which anyone can publish (see [Known limitations](#known-limitations)). The store holds one entry per data and
    publisher (storage key V2), but applications identify data without its publisher. If applications used these
    entries, the removal or expiry of one of them would remove the data of the owner from the applications. A copy
    would also make data available again which the owner removed.

## Transition

Oracle and seed nodes of earlier versions published data of the following types with the key pair of their network
identity, not with their authorized key pair:

| Type | Time to live |
| --- | --- |
| `AuthorizedBondedRole` | 100 days |
| `AuthorizedBurningmanListByBlock` | 100 days |
| `AuthorizedAccountAgeData`, `AuthorizedSignedWitnessData`, `AuthorizedTimestampData` | 30 days |
| `AuthorizedProofOfBurnData`, `AuthorizedBondedReputationData` | 30 days |
| `AuthorizedAccountTimestamp` | 20 days |
| `AuthorizedMarketPriceData` | 10 minutes |

12. For these types (`isLegacyOwnerAccepted()`), the store accepts authorized data with another owner if the add
    request was created before the cutoff date `AuthorizedData.LEGACY_OWNER_CUTOFF_DATE` (2027-01-01 UTC).
13. The cutoff date must be later than the date at which all oracle and seed nodes publish with their authorized key
    pair. Updated nodes reject data which an oracle node of an earlier version creates after the cutoff date.
14. At startup, oracle and seed nodes publish the data which they published with the key pair of their network identity
    again with their authorized key pair, and they remove the entries of their network identity. The remove request
    gets the next sequence number after the entry, the new entry a higher one. They do not publish data again whose
    owner has a remove request in the store, and they ignore expired entries. They remove an entry of their network
    identity only if their store accepted the new entry, or if the owner removed the data. If the store rejects the
    new entry, for example because it is full, the entry stays for a later run of the migration.
    - The node broadcasts the requests of the migration one at a time, with a pause of 500 ms, and only while it has a
      connection. For each store, it sends the new entries before the remove requests.
    - At the first run after a start, the node broadcasts the requests of the runs before the restart again. It finds
      them in its store: the remove requests with signature V2 of its network identity, and the entries of its
      authorized key pair for the same data. A later run at the same start sends only new requests.
15. Oracle and seed nodes update to this version before the users. Applications of updated nodes do not get data which
    an oracle or seed node has not published with its authorized key pair yet (rule 11).

Reasons:

- Rule 12: oracle and seed nodes publish their data again from their own stores (rule 14). Some of this data has no
  other source: oracle nodes do not publish timestamps, account age and signed witness data again, and they publish
  bonded roles again from their store. So the stores keep these entries until they expire. Applications do not use
  them (rule 11).
- The rule uses the creation date of the request, not the current time. So the validity of an entry does not change
  while it is stored, and no entry must be removed at a fixed time. An entry which rule 12 accepts expires at the
  latest at the cutoff date plus the time to live of its type.
- The other authorized types (alerts, difficulty adjustment, minimum reputation score, release notifications, banned
  user profiles) have no transition: their publishers already use the authorized key pair.
- Rule 14: without it, applications of updated nodes would miss the data of an oracle node until the oracle node
  publishes it again: timestamps, account age and signed witness data only when users request them again. Nodes of
  earlier versions store one entry per data hash (storage key V1) and accept for it only a higher sequence number. With
  the higher sequence number, they replace the entry of the network identity with the one of the authorized key, in
  any order of arrival. Then they also apply later remove requests of the authorized key, so oracle nodes remove data
  only with their authorized key pair. The remove request of the network identity stays in the stores, so a replay of
  the removed entry is rejected, and nodes of earlier versions which missed the migration receive it with the
  inventory.
- Rule 14, broadcast: an oracle node can have thousands of entries to migrate, and for some types (timestamps,
  account age, signed witness and account timestamps) the migration is their only source. A connection sends with a
  queue of 100 messages; if more messages wait, it rejects the message and closes, and the messages in its queue are
  lost. A broadcast sends each message on every connection, so a burst would close all connections of the node. The
  migration applies its requests to the store before it sends them, so a later run finds nothing to migrate; without
  sending the stored requests again, requests which were not sent before a restart would never be sent. The new
  entries are sent first, so that nodes of earlier versions receive the entry of the authorized key before the removal
  of the entry of the network identity; otherwise they remove the data until the new entry arrives.
- Rule 15: the developers and operators control the oracle and seed nodes. So the migration of rule 14 can run before
  the users update.

After the cutoff date plus 100 days, rule 12 accepts no valid data anymore. Then rules 12 to 15,
`isLegacyOwnerAccepted()` and `AuthorizedDataPublisherMigration` can be removed.

The entries of earlier versions have no signature V2. The cutoff date of signature V1 is the same date, but nodes
compare it with their own time, not with the creation date of the entry (see
[`storage-request-signatures.md`](storage-request-signatures.md#cutoff-date-of-signature-v1)). From that date on, nodes
reject the entries of earlier versions and remove the stored ones, also if rule 12 accepts their owner. So the
migration of rule 14 must run before that date. Rule 12 then still accepts only entries with signature V2 and an
earlier creation date, which are the copies described in [Known limitations](#known-limitations).

## Compatibility

- The rule adds no field and changes no signature. Nodes of earlier versions do not check the owner. They accept and
  relay data of any publisher, also copies which updated nodes reject.
- The storage key V2 of data of oracle and seed nodes changes, because the publisher changes. During the transition,
  an updated node can store two entries of the same data: the one of the earlier version and the one of the authorized
  key. Applications use only the one of the authorized key (rule 11).
- Nodes of earlier versions store authenticated data under storage key V1, which contains no owner key. They accept an
  add request of the authorized key only if its sequence number is higher than the one of the stored entry of the
  network identity key. The migration (rule 14) gives it a higher one, so they replace the entry of the earlier
  version.
- An oracle node of an earlier version which publishes after the cutoff date is not accepted by updated nodes.

## Known limitations

- **Copies during the transition.** Until the cutoff date plus the time to live of the type, anyone can publish a copy
  of data of the transition types with a creation date before the cutoff date: for bonded roles until about
  2027-04-11. Updated nodes store and relay such copies until they expire, but applications do not use them (rule 11).
  - Why it cannot be removed now: the stores keep the entries of earlier versions until oracle and seed nodes have
    migrated them (rule 14), and a store cannot tell these entries apart from copies.
  - What removes it: the cutoff date. After all oracle and seed nodes have migrated their data, an earlier cutoff date
    is possible.
  - Who decides: the developers, together with the operators of oracle and seed nodes.
- **Nodes of earlier versions** stay affected by the problem until they update.
- **Several publications of the same data.** Several oracle nodes can authorize the same data with different authorized
  keys. Their entries are separate, and each has its own owner. Applications still remove the data when one of the
  entries is removed or expires. This is not a question of ownership. It needs applications which keep track of each
  publication.

## Verification

- A copy of default authenticated data which another key signs is rejected, and the entry of the owner stays.
  Default authenticated data which names no owner is rejected, also when its data is authorized data.
- Authorized data is accepted only from its authorized key, or from the owner which the data names.
- The transition rule accepts another owner only for the transition types, and only before the cutoff date.
- Applications get neither entries of other publishers nor their removal. With the real
  `AuthorizedBondedRolesService`: the removal of the entry of the earlier version keeps a bonded role which the
  authorized key published, and a copy does not restore a removed role. The store reports the publisher also at
  expiry.
- The migration publishes the data again with the authorized key pair and a higher sequence number and removes the
  entry of the network identity. It runs once, does not publish data again which the owner removed, and ignores entries
  of other publishers, data of other authorities and expired entries. If the store rejects the new entry, the entry of
  the network identity stays, and a later run migrates it. A failure at one entry does not stop the migration of the
  others. Oracle nodes remove bonded roles only with their authorized key pair.
- The node broadcasts the requests of the migration one at a time, the new entries before the removals, and not twice
  at the same start. After a restart it broadcasts the requests of the earlier runs again. Removals of the network
  identity without signature V2 are not requests of the migration.
- After a restart with the startup of the bonded role service, a node still holds the remove request of the owner and
  rejects a replay of the removed entry.
- Persisted entries of another publisher are removed when a store is loaded. A refresh of such an entry is rejected.
- A mailbox add request of another sender is rejected.
- Mailbox sequential data with a receiver hash that does not match the receiver public key bytes is rejected.
- With the real MuSig offerbook: a copy of an offer which another user signs is rejected, the removal of that user does
  not remove the offer, and the maker can remove it.
- `PubKey.isId` and `PubKey.getHashFromId` accept only IDs which `PubKey.getId()` creates, also not hex encodings of
  another length.

## Related documents and code

- [`storage-keys.md`](storage-keys.md): the storage keys, which contain the key of the publisher.
- [`storage-request-signatures.md`](storage-request-signatures.md): the signatures of the requests.
- `OwnershipVerifiableData.getOwnerPubKeyHash()`, `AuthenticatedData.isOwner()`,
  `AuthenticatedSequentialData.isOwnerInvalid()`, `MailboxSequentialData.isSenderInvalid()`.
- `StorageService` (rule 11), `AuthorizedDataPublisherMigration` (rule 14).
