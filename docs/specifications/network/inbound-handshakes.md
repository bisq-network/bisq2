# Inbound handshakes

A node accepts sockets of unknown peers and runs the connection handshake for each of them. This specification limits
the resources which such handshakes can take.

## Terms

- An inbound handshake starts when the node accepts the socket of a peer, and ends when the handshake succeeds or
  fails.
- The node executor is the thread pool of a node. It runs the inbound handshakes, and it also creates the outbound
  connections of the node.
- The maximum number of connected peers is the configured limit of connections of the node, for example 12 on desktop
  nodes and 20 on seed nodes.

## Rules

1. A node runs at most as many inbound handshakes at the same time as its maximum number of connected peers. When this
   number of inbound handshakes is in progress, the node closes the socket of a new peer at once, without reading
   from it.
2. If the node executor rejects the inbound handshake of an accepted socket, the node closes the socket at once.

## Reasons

- Rule 1: a peer needs no work and no key to start an inbound handshake. Each handshake holds a thread of the node
  executor until it ends: 120 seconds for a peer which sends nothing, and several minutes for a peer which sends its
  bytes slowly. Without the limit, one attacker with cheap Tor streams takes all threads. The node can then neither
  complete honest handshakes nor create outbound connections, and an attack on all seed nodes blocks the start of new
  nodes. With the limit, the node executor keeps threads for outbound connections.
- Rule 1 closes the socket instead of queueing it, because a queued socket of an attacker would still wait for a
  thread, and the peer of a closed socket can try another node at once.
- Rule 2: nothing else holds the socket after the rejection. Without the close it stays open until garbage collection.
  Many such sockets use up the file descriptors of the process, and when the node cannot accept a socket, it stops its
  server.

## Limitations

- An attacker can still keep all places of rule 1 busy, so that the node closes the sockets of honest peers which try to
  connect. All inbound Tor sockets come from the same local address, so a limit per source address is not possible.
  Honest peers then connect to other nodes. A deadline for the whole handshake would shorten the time for which an
  attacker holds a place, and handshakes which do not hold a thread while they wait would remove the limit on threads.
  Both are open work for the developers (review finding F004, parts b and d).

## Compatibility

- The rules change no message. A peer of any version sees a closed socket, as after any failed connection attempt.
