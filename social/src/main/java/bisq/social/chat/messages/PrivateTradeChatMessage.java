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

package bisq.social.chat.messages;

import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.network.protobuf.ExternalNetworkMessage;
import bisq.network.protobuf.NetworkMessage;
import bisq.social.user.ChatUserProfile;
import com.google.protobuf.Any;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * PrivateChatMessage is sent as direct message to peer and in case peer is not online it can be stores as
 * mailbox message.
 */
@Getter
@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PrivateTradeChatMessage extends ChatMessage implements MailboxMessage {
    private final String receiversProfileId;

    public PrivateTradeChatMessage(String channelId,
                                   ChatUserProfile sender,
                                   String receiversProfileId,
                                   String text,
                                   Optional<Quotation> quotedMessage,
                                   long date,
                                   boolean wasEdited) {
        this(channelId,
                sender,
                receiversProfileId,
                text,
                quotedMessage,
                date,
                wasEdited,
                new MetaData(TimeUnit.DAYS.toMillis(1), 100000, PrivateTradeChatMessage.class.getSimpleName()));
    }

    private PrivateTradeChatMessage(String channelId,
                                    ChatUserProfile sender,
                                    String receiversProfileId,
                                    String text,
                                    Optional<Quotation> quotedMessage,
                                    long date,
                                    boolean wasEdited,
                                    MetaData metaData) {
        super(channelId,
                sender,
                Optional.of(text),
                quotedMessage,
                date,
                wasEdited,
                metaData);
        this.receiversProfileId = receiversProfileId;
    }

    @Override
    public NetworkMessage toProto() {
        return getNetworkMessageBuilder()
                .setExternalNetworkMessage(ExternalNetworkMessage.newBuilder().setAny(Any.pack(toChatMessageProto())))
                .build();
    }

    public bisq.social.protobuf.ChatMessage toChatMessageProto() {
        return getChatMessageBuilder()
                .setPrivateTradeChatMessage(bisq.social.protobuf.PrivateTradeChatMessage.newBuilder()
                        .setReceiversProfileId(receiversProfileId))
                .build();
    }

    public static PrivateTradeChatMessage fromProto(bisq.social.protobuf.ChatMessage baseProto) {
        Optional<Quotation> quotedMessage = baseProto.hasQuotedMessage() ?
                Optional.of(Quotation.fromProto(baseProto.getQuotedMessage())) :
                Optional.empty();
        return new PrivateTradeChatMessage(
                baseProto.getChannelId(),
                ChatUserProfile.fromProto(baseProto.getAuthor()),
                baseProto.getPrivateTradeChatMessage().getReceiversProfileId(),
                baseProto.getText(),
                quotedMessage,
                baseProto.getDate(),
                baseProto.getWasEdited(),
                MetaData.fromProto(baseProto.getMetaData()));
    }

    // Required for MailboxMessage use case
    @Override
    public MetaData getMetaData() {
        return metaData;
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - getDate() > getMetaData().getTtl());
    }
}