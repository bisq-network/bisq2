# Bisq Easy take-offer price policy

## Rules

The maker validates the take-offer contract and its amounts before signing it. The maker does
not require the contract's price specification to equal the original offer's price specification.
Keep this existing behavior for the release.

The amount check resolves the contract's price specification in the offer's market. For a fixed
price, it uses the contract's quote. For a market or floating price, it uses the maker's available
market price. The maker then converts the quote amount into an expected Bitcoin amount.

The configured maximum trade-price deviation limits an unfavorable difference between that
expected amount and the taker's Bitcoin amount. A favorable difference is accepted. A smaller
unfavorable difference may produce a warning.

These checks compare amounts at the contract's price. They do not compare that price against the
original offer's price. The policy can therefore admit a different fixed price or floating margin;
it is not limited to rounding differences between price providers.

## Compatibility

The same absence of price-specification equality checks exists in release 2.1.13. This note
documents the decision to retain that behavior and changes no message, validation, or signature.
Any later change to price equality requires a separate trade-policy decision.

## Related rules

- [Fixed price of an offer](../offer/fixed-price.md) binds the quote's currencies to its market.
- [Sender of a trade message](trade-message-sender.md) defines admission to the trade protocol.
