# Parties of a Bisq Easy mediation request

This specification defines which requester and peer a mediator accepts in a Bisq Easy mediation request. The request
carries the contract of the trade, the requester, the peer and the chat messages of the trade.

## Rules

1. The requester and the peer of a request are the two parties of the contract in the request: one is the maker and
   the other one is the taker. The mediator compares them by profile ID, which is the hash of the public key.
2. The requester and the peer are two different profiles. A request which names one profile as requester and as peer,
   with a contract which names this profile as maker and as taker, breaks rule 1.
3. The mediator checks rules 1 and 2 before it creates a channel or a case for the request. It ignores a request which
   breaks them and sends no answer. The node of a requester applies the same check before it sends a request (see
   [trade chat channel binding](../trade/chat-channel-binding.md)).

## Reasons

- Rule 1: the address map of a network ID is routing data, which can change after the offer was published, for example
  when a user adds a transport. The profile ID identifies a party by its key.
- Rule 2: a trade has two parties, and the mediator's channel of a case has the requester and the peer as its two
  traders. The mediator does not check the signatures of the contract, so anybody can send a mediator a request whose
  contract names the sender as maker and as taker. Without rule 2, the mediator's node failed with an exception when it
  created the channel. Before each replayed item was processed on its own (see
  [startup replay](../network/startup-replay.md)), this exception also stopped the start of the support services at
  startup, and the other mediation requests of this start were lost.

## Compatibility

- The rules change no message format.
- The desktop application takes only offers of other profiles, so its contracts and requests have two parties.
