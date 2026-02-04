# AGENTS.md — Bisq 2

## Overview
Bisq 2 is a decentralized trading platform.

## Tech Stack
- Java (JDK 21 required)
- Gradle (Kotlin DSL)
- JavaFX (required for desktop app)

## Repo Structure (high‑level)
- `apps/` — composite build for desktop app, seed node app, installers
- `network/` — composite build for networking components
- `common/`, `platform/`, `presentation/`, `application/`, `trade/`, etc. — core modules
- `docs/dev/` — dev guide, build instructions, code guidelines

## Build & Run
Common commands (macOS/Linux):
- Build all: `./gradlew clean build`
- Build without tests: `./gradlew clean build -x test`
- Install desktop app: `./gradlew :apps:desktop:desktop-app:installDist`
- Run desktop app: `./apps/desktop/desktop-app/build/install/desktop-app/bin/desktop-app`

Seed node:
- Install seed node: `./gradlew :apps:seed-node-app:installDist`

Full clean/rebuild:
- `./gradlew cleanAll buildAll`

## Local Network Scenarios
See `Makefile` for multi‑node local test setups (clearnet/tor/i2p) using `screen`:
- `make start-clearnet-full-env n=2`
- `make start-tor-seeds` → then `make start-tor-full-env n=2`

## Config & Runtime Options
- JVM args are the primary config mechanism (typesafe config)
- Program args supported: `--app-name`, `--data-dir`
- Custom config file: `bisq.conf` in the data dir

## Protobuf Conventions
- One `.proto` file per module; package name matches module name
- `option java_package = "bisq.<module>.protobuf"`
- Field names use lowerCamelCase (not underscore)
- Enums use ALL_CAPS with a type prefix (e.g., `CHATMESSAGETYPE_TEXT`)
- Optional Java fields should use `optional` in proto

## Testing Instructions
- Add or update tests when behavior changes
- Keep tests deterministic and fast
- Respect existing JUnit parallelization and resource locks
- Never disable or weaken assertions
- If a change cannot be reasonably tested, explain **why**

## Authority
Human contributors have final authority.
Agents are assistants, not decision-makers.

When uncertain: **do nothing and ask**.

## Conventions & Guidelines
- Follow `docs/dev/code-guidelines.md`
  - Lombok for getters/setters/toString/equals/hashCode
  - K&R brace style, always use braces
  - Avoid nullable values; use `Optional` and `@Nullable` where needed
- See `docs/dev/contributing.md` for PR workflow and commit style
- For i18n strings, only update the base file in `i18n/src/main/resources/<name>.properties` and do not edit `..._<lang>.properties` files directly.

## Useful Docs
- `docs/dev/build.md`
- `docs/dev/dev-guide.md`
- `docs/dev/code-guidelines.md`
- `docs/dev/protobuf-notes.md`

## License
Bisq 2 is licensed under the AGPL-3.0. All contributions are subject to this license.
