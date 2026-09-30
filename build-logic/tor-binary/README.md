# Tor bundle signature verification

The Tor plugin verifies expert bundles using the public key in
`src/main/resources/Tor_Browser_Developers_(signing_key).asc`.
`BisqTorBinaryPlugin` pins the primary fingerprint:

```text
EF6E286DDA85EA2A4BA7DE684E2C6E8793298290
```

Tor can introduce a signing subkey without changing that primary key. Updating
`tor.version` can therefore require refreshing the bundled public key as well.
For example, 15.0.24 uses subkey
`022DA248432D2A0E0F54E65E316C1FACD62D07D9`.

## Download and convert the official key

Run these commands from the repository root in the same Bash session. They
require `curl` and GnuPG (`gpg`). Stop if any command fails.

[Tor's verification instructions](https://support.torproject.org/tor-browser/getting-started/verifying-tor-browser/)
link to the official
[WKD key download](https://openpgpkey.torproject.org/.well-known/openpgpkey/torproject.org/hu/kounek7zrdx745qydx6p59t9mqjpuhdf).
Download that binary public key into a temporary directory:

```bash
tor_key_dir=$(mktemp -d)
tor_key_fingerprint=EF6E286DDA85EA2A4BA7DE684E2C6E8793298290
tor_key_url='https://openpgpkey.torproject.org/.well-known/openpgpkey/torproject.org/hu/kounek7zrdx745qydx6p59t9mqjpuhdf'

curl -fsSL "$tor_key_url" -o "$tor_key_dir/tor-key.pgp"
```

Convert the download to the ASCII-armored public-key format used by the
resource file:

```bash
gpg --batch --no-options --homedir "$tor_key_dir" \
    --armor --import-options import-export --import \
    < "$tor_key_dir/tor-key.pgp" > "$tor_key_dir/tor-key.asc"
```

The output starts with `-----BEGIN PGP PUBLIC KEY BLOCK-----`.
`import-export` writes the processed key to standard output without storing
it in the keyring. The temporary GnuPG home isolates this procedure from
your personal keys and configuration. Use this direct export of the
official key; merging the old bundled key first retains obsolete subkeys
and certification metadata and produces a different file.

## Check the primary key and signing subkeys

Inspect the downloaded key before using it:

```bash
gpg --batch --no-options --homedir "$tor_key_dir" \
    --show-keys --with-fingerprint --with-subkey-fingerprint \
    "$tor_key_dir/tor-key.asc"
```

Confirm that there is exactly one primary key and its fingerprint matches
`EF6E286DDA85EA2A4BA7DE684E2C6E8793298290`. The display name alone is not
sufficient. A different primary fingerprint requires a separate trust review.

Import the key into the temporary keyring and check its certification and
subkey-binding signatures:

```bash
gpg --batch --no-options --homedir "$tor_key_dir" \
    --import "$tor_key_dir/tor-key.asc"
gpg --batch --no-options --homedir "$tor_key_dir" \
    --check-sigs "$tor_key_fingerprint"
```

GnuPG marks successfully verified signatures with `sig!`. The release
signing subkey must have a valid binding signature from the pinned primary key.

## Verify the expert bundle

Read the release version from `gradle.properties`. Set `tor_platform` to
the bundle to check: `macos-aarch64`, `macos-x86_64`, `linux-x86_64`, or
`windows-x86_64`.

```bash
tor_version=$(sed -n 's/^tor\.version=//p' gradle.properties)
tor_platform=macos-aarch64
tor_bundle="tor-expert-bundle-${tor_platform}-${tor_version}.tar.gz"
tor_bundle_url="https://archive.torproject.org/tor-package-archive/torbrowser/$tor_version/$tor_bundle"

curl -fsSL "$tor_bundle_url" -o "$tor_key_dir/$tor_bundle"
curl -fsSL "$tor_bundle_url.asc" -o "$tor_key_dir/$tor_bundle.asc"

gpg --batch --no-options --homedir "$tor_key_dir" --no-auto-key-retrieve \
    --status-fd 1 --verify "$tor_key_dir/$tor_bundle.asc" "$tor_key_dir/$tor_bundle"
```

Require a successful exit and a `Good signature` result. The `VALIDSIG`
status line identifies the signing subkey first and its primary fingerprint
last; the latter must match the pinned fingerprint above. A warning about
the key not being personally certified is expected in this temporary keyring.
The primary fingerprint check establishes which key is trusted here.

For a key refresh, also repeat the bundle check with the previous release
version to confirm its signing subkey remains available.

## Install the verified ASCII key and run the build check

After the fingerprint and signature checks succeed, replace the resource
and run Bisq's verifier on the bundle for the build machine's platform:

```bash
cp "$tor_key_dir/tor-key.asc" \
    'build-logic/tor-binary/src/main/resources/Tor_Browser_Developers_(signing_key).asc'
./gradlew :network:tor:tor:verifyTorBinary
```

The Gradle task must finish successfully. Keep the pinned primary fingerprint
unchanged when refreshing its subkeys. Never bypass a failed signature check.
