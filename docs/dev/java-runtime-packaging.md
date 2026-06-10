# Java runtime packaging analysis

Assessed and implemented on 2026-05-21 against the current Bisq 2 Gradle workspace.

## Executive summary

Bisq can move away from shipping the full JDK with the desktop installer. The installer path needs a JDK at build time
for `jpackage` and `jlink`, but the installed app does not need compiler, debugger, source, header, or packaging tools.
It launches on the classpath with the application jars and OpenJFX jars in the app `lib` directory.

Implemented direction: keep the current classpath launch, keep the Azul JDK toolchain for build-time `jpackage`, and
feed `jpackage --runtime-image` from a separate runtime-image provider. By default, packaging now generates a conservative
Java 21 `jlink` runtime image with `bin/java` retained. The packaging extension can also point at an external vendor
JRE/runtime image if release engineering prefers that.

The larger benefit comes from a `jlink` runtime image. A local prototype for the desktop app plus the spawned BI2P and
webcam Java processes produced a 67 MB runtime image, compared with a 342 MB Zulu JDK 21.0.11 image on this macOS
machine. This is feasible while the application remains on the classpath, but it requires better runtime-module analysis
and smoke tests because `jdeps` cannot see every reflective, service-provider, crypto, locale, and native-integration
path.

Recommendation: use the generated `jlink` image as the default packaging target, keep the external JRE override as an
escape hatch, and require full platform smoke tests before release. Do not make a full JPMS migration a prerequisite for
this work.

## Implemented packaging state

- `apps/desktop/desktop-app-launcher` applies `bisq.gradle.packaging.PackagingPlugin`.
- `PackagingPlugin` obtains an Azul Java 21 toolchain through `JavaToolchainService`.
- The `generateInstallers` task still uses the Azul toolchain as its build-time `jdkDirectory`.
- A new `createJPackageRuntimeImage` task generates the default bundled runtime image with `jlink`.
- `generateInstallers.runtimeImageDirectory` now uses `packaging.runtimeImageDirectory` when explicitly configured, or
  the generated `createJPackageRuntimeImage` output otherwise.
- `JPackageTask` calls `<jdkDirectory>/bin/jpackage`.
- `PackageFactory` always passes `--runtime-image <runtimeImageDirectory>` to `jpackage`.
- `build-logic/toolchain-resolver` still pins Zulu JDK download URLs. The generated runtime image is derived from that
  pinned JDK.
- The generated runtime intentionally keeps `bin/java` because the desktop app starts child JVM processes.

The implementation now separates two concerns:

1. The build-time JDK needed to run `jpackage`, `jdeps`, and `jlink`.
2. The runtime image copied into the installer and used on end-user machines.

Relevant extension properties:

- `packaging.runtimeImageDirectory`: optional external runtime image, for example a pinned vendor JRE.
- `packaging.runtimeImageModules`: module roots used by the generated `jlink` image.
- `packaging.runtimeImageJlinkOptions`: options passed to `jlink`.
- `packaging.runtimeImageExcludedNativeCommands`: native commands removed after `jlink` while keeping the runtime usable.
- `packaging.requireJavaLauncher`: verifies `bin/java` or `bin/java.exe` exists in the generated image.

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
| Implemented generated `jlink` image | 67 MB |
| Implemented generated `jlink` image `bin` directory | 144 KB |

The `installDist` measurement is not the final compressed installer size. It also does not necessarily include every
resource wired only by `generateInstallers`, such as generated BI2P/webcam payload resources. The runtime savings are
still additive: replacing a 342 MB runtime with a smaller runtime directly reduces the app image contents before
installer compression.

## JRE vs custom `jlink` runtime

### Vendor JRE/runtime image

Benefits:

- Lowest behavioral risk because it is closest to a normal Java runtime.
- Less chance of missing modules discovered only by rarely used features.
- Easier support story: the runtime comes from a vendor artifact with normal release notes and checksums.
- Still removes most JDK-only material, such as `javac`, `javadoc`, `jlink`, `jpackage`, `jmods`, headers, and source.

Risks and costs:

- Size reduction is limited by what the vendor includes.
- Runtime contents are less tailored to Bisq.
- Release engineering must pin and verify additional JRE artifacts for every OS/architecture.
- Some Java vendors no longer publish traditional standalone JREs for every platform/version, so availability may vary.

### Custom `jlink` runtime

Benefits:

- Smallest runtime image because only selected JDK modules are present.
- Generated from the same pinned JDK already used by the release build, so there is no separate vendor runtime download.
- Better control over executable surface and runtime contents.
- The module list becomes explicit packaging documentation.

Risks and costs:

- Higher behavioral risk: static analysis can miss reflection, service providers, charsets, locale data, crypto, and
  platform integration needs.
- Every new feature or dependency may require revisiting the module list.
- More release testing is required, especially for BI2P, webcam, networking/TLS, notifications, resource loading, and
  non-English locales.
- A too-aggressive image can fail late on a user's machine rather than at compile time.

### Security impact of a stripped runtime

A stripped runtime has real but bounded security benefits.

It reduces local attack surface by removing tools and modules that the app does not need. For example, not shipping
`javac`, `jshell`, `jpackage`, `jlink`, `jmods`, source archives, and headers gives an attacker who already has local
code execution fewer convenient tools inside the app bundle. A selected-module `jlink` image also removes entire JDK
modules that cannot be loaded because they are not present.

It does not turn the runtime into a sandbox and it does not by itself mitigate vulnerabilities in the modules that remain
available, the JVM, JavaFX, third-party jars, native libraries, Tor, I2P, or the operating system. It is defense in depth,
not a primary security boundary. The most important security requirements remain fast runtime updates, dependency
updates, code signing/notarization, checksum verification, and feature-level hardening.

## Feasibility: vendor JRE/runtime image

Feasibility: high.

The desktop app is not using `javac`, `javadoc`, `jlink`, `jpackage`, `jdeps`, `jshell`, or other JDK tools at runtime.
The build needs a JDK, but the installed app should only need a Java 21 runtime with `bin/java`, standard runtime modules,
the certificate store, and platform native libraries.

Engineering work if choosing an external JRE instead of the generated `jlink` default:

- Configure `packaging.runtimeImageDirectory` to point at the unpacked, pinned runtime image.
- Keep the current Azul JDK toolchain as the build-time `jpackage` tool provider.
- Add a pinned per-OS/per-architecture runtime image source.
- Add release verification that records the runtime vendor, version, architecture, checksum, and `java -version` output.
- Run full installer smoke tests on macOS, Windows, and Linux.

Expected benefit:

- Removes compiler, debugger, packaging, source, header, and module-definition artifacts from the shipped app.
- Reduces download size and installed size.
- Reduces the number of executable tools available inside the installed app bundle.
- Keeps behavior close to the current full-JDK packaging because the runtime still contains the normal Java runtime
  modules.

Estimated additional effort: 2 to 5 engineering days, plus release-platform QA. The plugin now supports an external
runtime-image input; the remaining work is artifact pinning and platform verification.

## Feasibility: `jlink` runtime image

Feasibility: medium-high, with testing risk.

A custom `jlink` runtime can be used even if Bisq continues to launch on the classpath. The application jars do not need
to become JPMS modules first. `jdeps` can provide a starting module set, then the build can create a runtime image from
the pinned JDK and pass it to `jpackage` with `--runtime-image`.

The active packaging path now registers `createJPackageRuntimeImage`. The selected module set accounts for:

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

Default `jlink` options:

```text
--no-header-files
--no-man-pages
--strip-debug
--compress zip-6
```

Default commands removed after `jlink`:

```text
jdb
jfr
jwebserver
```

The generated macOS image currently retains only `java` and `keytool` in `bin`. `java` is required by webcam and BI2P
child-process launchers. `keytool` is kept conservatively because it is a normal runtime support tool and very small.

On this machine, `--compress zip-6` reduced the selected runtime from 123 MB to 67 MB. `zip-9` did not materially change
the measured size.

Estimated remaining effort: 3 to 7 engineering days for release hardening and cross-platform smoke testing, assuming no
major runtime regressions. Add more time if CI/release automation must build and archive runtime-image reports.

## Important blockers and risks

### `bin/java` must remain available

The current desktop app starts child Java processes with:

- `apps/desktop/desktop/src/main/java/bisq/desktop/webcam/WebcamProcessLauncher.java`
- `network/network/src/main/java/bisq/network/p2p/node/transport/i2p/Bi2pProcessLauncher.java`

Both derive the executable from `System.getProperty("java.home")` and expect `bin/java` or `bin/java.exe`.

Therefore, do not use `jlink --strip-native-commands` unless those launch paths are refactored or another Java launcher
is provided. A stripped image without native commands has no `bin/java` and will break webcam and embedded BI2P startup.
The implemented task removes selected unneeded commands after `jlink` instead.

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

   Done. The JDK toolchain is still used for `jpackage`; the bundled runtime comes from an external override or the
   generated `jlink` task.

2. Implement a low-risk runtime-image option first.

   Done as a conservative selected-module `jlink` image with `bin/java` retained. An external vendor JRE can still be
   supplied through `packaging.runtimeImageDirectory`.

3. Add a selected-module `jlink` path behind an explicit Gradle property.

   Done as the default path. Before release, add CI/runtime reports and smoke tests for the desktop launcher, desktop app,
   BI2P, and webcam jars.

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
| Vendor JRE/runtime image | high | low-medium | medium | low-medium | Supported fallback if pinned artifacts are available for all platforms. |
| Broad `jlink` image | high | medium | high | low-medium | Can be generated from the pinned JDK; may still include unwanted tool launchers if all modules are kept. |
| Selected-module `jlink` image | medium-high | medium | highest | medium | Implemented default; requires stronger analysis and smoke tests before release. |
| Full JPMS + `jlink` | medium | high | high | high | Not required for runtime-size work; see `docs/dev/jpms-feasibility.md`. |
