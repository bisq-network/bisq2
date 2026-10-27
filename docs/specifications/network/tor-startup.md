# Tor startup and configuration

## Scope

This specification covers how a node starts Tor or connects to an external Tor, the bridges of the Tor which it
starts, and when the start fails. The Tor library in `tor/` implements it for Bisq 2 and Bisq 1; its README describes
the implementation.

## Required behaviour

### Tor of the node

On every operating system the node starts the Tor of the Tor expert bundle which the build packages. It never starts a
`tor` of the system or from `PATH`, and the programs of the pluggable transports also come only from the bundle. The
node installs the files of the bundle into `bin/` of its Tor directory (`<data dir>/tor`): Tor, its libraries, its
GeoIP files and the programs of the pluggable transports. At the first start after an update which brings another
bundle, it installs them again.

The bundle is the stable expert bundle of the pinned Tor Browser version (`tor.version` in `tor/gradle.properties`).
The stable releases have no expert bundle for Linux ARM64, so a build on Linux ARM64 takes the expert bundle of the
alpha release `tor.bundleVersion.linux_arm64`, whose tor has the same version. On Linux, Tor needs the glibc and the
`libz.so.1` of the system; the bundle brings its other libraries. Every Debian and Ubuntu system has `libz.so.1` in the
package `zlib1g`. The bundles of the pinned versions need glibc 2.17 on x86_64 and 2.27 on ARM64.

The Linux packages do not depend on the `tor` package of the distribution. The packages of Bisq 2.1.3 to 2.1.14 did,
because these versions ran the `tor` of the system on Linux. An update from one of them leaves the `tor` package
installed, and on Debian and Ubuntu its service `tor@default` keeps running until the user removes the package. The
node does not use it.

The Tor directory has the parts `bin/` (the installed bundle), `data/` (the data directory of Tor) and `sandy/` (the
sandbox program). The torrc, the logs and `external_tor.config` lie in the Tor directory itself, outside the directory
which Tor writes. Tor writes its log to its standard output, which the node writes to `tor-stdout.log`; above 10 MiB
it moves the log to `tor-stdout.log.1` before the next start. The resources page opens this log, and the log zip
contains a copy without the home directory. Before a start of its own Tor, the node moves the state of Tor of the
layout before into `data/`, deletes only the files of the old layout which it knows, and keeps everything else. The
backup of the data directory skips `tor/data`, and `tor/bin` and `tor/sandy`, which the node installs again at its
next start of Tor. The backup contains `external_tor.config`, also when it holds the password of an external Tor
(`ControlPassword`) in plain text.

The node chooses the ports of the control and SOCKS listeners itself and checks with the hash of the control password,
and after the bootstrap with the SOCKS listener, that it talks to the Tor of this start.

The node starts its Tor, or sandy with Tor, in a session of its own with the `setsid` program of the system, if the
system has one, as Linux does. A Ctrl+C in the terminal which started the node then reaches only the node, whose
shutdown can still use Tor, and which stops Tor at its end. macOS has no `setsid`: there Tor stays in the process
group of the node, and a Ctrl+C in the terminal ends Tor at once.

### Torrc overrides and files of the Tor of the node

The configuration of the Tor transport can add entries to the torrc of the Tor which the node starts
(`torrcOverrides`). The node ignores an override of a key which it sets for its own Tor: `ControlPort`,
`ControlPortWriteToFile`, `SocksPort`, `DataDirectory`, `HashedControlPassword` and `DisableNetwork`, in any letter
case and also as an abbreviation of the option name, which Tor accepts. An override of another key replaces the entry
of the node with that key, in any letter case. Every entry stays one entry of the torrc, as Tor reads it. A key is
the name of a Tor option: a letter followed by letters and digits, after one or two underscores for a hidden option.
So it cannot carry the forms `+Key`, `/Key`, `Key#comment` and `%include` of the torrc syntax. A value contains no
line break and no control character other than a tab. It is either one quoted string, in which a backslash escapes the
next character, or it contains no `#`, which would start a comment, and does not end with a backslash, which would join
the next line to the value. The rule applies to the overrides, to the entries of the node and to the bridge lines. The
start fails for an entry which breaks it. This includes a path which the node writes into the torrc, all of them below
the data directory of the application, when the path contains a `#`: the node writes its paths without quotes.

The node sets `AvoidDiskWrites 1`; an override can change it. Tor reads its GeoIP files from the bundle and gets an
empty OpenSSL configuration file, so that it reads no file below `/var/tmp/dist`, the build path of the bundle, where
another user of the system could place files.

The node publishes an onion service only if the onion address which Tor reports for the key is the address of the
key. The control connection reads lines of at most 16 KiB and replies of at most 1 MiB; a longer line or reply ends
the connection.

### External Tor

The node uses an external Tor instead of its own if it runs on Whonix, if the environment variable `TOR_SKIP_LAUNCH`
is `1`, `true` or `yes`, if `external_tor.config` in the Tor directory sets `UseExternalTor 1`, or if the
configuration sets `useExternalTor`. The first of these conditions which applies decides: on Whonix the node always
uses the external Tor; otherwise a set `TOR_SKIP_LAUNCH` decides alone, and a value other than `1`, `true` or `yes`
means no external Tor; otherwise `UseExternalTor` of `external_tor.config` decides when it is set; otherwise the
configuration. `external_tor.config` sets the host and port of the control port and the authentication. As in a
torrc, a `#` after a value starts a comment, unless the value is a quoted string; other text after the closing quote
fails the start. The node uses the SOCKS port which the external Tor reports, on the host of the control port.

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
Tor itself logs the bridges which it uses at its notice level, for example the fingerprint which it learns for a
bridge, so the Tor log of the node and the log zip of the error popup contain them.

### Sandbox

The node runs the Tor which it starts in the sandbox of the operating system with sandy, unless `sandbox.enabled` of
the configuration of the Tor transport is `false`. Without that value the sandbox is on, and the desktop, seed node,
oracle node, API and node monitor applications set it to `true`. Every build contains the sandy binary of its
platform, which the Tor build checks against its pinned SHA-256 hash.

The base level of the sandbox is the maximum lockdown. Tor may read its torrc and the bundle and write only its data
directory; on Linux and macOS every other path looks missing to it. Tor has a network of its own: the node reaches the
control and SOCKS listeners through ports which sandy relays, Tor reaches the onion services of the node only through
ports which sandy forwards (`sandbox.onionServicePortPoolSize`, from 1 to 256, limits how many onion services can
receive connections; the node refuses another value), and Tor connects out only through the egress broker of sandy,
which reaches public addresses only. Tor cannot start other processes, cannot signal processes outside the sandbox,
and ends when the node ends. Where the system cannot enforce the base level, sandy selects a weaker level of the
policy: below Linux 6.12 a PID namespace isolates the signals of Tor, and where unprivileged user namespaces are
restricted or off, Tor gets the network of the system. The node logs the level, with a warning for a level with the
network of the system or without isolated signals.

Pluggable transports are programs which Tor starts for bridges. The sandbox lets Tor start them only if
`sandbox.allowPluggableTransports` is `true`; it is `false` by default. The network settings of the desktop
application have a switch for it, which asks for a confirmation before it allows them, writes only this value into
`bisq.conf` of the data directory, and takes effect after a restart; the switch is disabled while the sandbox is off
and on Windows. With the value `true`, Tor may start the programs of the transports of the configured bridges, and
they end with Tor. On Linux, obfs4 keeps the network of the sandbox and connects through the egress broker; with meek,
webtunnel, snowflake or conjure, Tor and its transports get the network of the system. On macOS every transport gets
the network of the system, and the sandbox lets Tor and its transports reach the certificate verifier of the system
(the Mach service `com.apple.trustd.agent`), with which webtunnel verifies its server. On Windows Tor cannot start
pluggable transports in its sandbox.

If the sandbox is on, the start fails with an error which names the reason and how to turn the sandbox off, and the
node never starts Tor without the sandbox instead, when:

- the bridges need pluggable transports and `sandbox.allowPluggableTransports` is not `true`, or the system is
  Windows;
- a torrc override sets a proxy option or `ServerTransportPlugin`, or without configured bridges sets
  `ClientTransportPlugin`, also as an abbreviation of the option name, which Tor accepts;
- the system can enforce no level of the sandbox, for example Linux without Landlock, or the build contains no sandy;
- sandy cannot tell now whether the system enforces the sandbox.

An external Tor, including the Tor of Whonix and of bisq-mobile, runs without the sandbox of the node.
`TorService.getSandboxStatus()` names the level of the sandbox, or why Tor runs without it, for the user interface.

## Rationale

Users configure bridges or an external Tor with bridges to get around a block of Tor or to hide from their network that
they use Tor. Connecting without the bridges would fail in the first case and expose the user in the second. Failing
the start leaves that decision to the user.

The node runs the Tor of its bundle on Linux as well, so that every node gets the version of Tor which the build pins
and checks, with its security fixes, also where the `tor` package of the distribution is older. On Linux ARM64 the
alpha release is the only source of an expert bundle.

Tor parses data from the network, and the sandbox keeps a compromised Tor away from the files and keys of the node. A
start which cannot get the sandbox fails instead of running Tor without it, so that the user does not lose the sandbox
without being told. Pluggable transports stay off by default, because with them Tor may start processes and, with most
transports, reaches the network of the system, including its local services.

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
- The user interface does not show whether Tor runs in the sandbox; the log says it.
- At a level with the network of the system, and with pluggable transports other than obfs4 on Linux, the sandbox does
  not keep Tor from connecting to local services of the user, for example the API of the node; their own
  authentication protects them. Tor cannot read the cookie and key files of the node.
- Bridges with pluggable transports need the sandbox off on Windows.
- On Windows Tor runs in its sandbox only in a desktop session. In session 0, where services and logins over SSH run,
  Tor ends at once when it starts in its sandbox; the report of sandy in the log of the node says so.
- The Tor log in the log zip names the bridges which Tor used. A user with private bridges has to remove these lines
  before sharing the zip; whether the node should remove them is an open decision.
