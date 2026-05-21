# Java runtime packaging analysis

Assessed on 2026-05-21 against the current Bisq 2 Gradle workspace.

## Executive summary

Bisq can move away from shipping the full JDK with the desktop installer. The current installer path does not require
compiler or packaging tools at runtime; it uses `jpackage` during the build and then launches the desktop application on
the classpath with the application jars and OpenJFX jars in the app `lib` directory.

The lowest-risk change is to keep the current classpath launch and replace the bundled JDK image with a pinned Java 21
JRE/runtime image for the target OS and architecture. This should be a packaging change, not an application code change.
It still needs cross-platform installer testing, but it does not require JPMS migration.

The larger benefit comes from a `jlink` runtime image. A local prototype for the desktop app plus the spawned BI2P and
webcam Java processes produced a 67 MB runtime image, compared with a 342 MB Zulu JDK 21.0.11 image on this macOS
machine. This is feasible while the application remains on the classpath, but it requires better runtime-module analysis
and smoke tests because `jdeps` cannot see every reflective, service-provider, crypto, locale, and native-integration
path.

Recommendation: split the packaging JDK from the bundled runtime image first, then choose between a pinned vendor JRE
for the quickest low-risk win and a generated `jlink` runtime for the best size/security result. Do not make a full JPMS
migration a prerequisite for this work.

## Current packaging state

- `apps/desktop/desktop-app-launcher` applies `bisq.gradle.packaging.PackagingPlugin`.
- `PackagingPlugin` obtains an Azul Java 21 toolchain through `JavaToolchainService`.
- The `generateInstallers` task sets both `jdkDirectory` and `runtimeImageDirectory` to the same toolchain directory.
- `JPackageTask` calls `<jdkDirectory>/bin/jpackage`.
- `PackageFactory` always passes `--runtime-image <runtimeImageDirectory>` to `jpackage`.
- `build-logic/toolchain-resolver` pins Zulu JDK download URLs, not JRE URLs.
- The launcher comment says "JRE", but the build currently provides the full JDK image.

The current implementation therefore conflates two separate concerns:

1. The build-time JDK needed to run `jpackage`, `jdeps`, and `jlink`.
2. The runtime image copied into the installer and used on end-user machines.

These should be separate Gradle inputs.

## Local measurements

Measurements are from the local macOS/aarch64 workspace and should be repeated on each release platform before making a
release decision.

| Item | Size |
| --- | ---: |
| Zulu JDK 21.0.11 image used by the build | 342 MB |
| JDK `jmods` directory alone | 80 MB |
| JDK `lib/src.zip` | 52 MB |
| JDK `lib/ct.sym` | 10 MB |
| Desktop launcher `installDist` lib directory | 198 MB |
| Desktop launcher `installDist` jar count | 153 jars |
| Prototype `jlink` image for desktop main runtime | 65 MB |
| Prototype `jlink` image for desktop plus BI2P/webcam child processes | 67 MB |
| Prototype `jlink ALL-MODULE-PATH` image | 92 MB |

The `installDist` measurement is not the final compressed installer size. It also does not necessarily include every
resource wired only by `generateInstallers`, such as generated BI2P/webcam payload resources. The runtime savings are
still additive: replacing a 342 MB runtime with a smaller runtime directly reduces the app image contents before
installer compression.

## Feasibility: vendor JRE/runtime image

Feasibility: high.

The desktop app is not using `javac`, `javadoc`, `jlink`, `jpackage`, `jdeps`, `jshell`, or other JDK tools at runtime.
The build needs a JDK, but the installed app should only need a Java 21 runtime with `bin/java`, standard runtime modules,
the certificate store, and platform native libraries.

Expected engineering work:

- Add a separate runtime image input to the packaging extension or plugin.
- Keep the current Azul JDK toolchain as the build-time `jpackage` tool provider.
- Add a pinned per-OS/per-architecture runtime image source, or generate a runtime image from the pinned JDK.
- Point `JPackageTask.runtimeImageDirectory` at that runtime image instead of the build JDK.
- Add release verification that records the runtime vendor, version, architecture, checksum, and `java -version` output.
- Run full installer smoke tests on macOS, Windows, and Linux.

Expected benefit:

- Removes compiler, debugger, packaging, source, header, and module-definition artifacts from the shipped app.
- Reduces download size and installed size.
- Reduces the number of executable tools available inside the installed app bundle.
- Keeps behavior close to the current full-JDK packaging because the runtime still contains the normal Java runtime
  modules.

Estimated effort: 3 to 7 engineering days, plus release-platform QA. The build change is small; the main work is pinning
runtime artifacts and proving the generated installers behave the same on all supported platforms.

## Feasibility: `jlink` runtime image

Feasibility: medium-high, with testing risk.

A custom `jlink` runtime can be used even if Bisq continues to launch on the classpath. The application jars do not need
to become JPMS modules first. `jdeps` can provide a starting module set, then the build can create a runtime image from
the pinned JDK and pass it to `jpackage` with `--runtime-image`.

The existing `JDepsTask` and `JLinkTask` classes are useful scaffolding, but they are not currently registered in the
active packaging path and are too narrow for the desktop app as-is. They need to account for:

- The desktop launcher app.
- The main desktop app jars.
- The BI2P shadow jar spawned by the desktop app.
- The webcam shadow jar spawned by the desktop app.
- Runtime-only needs not reliably visible to static analysis.

The local `jdeps` pass for the desktop launcher reported these direct JDK module dependencies:

```text
java.base
java.desktop
java.logging
java.management
java.naming
java.net.http
java.sql
java.xml
jdk.management
jdk.net
jdk.unsupported
```

The spawned BI2P and webcam shadow jars require additional modules:

```text
java.compiler
java.instrument
java.security.jgss
jdk.attach
jdk.httpserver
jdk.jdi
jdk.jfr
```

The prototype runtime also included conservative runtime modules such as `jdk.crypto.ec`, `jdk.localedata`, and
`jdk.charsets` because TLS, locale-sensitive UI/resources, and non-UTF charsets are common areas where static analysis
under-reports real runtime needs.

Useful `jlink` options:

```text
--no-header-files
--no-man-pages
--strip-debug
--compress zip-6
```

On this machine, `--compress zip-6` reduced the selected runtime from 123 MB to 67 MB. `zip-9` did not materially change
the measured size.

Estimated effort: 1 to 2 engineering weeks for a production-quality selected-module runtime, assuming no major
cross-platform regressions. Add more time if CI/release automation must build and test all runtime images.

## Important blockers and risks

### `bin/java` must remain available

The current desktop app starts child Java processes with:

- `apps/desktop/desktop/src/main/java/bisq/desktop/webcam/WebcamProcessLauncher.java`
- `network/network/src/main/java/bisq/network/p2p/node/transport/i2p/Bi2pProcessLauncher.java`

Both derive the executable from `System.getProperty("java.home")` and expect `bin/java` or `bin/java.exe`.

Therefore, do not use `jlink --strip-native-commands` unless those launch paths are refactored or another Java launcher
is provided. A stripped image without native commands has no `bin/java` and will break webcam and embedded BI2P startup.

### JavaFX module metadata is not clean

The desktop build declares `javafx.controls` and `javafx.media`. A direct whole-lib `jdeps --print-module-deps` run
currently fails because `de.jensd.fx.glyphs.commons` requires `javafx.fxml`, while the installed lib directory only
contains JavaFX base, controls, graphics, and media jars.

This does not block the current classpath launch and does not block using a normal JRE. It does mean runtime analysis
needs to be careful: a strict module-path analysis of the app lib directory is not currently clean.

### Static analysis is not enough

`jdeps` does not reliably capture:

- Service-provider modules.
- TLS and crypto-provider needs.
- Locale data and resource bundle behavior.
- Optional charsets.
- Reflection.
- Native library loading.
- Platform-specific desktop integrations.
- JavaCV/webcam behavior on all OSes.

Any selected-module image needs smoke tests that exercise login/startup, Tor, embedded BI2P, webcam QR scanning, REST/API
startup if enabled, TLS/network access, language/resource loading, notifications, and update/launcher verification paths.

### Runtime image provenance matters

If using a vendor JRE, the release process must pin and verify the runtime download just as carefully as the JDK
toolchain. If generating a `jlink` image, the release process must record the exact JDK input and jlink options. Either
way, runtime image provenance should be part of release readiness.

## Further size reduction outside the Java runtime

The Java runtime is not the only large binary payload. In the measured `installDist`, `desktop.jar` is 96 MB, dominated
by `bisq-easy-intro.mp4` at about 89 MB. BI2P and webcam shadow archives are also large when included by installer
resource generation. These are separate from the JDK/JRE question, but they can dominate the final compressed installer
after the runtime is reduced.

Runtime work should not be expected to solve all installer-size issues by itself.

## Recommended path

1. Split build JDK and bundled runtime image in `PackagingPlugin`.

   Keep the JDK toolchain for `jpackage`; add a separate runtime image provider for `--runtime-image`.

2. Implement a low-risk runtime-image option first.

   Either use a pinned vendor JRE or generate a broad `jlink` image. A broad `jlink ALL-MODULE-PATH` image measured 92 MB
   locally, but it still includes JDK tool launchers. A vendor JRE likely gives better tool removal with low module risk.

3. Add a selected-module `jlink` path behind an explicit Gradle property.

   Use it for CI experiments before making it the release default. Include the desktop launcher, desktop app, BI2P, and
   webcam jars in module analysis.

4. Keep `bin/java` in the runtime image.

   The current child-process launchers require it. If removing native commands is still desired, refactor those launchers
   first and test webcam/BI2P startup.

5. Add release checks.

   Verify runtime image size, module list, executable list, `java -version`, checksums, and installer smoke-test results
   per platform.

6. Defer full JPMS migration.

   A classpath-compatible `jlink` runtime can deliver most runtime-size benefits before Bisq becomes a module-path-clean
   application. The broader JPMS migration remains a separate dependency cleanup project.

## Decision matrix

| Option | Feasibility | Effort | Runtime size benefit | Behavior risk | Notes |
| --- | --- | ---: | ---: | --- | --- |
| Keep full JDK | Already done | none | none | low | Largest image and ships build tools. |
| Vendor JRE/runtime image | high | low-medium | medium | low-medium | Best first step if pinned artifacts are available for all platforms. |
| Broad `jlink` image | high | medium | high | low-medium | Can be generated from the pinned JDK; may still include unwanted tool launchers if all modules are kept. |
| Selected-module `jlink` image | medium-high | medium | highest | medium | Best long-term packaging target; requires stronger analysis and smoke tests. |
| Full JPMS + `jlink` | medium | high | high | high | Not required for runtime-size work; see `docs/dev/jpms-feasibility.md`. |
