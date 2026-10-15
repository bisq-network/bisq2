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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;

import static bisq.e2e.ApiClient.JSON;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The setting "close my offer when taken", and the rejection of a take, between the released version and this checkout
 * (docs/specifications/trade/take-offer-request-validation.md).
 * <ul>
 *     <li>With the setting, the maker removes its offer when a taker takes it. The offer disappears on the nodes of
 *     both versions, with a maker of each version.</li>
 *     <li>A maker of this checkout rejects a take of an offer which it no longer has, for example the second take of an
 *     offer which it closed. It rejects an ignored taker in the same way: with NO_MATCHING_OFFER_FOUND and a message
 *     which starts with "Could not find matching offer". The taker of the old version must get this rejection. The
 *     test uses the ignored taker, because a second take of a closed offer reaches the maker only in a race, which the
 *     test cannot control through the API.</li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class OfferClosingAcrossVersionsTest {
    private static final String NO_MATCHING_OFFER = "Could not find matching offer";

    private MixedVersionNetwork network;

    @BeforeAll
    void startNetwork() throws Exception {
        network = new MixedVersionNetwork("offer-closing-across-versions");
    }

    @AfterAll
    void stopNetwork() throws Exception {
        if (network != null) {
            network.close();
        }
    }

    @Test
    @Order(1)
    void newMakerClosesItsOfferWhenAnOldTakerTakesIt() throws Exception {
        network.newApi.changeSetting("closeMyOfferWhenTaken", JSON.getNodeFactory().booleanNode(true));
        String offerId = Trades.createOffer(network.newApi, "SELL");
        OffersAcrossVersionsTest.awaitOffers(network.oldApi, List.of(offerId), "the old taker sees the offer");

        long baseAmount = Trades.baseAmount(network.priceServer);
        String tradeId = network.oldApi.takeOffer(offerId, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN", "ZELLE");
        Trades.awaitTradeState(network.newApi, tradeId, "the new maker has the trade", state -> true);
        awaitOfferRemovedOnBothVersions(offerId);

        Trades.completeAfterTake(network.newApi, network.oldApi, "SELL", tradeId, baseAmount, Trades.QUOTE_AMOUNT,
                Trades.BTC_ADDRESS);
    }

    @Test
    @Order(2)
    void oldMakerClosesItsOfferWhenANewTakerTakesIt() throws Exception {
        network.oldApi.changeSetting("closeMyOfferWhenTaken", JSON.getNodeFactory().booleanNode(true));
        String offerId = Trades.createOffer(network.oldApi, "BUY");
        OffersAcrossVersionsTest.awaitOffers(network.newApi, List.of(offerId), "the new taker sees the offer");

        long baseAmount = Trades.baseAmount(network.priceServer);
        String tradeId = network.newApi.takeOffer(offerId, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN", "ZELLE");
        Trades.awaitTradeState(network.oldApi, tradeId, "the old maker has the trade", state -> true);
        awaitOfferRemovedOnBothVersions(offerId);

        Trades.completeAfterTake(network.oldApi, network.newApi, "BUY", tradeId, baseAmount, Trades.QUOTE_AMOUNT,
                Trades.BTC_ADDRESS);
    }

    @Test
    @Order(3)
    void newMakerRejectsTheTakeOfAnIgnoredOldTaker() throws Exception {
        String offerId = Trades.createOffer(network.newApi, "SELL");
        OffersAcrossVersionsTest.awaitOffers(network.oldApi, List.of(offerId), "the old taker sees the offer");
        String oldTakerProfileId = network.oldApi.userProfileId();
        network.newApi.ignoreUserProfile(oldTakerProfileId);
        // The maker logs the rejection and its reason, the ignored taker
        network.expectLogFinding(network.newNode.getName(), "error-line", NO_MATCHING_OFFER);
        network.expectLogFinding(network.newNode.getName(), "take-offer-rejected", "We reject the take offer request");
        try {
            long baseAmount = Trades.baseAmount(network.priceServer);
            ApiClient.ApiException rejection = assertThrows(ApiClient.ApiException.class,
                    () -> network.oldApi.takeOffer(offerId, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN", "ZELLE"),
                    "The new maker must reject the take of an ignored taker");
            assertTrue(rejection.getMessage().contains(NO_MATCHING_OFFER),
                    "The old taker must get the reason of the maker: " + rejection.getMessage());

            // A rejected take leaves no trade at the maker and does not close the offer
            assertTrue(network.newApi.openTrades().isEmpty(), "The new maker must not keep a trade of a rejected take");
            assertTrue(OffersAcrossVersionsTest.findOffer(network.oldApi, offerId).isPresent(),
                    "The rejected take must not remove the offer");
        } finally {
            network.newApi.undoIgnoreUserProfile(oldTakerProfileId);
        }
    }

    private void awaitOfferRemovedOnBothVersions(String offerId) throws InterruptedException {
        for (ApiClient api : List.of(network.oldApi, network.newApi)) {
            MixedVersionNetwork.await("the taken offer disappears on the node", Trades.STEP_TIMEOUT,
                    () -> OffersAcrossVersionsTest.findOffer(api, offerId).isEmpty());
        }
    }
}
