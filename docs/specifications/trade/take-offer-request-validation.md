# Take Offer Request Validation

The maker verifies a take offer request before it creates the trade and signs the contract. A rejected request leaves no trade and does not add the taker to the maker's contacts, and the maker reports the rejection to the taker. Earlier versions stored the trade before this verification, so a stored trade of a rejected request can remain, and nothing downstream may treat the presence of a trade as proof that the maker agreed to it. Only a trade carrying the maker's contract signature proves that.

Before this verification, the maker checks that the request can create a trade of its own. It drops the request when one of these conditions fails. A dropped request creates no trade, and the maker sends nothing to the taker. The maker logs the message type and the trade ID, and no content of the request.

- The request is addressed to the key of the offer's maker: the ID of the receiver's network ID is the ID of the maker's network ID in the offer. The layer for confidential messages binds the receiver's key to the key with which the maker decrypted the request.
- One of the maker's identities (the default identity, an active or a retired identity) has this key. The maker finds it by the ID of the key, not by the whole network ID: the addresses of the maker's network ID can have changed since it published the offer, see [identity by key](../user/identity-by-key.md). The trade uses this identity, and the maker sends its trade messages under the network ID to which the taker addressed the request.
- The trade ID of the request is the ID of the trade which the maker creates from it (see `trade-id.md`). A report of a rejection would otherwise carry the ID of a trade which the taker does not have.
- No trade and no protocol with the trade ID exist. The check, the verification and the creation of the trade are one step, because the maker can process two requests at the same time. The maker checks this before it verifies the request: a rejection of a copy of a request which it has handled would fail the trade at the taker.

The sender of the request controls these conditions. So the maker drops a request which fails them, and it does not throw an exception: at startup an exception would also end the processing of the other messages which arrived before the start. An honest request fails these conditions only when it is delivered a second time, for example when a mailbox message is processed again after the maker processed the first delivery.

The maker verifies, in this order:

- the sender's user profile is not one the maker ignores (`NO_MATCHING_OFFER_FOUND` otherwise: the maker rejects an ignored taker with the same failure, text and throw site as a request for an offer which it does not have, so the rejection does not tell the taker the reason);
- the contract offer is one of the maker's own offers: it must equal, by full value equality, an offer in an offerbook message whose author is the offer's maker and whose maker network ID has the key of the trade's identity (the addresses can differ); if the offer is no longer in the offerbook, a stored trade which carries the maker's contract signature and an equal offer is accepted, so that a second taker of the same offer is not rejected after the first trade was archived (`NO_MATCHING_OFFER_FOUND` otherwise);
- the contract's price specification equals the offer's price specification, see `bisq-easy-contract-price.md`;
- the message sender is the taker of the contract and the sender's public key matches the taker's network ID;
- the amounts are positive and within the offer's amount specification (equal to its fixed amount or within its range);
- the Bitcoin amount does not deviate, to the maker's disadvantage, by more than the maker's maximum trade price deviation from the amount the maker calculates for the quote amount at the price it derives itself from the offer's price specification (`PRICE_DEVIATION` otherwise);
- the payment method specifications of the contract are ones the offer lists;
- the contract mediator equals, by its whole network ID including the addresses, the mediator the maker selects deterministically for the two traders and the offer (`MEDIATORS_NOT_MATCHING` otherwise). The maker sends its mediation request and its chat messages for the mediator to the addresses of the copies of the mediator's profile which the taker supplies in the contract and in its first chat message. This check binds these addresses to the profile which the mediator published, see rule 4 of [identity by key](../user/identity-by-key.md);
- the taker's contract signature belongs to the taker's public key and verifies over the contract;
- if the taker is the seller, the taker's reputation score allows the trade amount, see `bisq-easy-seller-reputation-limit.md` (`SELLER_REPUTATION_TOO_LOW` otherwise).

The take offer chat message is not part of this verification. The trade chat channel created from it is bound to the signed contract separately, see `chat-channel-binding.md`.
