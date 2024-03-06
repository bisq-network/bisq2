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

Bisq Easy is designed for novice Bitcoin users with a focus on ease to use and convenience. It does not require that the
user has already Bitcoin and it is based on an easy-to-use chat interface. Security is based on reputation of the
Bitcoin seller. It does not compete with the current Bisq v1 trade protocol but is complementary to it. It serves and
targets a user group which we cannot reach with Bisq v1. You can read more about Bisq Easy
in [the Bisq wiki](https://bisq.wiki/Bisq_Easy).


 ![Actions Status](https://github.com/bisq-network/bisq2/actions/workflows/build.yml/badge.svg)

## Why work on Bisq 2?

- **Compensated Contributions:** Unique in open-source, contributors are rewarded for their work.
- **Technological Edge:** Java, Blockchain, P2P networks, JavaFX - cutting-edge tech at your fingertips.
- **Complex, Rewarding Challenges:** Dive into a sophisticated architecture that rewards ingenuity.

## Getting Started

### Quick Setup

1. **Clone Bisq 2:**
   ```bash
   git clone https://github.com/bisq-network/bisq2.git
   ```

2. **Install Dependencies:**
   See our [Installation Guide](./docs/dev/build.md) for detailed instructions.

3. **Run desktop client:**
   ```bash
   ./gradlew desktop:desktop-app:run
   ```

## Community and Contributions

Bisq is an open source project and a [Decentralized Autonomous Organization (DAO)](https://bisq.network/dao/).

Whether you're reporting bugs, suggesting features, or contributing code, you're part of our ecosystem. Get involved:
- [Contribution Guideline](./docs/dev/contributing.md)
- [Development Guidelines](./docs/dev/dev-guide.md)
- Join the discussion on [Matrix](https://matrix.to/#/#bisq.v2.dev:bitcoin.kyoto)

## Documentation

Dive into our extensive documentation for a deeper understanding of Bisq 2:

- [Distributed Data Storage](./docs/dev/distributed-data-storage-notes.md)
- [Protobuf](./docs/dev/protobuf-notes.md)

## Support and Troubleshooting

Run into issues? Check our [Troubleshooting Guide](./docs/known-issues-with-installation.md) or reach out on [Matrix](https://bisq.chat).

## License

Bisq 2 is licensed under the [AGPL-3.0 license](LICENSE). All contributions are subject to this license.

