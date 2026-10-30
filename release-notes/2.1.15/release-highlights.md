# Bisq 2.1.15 Highlights

## Bundled and Sandboxed Tor

Bisq now runs its bundled Tor on Linux as well as macOS and Windows. Its own Tor runs in a sandbox by default. The desktop explains when the system can provide only a weaker sandbox level.

## Bridge and External Tor Support

Built-in or custom bridges can be configured in `bisq.conf`, with bundled pluggable transport programs. The desktop adds a switch to permit those programs inside the sandbox. Bisq also supports external Tor, including Whonix, with stronger control-port authentication and a SOCKS isolation check.

## More Predictable Failures

A bridge that cannot run stops Tor startup instead of connecting directly. Seed and oracle nodes exit with a failure status when startup fails. HTTP clients reject oversized responses, and Bisq avoids deleting files through links or Windows junctions.

## Before Updating

Linux no longer needs the distribution's Tor package for Bisq. Pluggable transports in the sandbox need explicit permission; Windows requires the sandbox off to use them. Backups can include external Tor credentials, so review them before sharing.

See [notable changes](notable-changes.md) and [full release notes](full-release-notes.md) for details.
