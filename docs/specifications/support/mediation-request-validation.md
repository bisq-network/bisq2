# Mediation Request Validation

## What a request proves

A Bisq Easy mediation request is signed by the trader who sends it. It carries the trade contract, both traders' profiles and the requester's copy of the trade chat. The contract carries no signatures of the two traders and the chat messages carry no signatures of their authors, so the request proves who sent it and nothing about the contract or the forwarded messages. The mediator UI and the mediator service treat them accordingly.

## Accepted requests

The mediator accepts a request only when:

- the requester is not banned;
- the requester and the peer are the maker and the taker of the contract, in either order, by network ID;
- the contract's mediator is the receiving mediator, and neither trader is the mediator;
- every forwarded message belongs to the trade and its channel, is authored by one of the two traders, has a unique ID within the request, and is of a type a trader's channel holds: text, leave, take offer or protocol log;
- the requester has fewer than ten open cases for other trades, counting cases the requester is on record for as the other trader;
- while the mediator keeps a case of the trade, each trader may request once, whether or not the mediator's channel still exists, and a request must name the same two traders as the trade's cases;
- when the mediator's channel for the trade exists, it must hold the two traders; the channel's offer is not compared.

## Case identity and retained requests

A case is identified by the trade ID and the contract. A request from the other trader for the same contract joins the existing case; a request with a different contract for the same trade opens a second case, so that the mediator sees the conflict rather than the first submitter winning. Request identity includes the requester, so the network layer delivers both traders' requests.

All cases of a trade share the mediator's channel for that trade. The channel's offer is a display copy from the first accepted request, and each case shows its own contract. The mediator UI lists each case of a trade as its own row and acts on the selected row.

Every accepted request is retained on the case before its messages are imported. A repeated request does not replace a retained request or reopen a closed case; the mediator reopens a case from the case list. Channel existence is not mediation state: leaving removes the channel, and the mediator view creates it again for every stored case. A case stored before the other trader's request was retained loads with only the requester's request.

## Provenance of chat messages

A message is imported by its exact content, so both traders' versions of a message under one ID sit next to each other. A message is marked unverified when it reached the mediator only inside a request of a trader who is not its author. The mark is bound to the exact message content and author, not to the message ID, and it is cleared only by a request of the author carrying the same message. Every renderer of a forwarded message shows the mark.

## Not covered

Contract signatures of both traders and signed chat messages are not part of the request. Until they are, accepted contracts and forwarded messages are unauthenticated, the case view shows the contract as submitted, and a spammer can rotate profiles to open more cases. MuSig mediation has none of these checks.

The other trader sends no request when the mediator joins, so a differing contract shows only when both traders requested.

The first accepted request fixes the trade's two traders. A later request for that trade from or naming another profile is rejected, so until contracts are signed, a party can lock its counterparty out of mediation by naming another profile first.

A request sent after the mediator joined carries the mediator's own messages and is rejected as a whole, so a second case can only come from a request sent before the mediator's response arrived.
