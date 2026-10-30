# Pending trade messages

This specification defines how a node keeps trade messages whose trade has no protocol yet. It applies to Bisq
Easy and to MuSig. Rule 4 applies the same limits to chat messages of Bisq Easy and MuSig trades, and rule 5 to mediator
responses of Bisq Easy trades, which arrive before the trade channel exists. The terms are those of
[Sender of a trade message](trade-message-sender.md).

## Background

A node creates a trade when it takes an offer, or when it receives the take offer request of a taker. Trade messages
travel through the mailbox of the network, and the mailbox does not keep the order of messages. So a trade message can
arrive before the take offer request which creates its trade. The node keeps such a message as a pending message and
passes it to the protocol of its trade when that protocol handles its next message. A message of an existing trade
which the protocol of the trade cannot process yet is a queued trade message (see
[Queued trade messages](queued-trade-messages.md)).

## Rules

1. A node keeps a trade message whose trade has no protocol yet as a pending message. The decision to keep a message
   as pending and the registration of the protocol of its trade are one step. So a message which arrives while the
   node registers the protocol either reaches the protocol directly, or is pending before the registration. After the
   registration, the node passes the pending messages of the trade to the protocol, in arrival order, when the
   protocol has handled a message of the trade. A maker does this right after its protocol has handled the take offer
   request. A taker, and a node which registers the protocols of its stored trades at startup, register protocols for
   which no message can be pending. A pending message which the node passes to the protocol is no longer pending, and
   the node passes each pending message to the protocol only once. A pending message of an existing trade whose sender
   is not the trade peer is dropped, as [Sender of a trade message](trade-message-sender.md) defines.
2. A node keeps at most 100 pending messages. When a message arrives while the node keeps 100 pending messages, the
   node drops the oldest pending message and keeps the new one. A message which is already pending does not change
   the number or the order of the pending messages.
3. A log entry about a pending message names the type of the message and the trade ID. It does not contain the
   content of the message. The trade ID is sanitized for the log, because its sender controls it.
   The same applies when the state machine of a trade handles a message: its log entries and exception messages
   name only the type of the event, also when the trade is in a final state or the handler fails.

4. The chats of Bisq Easy open trades and of MuSig open trades keep a chat message whose trade channel does not exist
   yet as a pending chat message. Rules 2 and 3 apply to these pending chat messages as well, with a bound of 100 for
   each of the two chats.
5. A trader's node keeps the response of a mediator to a Bisq Easy mediation request, whose trade channel does not exist
   yet, as a pending response, only if its sender is a mediator whose bonded role is authorized and not banned. It
   processes the pending responses again when a trade channel is added. Rules 2 and 3 apply to these pending responses
   as well, with their own bound of 100. A pending response is identified by its trade ID and its sender, so the node
   keeps responses of different senders for the same trade apart. When the node processes a response, it applies it only
   if its sender is the mediator of the trade channel (see [Trade chat channel binding](chat-channel-binding.md)), and
   drops it otherwise. Dropping a response does not remove a pending response of another sender.

6. A received chat message creates a Bisq Easy trade channel only if it carries an offer and the node has a trade with
   its trade ID whose peer is the sender of the message and whose own user profile is the receiver of the message.
   Otherwise the message is a pending chat message (rule 4). When the node creates a trade or a trade channel, or loads
   a trade at startup, it processes the pending chat messages of that trade again. A chat message whose trade or trade
   channel the node creates while the message becomes pending still reaches the channel. If that channel does not
   accept the message (see [Private channel membership](../chat/private-channel-membership.md)), the node drops the
   message, and it is no longer pending. A received chat message makes the node process other pending chat messages
   only when it creates the trade channel, and then only those of its trade.

## Reasons

- Rule 1, one step: the network layer passes each message to the trade service in a task of its own, so the
  messages of a trade can be processed at the same time (see [Startup replay](../network/startup-replay.md)). The
  node acknowledges a message when it decrypts it, so the peer does not send it again (see
  [Message acknowledgement](../network/message-acknowledgement.md)). Without the one step, a message could find no
  protocol, then the take offer request could create the protocol and process the pending messages, and only then the
  message could become pending. Only a later message of the trade would pass it to the protocol. When a seller takes
  a buy offer and sends the payment account data right after the request, no such message comes: the buyer waits for
  the account data, and the seller waits for the payment.
- Rule 1, only once: a protocol must apply a message of the trade peer once. The state machine ignores a second copy
  only of a message which changed the state of the trade. A MuSig protocol applies some messages without a change of
  the state, and it would apply a second copy again.
- Rule 1, after the registration: a message can become pending only while its trade has no protocol, because the
  decision is one step with the registration. So the node gets all pending messages of the trade when it takes them
  after the registration, and no later message of the trade becomes pending. If it took them before the
  registration, a message which became pending afterwards would stay pending.
- Rule 1, taker and startup: the trade ID contains the take offer date (see [Trade ID](trade-id.md)). The trade peer
  of a taker learns the trade ID only from the take offer request, which the taker sends after it registered its
  protocol. The pending messages exist only in memory, and at startup the node registers the protocols of its stored
  trades before it receives messages. So no message of these trades can be pending.
- Rule 2: anybody can send trade messages with made-up trade IDs to a node. Such a message never gets a trade, so
  without a bound it stays in memory until the node restarts, and the memory of the node grows without limit. A node
  has only a few trades whose messages can arrive before the trade exists, so 100 pending messages leave room for all
  of them.
- Rule 2 drops the oldest message because a message of a legitimate trade gets its trade shortly after it arrives. An
  old pending message is most likely one which never gets a trade.
- Rule 4 for MuSig: every node processes chat messages of MuSig open trades, also when MuSig is not activated. Without
  the bound, anybody who knows the network ID of a node could exhaust its memory with chat messages for made-up trade
  IDs.
- Rule 6: the user does not see a trade channel without a trade and cannot leave it, and the node persists every trade
  channel. If any received chat message with an offer created a channel, anybody could make a node persist channels
  for made-up trade IDs with chat messages alone. The taker's first chat message can arrive before the take offer request which
  creates the trade of the maker, so the message waits as a pending chat message until the trade exists.
- Rule 6, receiver: a node can host several user profiles, and the trade peer knows the profiles of other identities
  from their offers. A channel which a message to another of our profiles created for our trade does not match the
  trade, and the trade service fails the trade with a report to the peer. The report would tell the peer that both
  profiles are hosted on one node (see rule 5 of [Sender of a trade message](trade-message-sender.md) for trade
  messages).
- Rule 6, at the same time: the node receives a chat message in another thread than the one which creates the trade
  or the channel. The creator processes the pending chat messages of the trade right after it added the trade or the
  channel. A message which found neither, and which became pending after that, could stay pending for good. If it is
  the taker's first chat message, the maker gets no trade channel. So the node looks for the channel and checks the
  trade again after it kept the message as pending.
- Rule 6, other pending chat messages: a pending chat message can reach a channel only when its trade or its channel
  is created, and the node processes the pending chat messages of the trade at that step. Processing all pending chat
  messages after each received chat message would repeat the work for up to 100 messages each time.
- Rule 3: trade messages carry private data of the traders: the payment account data of the seller, the Bitcoin
  address of the buyer, a transaction ID, or a payment proof. Users share their log files with support. A log file
  must not reveal the trade data of a trade peer.
- Rule 5, identity by trade ID and sender: anybody who knows the trade ID can send a response for it. If the node
  identified a pending response by its trade ID only, a response of another sender which arrived first would keep the
  response of the mediator out, and the trader would not learn that the mediator took up the case. The sender of a
  response cannot be forged: the network layer verifies that the sender signed the message with the key of the
  network ID which the response names.
- Rule 5, sender: only a mediator sends a response, and every node knows the mediators, so the node can check the
  sender before the trade channel exists. Unlike the messages of rules 1 and 4, whose sender the node does not know
  before the trade exists, a flood of another sender therefore cannot push the response of the mediator out.

## Limitations

- A sender who floods a node with trade messages for made-up trade IDs can push a pending message of a legitimate
  trade out of the pending messages before the trade exists. The node does not receive this message again. The bound
  limits the memory of the node; it does not protect the pending message of a legitimate trade against such a flood.
  Without the bound the flood would instead grow the memory of the node without limit. The same applies to pending
  chat messages (rule 4). For pending mediator responses (rule 5) only a mediator can cause such a flood, because the
  node keeps only responses of mediators.

- Rule 6: a flood of chat messages for made-up trade IDs can push the taker's first chat message out of the pending chat
  messages before the take offer request arrives. The maker then has no trade channel, and the trade fails at the
  next step of the maker (the trade requires its chat channel).
- Rule 6 relies on the trades of the node. The maker stores a trade only for a take offer request which it accepted (see
  [take offer request validation](take-offer-request-validation.md)). A trade which an earlier version stored for a
  rejected request still lets the taker of that request get a channel stored for that trade.

## Compatibility

- The rules change no message and no persisted data. The pending messages exist only in memory.
- Rule 5 does not change when two mediator response messages are equal. The node identifies a pending response with
  its own key of trade ID and sender.
