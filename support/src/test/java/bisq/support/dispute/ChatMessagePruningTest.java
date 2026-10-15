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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatMessagePruningTest {
    @Test
    void createWithMaybePrunedMessages_keepsNewestMessages_whenTextBytesExceedLimit() {
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
    void createWithMaybePrunedMessages_removesOldestMessage_whenSerializedSizeStillExceedsLimit() {
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
    void createWithMaybePrunedMessages_rethrows_whenRequestWithNoMessagesIsStillTooLarge() {
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

    @Test
    void createWithMaybePrunedMessages_keepsNewestMessagesInChronologicalOrder_whenInputIsNotChronological() {
        // Each text has 4 000 bytes. Four texts have 16 000 bytes. A fifth text would reach the limit of 18 000 bytes.
        // The order of the IDs differs from the order of the dates.
        TestChatMessage date10 = new TestChatMessage("e", "a".repeat(4_000), 10);
        TestChatMessage date20 = new TestChatMessage("b", "b".repeat(4_000), 20);
        TestChatMessage date30 = new TestChatMessage("f", "c".repeat(4_000), 30);
        TestChatMessage date40 = new TestChatMessage("a", "d".repeat(4_000), 40);
        TestChatMessage date50 = new TestChatMessage("d", "e".repeat(4_000), 50);
        TestChatMessage date60 = new TestChatMessage("c", "f".repeat(4_000), 60);

        // The callers copy the messages of a chat channel from a hash set, so their order is not chronological.
        List<TestChatMessage> result = ChatMessagePruning.createWithMaybePrunedMessages(
                List.of(date30, date10, date60, date20, date50, date40),
                "trade-unordered-text-prune",
                ArrayList::new);

        assertThat(result).containsExactly(date30, date40, date50, date60);
    }

    @Test
    void createWithMaybePrunedMessages_removesOldestMessages_whenSerializedSizeExceedsLimitAndInputIsNotChronological() {
        // The order of the IDs differs from the order of the dates.
        TestChatMessage date10 = new TestChatMessage("c", "aa", 10);
        TestChatMessage date20 = new TestChatMessage("e", "bb", 20);
        TestChatMessage date30 = new TestChatMessage("a", "cc", 30);
        TestChatMessage date40 = new TestChatMessage("d", "dd", 40);
        TestChatMessage date50 = new TestChatMessage("b", "ee", 50);
        List<List<TestChatMessage>> attempts = new ArrayList<>();

        // The factory fails until fewer than 3 chat messages are left.
        List<TestChatMessage> result = ChatMessagePruning.createWithMaybePrunedMessages(
                List.of(date40, date10, date50, date30, date20),
                "trade-unordered-size-prune",
                messages -> {
                    attempts.add(List.copyOf(messages));
                    if (messages.size() >= 3) {
                        throw new SerializedSizeExceededException("too large");
                    }
                    return new ArrayList<>(messages);
                });

        assertThat(result).containsExactly(date40, date50);
        assertThat(attempts).containsExactly(
                List.of(date10, date20, date30, date40, date50),
                List.of(date20, date30, date40, date50),
                List.of(date30, date40, date50),
                List.of(date40, date50));
    }

    @Test
    void createWithMaybePrunedMessages_keepsAllMessagesInGivenOrder_whenChronologicalInputIsWithinLimits() {
        // The order of the IDs differs from the order of the dates.
        TestChatMessage date10 = new TestChatMessage("c", "aa", 10);
        TestChatMessage date20 = new TestChatMessage("a", "bb", 20);
        TestChatMessage date30 = new TestChatMessage("b", "cc", 30);

        List<TestChatMessage> result = ChatMessagePruning.createWithMaybePrunedMessages(
                List.of(date10, date20, date30),
                "trade-no-prune",
                ArrayList::new);

        assertThat(result).containsExactly(date10, date20, date30);
    }

    @Test
    void createWithMaybePrunedMessages_keepsSameMessages_forEveryOrderOfTheInput() {
        // Each text has 7 000 bytes. Two texts have 14 000 bytes. A third text would reach the limit of 18 000 bytes.
        String text = "x".repeat(7_000);
        TestChatMessage date10 = new TestChatMessage("e", text, 10);
        TestChatMessage date20 = new TestChatMessage("d", text, 20);
        // Two messages with the same date. The message with the higher ID counts as the newer one.
        TestChatMessage date30LowerId = new TestChatMessage("b", text, 30);
        TestChatMessage date30HigherId = new TestChatMessage("c", text, 30);
        TestChatMessage date40 = new TestChatMessage("a", text, 40);
        List<List<TestChatMessage>> inputs = permutations(List.of(date10, date20, date30LowerId, date30HigherId, date40));
        assertThat(inputs).hasSize(120);

        for (List<TestChatMessage> input : inputs) {
            List<TestChatMessage> result = ChatMessagePruning.createWithMaybePrunedMessages(
                    input,
                    "trade-any-order",
                    ArrayList::new);

            assertThat(result)
                    .as("input with the IDs %s", input.stream().map(ChatMessage::getId).toList())
                    .containsExactly(date30HigherId, date40);
        }
    }

    private static <T> List<List<T>> permutations(List<T> items) {
        if (items.isEmpty()) {
            return List.of(List.of());
        }
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            List<T> rest = new ArrayList<>(items);
            T first = rest.remove(i);
            for (List<T> permutationOfRest : permutations(rest)) {
                List<T> permutation = new ArrayList<>();
                permutation.add(first);
                permutation.addAll(permutationOfRest);
                result.add(permutation);
            }
        }
        return result;
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
        public boolean hasSupportedChatMessageType() {
            return true;
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
