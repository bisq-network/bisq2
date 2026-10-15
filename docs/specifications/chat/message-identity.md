# Chat Message Identity

## Rule

A chat message that a channel already holds is not stored or shown a second time when it arrives again. Two messages are the same message when all their fields are equal, except the fields listed below. Two messages with the same ID and different content are different messages; the mediator keeps both traders' versions of a forwarded message (see [Mediation Request Validation](../support/mediation-request-validation.md)).

## Fields that are not part of identity

Reactions are not part of a message's identity, in public and in private chats. They are added to the stored message after it arrived, while a copy that arrives again carries the reactions it had when it was sent. Such a copy is the same message, for example a private message resent after a lost acknowledgement or a mailbox copy received again after a restart.

The receiver of a private message is not part of its identity, so that a message sent to several members of a group channel is kept once.

## Stored data

A channel stored with two copies of one message that differ only in their reactions loads with one of the two copies, and the reactions of the other copy are not shown.
