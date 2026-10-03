# Validation at parsing

This specification defines where a node checks the integrity rules of an object which it creates from a protobuf
message: a message which it receives from the network, an entry of an inventory response, or an entry which it reads
from disk. The object is created by `fromProto`. The rules for the exception of `fromProto` and its callers are
described in [`../../dev/protobuf-notes.md`](../../dev/protobuf-notes.md#errors-in-fromproto).

A message which a node receives from the network must first pass the size limit of
[`message-framing.md`](message-framing.md). The rules of this specification run after the node has read and parsed the
message, so they cannot limit the memory which these steps need.

## Rules

1. An object checks all its integrity rules in `verify()`. Each constructor which completes an object calls `verify()`
   after it set all fields, so `fromProto` applies the rules when it parses the object. A subclass which overrides
   `verify()` calls `verify()` of its super class. A class whose constructor calls `verify()` is final. Otherwise the
   constructor would run the `verify()` of a subclass before the subclass set its fields. An integrity rule is a rule
   which the node can decide from the object itself and cheaply, for example:
   - the size and the range of a field, for example the size of a public key or a sequence number of at least 1;
   - the encoding of a field, for example the canonical encoding of an EC public key (see
     [`../security/ec-public-key-encoding.md`](../security/ec-public-key-encoding.md)) or the encoding of a signature;
   - the consistency of two fields, for example the publisher hash and the public key of an add request (see
     [`storage-ownership.md`](storage-ownership.md#the-publisher-hash)), or the publisher of an add request and the
     owner which its data names (see [`storage-ownership.md`](storage-ownership.md#admission));
   - the type of a field, for example the payload of an envelope, which must not be a message that is valid only
     inside a confidential message (see [`confidential-messages.md`](confidential-messages.md));
   - the presence of a signature, and of signature V1 before the cutoff date of signature V1 (see
     [`storage-request-signatures.md`](storage-request-signatures.md#verification)). `verify()` uses the clock of
     the node for this rule. `verify()` does not require signature V2: a constructor also completes requests which
     the node did not receive, for example the request which a creator builds to compute the preimage of signature
     V2, and a persisted request which a store loads. The stores require signature V2 (rule 5).
2. An object which breaks an integrity rule fails at parsing: `fromProto` throws `UnresolvableProtobufException`. The
   node does not create the object, so no later component stores, relays, serves or uses it.
3. A new integrity rule goes into `verify()`, also if nodes of earlier versions accept objects which break the rule.
   Parsing does not have to stay as lenient as in earlier versions. The only exception is a rule which unmodified
   nodes of a supported earlier version break with the objects which they create. Such a rule stays at the later
   step until these versions are no longer supported, because a node of this version drops a message which it cannot
   parse (rule 6). Example: a refresh request needs signature V2 also before the cutoff date of
   signature V1, but nodes up to version 2.1.13 send refresh requests with signature V1 only (see
   [`storage-request-signatures.md`](storage-request-signatures.md#refresh-requests)). The store rejects them. From
   the cutoff date on, the stores reject every request without signature V2 (rule 5).
4. The caller of `fromProto` decides what fails with the object. The caller must know that an entry can be invalid:
   - A requester skips an inventory entry which it cannot parse, and processes the other entries (rule 7 of
     [`inventory-hash-set-filter.md`](inventory-hash-set-filter.md#entries-which-a-node-cannot-parse)).
   - A store skips a persisted entry which it cannot parse when it loads, and keeps the other entries. The exception
     is the store of witness bindings of the oracle node, which must not lose a binding (see
     [`../reputation/account-age.md`](../reputation/account-age.md#persisted-request-compatibility)).
   - A connection drops a message which the node cannot parse, and keeps the connection open (rule 6).
   - A mediation request, an arbitration request and a message with the data of a dispute case skip a chat message
     which the node cannot parse, and keep the other chat messages. The trade peers created these chat messages. A
     node of an earlier version does not check the rules of this version when it receives a chat message, and it puts
     all chat messages of the trade into its request. The node logs the type and the size of a skipped chat message,
     and no received values.
5. Checks which need the state of the node, not only the object, stay in the component which owns that state. For
   storage requests these are the stores. Examples: the admission of registered class names and MetaData (see
   [`storage-metadata.md`](storage-metadata.md)), the owner of a stored entry after a refresh, which replaces the
   creation date of the entry (see [`storage-ownership.md`](storage-ownership.md)), the order of sequence numbers (see
   [`storage-request-ordering.md`](storage-request-ordering.md)), the expiry of a request and the requirement of
   signature V2 from the cutoff date of signature V1 on, which depend on the time of the node (see
   [`storage-request-signatures.md`](storage-request-signatures.md#cutoff-date-of-signature-v1)), and the
   authorization of authorized data with the keys of the configuration. Expensive checks also stay
   at the later step: the verification of signatures against the public key, which the store runs after it rejected
   copies of stored requests, and the verification of a proof of work.

6. A connection closes if a message breaks a rule of [`message-framing.md`](message-framing.md), if the bytes of a
   message are not a valid protobuf `NetworkEnvelope`, if the network version differs, or if the input stream ends. A
   message which protobuf can parse, but which `fromProto` cannot parse, does not close the connection. The connection
   throttles such a message like every other message, and then drops it. The node logs at most three such messages per
   connection with their type and size, and no received values.
7. The message of an exception which an integrity rule throws names the rule, the limit and the size of the rejected
   value. It does not contain the rejected value. The callers log these messages, for example when a confidential
   message fails at parsing, and the rejected values come from other nodes: chat texts, payment account data, Bitcoin
   addresses, transaction IDs. Users share their log files with support.

## Reasons

- Rule 1 and 2: the node rejects an invalid object at the first step which creates it. No later component has to
  handle the invalid object, and each integrity rule has exactly one definition. Without this rule, each new rule
  needs its own check at add, remove and refresh, and when the store loads its entries, and every reader of a parsed
  object has to expect an object which breaks the rules.
- Rule 3: a lenient parser in this version does not make nodes of earlier versions accept or reject anything
  differently. It only moves the check to later steps of this version. The callers already handle objects which they
  cannot parse (rule 4), for example entries of types which a newer version added. The exception keeps the messages
  of unmodified nodes of supported earlier versions: a node of this version would otherwise drop each message which
  they create as their version defines it.
- Rule 5: a rule which depends on the state of the node can have another result on another node, or later on the same
  node. It is not a property of the object, so it does not belong into `verify()`.
- Rule 6: the peer is often only a relay of the message, for example a node of an earlier version which accepts a
  request that breaks a rule of this version. If the node closed the connection, a modified client could publish one
  such request and disconnect every honest relay of an earlier version from the nodes of this version, until the
  request expires. A message type which a newer version adds would have the same effect between versions. A relay
  cannot send bytes which are not a valid protobuf message: it parses each message before it relays it, and it sends
  the serialization of the parsed objects. Only the peer itself creates such bytes, so the connection closes.
- Rule 6 and the cost of such messages: the connection throttles every received message after it parses it, also a
  message which it drops. It reads the next message only after the throttle, so a peer can send such messages only at
  the throttled rate. The node parses every message before it checks the proof of
  work, so a message which fails parsing costs the node the same parse work as a message with an invalid proof of
  work. Checks before expensive parse steps limit this work (for example rule 5 of
  [`../security/ec-public-key-encoding.md`](../security/ec-public-key-encoding.md)). The difference is that the peer
  keeps the connection (see [Known limitations](#known-limitations)).

## Compatibility

- Nodes of earlier versions accept, store and relay objects which break a rule of this version. When such a node
  serves one in an inventory response, a requester of this version skips the entry. When such a node relays one as a
  broadcast, a node of this version drops the message and keeps the connection (rule 6). Nodes of this version
  create only objects which follow the rules. Apart from the exception of rule 3, unmodified nodes of earlier versions
  do so as well, so only a modified client can create such an object.
- An unmodified node of an earlier version can still pass on such an object inside an object which it creates itself.
  Example: a trade peer with a modified client sends a chat message which breaks a rule of this version, and the node
  of version 2.1.13 puts it into its mediation request. If the request failed as a whole, the mediator of this
  version would not receive the request of an honest user. So the request skips the chat message (rule 4).
- A node of an earlier version which cannot parse one entry of an inventory response drops the whole response (see
  [`inventory-hash-set-filter.md`](inventory-hash-set-filter.md#entries-which-a-node-cannot-parse)). A rule of this
  version cannot change this.

## Known limitations

- **A peer can send messages which the node cannot parse, without proof of work, and keep the connection.** The rate
  is limited by the receive throttle of the connection. A peer which sends only valid messages with an invalid proof of
  work loses the connection, but it can connect again.
  - Why it is accepted: a limit for such messages per connection would close connections to honest relays again, which
    rule 6 prevents.
  - What would remove it: a proof of work check before the payload is parsed. The proof of work covers the hash
    serialization of the parsed message, so this needs another design of the proof of work payload.
  - Who decides: the developers.
