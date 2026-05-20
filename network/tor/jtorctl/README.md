# jtorctl

`jtorctl` is a small Tor control-protocol library used by Bisq's Tor integration.

Copied from https://github.com/bisq-network/jtorctl with commit `30db998d855ff351c6f74783ff386f15304e875a`.

## Why this source is in the Bisq tree

Bisq previously consumed this code as a Bisq-maintained JitPack dependency pinned
to a Git commit. That kept the dependency stable, but it still left the build
dependent on JitPack publishing and resolving unsigned artifacts.

Keeping the module in source form reduces that supply-chain surface:

- The code is built directly as part of the Bisq 2 Gradle build.
- Reviewers can inspect changes in normal Bisq pull requests.
- Bisq does not need to trust a third-party build service for this internal
  dependency.
- The dependency no longer needs checksum-only verification metadata for
  unsigned JitPack artifacts.
- Required maintenance can happen in the same repository as the Tor integration
  that uses it.

This module should be treated as Bisq-owned infrastructure code. Keep changes
small, documented, and covered by tests where behavior changes.
