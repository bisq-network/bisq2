# Trade restrictions of emergency alerts

A security manager can publish an emergency alert which restricts Bisq Easy trading. This specification defines what
a node does while such an alert exists.

## Terms

- A halt of trading is an emergency alert with `haltTrading`.
- A minimum version for trading is an emergency alert with `requireVersionForTrading` and a `minVersion`. The node
  uses the newest such alert.
- An alert applies to a node if its app type is the app type of the node, or `UNSPECIFIED`. The desktop app and the
  api-app use the app type `DESKTOP`.

## Rules

1. While a halt of trading applies to the node, the node takes no offer, performs no trade action and processes no
   received trade message.
2. While a minimum version for trading applies to the node and its own version is lower, the node does the same.
3. While a minimum version for trading applies to the node, it also refuses to take an offer whose maker has a lower
   version. The version of the maker is the higher of two versions:
   - the version in the user profile of the maker, which the maker signs when it publishes the profile;
   - the version which created the offer.
   A version which the node cannot read counts as lower. The node refuses the take before it creates the trade, for
   every client: the desktop shows a message, and the API answers with 400 and the error code
   `MAKER_VERSION_BELOW_MIN_REQUIRED`, with the required version.

## Reasons

- Rules 1 and 2: the security manager uses these alerts in an emergency, and to move users to a version before an
  incompatible change, for example the cutoff date of signature V1.
- Rule 3: a node of the maker which rule 2 restricts acknowledges a take offer request when it decrypts it, and then
  drops it without an answer. Nodes up to version 2.1.13 do the same. The taker would wait without an error and
  without a timeout. Only the node of the taker can prevent this, because the released maker cannot change.
- Rule 3, sources of the version: both are lower bounds of the version which the maker runs. A node rebuilds its
  profiles with its own version when it starts, and it publishes the selected profile again, so the version of the
  profile follows an update of the maker. An offer keeps the version which created it.

## Compatibility

- Nodes up to version 2.1.13 apply an alert only to their own version, as in rules 1 and 2.
- The error code `MAKER_VERSION_BELOW_MIN_REQUIRED` is new. Its text does not contain "For trading you need to have
  version", which released mobile clients read as a restriction of their own node.

## Known limitations

- **Takers of another app type.** The check uses the alert which applies to the taker. A mobile node does not check
  makers under an alert for `DESKTOP` only. The security manager publishes the alert for all app types which trade.
- **Makers which updated without publishing their profile.** A node publishes only its selected profile at its start.
  The offer of another profile of an updated maker keeps the earlier version, so the node of a taker refuses it until
  the maker publishes that profile.
- **Open trades across versions.** A node which rules 1 or 2 restrict drops the trade messages of its open trades after
  it acknowledged them. The trade stops until the alert ends or the user updates; trade chat and mediation still work.
  - What would remove it: a restricted node keeps such messages and processes them when the restriction ends.
  - Who decides: the developers.
