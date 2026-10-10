# Options of an offer

This specification defines which options an offer can contain and how a node computes the hash serialization of
these options. It applies to Bisq Easy offers and to MuSig offers.

## Terms

- An offer option is an optional element of an offer, for example the trade terms of the maker or the reputation score
  which the maker requires from a taker. An offer has a list of options of different types.
- The hash serialization of an offer is the serialization from which every node computes the storage key, the
  `signatureV1` preimage and the proof of work payload of the offerbook message (see
  [`../network/distributed-data-payload-bytes.md`](../network/distributed-data-payload-bytes.md)).

## Rules

1. A Bisq Easy offer contains only trade terms options and reputation options.
2. A MuSig offer contains only account options and collateral options. It contains exactly one collateral option.
3. An offer sorts its options by their hash code. The hash code of an option and its part of the hash serialization
   leave out every field which nodes of version 2.1.13 do not know. For an account option this is the salted account
   payload hash. The complete serialization contains the field, so `signatureV2` covers it.
4. Rules 1 and 2 are integrity rules of the offer (see rule 1 of
   [`../network/validation-at-parsing.md`](../network/validation-at-parsing.md)). A received or persisted offer which
   breaks them fails at parsing, together with the object which contains it, for example the offerbook message.

## Reasons

- Rules 1 and 2: released clients create only these option types. The desktop and the mobile node create the options
  of a Bisq Easy offer from the trade terms and the required reputation score. The API accepts only these two option
  types. The MuSig offer of the desktop has one account option for each payment method and one collateral option.
- Rules 1 to 3: a node of version 2.1.13 computes the hash serialization of an offer from its own model. If it computes
  other bytes than a node of this version, it rejects the offer although a node of this version accepts it. Then:
  - a node of version 2.1.13 closes the connection to a node of this version which relays the offer to it, because the
    proof of work payload does not match;
  - a node of this version serves the offer in every inventory response to a node of version 2.1.13, which rejects it
    every time, so the offer never enters the inventory filter of that node and fills its inventory responses.

  Anybody can publish an offer, so without these rules one offer would separate the nodes of the two versions.
- Rule 3: the salted account payload hash was added after version 2.1.13. Nodes of version 2.1.13 drop the field, and
  they sort the options by a hash code without the field.
- Rules 1 and 2 also exclude other option types whose hash serialization nodes of version 2.1.13 do not compute
  reliably. For example, version 2.1.13 computes the hash code of a fee option from the object identity of an enum,
  which differs between the processes, so the order of the options differs between nodes.
- Rule 2 (exactly one collateral option): the components which show the offer and the dispute agents use the single
  symmetric security deposit of the offer.

## Compatibility

- No released client creates an offer which breaks rules 1 or 2, so rules 1 and 2 reject no offer of an honest user.
- Nodes up to version 2.1.13 do not check rules 1 and 2. They accept, store and relay such an offer; a node of this
  version rejects it at parsing and keeps the connection to the relaying peer.
- Rule 3 changes the hash serialization of MuSig offers and MuSig contracts of this version. MuSig is available only in
  developer mode, and no version released this hash serialization.
- A new option type for an offer type needs a release in which receivers accept it before a release in which makers
  create it (see the rule for new message types in [`../chat/message-types.md`](../chat/message-types.md)). Its hash
  serialization must not depend on a field which older supported versions do not know.
