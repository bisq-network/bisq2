# JPMS feasibility analysis

Assessed on 2026-05-21 against the current Bisq 2 Gradle workspace.

## Executive summary

Introducing Java Platform Module System (JPMS) descriptors into the Bisq 2 codebase is feasible as a staged engineering
project, but not as a low-risk mechanical change. The Bisq source layout is already close to JPMS-friendly: the code is
split into many Gradle projects, package ownership is clear, and a scan of `src/main/java` found no split packages across
Bisq projects.

The main blocker is the runtime dependency set, not Bisq package layout. The installed desktop runtime currently uses a
classpath launch with 152 jars. A direct `java --validate-modules --module-path .../lib` fails because several current
runtime jars cannot coexist on a strict module path. Examples include duplicate automatic module names, a project jar
whose automatic name shadows the JDK `java.se` module, and split packages inside third-party or shaded libraries.

Recommendation: do not attempt a one-step full JPMS conversion. Start with a documentation and validation phase, then add
explicit module descriptors to a small set of leaf/core modules while still launching on the classpath. Treat a fully
modular desktop runtime and `jlink` image as a later phase that first requires dependency cleanup.

## Current state

- Java 21 is already required and configured through Gradle toolchains.
- No `module-info.java` files are currently checked in.
- The root build includes 27 projects plus composite builds for `apps`, `apps/desktop`, `network`, `network/tor`, and
  `build-logic`.
- The Gradle convention plugin `bisq.java-library` applies `java-library`, configures Lombok as a compile-only
  annotation processor, and adds shared dependencies such as Guava, SLF4J, Logback, JUnit, AssertJ, and Mockito.
- `platform` is a Java platform/BOM project and does not produce a Java component.
- Protobuf is used broadly through `bisq.protobuf`; gRPC is used in `trade`, `wallet`, `network/i2p`,
  `apps/oracle-node-app`, and `daemon`.
- Desktop applications use JavaFX through the OpenJFX Gradle plugin and are currently launched by Gradle start scripts
  with a large `CLASSPATH`, not with `--module-path`.
- Packaging already contains custom `JDepsTask` and `JLinkTask` classes, but the active desktop installer path uses
  `jpackage` with the full toolchain runtime image rather than a generated minimal `jlink` runtime.

## Feasibility findings

### Positive signals

1. The Bisq source packages are not split across Gradle projects.

   A scan of main Java sources found 44 projects with `src/main/java` content and zero duplicated Java packages across
   those projects. That removes one of the hardest JPMS migration problems.

2. The Gradle project graph mostly maps to module boundaries already.

   Projects such as `common`, `persistence`, `security`, `i18n`, `identity`, `account`, `offer`, `trade`, `network`,
   and `desktop` are natural JPMS module candidates. Existing project dependencies can be translated into `requires`
   clauses over time.

3. Most internal reflection is localized.

   The scan found a small number of JPMS-sensitive reflection sites:

   - `common/src/main/java/bisq/common/proto/Proto.java` uses `setAccessible(true)` on fields of objects implementing
     `Proto`.
   - `chat/src/main/java/bisq/chat/ChatChannelDomain.java` and
     `chat/src/main/java/bisq/chat/common/SubDomain.java` inspect enum fields for `@Deprecated`.
   - `trade/src/main/java/bisq/trade/bisq_easy/protocol/BisqEasyProtocol.java` and
     `trade/src/main/java/bisq/trade/mu_sig/protocol/MuSigProtocol.java` instantiate event handlers reflectively.
   - `apps/desktop/desktop/src/main/java/bisq/desktop/components/table/BisqTableColumn.java` creates button classes
     reflectively.
   - `common/src/main/java/bisq/common/platform/OS.java` uses `Class.forName("android.os.Build")` for Android
     detection.

   These are manageable with targeted `opens` clauses or small refactors. There is no broad framework-driven reflection
   like FXML controller injection.

4. There is existing investment in dependency security and packaging validation.

   Dependency verification, pinned versions, release checks, and the existing jdeps/jlink task classes provide useful
   scaffolding for a future JPMS validation pipeline.

### Blocking issues

1. The current desktop runtime lib directory is not a valid module path.

   After running:

   ```sh
   ./gradlew :apps:desktop:desktop-app:installDist
   java --validate-modules --module-path apps/desktop/desktop-app/build/install/desktop-app/lib
   ```

   JPMS validation failed. The important failures were:

   - `java-se-2.1.11.jar` derives the automatic module name `java.se`, which is shadowed by the JDK `java.se` module.
     This must be fixed by giving the Bisq project an explicit module name such as `bisq.java_se`.
   - `i2p-2.12.0.jar` and Bisq's `i2p.jar` both derive the automatic module name `i2p`.
   - `swagger-annotations-2.2.50.jar` and `swagger-annotations-jakarta-2.2.50.jar` both derive
     `io.swagger.v3.oas.annotations`.
   - `router-2.12.0.jar` contains `org.bouncycastle.*` packages that conflict with the Bouncy Castle modules.
   - `grizzly-websockets-server-5.0.1.jar` and `grizzly-http-server-core-5.0.1.jar` contain packages that conflict with
     the explicit Grizzly modules, plus `jakarta.servlet` package conflicts.

2. Automatic module names are unstable or unsuitable for Bisq jars.

   Hyphenated project names derive automatic names such as `bisq.easy`, `bonded.roles`, `mu.sig`, `os.specific`, and
   `socks5.socket.channel`. Some are acceptable but not ideal; `java-se` is actively wrong because it collides with the
   JDK naming space. Explicit descriptors are required for any serious migration.

3. Third-party dependency cleanup is required before a fully modular application can launch on the module path.

   A scan of the installed desktop runtime found 152 jars: 65 with explicit descriptors and 87 automatic modules. The
   problematic jars mean a full module-path launch will require dependency exclusions, upgrades, replacement of shaded
   artifacts, or keeping some parts on the classpath. Keeping problematic dependencies on the classpath limits JPMS
   value because named modules cannot depend on the unnamed module in the normal way.

4. JavaFX module requirements need to be made explicit.

   The desktop build declares `javafx.controls` and `javafx.media`. A whole-runtime `jdeps` attempt reported that
   `de.jensd.fx.glyphs.commons` requires `javafx.fxml`, which is not currently in the declared JavaFX module list. This
   does not break the current classpath launch, but it matters for module-path or jlink work.

5. Resource loading and i18n need dedicated testing.

   `i18n/src/main/java/bisq/i18n/Res.java` uses a custom `ResourceBundle.Control`. Resource bundle lookup and arbitrary
   `ClassLoader.getResourceAsStream` calls are common JPMS migration risk areas because named modules change resource
   visibility rules. This should be tested early, especially for translated property files, CSS, images, Tor resources,
   webcam resources, and static API resources.

6. Tests need separate JPMS handling.

   Unit tests currently run in the normal Gradle/JUnit classpath style. Modular tests often need `--patch-module`,
   additional `opens` to test frameworks, or keeping tests on the classpath. This is manageable but not free.

## Effort estimate

These estimates assume one experienced contributor familiar with Gradle, Java 21, and Bisq module boundaries.

### Phase 0: validation baseline

Effort: 2 to 4 days.

- Add scripts or Gradle tasks to report duplicate packages, automatic module names, and module-path validation failures.
- Decide canonical module names for all Bisq projects.
- Document which projects are in scope for the first migration phase.
- Add CI checks that are informative but non-blocking at first.

### Phase 1: core module descriptors, classpath runtime

Effort: 1 to 2 weeks.

- Add `module-info.java` to low-risk modules first, for example `common`, `i18n`, `persistence`, `security`,
  `presentation`, and selected domain modules.
- Configure Gradle modular compilation for descriptor-bearing projects.
- Add `requires static` clauses for compile-only annotation modules where needed, such as Lombok and nullability
  annotations.
- Add narrowly scoped `exports` and `opens` clauses.
- Keep applications launched on the classpath initially to avoid third-party module-path blockers.

### Phase 2: broaden to network, trade, and API modules

Effort: 2 to 4 weeks.

- Add descriptors to modules using protobuf, gRPC, Jackson/Gson, Jersey, Bouncy Castle, I2P, Tor, and JavaFX-adjacent
  code.
- Stabilize generated protobuf/gRPC source handling under modular compilation.
- Resolve reflection and resource access failures found by tests.
- Add module descriptor checks to CI.

### Phase 3: full module-path desktop/runtime image

Effort: 4 to 8+ weeks, depending on dependency replacement complexity.

- Resolve or isolate all current `java --validate-modules` failures.
- Fix duplicate automatic module names and third-party split packages.
- Decide whether the REST/WebSocket stack and I2P stack can be made module-path clean or must remain in a classpath
  launched process.
- Rewire packaging to use a real `jlink` runtime image where appropriate.
- Run full desktop, seed node, oracle node, API app, local network, and installer verification.

Overall effort: medium-to-high. A partial descriptor migration is realistic in a few weeks. A fully modular runtime image
is a larger packaging and dependency project.

## Benefits

1. Stronger compile-time boundaries.

   Explicit `exports` make it clear which packages are public contracts and which are internal. This is useful in a
   large codebase with many domain modules.

2. Earlier detection of dependency mistakes.

   JPMS catches split packages, duplicate module names, missing reads, and accidental reliance on transitive classpath
   behavior. The current validation failures show that it can surface real packaging issues.

3. Better basis for custom runtimes.

   A module-path clean application can use `jlink` more effectively, reducing runtime size and making installer contents
   easier to reason about. This aligns with the existing packaging code that already contains jdeps/jlink task classes.

4. Improved encapsulation for security-sensitive code.

   Modules such as `security`, `identity`, `persistence`, `trade`, and `network` could export only stable API packages
   and keep implementation packages inaccessible by default.

5. Cleaner long-term architecture documentation.

   A maintained `module-info.java` becomes executable architecture documentation. It can clarify which modules are
   allowed to depend on networking, persistence, UI, or application services.

## Costs and risks

1. Build complexity increases.

   Gradle needs modular compilation setup, generated-source handling, test patching, and IDE compatibility checks.

2. Dependency friction is significant.

   The current runtime includes shaded or overlapping jars that are acceptable on the classpath but invalid on a module
   path. Resolving these could require dependency upgrades or substitutions unrelated to Bisq source code.

3. Refactoring pressure may spread.

   JPMS makes package-level boundaries explicit. Any package that is currently used by multiple modules as an internal
   convenience API may need to be exported, moved, or wrapped.

4. Reflection and resource behavior can fail late.

   Reflection failures often appear only in specific workflows. Resource lookup issues may appear only for certain
   locales, CSS paths, images, or packaged app layouts.

5. Partial migration can create confusion.

   Having some projects modular and others not can be useful as a transition state, but the build must make the intended
   launch mode clear. Otherwise contributors may assume stronger security or encapsulation than the runtime actually
   provides.

## Security considerations

JPMS improves encapsulation but is not a sandbox. It should be treated as a hardening and architecture tool, not as a
replacement for input validation, cryptographic review, dependency verification, process isolation, or OS-level
permissions.

Positive security impacts:

- Internal packages can be hidden from other modules unless explicitly exported.
- Reflective access can be denied by default and granted only through targeted `opens`.
- Dependency/module-path validation catches duplicate module names and split packages that can hide accidental or
  malicious class shadowing.
- A future `jlink` image can reduce runtime surface area by shipping fewer JDK modules.

Security risks and caveats:

- Opening broad packages to frameworks can undo much of the encapsulation benefit.
- Automatic modules export all packages and are weak security boundaries.
- A classpath launch still has classpath semantics, even if some jars contain descriptors.
- Shaded third-party jars with duplicated packages deserve special review because they complicate provenance and module
  ownership.
- Any `--add-opens` or `--add-exports` flags should be treated as exceptions, documented, and tested.

## Recommended migration strategy

1. Do not start with the desktop app.

   Start with leaf or low-level modules where dependencies are simpler. `common` is central but has reflection and many
   consumers; it may be worth prototyping there, but the first production descriptors should be chosen to minimize blast
   radius.

2. Define canonical Bisq module names.

   Use stable names under a Bisq namespace, for example:

   - `bisq.common`
   - `bisq.i18n`
   - `bisq.persistence`
   - `bisq.security`
   - `bisq.java_se`
   - `bisq.bisq_easy`
   - `bisq.mu_sig`
   - `bisq.network`
   - `bisq.network.i2p`
   - `bisq.desktop`
   - `bisq.desktop_app`

   Avoid automatic names for Bisq jars.

3. Add validation before descriptors.

   Add a Gradle report task that records:

   - Bisq split packages.
   - Runtime jar module names.
   - Duplicate automatic module names.
   - `java --validate-modules` output for application distributions.
   - JDK modules reported by `jdeps`.

4. Keep initial descriptors conservative.

   Export only packages that are consumed by other modules. Prefer package-private implementation classes where possible.
   Use `opens` only for concrete reflection users, for example serialization, tests, or JavaFX if needed.

5. Treat generated protobuf/gRPC packages as part of each owning module.

   Generated packages such as `bisq.trade.protobuf` and `bisq.wallet.protobuf` should be exported only if other modules
   need to compile against them. Avoid creating a shared generated-code module unless there is a clear ownership reason.

6. Postpone `jlink` until the runtime validates as a module path.

   The current runtime dependency set is not module-path clean. A jlink migration should come after dependency cleanup,
   not before.

## Suggested first proof of concept

Target: one or two non-application modules plus validation tasks.

Candidate modules:

- `i18n`: small source surface, but useful because resource loading risk is important.
- `persistence`: small source surface with protobuf and serialization relevance.
- `security`: security-sensitive and a good encapsulation candidate, but depends on crypto libraries and should be
  tested carefully.

Proof-of-concept acceptance criteria:

- The selected modules compile with `module-info.java`.
- Unit tests still pass.
- IDE import still works.
- No broad `opens` are introduced.
- Generated protobuf sources compile under the module descriptor.
- Runtime applications still launch on the classpath.
- The validation report documents all remaining module-path blockers.

## Conclusion

JPMS is worth investigating for Bisq 2, mainly for architecture clarity, dependency validation, and future packaging
hardening. It should not be framed as a quick security upgrade. The codebase itself is reasonably well shaped for JPMS,
especially because Bisq source packages are not split across projects. The current runtime dependency set is the major
obstacle, and a fully modular desktop application will require substantial third-party dependency cleanup before it can
replace the current classpath launch.
