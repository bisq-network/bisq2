# Fixed price of an offer

This specification defines the rule which binds the fixed price of an offer to the market of the offer. It applies to
every offer type (Bisq Easy and MuSig).

## Terms

- The market of an offer names two currencies by their codes: the base currency and the quote currency, for example
  `BTC` and `USD`.
- A fixed price is a price specification which carries a price quote. A price quote has a value and two amounts, the
  base side amount and the quote side amount. Each amount has a currency code.

## Rules

1. The price quote of a fixed price uses the currencies of the market of the offer. The currency code of its base side
   amount equals the base currency code of the market. The currency code of its quote side amount equals the quote
   currency code of the market.
2. This rule is an integrity rule of the offer (see rule 1 of
   [`../network/validation-at-parsing.md`](../network/validation-at-parsing.md)). An offer which breaks it cannot be
   created. A received or persisted offer which breaks it fails at parsing, together with the object which contains
   it, for example the offerbook message.
3. The rule does not apply to a market price and to a floating price. These price specifications carry no quote. Their
   quote is the market price of the market of the offer.

The rules for the price quote in itself (positive value, canonical amounts, a value which matches the amounts) belong
to the fixed price and do not depend on the offer.

## Reasons

- The amount conversions of an offer take the price from the price specification and the currencies of the amounts
  from the market. With a quote of another market, a conversion returns an amount at the rate of that market, or it
  throws because the quote and the amount are of different kinds, for example a coin amount and a fiat amount. The
  rule removes both cases for every component which uses the offer.
- Without the rule, one offer in the offerbook made the check which hides invalid offers throw. This check runs for
  all messages of an offerbook channel in one step, so the offer lists of the desktop and of the API showed no offers
  of that channel.

## Compatibility

- Nodes up to version 2.1.13 do not check this rule. They accept, store and relay an offer which breaks it. A node of
  this version cannot parse such an offer. It skips the entry of an inventory response and drops the broadcast message
  (see [`../network/validation-at-parsing.md`](../network/validation-at-parsing.md#compatibility)).
- Unmodified desktop nodes of this version and of version 2.1.13 create only offers which follow the rule. They derive
  the quote of a fixed price from the selected market or from the market price of that market.
- The API takes the price quote of a fixed price from its client, so the client must send a quote of the market of
  the offer. A request with a quote of another market fails with an error response, and no offer is published.
- A store skips a persisted entry which it cannot parse (rule 4 of
  [`../network/validation-at-parsing.md`](../network/validation-at-parsing.md)). This includes a persisted trade or
  trade channel whose offer breaks the rule.
