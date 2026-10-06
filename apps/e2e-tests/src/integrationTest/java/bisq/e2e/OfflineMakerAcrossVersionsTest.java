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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;

/**
 * A taker takes the offer of a maker which is offline, in both directions between the released version and this
 * checkout. The taker then sends the take offer request to the mailbox of the maker. The maker gets it from its mailbox
 * when it starts again, and the trade continues to the end. This is the case "keep the maker offline during the take,
 * then start it" of the trade matrix of the release test plan.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class OfflineMakerAcrossVersionsTest {
    private MixedVersionNetwork network;

    @BeforeAll
    void startNetwork() throws Exception {
        network = new MixedVersionNetwork("offline-maker-across-versions");
    }

    @AfterAll
    void stopNetwork() throws Exception {
        if (network != null) {
            network.close();
        }
    }

    @Test
    void oldTakerTakesTheOfferOfAnOfflineNewMaker() throws Exception {
        String offerId = Trades.createOffer(network.newApi, "SELL");
        OffersAcrossVersionsTest.awaitOffers(network.oldApi, List.of(offerId), "the old taker sees the offer");
        network.stopNewNode();

        long baseAmount = Trades.baseAmount(network.priceServer);
        String tradeId = network.oldApi.takeOffer(offerId, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN", "ZELLE");
        Trades.awaitTradeState(network.oldApi, tradeId, "the old taker has the trade", state -> true);

        network.startNewNode();
        Trades.completeAfterTake(network.newApi, network.oldApi, "SELL", tradeId, baseAmount, Trades.QUOTE_AMOUNT,
                Trades.BTC_ADDRESS);
    }

    @Test
    void newTakerTakesTheOfferOfAnOfflineOldMaker() throws Exception {
        String offerId = Trades.createOffer(network.oldApi, "BUY");
        OffersAcrossVersionsTest.awaitOffers(network.newApi, List.of(offerId), "the new taker sees the offer");
        network.stopOldNode();

        long baseAmount = Trades.baseAmount(network.priceServer);
        String tradeId = network.newApi.takeOffer(offerId, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN", "ZELLE");
        Trades.awaitTradeState(network.newApi, tradeId, "the new taker has the trade", state -> true);

        network.startOldNode();
        Trades.completeAfterTake(network.oldApi, network.newApi, "BUY", tradeId, baseAmount, Trades.QUOTE_AMOUNT,
                Trades.BTC_ADDRESS);
    }
}
