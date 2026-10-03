# Market prices

This specification defines which market prices a node uses. A market price is the price of one market, for example
BTC in EUR, with its timestamp and its provider.

## Sources

A node gets market prices from three sources:

- **Price node**: the node requests the prices from a price node over HTTP, if price requests are enabled. The
  response contains one entry per market.
- **Oracle data**: an oracle node requests the prices from a price node in the same way, and publishes them in the
  network as `AuthorizedMarketPriceData`. The oracle node signs the data with its authorized key (see
  [Authorization of authorized data](authorized-data.md)). The signature covers all entries of the data.
- **Persisted prices**: the node stores the market prices which it uses, and loads them at startup.

## Rules

1. A node uses only market prices with a positive price: the value of the price quote is above 0.
2. The node checks rule 1 for each price where the price enters the node. It skips only a price which breaks rule 1,
   and keeps the other prices of the same response, oracle data or store:
   - in the response of a price node, when it creates the market price of an entry;
   - in the oracle data, when it applies the prices of the data to the market prices which it uses;
   - in the persisted prices, when it loads them.
3. The node does not change or reject oracle data because one of its prices breaks rule 1. The network storage stores
   and relays the oracle data unchanged.
4. An oracle node publishes only the prices which it accepted from the response of its price node. So an oracle node
   of this version does not publish a price which breaks rule 1.
5. Code which uses the market prices of the node does not need its own check of rule 1.

## Reasons

- Rule 1: a price of zero divides by zero when the node converts an amount with it. A negative price gives negative
  amounts. Example: with a negative price in the offer's currency, the maker's check of the seller's reputation (see
  [Seller reputation limit](../trade/bisq-easy-seller-reputation-limit.md)) computes a negative required score, so a
  taker with score 0 would pass. Price nodes and oracle nodes are trusted sources, so such a price needs a
  faulty or compromised source. The node must still not accept a trade because of it.
- Rule 2: one invalid price must not remove the prices of all other markets.
- Rule 2 and 3, why the oracle data is not checked when the node parses it: the node verifies the signature of the
  oracle node against the data as the node parsed it. If parsing dropped one entry, the parsed data would differ from
  the signed data. The signature check would then fail, and the node would reject all prices of the data. If parsing
  failed instead, the node would also lose all prices of the data. So rule 1 is a rule of the market prices which the
  node uses, and not an integrity rule which the market price checks when it is parsed (rule 1 of
  [Validation at parsing](../network/validation-at-parsing.md)).
- Rule 4: the oracle data reaches all nodes. Skipping an invalid price before publication keeps it out of the network.
- Rule 5: before these rules, some code filtered such prices and other code did not. The maker's check of the seller's
  reputation did not filter them and accepted the taker (rule 1, example).

## Compatibility

- No network message and no persisted data changes.
- Nodes of earlier versions use a price which breaks rule 1. An oracle node of an earlier version publishes such a
  price if its price node returns one. A node of this version skips this price and uses the other prices of the same
  oracle data.
- A persisted price which breaks rule 1, for example from an earlier version, is skipped at startup. The node gets a
  new price for that market from the next response of a price node or from the next oracle data.
