# Identity by key

This specification defines how a node decides whether two values belong to the same user. It applies to Bisq Easy
trades, to the mediation of Bisq Easy trades and to the private chat channels of these trades. The check of the sender
of a trade message also applies to MuSig.

## Terms

- A network ID consists of a public key and an address map. The address map holds one address for each transport, for
  example Tor or I2P, under which the node of the user can be reached.
- The ID of a network ID is the hash of its public key. The ID of a user profile and the ID of an identity are the ID of
  their network ID.
- The address map is routing data. It changes when the user adds a transport: after the next start, the node gives
  its existing identities a network ID with the new address map. Offers, contracts, trades and chat channels keep the
  network ID with which they were created.

## Rules

1. A node decides whether two network IDs, user profiles or identities belong to the same user by their IDs. It does not
   compare whole network IDs, because these include the address map. It does not compare whole user profiles, because a
   user can change the terms and the statement of its profile, and the address map of its network ID can change. Rule 4
   names the exceptions.
2. Rule 1 applies to these decisions:
   - the maker's own identity for a take offer request is the identity with the ID of the maker's network ID in the
     offer (see [take offer request validation](../trade/take-offer-request-validation.md));
   - the sender of a trade message is the trade peer (see
     [sender of a trade message](../trade/trade-message-sender.md));
   - an offer in the offerbook is one of the maker's own offers (see
     [take offer request validation](../trade/take-offer-request-validation.md));
   - the requester and the peer of a mediation request are the two parties of the contract, the receiver of the
     request is the mediator of the contract, and neither party has the key of the mediator (see
     [mediation request validation](../support/mediation-request-validation.md));
   - the mediator shows a mediation case only if the mediator of its trade chat channel is the mediator of the
     contract;
   - a user cannot delete a profile while a private chat channel uses the identity of this profile;
   - the node selects the user identity of a chat channel, or another user identity which a caller passes, by its ID.
     It selects its own stored user identity with this ID, also if the caller holds an older copy of it;
   - the desktop shows an offer in the list of own offers, a notification as one of the mediator or of a trader, and
     the number of mediation cases of a trader.
3. A node sends its trade messages under the network ID which its trade peer stored for it. The taker sends under its
   network ID in the contract. The maker sends under the network ID to which the taker addressed the take offer request.
   An honest taker addresses it to the maker's network ID in the offer. The node sends from the identity of the trade,
   whose network ID can have a newer address map.
4. A node compares whole network IDs where another user supplies a copy of a profile to whose addresses the node sends
   messages, and the comparison binds these addresses to the profile which the owner of the key published:
   - The maker compares the mediator of the contract with the mediator which it selects itself by the whole network ID
     (see [take offer request validation](../trade/take-offer-request-validation.md)). On the maker's node, the
     contract and the trade chat channel hold the copy of the mediator's profile which the taker sent. The maker sends
     its mediation request to the addresses of the contract's copy, and its chat messages for the mediator to the
     addresses of the channel's copy.
   - The trade chat channel's copy of the mediator and the contract's copy are compared by the whole network ID (see
     [trade chat channel binding](../trade/chat-channel-binding.md)). Honest clients create both copies from one
     profile.

## Reasons

- The key of a user cannot change, and the layer for confidential messages binds the sender of a message to its key. The
  address map changes when a user adds a transport.
- A comparison of whole network IDs rejects an honest user whose address map differs from the copy which the other node
  holds. For example, the taker stores the maker's network ID from the offer as its trade peer. A comparison of whole
  network IDs drops every trade message which the maker sends under a newer address map.
- The maker's own identity: a maker which added a transport after it published an offer did not find its identity with
  the network ID of the offer. It dropped every take offer request for this offer, and the taker got no answer.
- The mediation case: the mediator's node creates the trade chat channel with the mediator's current profile. The
  contract holds the profile which the taker saw when it took the offer. A comparison of whole profiles hid the case
  after the mediator had changed its terms, its statement or its addresses, so the mediator did not see an honest
  request.
- Deleting a profile: a private chat channel keeps the copy of the user identity with which it was created. Editing the
  terms or the statement replaces the user identity with one of the same key. A comparison of whole user identities did
  not find the channel, so the user could delete an edited profile which an open trade still used, unless the profile
  also had a message in a public channel.
- Selecting a user identity: when the user opens a private chat channel, the node selects the channel's copy of the
  user identity. The channel keeps the copy from before an edit of the profile. A comparison of whole user identities
  did not find this copy, so the node kept the user identity which was selected before, which can be another profile
  than the one of the channel.
- Rule 3: nodes of version 2.1.13 compare the sender of a trade message with the network ID which they stored for the
  trade peer, including the address map. The handler throws if the two differ, and the state machine then fails the
  trade. Rule 3 keeps such trades working, because the sender is the value which the peer stored.
- Rule 4: with a comparison of keys only, a modified taker could put its own address into the copies of the mediator's
  profile. A node sends a message directly over an existing connection to the address. It stores the message in the
  mailbox only when there is no connection or when the direct sending fails. The taker can keep its node connected to
  the maker's node. So the maker's node would send its mediation request and its chat messages for the mediator to the
  taker's node, which cannot decrypt them, and the mediator would not see the case.
- Rule 4 has a cost: the maker rejects an honest take offer request with `MEDIATORS_NOT_MATCHING` if the mediator
  changed its addresses between the moment when the taker built the contract and the moment when the maker checks it.
  Sending to the mediator at the addresses of the latest profile which the mediator published, instead of a copy, would
  remove both the risk and the cost. This is a decision of the developers and not implemented.

## Compatibility

- The rules change no message and no persisted data.
- Nodes of version 2.1.13 and earlier compare whole network IDs or whole profiles in the decisions of rule 2. Rule 3
  keeps the trade messages of a maker whose address map changed acceptable for takers of version 2.1.13.
