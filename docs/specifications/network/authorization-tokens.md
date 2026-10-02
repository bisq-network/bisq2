# Authorization tokens

## Supported methods

Nodes must not create, accept, or select the legacy `HASH_CASH` authorization token.
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

A legacy token body is rejected regardless of its declared token type. Legacy, unspecified,
and unknown token types are rejected, including when paired with a supported token body.
The declared type must match the token body. There is no fallback for unknown token types.

Keep the legacy protobuf field and enum numbers allocated; do not reuse them for another
method. Keeping these identifiers does not permit legacy authorization.

## Compatibility

Connections using legacy authorization fail, including initial handshake requests from
older clients which use `HASH_CASH` even when they support V2 for subsequent messages.
Those clients must update their handshake behavior. There is no legacy fallback.

The shared hash cash proof-of-work algorithm remains available for V2 and other consumers.
