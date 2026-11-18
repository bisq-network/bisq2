# Bisq 2.1.16 Highlights

## Trades Continue After a Restart

A trade message which arrived out of order is now stored with the trade. Before, such a trade could wait without end after a restart. Closed trades also no longer stay among the open trades.

## Safer Configuration

If `bisq.conf` cannot be read, Bisq stops with a clear error instead of starting without the user's settings, such as the Tor bridges.

## Help When Tor Cannot Start in Its Sandbox

When the Tor sandbox refuses the configured bridges or cannot run, the desktop explains the choices and their risks and saves the selected setting.

## More Reliable Start

A slow first connection to the network, for example through bridges, no longer fails the start after 90 seconds.

## Updated Tor

Bisq bundles Tor 0.4.9.16 from Tor Browser 16.0.1 and runs it in the sandbox of sandy 1.2.0.

## Updated Java Runtime

The bundled Java runtime switches to Amazon Corretto 21.0.13.1.2, which fixes Java4Shell (CVE-2026-114938).

## Before Updating

Check a hand-edited `bisq.conf`; a file that cannot be parsed now stops the start. The Tor log is now `tor/tor.log`. Security managers must publish a trading halt for both desktop and mobile node app types to stop every node.

See [notable changes](notable-changes.md) and [full release notes](full-release-notes.md) for details.
