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
 * A node which joins the network later gets the data which exists already with an inventory request, also when it knows
 * only the seed node of the other version. A node of this checkout then requests the inventory with the filter of the
 * old version (HASH_SET), and a node of the old version gets an answer from a node of this checkout.
 * <p>
 * The late node also learns other peers through peer exchange, so the inventory can come from them too. The test shows
 * that a new node of each version gets the data of both versions. It does not prove which peer delivered it.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class InventoryAcrossVersionsTest {
    private MixedVersionNetwork network;

    @BeforeAll
    void startNetwork() throws Exception {
        network = new MixedVersionNetwork("inventory-across-versions");
    }

    @AfterAll
    void stopNetwork() throws Exception {
        if (network != null) {
            network.close();
        }
    }

    @Test
    void lateNodesGetTheExistingDataThroughTheSeedNodeOfTheOtherVersion() throws Exception {
        String offerOfOldNode = Trades.createOffer(network.oldApi, "SELL");
        String offerOfNewNode = Trades.createOffer(network.newApi, "BUY");
        OffersAcrossVersionsTest.awaitOffers(network.oldApi, List.of(offerOfOldNode, offerOfNewNode), "the old node has both offers");
        OffersAcrossVersionsTest.awaitOffers(network.newApi, List.of(offerOfOldNode, offerOfNewNode), "the new node has both offers");

        // Each late node knows only the seed node of the other version
        ApiClient lateNewNode = network.startExtraApiNode("late-new", true, List.of(network.oldSeed.getP2pPort()));
        ApiClient lateOldNode = network.startExtraApiNode("late-old", false, List.of(network.newSeed.getP2pPort()));
        OffersAcrossVersionsTest.awaitOffers(lateNewNode, List.of(offerOfOldNode, offerOfNewNode),
                "the late node of this checkout gets the existing offers through the seed node of the old version");
        OffersAcrossVersionsTest.awaitOffers(lateOldNode, List.of(offerOfOldNode, offerOfNewNode),
                "the late node of the old version gets the existing offers through the seed node of this checkout");

        // Data which is published after they joined reaches them too
        String laterOfferOfOldNode = Trades.createOffer(network.oldApi, "BUY");
        String laterOfferOfNewNode = Trades.createOffer(network.newApi, "SELL");
        OffersAcrossVersionsTest.awaitOffers(lateNewNode, List.of(laterOfferOfOldNode, laterOfferOfNewNode),
                "the late node of this checkout gets the new offers");
        OffersAcrossVersionsTest.awaitOffers(lateOldNode, List.of(laterOfferOfOldNode, laterOfferOfNewNode),
                "the late node of the old version gets the new offers");
    }
}
