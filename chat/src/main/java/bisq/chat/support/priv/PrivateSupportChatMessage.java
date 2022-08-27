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

package bisq.chat.support.priv;

import bisq.chat.message.ChatMessage;
import bisq.chat.message.PrivateChatMessage;
import bisq.chat.message.Quotation;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.protobuf.ExternalNetworkMessage;
import bisq.network.protobuf.NetworkMessage;
import bisq.user.profile.UserProfile;
import com.google.protobuf.Any;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class PrivateSupportChatMessage extends PrivateChatMessage {
    public PrivateSupportChatMessage(String messageId,
                                     String channelId,
                                     UserProfile sender,
                                     String receiversId,
                                     String text,
                                     Optional<Quotation> quotedMessage,
                                     long date,
                                     boolean wasEdited) {
        super(messageId,
                channelId,
                sender,
                receiversId,
                text,
                quotedMessage,
                date,
                wasEdited,
                new MetaData(ChatMessage.TTL, 100000, PrivateSupportChatMessage.class.getSimpleName()));
    }

    private PrivateSupportChatMessage(String messageId,
                                      String channelId,
                                      UserProfile sender,
                                      String receiversId,
                                      String text,
                                      Optional<Quotation> quotedMessage,
                                      long date,
                                      boolean wasEdited,
                                      MetaData metaData) {
        super(messageId, channelId, sender, receiversId, text, quotedMessage, date, wasEdited, metaData);
    }

    @Override
    public NetworkMessage toProto() {
        return getNetworkMessageBuilder()
                .setExternalNetworkMessage(ExternalNetworkMessage.newBuilder().setAny(Any.pack(toChatMessageProto())))
                .build();
    }

    public bisq.chat.protobuf.ChatMessage toChatMessageProto() {
        return getChatMessageBuilder()
                .setPrivateSupportChatMessage(bisq.chat.protobuf.PrivateSupportChatMessage.newBuilder()
                        .setReceiversId(receiversId)
                        .setSender(sender.toProto()))
                .build();
    }

    public static PrivateSupportChatMessage fromProto(bisq.chat.protobuf.ChatMessage baseProto) {
        Optional<Quotation> quotedMessage = baseProto.hasQuotation() ?
                Optional.of(Quotation.fromProto(baseProto.getQuotation())) :
                Optional.empty();
        bisq.chat.protobuf.PrivateSupportChatMessage privateSupportChatMessage = baseProto.getPrivateSupportChatMessage();
        return new PrivateSupportChatMessage(
                baseProto.getMessageId(),
                baseProto.getChannelId(),
                UserProfile.fromProto(privateSupportChatMessage.getSender()),
                privateSupportChatMessage.getReceiversId(),
                baseProto.getText(),
                quotedMessage,
                baseProto.getDate(),
                baseProto.getWasEdited(),
                MetaData.fromProto(baseProto.getMetaData()));
    }
}