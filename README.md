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
   git clone --recurse-submodules https://github.com/bisq-network/bisq2.git
   ```
   ```bash
   cd bisq2
   ```

2. **Install Dependencies:**
   Bisq requires JDK 21. See our [Installation Guide](./docs/dev/build.md) for detailed instructions.

3. **Update to latest GitHub version:**
   ```bash
   git pull --recurse-submodules
   ```

4. **Build desktop client distribution:**
   ```bash
   ./gradlew :apps:desktop:desktop-app:installDist
   ```

5. **Run desktop client:**
   ```bash
   ./apps/desktop/desktop-app/build/install/desktop-app/bin/desktop-app
   ```

6. **Run desktop client with custom data directory:**
   ```bash
   ./apps/desktop/desktop-app/build/install/desktop-app/bin/desktop-app --data-dir=<data_dir>
   ```

**For Windows environments**: replace `./gradlew` with `gradlew.bat`.

### Tor Submodule

The directory `tor/` is the Git submodule [tor](https://github.com/bisq-network/tor), the Tor build of Bisq. Bisq 2
records one commit of it, and `tor/` must contain this commit.

**Check out the recorded commit.** A clone without `--recurse-submodules`, a `git pull` without it, and a branch switch
leave `tor/` empty or at another commit. Then run:
```bash
git submodule update --init
```
This command does not take new commits of the tor repository. It checks out the commit which Bisq 2 records, also when
`tor/` has a newer commit checked out.

If `tor/` still contains files of a checkout from before the submodule, for example `tor/build` of an earlier build, the
command fails with `destination path 'tor' already exists and is not an empty directory`. Remove the directory first
(`rm -rf tor`) and run the command again.

To let `git pull` and `git switch` update `tor/` automatically, run once: `git config submodule.recurse true`.

**Update Bisq 2 to a newer commit of the tor repository:**
1. Fetch the new commits into `tor/` and check out the new commit there:
   ```bash
   git -C tor fetch origin
   ```
   ```bash
   git -C tor checkout <commit>
   ```
To use the main branch:
   ```bash
   git -C tor checkout origin/main
   ```
   `git submodule update --remote tor` does both for the newest commit of the `main` branch.
2. Review the changes which the update brings:
   ```bash
   git diff --submodule=log tor
   ```
3. Build and test, then commit the new commit of `tor/` in Bisq 2:
   ```bash
   git add tor
   ```
   ```bash
   git commit
   ```

**Change the Tor code.** `tor/` is a repository of its own. Commit the change in `tor/` on a branch and push it to the
tor repository. Then commit the new commit of `tor/` in Bisq 2 as in step 3. Push the tor commit first, otherwise other
clones of Bisq 2 cannot check it out.

**Local tor repository (temporary).** As long as the tor repository is not published, a checkout which uses a local
repository `tor` next to the Bisq 2 checkout sets the URLs once by hand. Use relative paths, so that the URLs also work
when another machine (for example a virtual machine) sees the directories under another absolute path:
```bash
git config submodule.tor.url ../tor
```
```bash
git -C tor remote set-url origin ../../tor
```
Git resolves the first URL from the Bisq 2 checkout and the second from `tor/`. Git refuses local submodule URLs by
default, so add `-c protocol.file.allow=always` to submodule commands which clone, for example
`git -c protocol.file.allow=always submodule update --init`. Do not run `git submodule sync` in such a checkout: it
writes the URL of `.gitmodules`, which does not exist yet, into both places.

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
