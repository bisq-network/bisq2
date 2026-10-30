# Tor bundle verification

The build downloads the Tor expert bundle of the pinned Tor version (`tor.version` in `tor/gradle.properties`) and its
detached signature, and packages the bundle into the apps. The stable releases have no expert bundle for Linux ARM64,
so a build on Linux ARM64 takes the bundle of the alpha release `tor.bundleVersion.linux_arm64`. Every user of the apps
on macOS, Windows and Linux runs the bundled `tor`, and users of bridges run its pluggable transports `lyrebird` and
`conjure-client`, unless they use an external Tor. The build must therefore accept a bundle only if the Tor Browser
developers signed it. The build also compares each bundle with the SHA-256 hash which `tor/gradle.properties` pins for
its platform, and checks that its tor has the version `tor.binaryVersion`, so that the alpha bundle of Linux ARM64
contains the same tor as the stable bundles.

These rules do not cover the sandy binary, which runs Tor in its sandbox and does not come from the bundle. The build
pins it by its SHA-256 hash for each platform and fails for another binary, as the section "Packaging" of
`tor/tor/src/main/resources/sandy/README.md` describes. Before the pins move to a new release of sandy, its signed tag
is verified, the release is built again from that tag and compared with the binary of the release, and the source
diff since the pinned release is reviewed, as the section "Updating sandy" of the same README describes.

## Rules

1. The build trusts the public key file of the Tor Browser developers which the repository contains, and only if the
   file contains exactly one primary key, the fingerprint of that key equals the pinned fingerprint, and the file
   contains no revocation signature for it. An armored key file must hold its key block alone: it begins with the
   armor header line of the block and holds nothing but white space after the tail line of the block. Otherwise the
   build fails.
2. The primary key, or a subkey which it binds, can sign a bundle only if the key was valid for signing at the creation
   time of the signature:
   - the self-signature of the primary key verifies, and for a subkey its binding signature by the primary key and its
     back-signature (type 0x19) by the subkey verify;
   - the signed key flags allow signing data;
   - neither the key nor the primary key had expired at that time.

   A subkey for which the key file contains a revocation signature is not trusted, whatever the reason and the time of
   the revocation. Other keys are not trusted.
3. The build accepts a bundle only if its detached signature has the type of a binary document, was made by a key which
   rule 2 accepts, and verifies over the bytes of the bundle. Otherwise the build fails.

## Reasons

- The key file of the Tor Browser developers keeps old subkeys. It contains a signing subkey which Tor revoked in 2015
  as compromised, and whose old binding signature still verifies. Before rule 2, the build trusted every subkey with
  any verifiable signature of the primary key, also the revocation signature, so a bundle signed with the compromised
  subkey would pass.
- Before rule 2, an error while verifying a binding signature counted as a valid binding. A later key file with such a
  subkey would have been trusted without a check.
- Rule 1: a revocation of the primary key retires all its subkeys. The procedure for a key refresh downloads the key
  from Tor, so a refreshed key file would contain such a revocation, and the build must not trust any of its keys. A
  file with a second primary key would let the build trust a key whose fingerprint it did not check. The parser of an
  armored file reads only its first key block, so text or a second key block before or after it was ignored without a
  word: whoever appends a new key of the Tor Browser developers to the file, or a reviewer who sees one there, would
  assume that the build uses it.
- Rule 2: the build evaluates the key at the creation time of the signature, so that the bundle of an older release,
  signed with a subkey which has expired since, can be built again. A revocation counts for every time, because the
  signer states the creation time, and the build cannot tell an older signature from one with an earlier time.
- Rule 3: a signature of another type, for example a key certification, does not sign the bytes of a file.

## Compatibility

- The bundle of Tor 15.0.25 is signed by subkey `022DA248432D2A0E0F54E65E316C1FACD62D07D9`, which is bound for signing
  and not revoked, so its verification passes. The Linux ARM64 bundle of the alpha release 16.0a14 is signed by the
  same subkey. Bundles of earlier releases which subkey `157432CF78A65729` signed still verify, although it expired on
  2026-10-26, because their signatures were made before it expired. The procedure for a new Tor version and key refresh
  is in `tor/build-logic/tor-binary/README.md`.

## Known limitations

- The signer states the creation time of a signature. Whoever holds the private key of an expired subkey can therefore
  still make a signature with an earlier time, which passes. A check at the current time instead would refuse the
  bundles of older releases once their subkey expires. The SHA-256 pin of each bundle limits the risk to bundles whose
  hashes a maintainer has pinned.
- The build reads the detached signature file of a bundle through its first armored block and ignores what follows
  it. Every signature of that block must pass rule 3, and the bundle is pinned by its SHA-256 hash, so this does not
  weaken the check.
