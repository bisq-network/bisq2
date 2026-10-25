# jsocks

`jsocks` provides SOCKS support used by Bisq's networking and Tor integration.

Copied from https://github.com/bisq-network/jsocks with commit `8bfc68c58d443b0ea03a8e8ec352e120fd020127`.

## Why this source is in the Bisq tree

Bisq previously consumed this code as a Bisq-maintained JitPack dependency pinned
to a Git commit. That made the dependency reproducible at the version level, but
the artifacts resolved from JitPack were unsigned and had to be accepted through
checksum-only dependency verification.

Keeping the module in source form reduces that supply-chain surface:

- The code is built directly as part of the Bisq 2 Gradle build.
- Reviewers can inspect changes in normal Bisq pull requests.
- Bisq does not need to trust a third-party build service for this internal
  dependency.
- The dependency no longer needs checksum-only verification metadata for
  unsigned JitPack artifacts.
- Required maintenance can happen in the same repository as the networking code
  that uses it.

This module should be treated as Bisq-owned infrastructure code. Keep changes
small, documented, and covered by tests where behavior changes.
