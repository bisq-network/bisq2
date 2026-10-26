# Tor startup and configuration

## Scope

This specification covers how a node starts Tor or connects to an external Tor, the bridges of the Tor which it
starts, and when the start fails. The Tor library in `tor/` implements it for Bisq 2 and Bisq 1; its README describes
the implementation.

## Required behaviour

### Tor of the node

On macOS and Windows the node starts the Tor of the bundled Tor expert bundle. On Linux it starts the `tor` of the
system, which the Linux packages depend on. The node installs the files of the bundle into its Tor directory
(`<data dir>/tor`); on Linux these are only the programs of the pluggable transports.

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

## Known gaps

- Bisq 2 has no user interface for bridges. They are set in `bisq.conf` of the data directory, see `docs/dev/build.md`.
- On Linux ARM64 the bundle has no programs for the pluggable transports, so the user has to install them.
- The onion service of the API is not reachable on Whonix, because the API server listens on `127.0.0.1` only.
