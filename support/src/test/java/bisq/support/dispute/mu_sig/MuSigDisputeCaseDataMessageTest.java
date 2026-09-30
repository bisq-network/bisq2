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

package bisq.support.dispute.mu_sig;

import bisq.chat.ChatMessageType;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeMessage;
import bisq.network.identity.NetworkId;
import bisq.support.dispute.SerializedSizeExceededException;
import bisq.user.profile.UserProfile;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static bisq.support.dispute.ChatMessagePruning.MAX_SERIALIZED_SIZE;
import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.TRADE_ID;
import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.assertCanBeEncrypted;
import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.createUserProfile;
import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.findBoundaryTextLength;
import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.findLargestAcceptedTextLength;
import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.wrappedEnvelopeSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MuSigDisputeCaseDataMessageTest {
    private static final String CHANNEL_ID = "channel-boundary";
    private static final byte[] CONTRACT_HASH = new byte[20];

    @Test
    void rejectsPayloadWhenFullEnvelopeExceedsLimit() {
        UserProfile sender = createUserProfile("sender");
        UserProfile peer = createUserProfile("peer");
        int textLength = findBoundaryTextLength(textLengthCandidate ->
                createDisputeCaseDataMessageProto(sender.getNetworkId(),
                        createBoundaryChatMessages(sender, peer, 20, textLengthCandidate)));
        List<MuSigOpenTradeMessage> chatMessages = createBoundaryChatMessages(sender, peer, 20, textLength);
        bisq.support.protobuf.MuSigDisputeCaseDataMessage innerProto =
                createDisputeCaseDataMessageProto(sender.getNetworkId(), chatMessages);

        assertTrue(innerProto.getSerializedSize() <= MAX_SERIALIZED_SIZE);
        assertTrue(wrappedEnvelopeSize(innerProto) > MAX_SERIALIZED_SIZE);
        SerializedSizeExceededException exception = assertThrows(SerializedSizeExceededException.class,
                () -> new MuSigDisputeCaseDataMessage(TRADE_ID,
                        sender.getNetworkId(),
                        CONTRACT_HASH,
                        chatMessages));
        assertEquals("Serialized dispute case data size must not exceed " + MAX_SERIALIZED_SIZE + " bytes",
                exception.getMessage());
    }

    @Test
    void acceptedPayloadCanBeEncrypted() throws GeneralSecurityException {
        UserProfile sender = createUserProfile("sender");
        UserProfile peer = createUserProfile("peer");
        int textLength = findLargestAcceptedTextLength(textLengthCandidate ->
                createDisputeCaseDataMessageProto(sender.getNetworkId(),
                        createBoundaryChatMessages(sender, peer, 20, textLengthCandidate)));
        List<MuSigOpenTradeMessage> chatMessages = createBoundaryChatMessages(sender, peer, 20, textLength);
        MuSigDisputeCaseDataMessage message = new MuSigDisputeCaseDataMessage(TRADE_ID,
                sender.getNetworkId(),
                CONTRACT_HASH,
                chatMessages);

        assertCanBeEncrypted(message.serialize(), sender.getNetworkId());
    }

    private List<MuSigOpenTradeMessage> createBoundaryChatMessages(UserProfile sender,
                                                                   UserProfile peer,
                                                                   int messageCount,
                                                                   int textLength) {
        long now = System.currentTimeMillis();
        return IntStream.range(0, messageCount)
                .mapToObj(index -> {
                    boolean senderIsRequester = index % 2 == 0;
                    UserProfile messageSender = senderIsRequester ? sender : peer;
                    UserProfile receiver = senderIsRequester ? peer : sender;
                    return createChatMessage("message-" + index,
                            messageSender,
                            receiver,
                            "x".repeat(textLength),
                            now + index);
                })
                .toList();
    }

    private MuSigOpenTradeMessage createChatMessage(String messageId,
                                                    UserProfile sender,
                                                    UserProfile receiver,
                                                    String text,
                                                    long date) {
        return new MuSigOpenTradeMessage(TRADE_ID,
                messageId,
                CHANNEL_ID,
                sender,
                receiver.getId(),
                receiver.getNetworkId(),
                text,
                Optional.empty(),
                date,
                false,
                Optional.empty(),
                Optional.empty(),
                ChatMessageType.TEXT,
                Optional.empty(),
                Set.of());
    }

    private bisq.support.protobuf.MuSigDisputeCaseDataMessage createDisputeCaseDataMessageProto(NetworkId senderNetworkId,
                                                                                                List<MuSigOpenTradeMessage> chatMessages) {
        return bisq.support.protobuf.MuSigDisputeCaseDataMessage.newBuilder()
                .setTradeId(TRADE_ID)
                .setSenderNetworkId(senderNetworkId.toProto(false))
                .setContractHash(ByteString.copyFrom(CONTRACT_HASH))
                .addAllChatMessages(chatMessages.stream()
                        .map(message -> message.toValueProto(false))
                        .toList())
                .build();
    }
}
