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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fixed prices which a client of the API of the released version creates, and the upgrade in place of its node to this
 * checkout (upgrade-fixed-price-from-api; open question api-fixprice-from-mobile of the release test plan). The API of
 * the released version builds the price quote of a fixed price from the fields of the request without a check. This
 * checkout parses a price quote only if its amounts are canonical and its value matches them (FixPriceSpec.verify()).
 * <ul>
 *     <li>A canonical fixed price, as the desktop creates it: the open offer and the open trade with this price stay
 *     after the upgrade, and both trades complete.</li>
 *     <li>A fixed price whose value does not match its amounts: the released version publishes the offer, and a peer
 *     of the released version takes it. Nodes of this checkout do not show the offer. After the upgrade, the node
 *     skips the offer and the trade with this price when it loads its stores, as a store skips every entry which it
 *     cannot parse (docs/specifications/network/validation-at-parsing.md, rule 4). It also loses the chat of the
 *     trade. The peer keeps its trade, and it still shows the offer, which its maker can no longer remove. Whether a
 *     real client sends such a price is the open question.</li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class FixedPriceAcrossUpgradeTest {
    // 64 321.12 USD, a price with decimals as a user enters it
    private static final long FIXED_PRICE = 643_211_200;
    // The log shows the exception of a skipped entry in the next lines, with the failed check within the first frames
    private static final int CAUSE_LINES = 8;

    private MixedVersionNetwork network;

    @BeforeAll
    void startNetwork() throws Exception {
        network = new MixedVersionNetwork("fixed-price-across-upgrade");
    }

    @AfterAll
    void stopNetwork() throws Exception {
        if (network != null) {
            network.close();
        }
    }

    @Test
    void fixedPricesOfTheApiOfTheOldVersionAcrossTheUpgrade() throws Exception {
        ApiClient upgradedNode = network.oldApi;
        ApiClient newPeer = network.newApi;
        ApiClient oldPeer = network.startExtraApiNode("old-peer", false,
                List.of(network.oldSeed.getP2pPort(), network.newSeed.getP2pPort()));
        long baseAmount = Trades.baseAmountFor(Trades.QUOTE_AMOUNT, FIXED_PRICE);

        // Nodes of this version drop the broadcast of an offer with a price which is not canonical
        for (String nodeName : List.of(network.newNode.getName(), network.newSeed.getName())) {
            network.expectLogFinding(nodeName, "network-message-unparsable",
                    "We drop a message which we cannot parse. Type=DATAREQUEST");
        }

        // The offers with the price which is not canonical come first. When a node sees the later offers, it had the
        // chance to see the earlier ones.
        ObjectNode notCanonicalPrice = OffersAcrossVersionsTest.fixedPrice(upgradedNode, FIXED_PRICE);
        ((ObjectNode) notCanonicalPrice.path("priceQuote")).put("value", FIXED_PRICE + 1);
        String notCanonicalToTake = createOffer(upgradedNode, notCanonicalPrice);
        String notCanonicalToKeep = createOffer(upgradedNode, notCanonicalPrice);
        ObjectNode canonicalPrice = OffersAcrossVersionsTest.fixedPrice(upgradedNode, FIXED_PRICE);
        String canonicalToTake = createOffer(upgradedNode, canonicalPrice);
        String canonicalToKeep = createOffer(upgradedNode, canonicalPrice);
        OffersAcrossVersionsTest.awaitOffers(oldPeer,
                List.of(notCanonicalToTake, notCanonicalToKeep, canonicalToTake, canonicalToKeep),
                "the old peer sees all offers");
        OffersAcrossVersionsTest.awaitOffers(newPeer, List.of(canonicalToTake, canonicalToKeep),
                "the new peer sees the offers with the canonical price");
        for (String offerId : List.of(notCanonicalToTake, notCanonicalToKeep)) {
            assertTrue(OffersAcrossVersionsTest.findOffer(newPeer, offerId).isEmpty(),
                    "A node of this version must not show a fixed price which is not canonical");
        }

        // An open trade with each price. Both stop after the take.
        String notCanonicalTrade = oldPeer.takeOffer(notCanonicalToTake, baseAmount, Trades.QUOTE_AMOUNT,
                "MAIN_CHAIN", "ZELLE");
        Trades.awaitTradeState(upgradedNode, notCanonicalTrade, "the old node has the trade with the old peer",
                state -> true);
        String canonicalTrade = newPeer.takeOffer(canonicalToTake, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN",
                "ZELLE");
        Trades.awaitTradeState(upgradedNode, canonicalTrade, "the old node has the trade with the new peer",
                state -> true);

        // The user quits v2.1.13 and starts the new version on the same data directory
        network.stopOldNode();
        network.startOldNodeWithNewVersion();
        upgradedNode.acceptTermsAndConditions();
        // The upgraded node skips the stored data with this price, and the offer in the inventory of its old peers
        network.expectLogFinding(network.oldNode.getName(), "store-entry-skipped",
                "We skip an entry which we cannot parse");
        network.expectLogFinding(network.oldNode.getName(), "error-line",
                "Could not create data request from protobuf data request. messageCase=ADDAUTHENTICATEDDATAREQUEST");

        // The canonical price stays: the open trade continues, and the open offer can be taken
        assertNotNull(upgradedNode.tradeProperties().get(canonicalTrade),
                "The upgraded node must keep the open trade with the canonical fixed price");
        OffersAcrossVersionsTest.awaitOffers(upgradedNode, List.of(canonicalToKeep),
                "the upgraded node keeps its offer with the canonical fixed price");
        Trades.completeAfterTake(upgradedNode, newPeer, "SELL", canonicalTrade, baseAmount, Trades.QUOTE_AMOUNT,
                Trades.BTC_ADDRESS);
        String tradeAfterUpgrade = newPeer.takeOffer(canonicalToKeep, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN",
                "ZELLE");
        Trades.completeAfterTake(upgradedNode, newPeer, "SELL", tradeAfterUpgrade, baseAmount, Trades.QUOTE_AMOUNT,
                Trades.BTC_ADDRESS);

        // The price which is not canonical: the upgraded node lost the offer and the trade, the peer keeps the trade
        assertNull(upgradedNode.tradeProperties().get(notCanonicalTrade),
                "The upgraded node skips the trade with the fixed price which is not canonical");
        assertTrue(OffersAcrossVersionsTest.findOffer(upgradedNode, notCanonicalToKeep).isEmpty(),
                "The upgraded node skips the offer with the fixed price which is not canonical");
        assertTrue(Trades.findTrade(oldPeer, notCanonicalTrade).isPresent(),
                "The old peer keeps the trade which the upgraded node lost");
        // The upgraded node skipped only the data with this price: the offer, the trade and the chat of the trade
        List<String> skippedEntries = skippedEntriesOfLastSession(network.oldNode.getName());
        assertEquals(3, skippedEntries.size(), "skipped entries: " + skippedEntries);
        for (String skippedEntry : skippedEntries) {
            assertTrue(skippedEntry.contains("FixPriceSpec.verify"), "cause of a skipped entry: " + skippedEntry);
        }
        // The maker cannot remove the offer which it lost, so the old peer still shows it
        assertTrue(OffersAcrossVersionsTest.findOffer(oldPeer, notCanonicalToKeep).isPresent(),
                "The old peer still shows the offer which the upgraded node lost");
    }

    // Each entry which a store of the node skipped since its last start, with the lines of its cause
    private List<String> skippedEntriesOfLastSession(String nodeName) throws IOException {
        List<String> log = network.logOfLastSession(nodeName);
        List<String> entries = new ArrayList<>();
        for (int i = 0; i < log.size(); i++) {
            if (log.get(i).contains("We skip an entry which we cannot parse")) {
                entries.add(String.join("\n", log.subList(i, Math.min(i + CAUSE_LINES, log.size()))));
            }
        }
        return entries;
    }

    private static String createOffer(ApiClient maker, ObjectNode priceSpec) {
        ObjectNode amountSpec = ApiClient.JSON.createObjectNode()
                .put("type", "QuoteSideFixedAmountSpec")
                .put("amount", Trades.QUOTE_AMOUNT);
        return maker.createOffer(OffersAcrossVersionsTest.offerRequest(maker.market("USD"), "SELL", amountSpec,
                priceSpec, List.of("MAIN_CHAIN"), List.of("ZELLE")));
    }
}
