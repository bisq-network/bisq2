# Fixed price of an offer

This specification defines the rule which binds the fixed price of an offer to the market of the offer, and the rules
for the price quote of a fixed price. It applies to every offer type (Bisq Easy and MuSig).

## Terms

- The market of an offer names two currencies by their codes: the base currency and the quote currency, for example
  `BTC` and `USD`.
- A fixed price is a price specification which carries a price quote. A price quote has a value and two amounts, the
  base side amount and the quote side amount. Each amount has a currency code.
- The canonical form of an amount is the form which the factories `Coin.fromValue` and `Fiat.fromValue` create for its
  code and value: its type (coin or fiat), its ID (for example `BTC [crypto]` and `USD`), its precision and its low
  precision.

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

## Rules for the price quote

4. At parsing, the price quote of a fixed price must have a positive value and positive amounts, and each amount must
   have the type, the precision and the low precision of its canonical form. This holds for every object which
   carries a fixed price, also for the offer and the agreed price of a stored trade, a trade channel, a mediation case
   and a received mediation request.
5. An offer in an offerbook needs a canonical fixed price: each amount has the ID of its canonical form, and the value
   is derived exactly from the amounts (`FixPriceSpec.verifyCanonical`). This is an integrity rule of the offerbook
   message of Bisq Easy and of the offer message of MuSig: a message which breaks it cannot be created, and a received
   or persisted one fails at parsing. Rule 4 alone applies to the other objects, so that trades and mediation cases
   which earlier versions stored with other fixed prices keep loading.
6. A fixed price which a node of this version creates is canonical: its amounts have their canonical form, and its
   value is derived exactly from them (`PriceQuote.from`). The desktop creates it with the canonical factories. The
   API builds the fixed price of a client's request with `FixPriceSpec.fromClientQuote`: it takes the codes and the
   values of the client's amounts, builds their canonical form, and derives the value. It does not use the value
   which the client sent.

## Reasons

- The amount conversions of an offer take the price from the price specification and the currencies of the amounts
  from the market. With a quote of another market, a conversion returns an amount at the rate of that market, or it
  throws because the quote and the amount are of different kinds, for example a coin amount and a fiat amount. The
  rule removes both cases for every component which uses the offer.
- Rule 4: the precisions bound the cost of the price arithmetic, which grows with a peer-supplied precision. They also
  keep the amount conversions in the units of the currency: an amount of 8 decimals in a conversion of 4 decimals
  shifts every result by a factor of 10,000.
- Rule 5: nodes convert the amounts of a trade with the value of the quote (`PriceQuote.toQuoteSideMonetary`,
  `PriceQuote.toBaseSideMonetary`). Bisq Connect shows the price of an offer from the quote side amount
  (`PriceQuoteFormatter` of bisq-mobile). With a value which does not match the amounts, a modified maker could show
  a taker on Bisq Connect a lower price than the trade uses. The offerbook is where a taker chooses an offer, so the
  rule applies there, for new offers from peers and from the node's own store alike.
- Rule 4 alone applies to stored trades, trade channels, mediation cases and mediation requests, because their offers
  and prices were agreed before. A trade of version 2.1.13 on an offer of Bisq Connect must load, and a trader of
  version 2.1.13 must be able to request mediation for it. No new trade can start from an offer which breaks rule 5:
  a taker of this version does not see it, and a maker of this version creates only canonical offers.
- Rules 4 and 5 do not depend on a date. A date which the data carries is set by its publisher, who could date new
  data back. A date which the node compares with its own clock would only move the loss of stored trades to that
  date, because stored trades and mediation cases do not expire.
- Rule 6: the quote side amount of a client's quote is the price which the user entered. The value is derived from it,
  and Bisq Connect rounds it to 8 significant digits. Deriving the value again keeps the entered price. Rule 6 keeps
  the data which this version creates canonical, also when parsing accepts other forms.
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
- Bisq Connect builds the quote of a fixed price with `PriceQuoteVOFactory.fromPrice` (bisq-mobile `7f9cddce7b`, also
  `connect_0.9.0`). The base side amount has the ID `BTC` instead of `BTC [crypto]`, and the value is the quote side
  amount divided with 8 significant digits, for example 12,345,679,000 for 1,234,567.89 ARS. Its types and precisions
  are canonical. API nodes of version 2.1.13 publish such quotes unchanged, and nodes of version 2.1.13 store them in
  offers, trades, trade channels and mediation cases. Nodes of this version load trades, trade channels and mediation
  cases with such quotes (rule 4) and keep their bytes, so the hash of a contract which both traders signed stays the
  same. They drop offers with such quotes (rule 5): users of this version do not see the fixed-price offers which
  Bisq Connect published through nodes of version 2.1.13, and a maker which upgrades no longer offers them. A new
  fixed price of Bisq Connect on a node of this version is published in the canonical form (rule 6).
- Earlier development builds of this version also checked the ID and the exact value at parsing of every object. They
  could not parse the fixed prices of Bisq Connect, so they skipped trades, trade channels and mediation cases with
  them when they loaded their stores.
- A store skips a persisted entry which it cannot parse (rule 4 of
  [`../network/validation-at-parsing.md`](../network/validation-at-parsing.md)). This includes a persisted trade or
  trade channel whose offer breaks the rule.
