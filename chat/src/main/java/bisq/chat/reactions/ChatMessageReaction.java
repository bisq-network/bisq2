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
import bisq.common.proto.NetworkProto;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.message.ExternalNetworkMessage;
import bisq.network.p2p.services.data.storage.DistributedData;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
@EqualsAndHashCode
public abstract class ChatMessageReaction implements NetworkProto {
    // The highest reaction ID of a public reaction (CommonPublicChatMessageReaction, BisqEasyOfferbookMessageReaction).
    // Nodes store, relay and serve a public reaction to all nodes. Version 2.1.13 rejects a reaction with a higher ID
    // at parsing, because it accepts only the ordinals of its 6 reactions. A 2.1.13 node which receives such a
    // reaction closes the connection, and it drops the whole inventory response which contains it. So a node of this
    // version must not accept, store or relay one.
    // The bound can be raised when no supported version rejects higher IDs at parsing.
    // A private reaction has no upper bound (see verify()), because only its receiver gets it.
    public static final int MAX_PUBLIC_REACTION_ID = 5;

    protected final String id;
    protected final String userProfileId;
    private final String chatChannelId;
    private final ChatChannelDomain chatChannelDomain;
    private final String chatMessageId;
    private final int reactionId; // ordinal of Reaction icon enum
    private final long date;

    public ChatMessageReaction(String id,
                               String userProfileId,
                               String chatChannelId,
                               ChatChannelDomain chatChannelDomain,
                               String chatMessageId,
                               int reactionId,
                               long date) {
        this.id = id;
        this.userProfileId = userProfileId;
        this.chatChannelId = chatChannelId;
        this.chatChannelDomain = chatChannelDomain;
        this.chatMessageId = chatMessageId;
        this.reactionId = reactionId;
        this.date = date;
    }

    @Override
    public void verify() {
        checkArgument(reactionId >= 0, "Invalid reaction id: " + reactionId);

        NetworkDataValidation.validateProfileId(userProfileId);
        NetworkDataValidation.validateText(chatChannelId, 200); // For private channels we combine user profile IDs for channelId
        NetworkDataValidation.validateDate(date);
    }

    // The verify() of a public reaction calls it
    protected void verifyPublicReactionId() {
        checkArgument(reactionId <= MAX_PUBLIC_REACTION_ID,
                "The reaction id of a public reaction must not be higher than " + MAX_PUBLIC_REACTION_ID);
    }

    public bisq.chat.protobuf.ChatMessageReaction.Builder getChatMessageReactionBuilder(boolean serializeForHash) {
        return bisq.chat.protobuf.ChatMessageReaction.newBuilder()
                .setId(id)
                .setUserProfileId(userProfileId)
                .setChatChannelId(chatChannelId)
                .setChatChannelDomain(chatChannelDomain.toProtoEnum())
                .setChatMessageId(chatMessageId)
                .setReactionId(reactionId)
                .setDate(date);
    }

    public static ChatMessageReaction fromProto(bisq.chat.protobuf.ChatMessageReaction proto)
            throws UnresolvableProtobufException {
        return switch (proto.getMessageCase()) {
            case COMMONPUBLICCHATMESSAGEREACTION -> CommonPublicChatMessageReaction.fromProto(proto);
            case BISQEASYOFFERBOOKMESSAGEREACTION -> BisqEasyOfferbookMessageReaction.fromProto(proto);
            case TWOPARTYPRIVATECHATMESSAGEREACTION -> TwoPartyPrivateChatMessageReaction.fromProto(proto);
            case BISQEASYOPENTRADEMESSAGEREACTION -> BisqEasyOpenTradeMessageReaction.fromProto(proto);
            case MUSIGOPENTRADEMESSAGEREACTION -> MuSigOpenTradeMessageReaction.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }

    public static ProtoResolver<DistributedData> getDistributedDataResolver() {
        return any -> {
            try {
                bisq.chat.protobuf.ChatMessageReaction proto = any.unpack(bisq.chat.protobuf.ChatMessageReaction.class);
                return switch (proto.getMessageCase()) {
                    case COMMONPUBLICCHATMESSAGEREACTION -> CommonPublicChatMessageReaction.fromProto(proto);
                    case BISQEASYOFFERBOOKMESSAGEREACTION -> BisqEasyOfferbookMessageReaction.fromProto(proto);
                    case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
                    default -> throw new UnresolvableProtobufMessageException(proto);
                };
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    public static ProtoResolver<ExternalNetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.chat.protobuf.ChatMessageReaction proto = any.unpack(bisq.chat.protobuf.ChatMessageReaction.class);
                return switch (proto.getMessageCase()) {
                    case TWOPARTYPRIVATECHATMESSAGEREACTION -> TwoPartyPrivateChatMessageReaction.fromProto(proto);
                    case BISQEASYOPENTRADEMESSAGEREACTION -> BisqEasyOpenTradeMessageReaction.fromProto(proto);
                    case MUSIGOPENTRADEMESSAGEREACTION -> MuSigOpenTradeMessageReaction.fromProto(proto);
                    case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
                    default -> throw new UnresolvableProtobufMessageException(proto);
                };
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    public boolean matches(ChatMessageReaction other) {
        return other.getUserProfileId().equals(getUserProfileId())
                && other.getChatChannelId().equals(getChatChannelId())
                && other.getChatChannelDomain() == getChatChannelDomain()
                && other.getChatMessageId().equals(getChatMessageId())
                && other.getReactionId() == getReactionId();
    }

    @Override
    public String toString() {
        return "ChatMessageReaction{" +
                "\r\n                    id='" + id + '\'' +
                ",\r\n                    userProfileId=" + userProfileId +
                ",\r\n                    chatChannelId=" + chatChannelId +
                ",\r\n                    chatChannelDomain=" + chatChannelDomain +
                ",\r\n                    chatMessageId='" + chatMessageId + '\'' +
                ",\r\n                    reactionId='" + reactionId + '\'' +
                ",\r\n                    date=" + date +
                "\r\n}";
    }
}
