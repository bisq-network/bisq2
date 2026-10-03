# Chat history in dispute messages

This specification defines which messages of a trade chat a trader's node sends to a dispute agent when the whole chat
history does not fit into one message.

## Terms

- A dispute agent is a mediator or an arbitrator.
- A dispute message is a message from a trader to a dispute agent which carries the chat history of the trade. These
  are the Bisq Easy mediation request, the MuSig mediation request, the MuSig arbitration request and the MuSig dispute
  case data message.
- The chat history is the set of messages in the trade chat channel at the trader's node. The node keeps these messages
  in a set, which has no defined order.
- The date of a chat message is the creation time which its author put into the message.
- The chronological order sorts chat messages by their date, oldest first. Messages with the same date are sorted by
  their ID.
- The text size of a chat message is the number of bytes of its text in UTF-8.
- The serialized size of a dispute message is the number of bytes of its protobuf serialization.

## Rules

1. The node puts the chat history into the chronological order before it selects messages. So the order in which the
   node reads the messages from the channel does not change the selection. The only exception are different messages
   with the same date and the same ID. They keep the order in which the node reads them.
2. The node keeps the newest messages whose text sizes add up to less than 18 000 bytes. It goes from the newest
   message to the oldest message and stops at the first message which would reach this limit. It does not keep this
   message or any older message, even if a shorter older message would still fit.
3. If the serialized size of the dispute message with the kept messages is above 19 984 bytes, the node removes the
   oldest kept message and creates the dispute message again. It repeats this until the dispute message fits.
4. If the dispute message is too large even without chat messages, the node does not send it.
5. If the node removes messages, it logs the trade ID and the numbers of kept and removed messages. It does not log the
   content of the messages.

## Reasons

- The end of the chat is closest to the problem which led to the dispute. So the node keeps the newest messages.
- Rule 1: the chat history comes from a set without a defined order. Rules 2 and 3 find the newest and the oldest
  messages by their position in the sorted list. Without rule 1, they would use the iteration order of the set, and
  the dispute agent would get an arbitrary part of a long chat instead of its end.
- Rule 1 uses the date because the users see the trade chat sorted by date. So the dispute agent gets the messages
  which the users see at the end of the chat. The ID only decides between messages with the same date. It makes the
  selection independent of the order of the input.
- The exception of rule 1 needs no further sort key. A node creates a random ID for each chat message, and trade chat
  messages cannot be edited. So two different messages with the same date and the same ID exist only if the trade peer
  creates them on purpose. The peer wrote both messages, so it gains nothing from the choice between them.
- Rule 2 removes most of the excess text before the node serializes the dispute message. Rule 3 then makes sure that
  the dispute message fits.
- Rule 3: the node sends a dispute message as a confidential message. The cipher text of a confidential message has at
  most 20 000 bytes. The encryption adds an authentication tag of 16 bytes to the serialized dispute message. So the
  serialized size may be at most 19 984 bytes.

## Limitations

- The author of a chat message sets its date. A node accepts a received chat message only if its date is after the
  launch of Bisq 1 and less than two hours ahead of the node's clock. So a trade peer can give its own messages earlier
  dates, or dates up to two hours ahead. Such a message counts as older or newer than it is. This applies to the
  selection of rules 2 and 3 and also to the chat which the users see.
- The node does not record when it received a chat message, so it has no time which the trade peer cannot choose. A
  rule based on the time of receipt needs a new persisted field for each chat message. The developers decide whether
  this is needed.

## Compatibility

- The rules do not change the format or the limits of a dispute message. A dispute message sorts its chat messages by
  ID before serialization, independent of this selection. Receivers of all versions handle dispute messages as before.
- For a long chat, the selected messages change. The node now sends the newest messages. Version 2.1.13 sends an
  arbitrary part of a long chat: its mediation requests keep the first messages in the iteration order of the set, up
  to 10 000 characters of text.
