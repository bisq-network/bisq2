# Storage MetaData

This specification defines where the network storage takes the MetaData of stored data from, and how nodes of
different versions stay compatible.

## Terms

- **Storable type**: a type whose objects the network storage stores, for example a `DistributedData` or a
  `MailboxMessage`.
- **MetaData**: the storage properties of a storable type:
  - `className`: the simple class name of the type. It selects the store and the name of the store file.
  - `ttl`: the time after which an entry expires.
  - `priority`: the order of entries in an inventory response.
  - `maxMapSize`: not used by this version. Nodes of earlier versions use it as an input of the cost factor of
    messages (see [Message cost](message-cost.md) and [Size limit](#size-limit)).
- **Store**: the storage of one storable type. Its key is the class name of the type.

## Rules

### MetaData belongs to the type

1. Each storable type declares exactly one MetaData. The `className` of that MetaData is the simple class name of the
   type.
2. At startup, the application registers every storable type with its MetaData in the storable types registry. The
   key of the registry is the class name.
3. The storage layer does not accept data of a type which is not registered. An empty registry accepts no type. A
   store also rejects a registered type of another store type (rule 9).

Reason: MetaData is a property of the type, not of a single object. A local table from type to MetaData lets a node
decide without trusting values from the network.

### A node uses only its own MetaData

4. For expiry, priority and size limit, a node uses only the MetaData which it registered for the class name of the
   data. It never uses MetaData values received from the network for these decisions. The cost factor of messages
   does not depend on MetaData (see [Message cost](message-cost.md)).
5. The class name is the only value which a node takes from a message for this purpose. It is the key into the
   registry.

Reason: for mailbox data, remove requests and refresh requests the receiver does not have the payload in readable
form. Earlier versions took the MetaData from the message. The sender or any relay chose it, and neither the
signature nor the hash covered it. So they could choose the time to live, the priority and the cost factor.

### Where the class name comes from

6. Authenticated data and append-only data: the class name of the deserialized payload.
7. `MailboxData`, `RemoveMailboxRequest`, `RemoveAuthenticatedDataRequest` and `RefreshAuthenticatedDataRequest`: the
   field `className`.

### A store holds only its own type

8. A store rejects a request whose class name differs from the store key. For authenticated and append-only data it
   compares the runtime class of the payload.

9. Each storable type belongs to exactly one store type: a `MailboxMessage` to the mailbox store, an `AppendOnlyData`
    to the append-only store, and any other `DistributedData` to the authenticated data store. A type which is both a
    `MailboxMessage` and a `DistributedData`, or neither, cannot be registered. A store accepts only class names of its
    own store type. This also applies when a node loads its persisted stores at startup.

Reason: all entries of a store then use the MetaData of the store type. A type which returned the MetaData of another
type would otherwise put its data into the store of that other type. For rule 9: mailbox data and remove requests
carry only a class name. Signature V1 does not cover it, and before the cutoff date of signature V1 a relay can remove
signature V2 (see [`storage-request-signatures.md`](storage-request-signatures.md#known-limitations)). Without rule 9, a sender or relay could name a
`DistributedData` type in a mailbox request. The request would then get the MetaData of that type, for example a time
to live of 100 days instead of at most 15 days, and an expired mailbox entry would become valid again.

### Expiry

10. An entry expires when the time since its `created` date is longer than the `ttl` of the registered MetaData.
    Mailbox data and mailbox remove requests expire at the latest after 15 days, even if their MailboxMessage type
    declares a longer `ttl`.

Reason for the upper bound: the mailbox store keeps encrypted data of receivers which are offline. Its retention is a
policy of the network storage. It must not grow because a new or changed MailboxMessage type declares a longer `ttl`.

### Size limit

11. A store accepts at most 100 000 entries. `MetaData.maxMapSize` is not used as limit.

Reason: until this rule, the code intended to read the limit from the first entry of the store, with a lower bound of
10 000. It cached the result on the first call, which happens while the map is still empty. So the effective limit of
all stores was 100 000. Enforcing `maxMapSize` would lower the limit to at most 10 000 and could remove entries from
existing stores. Whether stores need per-type limits is an open decision, see [Open points](#open-points).

12. A removal without entry is a removal for whose storage key the store holds no entry. A store accepts a removal
    without entry only while fewer than 50 000 of its entries are removals without
    [replaced add request](storage-request-ordering.md#replaced-add-request-of-a-stored-removal). This limit does not
    apply to a removal which replaces a stored add request or a stored removal.

Reason: a removal contains only the hash of the removed data. If the store does not hold the data, it cannot check
the owner of the data, and for an authorized type it cannot check the authorization. So every key pair can create
removals without entry for every store, also for the stores of alerts, bans and bonded roles. Without rule 12, a peer
can fill a store with 100 000 such removals. The store then rejects every add request under a new key until the
removals expire, which takes up to 100 days. With rule 12, such removals use at most half of a store. The other half
stays available for add requests and for the removals of data which the store checked.

The limit counts only the removals without replaced add request. A removal with replaced add request removed data which
passed the checks of an add request in this store. A peer which can create such data can fill the store with add
requests as well, so these removals give it no further way to fill the store.

## Compatibility

- The field `metaData` of `MailboxData`, `RemoveMailboxRequest`, `RemoveAuthenticatedDataRequest` and
  `RefreshAuthenticatedDataRequest` is deprecated. New nodes still set it to the registered MetaData of the type,
  because nodes of earlier versions use it for store selection, expiry and cost factor.
- When a node receives such a message, it replaces the received `metaData` by the MetaData which it registered for the
  class name, in every version of the message. It then relays and persists the message with the registered values. It
  keeps the received value only if the class name is not registered.
- `serializeForHash()` never contains `metaData`, in any version of these four classes. So the replacement changes
  neither signature V1, the storage key nor the proof of work payload. The preimage of signature V2 never contains
  `metaData` either (see
  [`storage-request-signatures.md`](storage-request-signatures.md#requirements-for-signature-v2)).

  Reason: `metaData` is derived from the class name; it carries no information of its own. Versions 1 to 3, which
  supported nodes send, already left it out of the hash. Version 0 is the format of releases before the version field
  existed and is no longer in use, and versions above 3 are not used. Leaving the field out of the hash in every version
  means that no version lets a relay change a hash input without changing the request's signed content.
- The class name of `metaData` must equal `className`. Parsing rejects a message which breaks this rule. For a
  registered class name the replacement above guarantees it; the rule matters for class names which this node does not
  know.

  Reason: nodes of earlier versions select the store by the class name of `metaData`, nodes of this version by
  `className`. If the two differed, the two kinds of nodes would put the data into different stores.

  Reason: otherwise a node forwards values which a sender or relay chose, and which it does not use itself. A node of
  an earlier version would use them for store selection, expiry and cost factor. It would then expect another cost
  factor than the relay paid (see [Message cost](message-cost.md#compatibility)), and select another store if
  `className` and `metaData` name different types.
- `className` is excluded from the hash. Preimages of signature V1, storage keys, proof of work payloads and the order
  of inventory entries are computed without it. Nodes of earlier versions drop the unknown field and compute the same
  bytes. The preimage of signature V2 contains it; nodes of earlier versions do not verify signature V2.
- If `className` is empty, the class name of `metaData` is used. This covers messages from nodes of earlier versions,
  messages relayed by such nodes, and data persisted by them.
- `className` must have the format of the class name of MetaData: at most 50 characters, starting with an upper-case
  letter, followed by letters, digits, `_` or `$`. A node checks the format when it parses a message (rule 1 of
  [`validation-at-parsing.md`](validation-at-parsing.md)). Nodes of this version take `className` from registered
  MetaData, so they create only valid values. Nodes of earlier versions do not send `className` (see the rule for an
  empty `className`).

  Reason: the sender chooses the value, and the node uses it to select a store and writes it to its log. With the check
  at parsing, no later component handles a value of any length or with line breaks.
- The store accepts only registered class names. The check depends on the types which the node registers, not only on
  the format of the message, so the store applies it (rule 5 of [`validation-at-parsing.md`](validation-at-parsing.md)).
- Each node applies its own MetaData. If two versions have different MetaData for a type, their entries can expire at
  different times. The cost factor of messages does not depend on MetaData. Nodes of earlier versions compute it
  from the MetaData in the message (see [Message cost](message-cost.md#compatibility)).
- Nodes of earlier versions do not apply rule 12. They accept removals without entry until their store holds
  100 000 entries. A node of this version which did not accept such a removal receives it again with the inventory
  responses of other nodes, until it expires.
- Nodes of earlier versions did not record replaced add requests. After an update, the removals which such a node
  stored count against the limit of rule 12 until they expire.

## Known limitations

- Signature V1 does not cover `className`. Signature V2 covers it for mailbox data and remove requests, but a relay can
  remove signature V2 while receivers accept requests with only signature V1 (see
  [`storage-request-signatures.md`](storage-request-signatures.md#known-limitations)). Until then, a relay can change
  `className` to the class name of another registered type of the same store type (rule 9). The data then goes into
  the store of that type and uses its MetaData. A remove request then does not find its add entry and is stored in the
  other store without an add entry. The cutoff date of signature V1 removes this: from 2027-01-01 on, nodes accept
  only requests with signature V2 (see
  [`storage-request-signatures.md`](storage-request-signatures.md#cutoff-date-of-signature-v1)).
- Nodes of earlier versions still use the MetaData from the network until they update.
- Removals without entry still use up to half of a store (rule 12). A node stores them, relays them and sends them
  with inventory responses, as it does with the removals of stored data. A peer can so place up to 50 000 entries in
  each store of each node. While a store holds 50 000 removals without replaced add request, it does not keep a
  further removal which arrives before its add request. That add request is then accepted until the removal arrives
  again with an inventory response.
  - The record of the replaced add request is local. A node which never held the data, for example a node which starts
    for the first time, holds every removal without replaced add request. For such a node, the removals of a peer which
    fills the store and the removals of real data look the same.
  - Why it is not removed now: a node which holds no add request, for example a node which starts for the first time,
    needs the removals to reject add requests which were removed. The inventory protocol sends a node every entry
    which is not in its filter, so a node which did not store these removals would receive them with every response.
  - What would remove it: a removal which the store can check without the data, or another way to tell nodes which
    data was removed. Both change the protocol. The developers have to decide.

## Open points

- Whether stores need per-type size limits, and which values. This needs the sizes of real stores.
