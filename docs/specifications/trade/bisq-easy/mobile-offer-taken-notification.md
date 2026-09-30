# Mobile notification when an offer is taken

The maker receives a mobile trade notification after accepting a peer's take-offer request. The notification
identifies the trade so the mobile client can open it. The taker receives no notification for initiating the trade.

The maker's automatic response to a received request represents a peer action. It must not be suppressed as a
local user action. Both buyer and seller makers receive the notification, including when account data or a Bitcoin
address arrives before the take-offer request.

Later payment-data transitions do not repeat the offer-taken notification. Loading an existing trade observes its
current state without emitting a historical notification. Payment-information notifications remain separate events.

Verification must drive the trade-state observer through the first accepted-request state of each maker protocol
branch and check delivery, trade identity, taker silence, and suppression of historical notifications.
