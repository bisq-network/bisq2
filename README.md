<p align="center">
  <a href="https://bisq.network">
    <img src="https://bisq.network/images/bisq-logo.svg"/>
  </a>
</p>

# Bisq 2: The Decentralized Trading Platform

Bisq 2 will be the successor to [Bisq v1](https://github.com/bisq-network/bisq) and will support multiple trade
protocols, multiple privacy networks and multiple identities. Read more about Bisq 2 in
the [Bisq wiki](https://bisq.wiki/Bisq_2)

For the initial release Bisq 2 will come with the [Bisq Easy](https://bisq.wiki/Bisq_Easy) protocol only. Work on the
other protocols will start after Bisq 2 has been shipped.

Bisq Easy is designed for novice Bitcoin users with a focus on ease of use and convenience. It does not require that the
user already has Bitcoin and it is based on an easy-to-use chat interface. Security is based on reputation of the
Bitcoin seller. It does not compete with the current Bisq v1 trade protocol but is complementary to it. It serves and
targets a user group which we cannot reach with Bisq v1. You can read more about Bisq Easy
in [the Bisq wiki](https://bisq.wiki/Bisq_Easy).


## Getting Started

### Quick Setup

1. **Clone Bisq 2:**
   ```bash
   git clone https://github.com/bisq-network/bisq2.git
   cd bisq2
   ```

2. **Install Dependencies:**
   Bisq requires JDK 21. See our [Installation Guide](./docs/dev/build.md) for detailed instructions.

3. **Setup bitcoind git submodule:**
   At project setup run first:
   ```bash
   git submodule init
   git submodule update
   ```

   In case the submodule has changed after a project update, run:
   ```bash
   git submodule update
   ```

4. **Run desktop client:**
   ```bash
   ./gradlew apps:desktop:desktop-app:run
   ```

5. **Run desktop client with custom data directory:**
   ```bash
   apps/desktop/desktop-app/build/install/desktop-app/bin/desktop-app --data-dir=<data_dir>
   ```

**For Windows environments**: replace ./gradlew with gradle.bat as the previous example shows

## Community and Contributions

Bisq is an open source project and a [Decentralized Autonomous Organization (DAO)](https://bisq.network/dao/).

If you want to contribute to Bisq get in touch on [Matrix](https://matrix.to/#/#bisq.v2.dev:bitcoin.kyoto).

If you are a developer check out the [dev guide](./docs/dev/dev-guide.md).

**Why work on Bisq 2?**

- Compensated Contributions: Unique in open-source, contributors are rewarded for their work.
- Technological Edge: [P2P network](./docs/dev/network.md), Bitcoin, Cryptography,...
- Complex, Rewarding Challenges: Dive into a sophisticated architecture that rewards ingenuity.


## License

Bisq 2 is licensed under the [AGPL-3.0 license](LICENSE). All contributions are subject to this license.

![Actions Status](https://github.com/bisq-network/bisq2/actions/workflows/build.yml/badge.svg)
