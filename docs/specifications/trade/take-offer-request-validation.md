# Take Offer Request Validation

The maker verifies a take offer request before signing the contract. A trade is created from the taker's contract and stored before this verification, and a rejected trade stays in the store, so nothing downstream may treat the presence of a trade as proof that the maker agreed to it. Only a trade carrying the maker's contract signature proves that.

The maker verifies, in this order:

- the contract offer is one of the maker's own offers: it must equal, by full value equality, an offer in an offerbook message whose author is the offer's maker; if the offer is no longer in the offerbook, a stored trade which carries the maker's contract signature and an equal offer is accepted, so that a second taker of the same offer is not rejected after the first trade was archived;
- the message sender is the taker of the contract and the sender's public key matches the taker's network ID;
- the amounts are within the offer's amount specification and within the maker's amount limits;
- the payment method specifications of the contract are ones the offer lists;
- the contract mediator equals, by network ID, the mediator the maker selects deterministically for the two traders and the offer (`MEDIATORS_NOT_MATCHING` otherwise);
- the taker's contract signature belongs to the taker's public key and verifies over the contract.

The take offer chat message is not part of this verification. The trade chat channel created from it is bound to the signed contract separately, see `chat-channel-binding.md`.
