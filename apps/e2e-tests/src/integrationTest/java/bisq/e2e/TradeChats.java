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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static bisq.e2e.ApiClient.JSON;

/** Steps and checks of the chat of a trade through the API, shared by the test classes. */
final class TradeChats {
    static final int THUMBS_UP = 0;
    static final int LAUGH = 3;
    static final int HEART = 4;

    private TradeChats() {
    }

    // The ID of the chat channel of the trade on the node
    static String channelId(ApiClient api, String tradeId) {
        return Trades.findTrade(api, tradeId).orElseThrow().path("channel").path("id").asText();
    }

    static ObjectNode citation(String authorUserProfileId, String text, JsonNode citedMessage) {
        ObjectNode citation = JSON.createObjectNode();
        citation.put("authorUserProfileId", authorUserProfileId);
        citation.put("text", text);
        citation.put("chatMessageId", citedMessage.path("messageId").asText());
        return citation;
    }

    static List<JsonNode> messages(ApiClient api, String channelId) {
        return api.tradeChatMessages().stream()
                .filter(message -> message.path("channelId").asText().equals(channelId))
                .toList();
    }

    static String textOf(JsonNode message) {
        JsonNode text = message.path("text");
        return text.isTextual() ? text.asText() : null;
    }

    static JsonNode awaitMessage(ApiClient api, String channelId, String text, String description)
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

    static void awaitActiveReactions(ApiClient api, String channelId, String text, Set<Integer> expected,
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

    // How often a message with the text exists in the channel on the node
    static long count(ApiClient api, String channelId, String text) {
        return messages(api, channelId).stream().filter(message -> text.equals(textOf(message))).count();
    }
}
