# Identity of a contract

This specification defines when two contracts are the same contract. It applies to every contract type (Bisq Easy and
MuSig).

## Rules

1. Two contracts are the same contract only when they name the same taker. The comparison covers all data of the taker
   which the contract carries: the role, the network ID with its addresses and its key, and the commitment to the
   payment account of the taker.
2. The comparison covers the maker through the offer and the other terms of the contract. A contract does not carry the
   maker as separate data: each contract type derives the maker from the offer, and a MuSig contract also from its
   payment method.

## Reasons

- Both traders sign the hash of the contract, and the hash covers the taker. A comparison which ignores the taker treats
  two contracts as the same although the traders signed different data.
- Before these rules, the comparison ignored the taker. When the contracts of the two traders of a trade differed only in
  the data of the taker, the mediator added the request of the second trader to the existing case and did not open a
  second case (see section "Case identity and retained requests" in
  [mediation request validation](../support/mediation-request-validation.md)).

## Compatibility

- A node compares contracts locally. The comparison is not serialized, so messages and persisted data do not change.
- Nodes up to version 2.1.14 ignore the taker when they compare contracts. A mediator with such a version adds a request
  whose contract differs only in the taker to the existing case. A mediator which goes back to such a version loads only
  one of two cases of a trade whose contracts differ only in the taker.
