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

import java.util.List;
import java.util.Set;

import static bisq.e2e.ApiClient.JSON;
import static bisq.e2e.TradeChats.LAUGH;
import static bisq.e2e.TradeChats.THUMBS_UP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A node of the released version is upgraded in place while it has an open trade and an open offer: it stops, its peer
 * sends a trade message to its mailbox, and the version of this checkout starts on the same data directory. The trade
 * continues to the end, and the offer stays. The chat of the trade keeps its messages, the citation and the reactions,
 * and it continues in both directions. At the end, the scan of the logs covers the session of each version of the
 * upgraded node, so an entry which the new version skips at load fails the test.
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

        // Both traders chat with v2.1.13 on the side of the old node: a message of each side, a reply with a citation,
        // and a reaction of each side. The receiver of an own message is the peer, so each node knows the profile ID
        // of its peer from its own message.
        String channelId = TradeChats.channelId(upgradedNode, tradeId);
        String textOfOld = "Before the upgrade, from " + network.oldVersion + "\nwith a second line";
        String textOfPeer = "Before the upgrade, from the peer";
        upgradedNode.sendTradeChatMessage(channelId, textOfOld, null);
        peer.sendTradeChatMessage(channelId, textOfPeer, null);
        JsonNode messageOfOld = TradeChats.awaitMessage(peer, channelId, textOfOld, "the peer gets the message of the old node");
        JsonNode messageOfPeer = TradeChats.awaitMessage(upgradedNode, channelId, textOfPeer,
                "the old node gets the message of the peer");
        String profileIdOfOld = TradeChats.awaitMessage(peer, channelId, textOfPeer, "the peer has its own message")
                .path("receiverUserProfileId").asText();
        String profileIdOfPeer = TradeChats.awaitMessage(upgradedNode, channelId, textOfOld, "the old node has its own message")
                .path("receiverUserProfileId").asText();
        String replyOfPeer = "A reply of the peer before the upgrade";
        peer.sendTradeChatMessage(channelId, replyOfPeer, TradeChats.citation(profileIdOfOld, textOfOld, messageOfOld));
        peer.sendTradeChatReaction(channelId, messageOfOld.path("messageId").asText(), THUMBS_UP, false, null);
        upgradedNode.sendTradeChatReaction(channelId, messageOfPeer.path("messageId").asText(), LAUGH, false, null);
        TradeChats.awaitMessage(upgradedNode, channelId, replyOfPeer, "the old node gets the reply of the peer");
        TradeChats.awaitActiveReactions(upgradedNode, channelId, textOfOld, Set.of(THUMBS_UP),
                "the old node gets the reaction of the peer");
        TradeChats.awaitActiveReactions(upgradedNode, channelId, textOfPeer, Set.of(LAUGH), "the old node has its reaction");

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

        // The upgraded node keeps the chat which it stored with v2.1.13, and the chat continues in both directions
        for (String text : List.of(textOfOld, textOfPeer, replyOfPeer)) {
            TradeChats.awaitMessage(upgradedNode, channelId, text, "the upgraded node keeps the message '" + text + "'");
            assertEquals(1, TradeChats.count(upgradedNode, channelId, text),
                    "The upgraded node must have the message '" + text + "' exactly once");
        }
        JsonNode reply = TradeChats.awaitMessage(upgradedNode, channelId, replyOfPeer, "the upgraded node keeps the reply");
        assertEquals(messageOfOld.path("messageId").asText(), reply.path("citation").path("chatMessageId").asText(),
                "The upgraded node must keep the citation of the reply");
        TradeChats.awaitActiveReactions(upgradedNode, channelId, textOfOld, Set.of(THUMBS_UP),
                "the upgraded node keeps the reaction of the peer");
        TradeChats.awaitActiveReactions(upgradedNode, channelId, textOfPeer, Set.of(LAUGH),
                "the upgraded node keeps its own reaction");
        String replyOfUpgraded = "A reply of the upgraded node";
        upgradedNode.sendTradeChatMessage(channelId, replyOfUpgraded,
                TradeChats.citation(profileIdOfPeer, textOfPeer, messageOfPeer));
        TradeChats.awaitMessage(peer, channelId, replyOfUpgraded, "the peer gets the reply of the upgraded node");
        String textAfterOfPeer = "After the upgrade, from the peer";
        peer.sendTradeChatMessage(channelId, textAfterOfPeer, null);
        TradeChats.awaitMessage(upgradedNode, channelId, textAfterOfPeer, "the upgraded node gets the new message of the peer");

        Trades.completeAfterFiatSent(peer, upgradedNode, tradeId, baseAmount, Trades.QUOTE_AMOUNT);

        assertTrue(OffersAcrossVersionsTest.findOffer(upgradedNode, keptOffer).isPresent(),
                "The upgraded node lost its own offer");
        assertTrue(OffersAcrossVersionsTest.findOffer(peer, keptOffer).isPresent(),
                "The peer lost the offer of the upgraded node");
    }
}
