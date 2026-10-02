# Validation at parsing

This specification defines where a node checks the integrity rules of an object which it creates from a protobuf
message: a message which it receives from the network, an entry of an inventory response, or an entry which it reads
from disk. The object is created by `fromProto`. The rules for the exception of `fromProto` and its callers are
described in [`../../dev/protobuf-notes.md`](../../dev/protobuf-notes.md#errors-in-fromproto).

A message which a node receives from the network must first pass the size limit of
[`message-framing.md`](message-framing.md). The rules of this specification run after the node has read and parsed the
message, so they cannot limit the memory which these steps need.

## Rules

1. An object checks all its integrity rules in `verify()`. The constructor calls `verify()`, so `fromProto` applies
   the rules when it parses the object. An integrity rule is a rule which the node can decide from the object itself,
   for example:
   - the size and the range of a field, for example the size of a public key or a sequence number of at least 1;
   - the encoding of a field, for example the canonical encoding of an EC public key (see
     [`../security/ec-public-key-encoding.md`](../security/ec-public-key-encoding.md)) or the encoding of a signature;
   - the consistency of two fields, for example the publisher hash and the public key of an add request (see
     [`storage-ownership.md`](storage-ownership.md#the-publisher-hash)), or the publisher of an add request and the
     owner which its data names (see [`storage-ownership.md`](storage-ownership.md#admission));
   - the presence of the signatures which the node requires (see
     [`storage-request-signatures.md`](storage-request-signatures.md#verification)). `verify()` can use the clock of
     the node for this rule, because the required signatures depend on the cutoff date of signature V1.
2. An object which breaks an integrity rule fails at parsing: `fromProto` throws `UnresolvableProtobufException`. The
   node does not create the object, so no later component stores, relays, serves or uses it.
3. A new integrity rule goes into `verify()`, also if nodes of earlier versions accept objects which break the rule.
   Parsing does not have to stay as lenient as in earlier versions.
4. The caller of `fromProto` decides what fails with the object. The caller must know that an entry can be invalid:
   - A requester skips an inventory entry which it cannot parse, and processes the other entries (rule 7 of
     [`inventory-hash-set-filter.md`](inventory-hash-set-filter.md#entries-which-a-node-cannot-parse)).
   - A store skips a persisted entry which it cannot parse when it loads, and keeps the other entries. The exception
     is the store of witness bindings of the oracle node, which must not lose a binding (see
     [`../reputation/account-age.md`](../reputation/account-age.md#persisted-request-compatibility)).
   - A connection closes when the node cannot parse a message which it receives.
5. Checks which need the state of the node, not only the object, stay in the component which owns that state. For
   storage requests these are the stores. Examples: the admission of registered class names and MetaData (see
   [`storage-metadata.md`](storage-metadata.md)), the owner of a stored entry after a refresh, which replaces the
   creation date of the entry (see [`storage-ownership.md`](storage-ownership.md)), the order of sequence numbers (see
   [`storage-request-ordering.md`](storage-request-ordering.md)), and the verification of the signatures against the
   public key, which the store runs after it rejected copies of stored requests.

## Reasons

- Rule 1 and 2: the node rejects an invalid object at the first step which creates it. No later component has to
  handle the invalid object, and each integrity rule has exactly one definition. Without this rule, each new rule
  needs its own check at add, remove and refresh, and when the store loads its entries, and every reader of a parsed
  object has to expect an object which breaks the rules.
- Rule 3: a lenient parser in this version does not make nodes of earlier versions accept or reject anything
  differently. It only moves the check to later steps of this version. The callers already handle objects which they
  cannot parse (rule 4), for example entries of types which a newer version added.
- Rule 5: a rule which depends on the state of the node can have another result on another node, or later on the same
  node. It is not a property of the object, so it does not belong into `verify()`.

## Compatibility

- Nodes of earlier versions accept, store and relay objects which break a rule of this version. When such a node
  serves one in an inventory response, a requester of this version skips the entry. When such a node relays one as a
  broadcast, a node of this version cannot parse the message and closes the connection (rule 4). Nodes of this version
  create only objects which follow the rules, so only a modified client can create such an object.
- A node of an earlier version which cannot parse one entry of an inventory response drops the whole response (see
  [`inventory-hash-set-filter.md`](inventory-hash-set-filter.md#entries-which-a-node-cannot-parse)). A rule of this
  version cannot change this.

## Open points

- **A relayed invalid broadcast closes the connection to the relay.** The relay is usually not the creator of the
  message. A modified client can publish one request which nodes of earlier versions accept and which breaks a rule
  of this version, for example a public key in another encoding. Each node of an earlier version which relays it then
  loses its connections to nodes of this version, until the request expires or the relay stops sending it.
  - What removes it: a connection which drops a broadcast that it cannot parse instead of closing the connection. A
    node would then need another rule for a peer which sends many messages that it cannot parse.
  - Who decides: the developers. The change affects the handling of all messages of a connection, not only storage
    requests.
