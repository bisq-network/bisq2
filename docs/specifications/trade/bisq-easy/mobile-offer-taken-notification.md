# Mobile notification when an offer is taken

The maker receives a mobile trade notification after accepting a peer's take-offer request. The notification
identifies the trade so the mobile client can open it. The taker receives no notification for initiating the trade.

The maker's automatic response to a received request represents a peer action. It must not be suppressed as a
local user action. Both buyer and seller makers receive the notification, including when account data or a Bitcoin
address arrives before the take-offer request.

Later payment-data transitions do not repeat the offer-taken notification. Loading an existing trade observes its
current state without emitting a historical notification. Payment-information notifications remain separate events.

The notification service observes the trade states before the trade service starts. The state which a trade has when
the node loads it from its store is historical. The trade service then applies queued events and replayed messages,
for example from the mailbox, and can create a maker trade from a replayed take-offer request. These state changes
happen in the current run, so they send notifications like a live message.

A trade which the node loads in a state of the payment-information exchange counts as notified for payment
information: the exchange started before the load, so its notification is historical. A later transition to another
state of the exchange, at startup or later, sends no payment-information notification.

One state transition produces at most one notification. A buyer maker who receives the account data before the
take-offer request enters a state which means both "offer taken" and "payment information received". In this case
only the offer-taken notification is sent. Both notifications for a trade share one device notification, so a second
notification would replace the offer-taken notification before the maker sees it. The offer-taken notification is the
first time the maker learns about the trade, and the trade view shows the received account data. The trade then counts
as notified for payment information, so the maker's own later step of sending the Bitcoin address sends no
payment-information notification.

Verification must drive the trade-state observer through the first accepted-request state of each maker protocol
branch and check delivery, trade identity, taker silence, and suppression of historical notifications. It must check
the total number of notifications, not only the offer-taken notifications, so that a second notification for the
same transition fails the test. It must also load a trade in a state of the payment-information exchange, move it to
another state of the exchange and check that no notification is sent.
