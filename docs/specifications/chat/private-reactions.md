# Received private reactions

This specification defines the limits of the IDs in a private reaction. A private reaction is a reaction to a message
of a private chat between two users or of a trade chat (see [reaction IDs](reaction-ids.md)). The sender sends a
private reaction only to its receiver, as a confidential message, and the receiver acknowledges it with an ACK message.

Other specifications define the other rules for a received private reaction:

- [Pending reactions](pending-reactions.md): a reaction which arrives before its message or before its channel.
- [Private channel membership](private-channel-membership.md): the sender and the receiver which a reaction must name.

## Terms

- The ID of a reaction is the text which identifies the reaction as a message. The receiver uses it as the ID of its
  ACK message. It is not the reaction ID of [reaction IDs](reaction-ids.md), which is the number of the reaction, for
  example 0 for "thumbs up".
- The chat message ID of a reaction is the ID of the message to which the user reacted.

## Rules

1. The ID of a private reaction and its chat message ID have at most 50 characters. These are integrity rules of the
   reaction (rule 1 of [validation at parsing](../network/validation-at-parsing.md)). A reaction which breaks them
   cannot be created, and a received or stored one fails at parsing.
2. A node stores a private reaction inside the message to which it reacts. A stored reaction which fails at parsing is
   skipped. The message and its other reactions stay.

## Reasons

- Rule 1: the ACK message carries the ID of the reaction and accepts at most 100 characters. Without rule 1, the
  receiver cannot create the ACK message for a reaction with a longer ID. The creation throws an exception, and the
  sender never gets an acknowledgement. Before each replayed item was processed on its own (see
  [startup replay](../network/startup-replay.md)), this exception also stopped the start of the network, and the
  application did not start. With rule 1, the reaction fails at parsing, and no service of the receiver gets it.
- Rule 1, the limit: 50 characters is the limit of other IDs (`NetworkDataValidation.validateId`), and the ID of a chat
  message has at most 50 characters.
- Rule 2: rule 1 is new, and nodes of version 2.1.13 store reactions which break it. A stored reaction must not remove
  its message.

## Compatibility

- Unmodified nodes of version 2.1.13 and of this version create the ID of a reaction as a UUID with 36 characters, and
  they react to chat messages, whose IDs have at most 50 characters also in version 2.1.13. So their reactions pass
  rule 1.
- A node of this version skips a stored reaction with a longer ID when it loads the message (rule 2).
- Public reactions are not acknowledged with an ACK message, so the reason for rule 1 does not apply to them. They keep
  their rules (see [reaction IDs](reaction-ids.md)). Nodes of version 2.1.13 relay public reactions with longer IDs.
