# Reaction IDs

This specification defines which reaction IDs a node accepts in a reaction to a chat message.

## Terms

- A reaction is a short answer of a user to a chat message, for example "thumbs up". The reaction ID is the number of
  the reaction. Version 2.1.13 and this version know 6 reactions with the IDs 0 to 5.
- A public reaction is a reaction to a message of a public channel: a common public chat channel or an offerbook
  channel. All nodes store, relay and serve it.
- A private reaction is a reaction to a message of a private chat or of a trade chat. The sender sends it only to the
  receiver.

## Rules

1. A reaction ID is not negative.
2. The reaction ID of a public reaction is at most 5. This is the highest ID which version 2.1.13 parses.
3. The reaction ID of a private reaction has no upper bound. A receiver ignores a reaction whose ID it does not know.
4. Rules 1 and 2 are integrity rules of the reaction (see rule 1 of
   [`../network/validation-at-parsing.md`](../network/validation-at-parsing.md)). A reaction which breaks one of them
   cannot be created, and a received or persisted one fails at parsing.
5. The bound of rule 2 can be raised when no supported version rejects higher IDs at parsing. Until then, a version
   which adds a reaction must not give it an ID above the bound if it can be used as a public reaction.

## Reasons

- Rule 2: version 2.1.13 rejects a reaction with an ID of 6 or higher at parsing. A 2.1.13 node closes the connection
  when it cannot parse a message, and it drops a whole inventory response which contains one entry that it cannot
  parse. If nodes of this version stored, relayed and served a public reaction with a higher ID, one such reaction
  would disconnect 2.1.13 nodes from them and would make their inventory responses unreadable for 2.1.13 nodes, as
  long as the reaction lives. A 2.1.13 node can never add the reaction to its inventory filter, so each later response
  contains it again.
- Rule 3: only the receiver gets a private reaction, and no node relays it. So a reaction ID of a later version breaks
  no other node, and users of a later version can use new reactions in private chats with each other.

## Compatibility

- Unmodified nodes of version 2.1.13 and of this version create only reactions with the IDs 0 to 5.
- A node of this version which receives a public reaction with a higher ID cannot parse it. It skips the entry of an
  inventory response and drops the broadcast message, and it keeps the connection (see
  [`../network/validation-at-parsing.md`](../network/validation-at-parsing.md)).
