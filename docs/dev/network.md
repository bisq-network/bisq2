# Bisq P2P Network

The Bisq 2 P2P network is based on a gossip network and an end-to-end messaging layer. It uses multiple privacy networks
and comes with a build in DoS protection and network load balancing.

## Multiple transport layers

To support multiple privacy overlay networks we allow multiple transport layers to be used in parallel.

Suppoted transport layers:

- Tor
- I2P
- Clearnet (primarily used for development and possibly among public nodes like seed nodes)

Users can choose their preferred networks, with Tor and I2P set as the default. All messages are sent over both networks
which improves reliability and delivery time as the first arriving message will be processed and the one later ignored.
If a user on Tor attempts to send a message to an I2P-only user, the send attempt fails but the message is then
propagated as a mailbox message. The recipient then receives this mailbox message as a gossip message, decrypts it, and
processes it accordingly. The mailbox message system acts as a relay for such cross-network scenarios.

The `NetworkId` encapsulates the multiple network addresses like the onion address for Tor or I2P address.

_Please note that the I2P network is not yet available. It has basic integration but it is not production ready. Any
developer familiar with I2P is welcome to join to make it production ready._

## Gossip Network

Each node in the network connects to a set number of peers, typically ranging from 8 to 12, forming a peer group. When a
node receives a gossip message, it checks if it has already received the message (using its storage lookup). If it's a
new message, the node relays it to a certain percentage of its peers, such as 75% of its peer group. If the message has
already been received, the node refrains from relaying it to prevent endless relay loops.

This architecture offers high resilience and censorship resistance, making it challenging to stop message propagation.
It allows for rapid message dissemination.
However, there's no guarantee that a message will reach all nodes and there is some overhead of the redundant
broadcast (messages sent to nodes which have the message already).
The network is unstructured, allowing any node to join or leave without coordination.

**Benefits:**

- Unstructured and thus simple and easy to be used without centralized coordination
- Censorship resistant
- Fast propagation

**Drawbacks:**

- Overhead from redundant propagation
- Scaling limits
- Resource and storage requirements for each node

## Access Control Framework

Due to the inherent scaling limits of a gossip network—where all messages are sent to and stored by all nodes—there is a
need for a system to manage network usage during high utilization or potential Denial of Service (DoS) attacks. Bisq
employs a flexible access control framework, initially implementing HashCash Proof of Work (PoW), with plans to
transition to Equihash.

Additionally, future access control implementations could be based on reputation or burned BSQ. The access control
operates between two connected peers, with each node reporting its network load to the other. Using a cost factor
determined by message type and properties (size, Time-To-Live, etc.), and the peers network load the required difficulty
is calculated. Before sending a message, the sender generates the necessary PoW and attaches it to the message. The
receiving node then verifies the PoW before accepting the message.

## Seed Nodes

When a node joins the network for the first time, it lacks knowledge of other node addresses. Thus, seed nodes with
well-known addresses serve as entry points to provide addresses of other network nodes.

These seed nodes, operated by Bisq contributors, run as headless applications on servers to provide high availability
and bandwidth.

Users can specify their own seed nodes through configuration or JVM arguments. Seed nodes are bonded roles, with BSQ
locked up as collateral.

## Inventory Requests

Upon joining the network, a new node sends an inventory request to others.
Bisq utilizes a flexible framework to transmit information about already locally held data efficiently, avoiding
redundant data delivery.

Currently, data hashes are used to compare with the receiver's data hashes, identifying missing data. However, this
approach can become inefficient with a large number of data entries.

The planned to implement [Minisketch](https://github.com/sipa/minisketch">https://github.com/sipa/minisketch) which will
provide a much more efficient solution.

Each inventory response is limited in size (2 MB), and requests continue until nodes confirm that no more data is
missing. The application can begin without receiving any inventory responses, eliminating the need for an initial data
delivery dependency. Data flows into the application in a "streaming" fashion, appearing no differently from data
received through gossip messages.

## End-to-End Messaging

Messages intended for a specific peer are transmitted as end-to-end encrypted and signed messages. If the peer is
offline, the message is encapsulated into a mailbox message (encrypted with the peer's public key) and distributed
through the gossip network.

## Distributed Data Storage

Gossip messages serve as the basis for data storage within the network. Each message is stored at every node, utilizing
the payload's hash as the storage key.

Data removal is protected by a signature scheme, allowing only authorized nodes (message originator or mailbox message
receiver) to delete data. All data have a defined Time-To-Live after which they are automatically removed.

Various storage implementations include:

- Authenticated Data Store
- Authorized Data Store
- Mailbox Message Data Store
- Append-Only Data Store

For more details on the storage mechanisms, refer
to [distributed-data-storage-notes.md](distributed-data-storage-notes.md).

## Mailbox Message System

In cases where an end-to-end message fails to reach its intended offline recipient, the message is encrypted and
distributed as a gossip message to the distributed data store. Once the recipient node comes online, it retrieves the
message from the network, decrypts the mailbox message, removes it from the network, and processes the message within
the application.

## Design considerations

### Network Data Abstraction

At a higher level, consumers of network data are shielded from the specifics of how the data is received.
Whether it arrives via a mailbox message, direct message, inventory request, or propagated gossip message is abstracted
away. Additionally, the transport method through which a message was received remains undisclosed to higher layers.

### Message Integrity and Abstraction

All messages within the network are rigorously typed, final classes, and are limited in size. They undergo message-level
verification to ensure adherence to basic limitations and expectations. This approach significantly reduces potential
attack surfaces. However, certain messages, such as mailbox messages, contain binary blobs, limiting inspection and
offering protection primarily through size constraints.

### Message Type Abstraction

Message types that are outside the scope of the network module are not exposed to the network module itself. To enable
processing without requiring all dependencies of higher layers, these messages are encapsulated within Protobuf's `Any`
object—a wrapper for binary data. The application defines the mapping of higher layer Java classes to the Protobuf
objects, which are included in the module. This design ensures that all messages are clearly defined within the network
and organized in a modular way.

### Handling Unknown Messages

In the event that a node receives an unknown Protobuf message or other data, it rejects the connection, maintaining
network integrity and security.

### Future Plans

There are ongoing plans to introduce a message type that enables third parties to utilize the network for transmitting
any form of data as a byte array within a message.
