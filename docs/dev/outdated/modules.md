_Note: This document is outdated_

### Network module

The network layer is already functional to a large
extent. [See the spec here](https://github.com/bisq-network/bisq2/blob/main/network/network/src/main/java/bisq/network/specification.md)
.

It supports:

- Multiple transport types (Clearnet, Tor, I2P)
- Peer management to build a gossip overlay network
- Distributed data storage for 4 different types of data:
  - Authenticated data (e.g. offers). Only the data creator can remove it.
  - Authorized data (e.g. filter, mediator). Similar to above, but only authorized users can publish such data (e.g.
    using bonded roles).
  - Mailbox messages: Only receiver can remove the data.
  - Append-only data: Only hash is used for storage. Data cannot be removed (e.g. trade statistics)
- Preparation for PoW based DoS protection (not implemented yet but the layer is already integrated)
- Sending and receiving confidential messages (encrypted, signed messages). Also supports handling of mailbox messages.
- Data retrieval at startup from other nodes

### Identity module

Work on the identity management has been started but requires more work.

An identity consists of a `domainId`, `networkId`, and `keyPair`. The `domainId` is a tag to associate the identity with
some domain aspect (e.g. offer ID). The `networkId` contains network addresses for the different transport types (onion
address, I2P address) and the public key.
Because the network addresses are only known after the server has started (e.g. hidden service is published) we have to
deal with some delay when creating a new identity (takes about 4 sec. for Tor). To avoid this UX degradation, we use a
pool of identities which are initialized at startup, so when the client requests a new identity we take one from the
pool, and it is ready to be used without delay.

What is missing is a way to manage the selection of identities. A user can define different strategies:

- use a new identity for each interaction (e.g. each offer has its isolated identity)
- group interactions to one identity (e.g. all altcoin trades use the same identity)
- use one global identity (as it is in Bisq v1)

### Desktop module

The current UI prototype comes with a create offer, offerbook, take offer, portfolio screens and a chat (Social).
In Settings/Network there is information about the network (connections, nodes).

Currently, one peer can publish an offer intent, and another peer can contact the maker peer and start a chat with them.
The purpose here is mainly to test the network with more realistic use cases. It is not clear yet if this example
use-case will get developed further for production or if it will be dropped.

### Offer, Contract, Protocol modules

These modules are implemented for some demo trade protocols (2-of-2 multisig, BSQ bond-based, LN-based) but there is no
integration yet with real wallets (ubut note that the network integration works already, so the demo protocols send real
messages between the trade peers). It is all still in a very preliminary state and will require much more work to get
closer to some production-level version.

Once we have a Liquid wallet integration, we will probably focus on an atomic swap USDT-BTC on Liquid, as that seems to
be one of the easier protocol types.

A reputation-based protocol can also be implemented without a full wallet integration, but this will require more work
on the identity and chat domains.

### Social module

The social module manages chat use-cases. User management is not implemented yet. Public chat channels are also not
implemented yet. 

