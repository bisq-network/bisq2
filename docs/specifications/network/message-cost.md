# Message cost

This specification defines the cost factor of the messages which peers send to each other, and how nodes of
different versions compute the same value.

## Terms

- **Cost factor**: a number from 0 to 1 for each message. A peer authorizes a received message only if the sender
  paid a cost which grows with the cost factor of the message.
- **Authorization method**: the way in which the sender pays the cost. Today this is a proof of work (hash cash). The
  method converts the cost factor and the network load of the receiver into its own measure, for example the
  difficulty of the proof of work, and it defines how much the paid cost may be below the expected cost. This
  specification does not depend on the method. Other methods can use the same cost factor.
- **Storable type**: a type whose objects the network storage stores (see [Storage MetaData](storage-metadata.md)).
- **Storage cost factor**: the part of the cost factor which depends on the storable type which a request adds,
  removes or refreshes.
- **Data cost factor**: the part of the cost factor which depends on the type of authenticated or append-only data.
  The receiver can read such data.
- **Policy**: the rules and values of this specification. The code implements them in `MessageCostPolicy`.

## Rules

### The policy defines the value

1. The cost factor of a message, which the policy defines, depends only on
   - the type of the message,
   - values which the receiver reads from the message: the class name of the stored type and the size of the cipher
     text of an encrypted message,
   - the values of the policy.

   It does not depend on other definitions of a node, for example the MetaData of a type or the size limits of
   messages.
2. The values of the policy are protocol constants. An existing value never changes. A new storable type adds its
   values.
3. The policy has a storage cost factor for every storable type, and a data cost factor for every type of
   authenticated or append-only data.

Reason: the sender and the receiver compute the cost factor independently, and they often run different versions. If
they compute different values, the receiver expects another cost than the sender paid. When the difference exceeds
what the authorization method tolerates, the receiver rejects the message and closes the connection. Earlier versions
computed the cost factor from the MetaData of each type and from a method of each message class. A change of these
definitions, for example a longer time to live for storage reasons, changed the cost factor without anybody noticing.

### Values

4. These messages have a constant cost factor:

   | Message | Cost factor |
   |---|---|
   | `ConnectionHandshake.Request`, `ConnectionHandshake.Response`, `CloseConnectionMessage`, `Ping`, `Pong`, `NetworkLoadExchangeRequest`, `NetworkLoadExchangeResponse` | 0.05 |
   | `PeerExchangeRequest`, `PeerExchangeResponse`, `InventoryResponse` | 0.1 |
   | `ReportResponse` | 0.2 |
   | `InventoryRequest` | 0.25 |
   | `ReportRequest` | 0.5 |

5. `ConfidentialMessage`: `bounded(0.1, 1, s / 20 000)`, where `s` is the size of the cipher text in bytes.
6. `AddAuthenticatedDataRequest`: `bounded(0.25, 0.75, storage(c) + data(c))`, where `c` is the class name of the
   authenticated data.
7. `AddAppendOnlyDataRequest`: `bounded(0.5, 1, data(c))`, where `c` is the class name of the append-only data.
8. `AddMailboxRequest`: `bounded(0.25, 1, storage(c) + m)`, where `c` is the field `className` of `MailboxData` and
   `m` is the cost factor of its `ConfidentialMessage` (rule 5).
9. `RemoveAuthenticatedDataRequest`, `RefreshAuthenticatedDataRequest` and `RemoveMailboxRequest`:
   `bounded(0.1, 0.3, storage(c))`, where `c` is the field `className` of the request.

   The receiver does not read the deprecated field `metaData` for rules 8 and 9 in any version. Parsing replaces
   the field with the registered MetaData of the class name, and `serializeForHash()` never contains it (see
   [Storage MetaData](storage-metadata.md#compatibility)). A sender or relay therefore cannot choose a MetaData which
   changes the cost which the next relay pays.
10. The storage cost factor of a type is a constant of the policy with two decimal places. Its value is the result
    of the formula which versions up to 2.1.13 apply to the MetaData of the type,
    `bounded(0, 1, bounded(0, 1, ttl / 100 days) + bounded(0, 1, n / 10 000))`, rounded half up to two decimal places.
    `ttl` and `n` are the time to live and the maximum map size of the MetaData when this policy was defined. A later
    change of the MetaData does not change the value. A test cross-checks the values with the current MetaData of all
    types.
11. The data cost factor of a type is a constant of the policy: 0.2, 0.3 or 0.5 for the types of this version.
12. A class name for which the policy has no value adds 0.

    Reason: the authorization runs before the storage layer checks the class name. A type which a later version adds
    must not make the authorization fail. The storage layer does not store data of such a type.
13. A message type for which the policy has no rule has the cost factor 1.

    Reason: peers send a `MailboxMessage`, including `AckMessage`, only inside a `ConfidentialMessage` or an
    `AddMailboxRequest`. They do not send it as a message of its own. A node drops such a message when it parses it,
    before it checks the cost (see [Confidential messages](confidential-messages.md)). A type for which the policy has
    no rule gets the highest cost.

`bounded(a, b, x)` is `x`, limited to the range from `a` to `b`. The class names and the values of rules 10 and 11 are
listed in `MessageCostPolicy`.

### Sender and receiver

14. The receiver of a message expects the cost factor of the policy. It accepts a higher cost.
15. The sender of a message pays at least the cost factor of the policy. For `AddMailboxRequest`,
    `RemoveMailboxRequest`, `RemoveAuthenticatedDataRequest` and `RefreshAuthenticatedDataRequest` it pays the higher
    of two values: the cost factor of the policy, and the cost factor of rules 8 and 9 with the storage cost factor of
    the MetaData in the deprecated field `metaData` of the message. That storage cost factor uses the formula of rule
    10 with the `ttl` and the maximum map size of this MetaData, without rounding.

    Reason: nodes of versions up to 2.1.13 compute the storage cost factor of these requests from the field `metaData`,
    not from the class name. A node of this version fills the field with its registered MetaData of the type (see
    [Storage MetaData](storage-metadata.md#compatibility)). Today the storage cost factor of this MetaData equals the
    value of the policy before rounding. If the MetaData of such a type changes, for example its time to live, nodes of
    earlier versions expect another cost factor than the policy. The sender pays the higher value, so that receivers of
    all versions accept the message.

## Changing a value

Rule 2 forbids changing a value of this policy. A change of the cost of an existing message needs a new version of the
policy which both peers of a connection agree on:

- A node which supports a new version announces it as a `Feature` in its `Capability`. Nodes of earlier versions
  ignore features which they do not know.
- Both peers of a connection use the highest version which both support. A peer which announces no version uses the
  policy of this specification.
- The handshake exchanges the capabilities, so no new field in any message is needed.

This mechanism does not exist yet, because no second version exists.

## Compatibility

- For every type of this version, the policy has the values which versions 2.1.11 to 2.1.13 compute, except for the
  rounding of the storage cost factors to two decimal places (rule 10). Nodes of these versions and nodes of this
  version therefore compute the same cost factor for all messages, with the exceptions below.
- The rounding changes one value noticeably: `AuthorizedMarketPriceData` (time to live 10 minutes) has 0.1 instead of
  0.10007. The other values change by less than 10^-15. For an `AddAuthenticatedDataRequest` of this type, a node of
  an earlier version expects a difficulty which is at most 0.012 % higher than a node of this version pays. At the
  highest difficulty of 1 048 576 this is about 120, far below the tolerance of 50 000 of the proof of work check. For
  mailbox, remove and refresh requests the sender pays the unrounded value (rule 15).
- Nodes of earlier versions compute the storage cost factor of `AddMailboxRequest`, `RemoveMailboxRequest`,
  `RemoveAuthenticatedDataRequest` and `RefreshAuthenticatedDataRequest` from the deprecated field `metaData` of the
  request, not from the class name. The signature does not cover this field, so a sender or relay can change it.
  - A node of an earlier version which relays a request with changed MetaData pays the cost factor of that MetaData.
    A node of this version expects the value of the policy. The difference in the storage cost factor is at most 0.2
    for the types of this version. With the proof of work, which tolerates a difference of 50 000 at a target
    difficulty of 65 536, the check fails only if the network load multiplied by the difficulty adjustment factor is
    above about 3.8. The receiver then closes the connection to the relay. This ends when the nodes of earlier versions
    have updated.
  - A node of this version sends its registered MetaData in this field, also when it relays a request (see
    [Storage MetaData](storage-metadata.md#compatibility)), and pays at least the cost factor which follows from this
    MetaData (rule 15). So a node of an earlier version accepts the request.

## Known limitations

- The receiver of a mailbox, remove or refresh request cannot read the stored object. The class name is chosen by the
  sender, and the signature does not cover it. So the sender can choose the class name with the lowest storage cost
  factor of the store type.

## Open points

- Which inputs the cost factor of mailbox, remove and refresh requests should have. One option is one value per store
  type instead of one value per class name. This needs a new version of the policy (see
  [Changing a value](#changing-a-value)).
