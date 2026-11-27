# Payment methods of an offer

This specification defines the rule which binds the payment methods of an offer to the currencies of the market of
the offer. It applies to every offer type (Bisq Easy and MuSig).

## Terms

- The market of an offer names two currencies by their codes: the base currency and the quote currency, for example
  `BTC` and `USD` (see [`fixed-price.md`](fixed-price.md#terms)).
- An offer has a list of payment methods for each side. The payment methods of the base side transfer the base
  currency, and the payment methods of the quote side transfer the quote currency. In a Bitcoin market, for example
  `BTC/USD`, the base side has the Bitcoin settlement methods and the quote side the fiat payment methods. In a crypto
  market, for example `XMR/BTC`, the base side has the payment method of the crypto asset and the quote side the
  Bitcoin settlement method.
- A payment method supports a list of currencies. A payment rail defines this list, for example `EUR` for SEPA, `USD`
  for Zelle, and several currencies for Revolut. The Bitcoin settlement methods support `BTC`. The payment method of
  a crypto asset supports this asset.
- Some fiat payment rails support all fiat currencies, for example national bank transfers, face to face trades and
  custom payment methods. A custom payment method is a method which the maker names. The maker's name describes it,
  so no list of currencies applies to it.

## Rules

1. Each payment method of the base side of an offer supports the base currency of the market. Each payment method of
   the quote side supports the quote currency of the market. A payment rail which supports all fiat currencies
   supports every quote currency, also one which the currency data of the node does not contain.
2. Rule 1 is an integrity rule of the offerbook message of Bisq Easy and of the offer message of MuSig (see rule 1 of
   [`../network/validation-at-parsing.md`](../network/validation-at-parsing.md)). A message which breaks it cannot be
   created, and a received or persisted one fails at parsing. The API returns an error response for a request to
   create such an offer, and no offer is published.
3. Rule 1 is not an integrity rule of the offer itself. A stored trade, a trade channel, a mediation case and a
   mediation request with an offer which breaks rule 1 keep loading.
4. A custom payment method must not have the name of a payment rail. Only the name of a payment method is part of the
   offer. A receiver resolves a name which equals the name of a payment rail to that payment rail. The desktop rejects
   such a name when the maker adds a custom payment method, for every market.

## Reasons

- Rule 1: the taker selects one of the payment methods of the offer, and the traders then pay with it. A SEPA payment
  in USD, for example, is not possible. The reporter of issue #5035 saw SEPA in the `USD` market in the trades of an
  attack. Without rule 1, such an offer is shown in the offerbook like a valid offer.
- Rule 1, payment rails of all fiat currencies: a node derives the list of all fiat currencies from the currency data
  of its JDK. This data differs between JDK versions, for example when a country introduces a new currency. A node
  with other currency data must not drop a national bank transfer offer of a market which it does not know.
- Rule 2: the offerbook is where a taker chooses an offer, so the rule applies there, for new offers from peers and
  from the node's own store alike. It is the same place as rule 5 of [`fixed-price.md`](fixed-price.md).
- Rule 3: the lists of currencies of the payment rails change between versions. For example, version 2.1.9 removed
  `BGN` from Revolut and Wise. A trade which the traders agreed on with an earlier list must keep loading, because
  stored trades and mediation cases do not expire.
- Rule 4: the desktop of version 2.1.15 compares a custom name only with the payment rails of the selected market. A
  maker in the `USD` market can add a custom payment method `SEPA`. The receivers of the offer, and the maker's own
  node after a restart, resolve it to the SEPA payment rail, so the offer breaks rule 1.

## Compatibility

- Nodes up to version 2.1.15 do not check rule 1. They accept, store and relay an offerbook message which breaks it. A
  node of this version cannot parse such a message. It skips the entry of an inventory response and drops the
  broadcast message (see [`../network/validation-at-parsing.md`](../network/validation-at-parsing.md#compatibility)).
- Unmodified desktop nodes of earlier versions offer only payment methods of the selected market, with one exception:
  a custom payment method with the name of a payment rail of another currency (rule 4). Nodes of this version drop
  such an offer. Its maker does not see the offer in the offerbook after an update to this version, and has to create
  the offer again with another name. This is the exception of rule 3 of
  [`../network/validation-at-parsing.md`](../network/validation-at-parsing.md): unmodified nodes of an earlier
  version can create objects which break rule 1. Such offers need an exact name match with a payment rail of another
  currency, for example `SEPA` in the `USD` market, so they are rare.
- Bisq Connect sends the names of the payment methods to the API. A name of a payment rail which does not support the
  currency of the market fails with an error response.
- Offers of earlier versions with a national bank transfer in `EUR` follow rule 1, because national bank transfers
  support all fiat currencies. The desktop no longer offers this payment rail for `EUR`.
