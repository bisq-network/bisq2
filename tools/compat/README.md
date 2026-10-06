# Compatibility tests with the last release

The unit tests of the main build use only the code of this version. They cannot show that this version still accepts
the data of the last release, or that the last release accepts the data of this version. The tools in this directory
run the real code of both versions and compare what each version does with the data of the other one.

They find failures which a tester easily overlooks, because the screen shows no error:

- A stricter check of this version rejects honest data of the last release, or a store skips entries of the last
  release when it loads them.
- The two versions compute another proof of work payload or another cost factor for the same request.
- A node of the last release does not accept a request of this version, or the copy which it relays.
- The updater of the last release does not accept the new release.

The last release is called the baseline. It is the newest release tag (see `docs/dev/backward-compatibility.md` and
the project instructions on backward compatibility).

## Tools

| Script | Purpose |
|---|---|
| `baseline.sh` | Builds a release tag and runs Gradle in it |
| `run-chain.sh` | Runs the compatibility chain |
| `old-updater-replay.sh` | Checks a release with the old updater |
| `run-mixed-network.sh` | Runs nodes of both versions together |

### `baseline.sh`

`tools/compat/baseline.sh <tag> [gradle arguments...]` exports the tag with `git archive` into
`$BISQ_BASELINE_ROOT/<tag>`. The default is `~/.cache/bisq2/baselines`. It exports a tag only once and keeps the
build outputs for later runs. It creates no git worktree.

The directory must be outside of the repository: the build of the baseline reads its commit hash with
`git rev-parse` and would otherwise get the commit of the checkout.

If `baselines/<tag>/init.gradle` exists, Gradle runs with this init script. It adds the test sources of
`baselines/<tag>/<project>/src/test/java` to the project of the same name in the baseline build. The exported tree
stays unchanged. Gradle properties named `bisq.compat.*` reach these tests as system properties.

The tag must exist locally: `git fetch origin tag v2.1.13 --no-tags`.

### `run-chain.sh`

`tools/compat/run-chain.sh [<tag>] [<exchange directory>]` runs four steps. Each step reads the files of the step
before it from the exchange directory, by default `build/compat/<tag>`.

| Step | Code | What it does |
|---|---|---|
| 1 | baseline | Creates requests and persists stores |
| 2 | this checkout | Processes them, creates own requests |
| 3 | baseline | Processes and relays them |
| 4 | this checkout | Processes the relayed copies |

Step 1 creates what an honest node of the baseline creates, as its services and its REST API create it: a user profile
and its refresh, public chat messages with a reply, a citation, a mention, an emoji, an edit and a removal, all six
reactions and the removal of one, offers with market price, float price and fixed price, fixed and range amounts and a
custom payment method, the removal of an offer and a reaction to an offer, authorized data of an oracle node, and
mailbox messages with a removal. It persists the stores and records the order of the requests, so that the later steps
can derive their expected results from it.

Step 2 checks with the code of this checkout:

- The proof of work payload and the cost factor of each request are the same as in the baseline.
- A new node accepts the requests of the baseline before the cutoff date of signature V1, and rejects them from the
  cutoff date on.
- A new node loads the persisted stores of the baseline without losing an entry. From the cutoff date on, only the
  mailbox removals stay.
- An upgraded oracle node publishes the data of the baseline again under its authorized key.
- An upgraded user refreshes, publishes and removes data, and the step writes these requests for step 3.

Step 3 checks with the code of the baseline:

- The baseline computes the same proof of work payload and cost factor for the requests of this checkout.
- A node of the baseline accepts these requests. It keeps the data of the oracle migration in both orders of arrival.
  If the migrated add request arrives first, it rejects the removal. This is the cause of the open point
  `inventory-migration-removals-loop` of the release test plan of v2.1.14.
- A node of the baseline accepts its own requests after a node of this checkout relayed them.
- A node of the baseline relays the requests of this checkout and drops the fields which it does not know.

Step 4 checks with the code of this checkout that a new node accepts these relayed copies before the cutoff date and
rejects them from the cutoff date on. A copy which a node held across the cutoff date comes back with the complete
request.

The expected results come from the specifications in `docs/specifications/network/`, mainly
`storage-request-signatures.md`, `storage-refresh-requests.md`, `storage-ownership.md` and `message-cost.md`. The
tests do not check the open points of these specifications.

The chain runs in dev mode, as a local test network does. The authorized key of the oracle node is the first dev key
of `AuthorizedPubKeys`, because nodes of this version accept authorized data only from the static oracle keys.

A run takes about 10 seconds when the builds of both versions are up to date. The first run exports and compiles the
baseline, which takes a few minutes.

### `old-updater-replay.sh`

Users of the last release update with its in-app updater. The script runs the verification code of that updater
(`DownloadItem`, `UpdaterService.download` and `DownloadedFilesVerification` of v2.1.13):

- `tools/compat/old-updater-replay.sh keys [<key dir>]` checks that the old code reads the key files of this checkout,
  for example after a key renewal.
- `tools/compat/old-updater-replay.sh signature <file> [<key id>]` verifies a detached signature of a test file with
  the key file of this checkout and with the key file which the old version has in its resources. The release manager
  creates the signature with `gpg --local-user <key id> --detach-sign --armor <file>`.
- `tools/compat/old-updater-replay.sh release <version> [<staging dir>] [<extensions>]` downloads each installer like
  the old updater and verifies it. Without a staging directory it uses the GitHub release and
  `https://bisq.network/pubkey/`. A staging directory contains the release assets, and the web page keys in the
  subdirectory `pubkey`. A local HTTP server serves it, so the release can be checked before the upload.
  Extensions default to `.dmg,.exe,.deb,.rpm`.

`./gradlew verifyReleaseReadiness` checks the release with `gpg`. This script adds the check with the code of the old
version, which reads the key files with its own version of Bouncy Castle.

### `run-mixed-network.sh`

`tools/compat/run-mixed-network.sh [<tag>] [<test filter>]` builds the distributions of `api-app` and `seed-node-app`
of the baseline and of this checkout, and runs the tests of `apps/e2e-tests`. Each test class starts its own network on
127.0.0.1:

- a seed node of each version and an api-app node of each version, which know both seed nodes;
- a fake price node in the test JVM, which all nodes use instead of the real price nodes;
- dev mode on all nodes, without authentication of the API.

The tests use only the REST and WebSocket API of the nodes, with the routes which both versions have. They cover:

- `OffersAcrossVersionsTest`: offers with market, float and fixed price, with fixed and range amounts, reach the other
  version with the same terms. A removed offer disappears on the other version.
- `TradeAcrossVersionsTest`: a trade runs to the end in all four combinations of maker version and direction. Both
  sides must reach the final state and agree on the contract.

At the end, each test class stops its nodes and scans their logs with `scripts/log_anomaly_scan.py`. A finding of
level warning or error fails the test class. The data directories and logs stay in `build/e2e/<tag>/<test class>`.

Limits of the first version:

- No Tor and no mediator. The trades run without a mediator, which dev mode allows.
- All nodes use `devModeReputationScore`, because the network has no oracle node. This hides the check of the
  reputation of a seller.
- Both versions limit the publications of an author to 3 per second and 30 per minute. The client keeps 2.1 seconds
  between its publications.
- Four node JVMs need about 1.5 GB of memory.

## Layout

| Path | Content |
|---|---|
| `baselines/<tag>/init.gradle` | Adds the overlay to the baseline build |
| `baselines/<tag>/application/...` | Steps 1 and 3 |
| `baselines/<tag>/evolution/...` | Replay of the updater |
| `application/src/test/java/bisq/application/compat/<package>` | Steps 2 and 4 |
| `apps/e2e-tests/src/integrationTest/java/bisq/e2e` | Tests with nodes of both versions |

The package of steps 2 and 4 is the tag with `_` instead of `.`, for example `v2_1_13`. The task
`:application:compatTest` runs them. The task `test` excludes them, because they need the files of the steps before
them. Both sides compile with the normal build of their version, so a change of an API shows as a compile error.

## The next baseline

After a release, the chain should run against that release:

1. Copy `baselines/<last tag>` to `baselines/<new tag>` and adapt the sources to the API of the new tag. The old code
   then compiles against that tag.
2. Copy `application/src/test/java/bisq/application/compat/<last package>` to the new package. Adapt the expected
   results to what the new baseline does, for example to requests with signature V2.
3. Keep the old baseline while nodes of that version can still take part in the network.
