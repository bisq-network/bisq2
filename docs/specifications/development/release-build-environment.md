# Release build environment

## Required build JDK

The build requires an Azul Zulu JDK of the Java feature version of `releaseBuild.javaVersion` (21), at or above that
version. `releaseBuild.javaVersion` is a minimum, not one exact build. Azul builds an update more than once, for example
21.0.13 as 21.0.13+9 and as 21.0.13.0.3+1, and not for every platform at the same time, so the operating systems of
one build may use different Zulu builds.

The build environment check of `check`, and the release environment check, must compare the running Gradle process
with the Java vendor, the Java feature version, the minimum Java version, and the Gradle version. The Java version
passes when its feature version equals that of the minimum and its version numbers, compared one by one with a missing
number counting as 0, are at or above those of the minimum. The build number is not compared. The check must log the
runtime build and the Zulu distribution build of the running JDK.

The installers bundle the JDK of the build as their runtime. Release builds use the newest Azul Zulu build of the
feature version which exists for the operating system. When a Zulu build fixes a flaw that a release must not ship
with, raise `releaseBuild.javaVersion` to it.

CI requests the feature version with `check-latest: true` in each Java setup step, so every runner installs the newest
Zulu build for its platform. The GitHub Actions workflow check must require the feature version of
`releaseBuild.javaVersion` and `check-latest: true` in each Java setup step.

Docker base images use the `azul/zulu-openjdk` tag of the feature version (`<feature>` or `<feature>-jre`), pinned by
digest. The toolchain resolver must provide Zulu builds at or above `releaseBuild.javaVersion`. When the minimum is
raised, update the toolchain URLs in the same change set. See [the Gradle notes](../../../gradle/README.md) for the
current values.
