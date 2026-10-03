# Authorization tokens

## Supported methods

Nodes must not create or select the legacy `HASH_CASH` authorization token. They accept it for one message only:
the handshake request of a node of an earlier version (see
[Handshake requests of earlier versions](#handshake-requests-of-earlier-versions)).
Only `HASH_CASH_V2` and `EQUI_HASH` have authorization implementations. Local features
control which implementations are available. A configuration naming `HASH_CASH` as a
preferred method is invalid.

The legacy feature can be read from old peer capabilities, but does not identify a
supported authorization method. Distributed application configurations must not advertise it.
Initial handshake requests use `HASH_CASH_V2` before the peer's capabilities are known.

Selection uses local preference order among the peer's supported methods. If no preference
matches, it uses the first supported method advertised by the peer. A peer advertising no
supported authorization method is rejected. Token creation fails if the selected method
has no local implementation. An empty peer feature collection uses the first local preference.

## Decoding

A legacy token body is rejected regardless of its declared token type. The only exception is a received handshake
request (see [Handshake requests of earlier versions](#handshake-requests-of-earlier-versions)). Legacy, unspecified,
and unknown token types are rejected, including when paired with a supported token body.
The declared type must match the token body. There is no fallback for unknown token types.

Keep the legacy protobuf field and enum numbers allocated; do not reuse them for another
method. Keeping these identifiers does not permit legacy authorization of other messages.

## Handshake request dates

A responder accepts a handshake request only if its date is within two hours of the receiver's current time.
Both boundaries are included. This rule applies to every transport and authorization method, including a
legacy handshake token. Extreme timestamp values outside this window must be rejected.

The date check limits how long a request can be used again, while allowing differences between the clocks of peers.

## Handshake requests of earlier versions

The releases up to 2.1.13 create the token of their handshake request with the legacy `HASH_CASH` method. They do not
know the features of the responder when they create the request, and their default is the legacy method. The releases
2.1.3 to 2.1.13 use `HASH_CASH_V2` for all later messages.

1. A responder accepts a handshake request with a legacy token only if all of these conditions hold:
   - The token has the legacy token body, and its declared token type is the legacy type.
   - The message counter of the token is 0.
   - The payload of the proof of work equals the handshake request, serialized for the hash.
   - The challenge of the proof of work is the challenge for the address of the responder and the message counter 0.
   - The difficulty of the proof of work is at least the difficulty of a handshake request at the initial network
     load: 65 536 * 0.05 * 0.1, about 327.68. There is no tolerance.
   - The proof of work is valid for its difficulty.
2. All other checks of the handshake apply unchanged: the network version, the ban list, the date of the request,
   and the address ownership proof.
3. The responder creates the token of its response with a supported method which the requester advertises. A requester
   which advertises only the legacy method gets no response, and the handshake fails. These are the releases before
   2.1.3.
4. A node rejects a legacy token in a handshake response and in every message of an established connection.

Reasons:

- Rule 1: the release 2.1.13 cannot change its first message. Without rule 1, a node of a release up to 2.1.13 cannot
  open a connection to a node of this version, also not to a seed node. It is then connected to nodes of this version
  only when they open the connection. Such a node may never receive the alert or the release notification which
  tells its user to update. Other rules of this version assume that nodes of both versions are connected until all
  nodes have updated, for example the rules for signature V1 and for the inventory filter of earlier versions.
- Rule 1, only the handshake request: it is the only message which the releases 2.1.3 to 2.1.13 send with the legacy
  token to a node of this version. The proof of work of the legacy method contains a copy of the message, so the
  envelope has about twice the size of the message. A handshake request is small: its largest envelope with the
  legacy token is below 1.8 KB, far below the size limit of the handshake (see rule 6 of
  [Message framing](message-framing.md)).
- Rule 1, no tolerance: the requester does not know the network load of the responder, so both use the fixed initial
  network load. The expected difficulty is therefore one fixed value. The releases up to 2.1.13 tolerate a difficulty
  which is up to 50 000 below the expected value. For a handshake request, this accepts every difficulty.
- Rule 1, payload: the releases up to 2.1.13 also accept a payload which is the complete serialization of the message.
  No release from 2.1.3 on creates such a payload for a handshake request.
- Rule 4: a node of this version never creates a legacy token. The releases 2.1.3 to 2.1.13 send a legacy token to a
  node of this version only with their handshake request. Any other legacy token comes from a modified client or
  from a release before 2.1.3.

## Compatibility

- Nodes of the releases 2.1.3 to 2.1.13 can open connections to nodes of this version, and nodes of this version to
  them. Only their handshake request uses legacy authorization.
- Nodes of releases before 2.1.3 support only legacy authorization. They cannot open a connection to a node of this
  version (rule 3 of the previous section), and they cannot read the `HASH_CASH_V2` token of its handshake request.
- The acceptance of the legacy token for handshake requests can be removed when nodes of the releases up to 2.1.13
  no longer need to connect to the network. The developers decide when this is the case.

The shared hash cash proof-of-work algorithm remains available for V2 and other consumers.

## Replay state lifetime

Each connection owns its received `HASH_CASH_V2` message counters. A counter is consumed atomically,
so simultaneous copies of one token cannot both be accepted on that connection. Handshake counters
remain consumed when the handshake becomes an established connection. Connections have independent
counter sets; this does not prevent replay across connections, as described for handshake requests.

Authorization services must not retain counters for failed handshakes or closed connections. The
state follows the handshake and connection lifetime, rather than a node-wide registry of past IDs.
