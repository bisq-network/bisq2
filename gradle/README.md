# Gradle Notes

## Dependency Verification

Bisq 2 uses Gradle dependency verification with:

- `verify-metadata`
- `verify-signatures`
- armored keyring format

The committed verification files are:

- `gradle/verification-metadata.xml`
- `gradle/verification-keyring.keys`
- `gradle/dependency-checksum-fallback-allowlist.tsv`

The root `resolveAndVerifyDependencies` task resolves every resolvable configuration in the Bisq 2 composite build,
including the root build and the `build-logic`, `apps`, `apps/desktop`, `network`, and `network/tor` included builds.

To verify the current metadata without rewriting it:

```bash
./gradlew resolveAndVerifyDependencies
./gradlew verifyDependencySignaturePolicy
```

To refresh dependency verification metadata:

```bash
./gradlew refreshDependencyVerificationKeyring
./gradlew resolveAndVerifyDependencies --write-verification-metadata pgp,sha256
./gradlew verifyDependencySignaturePolicy
./gradlew dependencySignatureReport
```

Refresh the keyring first so newly available signing keys are present before Gradle rewrites
`gradle/verification-metadata.xml`. This avoids adding checksum fallbacks for artifacts whose signatures can now be
verified.

`refreshDependencyVerificationKeyring` wraps:

```bash
./gradlew help --refresh-keys --export-keys
```

It exports the armored keyring to `gradle/verification-keyring.keys`. The binary keyring
`gradle/verification-keyring.gpg` is not used.

Some Gradle/Bouncy Castle combinations may write the armored keyring and then fail while finishing key export. The
wrapper task accepts that case only if `gradle/verification-keyring.keys` was updated.

Checksum fallback artifacts must be reviewed in `gradle/dependency-checksum-fallback-allowlist.tsv`:

```text
<group:name:version>\t<artifact-file-name>\t<review-rationale>
```

Keep the allowlist exact and sorted. `verifyDependencySignaturePolicy` fails when a fallback artifact is missing from
the allowlist, when an allowlist entry is stale, or when an entry has no rationale.

Executable Maven artifacts, such as `protoc` and `protoc-gen-grpc-java`, must also have explicit entries in
`gradle/verification-metadata.xml` for the resolved classifier. `verifyDependencySignaturePolicy` fails when a resolved
executable is covered only by a broad trusted-key rule or when executable metadata remains for an older unresolved
version of the same tool.
