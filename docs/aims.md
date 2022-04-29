# Bisq v2 Aims

Bisq v2 is a ground-up rebuild of the Bisq network to address the weaknesses of v1, which we outline [here](./motivations.md). 

It will be vastly more flexible and modular, supporting **multiple**: 
- [privacy networks](#multiple-networks)
- [trading protocols and contract types](#multiple-trade-protocols)
- [security mechanisms](#multiple-security-mechanisms)
- [interfaces](#multiple-interfaces)
- [dispute resolution mechanisms](#multiple-dispute-resolution-mechanisms)
- [identity setups](#multiple-identities)

In addition, Bisq v2 development will put the API at the forefront from the get-go, and the resulting software should give even more control to users and developers to get Bisq to do what they want to do.  

### Multiple Networks

Bisq v1 only allows peers to connect to each other over Tor, but Bisq v2 will aim to support more networks, be more resistant to DoS attacks, and put less trust in privileged nodes.
- Bisq v2 will support Tor, I2P, and hopefully Nym at some point (when it’s ready).
  - Tor and I2P could be combined for speed, stability, and redundancy since Tor can be unreliable.
  - Non-Tor networks may help get around the Great Firewall.
- DoS protection: some form of proof-of-work could be implemented so there is a cost to send messages across the network.
  - Potential solution: make the cost to send messages to a node continuously variable, so that if a node receives too many, its cost to receive messages increases. Peers continually exchange their network state and decide if/how to send messages to others based on PoW cost.
- Simplify role of infrastructure operators
  - Seed nodes should only provide nodes with addresses to use as entry points to the network, nothing more.
  - Specialized nodes could provide certain services
    - relay nodes could relay Tor messages to the I2P network (or vice versa)
    - service nodes could act as a proxy for mobile users with limited resource capabilities
    - other nodes could store mailbox messages

### Multiple Trade Protocols

Bisq v2 will offer a variety of trade protocols for users that vary in cost-benefit trade-offs across spectrums of security, privacy, accessibility, and convenience. Users can switch to the trade protocol they prefer based on conditions (mining fees, desired security, desired payment methods, desired assets to trade, etc).

Protocols that Bisq v2 will initially aim to make available include:
- Current Bisq v1 trade protocol (2-of-2 multisig + delayed payout tx + DAO)
  - or, 2-of-2 multisig without dispute resolution (mutually-assured destruction / MAD)
- BSQ bond-based protocol
- Same-chain atomic swaps (e.g. BSQ/BTC, already implemented in Bisq v1; Liquid L-BTC/L-USDT; etc)
- Cross-chain atomic swaps (e.g. XMR/BTC)
- Liquid BTC based protocol (multisig)
- Lightning Network based 3-party trade protocol
  - Unclear if this is feasible yet, but it's very interesting.

Some of these trade protocols will make it feasible to trade different types of contracts beyond currencies:
- loans
- bets/CFD
- options
- swaps
- futures

Liquid, for example, implements some covenants that could enable loan contracts.  

### Multiple Security Mechanisms

Different security mechanisms can be made available to work with different trade protocols:
- Collateral-based (e.g. Bisq v1 multisig 2-of-2, as described above)
- Atomic swaps (same-chain or cross-chain, as described above)
- Reputation-based; could be internal or external to Bisq network
  - Internal: reputation is derived from participation in Bisq chat, message board, etc.
  - External: reputation is derived from social graphs on external networks (social networks, personal connections, etc), PGP web-of-trust, etc.
- Account age witness (account age, perhaps extended with account signing based on successful payments, as used now in Bisq v1)

Separating the trade protocol from the security mechanism has a number of additional nifty benefits:

- Security mechanism and settlement mechanism can be separated depending on the chosen trade protocol:
  - On the cryptocurrency side, user can decide to settle via on-chain bitcoin, Lightning, wrapped BTC (L-BTC, R-BTC, etc).
  - On the fiat side, user can decide to settle via bank transfer, payment service, money order, face-to-face, etc.
- Users with no bitcoin can choose to use a reputation-based security mechanism to get started, and then use a more robust security mechanism once they have some bitcoin.
- Wallets beyond core ones (likely Bitcoin Core, Electrum, Liquid) can be integrated via RPC, if a trade protocol requires it
  - Bisq v2 itself will not have any wallet integrated into it (reduces attack surface; this was a top lesson learned from Bisq v1)
  - Such integrations (beyond core ones) will be left to users and communities

### Multiple Interfaces

Bisq v1 offers trading exclusively through a (rather heavy) desktop application.

Bisq v2 will be API-driven to enable new interfaces, bots, and integrations to be created in addition to the flagship desktop interface:
- a headless application controlled through a REST API
  - enables trading bots
  - enables alternative interfaces for mobile apps, RaspiBlitz, Umbrel, etc.
- a light mobile client that acts as a remote control for full nodes users host elsewhere
- a semi-full mobile client using specialized network nodes for gossip, but wouldn’t require a full node to be run elsewhere

### Multiple Dispute Resolution Mechanisms

Bisq v1 uses a single path for dispute resolution (mediation, arbitration, and the DAO). Users had no choice but to use it in case of any issues. Bisq v2 seeks to make dispute resolution an add-on service for users who desire it (still integrated when desired/needed, but not as central to the trading experience).

Multiple methods will be available, including none where appropriate:

- None, since atomic swaps don’t need any kind of dispute resolution

- Mediation, implemented through an open market of mediators across languages, time zones, etc.
  - Users seeking to use a mediation service can pay a fee directly to a service provider or perhaps even a service provider’s pool (sort of like an insurance).

Such an approach also reduces the project’s liability since it won’t need to directly offer these services.  

### Multiple Identities

Bisq v1 uses a single onion address for all operations until a user manually changes it and loses their local reputation.

Bisq v2 will offer more options:
- An isolated identity for every interaction (e.g. a separate identity for each offer)
  - Best privacy, but sacrifices functions like reputation and account age witness
- One identity per payment account
  - Allows compartmentalizing actions which involve real-life identity (e.g. avoid linking SEPA trade with full name to Revolut trade with just email address)
- One identity for all fiat accounts and one for all altcoin accounts
- One global identity
  - Like Bisq v1; this would enable reputation to accumulate at the cost of privacy.

---

If any of this sounds interesting to you, please [get in touch with us in our Matrix room](https://matrix.to/#/#bisq.v2.dev:bitcoin.kyoto)!

There are a ton of challenges for many kinds of developers, but especially for those interested in:
- How to scale a complex peer-to-peer network
- How to guard such a peer-to-peer network against DoS attacks (rough concept involving proof-of-work outlined above, but specifics still needed)
- How to handle distributed data in a peer-to-peer network of anonymous nodes where any node can be malicious
- Designing trade protocols and trade execution engines on Bitcoin, Liquid, RSK, etc.
  
Bitcoin developers, developers with backgrounds in distributed systems, UX designers, mobile app developers, and any others who can make the ideas in this document a reality are also very welcome.  
