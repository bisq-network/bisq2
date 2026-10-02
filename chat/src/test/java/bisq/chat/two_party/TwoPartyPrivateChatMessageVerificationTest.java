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

package bisq.chat.two_party;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.ChatMessageType;
import bisq.chat.reactions.TwoPartyPrivateChatMessageReaction;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.identity.NetworkId;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.pow.ProofOfWork;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// The integrity rules of a chat message apply when it is created and when it is parsed (see
// docs/specifications/network/validation-at-parsing.md)
class TwoPartyPrivateChatMessageVerificationTest {
    private final UserProfile sender = createUserProfile("sender", 1001);
    private final UserProfile receiver = createUserProfile("receiver", 1002);

    @Test
    void messageWithTooLongTextCannotBeCreated() {
        assertThrows(IllegalArgumentException.class, () -> message("x".repeat(ChatMessage.MAX_TEXT_LENGTH + 1)));
    }

    @Test
    void receivedMessageWithTooLongTextFailsAtParsing() {
        bisq.chat.protobuf.ChatMessage proto = message("text").toValueProto(false).toBuilder()
                .setText("x".repeat(ChatMessage.MAX_TEXT_LENGTH + 1))
                .build();

        assertThrows(UnresolvableProtobufException.class, () -> TwoPartyPrivateChatMessage.fromProto(proto));
    }

    // A reaction which this version cannot parse does not remove its message, for example from a persisted channel
    @Test
    void invalidReactionIsSkippedAndTheMessageIsKept() throws UnresolvableProtobufException {
        TwoPartyPrivateChatMessage message = message("text");
        bisq.chat.protobuf.ChatMessageReaction validReaction = new TwoPartyPrivateChatMessageReaction("reactionId",
                sender,
                receiver.getId(),
                receiver.getNetworkId(),
                message.getChannelId(),
                ChatChannelDomain.DISCUSSION,
                message.getId(),
                0,
                System.currentTimeMillis(),
                false).toValueProto(false);
        // A receiver profile ID of another length
        bisq.chat.protobuf.ChatMessageReaction invalidReaction = validReaction.toBuilder()
                .setTwoPartyPrivateChatMessageReaction(validReaction.getTwoPartyPrivateChatMessageReaction().toBuilder()
                        .setReceiverUserProfileId("tooShort"))
                .build();
        bisq.chat.protobuf.ChatMessage messageProto = message.toValueProto(false);
        bisq.chat.protobuf.ChatMessage proto = messageProto.toBuilder()
                .setTwoPartyPrivateChatMessage(messageProto.getTwoPartyPrivateChatMessage().toBuilder()
                        .addChatMessageReactions(validReaction)
                        .addChatMessageReactions(invalidReaction))
                .build();

        TwoPartyPrivateChatMessage parsed = TwoPartyPrivateChatMessage.fromProto(proto);

        assertEquals(message.getId(), parsed.getId());
        assertEquals(1, parsed.getChatMessageReactions().size());
    }

    private TwoPartyPrivateChatMessage message(String text) {
        return new TwoPartyPrivateChatMessage("messageId",
                ChatChannelDomain.DISCUSSION,
                TwoPartyPrivateChatChannel.createId(ChatChannelDomain.DISCUSSION, sender.getId(), receiver.getId()),
                sender,
                receiver.getId(),
                receiver.getNetworkId(),
                text,
                Optional.empty(),
                System.currentTimeMillis(),
                false,
                ChatMessageType.TEXT,
                Set.of());
    }

    private static UserProfile createUserProfile(String nickName, int port) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(keyPair.getPublic(), "key-" + port);
        Address address = Address.from("127.0.0.1", port);
        NetworkId networkId = new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                pubKey);
        ProofOfWork proofOfWork = new ProofOfWork(pubKey.getHash(), 0, null, 1.0, new byte[72], 0);
        return new UserProfile(1, nickName, proofOfWork, 0, networkId, "", "", "1.0.0");
    }
}
