# Tor startup and configuration

## Scope

This specification covers how a node starts Tor or connects to an external Tor, the bridges of the Tor which it
starts, and when the start fails. The Tor library in `tor/` implements it for Bisq 2 and Bisq 1; its README describes
the implementation. This specification describes version 1.0.0 of the library (`version` in `tor/gradle.properties`).

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
layout before into `data/`, deletes only the files of the old layout which it knows, and keeps everything else. A file
or a link at the path `data`, or at the path `data.migration` of the temporary directory of the move, belongs to no
layout, so the node deletes it before the move; it deletes a link itself, never its target. The
backup of the data directory skips `tor/data`, and `tor/bin` and `tor/sandy`, which the node installs again at its
next start of Tor. The backup contains `external_tor.config`, also when it holds the password of an external Tor
(`ControlPassword`) in plain text.

The node never deletes the `lock` file which Tor keeps in `data/`. Tor locks the file while it runs, so a file which an
ended Tor left blocks no start, and a Tor which still runs on the data directory makes the next Tor exit instead of
sharing the directory with it.

The node chooses the ports of the control and SOCKS listeners itself and checks with the hash of the control password,
and after the bootstrap with the SOCKS listener, that it talks to the Tor of this start.

The node starts its Tor, or sandy with Tor, in a session of its own with the `setsid` program of the system, if the
system has one, as Linux does. A Ctrl+C in the terminal which started the node then reaches only the node, whose
shutdown can still use Tor, and which stops Tor at its end. macOS has no `setsid`: there Tor stays in the process
group of the node, and a Ctrl+C in the terminal ends Tor at once.

The callers of the start of the Tor service share one start. A call while the start runs gets its result, also its
failure, and a call after a successful start gets the success at once. After a failed start, the next call starts
again. A shutdown during the start makes that start fail with an error which says that the Tor service was shut down
during its start, also when every step of the start succeeded, and the shutdown completes only once no Tor of the
service runs. A call of the start during a shutdown waits for the end of the shutdown and then starts again.

When the start of the node fails, for example because the Tor library refused to start Tor, the seed node and the
oracle node shut down and exit with the failure status 1, so that systemd, whose units of these nodes have
`Restart=on-failure`, starts them again instead of leaving them running without a network. A start fails when the
initialization of the application fails or completes without success. With several transports the network, and so the
start, succeeds once one of them has initialized. A shutdown which runs already keeps its exit status. The API
application and the node monitor log the failure and keep running (see "Known gaps").

### Torrc overrides and files of the Tor of the node

The configuration of the Tor transport can add entries to the torrc of the Tor which the node starts
(`torrcOverrides`). The node ignores an override of a key which it sets for its own Tor: `ControlPort`,
`ControlPortWriteToFile`, `SocksPort`, `DataDirectory`, `HashedControlPassword` and `DisableNetwork`, in any letter
case, also as an abbreviation of the option name, which Tor accepts, and in the hidden forms which Tor accepts for the
listeners, such as `__SocksPort` and `__ControlPort`, with which an override would add a second listener. In the same
forms it ignores an override of an option which would let another program control its Tor, which the node alone
controls: `ControlSocket` (a control listener on a Unix socket), `ControlSocketsGroupWritable`, `CookieAuthentication`,
`CookieAuthFile`, `CookieAuthFileGroupReadable`, and the hidden options `__HashedControlSessionPassword` (a second
password) and `__OwningControllerFD` (a control connection on an inherited file descriptor). The node logs a warning for
each override which it ignores. An override
of another key replaces the entry of the node with that key, in any letter case and also as an abbreviation which Tor
accepts, for example `AvoidDiskWrite` for `AvoidDiskWrites`. An override of a longer option which only starts like
such a key, such as `LogTimeGranularity`, replaces nothing. Every entry stays one entry of the torrc, as Tor reads it.
A key is the name of a Tor option: a letter followed by letters and digits, after one or two underscores for a hidden
option. So it cannot carry the forms `+Key`, `/Key`, `Key#comment` and `%include` of the torrc syntax. A value contains
no line break and no control character other than a tab. It is either one quoted string, in which a backslash escapes
the next character, or it contains no `#`, which would start a comment, and does not end with a backslash, which would
join the next line to the value. The rule applies to the overrides, to the entries of the node and to the bridge lines.
The start fails for an entry which breaks it. This includes a path which the node writes into the torrc, all of them
below the data directory of the application, when the path contains a `#`: the node writes its paths without quotes.

The node sets `AvoidDiskWrites 1`; an override can change it. Tor reads its GeoIP files from the bundle and gets an
empty OpenSSL configuration file, so that it reads no file below `/var/tmp/dist`, the build path of the bundle, where
another user of the system could place files.

The node publishes an onion service only if the onion address which Tor reports for the key is the address of the
key. A publication completes once the first HSDir has stored the descriptor of the onion service, which Tor reports
with `HS_DESC UPLOADED`, not at the start of the upload (`HS_DESC UPLOAD`), when no peer could find the onion service
yet. If no HSDir has stored the descriptor within `hsUploadTimeout` (120 seconds in the shipped configurations), the
publication fails and the node removes the onion service. The control connection reads lines of at most 16 KiB and
replies of at most 1 MiB; a longer line or reply ends the connection.

A server which the node publishes as an onion service listens on the address where Tor, or the relay of the sandbox in
front of Tor, connects: on the loopback address, and on Whonix on all addresses. This includes the server of the API
when the API uses the Tor transport. Its configured bind host (`application.api.server.bind.host`) applies only to the
clearnet transport. The API starts this server and publishes its onion service only once the Tor transport has
initialized, because the network of the node is ready with its first initialized transport, which with several
transports need not be Tor. If the Tor transport fails, the API does not start.

### External Tor

The node uses an external Tor instead of its own if it runs on Whonix, if the environment variable `TOR_SKIP_LAUNCH`
is `1`, `true` or `yes`, if `external_tor.config` in the Tor directory sets `UseExternalTor 1`, or if the
configuration sets `useExternalTor`. The first of these conditions which applies decides: on Whonix the node always
uses the external Tor; otherwise a set `TOR_SKIP_LAUNCH` decides alone, and a value other than `1`, `true` or `yes`
means no external Tor; otherwise `UseExternalTor` of `external_tor.config` decides when it is set; otherwise the
configuration. An empty `TOR_SKIP_LAUNCH`, or one of white space only, counts as not set, and `true` and `yes` count in
any letter case. `external_tor.config` sets the host and port of the control port and the authentication. As in a
torrc, a `#` after a value starts a comment, unless the value is a quoted string; other text after the closing quote
fails the start.

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

The control protocol of Tor has no encryption. The control connection carries the private keys of the onion services
of the node (`ADD_ONION`), and with `ControlPassword` the password, which the node sends hex-encoded. The own Tor of the
node listens on `127.0.0.1`. An external Tor on another host, for example the Whonix-Gateway, receives the keys over
the network, so that network must be one which only trusted systems can use.

While the control port refuses the connection, the node tries again for about two seconds, because the external Tor may
not listen yet. If the control port does not accept the connection within 10 seconds, or its host cannot be reached,
the start fails at once.

The node asks the external Tor for its SOCKS listeners and uses the first TCP listener which it can reach: a listener on
a specific address at that address, and a listener on a wildcard address (`0.0.0.0` or `::`) at the host of the control
port. A loopback address is an address of the host of Tor, so the node uses a listener on a loopback address only if
the host of the control port is a loopback address as well. If no listener fits, for example because a Tor on another
host listens for SOCKS connections only on its loopback address, the start fails with an error which names the
listeners. The start also fails if an entry of the configuration of the external Tor which may have opened that SOCKS
port turns the isolation of streams by their SOCKS credentials off with `NoIsolateSOCKSAuth`, because the nodes of
different user profiles could then share circuits; the error names the entry (see
[Tor stream isolation](tor-stream-isolation.md)).

The node waits up to `bootstrapTimeout` until the external Tor has bootstrapped. It does not change the configuration
of the external Tor.

If the node cannot use the external Tor, the start fails with an error which names the host and port and the cause.
The node never starts its own Tor instead, because the user may rely on the external Tor, for example for its bridges,
and the own Tor would connect directly to the Tor network.

### Whonix

On a Whonix-Workstation, the node always uses the Tor of the Whonix-Gateway. onion-grater filters the control port of
the Gateway and needs the profile of Bisq there (`sudo onion-grater-add 40_bisq`). onion-grater accepts only the empty
authentication, so the node uses it there also with `CookieAuthentication 1`; the cookie of a Whonix-Workstation is a
dummy. The node listens on all addresses, also with the server of its API, because the Tor of the Gateway connects to
the address of the Workstation. onion-grater does not report the configuration of the Tor of the Gateway, so the node
cannot check whether its SOCKS port isolates streams; it logs so and starts. Every profile of onion-grater delivers
`HS_DESC UPLOADED`, which completes a publication. Profiles before September 2024 shorten `HS_DESC UPLOAD` to a form
without the `HSDIR_INDEX` which Tor sends with every upload of a v3 descriptor, and such a shortened `UPLOAD` completes
a publication as well.

Both Whonix systems have the markers by which the node detects Whonix. On the Whonix-Gateway, which also has
`/usr/share/anon-gw-base-files/gateway`, the node does not start: the start fails with an error which tells the user to
run Bisq on a Whonix-Workstation, which uses the Tor of the Gateway.

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
restricted or off, Tor gets the network of the system. Only when the Tor of a start answers on its control port does
the node report the level and log that Tor runs in it, with a warning for a level with the network of the system or
without isolated signals. Until then the start reports no sandboxed Tor, also after sandy selected a level, so that a
start which fails before Tor answers never claims a sandboxed Tor.

`data/` is the only directory which a sandboxed Tor may write. Right before Tor runs, the node records in `data-mode` of
the Tor directory whether Tor runs in the sandbox; if it cannot record a sandboxed start, the start fails. Before Tor
runs without the sandbox on a `data/` which a sandboxed Tor may have written, because the record says so, or because
the record is missing or unreadable while `data/` existed before the start, the node clears `data/`. Tor then selects
new entry guards and downloads the directory documents again. A start in the sandbox keeps `data/`.

In a level with the network of the system or without isolated signals, the desktop application shows a notice once per
start, which the user can turn off for that set of weaknesses. It says what the sandbox cannot do on this system: with
the network of the system a compromised Tor could reach the services of the computer and of the local network, and
without isolated signals it could send signals to the other processes of the user. Where one exists, it names the way
to a stronger level: obfs4 bridges or no bridges on Linux, and no bridges on macOS, when pluggable transports need the
network of the system, and Linux 6.12 or newer for the signals. It names none where the system cannot give Tor a
network of its own, for example because it restricts user namespaces, because Bisq does not prepare the system for
them. A user who turned the sandbox off, or who uses an external Tor, gets no notice.

Pluggable transports are programs which Tor starts for bridges. The sandbox lets Tor start them only if
`sandbox.allowPluggableTransports` is `true`; it is `false` by default. The network settings of the desktop
application have a switch for it, which asks for a confirmation before it allows them, writes only this value into
`bisq.conf` of the data directory, and takes effect after a restart; the switch is disabled while the sandbox is off
and on Windows. With the value `true`, Tor may start the programs of the transports of the configured bridges, and
they end with Tor. On Linux, obfs4 keeps the network of the sandbox and connects through the egress broker; with meek,
webtunnel, snowflake or conjure, Tor and its transports get the network of the system. On macOS every transport gets
the network of the system. There the sandbox lets Tor and its transports reach the certificate verifier of the system
(the Mach service `com.apple.trustd.agent`) only when the bridges use a transport which verifies its server with it:
webtunnel, snowflake or conjure. obfs4, which uses no TLS, and meek, which verifies its server with a root certificate
of its own, get no Mach service, and neither does Tor without transports. On Windows Tor cannot start pluggable
transports in its sandbox.

If the sandbox is on, the start fails with an error which names the reason and how to turn the sandbox off, and the
node never starts Tor without the sandbox instead, when:

- the bridges need pluggable transports and `sandbox.allowPluggableTransports` is not `true`, or the system is
  Windows;
- a torrc override sets a proxy option or `ServerTransportPlugin`, or without configured bridges sets
  `ClientTransportPlugin`, also as an abbreviation of the option name, which Tor accepts;
- a torrc override sets `GeoIPFile` or `GeoIPv6File`, also as an abbreviation: the sandbox lets Tor read only the
  GeoIP files of the bundle, and with another file a country selector such as `ExcludeNodes {us}` would match no relay,
  without an error;
- in a level in which Tor connects out only through the egress broker, which reaches public addresses only, the
  configuration has the directory authorities of a test network, which listen on this host, or every bridge is on an
  address of this host or of the local network. If only some bridges are, the node logs a warning with their number;
  neither names the addresses;
- the system can enforce no level of the sandbox, for example Linux without Landlock, or the build contains no sandy;
- sandy cannot tell now whether the system enforces the sandbox, or it ends the run before Tor runs, for example
  because another process took a port of the policy after the node released it for sandy. The node then starts once
  more with other ports, within the same start, and only a second such refusal fails the start.

Whether sandy ended the run before Tor ran, the node decides by the status record which sandy writes into
`sandy/run-status` of the Tor directory, which Tor can neither write nor remove, and not by the exit status, which Tor
can choose as well. Every other end of a sandboxed Tor before it answers on its control port fails the start without
the advice to turn the sandbox off. The error of a failed start quotes the end of `tor-stderr.log`, the standard error
of Tor, and in the sandbox also of sandy. There Tor can rewrite that file, so the error first names what the node knows,
the exit status and the status record, and then the quote, labeled as written by sandy and by Tor in its sandbox and
not verified by Bisq. A start without the sandbox quotes the file without the label.

When the bridges need pluggable transports which the configuration does not allow, the start error of the desktop
application names the transports and the ways out: allow them with `sandbox.allowPluggableTransports`, except on
Windows, where the sandbox cannot run them; remove the bridges or use bridges without pluggable transports; or turn the
sandbox off. Its switch in the network settings is out of reach after a failed start, so the message names the entries
which the user adds to `bisq.conf`. It names no path of the user, because the error popup copies its message into a bug
report.

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

A Tor which an attacker compromised over the network can leave files in `data/` which exploit the next Tor that parses
them, and it controls its own exit status and output. So the advice to turn the sandbox off follows only from the
status record of sandy, which Tor cannot write, and the node clears `data/` before a Tor without the sandbox, which has
all rights of the user, could parse what a sandboxed Tor wrote. The ports of the policy are free only at the moment
when the node reserves them, so a refusal over a taken port is retried once instead of ending in that advice.

A publication completes only once an HSDir has stored the descriptor, so that a peer which looks up the onion service
of the node after the publication finds it. An external Tor whose SOCKS port does not isolate streams by their SOCKS
credentials would let the connections of different user profiles share circuits, which
[Tor stream isolation](tor-stream-isolation.md) rules out.

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
- The user interface shows no level of the sandbox, only the notice for a weak level; the log names the level.
- At a level with the network of the system, and with pluggable transports other than obfs4 on Linux, the sandbox does
  not keep Tor from connecting to local services of the user, for example the API of the node; their own
  authentication protects them. Tor cannot read the cookie and key files of the node.
- Bridges with pluggable transports need the sandbox off on Windows.
- On Windows Tor runs in its sandbox only in a desktop session. In session 0, where services and logins over SSH run,
  Tor ends at once when it starts in its sandbox. The check of sandy warns in session 0 but does not refuse, and it
  detects no other window station which admits no AppContainer; the node logs the warning, and the error of the failed
  start names the session as the likely cause.
- A compromised Tor in the sandbox which fills the file system through `data/` can make sandy fail to create its
  status record at a later start, which the node takes for a refusal before Tor ran and answers with the advice to
  turn the sandbox off. If the user follows it, the node clears `data/` before Tor runs without the sandbox.
- The isolation check of an external Tor reads the configuration which that Tor reports. A Tor which reports a false
  configuration, or a filter in front of Tor which does not report it, as onion-grater on Whonix, is not checked.
- With Tor and I2P, which the network settings of the desktop application offer, the network of the node is ready with
  its first initialized transport. A failed start of Tor then fails no start: the node runs without Tor, logs the
  failure with its reason at error level, and shows the user no notice.
- The API application and the node monitor keep running without a network after a failed start and only log the
  failure. Whether they should exit with the failure status, as the seed node and the oracle node do, is an open
  decision, which the daemon of Bisq 1 shares.
- The Tor log in the log zip names the bridges which Tor used. A user with private bridges has to remove these lines
  before sharing the zip; whether the node should remove them is an open decision.
