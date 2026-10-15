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
their requests are rejected. Trades signed before this rule keep their contract, but the desktop
client no longer asks a buyer who is maker to accept or reject a price specification that differs
from the offer's.

## The contract field

The contract's price specification field is deprecated. New code uses the price specification of
the offer, and the taker puts it into the contract (`BisqEasyTradeService.takerCreatesProtocol`).
The field stays and is still written: the hash which both traders sign covers it, nodes up to
2.1.13 read it when they verify a take offer request, and a contract without it fails parsing. The
maker's check of this rule reads it, and the REST API maps it into `BisqEasyContractDto`.

A later version can stop writing the field when all supported versions accept a contract without
it. Before that, a version has to parse a missing field as missing and must not fill it in from
the offer, because a filled-in value changes the signed hash. The switch then needs an activation
date (see `docs/dev/backward-compatibility.md`).
