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

import static bisq.e2e.ApiClient.JSON;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A node of the released version is upgraded in place while it has an open trade and an open offer: it stops, its peer
 * sends a trade message to its mailbox, and the version of this checkout starts on the same data directory. The trade
 * continues to the end, and the offer stays. At the end, the scan of the logs covers the session of each version of
 * the upgraded node, so an entry which the new version skips at load fails the test.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class UpgradeInPlaceTest {
    private MixedVersionNetwork network;

    @BeforeAll
    void startNetwork() throws Exception {
        network = new MixedVersionNetwork("upgrade-in-place");
    }

    @AfterAll
    void stopNetwork() throws Exception {
        if (network != null) {
            network.close();
        }
    }

    @Test
    void openTradeAndOfferOfAnOldNodeContinueAfterItsUpgrade() throws Exception {
        ApiClient upgradedNode = network.oldApi;
        ApiClient peer = network.newApi;

        // The old node is the seller maker of a trade, and has a buy offer which nobody takes
        String keptOffer = upgradedNode.createOffer(OffersAcrossVersionsTest.offerRequest(upgradedNode.market("USD"), "BUY",
                JSON.createObjectNode().put("type", "QuoteSideRangeAmountSpec").put("minAmount", 500_000).put("maxAmount", 2_000_000),
                JSON.createObjectNode().put("type", "FloatPriceSpec").put("percentage", -0.02),
                List.of("MAIN_CHAIN"), List.of("ZELLE")));
        String tradeOffer = Trades.createOffer(upgradedNode, "SELL");
        OffersAcrossVersionsTest.awaitOffers(peer, List.of(keptOffer, tradeOffer), "the peer sees the offers of the old node");

        long baseAmount = Trades.baseAmount(network.priceServer);
        String tradeId = peer.takeOffer(tradeOffer, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN", "ZELLE");
        Trades.awaitTradeState(upgradedNode, tradeId, "the old node has the trade", state -> true);
        upgradedNode.tradeEvent(tradeId, "SELLER_SENDS_PAYMENT_ACCOUNT", "e2e payment account of the seller");
        peer.tradeEvent(tradeId, "BUYER_SEND_BITCOIN_PAYMENT_DATA", Trades.BTC_ADDRESS);
        Trades.awaitTradeState(upgradedNode, tradeId, "the old node receives the Bitcoin address",
                state -> state.contains("SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS"));

        // The user quits v2.1.13. While the node is offline, the buyer confirms the fiat payment. The message goes to
        // the mailbox of the seller.
        network.stopOldNode();
        peer.tradeEvent(tradeId, "BUYER_CONFIRM_FIAT_SENT", null);
        Trades.awaitTradeState(peer, tradeId, "the buyer sent the fiat confirmation",
                state -> state.equals("BUYER_SENT_FIAT_SENT_CONFIRMATION"));

        // The user starts the new version on the same data directory
        network.startOldNodeWithNewVersion();
        upgradedNode.acceptTermsAndConditions();
        Trades.awaitTradeState(upgradedNode, tradeId, "the upgraded node gets the fiat confirmation from its mailbox",
                state -> state.equals("SELLER_RECEIVED_FIAT_SENT_CONFIRMATION"));
        Trades.completeAfterFiatSent(peer, upgradedNode, tradeId, baseAmount);

        assertTrue(OffersAcrossVersionsTest.findOffer(upgradedNode, keptOffer).isPresent(),
                "The upgraded node lost its own offer");
        assertTrue(OffersAcrossVersionsTest.findOffer(peer, keptOffer).isPresent(),
                "The peer lost the offer of the upgraded node");
    }
}
