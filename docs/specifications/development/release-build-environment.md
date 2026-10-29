# Release build environment

## Required build JDK

CI must request one exact Azul Zulu JDK build for Java 21. The pinned build must be the same on every operating system
used by the build and compatibility workflows. The build JDK selected by CI and the JDK archives selected by the
toolchain resolver must identify the same Java update and Zulu distribution build.

The release environment check must compare the running Gradle process with the pinned Java update, Java vendor, Java
runtime build, Zulu distribution build, and Gradle version. Each value must match exactly. The GitHub Actions workflow
check must require an exact Java build in each Java setup step and must compare it with the pinned runtime build.

The Java update version does not identify a unique Zulu build. A broader CI request can select another Zulu build,
which may report a different `java.version` and fail release verification. It can also change release build inputs
without a reviewed configuration change.

When the build JDK changes, update the CI requests, pinned environment properties, and toolchain URLs in the same
change set. See [the Gradle notes](../../../gradle/README.md) for the current values.
