# Trade restrictions of emergency alerts

A security manager can publish an emergency alert which restricts Bisq Easy trading. This specification defines what
a node does while such an alert exists.

## Terms

- A halt of trading is an emergency alert with `haltTrading`.
- A minimum version for trading is an emergency alert with `requireVersionForTrading` and a `minVersion`. The node
  uses the newest such alert.
- An alert applies to a node if its app type is the app type of the node, or `UNSPECIFIED`. The desktop app and the
  api-app use the app type `DESKTOP`, and a mobile node uses `MOBILE_NODE`. A mobile client trades through the api-app
  of its trusted node, so the alert for `DESKTOP` applies to its trades.
- The version of a node is the version of Bisq 2 which it runs. A mobile node contains Bisq 2, and a mobile release
  can contain an earlier version than the current desktop release.

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
- Rule 3, sources of the version: a node rebuilds its profiles with its own version when it starts, and it publishes
  the selected profile again, so the version of the profile follows an update of the maker. An offer keeps the version
  which created it, so an offer which the maker created after its update shows the update also if the maker did not
  publish that profile again. Unless a relay changed the version of the offer, both versions are lower bounds of the
  version which the maker runs. The maker signs the version of its profile. Only signature V2 covers the version of
  the offer (see Known limitations).

## Compatibility

- Nodes up to version 2.1.13 apply an alert only to their own version, as in rules 1 and 2. They apply only an alert
  whose app type is their own app type, so an alert with `UNSPECIFIED` does not restrict them.
- The error code `MAKER_VERSION_BELOW_MIN_REQUIRED` is new. Released mobile apps ignore error codes and replace the
  text of a take failure if it contains certain fragments. With "min. version required for trading" and "version <x>
  installed" they ask the user to update their own node, and with "Trading is on halt" they report a halt of trading.
  The text of this error contains none of these fragments, so the mobile apps show the text itself.

## Known limitations

- **The alerts of taker and maker differ.** The node of the taker checks the maker with the alert which applies to the
  taker, because neither the profile nor the offer shows the app type of the maker. If the alerts which apply to
  taker and maker differ, the decision of the taker can be wrong:
  - The alert of the taker requires a version which the alert of the maker does not require, or no alert applies to
    the maker. The node of the taker refuses a maker which would accept the take. Example: an alert for `DESKTOP`
    requires 2.1.14 and no alert for `MOBILE_NODE` exists. Desktop users and mobile clients cannot take the offers of
    mobile nodes which contain 2.1.13.
  - The alert of the maker requires a version which the alert of the taker does not require, or no alert applies to
    the taker. The node of the taker does not refuse the restricted maker, and the take hangs as without rule 3.
    Example: an alert for `MOBILE_NODE` requires 2.1.14 and no alert for `DESKTOP` exists. A desktop user takes the
    offer of a mobile node which contains 2.1.13.
  - An alert with `UNSPECIFIED` applies to every node of version 2.1.14 or later, but to no node up to 2.1.13 (see
    Compatibility). The node of a taker therefore refuses makers up to 2.1.13, although the alert does not restrict
    them.
  - Operational rule: the security manager publishes a minimum version for trading as one alert for each app type
    which trades, `DESKTOP` and `MOBILE_NODE`, with the same `minVersion`. Then the first two cases do not occur. If
    no mobile release contains this version yet, the alert stops trading on mobile nodes until such a release exists.
    The security manager does not use `UNSPECIFIED` for a minimum version as long as nodes up to 2.1.13 trade.
  - What would remove it: the maker states its app type in its profile or offer, covered by its signature. The node of
    the taker then uses the alert which applies to the maker.
  - Who decides: the developers.
- **Makers which updated without publishing their profile.** A node publishes only its selected profile at its start.
  The offer of another profile of an updated maker keeps the earlier version, so the node of a taker refuses it until
  the maker publishes that profile.
- **Version of an offer changed by a relay.** Signature V1 does not cover the version of an offer. Before the cutoff
  date of signature V1, a relay can remove signature V2 of an offer and change its version (see the first known
  limitation of [`storage-request-signatures.md`](../network/storage-request-signatures.md#known-limitations)).
  - A higher version lets a taker take the offer of a maker which rule 2 restricts. The take then hangs, as without
    rule 3.
  - A lower version makes a taker refuse a maker which would accept the take, if the profile of the maker also has an
    earlier version.
  - The change gives the relay no access to funds or trade data.
  - What removes it: the cutoff date of signature V1. From then on, nodes accept and keep only offers with signature
    V2, which covers the version of the offer.
  - What would remove it earlier: rule 3 uses only the version of the profile. Then the node of a taker also refuses
    an updated maker whose offer was created after the update, if the maker did not publish that profile again.
  - Who decides: the developers. This version uses the higher of both versions.
- **Open trades across versions.** A node which rules 1 or 2 restrict drops the trade messages of its open trades after
  it acknowledged them. The trade stops until the alert ends or the user updates; trade chat and mediation still work.
  - What would remove it: a restricted node keeps such messages and processes them when the restriction ends.
  - Who decides: the developers.
