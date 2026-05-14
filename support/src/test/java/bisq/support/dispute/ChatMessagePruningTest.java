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

package bisq.support.dispute;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.ChatMessageType;
import bisq.chat.reactions.ChatMessageReaction;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.p2p.services.data.storage.MetaData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatMessagePruningTest {
    @Test
    @DisplayName("create with maybe pruned messages keeps newest messages when text bytes exceed limit")
    void create_with_maybe_pruned_messages_keeps_newest_messages_when_text_bytes_exceed_limit() {
        TestChatMessage first = new TestChatMessage("1", "x".repeat(17_999), 10);
        TestChatMessage second = new TestChatMessage("2", "middle", 20);
        TestChatMessage third = new TestChatMessage("3", "new", 30);

        List<TestChatMessage> result = ChatMessagePruning.createWithMaybePrunedMessages(
                List.of(first, second, third),
                "trade-text-prune",
                ArrayList::new);

        assertThat(result).containsExactly(second, third);
    }

    @Test
    @DisplayName("create with maybe pruned messages removes oldest message when serialized size still exceeds limit")
    void create_with_maybe_pruned_messages_removes_oldest_message_when_serialized_size_still_exceeds_limit() {
        TestChatMessage first = new TestChatMessage("1", "aa", 10);
        TestChatMessage second = new TestChatMessage("2", "bb", 20);
        TestChatMessage third = new TestChatMessage("3", "cc", 30);
        AtomicInteger attempts = new AtomicInteger();

        List<TestChatMessage> result = ChatMessagePruning.createWithMaybePrunedMessages(
                List.of(first, second, third),
                "trade-size-prune",
                messages -> {
                    attempts.incrementAndGet();
                    if (messages.size() > 2) {
                        throw new SerializedSizeExceededException("too large");
                    }
                    return new ArrayList<>(messages);
                });

        assertThat(result).containsExactly(second, third);
        assertThat(attempts).hasValue(2);
    }

    @Test
    @DisplayName("create with maybe pruned messages rethrows when request with no messages is still too large")
    void create_with_maybe_pruned_messages_rethrows_when_request_with_no_messages_is_still_too_large() {
        TestChatMessage message = new TestChatMessage("1", "aa", 10);

        assertThatThrownBy(() -> ChatMessagePruning.createWithMaybePrunedMessages(
                List.of(message),
                "trade-too-large",
                messages -> {
                    throw new SerializedSizeExceededException("too large");
                }))
                .isInstanceOf(SerializedSizeExceededException.class)
                .hasMessage("too large");
    }

    private static final class TestChatMessage extends ChatMessage {
        private static final MetaData META_DATA = new MetaData(1_000, 1, TestChatMessage.class.getSimpleName());

        private TestChatMessage(String id, String text, long date) {
            super(id,
                    ChatChannelDomain.SUPPORT,
                    "channel",
                    "author",
                    Optional.of(text),
                    Optional.empty(),
                    date,
                    false,
                    ChatMessageType.TEXT);
        }

        @Override
        public void verify() {
        }

        @Override
        public bisq.chat.protobuf.ChatMessage.Builder getBuilder(boolean serializeForHash) {
            return getChatMessageBuilder(serializeForHash)
                    .setCommonPublicChatMessage(bisq.chat.protobuf.CommonPublicChatMessage.newBuilder().build());
        }

        @Override
        protected MetaData getMetaData() {
            return META_DATA;
        }

        @Override
        public <R extends ChatMessageReaction> ObservableSet<R> getChatMessageReactions() {
            return new ObservableSet<>();
        }

        @Override
        public boolean addChatMessageReaction(ChatMessageReaction reaction) {
            return false;
        }
    }
}
