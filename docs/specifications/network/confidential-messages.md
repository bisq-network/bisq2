# Confidential messages

A confidential message carries a message which the sender encrypted for the receiver and signed. This specification
defines which messages a node accepts only inside a confidential message, and which messages the node passes to the
services that handle them.

## Terms

- A confidential message (`ConfidentialMessage`) contains the encrypted message, the public key and the signature of
  the sender (`ConfidentialData`), and the ID of the key of the receiver. A node sends it directly to the peer, or it
  stores it as mailbox data in the network.
- The confidential payload is the message inside a confidential message, after the receiver decrypted it.
- An envelope (`NetworkEnvelope`) is the unit which a connection sends and receives. Its payload is one message of the
  type `EnvelopePayloadMessage`.
- A confidential message listener is a service which receives confidential payloads, for example the chat, trade and
  support services, and the services of the oracle node.
- The service for message delivery states sends the `AckMessage`s of the node and applies the `AckMessage`s which the
  node receives. It is not a confidential message listener. The node passes each confidential payload to this service
  directly, together with the ID of the key which decrypted the payload (see
  [Message acknowledgement](message-acknowledgement.md)).

## Rules

1. These messages are valid only as confidential payload:
   - a message of another module, which the network layer carries as `ExternalNetworkMessage` (field 15 of
     `EnvelopePayloadMessage`). Examples: private chat messages and their reactions, trade messages, mediation,
     arbitration and moderation messages, and the requests to oracle nodes;
   - `AckMessage`.
2. A node sends such a message only inside a confidential message.
3. An envelope cannot carry such a message. This is an integrity rule of the envelope (rule 1 of
   [Validation at parsing](validation-at-parsing.md)):
   - A node which receives such an envelope drops the message when it parses it, so no listener of the node receives
     it. The node keeps the connection open (rule 6 of [Validation at parsing](validation-at-parsing.md)). It logs the
     type, the size and the peer of at most three such messages per connection, and no content.
   - A node cannot create such an envelope, so it cannot send one by mistake.
4. A node passes a confidential payload to the confidential message listeners and to the service for message delivery
   states only after these checks:
   - The node decrypted the confidential message with one of its own keys, and the signature of the sender is valid.
   - If the payload names the public key of its sender, this key is the key which signed the confidential message.
   - If the payload names the public key of its receiver, this key is the key which decrypted the confidential
     message.
5. The confidential message listeners and the service for message delivery states receive no other messages. In
   particular, they never receive a message which the node received as payload of an envelope.

## Reasons

- The services decide with the sender which a message names. Examples:
  - A chat service adds a message to a private channel only if its sender is a peer of the channel (rule 4 of
    [Private channel membership](../chat/private-channel-membership.md)).
  - A trade service passes a trade message to the protocol of a trade only if its sender is the trade peer (see
    [Sender of a trade message](../trade/trade-message-sender.md)).
  - A mediator accepts a mediation request only from a trader of the trade.

  These checks depend on rule 4. The sender which a message names is authentic only because the node compared it with
  the key which signed the confidential message.
- A message which a peer sends as payload of an envelope has no such check. Before rules 3 and 5, the node passed it
  to the confidential message listeners as well. At that time, the service for message delivery states was one of
  them. A peer could send a chat, trade or support message in the name of any user, without encryption, and the
  services accepted it. A peer could also send an `AckMessage` with the ID of a message of the node. The node then
  showed the message as received and stopped to resend it.
- Rules 3 and 5 each prevent this. Rule 5 makes the listeners and the service for message delivery states independent
  of the parser: they receive only what the node decrypted and checked. Rule 3 drops the message before any component
  of the node sees it, so that no other listener of the node can act on it.
- Rule 3 keeps the connection, as for every message which breaks an integrity rule. The receive throttle of the
  connection limits the rate of such messages.

## Compatibility

- Unmodified nodes of version 2.1.13 and of this version send these messages only inside confidential messages. They
  have no other way to send them. So the rules drop no message of an unmodified node. They change no message format
  and no persisted data.
- Nodes of version 2.1.13 and of earlier versions still pass such messages to their confidential message listeners.
  They stay exposed until they update.

## Known limitations

- A peer can send such envelopes without proof of work and keep the connection. This is the known limitation of
  [Validation at parsing](validation-at-parsing.md#known-limitations) for every message which breaks an integrity rule.
