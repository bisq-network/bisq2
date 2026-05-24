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

package bisq.chat.reactions;

import bisq.chat.ChatChannelDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactionCompatibilityTest {
    @Test
    @DisplayName("from ordinal returns empty for unsupported future reaction")
    void from_ordinal_returns_empty_for_unsupported_future_reaction() {
        assertTrue(Reaction.fromOrdinal(Reaction.PARTY.ordinal()).isPresent());
        assertFalse(Reaction.fromOrdinal(Reaction.values().length).isPresent());
    }

    @Test
    @DisplayName("common public chat message reaction allows future reaction ids")
    void common_public_chat_message_reaction_allows_future_reaction_ids() {
        bisq.chat.protobuf.ChatMessageReaction proto = bisq.chat.protobuf.ChatMessageReaction.newBuilder()
                .setId("reaction-id")
                .setUserProfileId("0123456789abcdef0123456789abcdef01234567")
                .setChatChannelId("channel-id")
                .setChatChannelDomain(ChatChannelDomain.DISCUSSION.toProtoEnum())
                .setChatMessageId("message-id")
                .setReactionId(Reaction.values().length)
                .setDate(System.currentTimeMillis())
                .setCommonPublicChatMessageReaction(
                        bisq.chat.protobuf.CommonPublicChatMessageReaction.newBuilder().build())
                .build();

        ChatMessageReaction reaction = assertDoesNotThrow(() -> ChatMessageReaction.fromProto(proto));

        assertEquals(Reaction.values().length, reaction.getReactionId());
    }

    @Test
    @DisplayName("common public chat message reaction still rejects negative reaction ids")
    void common_public_chat_message_reaction_still_rejects_negative_reaction_ids() {
        assertThrows(IllegalArgumentException.class, () -> new CommonPublicChatMessageReaction(
                "reaction-id",
                "0123456789abcdef0123456789abcdef01234567",
                "channel-id",
                ChatChannelDomain.DISCUSSION,
                "message-id",
                -1,
                System.currentTimeMillis()));
    }
}
