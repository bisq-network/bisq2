# Identity by key

This specification defines how a node decides whether two values belong to the same user. It applies to Bisq Easy
trades, to the mediation of Bisq Easy trades and to the private chat channels of these trades. The check of the sender of
a trade message also applies to MuSig.

## Terms

- A network ID consists of a public key and an address map. The address map holds one address for each transport, for
  example Tor or I2P, under which the node of the user can be reached.
- The ID of a network ID is the hash of its public key. The ID of a user profile and the ID of an identity are the ID of
  their network ID.
- The address map is routing data. It changes when the user adds a transport: after the next start, the node gives its
  existing identities a network ID with the new address map. Offers, contracts, trades and chat channels keep the network
  ID with which they were created.

## Rules

1. A node decides whether two network IDs, user profiles or identities belong to the same user by their IDs. It does not
   compare whole network IDs, because these include the address map. It does not compare whole user profiles, because a
   user can change the terms and the statement of its profile, and the address map of its network ID can change.
2. Rule 1 applies to these decisions:
   - the maker's own identity for a take offer request is the identity with the ID of the maker's network ID in the
     offer (see [take offer request validation](../trade/take-offer-request-validation.md));
   - the sender of a trade message is the trade peer (see [sender of a trade message](../trade/trade-message-sender.md));
   - an offer in the offerbook is one of the maker's own offers (see
     [take offer request validation](../trade/take-offer-request-validation.md));
   - the requester and the peer of a mediation request are the two parties of the contract, and the receiver of the
     request is the mediator of the contract.
3. A node sends its trade messages under the network ID which its trade peer stored for it. The taker sends under its
   network ID in the contract. The maker sends under the network ID to which the taker addressed the take offer request.
   An honest taker addresses it to the maker's network ID in the offer. The node sends from the identity of the trade,
   whose network ID can have a newer address map.

## Reasons

- The key of a user cannot change, and the layer for confidential messages binds the sender of a message to its key. The
  address map changes when a user adds a transport.
- A comparison of whole network IDs rejects an honest user whose address map differs from the copy which the other node
  holds. For example, the taker stores the maker's network ID from the offer as its trade peer. A comparison of whole
  network IDs drops every trade message which the maker sends under a newer address map.
- The maker's own identity: a maker which added a transport after it published an offer did not find its identity with
  the network ID of the offer. It dropped every take offer request for this offer, and the taker got no answer.
- Rule 3: nodes of version 2.1.13 compare the sender of a trade message with the network ID which they stored for the
  trade peer, including the address map. The handler throws if the two differ, and the state machine then fails the
  trade. Rule 3 keeps such trades working, because the sender is the value which the peer stored.

## Compatibility

- The rules change no message and no persisted data.
- Nodes of version 2.1.13 and earlier compare whole network IDs in the decisions of rule 2. Rule 3 keeps the trade
  messages of a maker whose address map changed acceptable for takers of version 2.1.13.
