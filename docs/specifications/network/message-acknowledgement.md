# Message acknowledgement

This specification defines how a node acknowledges a confidential message which requests an acknowledgement (an
ACK-requesting message), and how it applies a received `AckMessage` to the delivery status of its own messages.

## Terms

- **Identity.** A node has a default identity, and a user node has one identity for each user profile. The NetworkId
  of an identity contains the public key with its key ID, and the address map: one address for each transport.
- **Decrypting key.** The key of the node which decrypted a confidential message. The confidential message names the
  key ID of the receiver key outside of the encrypted data. The node finds its key pair by this key ID. The decryption
  succeeds only if the message was encrypted for this key.
- **Service for message delivery states.** The service of the node which sends the `AckMessage`s and which keeps the
  delivery status of the ACK-requesting messages of the node.

## Rules

1. **Only decrypted messages.** A node sends an `AckMessage` only for an ACK-requesting message which it decrypted with
   one of its keys. It applies only an `AckMessage` which it decrypted. The node passes each confidential payload to
   the service for message delivery states when it decrypts the message, together with the key ID of the decrypting
   key. It does this after the checks of rule 4 of [Confidential messages](confidential-messages.md). The service
   receives no other messages (rule 5 of [Confidential messages](confidential-messages.md)). It also receives the
   messages which the node decrypts before the service is initialized, so it needs no startup replay (see
   [Startup replay](startup-replay.md)).
2. **The identity of the decrypting key sends the AckMessage.** The node sends the `AckMessage` from its own NetworkId
   for the decrypting key, with the key pair of that key. It finds this NetworkId in its own local state by the key ID
   of the decrypting key. It never uses the receiver NetworkId which the message names as its own identity, also not
   if this NetworkId contains a public key or a key ID of the node.
3. **Older address maps are valid.** A receiver NetworkId in the message with another address map than the current
   one of the node is not an error. An honest peer can address the node with an older address map, for example from
   an offer which the node published before it added a transport. The node acknowledges the message from its current
   NetworkId.
4. **No identity, no AckMessage.** If the decrypting key belongs to no NetworkId of the node, the node sends no
   `AckMessage`. It writes one log line without content of the message. Example: a key which the node uses for another
   purpose than messaging, such as the API access.
5. **Each identity acknowledges on its own.** Each identity of a node acknowledges a message as a separate node would
   do. The node processes a decrypted message once for each decrypting key. It remembers the messages which it
   acknowledged by decrypting key and message ID, and sends no second `AckMessage` for a second copy of a message, for
   example the copy which arrives over the second transport.
6. **AckMessages only for our own messages.** The node creates the delivery status of its own ACK-requesting message
   before it sends the message. It applies an `AckMessage` only to a delivery status which exists. An `AckMessage` for
   an unknown message ID changes nothing and creates no state, also no stored state.

## Reasons

- Rule 1: only the decryption knows the decrypting key, and rule 2 needs this key. The confidential message listeners
  do not receive the decrypting key, so the service for message delivery states is not one of them.
- Rule 2: the node creates a network node, with a server socket and threads, for each NetworkId from which it sends,
  and does not remove these nodes before it shuts down. If the node used the receiver NetworkId of the message, a peer
  could make it create a new network node with each message, by changing the port or the address in the message,
  until the node runs out of threads or file descriptors.
- Rule 2: the key ID in a NetworkId is data of the message. It is not derived from the public key. A peer could
  encrypt a message for one identity of the node and name another identity of the node as receiver. If the node
  answered with the key of the named identity, the peer would learn from the key of the `AckMessage` that the two user
  profiles belong to the same node.
- Rule 5: if the node remembered acknowledged messages by message ID only, a peer could send messages with the same ID
  to two user profiles and count the `AckMessage`s: one `AckMessage` means one node, two mean two nodes. The same holds
  for the deduplication of decrypted messages if it ignored the decrypting key, for message types whose content does
  not name the receiver key.
- Rule 6: an `AckMessage` for an unknown ID created a stored delivery status in earlier versions. Anybody who knows a
  user profile could grow this store without a limit, and the node loads the store completely at every start.

## Compatibility

- The format of `AckMessage` and of the stored delivery statuses does not change.
- A node of version 2.1.13 which sent an ACK-requesting message matches an `AckMessage` by the message ID only. It
  does not check from which NetworkId the `AckMessage` comes. So it accepts the `AckMessage` from the current NetworkId
  of the receiver (rule 3).
- Delivery statuses which earlier versions stored for `AckMessage`s with unknown IDs are removed by the existing rule
  that the node removes stored delivery statuses after 30 days.
