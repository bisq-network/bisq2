# Take Offer Request Validation

The maker verifies a take offer request before signing the contract. A trade is created from the taker's contract and stored before this verification, and a rejected trade stays in the store, so nothing downstream may treat the presence of a trade as proof that the maker agreed to it. Only a trade carrying the maker's contract signature proves that.

The maker verifies, in this order:

- the sender's user profile is not one the maker ignores (`OFFER_NOT_AVAILABLE` otherwise, which does not tell the taker the reason);
- the contract offer is one of the maker's own offers: it must equal, by full value equality, an offer in an offerbook message whose author is the offer's maker; if the offer is no longer in the offerbook, a stored trade which carries the maker's contract signature and an equal offer is accepted, so that a second taker of the same offer is not rejected after the first trade was archived (`NO_MATCHING_OFFER_FOUND` otherwise);
- the contract's price specification equals the offer's price specification, see `bisq-easy-contract-price.md`;
- the message sender is the taker of the contract and the sender's public key matches the taker's network ID;
- the amounts are positive and within the offer's amount specification (equal to its fixed amount or within its range);
- the Bitcoin amount does not deviate, to the maker's disadvantage, by more than the maker's maximum trade price deviation from the amount the maker calculates for the quote amount at the price it derives itself from the offer's price specification (`PRICE_DEVIATION` otherwise);
- the payment method specifications of the contract are ones the offer lists;
- the contract mediator equals, by network ID, the mediator the maker selects deterministically for the two traders and the offer (`MEDIATORS_NOT_MATCHING` otherwise);
- the taker's contract signature belongs to the taker's public key and verifies over the contract;
- if the taker is the seller, the taker's reputation score allows the trade amount, see `bisq-easy-seller-reputation-limit.md` (`SELLER_REPUTATION_TOO_LOW` otherwise).

The take offer chat message is not part of this verification. The trade chat channel created from it is bound to the signed contract separately, see `chat-channel-binding.md`.
