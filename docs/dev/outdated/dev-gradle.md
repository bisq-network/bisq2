_Note: This document is outdated_

## How to update the Gradle Wrapper

The wrapper can update itself using:

```
./gradlew wrapper --gradle-version 7.4.1 \
    --distribution-type all \
    --gradle-distribution-sha256-sum a9a7b7baba105f6557c9dcf9c3c6e8f7e57e6b49889c5f1d133f015d0727e4be
```

- `--gradle-version`: the version you wish to update to
- `--distribution-type`: `bin` or `all`. Use the `all` distribution to provide the sources needed for the IDE for
  code-completion and the ability to navigate the Gradle source code.
- `--gradle-distribution-sha256-sum`: the checksum[^1] for the chosen distribution

## Managing dependency versions

Two gradle constructs[^2] are used: version catalog and platform.

The **version catalog**[^3] defines a list of possible dependencies and their versions. Subprojects can reference them
using type-safe accessors. Only direct dependencies can make use of the catalog, transitive dependencies not. Unless
using keywords[^5] such as `strictly` or `require`, the versions declared in the catalog act as a soft declaration,
meaning the actual version used can be overridden during conflict resolution, usually when transitive dependencies are
involved.

The version catalog is maintained in `gradle/libs.versions.toml`.

The **platform**[^6] applies dependency constraints[^4] on the dependency graph, thus affecting the entire dependency
resolution process. This affects both direct and transitive dependencies. Similar to the version catalog, the priority
of a version declaration can be influenced using various keywords[^5].

The platform dependency constraints are maintained in `platform/build.gradle.kts`.

### Usage

To combine the strengths of a version catalog

- central version definition
- type-safe accessors
- usable by all subprojects in the `plugins` and `dependencies` sections of their `build.gradle`

with those of a platform

- defined constraints enforced across all dependencies (direct and transitive)

we can use the catalog within the platform definition, as described at the end of this section[^2].

In addition, we can use rich versions[^5] for more nuanced version definitions.

To summarize:
- Use the version catalog to define dependencies usable across all subprojects.
- Use the platform, by referencing dependencies from the catalog, to define constraints affecting all dependencies,
  direct and transitive.

## Package binaries

Platform-specific binaries and installers can be generated with

```
./gradlew :apps:desktop:desktop-app-launcher:generateInstallers
```

Optionally add the `--info` flag at the end for a more verbose output.

Requirements[^7] for building on:
- Fedora: `rpm-build`
- Debian: `fakeroot`
- macOS: Xcode command line tools are required when the `--mac-sign` or `--icon` options are used.
- Windows: WiX 3.0 or later is required

## Notes on java modularization

The Bisq 2 application currently does not use and expose java modules.

To achieve that, there are two main challenges:
- **Integrating non-modular dependencies**, like `i2p`, `jtorctl` or `jsocks`. One way to do this is to add java module
  support in the upstream dependencies themselves, then use the modularized version. Another way is to use gradle
  plugins[^8] that can programmatically generate and customize java modules from standard non-modular libs.
- **Integrating non-conventional already-modular dependencies**. Partly due to their own dependencies,
  some libraries have a non-conventional approach to java modularization. To integrate them in a modular setup, Bisq 2
  would have to define extra exports for the non-accessible packages. One way to do this is by adding `--add-exports` as
  compile args:

```
// See https://stackoverflow.com/a/53527897
// Format: --add-exports $MODULE/$PACKAGE=$READING_MODULE
compileJava {
    options.incremental = true
    // jfoenix lib (already a java module) doesn't export two packages => use add-exports to export them to this module
    options.compilerArgs.addAll([
            "--add-exports", "com.jfoenix/com.jfoenix.adapters=bisq.desktop",
            "--add-exports", "com.jfoenix/com.jfoenix.controls.base=bisq.desktop"
    ])
}
```

[^1]: https://gradle.org/release-checksums/
[^2]: https://docs.gradle.org/7.4.1/userguide/platforms.html#sub:platforms-vs-catalog
[^3]: https://docs.gradle.org/7.4.1/userguide/platforms.html#sub:version-catalog-declaration
[^4]: https://docs.gradle.org/current/userguide/dependency_management_terminology.html#sub:terminology_dependency_constraint
[^5]: https://docs.gradle.org/7.4.1/userguide/rich_versions.html
[^6]: https://docs.gradle.org/current/userguide/java_platform_plugin.html
[^7]: https://docs.oracle.com/en/java/javase/17/jpackage/packaging-overview.html#GUID-786E15C0-2CE7-4BDF-9B2F-AC1C57249134
[^8]: https://github.com/jjohannes/extra-java-module-info