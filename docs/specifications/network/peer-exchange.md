# Peer exchange

This specification defines which peers a node keeps from the peer exchange, and how many.

A node runs one peer group for each transport (Tor, I2P, clearnet). In the peer exchange, two connected nodes send
each other a list of peers. Each peer has an address, the capability and the network load of the peer, and a date.
The reporter sets the date: for a peer of one of its connections, it is the creation date of the connection plus a
random offset of up to 3 seconds. A peer exchange message contains at most 500 peers.

The receiving node keeps the reported peers in memory, and it keeps the persisted peers on disk for the next start. It
uses both as candidates for new connections, and it reports them to other nodes. The node does not verify the reported
peers: any connected node can send any list.

## Rules

1. **Transport.** A peer group keeps a reported peer only if the address of the peer is of the transport of the peer
   group. For example, the Tor peer group does not keep clearnet addresses.
2. **Date.** A peer group keeps a reported peer only if its date is at most 1 minute in the future of the local clock.
3. **Limits.** A peer group keeps at most `maxReported` reported peers (default 500) and at most `maxPersisted`
   persisted peers (default 100). The limits apply to the peers of all address types. When the node adds peers and a
   limit is exceeded, it removes the peers with the oldest dates.
4. **Load.** When the node loads the persisted peers, it applies rules 1 to 3, and it writes the result back to disk.

## Reasons

- Rule 1: the node can connect only to addresses of the transport of the peer group. A reporter can send any address,
  and every IP literal with a valid port becomes a clearnet address, also in a message to a Tor node. Up to version
  2.1.13, the node kept these addresses, and from version 2.1.8 it removed persisted peers beyond the limit only if
  they were Tor or I2P peers. So any connected node could grow the persisted clearnet peers without a limit, and the
  node loaded all of them at every start.
- Rule 2: the limits keep the peers with the newest dates. The general date check of a peer allows dates up to 2 hours
  in the future, so a reporter could rank its peers above all honest peers and push the honest peers out. The
  tolerance of 1 minute covers the random offset of 3 seconds and small differences of the clocks. The node drops a
  peer of an honest reporter whose clock is more than 1 minute ahead only if the peer is very new; such a peer is only
  a candidate for a connection, so the loss is small.
- Rule 2 does not use the receive time as the date: a peer dated in the future would then still rank above the honest
  peers which the node received earlier.
- Rule 3: the node enforces the limits when it adds peers, so the collections are bounded at all times, not only after
  the next periodic housekeeping.
- Rule 4: a store which an earlier version wrote can contain peers of other transports, peers dated in the future and
  more peers than the limit. Rule 4 makes such a store shrink at the first start of this version.

## Compatibility

- The peer exchange messages and the format of the persisted peers do not change.
- At the first start of this version, the node removes the persisted peers which break rules 1 to 3. The removed peers
  are candidates for connections only, so the node loses no other data.
