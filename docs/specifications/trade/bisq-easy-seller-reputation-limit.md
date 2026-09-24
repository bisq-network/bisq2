# Bisq Easy Seller Reputation Limit

In a Bisq Easy trade the buyer sends the fiat payment first, and the seller sends the bitcoin after receiving it. The seller gives no deposit, so the seller's reputation is the buyer's only security against a seller who does not send the bitcoin. Therefore the amount a seller can trade is limited by the seller's reputation score.

## Rule

- A trade amount requires a reputation score of 200 per USD of its value. 200 equals the score required to create a sell offer (1200) divided by the minimum trade amount (6 USD), so a seller who can create a sell offer can also trade the minimum amount.
- The USD value of an amount is computed with two market prices: the amount is converted to BTC with the BTC price in the offer's currency, and the BTC amount is converted to USD with the BTC price in USD. The USD value is rounded half up to whole USD before it is multiplied by 200. Example: with BTC at 50000 EUR and at 60000 USD, 500 EUR are 600 USD and require a score of 120000.
- The seller's score is accepted with a tolerance of 5%: the score is multiplied by 1.05 and rounded to a whole number, and the amount is allowed if the result is at least the required score. The effective limit is therefore 1.05 times the score. The tolerance exists because the two traders' nodes can have slightly different market prices and then compute slightly different USD values for the same amount. A modified client can use the full tolerance, so a seller can trade up to 1.05 times the amount the score covers.
- The seller's maximum amount, derived as described below, starts from at most 600 USD, which a score of 120000 reaches. The client's amount range for new offers also ends at the equivalent of 600 USD. The maker's check described below does not apply this cap itself: it compares the taker's score with the amount of the take offer request, and that amount must be within the maker's own offer.
- Creating a sell offer requires a score of at least 1200. The client checks this when the user creates an offer.

## Seller Maximum in the Client

The client derives the seller's maximum amount from the score in a way that the maker accepts:

1. The score is divided by 200 and rounded down to whole USD, with at most 600 USD.
2. This USD amount is converted through BTC to the offer's currency with the client's own market prices, and rounded to whole units of that currency.
3. The amount is lowered by one whole unit at a time until the rule requires at most the score for it. The tolerance is not used here, because it is left for price differences between the two traders' nodes.

Step 2 rounds to the nearest unit and not down, because the conversion through BTC can end slightly below a whole unit: with BTC at 83875.15 USD, 6 USD come back as 5.9995 USD, which rounding down would turn into 5 USD. Step 3 is needed because rounding up in step 2 can raise the USD value enough for the rule to round it to the next USD. Example: with BTC at 55200 EUR and at 60000 USD, a score of 1200 allows 6 USD, which are 5.52 EUR; 6 EUR would be 6.52 USD, count as 7 USD and require 1400, so the maximum is 5 EUR.

With the same market prices on both nodes, the maker accepts the maximum. The maximum is computed with the prices of the node that shows it. Clients that compute the maximum themselves, for example through the API, need to use the same steps, otherwise the maker can reject their maximum.

## Enforcement

The rule protects the buyer, so the buyer's node enforces it:

- When the buyer takes a sell offer, the buyer's client checks the maker's score before it lets the buyer take the offer, and it limits the amount to the seller's maximum. There is no additional check in the trade protocol, because only the buyer is at risk and the check runs in the buyer's own client.
- When the seller takes a buy offer, the buyer is the maker. The maker's node checks the taker's score against the amount of the take offer request before it signs the contract, and rejects the request if the score is too low.

The seller's client also shows the seller's maximum and warns when the seller's score is too low for an offer, but the buyer cannot rely on that, because a modified client can skip these checks. The maker's check uses the maker's own market prices and the maker's own reputation data for the taker. The offer in the take offer request must be an offer of the maker. Apart from the offer, the check takes only two values from the request: the amount, and the taker's network ID, with which it looks up the taker's score. Before the check, the maker verifies the amount against its offer and verifies that the network ID is the one of the sender of the request.

## Market Prices at the Maker

The maker's check uses its BTC price in the offer's currency and its BTC price in USD. Both prices must be younger than 12 hours, and their timestamps must not be more than 5 minutes apart. 5 minutes is the age after which the client shows a market price as stale. A USD value computed from two prices of very different times could understate the trade value. In a USD market both prices are the same price.

## Fail-Closed Cases

The maker rejects the take offer request, before it signs the contract and before any payment, when:

- it has no market price for the offer's currency or for USD;
- one of the two prices is 12 hours old or older;
- the two prices are more than 5 minutes apart;
- the taker's score does not cover the amount.

The maker knows the taker's score only after it has received the taker's user profile and the taker's reputation data from the network. Until then the taker's score is 0. Right after the maker has started, for example while it processes take offer requests that arrived while it was offline, this data may not have arrived yet, and its stored market prices may be older than 12 hours until it receives current prices. Such requests are rejected. A rejected request does not remove the offer.

## Failure Reported to the Taker

A rejection because of the score is reported to the taker with the failure reason `SELLER_REPUTATION_TOO_LOW`. The taker's client shows it as an expected trade failure with the maker's message. A rejection because of market prices is reported with the failure reason `UNKNOWN`, which the taker's client shows as an unexpected error with the maker's error details.

## Older Versions

- A maker with an older version does not check the taker's score.
- A taker with an older version does not know the failure reason `SELLER_REPUTATION_TOO_LOW`, reads it as `UNKNOWN` and shows an unexpected error.
- A taker with an older version derives its maximum with the USD amount of step 1 rounded half up instead of down, and without step 3. An updated maker rejects that maximum for some low scores, also in the USD market. Example: a score of 1300 gives 7 USD, which requires 1400, more than the 1365 that the tolerance allows. The rejection happens at the take offer request, before any payment.

## Known Limitation

Below a score of 4000, the tolerance of 5% of the score is less than 200, the score for one USD. If the maker's prices value an amount slightly higher than the taker's prices, the USD value of an amount just below a half USD at the taker can be just above it at the maker, and the maker then rounds it to the next USD. The maker then requires 200 more than the taker computed, which the tolerance does not cover. Example: with BTC at 73808.56 EUR and at 83875.15 USD at the taker's node, the maximum for a score of 2400 is 11 EUR, which are 12.4999 USD and require 2400. At a maker with the same USD price and a EUR price that is 0.05% lower, 73771.66 EUR, 11 EUR are 12.5066 USD, which count as 13 USD and require 2600, more than the 2520 that the tolerance allows.

## Not Covered

MuSig trades have their own amount limits and are not covered by this specification.
