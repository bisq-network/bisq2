# Bisq 2.1.11 Highlights

Short text for GitHub release notes and the in-app update message.

## Security And Release Integrity

- Dependency verification is much stricter: checksum-only artifacts are gated, verification metadata XML parsing is hardened, source artifacts are covered, and executable protobuf tool classifiers are pinned explicitly for Windows, Linux, macOS Intel, and macOS Apple Silicon.
- Release signing readiness was hardened with expected signer checks, signing fingerprint enforcement, and release asset coverage checks.
- Gradle wrapper verification, dependency signature policy checks, an isolated CVE scan tool, and pinned composite-build environments now run through the release/check workflow.
- GitHub Actions workflows were hardened, archived build actions were replaced, and Apple Silicon macOS CI coverage was added.
- Update signature verification now checks each key source, and launcher update JAR loading is disabled.

## Runtime, Dependencies, And Packaging

- The release version is `2.1.11`.
- The Java toolchain is updated to Zulu `21.0.11`, with OpenJFX runtime `21.0.11`.
- Core runtime and build dependencies were refreshed, including gRPC, Bouncy Castle, HttpClient5, Logback, Jackson, JNA, Lombok, JUnit, Mockito, I2P, JavaCV, OpenJFX, Jersey, OkHttp, Swagger Core, and other libraries.
- Tor was updated, macOS aarch64 support was added, embedded Tor process handling was improved, and inherited `LD_PRELOAD` is cleared for embedded Tor and installer tests.
- Local Tor helper modules were added for `jsocks` and `jtorctl`, replacing external helper usage.
- `cleanAll` and `buildAll` were improved, temporary build output is ignored, and JPMS/runtime packaging analysis docs were added.

## Trading, Accounts, And User Flows

- Bisq Easy offer amount constraints are enforced.
- REST take-offer amount handling now uses the quote currency.
- Banned users no longer send messages or create offers.
- QR-code text output was added for pairing codes.
- Contacts wording/style was improved and the contact-list auto-popup was removed.

## Notifications, Webcam, And IPC

- Relay push notifications gained mutable-content support and optional iOS-compatible symmetric encryption.
- Relay notification encoding was normalized to base64 and migrated to the newer relay API.
- Confidential message keys are bound to Bisq Easy identities.
- Webcam launch and IPC handling are more defensive: webcam JARs are verified before launch, IPC messages are authenticated, failure handling is hardened, and native webcam resources are closed on capture exit.

## Build And Verification Docs

- Dependency signature reporting and release-readiness docs were expanded.
- Java runtime packaging and JPMS feasibility analysis documents were added for future runtime packaging decisions.
