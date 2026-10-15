# Amounts of a contract

This specification defines the rules for the two amounts of a trade contract. It applies to every contract type (Bisq
Easy and MuSig).

## Terms

- A contract holds the base side amount and the quote side amount of a trade. Each amount is a number of the smallest
  unit of its currency. The currencies are the base currency and the quote currency of the market of the offer.
- The price of a contract is the price quote which is derived from these two amounts.

## Rules

1. The base side amount and the quote side amount of a contract are positive.
2. The price of a contract can be computed from its amounts: the value of the price fits into a signed number of 64
   bits.
3. Both rules are integrity rules of the contract (see rule 1 of
   [`../network/validation-at-parsing.md`](../network/validation-at-parsing.md)). A contract which breaks one of them
   cannot be created. A received or persisted contract which breaks one of them fails at parsing, together with the
   object which contains it, for example a take offer request, a mediation request, a persisted trade or a persisted
   mediation case.

## Reasons

- A trade exchanges an amount of the base currency for an amount of the quote currency. A contract without a positive
  amount on each side describes no trade.
- Components derive the price from the amounts of the contract, for example to display it. For amounts which break
  the rules this derivation throws. Without the rules, every component which uses a contract has to handle this case.
- Before these rules, the node of a mediator accepted and persisted a mediation request whose contract had the base
  side amount 0. The case list of the mediator builds all its items in one step. The item of this case threw, and the
  list showed no case.

## Compatibility

- Nodes up to version 2.1.13 do not check these rules at parsing. In version 2.1.13 and in this version, the Bisq Easy
  take offer process checks that both amounts are positive and match the amounts of the offer: the taker checks them
  before it creates the contract, and the maker checks them when it receives the take offer request. So unmodified
  nodes create only Bisq Easy contracts which follow the rules. MuSig is available only in dev mode.
- A store skips a persisted entry which it cannot parse (rule 4 of
  [`../network/validation-at-parsing.md`](../network/validation-at-parsing.md)). A mediation case with such a contract
  which a mediator accepted with an earlier version is no longer loaded.
