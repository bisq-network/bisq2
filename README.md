# Bisq v2

Bisq v2 is development for a new enhanced version of Bisq (formerly working title was Misq).
You can find an overview about the motivation and the scope of the project [here](https://docs.google.com/document/d/1W6eyAzUv_3YRU-7LTax8eoIB3w6QFCu5vdNICy_jAlA/edit#heading=h.xs3m66c7ihbf)

## Current development state

### Network module

The network layer is functional to a large extent. It supports:
- Multiple transport types (Clearnet, Tor, I2P)
- Peer management to build a gossip overlay network
- Distributed data storage for 4 different types of data:
  - Authenticated data (e.g. offers). Only data creator can remove it
  - Authorized data (e.g. filter, mediator). Similar like above but only authorized users can publish such data (e.g. using bonded roles)
  - Mailbox messages: Only receiver can remove the data
  - Append only data: Only hash is used for storage. Data cannot be removed (e.g. trade statistics)
- Preparation for PoW based Dos protection (not implemented yet but the layer is already integrated)
- Sending and receiving confidential messages (encrypted, signed messages). Supports also handling of mailbox messages.
- Data retrieval at startup from other nodes

See spec at: https://github.com/bisq-network/bisq2/blob/main/network/src/main/java/bisq/network/specification.md

### Identity module

Work on the identity management has been started but will require more work.

An identity consists of a `domainId`, `networkId` and a `keyPair`. The `domainId` is a tag to associate the identity with some domain aspect (e.g. offer ID). The `networkId` contains the network addresses for the different transport types (onion address, I2P address) and the public key. 
Because the network addresses are only known after the server has started (e.h. hidden service is published) we have to deal with some delay when creating a new identity (takes about 4 sec. for Tor). To avoid that UX degradation we use a pool of identities which are initialized at startup, so when the client requests a new identity we take one from the pool, and it is ready to get used without delay. 

What is missing is the management for the selection of identities. The user can define different strategies from using a new identity for each interaction (e.g. each offer has its isolated identity) to grouping interactions to one identity (e.g. all altcoin trades use the same identity) to using only one global identity as it is the case in the current Bisq version.  

### Desktop module
The current UI prototype comes with a simple offer message board (Social/Trade Intent) and a chat (Social/Hangout).
In Settings/Network there are information about the network (connections, nodes).

One can publish an offer intent and another peer can contact the maker and start a chat with the maker.
Purpose of that was mainly to test the network with more realistic use cases. It is not clear yet if that example use case will get developed further to a production quality level or if it will be dropped later.

### Offer, Contract, Protocol modules
These domains are implemented for some demo trade protocols (2of2 multisig, BSQ bond based, LN based) but no integration with real wallets. Though the network integration works already, so the demo protocols send real messages between the trade peers. It is all still in a very preliminary state and will require much more work to get closer to some production-level version. 
Once we have a Liquid wallet integration we will probably focus on an atomic swap USDT-BTC on Liquid as that seems to be one of the easier protocol types. 
A reputation based protocol can also be implemented without a full wallet integration. But this will require more work on the identity and chat domains.

### Social module
The social module manages chat use cases. User management is not implemented yet. Public chat channels are also not implemented yet. 


## How to run the prototype

Currently, you need to start the Desktop app from the IntelliJ IDE and not via gradle as there are some open issues with the JFoenix library we are currently using (might get replaced with Gluon).

You can right-click the `bisq.desktop.Main` class in the desktop module to create a run config or create a `run config` in `Run/Edi Configurations`. You need to add the JMV argument: `--add-opens java.base/java.lang.reflect=ALL-UNNAMED` (due the JFoenix issue with Java 16).

If you want to use the network you can start the `bisq.tools.network.monitor.MultiNodesMain` application which starts up several seed nodes and normal nodes to build up a P2P network. 
To start with a minimal set of 2 seeds and 2 normal nodes in clearnet (localhost) use the program argument:
`--bootstrapAll=true`
If you want to run it with clearnet, Tor and I2P use:
`--bootstrapAll=true --transports=CLEAR,TOR,I2P`

The desktop app requires then also JVM args (the typesafe config lib we use do not support overriding program args, so you have to use JVM args).
For using clearnet use:
`--add-opens java.base/java.lang.reflect=ALL-UNNAMED -Dbisq.networkServiceConfig.supportedTransportTypes.0=CLEAR`

For clearnet, Tor and I2P use:
`--add-opens java.base/java.lang.reflect=ALL-UNNAMED -Dbisq.networkServiceConfig.supportedTransportTypes.0=CLEAR -Dbisq.networkServiceConfig.supportedTransportTypes.1=TOR -Dbisq.networkServiceConfig.supportedTransportTypes.2=I2P`

When you use I2P you need to install I2P and start the router application. The web console opens automatically. There you  need to navigate to [Clients](http://127.0.0.1:7657/configclients) and start the `SAM application bridge`. It will take about 2 minutes until it is ready.

## How to contribute?
Please get in touch via our Keybase or Matrix channels (transition from keybase to matrix is in progress). 

## References:
- [Bisq v2 Overview](https://docs.google.com/document/d/1W6eyAzUv_3YRU-7LTax8eoIB3w6QFCu5vdNICy_jAlA/edit#heading=h.xs3m66c7ihbf) 
- [Bisq v2 Original proposal](https://github.com/bisq-network/proposals/issues/330)
- [Bisq v2 Projects](https://github.com/bisq-network/projects/issues/51)
- [Bisq v2 UX Challenges](https://github.com/bisq-network/bisq/discussions/5959)
- [Bisq v2 Matrix room](https://matrix.to/#/#bisq.v2.dev:bitcoin.kyoto)





