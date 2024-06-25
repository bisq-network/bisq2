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
import bisq.common.encoding.Hex;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.services.data.storage.DistributedData;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
@EqualsAndHashCode
public abstract class ChatMessageReaction implements DistributedData {
    public static String createId(String channelId, String messageId, int reactionId, String userProfileId) {
        return String.format("%s.%s.%s.%s", channelId, messageId, reactionId, userProfileId);
    }

    private final String id;
    private final String userProfileId;
    private final String chatChannelId;
    private final ChatChannelDomain chatChannelDomain;
    private final String chatMessageId;
    private final int reactionId;
    private final boolean isRemoved;
    private final long date;

    public ChatMessageReaction(String id,
                               String userProfileId,
                               String chatChannelId,
                               ChatChannelDomain chatChannelDomain,
                               String chatMessageId,
                               int reactionId,
                               boolean isRemoved,
                               long date) {
        this.id = id;
        this.userProfileId = userProfileId;
        this.chatChannelId = chatChannelId;
        this.chatChannelDomain = chatChannelDomain;
        this.chatMessageId = chatMessageId;
        this.reactionId = reactionId;
        this.isRemoved = isRemoved;
        this.date = date;
    }

    @Override
    public boolean isDataInvalid(byte[] pubKeyHash) {
        // AuthorId must be pubKeyHash. We get pubKeyHash passed from the data storage layer where the signature is
        // verified as well, so we can be sure it's the sender of the message. This check prevents against
        // impersonation attack.
        return !userProfileId.equals(Hex.encode(pubKeyHash));
    }

    @Override
    public double getCostFactor() {
        return 0.3;
    }

    @Override
    public void verify() {
        checkArgument(reactionId >= 0 && reactionId < Reaction.values().length, "Invalid reaction id: " + reactionId);

        NetworkDataValidation.validateProfileId(userProfileId);
        NetworkDataValidation.validateText(chatChannelId, 200); // For private channels we combine user profile IDs for channelId
        NetworkDataValidation.validateDate(date);
    }

    @Override
    public bisq.chat.protobuf.ChatMessageReaction toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    protected bisq.chat.protobuf.ChatMessageReaction.Builder getChatMessageReactionBuilder(boolean serializeForHash) {
        return bisq.chat.protobuf.ChatMessageReaction.newBuilder()
                .setId(id)
                .setUserProfileId(userProfileId)
                .setChatChannelId(chatChannelId)
                .setChatChannelDomain(chatChannelDomain.toProtoEnum())
                .setChatMessageId(chatMessageId)
                .setReactionId(reactionId)
                .setIsRemoved(isRemoved)
                .setDate(date);
    }

    public static ChatMessageReaction fromProto(bisq.chat.protobuf.ChatMessageReaction proto) {
        switch (proto.getMessageCase()) {
            // TODO: Implement reactions for the remaining messages
            case COMMONPUBLICCHATMESSAGEREACTION: {
                return CommonPublicChatMessageReaction.fromProto(proto);
            }
            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }

    public static ProtoResolver<DistributedData> getDistributedDataResolver() {
        return any -> {
            try {
                bisq.chat.protobuf.ChatMessageReaction proto = any.unpack(bisq.chat.protobuf.ChatMessageReaction.class);
                switch (proto.getMessageCase()) {
                    // TODO: Implement reactions for the remaining messages
                    case COMMONPUBLICCHATMESSAGEREACTION: {
                        return CommonPublicChatMessageReaction.fromProto(proto);
                    }
                    case MESSAGE_NOT_SET: {
                        throw new UnresolvableProtobufMessageException(proto);
                    }
                }
                throw new UnresolvableProtobufMessageException(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
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
                ",\r\n                    isRemoved='" + isRemoved + '\'' +
                ",\r\n                    date=" + date +
                "\r\n}";
    }
}
