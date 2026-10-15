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

/**
 * A Bisq Easy trade runs to the end between a node of the released version and a node of this checkout, in all four
 * combinations of maker version and direction. Both sides must agree on the contract and reach the final state. The
 * trades run without a mediator, which dev mode allows.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class TradeAcrossVersionsTest {
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
        Trades.runTrade(maker, taker, makerDirection, network.priceServer);
    }
}
