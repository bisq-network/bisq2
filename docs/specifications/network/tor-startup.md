# Tor startup and configuration

## Scope

This specification covers how a node starts Tor or connects to an external Tor, the bridges of the Tor which it
starts, and when the start fails. The Tor library in `tor/` implements it for Bisq 2 and Bisq 1; its README describes
the implementation.

## Required behaviour

### Tor of the node

On every operating system the node starts the Tor of the Tor expert bundle which the build packages. It never starts a
`tor` of the system or from `PATH`, and the programs of the pluggable transports also come only from the bundle. The
node installs the files of the bundle into its Tor directory (`<data dir>/tor`): Tor, its libraries, its GeoIP files
and the programs of the pluggable transports. At the first start after an update which brings another bundle, it
installs them again.

The bundle is the stable expert bundle of the pinned Tor Browser version (`tor.version` in `tor/gradle.properties`).
The stable releases have no expert bundle for Linux ARM64, so a build on Linux ARM64 takes the expert bundle of the
alpha release `tor.bundleVersion.linux_arm64`, whose tor has the same version. On Linux, Tor needs the glibc and the
`libz.so.1` of the system; the bundle brings its other libraries. Every Debian and Ubuntu system has `libz.so.1` in the
package `zlib1g`. The bundles of the pinned versions need glibc 2.17 on x86_64 and 2.27 on ARM64.

The Linux packages do not depend on the `tor` package of the distribution. The packages of Bisq 2.1.3 to 2.1.14 did,
because these versions ran the `tor` of the system on Linux. An update from one of them leaves the `tor` package
installed, and on Debian and Ubuntu its service `tor@default` keeps running until the user removes the package. The
node does not use it.

### External Tor

The node uses an external Tor instead of its own if it runs on Whonix, if the environment variable `TOR_SKIP_LAUNCH`
is `1`, `true` or `yes`, if `external_tor.config` in the Tor directory sets `UseExternalTor 1`, or if the
configuration sets `useExternalTor`. The first of these conditions which applies decides: on Whonix the node always
uses the external Tor; otherwise a set `TOR_SKIP_LAUNCH` decides alone, and a value other than `1`, `true` or `yes`
means no external Tor; otherwise `UseExternalTor` of `external_tor.config` decides when it is set; otherwise the
configuration. `external_tor.config` sets the host and port of the control port and the authentication. The node uses
the SOCKS port which the external Tor reports, on the host of the control port.

- With `CookieAuthentication 1`, the node authenticates with the SAFECOOKIE method: Tor first proves that it knows the
  cookie of `CookieAuthFile`, and the node never sends the cookie. A process which listens on the control port instead
  of Tor learns nothing about the cookie from the handshake, and the node sends it no key, unless the process can relay
  the handshake to a Tor which knows the cookie: SAFECOOKIE proves knowledge of the cookie, not the identity of the
  connection.
- `ControlPassword` sets the password of an external Tor which uses `HashedControlPassword`. The process on the control
  port cannot prove that it is Tor, so the cookie is the better choice on the same system. Setting both fails the
  start.
- Without both, the node sends an empty authentication, which only fits a control port that other processes cannot
  reach.

The node waits up to `bootstrapTimeout` until the external Tor has bootstrapped. It does not change the configuration
of the external Tor.

If the node cannot use the external Tor, the start fails with an error which names the host and port and the cause.
The node never starts its own Tor instead, because the user may rely on the external Tor, for example for its bridges,
and the own Tor would connect directly to the Tor network.

### Whonix

On Whonix, the node always uses the Tor of the Whonix-Gateway. onion-grater filters the control port of the Gateway and
needs the profile of Bisq there (`sudo onion-grater-add 40_bisq`). onion-grater accepts only the empty authentication,
so the node uses it there also with `CookieAuthentication 1`; the cookie of a Whonix-Workstation is a dummy. The node
listens on all addresses, because the Tor of the Gateway connects to the address of the Workstation. The publication
of an onion service completes at the start or at the end of the upload of its descriptor, because older profiles of
onion-grater deliver only the end in a form which the node accepts.

### Bridges

The configuration of the Tor transport sets the bridges of the Tor which the node starts:

- `builtInBridges` names a type of the built-in bridges of the Tor Browser version of the bundle: `obfs4`,
  `snowflake` or `meek`. They change with the version of the bundle.
- `bridges` lists bridge lines, for example from https://bridges.torproject.org.

At most one of them may be set. With bridges, the Tor of the node connects only through them. If a bridge cannot be
used, for example because its pluggable transport has no program, the start fails. The node never connects without the
configured bridges. Torrc overrides of `UseBridges`, `Bridge` and `ClientTransportPlugin` are ignored while bridges are
configured.

An external Tor uses the bridges of its own configuration. The node then ignores the configured bridges and logs a
warning. The node does not log bridge lines, because a bridge which a user got privately is blocked once it is known.

## Rationale

Users configure bridges or an external Tor with bridges to get around a block of Tor or to hide from their network that
they use Tor. Connecting without the bridges would fail in the first case and expose the user in the second. Failing
the start leaves that decision to the user.

The node runs the Tor of its bundle on Linux as well, so that every node gets the version of Tor which the build pins
and checks, with its security fixes, also where the `tor` package of the distribution is older. On Linux ARM64 the
alpha release is the only source of an expert bundle.

## Known gaps

- Bisq 2 has no user interface for bridges. They are set in `bisq.conf` of the data directory, see `docs/dev/build.md`.
- On Linux ARM64 the bundle comes from an alpha release of Tor Browser until a stable release has an expert bundle for
  Linux ARM64. The build checks that its tor has the version of the stable bundles. Its libraries and its programs of
  the pluggable transports are those of the alpha release and can be newer than those of the stable bundles.
- The bundled tor of Linux is built for the usual layout of the file system: it takes the loader, glibc and
  `libz.so.1` from the system. It does not run on NixOS or Guix, whose libraries lie in their stores; there the node
  needs an external Tor.
- On Linux the bundled tor finds its libraries through `LD_LIBRARY_PATH`, which the loader splits at `:` and `;`. A
  data directory whose path contains one of them fails the start of Tor with a message that says so.
- The onion service of the API is not reachable on Whonix, because the API server listens on `127.0.0.1` only.
