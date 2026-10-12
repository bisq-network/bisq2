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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fixed prices which a client of the API of the released version creates, and the upgrade in place of its node to this
 * checkout (upgrade-fixed-price-from-api and api-fixprice-from-mobile of the release test plan). The API of the
 * released version builds the price quote of a fixed price from the fields of the request without a check.
 * <ul>
 *     <li>A canonical fixed price, as the desktop creates it.</li>
 *     <li>The fixed price which Bisq Connect sends: the base side id is BTC instead of BTC [crypto], and the value is
 *     rounded to 8 significant digits (docs/specifications/offer/fixed-price.md).</li>
 * </ul>
 * Nodes of this checkout show only offers with a canonical fixed price (rule 5 of
 * docs/specifications/offer/fixed-price.md), so they drop the offers with the price of Bisq Connect. After the upgrade,
 * the node keeps its open trades with both prices, and all trades complete. It skips only its own offer with the price
 * of Bisq Connect, and no trade data.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class FixedPriceAcrossUpgradeTest {
    // 64 321.12 USD, a price with decimals as a user enters it
    private static final long FIXED_PRICE = 643_211_200;
    // 64 321.1234 USD as Bisq Connect sends it (PriceQuoteVOFactory.fromPrice of bisq-mobile 7f9cddce7b): the quote
    // side amount is the entered price, and the value is that amount with 8 significant digits
    private static final long MOBILE_QUOTE_SIDE_AMOUNT = 643_211_234;
    private static final long MOBILE_VALUE = 643_211_230;
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
        long canonicalBaseAmount = Trades.baseAmountFor(Trades.QUOTE_AMOUNT, FIXED_PRICE);
        // Both versions convert the amounts with the value of the quote
        long mobileBaseAmount = Trades.baseAmountFor(Trades.QUOTE_AMOUNT, MOBILE_VALUE);

        // Nodes of this version drop the broadcast of an offer whose fixed price is not canonical
        for (String nodeName : List.of(network.newNode.getName(), network.newSeed.getName())) {
            network.expectLogFinding(nodeName, "network-message-unparsable",
                    "We drop a message which we cannot parse. Type=DATAREQUEST");
        }

        // The offers with the price of Bisq Connect come first. When a node sees the later offers, it had the chance to
        // see the earlier ones.
        ObjectNode mobilePrice = mobileFixedPrice();
        String mobileToTake = createOffer(upgradedNode, mobilePrice);
        String mobileToKeep = createOffer(upgradedNode, mobilePrice);
        ObjectNode canonicalPrice = OffersAcrossVersionsTest.fixedPrice(upgradedNode, FIXED_PRICE);
        String canonicalToTake = createOffer(upgradedNode, canonicalPrice);
        String canonicalToKeep = createOffer(upgradedNode, canonicalPrice);
        OffersAcrossVersionsTest.awaitOffers(oldPeer,
                List.of(mobileToTake, mobileToKeep, canonicalToTake, canonicalToKeep),
                "the old peer sees all offers");
        OffersAcrossVersionsTest.awaitOffers(newPeer, List.of(canonicalToTake, canonicalToKeep),
                "the new peer sees the offers with the canonical price");
        for (String offerId : List.of(mobileToTake, mobileToKeep)) {
            assertTrue(OffersAcrossVersionsTest.findOffer(newPeer, offerId).isEmpty(),
                    "A node of this version must not show an offer whose fixed price is not canonical");
        }

        // An open trade with each price. Both stop after the take.
        String mobileTrade = oldPeer.takeOffer(mobileToTake, mobileBaseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN",
                "ZELLE");
        Trades.awaitTradeState(upgradedNode, mobileTrade, "the old node has the trade with the old peer",
                state -> true);
        String canonicalTrade = newPeer.takeOffer(canonicalToTake, canonicalBaseAmount, Trades.QUOTE_AMOUNT,
                "MAIN_CHAIN", "ZELLE");
        Trades.awaitTradeState(upgradedNode, canonicalTrade, "the old node has the trade with the new peer",
                state -> true);

        // The user quits v2.1.13 and starts the new version on the same data directory
        network.stopOldNode();
        network.startOldNodeWithNewVersion();
        upgradedNode.acceptTermsAndConditions();
        // The upgraded node skips its own offer with the price of Bisq Connect, and that offer in the inventory of its
        // old peers
        network.expectLogFinding(network.oldNode.getName(), "store-entry-skipped",
                "We skip an entry which we cannot parse");
        network.expectLogFinding(network.oldNode.getName(), "error-line",
                "Could not create data request from protobuf data request. messageCase=ADDAUTHENTICATEDDATAREQUEST");

        // Both trades stay and complete
        assertNotNull(upgradedNode.tradeProperties().get(mobileTrade),
                "The upgraded node must keep the open trade with the fixed price of Bisq Connect");
        assertNotNull(upgradedNode.tradeProperties().get(canonicalTrade),
                "The upgraded node must keep the open trade with the canonical fixed price");
        Trades.completeAfterTake(upgradedNode, oldPeer, "SELL", mobileTrade, mobileBaseAmount, Trades.QUOTE_AMOUNT,
                Trades.BTC_ADDRESS);
        Trades.completeAfterTake(upgradedNode, newPeer, "SELL", canonicalTrade, canonicalBaseAmount,
                Trades.QUOTE_AMOUNT, Trades.BTC_ADDRESS);
        OffersAcrossVersionsTest.awaitOffers(upgradedNode, List.of(canonicalToKeep),
                "the upgraded node keeps its offer with the canonical fixed price");
        String tradeAfterUpgrade = newPeer.takeOffer(canonicalToKeep, canonicalBaseAmount, Trades.QUOTE_AMOUNT,
                "MAIN_CHAIN", "ZELLE");
        Trades.completeAfterTake(upgradedNode, newPeer, "SELL", tradeAfterUpgrade, canonicalBaseAmount,
                Trades.QUOTE_AMOUNT, Trades.BTC_ADDRESS);

        // The upgraded node no longer offers the price of Bisq Connect, and it skipped nothing else: no trade, trade
        // channel or mediation case is skipped or kept unparsed
        assertTrue(OffersAcrossVersionsTest.findOffer(upgradedNode, mobileToKeep).isEmpty(),
                "The upgraded node does not show its offer whose fixed price is not canonical");
        List<String> entries = skippedEntriesOfLastSession(network.oldNode.getName());
        assertFalse(entries.isEmpty(), "The upgraded node must skip its offer with the price of Bisq Connect");
        for (String entry : entries) {
            assertFalse(entry.contains("We keep an entry"), "A store kept an entry which it cannot parse: " + entry);
            assertTrue(entry.contains("verifyCanonical"), "cause of a skipped entry: " + entry);
        }
    }

    // The price spec of a fixed price as Bisq Connect posts it to the API (bisq-mobile 7f9cddce7b)
    private static ObjectNode mobileFixedPrice() {
        ObjectNode market = ApiClient.JSON.createObjectNode()
                .put("baseCurrencyCode", "BTC")
                .put("quoteCurrencyCode", "USD")
                .put("baseCurrencyName", "BTC")
                .put("quoteCurrencyName", "USD");
        ObjectNode baseSideMonetary = ApiClient.JSON.createObjectNode()
                .put("type", "Coin")
                .put("id", "BTC")
                .put("value", 100_000_000L)
                .put("code", "BTC")
                .put("precision", 8)
                .put("lowPrecision", 4);
        ObjectNode quoteSideMonetary = ApiClient.JSON.createObjectNode()
                .put("type", "Fiat")
                .put("id", "USD")
                .put("value", MOBILE_QUOTE_SIDE_AMOUNT)
                .put("code", "USD")
                .put("precision", 4)
                .put("lowPrecision", 2);
        ObjectNode priceQuote = ApiClient.JSON.createObjectNode()
                .put("value", MOBILE_VALUE)
                .put("precision", 4)
                .put("lowPrecision", 2);
        priceQuote.set("market", market);
        priceQuote.set("baseSideMonetary", baseSideMonetary);
        priceQuote.set("quoteSideMonetary", quoteSideMonetary);
        ObjectNode priceSpec = ApiClient.JSON.createObjectNode().put("type", "FixPriceSpec");
        priceSpec.set("priceQuote", priceQuote);
        return priceSpec;
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
