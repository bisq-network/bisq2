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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static bisq.e2e.ApiClient.JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The chat of a trade between a node of the released version and a node of this checkout: text messages with line
 * breaks and an emoji, replies which cite a message of the peer, and reactions, one of which is removed again. Each
 * message must arrive exactly once with the same text and citation, and each node must see the reactions of the peer.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class TradeChatAcrossVersionsTest {
    private static final int THUMBS_UP = 0;
    private static final int LAUGH = 3;
    private static final int HEART = 4;

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
        String channelId = Trades.findTrade(oldTaker, tradeId).orElseThrow().path("channel").path("id").asText();
        assertEquals(channelId, Trades.findTrade(newMaker, tradeId).orElseThrow().path("channel").path("id").asText(),
                "Both nodes must use the same trade chat channel");

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
                long count = messages(api, channelId).stream().filter(message -> text.equals(textOf(message))).count();
                assertEquals(1, count, "The message '" + text + "' must exist exactly once");
            }
        }
    }

    private static ObjectNode citation(String authorUserProfileId, String text, JsonNode citedMessage) {
        ObjectNode citation = JSON.createObjectNode();
        citation.put("authorUserProfileId", authorUserProfileId);
        citation.put("text", text);
        citation.put("chatMessageId", citedMessage.path("messageId").asText());
        return citation;
    }

    private static List<JsonNode> messages(ApiClient api, String channelId) {
        return api.tradeChatMessages().stream()
                .filter(message -> message.path("channelId").asText().equals(channelId))
                .toList();
    }

    private static String textOf(JsonNode message) {
        JsonNode text = message.path("text");
        return text.isTextual() ? text.asText() : null;
    }

    private static JsonNode awaitMessage(ApiClient api, String channelId, String text, String description)
            throws InterruptedException {
        JsonNode[] found = new JsonNode[1];
        MixedVersionNetwork.await(description, Trades.STEP_TIMEOUT, () -> {
            Optional<JsonNode> message = messages(api, channelId).stream()
                    .filter(candidate -> text.equals(textOf(candidate)))
                    .findFirst();
            message.ifPresent(value -> found[0] = value);
            return message.isPresent();
        });
        return found[0];
    }

    private static void awaitActiveReactions(ApiClient api, String channelId, String text, Set<Integer> expected,
                                             String description) throws InterruptedException {
        MixedVersionNetwork.await(description, Trades.STEP_TIMEOUT, () -> {
            Optional<JsonNode> message = messages(api, channelId).stream()
                    .filter(candidate -> text.equals(textOf(candidate)))
                    .findFirst();
            if (message.isEmpty()) {
                return false;
            }
            Set<Integer> active = StreamSupport.stream(message.get().path("chatMessageReactions").spliterator(), false)
                    .filter(reaction -> !reaction.path("isRemoved").asBoolean(false))
                    .map(reaction -> reaction.path("reactionId").asInt())
                    .collect(Collectors.toSet());
            return active.equals(expected);
        });
    }
}
