# Release build environment

## Required build JDK

CI must request one exact Azul Zulu JDK build for Java 21. The pinned build must be the same on every operating system
used by the build and compatibility workflows. The build JDK selected by CI and the JDK archives selected by the
toolchain resolver must identify the same Java update and Zulu distribution build.

The release environment check must compare the running Gradle process with the pinned Java update, Java vendor, Java
runtime build, Zulu distribution build, and Gradle version. The Java update matches when the version numbers of the
running JDK start with those of the pinned update; each other value must match exactly. The installers bundle the JDK
of the build as their runtime, so a release build needs this exact build. The GitHub Actions workflow check must
require an exact Java build in each Java setup step, must compare it with the pinned runtime build, and must require
the pinned runtime build to be a build of the pinned Java update.

The build environment check of `check` must compare the running Gradle process with the pinned Java update, Java
vendor, and Gradle version in the same way, and must accept every build of the pinned Java update. Azul builds an
update more than once, and an installer may offer only the latest build. The builds of one update compile the same
classes; they differ in their runtime, which matters for the installers only. The check must log the runtime build and
the Zulu distribution build of the running JDK.

The Java update version does not identify a unique Zulu build. A broader CI request can select another Zulu build,
which may report a different `java.version` and fail release verification. It can also change release build inputs
without a reviewed configuration change.

When the build JDK changes, update the CI requests, pinned environment properties, and toolchain URLs in the same
change set. See [the Gradle notes](../../../gradle/README.md) for the current values.
