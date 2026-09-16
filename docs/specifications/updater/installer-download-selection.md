# Installer Download Selection

## Runtime Platform Binding

An in-app launcher update must download the installer and detached signature for the operating system and architecture running the application. The selected filename must match the corresponding signed release asset.

macOS release assets are architecture-specific:

- Intel (`x86_64`) uses `Bisq-x86_64-<version>.dmg`.
- Apple Silicon (`arm64`) uses `Bisq-aarch64-<version>.dmg`.

The updater must not use the generic `Bisq-<version>.dmg` target on macOS because that name does not identify which architecture the image supports. Installer naming for platforms with a single published architecture remains unchanged.
