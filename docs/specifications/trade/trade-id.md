# Trade ID

This specification defines how the ID of a trade is derived, and which receivers check that a trade ID belongs to a
contract. It applies to Bisq Easy and to MuSig.

## Derivation

1. The ID of a trade is derived from data of its contract: the ID of the offer, the ID of the network ID of the taker,
   and the take offer date. The node concatenates the three values as text, hashes the result with SHA-256 and then
   RIPEMD-160, and takes the name-based UUID (version 3) of this hash.
2. Versions up to 2.1.6 created the ID without the take offer date. Version 2.1.7 added the take offer date for the
   trades which it created from 2025-06-01 (UTC) on. Since version 2.1.8 every new trade gets the ID with the take
   offer date.
3. A trade keeps the ID with which it was created. A trade which an earlier version created can still have the ID
   without the take offer date.

## Receivers which check the trade ID

A message which carries a trade ID and the contract of the trade can carry an ID which does not belong to the
contract. A receiver which files the message under its trade ID checks that the ID is the ID of the trade of the
contract.

4. The maker checks the trade ID of a MuSig take offer request: it must be the ID with the take offer date. A take
   offer request creates a new trade, so the form without the date does not apply.
5. A node checks the trade ID of a Bisq Easy mediation request at parsing: it must be the ID of the trade of the
   contract in the request, with or without the take offer date (rule 3). A request which breaks the rule fails at
   parsing (rule 1 of [validation at parsing](../network/validation-at-parsing.md)), and the store of the mediator
   skips a persisted case with such a request.

## Reasons

- Rule 5: the mediator files a mediation request under its trade ID, and it adds the chat messages of the request to
  the trade channel of this ID. A trade ID is not a secret: both traders know it, the mediator knows it, and it appears
  in support chats and log files. Without rule 5, a user who knows the trade ID of a mediation case of other users could
  send a request with this ID and a contract of its own. Its chat messages would appear in the channel of the other
  case, and the mediator would get a second case with the same trade ID.
- Rule 5 accepts the ID without the take offer date, because a trade which an earlier version created keeps this ID
  (rule 3). Both forms are derived from the contract, so a sender cannot choose the ID of another trade with either
  form.

## Compatibility

- The derivation is unchanged. Rules 4 and 5 change no message format.
- Unmodified nodes send a mediation request with the ID of their trade, so no request of an unmodified node fails
  rule 5. Nodes of version 2.1.13 do not check rule 5.

## Limitation

- The MuSig mediation and arbitration requests do not check rule 5 yet. MuSig is not released.
