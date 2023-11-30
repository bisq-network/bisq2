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

package bisq.chat.priv;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.ChatMessageType;
import bisq.chat.Citation;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.confidential.ack.AckRequestingMessage;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * PrivateChatMessage is sent as direct message to peer and in case peer is not online it can be stores as
 * mailbox message.
 */
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class PrivateChatMessage extends ChatMessage implements MailboxMessage, AckRequestingMessage {
    // In group channels we send a message to multiple peers but want to avoid that the message gets duplicated in our hashSet by a different receiverUserProfileId
    @EqualsAndHashCode.Exclude
    protected final String receiverUserProfileId;
    protected final UserProfile senderUserProfile;
    @EqualsAndHashCode.Exclude
    protected final NetworkId receiverNetworkId;

    protected PrivateChatMessage(String messageId,
                                 ChatChannelDomain chatChannelDomain,
                                 String channelId,
                                 UserProfile senderUserProfile,
                                 String receiverUserProfileId,
                                 NetworkId receiverNetworkId,
                                 @Nullable String text,
                                 Optional<Citation> citation,
                                 long date,
                                 boolean wasEdited,
                                 ChatMessageType chatMessageType) {
        this(messageId,
                chatChannelDomain,
                channelId,
                senderUserProfile,
                receiverUserProfileId,
                receiverNetworkId,
                Optional.ofNullable(text),
                citation,
                date,
                wasEdited,
                chatMessageType);
    }

    protected PrivateChatMessage(String messageId,
                                 ChatChannelDomain chatChannelDomain,
                                 String channelId,
                                 UserProfile senderUserProfile,
                                 String receiverUserProfileId,
                                 NetworkId receiverNetworkId,
                                 Optional<String> text,
                                 Optional<Citation> citation,
                                 long date,
                                 boolean wasEdited,
                                 ChatMessageType chatMessageType) {
        super(messageId,
                chatChannelDomain,
                channelId,
                senderUserProfile.getId(),
                text,
                citation,
                date,
                wasEdited,
                chatMessageType);
        this.receiverUserProfileId = receiverUserProfileId;
        this.senderUserProfile = senderUserProfile;
        this.receiverNetworkId = receiverNetworkId;

        NetworkDataValidation.validateProfileId(receiverUserProfileId);
    }

    @Override
    public NetworkId getSender() {
        return senderUserProfile.getNetworkId();
    }

    @Override
    public NetworkId getReceiver() {
        return receiverNetworkId;
    }
}