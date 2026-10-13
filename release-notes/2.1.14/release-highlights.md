# Bisq 2.1.14 Highlights

## Safer Bisq Easy Trades

- Take requests must match a current maker offer and its price before a trade is stored.
- Buyers enforce seller reputation limits as maker and as taker, also for takes through the API and the mobile node.
- Bitcoin URI and Lightning invoice amounts are checked against the agreed trade amount.

## Stronger Network Integrity and Tor Privacy

- New storage signatures protect versions, ordering fields, and payloads. Owner-specific storage keys prevent interference between publishers.
- Tor separates outgoing connections of different identities and gives each HTTP connection fresh isolation credentials.
- Network message sizes and pending queues have tighter limits.

## Desktop and Connected Devices

- Support chat adds Markdown and clearer link destinations; mentions and private-chat navigation are improved.
- Bisq Easy history gains CSV export and a contact-peer action. Language settings add country and currency selection.
- Bisq Connect adds device listing, revocation, and session expiry. APIs expand chat, contacts, alerts, and payment-account support, and stop exposing private keys.

## More Reliable Storage

- Store replacement preserves the previous file until the new file is ready, and write failures report failure.
- Invalid records no longer interrupt the remaining startup replay or bonded-role revalidation.
- When stored data cannot be read, Bisq keeps a copy of the file for 90 days, and the desktop app lists the affected files.

## Do Not Share Old Log Files

Log files written by versions 2.1.3 to 2.1.13 contain the onion private keys of all identities of the node. The desktop error report now hides these keys, but the log files on disk still contain them.

## Update Before November 25, 2026

Updated nodes require V2 storage signatures from **2026-11-25 00:00 UTC**. Oracle and seed operators must upgrade and migrate publications; security managers, moderators, and release managers must republish long-lived data with this version. API clients must support default session authentication and the revised DTOs.

MuSig and integrated wallet improvements remain available only through developer-mode MuSig activation.

See [notable changes](notable-changes.md) and [full release notes](full-release-notes.md) for compatibility details and the verified comparison with `v2.1.13`.
