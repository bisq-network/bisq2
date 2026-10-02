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
   | `ConnectionHandshake.Request` | 1 771 bytes | an I2P address of at most 700 characters, at most all transport types and features, an address ownership proof of 64 bytes |
   | `InventoryRequest` with a hash set filter | 3 000 130 bytes | at most 100 000 filter entries of at most 30 bytes each (see [`inventory-hash-set-filter.md`](inventory-hash-set-filter.md#size-of-the-filter)) |
   | `InventoryResponse` | about 2 MB, about 4.1 MB with the token type `HASH_CASH` | the inventory size limit of the responder, 2 000 000 bytes by default; the requester accepts up to twice its own inventory size limit |

   A node creates the authorization token of each handshake request with the token type `HASH_CASH`, because it does
   not know the features of the peer yet. The proof of work of this type contains a copy of the request. A handshake
   response is smaller than the request. Nodes use the token type `HASH_CASH_V2` since version 2.1.3 for all other
   messages. Its proof of work contains a 20 byte hash of the message. For a peer before version 2.1.3, a node still
   uses `HASH_CASH`, and the envelope is about twice as large as the message. An inventory response of the default
   size then has 4 080 153 bytes, measured with entries of 100 bytes. Smaller entries need more bytes for the repeated
   field. Signed storage requests have more than 100 bytes.

   A change which makes a message type larger must keep its largest message below the size limit. Examples are a new
   field of a filter entry, a higher number of filter entries, and a higher default inventory size limit.

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
  receives the message. It creates the parsed objects when all bytes have arrived.
- Rule 5: a node of this version which receives a larger message closes the connection. A failure at the sender shows
  the cause and the type of the message in the log of the sender, and the sender does not waste bandwidth. Only a
  defect or the configuration of the sender can create such a message, for example a high inventory size limit.
- Rule 2, limit of the handshake: the node parses the first message of a peer before it checks anything of the peer:
  the type of the message, the address ownership proof and the proof of work. Any peer can send this message, without
  a key and without a proof of work. A node runs several handshakes at the same time, up to 16 with the default
  configuration of the desktop application. With the limit of an established connection, these handshakes could need
  16 times about 93 MB (see the reasons for rule 4). With 64 KiB, a parse needs at most about 1.4 MB.
- The values of the limits: the largest handshake message of rule 6 has about 1.8 KB. The largest other message is an
  inventory response with the token type `HASH_CASH`, with about 4.1 MB. The limit of an established connection is
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
  an earlier version with an inventory size limit above about 4 100 KB, or above about 2 050 KB for a peer before
  version 2.1.3, sends a full response above the size limit. A node of this version then closes the connection. The
  default limit is 2 000 KB.
- Nodes up to version 2.1.2 use the token type `HASH_CASH`. Its proof of work contains the whole message, so their
  envelopes have about twice the size of the message. An inventory request of such a node, or of a node of this
  version to such a node, is larger than the size limit if its hash set filter has more than about 69 900 entries of
  30 bytes, or about 80 600 entries of 26 bytes. A node of this version then closes the connection, and a sender of
  this version fails before it sends the request (rule 5).

## Known limitations

- **Memory of several parses at the same time.** The size limit bounds the bytes of one message, not the memory of
  its parse. A message at the limit of an established connection with many empty sub-messages needs about 93 MB
  (estimate, see the reasons for rule 4). A peer which completed the handshake can open several connections
  and complete such messages at the same time. A node with a small heap, for example 2 GB, can then run out of memory.
  - Why it is not removed now: the size limits remove the unbounded case, in which one message could exhaust the
    memory, and the limit of the handshake removes the case without a handshake. A lower limit of an established
    connection only reduces the factor, because the largest legal message has about 4.1 MB.
  - What would remove it: a limit of the number of large messages which a node parses at the same time. Because the
    node reads all bytes of a message before it parses them (rule 4), a slow sender does not hold such a limit while
    it sends. Another option is a format of the hash set filter without one sub-message per entry, which needs a new
    filter type. A limit for each message type, which needs a parse in two steps, does not remove it: the inventory
    messages need megabytes and contain the repeated sub-messages.
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
