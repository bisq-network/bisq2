# Gradle Notes

## Gradle Wrapper Verification

`verifyGradleWrapperSecurity` (part of the root `check`) and the CI workflow compare `gradlew`, `gradlew.bat`,
`gradle/wrapper/gradle-wrapper.jar` and `gradle/wrapper/gradle-wrapper.properties` with the approved checksums in
`gradle/wrapper/gradle-wrapper.sha256`. The task also requires `distributionUrl` to point to `services.gradle.org` for
`releaseBuild.gradleVersion` with a pinned `distributionSha256Sum`.

The checksums cover the committed bytes. `.gitattributes` disables line-ending conversion for `gradlew.bat` (`-text`),
which is committed with CRLF line endings as Gradle generates it; the other files are committed with LF. Do not
renormalize these files.

After a reviewed wrapper change, for example `./gradlew wrapper --gradle-version <version>`, recompute and review the
checksums and update `gradle/wrapper/gradle-wrapper.sha256`, `distributionSha256Sum` and `releaseBuild.gradleVersion`
in the same change:

```bash
sha256sum gradlew gradlew.bat gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties
```

A checksum mismatch means the working copy differs from the approved bytes. Review the difference; do not update the
checksum file to match an unreviewed file.

See [the specification](../docs/specifications/development/gradle-wrapper-verification.md).

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
including the root build and the `build-logic`, `apps`, `apps/desktop`, `network`, `tor`, and `tor/build-logic`
included builds.

The audit runs in one Gradle invocation through dependencies on the included builds' tasks. Each build keeps its own
`.gradle/dependency-verification/inventory.tsv` relative to its build root. These inventories survive `clean` and
`cleanAll`, which can run after included builds have already generated or reused their inventories. Unchanged
inventories are `UP-TO-DATE`; they are regenerated when resolved modules, direct dependency scopes, artifact names or
contents, verification metadata, or keyring change, or when an inventory is removed or modified. Java source changes
alone do not invalidate them.

Gradle still resolves and verifies dependencies when evaluating the inventory inputs. This keeps graph changes from
properties, resolution rules, dynamic versions, and local repositories visible without starting additional Gradle
processes. The signature policy remains part of `check` and is checked on each invocation. `--refresh-dependencies`,
`--write-verification-metadata`, and `--rerun-tasks` force inventory generation. The audit fails when the verification
mode is `off` or `lenient`, whether set with `--dependency-verification` or the `org.gradle.dependency.verification`
property (for example in `~/.gradle/gradle.properties`), because Gradle would not verify artifacts in those modes.

IDE import can also resolve `-sources.jar` and `-javadoc.jar` artifacts that normal builds and
`resolveAndVerifyDependencies` do not request. When adding a dependency, include verification metadata for these IDE
artifacts after verifying their signatures or checksums. A successful build alone does not verify that IDE import has
all required metadata.

### Sources and Javadoc requested by the IDE

`--write-verification-metadata` records artifacts requested during the run; it does not request additional classifiers.
Refreshing signing keys or regenerating the signature report also does not resolve sources or Javadoc archives.

To generate their metadata for a specific dependency, run this command from the repository root:

```bash
./gradlew :resolveIdeArtifactsForVerification \
  --init-script gradle/resolve-ide-artifacts.gradle \
  -PverificationDependency=org.testfx:openjfx-monocle:21.0.2 \
  --write-verification-metadata pgp,sha256
```

The [init script](resolve-ide-artifacts.gradle) registers the task for this invocation. Replace the coordinate with
`group:name:version` for another dependency; a comma-separated list resolves several dependencies. The helper requests
the classifiers in `-PverificationClassifiers`, by default `sources,javadoc`; each requested archive must be published.
Omit `--write-verification-metadata pgp,sha256` to verify them against the existing metadata instead.

Review the generated diff and verify new signatures or checksums before committing. Metadata generation records
downloaded artifacts; it does not independently establish their trustworthiness. The helper handles only the listed
dependencies; `resolveAndVerifyDependencies` still does not automatically resolve IDE artifacts for every dependency.

### Verifying and refreshing build dependencies

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

Gradle checks every checksum which the metadata declares for an artifact, also for a signed artifact. A checksum is the
only verification of an artifact if the artifact has no signature or if every key which signed it is ignored. The policy
treats a checksum as fallback if it has a reason (Gradle writes `Artifact is not signed`, `A key couldn't be downloaded`
or `PGP signature verification failed!`) or if neither a `pgp` entry nor a `trusted-key` entry for the module names a
key which is not ignored. This applies to every checksum kind which Gradle verifies (`md5`, `sha1`, `sha256` and
`sha512`), also if the metadata of this repository uses only `sha256` today. It checks every entry in
`gradle/verification-metadata.xml`, not only the resolved dependency graph, because Gradle also verifies parent POMs,
imported BOMs and artifacts of other platforms against it. Entries for versions no build uses therefore also need an
allowlist entry until they are removed.

Gradle writes a reason once and keeps it when it regenerates the metadata. After `refreshDependencyVerificationKeyring`
adds a key, remove the key from `ignored-keys`, delete the component entries of the artifacts it signs and run
`resolveAndVerifyDependencies --write-verification-metadata pgp,sha256` so Gradle writes them again with the current
result. Do not remove reasons by hand: the policy cannot see which key signed an artifact, so a wrongly removed reason
can hide a checksum-only artifact.

`ignored-keys` is only for signing keys that no key server publishes. `verifyDependencySignaturePolicy` fails when a
global or artifact-level ignored key is in `gradle/verification-keyring.keys`. Gradle never verifies a signature made
with an ignored key, also when the keyring contains the key, so such an entry reduces the artifacts it signs to their
checksums without need.

Executable Maven artifacts, such as `protoc` and `protoc-gen-grpc-java`, must also have explicit entries in
`gradle/verification-metadata.xml` for the resolved classifier. `verifyDependencySignaturePolicy` fails when a resolved
executable is covered only by a broad trusted-key rule or when executable metadata remains for an older unresolved
version of the same tool.

The audit's functional checks use isolated composite builds and a local Maven repository. With Python 3 and the
Gradle wrapper distribution already available, run them without accessing remote artifact repositories:

```bash
python3 gradle/tests/dependency_verification_test.py -v
```
