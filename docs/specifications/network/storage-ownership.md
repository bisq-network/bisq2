# Ownership of stored data

This specification defines which key may publish data in the network storage. It covers the stores of authenticated
data and mailbox data. Append-only data has no owner.

## Terms

- **Owner**: the key which may publish the data. In the authenticated data store, only the owner can also refresh and
  remove it. The data names its owner. Authorized data which names no owner is owned by its authorized key (rule 4).
- **Publisher**: the key which signs an add request. The add request contains the hash of this key
  (`pubKeyHash` of `AuthenticatedSequentialData`, `senderPublicKeyHash` of `MailboxSequentialData`). This hash must be
  the owner hash of the signing key (see [The publisher hash](#the-publisher-hash)).
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
   | `AuthorizedOracleNode` | the key of its network ID; a static authorized key owns it as well (rule 4) |
   | other authorized data | none: the authorized key is the owner (rule 4) |
   | `AccountTimestamp` | none: it is published only inside `AuthorizedAccountTimestamp` |
   | append-only data | none: it is not signed and cannot be removed |

2. A profile ID names the owner hash only if it is the lower-case hex encoding of the hash, as `PubKey.getId()` creates
   it: 40 characters for the 20 bytes of the hash. Data with another profile ID is invalid. A node rejects it when it
   constructs or decodes it (see [`validation-at-parsing.md`](validation-at-parsing.md)).

   Reason: one hash must have only one ID. Earlier versions compared the ID with the hex encoding of the publisher's
   hash as strings, so they also accept only this encoding.

   A `BisqEasyOfferbookMessage` which contains an offer is valid only if its author is the maker of the offer. A node
   rejects another message when it constructs or decodes it. So only the maker publishes its offer, and nobody can
   show the offer of another user in the offerbook. Released nodes publish only their own offers.

   Such a message is also valid only if its channel is the offerbook channel of the market of its offer. The ID of
   this channel is `bisq_easy_offerbook.<base currency code>-<quote currency code>`, for example
   `bisq_easy_offerbook.BTC-EUR`. A node rejects another message when it constructs or decodes it. A message without
   an offer can be in any channel.

   Reason: a node shows a message in the channel which the message names. Without the rule, a maker can show its offer
   in the offerbook of every market. For example, the offer list of the desktop shows the amount of a BTC/USD offer in
   the BTC/EUR offerbook without its currency, under the column title "EUR AMOUNT". The API lists the offer for the
   market of the channel.

   Compatibility: unmodified nodes of all released versions publish an offer in the channel of its market. Versions up
   to 2.1.7 wrote the domain part of the ID in lower case with the rules of the default locale of the system. With a
   Turkish or Azerbaijani locale, the letter i in `bisq` of their ID is a dotless i (U+0131). A node of this version
   rejects the offer messages of such nodes. A node shows a message only in a channel with the same ID, and a node
   keeps the IDs of its channels when it updates. So only nodes which first ran a version up to 2.1.7 with such a
   locale showed these offers. Nodes of version 2.1.13 and earlier accept and relay offer messages in other channels
   (see [`validation-at-parsing.md`](validation-at-parsing.md#compatibility)).

### The owner of authenticated data

3. The owner of default authenticated data is the owner which the data names. The store rejects default authenticated
   data whose data names no owner.

   Reason: authorized data names no owner. Wrapped as default authenticated data, it would otherwise skip the check of
   its authorization. Authorized data which names its owner, `AuthorizedOracleNode`, would pass this rule. It is
   rejected at parsing, because authorized data is valid only inside `AuthorizedData` (rule 15 of
   [`../bonded-roles/authorized-data.md`](../bonded-roles/authorized-data.md#the-holder-of-authorized-data)).

4. The authorized key (`authorizedPublicKeyBytes`) is the owner of authorized data which names no owner. If the data
   names an owner, that owner is an owner. The authorized key is then an owner as well only if it is a static key of
   the type of the data. A static key is a public key which is written in the code for a role type (see
   [`../bonded-roles/authorized-data.md`](../bonded-roles/authorized-data.md#terms)). The static flag of the data does
   not matter for this rule, because the publisher sets it.

   Reason: the authorization signature covers the data, but not the publisher. Anyone can copy authorized data with a
   valid authorization signature into their own add request. Only the authority can decide who publishes and removes
   its data.

   Only `AuthorizedOracleNode` names an owner: the key of the network ID of the oracle node. Nodes of v2.1.13 and
   earlier accept an `AuthorizedOracleNode` only from this key, so oracle nodes still publish it with that key pair
   (rule 9). Updated nodes also accept a static authorized key as its owner. A root oracle node is an oracle node
   whose authorized key is a static key. So a later version can publish the `AuthorizedOracleNode` of a root oracle
   node with its authorized key pair, like all other authorized data (see
   [Owner of `AuthorizedOracleNode`](#owner-of-authorizedoraclenode)).

   Reason for the limit to static keys: the store checks only the authorization signature of data without the static
   flag, unless its type requires a static key (rules 2 and 4 of
   [`../bonded-roles/authorized-data.md`](../bonded-roles/authorized-data.md#rules)). So for the store, every key can
   be the authorized key of such data which it signs itself. If every authorized key were an owner, any node
   could publish an `AuthorizedOracleNode` with its own key as authorized key and as publisher. Updated nodes would
   store, relay and serve such entries. Nodes of v2.1.13 and earlier reject them each time they receive them, so the
   entries never enter the inventory filter of these nodes. Updated nodes would send them again with every inventory
   response, with the highest priority, until the response is full. Nodes of v2.1.13 and earlier could then not
   synchronize from updated nodes. Only the operators of the root oracle nodes hold the static keys, so no third party
   can publish an `AuthorizedOracleNode` as its second owner.

### The publisher hash

The publisher hash of an add request must equal the hash of the public key bytes which sign the request
(`ownerPublicKeyBytes` of `AddAuthenticatedDataRequest`, `senderPublicKeyBytes` of `AddMailboxRequest`). An add request
with another hash is invalid when it is constructed or decoded, including persisted entries (see
[`validation-at-parsing.md`](validation-at-parsing.md)).

Reason: storage key V2 and the signature use the public key bytes, but the owner checks (rules 5 and 6) use the
publisher hash. If both could differ, the owner check would not check the key which signs the request. Nodes of all
released versions (since 2.0.0) reject such add requests at admission, so no honest node stores or relays one. A parse
failure therefore affects only a peer which sends a request that no released node creates.

### Admission

5. An add request of authenticated data is valid only if the publisher is the owner of the data. The only exception is
   the transition rule below. The rule compares fields of the request, so a node rejects other add requests when it
   constructs or decodes them, including persisted entries (see [`validation-at-parsing.md`](validation-at-parsing.md)).
   The owner can only be read from valid data, for example from a valid profile ID, so the data checks its own content
   when it is created, before the add request reads the owner.
6. A mailbox add request is valid only if the sender is the owner of the confidential message. A node rejects mailbox
   sequential data of another sender when it constructs or decodes it, including persisted entries (see
   [`validation-at-parsing.md`](validation-at-parsing.md)). Released nodes since 2.0.0 sign the add request with the
   key pair which encrypts the confidential message, so they create only valid requests. The receiver removes mailbox
   data; storage key V2 of mailbox data contains the key of the
   receiver (see [`storage-keys.md`](storage-keys.md)). The receiver hash in the sequential data must equal the hash of
   its receiver public key bytes. Reject inconsistent sequential data when it is constructed or decoded, including
   persisted entries. Otherwise the key which finds the entry cannot pass its removal authorization check, and the
   key which passes that check looks up another entry. No key could remove the message. Released nodes always derive
   the receiver hash from the receiver public key, so valid existing entries already satisfy this rule.
7. Remove requests need no own owner check. Their storage key contains the key which signs them, so they find only the
   entry which the same key added (see [`storage-keys.md`](storage-keys.md)). The same holds for refresh requests, but
   a refresh has signature V2, and the transition rule does not accept another owner for a request with signature V2
   (rule 12). So the store accepts a refresh only from the owner of the entry. An entry of another owner, which rule 12
   accepted from a node of an earlier version, keeps its creation date and expires.
8. When a node loads an authenticated data store, it does not load the add requests whose publisher is not the owner of
   the data. They fail at parsing (rule 5), and the store skips entries which it cannot parse.

   Reason: earlier versions did not check the owner. An entry which an attacker added before the update could
   otherwise be removed after the update, with the effect described in [Problem](#problem).

### Publishers of authorized data

9. Oracle and seed nodes publish and remove authorized data with their authorized key pair: the key pair whose public
   key is `authorizedPublicKeyBytes`. `AuthorizedOracleNode` is the exception: the oracle node publishes it with the key
   pair of its network ID, because nodes of v2.1.13 and earlier accept it only from that key.
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

    Several keys can authorize the same data, for example several oracle nodes the same bonded role. Their entries are
    separate, and each has its own owner. Applications keep the data while one of these publications remains (rule 18
    of [`../bonded-roles/authorized-data.md`](../bonded-roles/authorized-data.md#several-publications-of-the-same-data)).

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
    request was created before the cutoff date `AuthorizedData.LEGACY_OWNER_CUTOFF_DATE` (2027-01-01 UTC) and has no
    signature V2. An add request with signature V2 of another owner fails at parsing.
13. The cutoff date must be later than the date at which all oracle and seed nodes publish with their authorized key
    pair. Updated nodes reject data which an oracle node of an earlier version creates after the cutoff date. While the
    cutoff date of signature V1 (2026-11-25) is earlier, this date decides nothing: rule 12 needs a request without
    signature V2, and from the cutoff date of signature V1 on, nodes reject such requests and remove the stored ones.
    The date is kept as a backstop in case the cutoff date of signature V1 moves past it.
14. At startup, oracle and seed nodes publish the data of the transition types which they published with the key pair
    of their network identity again with their authorized key pair, and they remove the entries of their network
    identity. They do not migrate data of other types: `AuthorizedOracleNode` names the key of the network identity as
    its owner, and nodes of earlier versions accept it only from that key (see rule 9). The remove request
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
- Rule 12, signature V2: the entries of the transition come only from oracle and seed nodes of earlier versions, which
  create only signature V1. A node of this version publishes its own data with the key of the owner. A request with
  signature V2 of another owner is therefore a copy, which any key can create from stored data. Each key gives a new
  storage key, so copies of one item can fill the store of its type, and the store then refuses new data of the real
  oracle nodes. Without this condition, copies were possible until the cutoff date plus the time to live of the type,
  for bonded roles until about 2027-04-11. With it, copies without signature V2 are still possible until the cutoff
  date of signature V1 (see [Known limitations](#known-limitations)).
- The rule uses the creation date of the request, not the current time, so the validity of an entry under rule 12
  does not change while it is stored. An entry which rule 12 accepts has no signature V2, so each node removes it at
  the cutoff date of signature V1, which it measures with its own clock (see
  [`storage-request-signatures.md`](storage-request-signatures.md#cutoff-date-of-signature-v1)).
- The other authorized types (alerts, difficulty adjustment, minimum reputation score, release notifications, banned
  user profiles) have no transition: their publishers already use the authorized key pair.
- Rule 14: without it, applications of updated nodes would miss the data of an oracle node until the oracle node
  publishes it again: timestamps, account age and signed witness data only when users request them again. Nodes of
  earlier versions store one entry per data hash (storage key V1) and accept for it only a higher sequence number. With
  the higher sequence number, they replace the entry of the network identity with the one of the authorized key, in
  any order of arrival. Then they also apply later remove requests of the authorized key, so oracle nodes remove data
  only with their authorized key pair. The remove request of the network identity stays in the stores, so a replay of
  the removed entry is rejected, and updated nodes which missed the migration receive it with the inventory. Nodes of
  earlier versions which missed the migration receive the entry of the authorized key with the inventory, which
  replaces the entry of the network identity. They do not receive the remove request, because they could not store it
  (rule 11 of [`inventory-hash-set-filter.md`](inventory-hash-set-filter.md)).
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

The entries of earlier versions have no signature V2. The cutoff date of signature V1 (2026-11-25) is earlier than the
cutoff date of rule 12 (2027-01-01), and nodes compare it with their own time, not with the creation date of the entry
(see [`storage-request-signatures.md`](storage-request-signatures.md#cutoff-date-of-signature-v1)). From that date on,
nodes reject the entries of earlier versions and remove the stored ones, also if rule 12 accepts their owner. So the
migration of rule 14 must run before that date. From that date on, rule 12 accepts no entry, because an entry of
another owner needs to be without signature V2.

So from the cutoff date of signature V1 on, the stores hold no entry of rule 12 and accept none. A version which is
released after that date can remove rules 12 to 15, `isLegacyOwnerAccepted()`, `LEGACY_OWNER_CUTOFF_DATE` and
`AuthorizedDataPublisherMigration`.

## Owner of `AuthorizedOracleNode`

The exception of `AuthorizedOracleNode` (rules 4 and 9) makes the oracle node use two key pairs for its authorized
data. Its cause is a check of nodes of v2.1.13 and earlier: they reject an `AuthorizedOracleNode` whose publisher is
not the key of its network ID. A node can accept a new owner only if it runs a version which accepts it. So the
exception is removed in three steps, each in a later release than the one before:

1. Nodes accept the key of the network ID and a static authorized key as owner (rule 4). Oracle nodes publish with the
   key of the network ID. This is the current version.
2. When most nodes accept both keys, root oracle nodes also publish their `AuthorizedOracleNode` with the authorized
   key pair. They keep the entry of the network ID for nodes of v2.1.13 and earlier. Applications keep the oracle node
   while one authorized publication of it is stored (rule 8 of
   [`../bonded-roles/authorized-data.md`](../bonded-roles/authorized-data.md#rules)).
3. When nodes of v2.1.13 and earlier are no longer supported, oracle nodes publish only with the authorized key pair
   and remove the entry of the network ID. `AuthorizedOracleNode` then names no owner, and rules 1, 4 and 9 lose the
   exception. This step removes the oracle nodes from nodes of v2.1.13 and earlier, so it must come with a minimum
   required version.

Consequences of the limit to static keys (rule 4):

- The exception is not a security problem while the second owner is a static key. Both owners then belong to the
  operator of a root oracle node.
- Step 2: nodes of v2.1.13 and earlier reject the entry of the authorized key each time they receive it, and updated
  nodes send it to them with every inventory response. This costs one entry per root oracle node and response. An
  entry has about 1 KB (879 bytes with one clearnet address, more with further addresses). Root oracle nodes are few,
  so the responses still have room for the other data.
- An oracle node whose authorized key is not a static key cannot use step 2. Updated nodes accept its
  `AuthorizedOracleNode` only from the key of its network ID, as nodes of v2.1.13 do. Such oracle nodes are not allowed
  at the moment (see
  [`../bonded-roles/authorized-data.md`](../bonded-roles/authorized-data.md#temporary-restriction-to-static-oracle-node-keys)).
  If they are allowed again, they publish with the key of their network ID until step 3. From step 3 on, only nodes
  with the version of step 3 or a later one accept their `AuthorizedOracleNode`, because earlier versions still apply
  rule 4. The minimum required version of step 3 must then be the version of step 3.

The data publisher migration (rule 14) does not apply to `AuthorizedOracleNode`. It would remove the entry of the
network ID, which nodes of v2.1.13 and earlier need.

Who decides on steps 2 and 3: the developers, together with the operators of the oracle nodes.

## Compatibility

- The rule adds no field and changes no signature. Nodes of earlier versions do not check the owner, except for
  `AuthorizedOracleNode`, which they accept only from the key of its network ID. They accept and relay data of any
  publisher, also copies which updated nodes reject. An updated node skips such a copy in an
  inventory response. When a node of an earlier version relays such a copy as a broadcast, an updated node drops the
  message and keeps the connection to the relay (see rule 6 of
  [`validation-at-parsing.md`](validation-at-parsing.md#rules)). Honest nodes publish only data which they own,
  so only a modified client creates such a copy.
- Nodes of v2.1.13 and earlier reject an `AuthorizedOracleNode` which its authorized key published, the second owner
  of rule 4. They reject it each time they receive it, so it never enters their inventory filter, and an updated node
  sends it to them again with every inventory response. Rule 4 therefore accepts the authorized key as owner only if
  it is a static key. Then only root oracle nodes can publish such an entry, and they do so only from step 2 of
  [Owner of `AuthorizedOracleNode`](#owner-of-authorizedoraclenode) on. An `AuthorizedOracleNode` which the key of its
  network ID published is accepted by all versions.
- The storage key V2 of data of oracle and seed nodes changes, because the publisher changes. During the transition,
  an updated node can store two entries of the same data: the one of the earlier version and the one of the authorized
  key. Applications use only the one of the authorized key (rule 11).
- Nodes of earlier versions store authenticated data under storage key V1, which contains no owner key. They accept an
  add request of the authorized key only if its sequence number is higher than the one of the stored entry of the
  network identity key. The migration (rule 14) gives it a higher one, so they replace the entry of the earlier
  version.
- An oracle node of an earlier version which publishes after the cutoff date is not accepted by updated nodes.

## Known limitations

- **Copies during the transition.** Until the cutoff date of signature V1 (2026-11-25), anyone can publish a copy
  without signature V2 of data of the transition types, under a new key for each copy. Each copy gets its own storage
  key, so copies can fill the store of a type up to its limit of 100,000 entries. Updated nodes then refuse new storage
  keys of that type, also the new data of the real oracle and seed nodes, for example new timestamps, account age data
  or bonded roles. Copies expire with the time to live of their type, so an attacker has to publish them again, and this
  can last until the cutoff date of signature V1, when the nodes remove all copies. Applications do not use the copies
  (rule 11). Nodes of earlier versions store one entry per data hash, so the copies do not fill their stores.
  - Why it cannot be removed now: the stores keep the entries of earlier versions until oracle and seed nodes have
    migrated them (rule 14), and a store cannot tell these entries apart from copies without signature V2.
  - What would close it earlier: the store accepts at most one entry of another owner per storage key V1 (the data
    hash), so the number of such entries cannot exceed the number of real items. The `HASH_SET_V2` filter would then
    have to list these entries by that key, or nodes which hold different copies send them to each other in every
    inventory round. The developers decided on 2026-10-13 to accept the window for 2.1.14 instead.
  - What removes it: the cutoff date of signature V1.
  - Who decides: the developers, together with the operators of oracle and seed nodes.
- **Nodes of earlier versions** stay affected by the problem until they update.

## Verification

- A copy of default authenticated data which another key signs is rejected, and the entry of the owner stays.
  Default authenticated data which names no owner is rejected, also when its data is authorized data.
- Authorized data which names no owner is accepted only from its authorized key. Authorized data which names an owner
  is accepted from that owner. It is accepted from its authorized key only if that key is a static key of its type.
  An add request of another authorized key cannot be created, and a received one fails at parsing. A static key which
  did not authorize the data is not its owner.
- With the real `AuthorizedOracleNode` and the real store: the store accepts it from the key of its network ID and
  from a static authorized key, with and without the static flag. An authorized key which is no static key is not
  its owner.
- The transition rule accepts another owner only for the transition types, only before the cutoff date, and only for
  requests without signature V2. A request with signature V2 of another owner cannot be created, and the entry of
  another owner cannot be refreshed.
- Applications get neither entries of other publishers nor their removal. With the real
  `AuthorizedBondedRolesService`: the removal of the entry of the earlier version keeps a bonded role which the
  authorized key published, and a copy does not restore a removed role. The store reports the publisher also at
  expiry.
- The migration publishes the data again with the authorized key pair and a higher sequence number and removes the
  entry of the network identity. It runs once, does not publish data again which the owner removed, and ignores entries
  of other publishers, data of other authorities, data of other types than the transition types and expired entries. If the store rejects the new entry, the entry of
  the network identity stays, and a later run migrates it. A failure at one entry does not stop the migration of the
  others. Oracle nodes remove bonded roles only with their authorized key pair.
- The node broadcasts the requests of the migration one at a time, the new entries before the removals, and not twice
  at the same start. After a restart it broadcasts the requests of the earlier runs again. Removals of the network
  identity without signature V2 are not requests of the migration.
- After a restart with the startup of the bonded role service, a node still holds the remove request of the owner and
  rejects a replay of the removed entry.
- Persisted entries of another publisher are removed when a store is loaded. A refresh of such an entry is rejected.
- A mailbox add request of another sender is rejected.
- Add requests (authenticated and mailbox) whose publisher hash does not match the signing public key bytes are
  rejected when they are constructed or decoded, whichever of the two fields is changed.
- Mailbox sequential data with a receiver hash that does not match the receiver public key bytes is rejected.
- With the real MuSig offerbook: a copy of an offer which another user signs is rejected, the removal of that user does
  not remove the offer, and the maker can remove it.
- An offerbook message with the offer of another maker, or with an offer in the channel of another market, cannot be
  created, and a received one fails at parsing. A message without an offer can be in the channel of any market.
- `PubKey.isId` and `PubKey.getHashFromId` accept only IDs which `PubKey.getId()` creates, also not hex encodings of
  another length.

## Related documents and code

- [`storage-keys.md`](storage-keys.md): the storage keys, which contain the key of the publisher.
- [`storage-request-signatures.md`](storage-request-signatures.md): the signatures of the requests.
- `OwnershipVerifiableData.getOwnerPubKeyHash()`, `AuthenticatedData.isOwner()`, `AuthorizedData.isOwner()` (rule 4),
  `AuthenticatedSequentialData.verify()`, `MailboxSequentialData.verify()`.
- `StorageService` (rule 11), `AuthorizedDataPublisherMigration` (rule 14).
- `BisqEasyOfferbookMessage.verify()` (the maker and the channel of an offerbook message).
