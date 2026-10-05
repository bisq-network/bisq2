# Tor stream isolation

## Purpose

Bisq must not let an observer link the user profiles of one user through a shared Tor circuit.
Therefore, outgoing peer-to-peer (P2P) connections of different user profiles must use different
Tor circuits.

## Terms

- **Circuit:** a path through Tor relays. Tor can carry several connections over one circuit.
- **Stream:** one TCP connection that Tor carries over a circuit.
- **Rendezvous circuit:** the circuit between a client and an onion service. The onion service sees
  every stream that arrives on its end of this circuit.
- **SOCKS port:** the local proxy port of Tor. An application asks Tor through this port to open a
  stream.
- **SOCKS credentials:** the username and password that an application sends in the SOCKS5 handshake
  (RFC 1928, RFC 1929). Tor does not check them. Tor uses them only to decide which streams may
  share a circuit.
- **Node:** Bisq creates one node for each network identity: the default node and one node for each
  user profile. All nodes use the same Tor process and the same SOCKS port.
- **Node ID:** the hex-encoded hash of the public key of the network identity of a node. Each node
  has its own node ID.

## How Tor decides whether two streams share a circuit

Tor puts a new stream on an existing circuit only if its isolation rules allow it. The SOCKS port
flag `IsolateSOCKSAuth` adds this rule: two streams with different SOCKS credentials never share a
circuit. Tor turns this flag on by default, and Bisq does not change it.

The flag only compares credentials. It does not create them. If two streams arrive with the same
credentials, Tor may put them on the same circuit. To keep two connections on different circuits,
Bisq must send different credentials for them.

## Rules for P2P connections

1. A node opens each outgoing P2P connection through the SOCKS port of Tor.
2. The node sends SOCKS credentials derived from its node ID. The username is a hash of the node ID.
   The password is the fixed value `0`.
3. All outgoing P2P connections of one node use the same credentials. They may share a circuit,
   because they belong to the same identity.
4. Different nodes have different node IDs and therefore different credentials. Tor never puts the
   connections of two nodes on the same circuit.

## Rules for the SOCKS5 handshake

1. Bisq performs the SOCKS5 handshake itself. It does not use the SOCKS client of the Java runtime
   (see "Why Bisq performs the SOCKS5 handshake itself").
2. Bisq offers only username and password authentication. It rejects the connection unless the
   proxy selects this method and accepts the credentials. If Bisq continued without credentials,
   the connection would have the same empty credentials as every other connection without
   credentials.
3. Bisq sends the target host as a domain name, and Tor resolves it. Bisq never resolves an onion
   address locally.
4. Bisq rejects the connection if the proxy reports an error or sends an invalid reply.

## Why separate circuits are needed

Bisq automatically sends an acknowledgement (ACK) when a node receives a message. The node of the
receiving profile sends the ACK to the sender's address. If it has no connection to that address,
it opens one. The user does not need to read or answer the message.

If two nodes of one user could share a circuit, an attacker could link their profiles:

1. The attacker runs an onion service and sends one message to each of two profiles.
2. Both messages give the attacker's onion service as the sender's address.
3. The node of each profile opens a connection to that onion service and sends its ACK.
4. Both streams arrive on the same rendezvous circuit. The attacker learns that the two profiles
   use the same Tor client and therefore belong to one user.

With different credentials for each node, the two streams arrive on different circuits.

## Why Bisq performs the SOCKS5 handshake itself

Earlier versions of Bisq created P2P sockets as `java.net.Socket` with a SOCKS `java.net.Proxy`.
For such a socket, the Java runtime performs the SOCKS5 handshake inside `Socket.connect`. This
Java SOCKS client cannot send different credentials for each node:

- It offers "no authentication" and "username and password". Tor selects username and password,
  so that `IsolateSOCKSAuth` can work.
- It takes the credentials from the global `java.net.Authenticator`. The authenticator receives
  only the address of the proxy. It cannot tell which node opens the connection.
- Bisq sets no authenticator. The Java SOCKS client then sends the Java system property `user.name`
  (the operating system account) with an empty password.

So every node sent the same credentials. `IsolateSOCKSAuth` was on, but it had nothing to separate.
Tor could put the connections of different nodes on the same circuit.

## Remaining privacy limits and costs

Separate P2P circuits prevent the attack above. They do not remove every way to link profiles.

- **HTTP requests:** HTTP requests over Tor, for example to price nodes and block explorers, still
  use the Java SOCKS client. They all send the same credentials. They do not share circuits with
  P2P connections, because the credentials differ, but they can share circuits with each other.
- **Shared availability:** All onion services of a user run in the same Tor process. They are
  published together and become reachable or unreachable together. A peer can observe this over
  time and conclude that the profiles belong to the same user.
- **Shared Tor state:** All nodes use the same guard relay, the first relay of every circuit of the
  Tor process. They also share the cache of onion service descriptors, which contain the information
  needed to connect to an onion service. After one node fetches a descriptor, another node can reuse
  it. The second connection can then be faster.
- **More circuits:** When several nodes of one user connect to the same peer, each node needs its
  own circuit to that peer. Users with several profiles therefore create more circuits.
- **Configuration:** The SOCKS port flag `NoIsolateSOCKSAuth` disables this protection. It can be
  set in the configuration of an external Tor or with a `torrc` override.

## Compatibility and connection failures

### Network and proxy compatibility

Network messages and persisted data do not change. Peers see the same protocol as before.

Tor accepts username and password authentication. Bisq offers only this method. A proxy in front of
an external Tor that accepts only connections without authentication rejects Bisq's connections.
In earlier versions, such a proxy worked.

### Timeouts

The socket timeout of the transport applies to the connect to the SOCKS port and to each read during
the handshake. This includes the wait for Tor's reply to the connect request. The shipped
configurations use 120 seconds. The timeout applies to each operation, not to the whole handshake.

In earlier versions, the wait for Tor's connect reply had no timeout. Now a read that waits longer
than the socket timeout fails. This also applies when an onion service uses proof-of-work protection
and Tor needs more time to establish the connection.

After the handshake, the returned socket keeps the socket timeout for later reads, as before.
Without it, a read on the established connection could block forever if the peer stops sending.

### Reported errors

| Failure | Exception |
| --- | --- |
| Tor reports that it could not connect to the peer | `ConnectException` |
| Another SOCKS error occurs | `SocketException` |
| A connection or read times out | `SocketTimeoutException` |
| The proxy closes the connection during the handshake | `EOFException` |

The Java SOCKS client reported the failed connection, other SOCKS errors, and the closed connection
as `SocketException`. It did not time out during the handshake (see Timeouts).
`ConnectException` is a subclass of `SocketException`, so code that handles `SocketException` still
handles a failed connection to the peer. `EOFException` is not a subclass of `SocketException`.
