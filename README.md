# Bisq v2

Hello world

Bisq v2 (fka Misq) will be the successor to [Bisq v1](https://github.com/bisq-network/bisq).

Contents:
- [Current Development State](#current-development-state)
- [Running the Prototype](#running-the-desktop-prototype)
- [Contributing](#contributing)
- [References](#references)

You might find it helpful to read through [this doc](./docs/motivations.md) on the motivations for Bisq v2 as well as [this doc](./docs/aims.md) which includes a comprehensive look at the aims for Bisq v2.

## Current Development State

### Network module

The network layer is already functional to a large extent. [See the spec here](https://github.com/bisq-network/bisq2/blob/main/network/network/src/main/java/bisq/network/specification.md).

It supports:
- Multiple transport types (Clearnet, Tor, I2P)
- Peer management to build a gossip overlay network
- Distributed data storage for 4 different types of data:
  - Authenticated data (e.g. offers). Only the data creator can remove it.
  - Authorized data (e.g. filter, mediator). Similar to above, but only authorized users can publish such data (e.g. using bonded roles).
  - Mailbox messages: Only receiver can remove the data.
  - Append-only data: Only hash is used for storage. Data cannot be removed (e.g. trade statistics)
- Preparation for PoW based DoS protection (not implemented yet but the layer is already integrated)
- Sending and receiving confidential messages (encrypted, signed messages). Also supports handling of mailbox messages.
- Data retrieval at startup from other nodes

### Identity module

Work on the identity management has been started but requires more work.

An identity consists of a `domainId`, `networkId`, and `keyPair`. The `domainId` is a tag to associate the identity with some domain aspect (e.g. offer ID). The `networkId` contains network addresses for the different transport types (onion address, I2P address) and the public key. 
Because the network addresses are only known after the server has started (e.g. hidden service is published) we have to deal with some delay when creating a new identity (takes about 4 sec. for Tor). To avoid this UX degradation, we use a pool of identities which are initialized at startup, so when the client requests a new identity we take one from the pool, and it is ready to be used without delay. 

What is missing is a way to manage the selection of identities. A user can define different strategies:
- use a new identity for each interaction (e.g. each offer has its isolated identity)
- group interactions to one identity (e.g. all altcoin trades use the same identity)
- use one global identity (as it is in Bisq v1)  

### Desktop module
The current UI prototype comes with a create offer, offerbook, take offer, portfolio screens and a chat (Social).
In Settings/Network there is information about the network (connections, nodes).

Currently, one peer can publish an offer intent, and another peer can contact the maker peer and start a chat with them. The purpose here is mainly to test the network with more realistic use cases. It is not clear yet if this example use-case will get developed further for production or if it will be dropped.

### Offer, Contract, Protocol modules
These modules are implemented for some demo trade protocols (2-of-2 multisig, BSQ bond-based, LN-based) but there is no integration yet with real wallets (ubut note that the network integration works already, so the demo protocols send real messages between the trade peers). It is all still in a very preliminary state and will require much more work to get closer to some production-level version. 

Once we have a Liquid wallet integration, we will probably focus on an atomic swap USDT-BTC on Liquid, as that seems to be one of the easier protocol types.

A reputation-based protocol can also be implemented without a full wallet integration, but this will require more work on the identity and chat domains.

### Social module
The social module manages chat use-cases. User management is not implemented yet. Public chat channels are also not implemented yet. 


## Running the Desktop prototype

Bisq 2 requires Java 17 and Gradle 7.3.3.

You can right-click the `bisq.desktop_app.DesktopApp` class in the desktop module to create a run config or create
a `run config` in `Run/Edit Configurations`.

The desktop app also requires JVM args (the typesafe config lib we use does not support overriding program args, so you
have to use JVM args).
- For clearnet use
  `-Dapplication.network.supportedTransportTypes.0=CLEAR`

- For clearnet, Tor, and I2P use
  `-Dapplication.network.supportedTransportTypes.0=CLEAR -Dapplication.network.supportedTransportTypes.1=TOR -Dapplication.network.supportedTransportTypes.2=I2P`

## Running Bisq using the Makefile targets with Unix environments

Makefile requires `make` and `GNU Screen` packages installed on your Unix system.
For reference, screen (https://savannah.gnu.org/projects/screen) is a command that give you the ability to launch and use multiple shells from a single shell instance. It' s imperative to use the `make` targets that you understand how to work with `screen`.

For Debian/Ubuntu environments:
- `# apt-get install screen make`

For Redhat/Fedora environments:
- `# yum install screen make -y`

After installing the packages, check the comments on the `USAGE` section of the `Makefile` for a quick introduction on how to use screen.

The `Makefile` allows you to start local seed nodes and desktop apps and full environments of Bisq 2. It's possible to start nodes with `clearnet`, `tor` and `i2p` scenarios.

Starting full environment scenarios are as simple as running:
- `make start-clearnet-full-env` will start a full clearnet environment with 2 seeds and 2 desktop apps.
- `make start-tor-full-env` will start a full tor environment with clearnet composed of 2 seeds and 2 desktop apps.
- `make start-i2p-full-env` will start a full i2p environment with clearnet composed of 2 seeds and 2 desktop apps.

Another example is starting just seed nodes:
- `make start-clearnet-seeds` will start 2 seed nodes using clearnet.
- `make start-tor-seeds` will start 2 seed nodes using tor and clearnet.
- `make start-i2p-seeds` will start 2 seed nodes using i2p and clearnet.

Notice that to start the `tor` and `i2p` desktop applications, you need to point to an existing seed. So if you only start seeds using `Makefile` you need to ensure the parameters to the seeds of the `tor` and `i2p` environments are correct.

It's also possible to specify how many desktop apps you want to spawn with the n parameter, see bellow:
- `make start-clearnet-full-env n=4` starts a clearnet environment composed of 2 seeds and 4 clients

## Running the Prototype with a local network
If you want to use the network, you have to start at least one seed node with the appropriate JVM arguments (see instructions below) as there are no public seed nodes available at that stage. You can run clear net, Tor and I2P or any combination of those.
You specify the network by:
`-Dapplication.network.supportedTransportTypes.0=CLEAR`
Where you need to use a different index for adding more. Values are: CLEAR, TOR, I2P.

When you use I2P, you need to install I2P and start the router application. The web console opens automatically. There you  need to navigate to [Clients](http://127.0.0.1:7657/configclients) and start the `SAM application bridge`. It will take about 2 minutes to be ready.
Please note that the I2P integration is not very stable yet. 

### Set up your local seed nodes:

To start 2 seed nodes on ports 8000 and 8001 connecting to each other use those JVM arguments.
```
-Dapplication.appName=bisq2_seed1 
-Dapplication.network.defaultNodePortByTransportType.clear=8000 
-Dapplication.network.supportedTransportTypes.0=CLEAR 
-Dapplication.network.seedAddressByTransportType.clear.0=127.0.0.1:8000 
-Dapplication.network.seedAddressByTransportType.clear.1=127.0.0.1:8001 

-Dapplication.appName=bisq2_seed2 
-Dapplication.network.defaultNodePortByTransportType.clear=8001 
-Dapplication.network.supportedTransportTypes.0=CLEAR 
-Dapplication.network.seedAddressByTransportType.clear.0=127.0.0.1:8000 
-Dapplication.network.seedAddressByTransportType.clear.1=127.0.0.1:8001 
```
Data directory is defined by `application.appName` in the example here `bisq2_seed1`.

Using other network types or multiple network types use one or multiple of those:
```
-Dapplication.network.supportedTransportTypes.0=TOR 
-Dapplication.network.supportedTransportTypes.1=I2P 
-Dapplication.network.supportedTransportTypes.2=CLEAR 
```

You have to provide then the seed addresses for the supported network types
To add multiple seeds add more lines of the same network type with other index (`.1`).
```
-Dapplication.network.seedAddressByTransportType.tor.0=TOR_SEED_ADDRESS:8000 
-Dapplication.network.seedAddressByTransportType.i2p.0=I2P_SEED_ADDRESS:5000 
-Dapplication.network.seedAddressByTransportType.clear.0=127.0.0.1:8000 
```

To set up your local tor hidden service addresses and I2P addresses for the seed nodes you need to start once to get
them created and then take them from the data directories.
Start the 'SeedNodeApp' with Tor and I2P enabled (I2P need to be started manually and SAM enabled).
Let the nodes start up for about 2 minutes so the hidden service is deployed. Then stop it (no seeds are found at that
point).
Pick the onion and I2P addresses from the generated files and put them into the seed node config.

Go to:
`[PATH to OS data dir]/[SEED_NODE_DATA_DIR]/tor/hiddenservice/default/hostname`
`[PATH to OS data dir]/[SEED_NODE_DATA_DIR]/tor/hiddenservice/default/hostname`
`[PATH to OS data dir]/[SEED_NODE_DATA_DIR]/i2p/default5000.destination`
`[PATH to OS data dir]/[SEED_NODE_DATA_DIR]/i2p/default5001.destination`

PATH to OS data dir is on OSX:
`/Users/[USER]/Library/Application\ Support`
On Linux:
`[USER]/.local/share`

Copy those addresses and add it to the JVM args as following:

```
-Dapplication.network.seedAddressByTransportType.tor.0=[onion address for node 1000]:1000
-Dapplication.network.seedAddressByTransportType.tor.1=[onion address for node 1001]:1001
-Dapplication.network.seedAddressByTransportType.i2p.0=[I2P address for node 5000]:5000
-Dapplication.network.seedAddressByTransportType.i2p.1=[I2P address for node 5001]:5001
```

If you want to use more seed nodes repeat it and fill in more but 1-2 is usually sufficient for dev testing.

### Example Run Configurations

See [here](docs/sample-run-configs.md)


## Contributing

Please get in touch on our [Matrix room](https://matrix.to/#/#bisq.v2.dev:bitcoin.kyoto). We can use help across many areas of development, UI/UX, etc!

See the [Bisq v2 aims](./docs/aims.md) for a more comprehensive look at what we're building.

## References
- [Bisq v2 Original proposal](https://github.com/bisq-network/proposals/issues/330)
- [Bisq v2 Projects](https://github.com/bisq-network/projects/issues/51)
- [Bisq v2 UX Challenges](https://github.com/bisq-network/bisq/discussions/5959)
- [Bisq v2 Matrix room](https://matrix.to/#/#bisq.v2.dev:bitcoin.kyoto)
