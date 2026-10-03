# Message framing

This specification defines how a node separates the messages in the byte stream of a connection, and the size limits
of a message. The framing is the format which marks where a message starts and where it ends.

## Format

1. A node writes each message as a length prefix, followed by the serialized `NetworkEnvelope`. The length prefix is
   the size of the serialized envelope in bytes, encoded as a protobuf varint of at most 5 bytes. This is the format of
   the protobuf methods `writeDelimitedTo` and `parseDelimitedFrom`.

## Size limits

2. The size limit of a message depends on the phase of the connection. It applies to the serialized envelope, without
   the length prefix. Both values are protocol constants. In the following rules, "the size limit" is the limit of the
   current phase.

   | Phase | Messages | Size limit |
   |---|---|---|
   | Handshake | `ConnectionHandshake.Request` and `ConnectionHandshake.Response` | 64 KiB (65 536 bytes) |
   | Established connection | all other messages | 4 MiB (4 194 304 bytes) |

3. A node reads the length prefix of a message before the message. If the length prefix has more than 5 bytes, or if
   the length is larger than the size limit, the node closes the connection. It does not read the message. The node
   decodes all bits of the length prefix, so a larger length never becomes a smaller one. This applies to every message
   of a connection, also to the first message of the handshake.
4. A node reads all bytes of a message before it parses the message.
5. A node does not send a message which is larger than the size limit. The sending fails before the node writes any
   byte of the message. As after any failed send, the node then closes the connection.
6. The limits of the message types keep each message which a node sends below the size limit. The largest messages are:

   | Message | Largest envelope | Limit of the message type |
   |---|---|---|
   | `ConnectionHandshake.Request` | below 1.8 KB | an I2P address of at most 700 characters, at most all transport types and features, an address ownership proof of 64 bytes |
   | `InventoryRequest` with a hash set filter | 3 000 130 bytes | at most 100 000 filter entries of at most 30 bytes each (see [`inventory-hash-set-filter.md`](inventory-hash-set-filter.md#size-of-the-filter)) |
   | `InventoryResponse` | about 2 MB with `HASH_CASH_V2` | the inventory size limit of the responder, 2 000 000 bytes by default; the requester accepts up to twice its own inventory size limit |

   A node uses `HASH_CASH_V2` for initial handshake requests. Its proof of work contains a
   20 byte hash of the message, and the largest envelope of a handshake request has about 950 bytes. A node of a
   release up to 2.1.13 uses the legacy `HASH_CASH` method for its handshake request. The proof of work of this
   method contains a copy of the request, and the largest envelope is the one of the table, with about 1 770 bytes.
   Legacy `HASH_CASH` tokens are rejected for all other messages; see
   [Authorization tokens](authorization-tokens.md#handshake-requests-of-earlier-versions).

   A change which makes a message type larger must keep its largest message below the size limit. Examples are a new
   field of a filter entry, a higher number of filter entries, and a higher default inventory size limit.

## Reading a message

7. A node reads exactly the bytes of the length prefix and of the message. It reads no byte of the next message.
8. Only the length prefix marks the end of a message. The protobuf format has no end marker for a message. If the
   stream ends before the first byte of a length prefix, the connection ends normally. If it ends inside a length
   prefix or inside a message, the read fails, also if the received bytes form a valid message.
9. The bytes of a message must be a valid protobuf message: a field must not be longer than the rest of the message,
   and each end-group tag needs a matching start-group tag. The node uses the nesting limit of protobuf-java, 100
   levels. It does not set a lower limit.
10. All bytes of a message must arrive within 4 minutes after its length prefix. Otherwise the node closes the
    connection. The node checks the time after each read, including the read which completes the message. A message
    completed after the deadline is rejected. The connection closes at the latest one socket timeout after these
    4 minutes.
11. A node uses two independent parse budgets, each shared by all inbound and outbound connections of its phase:

    | Phase | Shared budget | Messages counted |
    |---|---|---|
    | Handshake | 128 KiB (131 072 bytes) | every message |
    | Established connection | 8 MiB (8 388 608 bytes) | messages of at least 64 KiB |

    Each budget counts the serialized bytes of messages being parsed and converted into objects of the node. A
    message waits until its budget has room for it. Reading the message, authorization and message handling do not
    hold this budget. A failed parse or conversion must discard partial protobuf messages before releasing the
    budget. Failure diagnostics must not retain these messages through exceptions or their causes. The same
    requirement applies to smaller established-connection messages, which do not use a budget.

## Reasons

- Rule 3: the node creates the message from the received bytes before it checks the proof of work and the other rules
  of the message. Without a size limit, protobuf accepts a length of up to 2^31 - 1 bytes. A peer could make the node
  read and parse a message of almost 2 GiB. It needs no proof of work for this, and it can do it before the handshake.
  The parsed objects need more memory than the bytes of the message. One such message can exhaust the memory of the
  node.
- Rule 3, closing the connection: a node which follows this specification never sends a larger message (rules 5 and
  6). A larger length prefix means a broken framing or a modified client. To read the next message, the node would have
  to read and discard all bytes of the larger message first. Closing the connection is simpler. For the relay of such a
  message by a node of an earlier version, see [Known limitations](#known-limitations).
- Rule 3, decoding all bits: a decoder which keeps only the lower 32 bits of the length prefix, as the protobuf method
  `readRawVarint32` does, turns a length of 2^32 into 0. The node would then accept the prefix and read the following
  bytes as the next message. A node which follows this specification never writes such a prefix.
- Rule 4: the parsed objects of a message can need many times more memory than its bytes. A measurement with
  protobuf-java 4.28.2 on one machine showed these values:

  | Message of | Bytes | Memory of the parsed objects | Parse time |
  |---|---|---|---|
  | a hash set filter of 199 999 entries, without authorization token | 6 000 003 | 21.7 MB, 3.6 times the bytes | 50 ms |
  | 5.2 million empty filter entries | 10 485 729 | 232 MB, 22 times the bytes | 218 ms |

  The measurement used the earlier limits of 199 999 filter entries and 10 MiB. For a message of 4 MiB, a linear
  estimate gives about 93 MB with empty filter entries. This value was not measured.

  A parser which reads from the stream creates the objects while the bytes arrive. A peer which sends a message slowly
  would make the node hold the parsed objects for that time. With rule 4, the node holds only the bytes while it
  receives the message. It creates the parsed objects when all bytes have arrived. The buffer for the bytes grows
  while they arrive, so a length prefix alone does not make the node allocate the announced size.

  While a message arrives, the read thread of its connection waits. The socket timeout of the transport applies to each
  single read, not to the whole message: 120 seconds in the default configuration. A peer which sends slowly holds
  only the bytes which it has sent. Rule 10 limits how long it can keep the read thread and the connection.
- Rule 5: a node of this version which receives a larger message closes the connection. A failure at the sender shows
  the cause and the type of the message in the log of the sender, and the sender does not waste bandwidth. Only a
  defect or the configuration of the sender can create such a message, for example a high inventory size limit.
- Rule 7: the handshake and the connection read the same input stream of the socket, each with its own reader. A byte
  of the next message which the reader of the handshake took in advance would be missing for the connection, and the
  connection would read the following bytes at the wrong position. A node therefore reads the length prefix one byte
  at a time.
- Rule 8: a stream which ends inside a message can still contain a valid protobuf message, for example when it ends at
  a field boundary. The node would then accept a message which the peer did not complete.
- Rule 9, nesting limit: the deepest valid message has 9 levels below `NetworkEnvelope`: `EnvelopePayloadMessage`,
  `InventoryResponse`, `Inventory`, `DataRequest`, `AddMailboxRequest`, `MailboxSequentialData`, `MailboxData`,
  `ConfidentialMessage`, `ConfidentialData`. The schema has no cycles. Data of other modules is inside `Any` or inside
  encrypted bytes, which this parse does not decode. A lower limit would not reduce the memory of a parse: the memory
  grows with the number of sub-messages, and the worst case uses many entries of a repeated field at 5 levels
  (`HashSetFilterEntry`), not a deep nesting. 100 levels need only a small part of the stack of the read thread. A
  lower limit would be a new protocol rule, and a later message type with more levels would close connections.
- Rule 10: a peer which sends one byte shortly before each socket timeout would otherwise keep the read thread and the
  connection without limit. 4 MiB over a slow Tor circuit of 20 KB per second need about 210 seconds, so the deadline
  does not close honest connections. A read which waits cannot be interrupted without closing the socket from another
  thread. The check after each read needs no further thread.
- Rule 11: the size limit bounds the parse memory of one message, not of several connections which parse large
  messages at the same time. The established-connection budget bounds simultaneous large-message parsing at about
  2 times 93 MB (estimate, see the reasons for rule 4). A slow sender does not hold a budget, because the node reads
  all bytes of a message before it parses them (rule 4). The budget covers the parse and the conversion into the objects of the
  node, because only then are the parsed protobuf objects no longer used. It does not cover the handling of the
  message: the handling can wait for a response on another connection, whose read thread can wait for the budget.
  Established-connection messages below 64 KiB need little memory and do not wait for large ones.
  The separate handshake budget allows two messages at the handshake size limit to be parsed together, or more
  smaller messages. It counts every handshake message: otherwise an unauthenticated peer could bypass the budget
  by sending just below the accounting threshold. Its estimated parse memory is about 2.8 MB, using the same
  amplification estimate as rule 4. A separate budget prevents large established-connection messages from delaying
  handshakes. Under handshake load, completed messages may wait briefly before parsing.
- Rule 2, limit of the handshake: the node parses the first message of a peer before it checks anything of the peer:
  the type of the message, the address ownership proof and the proof of work. Any peer can send this message, without
  a key and without a proof of work. A node runs several handshakes at the same time, up to 16 with the default
  configuration of the desktop application. With the limit of an established connection, these handshakes could need
  16 times about 93 MB (see the reasons for rule 4). With 64 KiB, one parse needs about 1.4 MB by the same estimate.
  The separate budget of rule 11 also bounds their combined parsing, independently of the handshake thread count.
- The values of the limits: the largest handshake message of rule 6 is below 1.8 KB. The largest other message is an
  inventory request with a full hash set filter, with about 3.0 MB. The limit of an established connection is
  small, because the memory of a parse grows with the size of a message (see the reasons for rule 4), and because the
  largest message types are known. The limit of the filter entries keeps the largest inventory request at 3.0 MB. An
  increase of a limit needs all nodes to update first (see [Compatibility](#compatibility)).

## Compatibility

- Nodes of earlier versions read messages of any size. A node of this version sends them no message above the size
  limit (rule 5), so the limits change nothing for them.
- The handshake messages of nodes of earlier versions have the limits of rule 6, so they are below the limit of the
  handshake.
- Nodes of earlier versions accept hash set filters with fewer than 200 000 entries. A node of this version rejects a
  filter with more than 100 000 entries (see
  [`inventory-hash-set-filter.md`](inventory-hash-set-filter.md#size-of-the-filter)), and the request of such a filter
  can also exceed the size limit. Both need a node of an earlier version with more than 100 000 stored requests. A
  filter of a node had 38 280 entries on 2026-10-02.
- Nodes of earlier versions accept an inventory response of up to twice their own inventory size limit. A responder of
  an earlier version with an inventory size limit above about 4 100 KB sends a full response above the size limit.
  A node of this version then closes the connection. The default limit is 2 000 KB.
- Legacy `HASH_CASH` authorization is rejected, except for the handshake request of a node of a release up to
  2.1.13. See [Authorization tokens](authorization-tokens.md#compatibility).

## Known limitations

- **Memory of the parse of one message.** The budget of rule 11 bounds the parse memory of all connections together,
  at about 2 times 93 MB (estimate). One message at the size limit with many empty sub-messages still needs about
  93 MB. The parsed objects are a temporary peak: the bytes of a message are garbage after the parse, and the parsed
  protobuf objects after the conversion. Only the objects of accepted data stay, because the node stores the data.
  Under an attack with large messages, honest inventory messages wait for the budget. A parse takes about 50 ms.
  - Why it is not removed now: a lower size limit only reduces the factor, because the largest legal message has
    about 4.1 MB.
  - What would remove it: a format of the hash set filter without one sub-message per entry, which needs a new filter
    type. A limit for each message type, which needs a parse in two steps, does not remove it: the inventory messages
    need megabytes and contain the repeated sub-messages.
  - Who decides: the developers.
- **A relayed message above the size limit closes the connection to the relay.** Nodes relay broadcasts of other
  nodes, for example storage requests. The broadcasts which nodes create are far below the size limit, so only a
  modified client can create a larger one. If nodes of an earlier version accept and relay it, a node of this version
  closes its connection to each of these relays. Whether nodes of earlier versions accept such a message depends on
  the field limits of each type of stored data.
  - Why it is accepted now: it needs a modified client, and a type of stored data whose fields nodes of earlier
    versions accept with more than 4 MiB in total. It is not known whether such a type exists. The field limits of
    the types of earlier versions were not checked for this.
  - What would remove it: a node which reads and discards a message above the size limit, instead of closing the
    connection. It would read the bytes of the message, but it would not hold them.
  - Who decides: the developers.
