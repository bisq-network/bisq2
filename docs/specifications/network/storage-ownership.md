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

1. A `DistributedData` type whose data names its owner in its own fields implements `OwnedDistributedData`. Its
   method `getOwnerPubKeyHash()` returns the owner hash. Types whose data names no owner do not implement it.
   The methods of `NetworkService` which publish, refresh and remove default authenticated data accept only
   `OwnedDistributedData`, so the compiler rejects a type which cannot have an owner there.

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
   it. Data with another profile ID is invalid (`isDataInvalid()`).

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
   receiver (see [`storage-keys.md`](storage-keys.md)).
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

11. For these types (`isLegacyOwnerAccepted()`), the store accepts authorized data with another owner if the add
    request was created before the cutoff date `AuthorizedData.LEGACY_OWNER_CUTOFF_DATE` (2027-01-01 UTC).
12. The cutoff date must be later than the date at which all oracle and seed nodes publish with their authorized key
    pair. Updated nodes reject data which an oracle node of an earlier version creates after the cutoff date.
13. Until no data of rule 11 is valid anymore, oracle nodes remove a bonded role also with the key pair of their network
    identity. Otherwise the copy of the earlier version keeps the role in the store of updated nodes until it expires.

Reasons:

- Oracle nodes do not publish timestamps, account age and signed witness data again; users request them again before
  they expire. Oracle nodes publish DAO data again only after a minimum age. Rejecting these entries at once would
  remove reputation data from updated nodes and from new nodes for up to the time to live.
- Oracle nodes publish bonded roles again from their own authenticated data store. If an updated oracle node removed its
  entries of earlier versions, it could not publish the roles again.
- The rule uses the creation date of the request, not the current time. So the validity of an entry does not change
  while it is stored, and no entry must be removed at a fixed time. An entry which rule 11 accepts expires at the
  latest at the cutoff date plus the time to live of its type.
- The other authorized types (alerts, difficulty adjustment, minimum reputation score, release notifications, banned
  user profiles) have no transition: their publishers already use the authorized key pair.

After the cutoff date plus 100 days, rule 11 accepts no valid data anymore. Then rules 11 to 13 and
`isLegacyOwnerAccepted()` can be removed.

## Compatibility

- The rule adds no field and changes no signature. Nodes of earlier versions do not check the owner. They accept and
  relay data of any publisher, also copies which updated nodes reject.
- The storage key V2 of data of oracle and seed nodes changes, because the publisher changes. During the transition,
  an updated node can store two entries of the same data: the one of the earlier version and the one of the authorized
  key.
- Nodes of earlier versions store authenticated data under storage key V1, which contains no owner key. They accept an
  add request of the authorized key only if its sequence number is higher than the one of the stored entry of the
  network identity key. Otherwise they keep the stored entry until it expires, and then accept the data of the
  authorized key again, for example from an inventory response.
- An oracle node of an earlier version which publishes after the cutoff date is not accepted by updated nodes.

## Known limitations

- **Copies during the transition.** Until the cutoff date plus the time to live of the type, anyone can publish a copy
  of data of the transition types with a creation date before the cutoff date, and remove it later. The effect is the
  one of [Problem](#problem), for bonded roles until about 2027-04-11. `OracleNodeService` removes the entry of its
  own bonded role when a role disappears from its local list of bonded roles. So during the transition, the removal of
  a copy can also make an oracle node remove the real bonded role.
  - Why it cannot be removed now: data which earlier oracle nodes published stays valid for up to its time to live,
    and some of it is not published again (see the reasons of [Transition](#transition)).
  - What removes it: the cutoff date. An earlier cutoff date is possible if oracle nodes publish all their data of
    earlier versions again with their authorized key pair before the release, with a sequence number higher than the
    one of the entry of the earlier version.
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
- Persisted entries of another publisher are removed when a store is loaded. A refresh of such an entry is rejected.
- A mailbox add request of another sender is rejected.
- With the real MuSig offerbook: a copy of an offer which another user signs is rejected, the removal of that user does
  not remove the offer, and the maker can remove it.
- `PubKey.findHashFromId` accepts only IDs which `PubKey.getId()` creates.

## Related documents and code

- [`storage-keys.md`](storage-keys.md): the storage keys, which contain the key of the publisher.
- [`storage-request-signatures.md`](storage-request-signatures.md): the signatures of the requests.
- `OwnedDistributedData.getOwnerPubKeyHash()`, `AuthenticatedData.isOwner()`,
  `AuthenticatedSequentialData.isOwnerInvalid()`, `MailboxSequentialData.isSenderInvalid()`.
