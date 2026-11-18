# Release build environment

## Required build JDK

The build requires an Amazon Corretto JDK of the Java feature version of `releaseBuild.javaVersion` (21), at or above
that version. `releaseBuild.javaVersion` is a minimum, not one exact build. Later security updates of the same
feature version are allowed on every platform.

The minimum is `21.0.13.1`, provided by Corretto `21.0.13.1.2`. This release fixes CVE-2026-114938 (Java4Shell), a memory
corruption defect in the JVM's accelerated Base64 decoder on x86-64 CPUs with AVX-512 VBMI. The runtime must contain
the fix; a patched compiler alone does not protect an application running on an older JVM. See the
[Corretto release notes](https://github.com/corretto/corretto-21/blob/21.0.13.1.2/CHANGELOG.md).

The build environment check of `check`, and the release environment check, must compare the running Gradle process
with the Java vendor, the Java feature version, the minimum Java version, and the Gradle version. The Java version
passes when its feature version equals that of the minimum and its version numbers, compared one by one with a missing
number counting as 0, are at or above those of the minimum. The build number is not compared. The check must log the
runtime build and the Corretto distribution build of the running JDK.

The installers bundle the JDK of the build as their runtime. Release builds use the newest Amazon Corretto build of
the feature version which exists for the operating system. When a Corretto build fixes a flaw that a release must not
ship with, raise `releaseBuild.javaVersion` to it. The Tor submodule must use the same vendor and minimum version.

CI requests the feature version with `distribution: corretto` and `check-latest: true` in each Java setup step, so
every runner installs the newest Corretto build for its platform. The GitHub Actions workflow check must require
the feature version of `releaseBuild.javaVersion` and `check-latest: true` in each Java setup step.

Docker base images use `amazoncorretto:<feature>-al2023` for the build and `amazoncorretto:<feature>-al2023-headless`
for the runtime, pinned by image index digest. Both architectures of both images must contain a runtime at or above
the minimum; changing the tag alone does not update a pinned image. The toolchain resolver must provide Corretto
builds at or above `releaseBuild.javaVersion`. When the minimum is raised, update the toolchain URLs in the same
change set. See [the Gradle notes](../../../gradle/README.md) for the
current values.
