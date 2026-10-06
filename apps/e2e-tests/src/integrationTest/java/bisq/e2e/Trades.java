/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.e2e;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static bisq.e2e.ApiClient.JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Steps and checks of Bisq Easy trades through the API, shared by the test classes. */
final class Trades {
    static final Duration STEP_TIMEOUT = Duration.ofMinutes(2);
    // 150 USD, the fiat precision is 4
    static final long QUOTE_AMOUNT = 1_500_000;
    static final String BTC_ADDRESS = "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq";
    static final String TX_ID = "4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b";
    private static final Set<String> FAILED_STATES = Set.of("REJECTED", "PEER_REJECTED", "CANCELLED", "PEER_CANCELLED",
            "FAILED", "FAILED_AT_PEER");

    private Trades() {
    }

    // An offer with market price, a fixed amount of 150 USD, main chain and Zelle
    static String createOffer(ApiClient maker, String direction) {
        return maker.createOffer(OffersAcrossVersionsTest.offerRequest(maker.market("USD"), direction,
                JSON.createObjectNode().put("type", "QuoteSideFixedAmountSpec").put("amount", QUOTE_AMOUNT),
                JSON.createObjectNode().put("type", "MarketPriceSpec"),
                List.of("MAIN_CHAIN"), List.of("ZELLE")));
    }

    // The taker derives the Bitcoin amount from the fiat amount and the market price, which all nodes get from the fake
    // price node. Fiat has the precision 4 and Bitcoin the precision 8.
    static long baseAmount(FakePriceServer priceServer) {
        return Math.round(QUOTE_AMOUNT * 1e8 / (priceServer.getPrice("USD") * 1e4));
    }

    static Optional<JsonNode> findTrade(ApiClient api, String tradeId) {
        return api.openTrades().stream()
                .filter(item -> item.path("trade").path("id").asText().equals(tradeId))
                .findAny();
    }

    // Waits until the current state of the trade on the node matches. Fails at once if the trade failed.
    static void awaitTradeState(ApiClient api,
                                String tradeId,
                                String description,
                                Predicate<String> expectedState) throws InterruptedException {
        MixedVersionNetwork.await(description, STEP_TIMEOUT, () -> {
            JsonNode properties = api.tradeProperties().get(tradeId);
            if (properties == null) {
                return false;
            }
            String state = properties.path("tradeState").asText();
            if (FAILED_STATES.contains(state)) {
                throw new AssertionError("Trade " + tradeId + " ended in " + state + ". Error: " +
                        properties.path("errorMessage").asText() + " Error of the peer: " +
                        properties.path("peersErrorMessage").asText());
            }
            return expectedState.test(state);
        });
    }

    static void awaitClosed(ApiClient api, String tradeId) throws InterruptedException {
        MixedVersionNetwork.await("the closed trade leaves the open trades", STEP_TIMEOUT,
                () -> findTrade(api, tradeId).isEmpty());
    }

    /**
     * Runs a trade from the offer to its closure: the maker publishes an offer with market price and a fixed amount, the
     * taker takes it, and both sides go through all steps until BTC_CONFIRMED. Then both sides must agree on the
     * contract, and both close the trade.
     */
    static void runTrade(ApiClient maker, ApiClient taker, String makerDirection, FakePriceServer priceServer)
            throws Exception {
        String offerId = createOffer(maker, makerDirection);
        OffersAcrossVersionsTest.awaitOffers(taker, List.of(offerId), "the taker sees the offer of the maker");

        long baseAmount = baseAmount(priceServer);
        String tradeId = taker.takeOffer(offerId, baseAmount, QUOTE_AMOUNT, "MAIN_CHAIN", "ZELLE");
        completeAfterTake(maker, taker, makerDirection, tradeId, baseAmount, QUOTE_AMOUNT, BTC_ADDRESS);
    }

    /**
     * The steps from the take to the closure: both sides get the trade, the seller sends its account data, the buyer its
     * Bitcoin address or Lightning invoice, and the buyer confirms the fiat payment. Then {@link #completeAfterFiatSent}
     * follows.
     */
    static void completeAfterTake(ApiClient maker,
                                  ApiClient taker,
                                  String makerDirection,
                                  String tradeId,
                                  long baseAmount,
                                  long quoteAmount,
                                  String bitcoinPaymentData) throws Exception {
        ApiClient seller = makerDirection.equals("SELL") ? maker : taker;
        ApiClient buyer = seller == maker ? taker : maker;
        awaitTradeState(maker, tradeId, "the maker has the trade", state -> true);
        awaitTradeState(taker, tradeId, "the taker has the trade", state -> true);

        seller.tradeEvent(tradeId, "SELLER_SENDS_PAYMENT_ACCOUNT", "e2e payment account of the seller");
        buyer.tradeEvent(tradeId, "BUYER_SEND_BITCOIN_PAYMENT_DATA", bitcoinPaymentData);
        awaitTradeState(buyer, tradeId, "the buyer receives the account data",
                state -> state.contains("BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA"));
        awaitTradeState(seller, tradeId, "the seller receives the Bitcoin address",
                state -> state.contains("SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS"));

        buyer.tradeEvent(tradeId, "BUYER_CONFIRM_FIAT_SENT", null);
        awaitTradeState(seller, tradeId, "the seller learns that the fiat was sent",
                state -> state.equals("SELLER_RECEIVED_FIAT_SENT_CONFIRMATION"));

        completeAfterFiatSent(buyer, seller, tradeId, baseAmount, quoteAmount);
    }

    // The steps from the confirmation of the fiat receipt to the closure, and the comparison of the contract
    static void completeAfterFiatSent(ApiClient buyer, ApiClient seller, String tradeId, long baseAmount, long quoteAmount)
            throws Exception {
        seller.tradeEvent(tradeId, "SELLER_CONFIRM_FIAT_RECEIPT", null);
        awaitTradeState(buyer, tradeId, "the buyer learns that the fiat was received",
                state -> state.equals("BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION"));

        seller.tradeEvent(tradeId, "SELLER_CONFIRM_BTC_SENT", TX_ID);
        awaitTradeState(buyer, tradeId, "the buyer learns that the Bitcoin was sent",
                state -> state.equals("BUYER_RECEIVED_BTC_SENT_CONFIRMATION"));

        buyer.tradeEvent(tradeId, "BTC_CONFIRMED", null);
        seller.tradeEvent(tradeId, "BTC_CONFIRMED", null);
        awaitTradeState(buyer, tradeId, "the buyer completes the trade", state -> state.equals("BTC_CONFIRMED"));
        awaitTradeState(seller, tradeId, "the seller completes the trade", state -> state.equals("BTC_CONFIRMED"));
        JsonNode tradeOfBuyer = findTrade(buyer, tradeId).orElseThrow();
        JsonNode tradeOfSeller = findTrade(seller, tradeId).orElseThrow();

        // Both sides use the same contract. Its hash is the base of the signatures of both sides, so a difference here
        // would also break a mediation.
        for (String field : List.of("price", "formattedPrice", "formattedBaseAmount", "formattedQuoteAmount")) {
            assertEquals(tradeOfBuyer.path(field), tradeOfSeller.path(field), field + " differs between buyer and seller");
        }
        JsonNode contractOfBuyer = tradeOfBuyer.path("trade").path("contract");
        JsonNode contractOfSeller = tradeOfSeller.path("trade").path("contract");
        for (String field : List.of("baseSideAmount", "quoteSideAmount", "agreedPriceSpec", "marketPrice",
                "baseSidePaymentMethodSpec", "quoteSidePaymentMethodSpec", "takeOfferDate")) {
            assertEquals(contractOfBuyer.path(field), contractOfSeller.path(field), "contract field " + field + " differs");
        }
        assertEquals(baseAmount, contractOfBuyer.path("baseSideAmount").asLong(), "Bitcoin amount of the contract");
        assertEquals(quoteAmount, contractOfBuyer.path("quoteSideAmount").asLong(), "fiat amount of the contract");

        buyer.tradeEvent(tradeId, "CLOSE_TRADE", null);
        seller.tradeEvent(tradeId, "CLOSE_TRADE", null);
        awaitClosed(buyer, tradeId);
        awaitClosed(seller, tradeId);
    }
}
