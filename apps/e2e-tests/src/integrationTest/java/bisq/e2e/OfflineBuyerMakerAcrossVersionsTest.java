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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A buyer maker of this checkout which is offline while sellers of both versions take its offers, and which starts
 * after the market price moved (trade-buyer-maker-restart of the release test plan). The network runs without the
 * score of dev mode, and the sellers have a score of 30000 from a proof of burn, which covers 150 USD.
 * <p>
 * The maker checks a take at its start with its own current market price (take-offer-request-validation.md). The age
 * of the request does not matter, so the test moves the price instead of the clocks: the USD price falls by 8 %, more
 * than the default maximum trade price deviation of 5 %. The Bitcoin amount of each take is then too low for the
 * maker, which rejects both takes with PRICE_DEVIATION. Each taker sees the failure, the offers stay, and a retake at
 * the new price completes.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class OfflineBuyerMakerAcrossVersionsTest {
    private static final String ORACLE = "oracle";
    // 150 USD of the trade need 30000
    private static final long SCORE = 30_000;
    private static final double PRICE_AFTER_FALL = 65_000.0 * 0.92;
    // The reason of the maker for the failure PRICE_DEVIATION
    private static final String AMOUNT_TOO_LOW = "Takers (sellers) Bitcoin amount is too low";
    // The nodes request the market prices every 180 seconds
    private static final Duration PRICE_REFRESH = Duration.ofMinutes(4);

    private MixedVersionNetwork network;

    @BeforeAll
    void startNetwork() throws Exception {
        network = MixedVersionNetwork.withRealReputation("offline-buyer-maker-across-versions");
    }

    @AfterAll
    void stopNetwork() throws Exception {
        if (network != null) {
            network.close();
        }
    }

    @Test
    void offlineBuyerMakerRejectsTakesAtAnOldPriceAndAcceptsRetakes() throws Exception {
        ApiClient maker = network.newApi;
        ApiClient sellerOld = network.oldApi;
        ApiClient sellerNew = network.startExtraApiNode("seller-new", true,
                List.of(network.oldSeed.getP2pPort(), network.newSeed.getP2pPort()));

        // The sellers get a score which covers the trade amount
        ApiClient oracle = network.startAuthorizedDataPublisher(ORACLE);
        network.runPublisherCommand(ORACLE, "oracle");
        for (ApiClient seller : List.of(sellerOld, sellerNew)) {
            String sellerId = seller.userProfileId();
            MixedVersionNetwork.await("the oracle knows the profile of the seller", Trades.STEP_TIMEOUT,
                    () -> oracle.knowsUserProfile(sellerId));
            network.runPublisherCommand(ORACLE, "burn " + sellerId + " " + SCORE);
            MixedVersionNetwork.await("the maker knows the score of the seller", Trades.STEP_TIMEOUT,
                    () -> maker.get("/reputation/score/" + sellerId).path("totalScore").asLong() == SCORE);
        }

        // An offer for each seller. The maker goes offline, and the sellers take the offers.
        Map<ApiClient, String> offerBySeller = Map.of(sellerOld, Trades.createOffer(maker, "BUY"),
                sellerNew, Trades.createOffer(maker, "BUY"));
        for (Map.Entry<ApiClient, String> entry : offerBySeller.entrySet()) {
            OffersAcrossVersionsTest.awaitOffers(entry.getKey(), List.of(entry.getValue()),
                    "the seller sees the offer");
        }
        network.stopNewNode();
        long baseAmountAtOldPrice = Trades.baseAmount(network.priceServer);
        Map<ApiClient, String> tradeBySeller = Map.of(
                sellerOld, sellerOld.takeOffer(offerBySeller.get(sellerOld), baseAmountAtOldPrice, Trades.QUOTE_AMOUNT,
                        "MAIN_CHAIN", "ZELLE"),
                sellerNew, sellerNew.takeOffer(offerBySeller.get(sellerNew), baseAmountAtOldPrice, Trades.QUOTE_AMOUNT,
                        "MAIN_CHAIN", "ZELLE"));

        // The market falls while the maker is offline. The maker starts and rejects both takes.
        network.priceServer.setPrice("USD", PRICE_AFTER_FALL);
        network.expectLogFinding(network.newNode.getName(), "take-offer-rejected", AMOUNT_TOO_LOW);
        network.expectLogFinding(network.newNode.getName(), "error-line", AMOUNT_TOO_LOW);
        // The API logs the properties of the failed trade, which name the failure
        for (String takerName : List.of(network.oldNode.getName(), "seller-new")) {
            network.expectLogFinding(takerName, "take-offer-price-deviation", "PRICE_DEVIATION");
        }
        network.startNewNode();
        for (Map.Entry<ApiClient, String> entry : tradeBySeller.entrySet()) {
            JsonNode failedTrade = awaitFailedTrade(entry.getKey(), entry.getValue());
            assertTrue(failedTrade.path("peersErrorMessage").asText().contains(AMOUNT_TOO_LOW),
                    "The taker must get the reason of the maker: " + failedTrade);
            assertTrue(Trades.findTrade(maker, entry.getValue()).isEmpty(),
                    "The maker must not have the rejected trade");
        }

        // The offers stay. Each seller takes again at the new price, which it gets from the price node.
        long baseAmountAtNewPrice = Trades.baseAmount(network.priceServer);
        long newPriceValue = Math.round(PRICE_AFTER_FALL * 1e4);
        for (Map.Entry<ApiClient, String> entry : offerBySeller.entrySet()) {
            ApiClient seller = entry.getKey();
            String offerId = entry.getValue();
            assertTrue(OffersAcrossVersionsTest.findOffer(seller, offerId).isPresent(), "The offer must stay");
            MixedVersionNetwork.await("the seller gets the new price", PRICE_REFRESH,
                    () -> OffersAcrossVersionsTest.marketQuote(seller, "USD").path("value").asLong() == newPriceValue);
            String tradeId = seller.takeOffer(offerId, baseAmountAtNewPrice, Trades.QUOTE_AMOUNT, "MAIN_CHAIN",
                    "ZELLE");
            Trades.completeAfterTake(maker, seller, "BUY", tradeId, baseAmountAtNewPrice, Trades.QUOTE_AMOUNT,
                    Trades.BTC_ADDRESS);
        }
    }

    // The trade of the taker fails at the peer, which is the maker
    private static JsonNode awaitFailedTrade(ApiClient taker, String tradeId) throws InterruptedException {
        JsonNode[] found = new JsonNode[1];
        MixedVersionNetwork.await("the taker sees that the maker rejected the take", Trades.STEP_TIMEOUT, () -> {
            JsonNode properties = taker.tradeProperties().get(tradeId);
            if (properties != null && properties.path("tradeState").asText().equals("FAILED_AT_PEER")) {
                found[0] = properties;
                return true;
            }
            return false;
        });
        return found[0];
    }
}
