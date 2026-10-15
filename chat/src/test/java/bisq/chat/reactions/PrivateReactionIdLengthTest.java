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
import bisq.chat.ChatMessageType;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.util.StringUtils;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.confidential.ack.AckMessage;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.pow.ProofOfWork;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The receiver acknowledges a private reaction with an AckMessage which carries the ID of the reaction. An AckMessage
 * accepts at most 100 characters, so a longer ID made the ACK service throw. Before StartupReplay processed each
 * replayed message on its own, this also stopped the network start.
 */
class PrivateReactionIdLengthTest {
    private static final String TOO_LONG_ID = "x".repeat(51);

    private final UserProfile sender = createUserProfile("sender", 1001);
    private final UserProfile receiver = createUserProfile("receiver", 1002);
    private final String channelId = TwoPartyPrivateChatChannel.createId(ChatChannelDomain.DISCUSSION,
            sender.getId(),
            receiver.getId());

    @Test
    void privateReactionWithAnIdOverTheLimitCannotBeCreated() {
        assertThrows(IllegalArgumentException.class, () -> twoPartyReaction(TOO_LONG_ID, "message-id"));
        assertThrows(IllegalArgumentException.class, () -> openTradeReaction(TOO_LONG_ID, "message-id"));
    }

    @Test
    void privateReactionWithAChatMessageIdOverTheLimitCannotBeCreated() {
        assertThrows(IllegalArgumentException.class, () -> twoPartyReaction("reaction-id", TOO_LONG_ID));
        assertThrows(IllegalArgumentException.class, () -> openTradeReaction("reaction-id", TOO_LONG_ID));
    }

    @Test
    void receivedPrivateReactionWithAnIdOverTheLimitFailsAtParsing() {
        bisq.chat.protobuf.ChatMessageReaction proto = twoPartyReaction("reaction-id", "message-id").toValueProto(false);

        assertThrows(UnresolvableProtobufException.class,
                () -> ChatMessageReaction.fromProto(proto.toBuilder().setId(TOO_LONG_ID).build()));
        assertThrows(UnresolvableProtobufException.class,
                () -> ChatMessageReaction.fromProto(proto.toBuilder().setChatMessageId(TOO_LONG_ID).build()));
    }

    // Unmodified clients create the ID of a reaction as a UUID and react to chat messages, whose IDs have at most 50
    // characters
    @Test
    void reactionOfAnUnmodifiedClientCanBeAcknowledged() {
        PrivateChatMessageReaction reaction = twoPartyReaction(StringUtils.createUid(), "x".repeat(50));

        assertDoesNotThrow(() -> new AckMessage(reaction.getAckRequestingMessageId()));
    }

    // Private reactions are stored inside their chat message. A stored reaction which breaks the rule is skipped, and
    // the message and its other reactions stay.
    @Test
    void storedMessageWithAReactionWhoseIdIsOverTheLimitKeepsItsOtherReactions() throws UnresolvableProtobufException {
        TwoPartyPrivateChatMessageReaction validReaction = twoPartyReaction("reaction-id", "message-id");
        TwoPartyPrivateChatMessage message = new TwoPartyPrivateChatMessage("message-id",
                ChatChannelDomain.DISCUSSION,
                channelId,
                sender,
                receiver.getId(),
                receiver.getNetworkId(),
                "text",
                Optional.empty(),
                System.currentTimeMillis(),
                false,
                ChatMessageType.TEXT,
                new HashSet<>(Set.of(validReaction)));
        bisq.chat.protobuf.ChatMessageReaction invalidReactionProto = twoPartyReaction("other-reaction-id", "message-id")
                .toValueProto(false)
                .toBuilder()
                .setId(TOO_LONG_ID)
                .build();
        bisq.chat.protobuf.ChatMessage proto = message.toValueProto(false);
        bisq.chat.protobuf.ChatMessage storedProto = proto.toBuilder()
                .setTwoPartyPrivateChatMessage(proto.getTwoPartyPrivateChatMessage().toBuilder()
                        .addChatMessageReactions(invalidReactionProto))
                .build();

        TwoPartyPrivateChatMessage storedMessage = TwoPartyPrivateChatMessage.fromProto(storedProto);

        assertEquals("message-id", storedMessage.getId());
        assertEquals(1, storedMessage.getChatMessageReactions().size());
        assertTrue(storedMessage.getChatMessageReactions().contains(validReaction));
    }

    private TwoPartyPrivateChatMessageReaction twoPartyReaction(String id, String chatMessageId) {
        return new TwoPartyPrivateChatMessageReaction(id,
                sender,
                receiver.getId(),
                receiver.getNetworkId(),
                channelId,
                ChatChannelDomain.DISCUSSION,
                chatMessageId,
                Reaction.THUMBS_UP.ordinal(),
                System.currentTimeMillis(),
                false);
    }

    private BisqEasyOpenTradeMessageReaction openTradeReaction(String id, String chatMessageId) {
        return new BisqEasyOpenTradeMessageReaction(id,
                sender,
                receiver.getId(),
                receiver.getNetworkId(),
                "bisq_easy_open_trades.trade-id",
                ChatChannelDomain.BISQ_EASY_OPEN_TRADES,
                chatMessageId,
                Reaction.THUMBS_UP.ordinal(),
                System.currentTimeMillis(),
                false);
    }

    private static UserProfile createUserProfile(String nickName, int port) {
        PubKey pubKey = new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "key-" + port);
        Address address = Address.from("127.0.0.1", port);
        NetworkId networkId = new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                pubKey);
        ProofOfWork proofOfWork = new ProofOfWork(pubKey.getHash(), 0, null, 1.0, new byte[72], 0);
        return new UserProfile(1, nickName, proofOfWork, 0, networkId, "", "", "1.0.0");
    }
}
