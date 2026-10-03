# Bisq Easy Contract Price

## Rule

The price specification of a Bisq Easy contract must equal the price specification of the offer.
The maker's node rejects a take offer request whose contract carries another price specification,
before it signs the contract (`BisqEasyPriceSpecValidator`).

## Why the maker checks it

The taker creates the contract, and the maker signs it. The contract's price specification is what
both traders and a mediator see as the agreed price. Before this rule the maker calculated the
Bitcoin amount it expects for the quote amount at a price derived from the contract's price
specification, and rejected the request only if the taker's Bitcoin amount deviated from it to the
maker's disadvantage by more than the maker's maximum trade price deviation. With a price
specification of its own choice, a taker could therefore make the maker sign a contract at any
price, for example one in which the seller sends more Bitcoin for the same fiat amount.

## Equality

Both values come from the taker's message. The maker has already matched the contract's offer
against its own offer by full value equality, so the offer's price specification is the maker's
own, and this rule binds the contract's price specification to it. The comparison uses value
equality: a market price specification equals any other market price specification, a float price
specification equals one with the same percentage, and a fixed price specification equals one
with the same price quote, including its market.

The maker also derives its expected Bitcoin amount from the offer's price specification, so the
price deviation check does not depend on the order of the two checks.

## Compatibility

The rule does not change the protocol or the storage format. The contract keeps its own price
specification field for stored trades and the protobuf format. All current clients put the offer's
price specification into the contract: the desktop client (since version 2.0.3), the REST API and
the Bisq mobile node mode. Only desktop takers older than 2.0.3 could propose another price, and
their requests are rejected. Trades signed before this rule are not affected; for those the
desktop client still asks a buyer who is maker to accept or reject a price specification that
differs from the offer's.
