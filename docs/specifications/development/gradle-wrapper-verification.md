# Gradle wrapper verification

## Scope and policy

The root build's `check` must verify the Gradle wrapper files (`gradlew`, `gradlew.bat`,
`gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`) against approved SHA-256 checksums
committed in the repository. It must also require that the wrapper distribution URL points to the official Gradle
distribution server for the pinned release Gradle version, and that the distribution checksum is pinned.

Continuous integration must perform the same checksum comparison before it runs the wrapper.

## Verified bytes are committed bytes

The approved checksums apply to the exact bytes of the committed wrapper files. Checking out the repository must not
transform these files: no line-ending conversion, filtering, or keyword expansion may apply to them, regardless of the
Git configuration of a developer machine or a build runner.

`gradlew.bat` is committed with CRLF line endings, as Gradle generates it and as `cmd.exe` expects, and is excluded from
text normalization. `gradlew`, the wrapper properties file, and the checksum file are committed with LF line endings.

Rationale: if the verified bytes depended on checkout-time conversion, the same commit would pass on one machine and fail
on another, and a reviewer could not determine from the repository content which bytes the checksums approve. This
happened when the checksum for `gradlew.bat` was approved on its CRLF form while the committed blob was LF-normalized;
a local Git attribute override then produced a false verification failure.

## Updating checksums

Checksums may change only together with a reviewed change of the wrapper files, such as a Gradle version upgrade. The
pinned release Gradle version, the wrapper distribution URL, the distribution checksum, and the file checksums must
change in the same change set. A checksum update without a corresponding reviewed wrapper change must be rejected.

See [the Gradle notes](../../../gradle/README.md) for the maintenance commands.
