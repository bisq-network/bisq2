# Tor bundle verification

The build downloads the Tor expert bundle of the pinned Tor version (`tor.version` in `gradle.properties`) and its
detached signature, and packages the bundle into the desktop app for macOS and Windows. Every user of these apps runs
the bundled `tor`. The build must therefore accept a bundle only if the Tor Browser developers signed it.

## Rules

1. The build trusts the public key file of the Tor Browser developers which the repository contains, and only if the
   fingerprint of its primary key equals the pinned fingerprint and the file contains no revocation signature for the
   primary key. Otherwise the build fails.
2. A subkey of this key can sign a bundle only if:
   - the key file contains no revocation signature for it, and
   - the primary key binds it with a subkey binding signature which verifies, and the key flags of the newest such
     binding signature allow signing data. If several of them have the newest creation time, all of them must allow
     signing data.

   Other subkeys are not trusted. An error while the build verifies a binding signature makes that binding count as not
   verified.
3. The build accepts a bundle only if its detached signature has the type of a binary document, was made by the primary
   key or by a trusted subkey, and verifies over the bytes of the bundle. Otherwise the build fails.

## Reasons

- The key file of the Tor Browser developers keeps old subkeys. It contains a signing subkey which Tor revoked in 2015
  as compromised, and whose old binding signature still verifies. Before rule 2, the build trusted every subkey with
  any verifiable signature of the primary key, also the revocation signature, so a bundle signed with the compromised
  subkey would pass.
- Before rule 2, an error while verifying a binding signature counted as a valid binding. A later key file with such a
  subkey would have been trusted without a check.
- Rule 1: a revocation of the primary key retires all its subkeys. The procedure for a key refresh downloads the key
  from Tor, so a refreshed key file would contain such a revocation, and the build must not trust any of its keys.
- Rule 2: a newer binding signature replaces an older one. If the primary key binds a subkey again without the right to
  sign, an older binding which allowed it must not count.
- Rule 3: a signature of another type, for example a key certification, does not sign the bytes of a file.

## Compatibility

- The bundle of Tor 15.0.24 is signed by subkey `022DA248432D2A0E0F54E65E316C1FACD62D07D9`, which is bound for signing
  and not revoked, so its verification passes. The procedure for a new Tor version and key refresh is in
  `build-logic/tor-binary/README.md`.

## Known limitations

- The build does not check whether a subkey had expired when it signed a bundle. A signature of an expired subkey
  passes. The pinned version is signed by a subkey which is valid until 2028.
- The build logic has no unit tests. The rules were checked with the verification task against the bundle of the
  pinned version.
