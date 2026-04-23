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
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMessagePruningTest {
    @Test
    void maybePrune_keepsAllMessages_whenUnderBothLimits() {
        TestChatMessage first = new TestChatMessage("1", "hello", 10);
        TestChatMessage second = new TestChatMessage("2", "world", 20);

        List<TestChatMessage> result = ChatMessagePruning.maybePrune(
                List.of(first, second),
                100,
                100,
                messages -> messages.size() * 10,
                LoggerFactory.getLogger(ChatMessagePruningTest.class),
                "trade-1");

        assertThat(result).containsExactly(first, second);
    }

    @Test
    void maybePrune_usesUtf8ByteLengthForTheHeuristic() {
        TestChatMessage first = new TestChatMessage("1", "🙂", 10);
        TestChatMessage second = new TestChatMessage("2", "ok", 20);

        List<TestChatMessage> result = ChatMessagePruning.maybePrune(
                List.of(first, second),
                5,
                100,
                messages -> 0,
                LoggerFactory.getLogger(ChatMessagePruningTest.class),
                "trade-2");

        assertThat(result).containsExactly(first);
    }

    @Test
    void maybePrune_dropsMessage_whenUtf8ByteLengthHitsTheLimitExactly() {
        TestChatMessage first = new TestChatMessage("1", "🙂", 10);
        TestChatMessage second = new TestChatMessage("2", "ok", 20);

        List<TestChatMessage> result = ChatMessagePruning.maybePrune(
                List.of(first, second),
                4,
                100,
                messages -> 0,
                LoggerFactory.getLogger(ChatMessagePruningTest.class),
                "trade-equal-boundary");

        assertThat(result).isEmpty();
    }

    @Test
    void maybePrune_trimsFromTheEnd_whenSerializedSizeStillExceedsLimit() {
        TestChatMessage first = new TestChatMessage("1", "aa", 10);
        TestChatMessage second = new TestChatMessage("2", "bb", 20);
        TestChatMessage third = new TestChatMessage("3", "cc", 30);

        List<TestChatMessage> result = ChatMessagePruning.maybePrune(
                List.of(first, second, third),
                100,
                25,
                messages -> messages.size() * 10,
                LoggerFactory.getLogger(ChatMessagePruningTest.class),
                "trade-3");

        assertThat(result).containsExactly(first, second);
    }

    @Test
    void maybePrune_returnsEmpty_whenFirstMessageAlreadyReachesTextLimit() {
        TestChatMessage first = new TestChatMessage("1", "🙂🙂🙂", 10);
        TestChatMessage second = new TestChatMessage("2", "ok", 20);

        List<TestChatMessage> result = ChatMessagePruning.maybePrune(
                List.of(first, second),
                4,
                100,
                messages -> 0,
                LoggerFactory.getLogger(ChatMessagePruningTest.class),
                "trade-4");

        assertThat(result).isEmpty();
    }

    @Test
    void maybePrune_usesRealRequestSerializedSizeSupplierWithContextFields() {
        TestChatMessage first = new TestChatMessage("1", "aa", 10);
        TestChatMessage second = new TestChatMessage("2", "bb", 20);
        int maxSerializedSize = getMediationRequestSerializedSize(List.of(first));

        List<TestChatMessage> result = ChatMessagePruning.maybePrune(
                List.of(first, second),
                100,
                maxSerializedSize,
                ChatMessagePruningTest::getMediationRequestSerializedSize,
                LoggerFactory.getLogger(ChatMessagePruningTest.class),
                "trade-real-request-size");

        assertThat(getMediationRequestSerializedSize(List.of(first, second))).isGreaterThan(maxSerializedSize);
        assertThat(result).containsExactly(first);
        assertThat(getMediationRequestSerializedSize(result)).isLessThanOrEqualTo(maxSerializedSize);
    }

    private static int getMediationRequestSerializedSize(List<TestChatMessage> chatMessages) {
        return bisq.support.protobuf.MediationRequest.newBuilder()
                .setTradeId("trade-real-request-size")
                .setContract(bisq.contract.protobuf.Contract.newBuilder()
                        .setTakeOfferDate(1)
                        .build())
                .setRequester(createUserProfile("requester"))
                .setPeer(createUserProfile("peer"))
                .setMediatorNetworkId(createNetworkId("mediator"))
                .addAllChatMessages(chatMessages.stream()
                        .map(message -> message.<bisq.chat.protobuf.ChatMessage>unsafeToProto(false))
                        .toList())
                .build()
                .getSerializedSize();
    }

    private static bisq.user.protobuf.UserProfile createUserProfile(String id) {
        return bisq.user.protobuf.UserProfile.newBuilder()
                .setNickName(id + "-" + createString(1_000))
                .setNetworkId(createNetworkId(id))
                .setTerms(createString(1_000))
                .setStatement(createString(1_000))
                .setApplicationVersion("1.0.0")
                .build();
    }

    private static bisq.network.identity.protobuf.NetworkId createNetworkId(String id) {
        return bisq.network.identity.protobuf.NetworkId.newBuilder()
                .setPubKey(bisq.security.protobuf.PubKey.newBuilder()
                        .setKeyId(id + "-" + createString(1_000))
                        .setPublicKey(ByteString.copyFrom(new byte[1_000]))
                        .build())
                .build();
    }

    private static String createString(int length) {
        char[] chars = new char[length];
        Arrays.fill(chars, 'x');
        return new String(chars);
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
