# Offerbook API

The offerbook API publishes the Bisq Easy offers of the node to API clients, such as Bisq Connect.
It consists of the WebSocket topic `OFFERS`, the WebSocket topic `NUM_OFFERS` and the REST list
`GET /api/v1/offerbook/markets/{currencyCode}/offers`. This specification defines which offers they
publish and how the three stay consistent with each other.

## Shown offers

The API shows an offer when the offerbook validity rule accepts it, or when the offer is an own
offer of the node.

The offerbook validity rule is the rule of the Bisq Easy offerbook domain which the desktop offerbook
applies. An offer is valid when its maker is not banned and not ignored, when its amounts can be
converted at the current price on both sides, and when it is a buy offer or its maker has sufficient
reputation for its amount.

Own offers are shown also when they are invalid. The maker must see an own offer whose reputation
requirement is no longer met, so that the app can warn its user. An invalid offer of another maker
is hidden, like in the desktop offerbook. Its reputation is not checked.

An offer whose maker profile is unknown to the node is not shown until the profile arrives. The item
of an offer shows the maker profile, so no item can be built without it. A check without the profile
would also use a reputation score of 0 and cache the offer as insufficient.

## Seller reputation verdict

Each item carries the field `hasSellerSufficientReputation`. For a shown offer of another maker, the
field is `true`, because the validity rule already checked the reputation. For an own offer, the
field carries the current result of the reputation check. The field is advisory: the node checks the
reputation again when an offer is taken.

A missing market price gives `true`, as the required reputation cannot be derived without it. Buy
offers give `true`.

## Events of the OFFERS topic

The node sends `ADDED` with the item of an offer when the offer becomes shown, and again when its
validity or its verdict changes while it is shown. Clients replace an item with the same offer ID.
An own offer which became valid is sent again, because its item showed no amounts while the price
was missing.

The node sends `REMOVED` when a shown offer leaves the offerbook or becomes hidden. The payload of
a removal is the last item which the node sent for the offer. Released clients parse the payload of
every event of the topic as a list of items, so a removal cannot carry only the offer ID. An offer
which was never sent produces no removal.

Additions and removals of offerbook messages are published right away. A change of visibility caused
by a market price, a reputation score, the ban list or the ignore list is applied by a periodic pass
over all offers. The pass starts at most once every 30 seconds, because these changes come in bursts
and each pass checks every offer. A visibility change which happens during a pass is applied by the
next pass.

## Snapshot of the OFFERS topic

The snapshot which a subscriber receives contains the offers which the node has sent, or would have
sent, to the other subscribers: the recorded offers. Each item is built again with the current
amounts and reputation and with the recorded verdict. When no item can be built, the snapshot carries
the last item sent. An offer which is shown but not recorded yet, for example because its maker
profile arrived after the offer, is left out of the snapshot and sent by the next pass.

## NUM_OFFERS topic

`NUM_OFFERS` publishes, per market, the count of the recorded offers of that market. The count and
the `OFFERS` topic therefore describe the same set of offers, including own offers with insufficient
reputation, which the desktop offerbook does not count.

The count of a market changes in the same step which adds or removes an offer of the market. A pass
publishes the count of a market once, after it has checked all offers of the market. Every market
has a count, also a market without offers, because clients treat a market without a count as
unknown and keep waiting for it.

## REST list

While the WebSocket service records the offers, the REST list of a market contains the same items as
the snapshot of the `OFFERS` topic for that market, so that the list matches `NUM_OFFERS`.

When the WebSocket service is disabled or not initialized yet, nothing records the offers. The REST
list then applies the rule of the shown offers itself and builds the items at the time of the
request.

## Limitations

The order of a snapshot and the events sent during the subscription is not guaranteed by the
WebSocket subscription itself. A client can receive an event before the snapshot which already
reflects it. Until the subscription mechanism orders them, a lost `ADDED` leaves a shown offer out of
the client list, and a lost `REMOVED` leaves a hidden offer in it, until the client reloads the list.

A shown offer whose maker profile became unknown stays as the clients hold it until the profile
returns or the offer leaves the offerbook, unless the maker is banned or ignored. The ban and ignore
checks need no profile; the other checks would cache the offer as insufficient.

An item is sent again only when the validity or the verdict of its offer changes. Values of the item
which depend on the market price, such as the amounts of an offer with a floating price, become
stale on the clients. The snapshot and the REST list build fresh items.

The REST count `GET /api/v1/offerbook/markets/offers/count` counts every offer message of each
market, shown or not, and is not part of this consistency. No client uses it. Changing it is a
separate decision.
