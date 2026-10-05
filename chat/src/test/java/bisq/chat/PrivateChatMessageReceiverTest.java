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

package bisq.chat;

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.chat.reactions.TwoPartyPrivateChatMessageReaction;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

// The key of the receiver's network ID decrypts a private message or reaction, so its receiver profile is the profile
// of that key (see docs/specifications/chat/private-channel-membership.md).
class PrivateChatMessageReceiverTest {
    private static final UserProfile SENDER = ChatTestUsers.createUserIdentity("sender").getUserProfile();
    private static final UserProfile RECEIVER = ChatTestUsers.createUserIdentity("receiver").getUserProfile();
    private static final UserProfile OTHER_PROFILE = ChatTestUsers.createUserIdentity("other").getUserProfile();
    private static final String TRADE_ID = "tradeId";

    @Test
    void twoPartyMessageToAnotherProfileIsInvalid() {
        bisq.chat.protobuf.ChatMessage proto = twoPartyMessage(RECEIVER.getId()).toValueProto(false);
        bisq.chat.protobuf.ChatMessage protoToOtherProfile = proto.toBuilder()
                .setTwoPartyPrivateChatMessage(proto.getTwoPartyPrivateChatMessage().toBuilder()
                        .setReceiverUserProfileId(OTHER_PROFILE.getId()))
                .build();

        assertDoesNotThrow(() -> TwoPartyPrivateChatMessage.fromProto(proto));
        assertThrows(UnresolvableProtobufException.class,
                () -> TwoPartyPrivateChatMessage.fromProto(protoToOtherProfile));
        assertThrows(IllegalArgumentException.class, () -> twoPartyMessage(OTHER_PROFILE.getId()));
    }

    @Test
    void tradeMessageToAnotherProfileIsInvalid() {
        bisq.chat.protobuf.ChatMessage proto = tradeMessage(RECEIVER.getId()).toValueProto(false);
        bisq.chat.protobuf.ChatMessage protoToOtherProfile = proto.toBuilder()
                .setBisqEasyOpenTradeMessage(proto.getBisqEasyOpenTradeMessage().toBuilder()
                        .setReceiverUserProfileId(OTHER_PROFILE.getId()))
                .build();

        assertDoesNotThrow(() -> BisqEasyOpenTradeMessage.fromProto(proto));
        assertThrows(UnresolvableProtobufException.class,
                () -> BisqEasyOpenTradeMessage.fromProto(protoToOtherProfile));
        assertThrows(IllegalArgumentException.class, () -> tradeMessage(OTHER_PROFILE.getId()));
    }

    @Test
    void reactionToAnotherProfileIsInvalid() {
        bisq.chat.protobuf.ChatMessageReaction proto = reaction(RECEIVER.getId()).toValueProto(false);
        bisq.chat.protobuf.ChatMessageReaction protoToOtherProfile = proto.toBuilder()
                .setTwoPartyPrivateChatMessageReaction(proto.getTwoPartyPrivateChatMessageReaction().toBuilder()
                        .setReceiverUserProfileId(OTHER_PROFILE.getId()))
                .build();

        assertDoesNotThrow(() -> TwoPartyPrivateChatMessageReaction.fromProto(proto));
        assertThrows(UnresolvableProtobufException.class,
                () -> TwoPartyPrivateChatMessageReaction.fromProto(protoToOtherProfile));
        assertThrows(IllegalArgumentException.class, () -> reaction(OTHER_PROFILE.getId()));
    }

    private static TwoPartyPrivateChatMessage twoPartyMessage(String receiverUserProfileId) {
        return new TwoPartyPrivateChatMessage("messageId",
                ChatChannelDomain.DISCUSSION,
                TwoPartyPrivateChatChannel.createId(ChatChannelDomain.DISCUSSION, SENDER.getId(), RECEIVER.getId()),
                SENDER,
                receiverUserProfileId,
                RECEIVER.getNetworkId(),
                "hi",
                Optional.empty(),
                System.currentTimeMillis(),
                false,
                ChatMessageType.TEXT,
                new HashSet<>());
    }

    private static BisqEasyOpenTradeMessage tradeMessage(String receiverUserProfileId) {
        return new BisqEasyOpenTradeMessage(TRADE_ID,
                "messageId",
                BisqEasyOpenTradeChannel.createId(TRADE_ID),
                SENDER,
                receiverUserProfileId,
                RECEIVER.getNetworkId(),
                "hi",
                Optional.empty(),
                System.currentTimeMillis(),
                false,
                Optional.empty(),
                ChatMessageType.TEXT,
                Optional.empty(),
                new HashSet<>());
    }

    private static TwoPartyPrivateChatMessageReaction reaction(String receiverUserProfileId) {
        return new TwoPartyPrivateChatMessageReaction("reactionId",
                SENDER,
                receiverUserProfileId,
                RECEIVER.getNetworkId(),
                TwoPartyPrivateChatChannel.createId(ChatChannelDomain.DISCUSSION, SENDER.getId(), RECEIVER.getId()),
                ChatChannelDomain.DISCUSSION,
                "messageId",
                0,
                System.currentTimeMillis(),
                false);
    }
}
