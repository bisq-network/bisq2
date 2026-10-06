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

import static bisq.e2e.TradeChats.HEART;
import static bisq.e2e.TradeChats.LAUGH;
import static bisq.e2e.TradeChats.THUMBS_UP;
import static bisq.e2e.TradeChats.awaitActiveReactions;
import static bisq.e2e.TradeChats.awaitMessage;
import static bisq.e2e.TradeChats.citation;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The chat of a trade between a node of the released version and a node of this checkout: text messages with line
 * breaks and an emoji, replies which cite a message of the peer, and reactions, one of which is removed again. Each
 * message must arrive exactly once with the same text and citation, and each node must see the reactions of the peer.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class TradeChatAcrossVersionsTest {
    private MixedVersionNetwork network;

    @BeforeAll
    void startNetwork() throws Exception {
        network = new MixedVersionNetwork("trade-chat-across-versions");
    }

    @AfterAll
    void stopNetwork() throws Exception {
        if (network != null) {
            network.close();
        }
    }

    @Test
    void messagesRepliesAndReactionsReachTheOtherVersion() throws Exception {
        ApiClient newMaker = network.newApi;
        ApiClient oldTaker = network.oldApi;
        String offerId = Trades.createOffer(newMaker, "SELL");
        OffersAcrossVersionsTest.awaitOffers(oldTaker, List.of(offerId), "the old taker sees the offer");
        String tradeId = oldTaker.takeOffer(offerId, Trades.baseAmount(network.priceServer), Trades.QUOTE_AMOUNT,
                "MAIN_CHAIN", "ZELLE");
        Trades.awaitTradeState(newMaker, tradeId, "the new maker has the trade", state -> true);
        String channelId = TradeChats.channelId(oldTaker, tradeId);
        assertEquals(channelId, TradeChats.channelId(newMaker, tradeId), "Both nodes must use the same trade chat channel");

        String textOfOld = "A message of " + network.oldVersion + "\nwith a second line and an emoji \uD83D\uDE42";
        String textOfNew = "A message of the new version";
        oldTaker.sendTradeChatMessage(channelId, textOfOld, null);
        newMaker.sendTradeChatMessage(channelId, textOfNew, null);
        JsonNode messageOfOld = awaitMessage(newMaker, channelId, textOfOld, "the new node gets the message of the old node");
        JsonNode messageOfNew = awaitMessage(oldTaker, channelId, textOfNew, "the old node gets the message of the new node");

        // The receiver of an own message is the peer, so each node knows the profile id of its peer from its own message
        String profileIdOfOld = awaitMessage(newMaker, channelId, textOfNew, "the new node has its own message")
                .path("receiverUserProfileId").asText();
        String profileIdOfNew = awaitMessage(oldTaker, channelId, textOfOld, "the old node has its own message")
                .path("receiverUserProfileId").asText();

        String replyOfNew = "A reply of the new version";
        String replyOfOld = "A reply of " + network.oldVersion;
        newMaker.sendTradeChatMessage(channelId, replyOfNew, citation(profileIdOfOld, textOfOld, messageOfOld));
        oldTaker.sendTradeChatMessage(channelId, replyOfOld, citation(profileIdOfNew, textOfNew, messageOfNew));
        for (ApiClient api : List.of(newMaker, oldTaker)) {
            JsonNode reply = awaitMessage(api, channelId, replyOfNew, "the reply of the new node is on both nodes");
            assertEquals(messageOfOld.path("messageId").asText(), reply.path("citation").path("chatMessageId").asText());
            assertEquals(textOfOld, reply.path("citation").path("text").asText());
            reply = awaitMessage(api, channelId, replyOfOld, "the reply of the old node is on both nodes");
            assertEquals(messageOfNew.path("messageId").asText(), reply.path("citation").path("chatMessageId").asText());
            assertEquals(textOfNew, reply.path("citation").path("text").asText());
        }

        newMaker.sendTradeChatReaction(channelId, messageOfOld.path("messageId").asText(), THUMBS_UP, false, null);
        newMaker.sendTradeChatReaction(channelId, messageOfOld.path("messageId").asText(), HEART, false, null);
        newMaker.sendTradeChatReaction(channelId, messageOfOld.path("messageId").asText(), HEART, true, profileIdOfNew);
        oldTaker.sendTradeChatReaction(channelId, messageOfNew.path("messageId").asText(), LAUGH, false, null);
        for (ApiClient api : List.of(newMaker, oldTaker)) {
            awaitActiveReactions(api, channelId, textOfOld, Set.of(THUMBS_UP),
                    "the reactions of the new node to the message of the old node, without the removed one");
            awaitActiveReactions(api, channelId, textOfNew, Set.of(LAUGH),
                    "the reaction of the old node to the message of the new node");
        }

        for (ApiClient api : List.of(newMaker, oldTaker)) {
            for (String text : List.of(textOfOld, textOfNew, replyOfNew, replyOfOld)) {
                assertEquals(1, TradeChats.count(api, channelId, text), "The message '" + text + "' must exist exactly once");
            }
        }
    }
}
