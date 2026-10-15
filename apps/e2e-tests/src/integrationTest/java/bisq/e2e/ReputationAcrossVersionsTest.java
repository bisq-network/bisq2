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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;

import static bisq.e2e.ApiClient.JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reputation of a seller of the released version, which takes the buy offer of a maker of this checkout
 * (trade-rejection-old-taker of the release test plan, MV-07 and MV-14). The network runs without the score of dev
 * mode, which replaces the score of every profile and hides the check of the seller's reputation. The authorized data
 * publisher acts as a root oracle node and publishes a proof of burn which gives the seller a score of 1300. The
 * expectations come from docs/specifications/trade/bisq-easy-seller-reputation-limit.md, section Older Versions:
 * <ul>
 *     <li>Both versions show the score 1300 for the seller.</li>
 *     <li>A take at 8 USD requires 1600, above the allowance of 1500, so the maker rejects it. The taker of v2.1.13
 *     does not know the failure reason SELLER_REPUTATION_TOO_LOW and reads it as UNKNOWN, but its API answers the take
 *     with HTTP 400 and the reason text of the maker. The maker has no trade, and the offer stays.</li>
 *     <li>v2.1.13 derives a maximum of 7 USD from the score. It requires 1400, within the allowance, so the maker
 *     accepts the retake at this amount, and the trade completes.</li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class ReputationAcrossVersionsTest {
    private static final String ORACLE = "oracle";
    // 13 BSQ, in BSQ satoshis. The score of a proof of burn from a block of now is its amount.
    private static final long SCORE = 1300;
    // 65000 USD per Bitcoin with the 4 decimals of fiat, the price of the fake price node
    private static final long USD_PRICE = 650_000_000;
    private static final long SEVEN_USD = 70_000;
    private static final long EIGHT_USD = 80_000;

    private MixedVersionNetwork network;

    @BeforeAll
    void startNetwork() throws Exception {
        network = MixedVersionNetwork.withRealReputation("reputation-across-versions");
    }

    @AfterAll
    void stopNetwork() throws Exception {
        if (network != null) {
            network.close();
        }
    }

    @Test
    void makerOfThisVersionChecksTheReputationOfASellerOfTheOldVersion() throws Exception {
        ApiClient seller = network.oldApi;
        ApiClient buyer = network.newApi;
        String sellerId = seller.userProfileId();

        // The oracle node publishes a proof of burn for the seller, and both versions compute the same score
        ApiClient oracle = network.startAuthorizedDataPublisher(ORACLE);
        network.runPublisherCommand(ORACLE, "oracle");
        MixedVersionNetwork.await("the oracle knows the profile of the seller", Trades.STEP_TIMEOUT,
                () -> oracle.knowsUserProfile(sellerId));
        network.runPublisherCommand(ORACLE, "burn " + sellerId + " " + SCORE);
        for (ApiClient api : List.of(seller, buyer)) {
            MixedVersionNetwork.await("the node knows the score of the seller", Trades.STEP_TIMEOUT,
                    () -> scoreOf(api, sellerId) == SCORE);
        }

        // A buy offer of 6 to 100 USD at the market price
        ObjectNode amountSpec = JSON.createObjectNode()
                .put("type", "QuoteSideRangeAmountSpec")
                .put("minAmount", 60_000)
                .put("maxAmount", 1_000_000);
        String offerId = buyer.createOffer(OffersAcrossVersionsTest.offerRequest(buyer.market("USD"), "BUY",
                amountSpec, JSON.createObjectNode().put("type", "MarketPriceSpec"), List.of("MAIN_CHAIN"),
                List.of("ZELLE")));
        OffersAcrossVersionsTest.awaitOffers(seller, List.of(offerId), "the seller sees the offer");

        // 8 USD require more than the score and its allowance. The maker rejects the take, and the offer stays.
        network.expectLogFinding(network.newNode.getName(), "take-offer-rejected",
                "The seller's reputation score of 1300 is too low");
        network.expectLogFinding(network.oldNode.getName(), "enum-unresolved",
                "Enum type= TradeProtocolFailure; name=UNRECOGNIZED; fallBack=UNKNOWN");
        ApiClient.ApiException rejection = assertThrows(ApiClient.ApiException.class,
                () -> seller.takeOffer(offerId, Trades.baseAmountFor(EIGHT_USD, USD_PRICE), EIGHT_USD, "MAIN_CHAIN",
                        "ZELLE"),
                "The maker must reject a take which the score of the seller does not cover");
        assertEquals(400, rejection.status, rejection.getMessage());
        assertTrue(rejection.getMessage().contains("The seller's reputation score of 1300 is too low") &&
                rejection.getMessage().contains("requires a reputation score of 1600"), rejection.getMessage());
        assertTrue(buyer.openTrades().isEmpty(), "The maker must not have the rejected trade");
        assertTrue(OffersAcrossVersionsTest.findOffer(seller, offerId).isPresent(), "The offer must stay");

        // 7 USD, the maximum of v2.1.13, are within the allowance. The retake works, and the trade completes.
        long baseAmount = Trades.baseAmountFor(SEVEN_USD, USD_PRICE);
        String tradeId = seller.takeOffer(offerId, baseAmount, SEVEN_USD, "MAIN_CHAIN", "ZELLE");
        Trades.completeAfterTake(buyer, seller, "BUY", tradeId, baseAmount, SEVEN_USD, Trades.BTC_ADDRESS);
    }

    private static long scoreOf(ApiClient api, String userProfileId) {
        return api.get("/reputation/score/" + userProfileId).path("totalScore").asLong();
    }
}
