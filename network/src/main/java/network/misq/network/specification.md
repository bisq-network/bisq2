# P2P Network specification

## Features

- Support multiple privacy overlay networks (Tor, I2P) as well as clear net
- Support multiple network identities (e.g. maker use for each offer a dedicated network node and its address)
- Dos protection (e.g. PoW)
- Confidential end to end messaging (encryption and signing keys are decoupled from network identity)
- Peer discovery (seed nodes, handling of persisted peers, peer exchange protocol)
- Peer group management (using a gossip network topology)
- Distributed data management with different data handling policies
- Capability concept for supporting different node types, services and for adding new features
- Persistence

## Architecture

### Node

- `Transport`
    - Interface for getting a `ServerSocket` and a `Socket` from different transport networks (Tor, I2P, ClearNet)
- `Server`
    - Listening for connections
- `Connection`
    - Listening for messages
    - Send a `Message` by wrapping it into an `Envelope`
    - Unwrap `Envelope` and pass `Message` to message handler (`Node`)
- `Node`
    - Provides factory method for creating the `Transport` for the given `Transport.Type`
    - Creates a `Server`, `InboundConnection`s and `OutboundConnection`s
    - Performs initial _Connection Handshake Protocol_ via `ConnectionHandshake`
    - Listens on messages from connection and performs _Authorization Protocol_. If successful notify `MessageListener`s
    - At requests to send a message add AuthorizationToken according to _Authorization Protocol_
    - Handles `CloseConnectionMessage`s
- `ConnectionHandshake`
    - Implements the _Connection Handshake Protocol_
    - Blocking the input and output streams during the request/response handshake, guaranteeing the socket cannot be
      used otherwise until the handshake is complete
    - Exchange `Capability`s
- `NodesById`
    - Manages a map of `Nodes`s by using the nodeId as key
    - Delegate to chosen `Node`
    - Creates `Node`s on demand

#### Service layer

- `ServiceNode`
    - Creates `NodesById`
    - Creates `DefaultNode` used for the `PeerGroupService` and related services
    - Creates `ConfidentialMessageService` if that service is supported
    - Creates `PeerGroupService` if that service is supported
    - Creates `DataService` if that service is supported
    - Creates `RelayService` if that service is supported
    - Creates `MonitorService` if that service is supported
    - Delegate to relevant services.
    - Provide a bootstrap method which initializes the nodes and bootstraps the network
- `ServiceNodeByTransport`
    - Creates `ServiceNode`
    - Maintains a map of `ServiceNode`s by `NetworkType`
    - Delegate to relevant `ServiceNode`s

#### Top level API

- `NetworkService`
    - Top level service for delegating to `ServiceNodeByTransport` and `HttpService`

### Services

- `ConfidentialMessageService`
    - Encrypts and signs a `Message` and creates a `ConfidentialMessage`
    - Decrypts and verify signature when receiving a `ConfidentialMessage` and send `Message` to listeners.
    - Send messages via `NodesById` to node specified by nodeId
    - Listens on messages via `NodesById`
    - Use a keyId for quick lookup to keyPair

- `PeerGroupService`
    - Creates `Quarantine`, `PeerGroup`, `PeerExchangeService`, `KeepAliveService` and `AddressValidationService`
    - At `initialize` starts initial peer exchange via `peerExchangeService`. When this completes we start a `Scheduler` 
      to run several maintenance tasks as well we initialize the `KeepAliveService`.
    - Maintenance tasks:
        - `closeQuarantine`: We close the connection if the address of our connection is found in the `Quarantine` map. We do not send a `CloseConnectionMessage` but close immediately as addresses from `Quarantine` are only added due non protocol compliant behaviour. 
        - `verifyInboundConnections`: 
- 
- `RelayService`
  TODO for relaying messages from one transport network to another

- `DataService`
  TODO

- `MonitorService`
  TODO will provide network monitoring features

### Basic data structures

- `Transport.Type`: Enum with values`TOR, I2P, CLEAR`
- `Address`: Holds `host` and `port`
- `Capability`: Holds connection initiators address (not verified if that address is really reachable, might be done
  later in a dedicated protocol) and peers supported `Transport.Type`s. Will get added more data later.

### Messages

#### Node level

- `Message`: Base type of all messages
- `Envelope`: Holds a `Message` payload and the network version. Used at the `Connection` level
- `ConnectionHandshake.Request`, `ConnectionHandshake.Response`: Used for the _Connection Handshake Protocol_
- `AuthorizedMessage`: Used for the _Authorization Protocol_

#### ConfidentialMessageService

- `ConfidentialMessage`: Holds `ConfidentialData` and `keyId`

### Options/Configs

- Most domain classes come with a `Config` record for providing specific config data

### Protocols

#### Connection Handshake Protocol

At the first connection we perform a handshake protocol for exchanging the nodes capabilities. Basic capabilities are
the set of supported network types and the announced own address which is sent by the initiating node. This is not a
verified address but is useful for most cases to avoid creating a new outbound connection in case there is already an
inbound connection to that node. Further capabilities will be PoW related parameters and supported services. The initial
messages require as well to pass the Authorization Protocol using default parameters as nodes parameters are only known
after capability exchange.

The initiator of the connection starts the protocol with sending a Connection.Request.

1. Send ConnectionHandshake.Request with own Capability and AuthorizationToken
2. On receiving the ConnectionHandshake.Request send back the ConnectionHandshake.Response with own Capability and
   AuthorizationToken. Apply capability and complete protocol.
3. On receiving the ConnectionHandshake.Response check if AuthorizationToken matches requirement and if peers address
   matches the address used to send the request. If matches apply capability and complete protocol.

#### Authorization Protocol

Wraps a message into a AuthorizedMessage with adding the AuthorizationToken, which can contain in case of PoW the pow
hash and the related parameters to allow verification. Verifies received messages by unwrapping the AuthorizedMessage
using the specific AuthorizationToken for verification. PoW is not implemented yet.

### Node config examples:

#### Node supporting all core services and all privacy overlay networks

A typical config contains one Tor and one I2P `ServiceNode` with all core services activated. First we start the servers
of all our nodes. Then we bootstrap all nodes to the network. After that our node is bootstrapped. When sending a
message we create a node with the defined nodeId and an outbound connection if no connection to that peer already
exists. After connection is established it performs the _Connection Handshake Protocol_ and once completed it is used to
send the message. We send the message from all our network nodes matching the supported `Transport.Type`s of the
receiver. E.g. if peer also supports both Tor and I2P we send the message over both those networkNodes. We use a
default `Node` with nodeId `DEFAULT` used for the `PeerGroupService`.

#### Node just supporting the overlay network and data distribution

This node has not enabled the `ConfidentialMessageService` as it is not used as a user agent node but only for
propagating data in the overlay network.

## Threading model

Threading is complex in the network layer as many parallel execution streams happen, and we deal with blocking IO.
Javas non-blocking IO APIs do not support socks proxy which is required for Tor. 


### Server
A node starts a `Server` instance with a blocking IO thread for accepting new sockets on the given port.
Once a new socket is accepted it creates a new thread and call the socket handler (`Node`) in that thread context.
The `Node` starts the `ConnectionHandshake` with a blocking read on the input stream of the socket. Once the request 
message has been read it writes (blocking) to the output stream the reply message and after that returns to the 
caller (`Node`). The `Node` creates an `InboundConnection` puts it into the concurrent map and creates a 
dispatcher thread to notify the listeners about the new connections.
After that the thread execution is completed.

We have this structure of threads:
- `Server.listen`: Blocking IO thread listening for new sockets in a while loop
    - `Server.acceptSocket`: Starts blocking `ConnectionHandshake` protocol, creates `InboundConnection` and creates a dispatcher thread for notifying listeners.
      - `Node.dispatcher`: Thread for calling `onConnection` on listeners. 

Last update: 11.12.2021

