# Mediation Request Validation

## What a request proves

A Bisq Easy mediation request is signed by the trader who sends it. It carries the trade contract, both traders' profiles and the requester's copy of the trade chat. The contract carries no signatures of the two traders and the chat messages carry no signatures of their authors, so the request proves who sent it and nothing about the contract or the forwarded messages. The mediator UI and the mediator service treat them accordingly.

## Requester

A trader's node puts its trade channel in mediation when it sends a request, so that its messages go to the mediator as well, whether or not a response of the mediator arrives. The desktop and the API send the request through the same service.

## Accepted requests

The mediator accepts a request only when:

- the requester is not banned;
- the requester and the peer are the two parties of the contract, see [parties of a mediation request](mediation-request-parties.md);
- the contract's mediator is the receiving mediator, and neither trader has the mediator's key;
- the requester has fewer than ten open cases for other trades, counting cases the requester is on record for as the other trader;
- while the mediator keeps a case of the trade, each trader may request once, whether or not the mediator's channel still exists, and a request must name the same two traders as the trade's cases.

The checks do not look at the mediator's channel of the trade. The chat service decides which channel the mediator uses, see the section "Channel on the mediator's node" in [trade chat channel binding](../trade/chat-channel-binding.md). When the chat service keeps the channel of another identity of the mediator's node, the mediator ignores the request.

## Forwarded messages

The mediator imports a forwarded message only when it belongs to the trade and its channel, is authored by one of the two traders, and is of a type a trader's channel holds: text, leave, take offer or protocol log. It skips any other forwarded message and still accepts the request. A trader's channel can hold messages which the requester did not choose: the peer chooses the trade ID and the ID of its messages, and once the mediator joined, the channel holds the mediator's messages. If the mediator ignored a request with such a message, the peer could block the mediation of the trade with one message. A skipped message is not imported and does not count for the unverified mark.

Forwarded messages can share an ID, for example when the peer sent a message under the ID of a message of the requester. Each of them is imported and marked by its exact content (see Provenance of chat messages). If the mediator skipped messages with a shared ID, the peer could remove a message of the requester from the request.

## Case identity and retained requests

A case is identified by the trade ID and the contract. A request from the other trader for the same contract joins the existing case; a request with a different contract for the same trade opens a second case, so that the mediator sees the conflict rather than the first submitter winning. Request identity includes the requester, so the network layer delivers both traders' requests.

All cases of a trade share the mediator's channel for that trade. The channel's offer is a display copy from the first accepted request, and each case shows its own contract. The mediator UI lists each case of a trade as its own row and acts on the selected row.

Every accepted request is retained on the case before its messages are imported. A repeated request does not replace a retained request or reopen a closed case; the mediator reopens a case from the case list. Channel existence is not mediation state: leaving removes the channel, and the mediator view creates it again for every stored case. A case stored before the other trader's request was retained loads with only the requester's request.

## Provenance of chat messages

A message is imported by its exact content, so both traders' versions of a message under one ID sit next to each other. A message is marked unverified when it reached the mediator only inside a request of a trader who is not its author. The mark is bound to the exact message content and author, not to the message ID, and it is cleared only by a request of the author carrying the same message. Every renderer of a forwarded message shows the mark. A marked message is not shown as the mediator's own message, also when another profile of the mediator's node is its author. Forwarded messages are imported without their reactions, because nothing shows who added a reaction in a transcript; the traders' live reactions are not sent to the mediator.

## Not covered

Contract signatures of both traders and signed chat messages are not part of the request. Until they are, accepted contracts and forwarded messages are unauthenticated, the case view shows the contract as submitted, and a spammer can rotate profiles to open more cases. MuSig mediation has none of these checks.

The other trader sends no request when the mediator joins, so a differing contract shows only when both traders requested.

The first accepted request fixes the trade's two traders. A later request for that trade from or naming another profile is rejected, so until contracts are signed, a party can lock its counterparty out of mediation by naming another profile first.

A mark on a message already displayed is updated when the mediator opens the channel again. Until then, a message can keep a mark that a later request of its author cleared. A displayed message cannot miss a mark it needs: an unmarked message is either in a request of its author, which stays on record, or was received live, and a live message has the mediator as receiver, so it never matches a forwarded copy.

A quote inside a message is part of that message. Its text is not checked against the quoted author's messages, in live chat as well, so a quote carries no mark.

A reaction the mediator receives for a message ID attaches to the first message in the channel with that ID, also when both traders' versions of that message are shown. Trader clients send reactions only to the other trader, so this needs a modified client.
