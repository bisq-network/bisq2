# Connection registration

A node keeps at most one inbound and one outbound connection for each peer address. It counts,
limits and closes only the connections it has registered.

## Rules

1. Registration of a connection for an address is one atomic step. Two handshakes or connection
   attempts which finish at the same time cannot both stay registered.
2. Inbound: a new connection replaces a registered connection from the same address, because the
   older one may be stale. The node closes the replaced connection.
3. Outbound: if a connection to the address is already registered, the node keeps it, because
   callers may already use it, and closes the new connection.
4. When a connection closes, the node removes the entry only if the entry is this connection. An
   entry which a newer connection replaced stays with the newer connection.
5. Listeners receive exactly one disconnect event for each connection which they received as
   connected: either from the close of a registered connection, or from the replacement in rule 2.
   A connection which was never registered is not reported.

## Reason

Before these rules, the node checked for an existing entry and registered in a separate step, and
a closing connection removed whatever entry its address had. A peer which opened two connections
at the same time, or an honest peer which reconnected while its old connection was closing, could
leave a running connection outside of the map. The node then did not count it against its limits,
never closed it for a limit, and never reported its close to listeners, so requests which waited
for the disconnect stayed pending until their timeout.
