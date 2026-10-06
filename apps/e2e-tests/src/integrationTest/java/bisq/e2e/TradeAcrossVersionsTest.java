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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static bisq.e2e.ApiClient.JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A Bisq Easy trade runs to the end between a node of the released version and a node of this checkout, in all four
 * combinations of maker version and direction. Both sides must agree on the contract and reach the final state. The
 * trades run without a mediator, which dev mode allows.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class TradeAcrossVersionsTest {
    private static final Duration STEP_TIMEOUT = Duration.ofMinutes(2);
    // 150 USD, the fiat precision is 4
    private static final long QUOTE_AMOUNT = 1_500_000;
    private static final String BTC_ADDRESS = "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq";
    private static final String TX_ID = "4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b";
    private static final Set<String> FAILED_STATES = Set.of("REJECTED", "PEER_REJECTED", "CANCELLED", "PEER_CANCELLED",
            "FAILED", "FAILED_AT_PEER");

    private MixedVersionNetwork network;

    @BeforeAll
    void startNetwork() throws Exception {
        network = new MixedVersionNetwork("trade-across-versions");
    }

    @AfterAll
    void stopNetwork() throws Exception {
        if (network != null) {
            network.close();
        }
    }

    @Test
    void newSellerMakerTradesWithOldBuyerTaker() throws Exception {
        trade(network.newApi, network.oldApi, "SELL");
    }

    @Test
    void oldSellerMakerTradesWithNewBuyerTaker() throws Exception {
        trade(network.oldApi, network.newApi, "SELL");
    }

    @Test
    void newBuyerMakerTradesWithOldSellerTaker() throws Exception {
        trade(network.newApi, network.oldApi, "BUY");
    }

    @Test
    void oldBuyerMakerTradesWithNewSellerTaker() throws Exception {
        trade(network.oldApi, network.newApi, "BUY");
    }

    private void trade(ApiClient maker, ApiClient taker, String makerDirection) throws Exception {
        JsonNode market = maker.market("USD");
        String offerId = maker.createOffer(OffersAcrossVersionsTest.offerRequest(market, makerDirection,
                JSON.createObjectNode().put("type", "QuoteSideFixedAmountSpec").put("amount", QUOTE_AMOUNT),
                JSON.createObjectNode().put("type", "MarketPriceSpec"),
                List.of("MAIN_CHAIN"), List.of("ZELLE")));
        OffersAcrossVersionsTest.awaitOffers(taker, List.of(offerId), "the taker sees the offer of the maker");

        // The taker derives the Bitcoin amount from the fiat amount and the market price, which all nodes get from the
        // fake price node. Fiat has the precision 4 and Bitcoin the precision 8.
        long baseAmount = Math.round(QUOTE_AMOUNT * 1e8 / (network.priceServer.getPrice("USD") * 1e4));
        String tradeId = taker.takeOffer(offerId, baseAmount, QUOTE_AMOUNT, "MAIN_CHAIN", "ZELLE");

        ApiClient seller = makerDirection.equals("SELL") ? maker : taker;
        ApiClient buyer = seller == maker ? taker : maker;
        awaitTradeState(maker, tradeId, "the maker has the trade", state -> true);
        awaitTradeState(taker, tradeId, "the taker has the trade", state -> true);

        seller.tradeEvent(tradeId, "SELLER_SENDS_PAYMENT_ACCOUNT", "e2e payment account of the seller");
        buyer.tradeEvent(tradeId, "BUYER_SEND_BITCOIN_PAYMENT_DATA", BTC_ADDRESS);
        awaitTradeState(buyer, tradeId, "the buyer receives the account data",
                state -> state.contains("BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA"));
        awaitTradeState(seller, tradeId, "the seller receives the Bitcoin address",
                state -> state.contains("SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS"));

        buyer.tradeEvent(tradeId, "BUYER_CONFIRM_FIAT_SENT", null);
        awaitTradeState(seller, tradeId, "the seller learns that the fiat was sent",
                state -> state.equals("SELLER_RECEIVED_FIAT_SENT_CONFIRMATION"));

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
        assertEquals(QUOTE_AMOUNT, contractOfBuyer.path("quoteSideAmount").asLong(), "fiat amount of the contract");

        buyer.tradeEvent(tradeId, "CLOSE_TRADE", null);
        seller.tradeEvent(tradeId, "CLOSE_TRADE", null);
        for (ApiClient api : List.of(buyer, seller)) {
            MixedVersionNetwork.await("the closed trade leaves the open trades", STEP_TIMEOUT,
                    () -> findTrade(api, tradeId).isEmpty());
        }
    }

    private static Optional<JsonNode> findTrade(ApiClient api, String tradeId) {
        return api.openTrades().stream()
                .filter(item -> item.path("trade").path("id").asText().equals(tradeId))
                .findAny();
    }

    // Waits until the current state of the trade on the node matches. Fails at once if the trade failed.
    private static void awaitTradeState(ApiClient api,
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
}
