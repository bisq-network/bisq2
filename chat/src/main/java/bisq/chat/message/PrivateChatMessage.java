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

package bisq.chat.message;

import bisq.chat.channel.ChatChannelDomain;
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
public abstract class PrivateChatMessage extends ChatMessage implements MailboxMessage {
    // In group channels we send a message to multiple peers but want to avoid that the message gets duplicated in our hashSet by a different receiverUserProfileId
    @EqualsAndHashCode.Exclude
    protected final String receiverUserProfileId;
    protected final UserProfile sender;

    protected PrivateChatMessage(String messageId,
                                 ChatChannelDomain chatChannelDomain,
                                 String channelId,
                                 UserProfile sender,
                                 String receiverUserProfileId,
                                 @Nullable String text,
                                 Optional<Citation> citation,
                                 long date,
                                 boolean wasEdited,
                                 ChatMessageType chatMessageType) {
        this(messageId,
                chatChannelDomain,
                channelId,
                sender,
                receiverUserProfileId,
                Optional.ofNullable(text),
                citation,
                date,
                wasEdited,
                chatMessageType);
    }

    protected PrivateChatMessage(String messageId,
                                 ChatChannelDomain chatChannelDomain,
                                 String channelId,
                                 UserProfile sender,
                                 String receiverUserProfileId,
                                 Optional<String> text,
                                 Optional<Citation> citation,
                                 long date,
                                 boolean wasEdited,
                                 ChatMessageType chatMessageType) {
        super(messageId,
                chatChannelDomain,
                channelId,
                sender.getId(),
                text,
                citation,
                date,
                wasEdited,
                chatMessageType);
        this.receiverUserProfileId = receiverUserProfileId;
        this.sender = sender;
    }
}