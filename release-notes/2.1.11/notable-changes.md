# Bisq 2.1.11 Notable Changes

This file filters the full release notes down to the most relevant engineering and product changes. Each section gives a short summary first and then lists representative related commits.

## Dependency Verification And Release Integrity

Bisq 2.1.11 substantially tightens dependency and release verification. Gradle dependency verification now validates checksum-only fallback entries against the resolved graph, rejects stale allowlist entries, validates fallback coordinates, hardens verification metadata XML parsing, covers source artifacts, and requires explicit metadata for executable build-tool classifiers. Release signing readiness now enforces the configured fingerprint, checks expected signers, verifies expected signable assets, and preserves existing release signatures. Gradle wrapper verification, CVE scanning, and dependency signature policy checks are wired into the verification flow.

Related commits:
- [4e9b8778bb](https://github.com/bisq-network/bisq2/commit/4e9b8778bb5fbf0e14ef2523ed453cb7b3caa2db) - Harden Gradle dependency signature verification.
- [3b3584f01e](https://github.com/bisq-network/bisq2/commit/3b3584f01e2fa3ff8210853a30f8bcc99d0f98cf) - Add dependency verification for source artifacts.
- [0597bd7904](https://github.com/bisq-network/bisq2/commit/0597bd79047b8e7feaaafa0185f883e06528e3d3) - Validate checksum fallback coordinates.
- [3c2fbff691](https://github.com/bisq-network/bisq2/commit/3c2fbff691a0943ee2e45c0893f818bde9d44cb6) - Harden verification metadata XML parsing.
- [52f3ff275f](https://github.com/bisq-network/bisq2/commit/52f3ff275f7de799347af1777b6c555e47e88158) - Gate checksum-only dependency artifacts.
- [a606f46516](https://github.com/bisq-network/bisq2/commit/a606f465165c901effd189d5836f129a00b35df4) - Validate checksum allowlist against resolved graph.
- [a43024b14a](https://github.com/bisq-network/bisq2/commit/a43024b14a636be076fd6ddadaaf09e92fa7db92) - Run dependency signature policy during checks.
- [e877a2f823](https://github.com/bisq-network/bisq2/commit/e877a2f8233a485573e5d32bae76d78043d51d4d) - Tighten executable dependency verification metadata.
- [fa18b01e35](https://github.com/bisq-network/bisq2/commit/fa18b01e35fcf5c557f9200dce1a9b1bb72e906c) - Add Windows protobuf tool verification metadata.
- [70c5836969](https://github.com/bisq-network/bisq2/commit/70c583696989ec7910f69a9d137a3ad69d28ba42) - Add Linux and macOS protobuf tool verification metadata.
- [7acd6f6f86](https://github.com/bisq-network/bisq2/commit/7acd6f6f8673a39be6872df4baeeb25454bc440e) - Add Gradle wrapper security verification.
- [c1781c18c7](https://github.com/bisq-network/bisq2/commit/c1781c18c7802d377e9578b3765d178e320ea4db) - Enforce Gradle wrapper verification in check.
- [63025796ce](https://github.com/bisq-network/bisq2/commit/63025796ceb6e35fc604bd642496ca842500c1b6) - Add isolated CVE scan tool.
- [9b7f958dae](https://github.com/bisq-network/bisq2/commit/9b7f958daedf93724d1bbc50fa0d46f13f796182) - Harden Bisq 2 release signing readiness.
- [4e5ca4f031](https://github.com/bisq-network/bisq2/commit/4e5ca4f031ba69cfd0449d8b61607457c6a3e7ed) - Enforce release signing fingerprint by default.
- [6e0d0bbd9c](https://github.com/bisq-network/bisq2/commit/6e0d0bbd9c59a5619068a2159bf3eebcf2e0ea0f) - Expect all signable Bisq 2 release assets.
- [e7ef38a8ba](https://github.com/bisq-network/bisq2/commit/e7ef38a8bafa308276b6677209b3ba03d20e1110) - Assert expected release signature signers.

## Runtime, Dependency, And Tor Stack Updates

The build and runtime stack was refreshed across Java, JavaFX, protobuf/gRPC, logging, crypto, HTTP, UI, test, and platform libraries. The Java toolchain now targets Zulu `21.0.11`, OpenJFX is updated to `21.0.11`, and a broad set of dependencies are updated or cleaned up. Tor packaging and embedded process lifecycle handling were improved, macOS aarch64 support was added, inherited `LD_PRELOAD` is cleared around embedded Tor, and local `jsocks`/`jtorctl` helper modules were added.

Related commits:
- [890c2fafd9](https://github.com/bisq-network/bisq2/commit/890c2fafd913daebbb63b7df7fc3dfa9cb32c3c0) - Update Java 21 toolchain to Zulu 21.0.11.
- [0056ecc2bf](https://github.com/bisq-network/bisq2/commit/0056ecc2bf3608110eda9313bda055297d905393) - Update gRPC to 1.81.0.
- [62a3c43e07](https://github.com/bisq-network/bisq2/commit/62a3c43e07e40d4daf5e44ab7fb860457ac64be6) - Update Bouncy Castle to 1.84.
- [cd741962e2](https://github.com/bisq-network/bisq2/commit/cd741962e2754f47a97a050fc02d0dc1e66758cb) - Update Logback to 1.5.32.
- [881c4bd003](https://github.com/bisq-network/bisq2/commit/881c4bd00329f5cdcc0f4d477a8dbc1106bf567d) - Update Jackson to 2.21.3.
- [de70c4badb](https://github.com/bisq-network/bisq2/commit/de70c4badbcee49c7bc951220a3f44b5b78a3ff9) - Update OpenJFX runtime to 21.0.11.
- [e5aef29c00](https://github.com/bisq-network/bisq2/commit/e5aef29c00323c22b79f69755ac8db4a3e29d20d) - Update I2P libraries to 2.12.0.
- [068328b253](https://github.com/bisq-network/bisq2/commit/068328b253c440465b7cbbe3512815c7b0cc25e6) - Update JavaCV runtime to 1.5.13.
- [47323c618a](https://github.com/bisq-network/bisq2/commit/47323c618a8f44da5a71d4b774842e108083e5b8) - Update Tor, support aarch64 macos and fix Tor packaging and embedded process lifecycle handling.
- [ee2c3dbc51](https://github.com/bisq-network/bisq2/commit/ee2c3dbc51adad7232c024ca4e05ba0e73210920) - Clear inherited LD_PRELOAD for embedded Tor.
- [508816c87e](https://github.com/bisq-network/bisq2/commit/508816c87eb555f146c751f2dd9caa36b7116992) - Clear inherited LD_PRELOAD in Tor installer integration test.
- [966e63ba5e](https://github.com/bisq-network/bisq2/commit/966e63ba5e9ab131ed2c95c97f617fd115c96bb4) - Add source module shells for Tor libraries.
- [ea20c8b3ff](https://github.com/bisq-network/bisq2/commit/ea20c8b3ff5c4822604d729c54fe5b6011885277) - Add source code from jsocks and jtorctl projects.
- [3069038c62](https://github.com/bisq-network/bisq2/commit/3069038c62a1197c527dc793e665bbc3f2478101) - Use local Tor helper modules.

## Bisq Easy, Contacts, And User-Facing Behavior

Trading and user-facing behavior received focused fixes. Bisq Easy offer amount constraints are enforced, REST take-offer amount handling uses the quote currency, banned users are prevented from sending messages or creating offers, contact-list behavior was made less intrusive, and contacts wording/style was improved. QR-code text output was added for pairing codes.

Related commits:
- [75b487db6c](https://github.com/bisq-network/bisq2/commit/75b487db6c91cda41bf3c3e09b1ae26ac4785a6a) - Enforce Bisq Easy offer amount constraints.
- [11ba89c814](https://github.com/bisq-network/bisq2/commit/11ba89c814ac655069a5b57831539542af3d78ca) - Use quote currency for REST take-offer amount.
- [7237c9b0dd](https://github.com/bisq-network/bisq2/commit/7237c9b0dd885f813666a5e84d91d04b31030ae9) - Implement QRcode in text for pairing code.
- [01f2346771](https://github.com/bisq-network/bisq2/commit/01f234677137832a0ca2c945641304985ef93eab) - Remove auto popup in contact list.
- [c3f00066c3](https://github.com/bisq-network/bisq2/commit/c3f00066c3421b52b6e82129970ca79a56469df5) - Improve wording and style.
- [ebdf53ee6c](https://github.com/bisq-network/bisq2/commit/ebdf53ee6c2e9c56eafae7ab89cb6dcd61c7361b) - Don't send a message or create offer when user is banned.

## Notifications, Webcam, And Message Security

The notification and webcam surfaces were hardened. Relay notifications now support mutable content and optional iOS-compatible symmetric encryption, normalize encoded content to base64, and use the newer relay API. Confidential message keys are bound to Bisq Easy identities. Webcam execution now verifies the JAR before launch, authenticates IPC messages, hardens failure handling, and closes native webcam resources on capture exit.

Related commits:
- [a48ae59f00](https://github.com/bisq-network/bisq2/commit/a48ae59f0043900cca53398c99b79ffa2d286df3) - Add relay notification mutable-content and iOS-compatible encryption support.
- [9b0dad2353](https://github.com/bisq-network/bisq2/commit/9b0dad2353e3e10b614763800bfe92a1a1780527) - Bind confidential message keys to Bisq Easy identities.
- [b22a500053](https://github.com/bisq-network/bisq2/commit/b22a500053eb345e186764ef92150f6bf484a44f) - Close webcam native resources on capture exit.
- [17d8dd262e](https://github.com/bisq-network/bisq2/commit/17d8dd262eb6b514d52a22604ca63b67ff8cc50e) - Authenticate webcam IPC messages.
- [40c3c485ea](https://github.com/bisq-network/bisq2/commit/40c3c485ea6364eb5f0bbc28be19ddbf167a47a7) - Verify webcam jar before launch.
- [b3bfcd6de9](https://github.com/bisq-network/bisq2/commit/b3bfcd6de93baa80ef1b1f15c12616b76aa64d5d) - Harden webcam IPC failure handling.

## CI, Build Workflow, And Documentation

The release workflow is more deterministic and documented. GitHub Actions workflows were hardened, Apple Silicon macOS CI coverage was added, the build action was replaced, composite roots pin build environments, `cleanAll` and `buildAll` were improved, and runtime/JPMS packaging notes were added for future release packaging decisions.

Related commits:
- [daaf56c7ba](https://github.com/bisq-network/bisq2/commit/daaf56c7baf871e1d8d7e462ca850e72bbe1c957) - Harden GitHub Actions workflows.
- [7232b7b3de](https://github.com/bisq-network/bisq2/commit/7232b7b3de8939466d2d33a95a730b912921983d) - Add Apple Silicon macOS CI coverage.
- [cb1259298f](https://github.com/bisq-network/bisq2/commit/cb1259298f14fc999492ed7d68b3538670b46424) - Replace archived Gradle build action.
- [e4e6f22151](https://github.com/bisq-network/bisq2/commit/e4e6f221518a03fb1ab9ad65412064cab2367ad5) - Harden workflow step boundary scanning.
- [8ee8162e96](https://github.com/bisq-network/bisq2/commit/8ee8162e9617dd4602b96d0fa60f25c0367433a1) - Pin build environment in composite roots.
- [716ed91153](https://github.com/bisq-network/bisq2/commit/716ed911539d3b7169fe6c876a4eb380ab2a7ec3) - Improve cleanAll and buildAll tasks.
- [374080af47](https://github.com/bisq-network/bisq2/commit/374080af47c70e32fb923814e1e59c3f17a34c6e) - Add JPMS feasibility analysis doc.
- [6b1d7071a7](https://github.com/bisq-network/bisq2/commit/6b1d7071a71ef712e40f49dca8e38ecca37d0642) - Add Java runtime packaging analysis doc.
