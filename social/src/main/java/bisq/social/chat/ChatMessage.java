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

package bisq.social.chat;

import bisq.network.NetworkId;
import bisq.network.p2p.message.Proto;
import bisq.social.user.ChatUser;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nullable;
import java.util.Optional;

@Getter
@ToString
@EqualsAndHashCode
public abstract class ChatMessage implements Proto {
    protected final String channelId;
    protected final String text;

    @Nullable
    protected final QuotedMessage quotedMessage;
    protected final NetworkId senderNetworkId;
    protected final long date;
    protected final ChannelType channelType;
    protected final boolean wasEdited;

    @Nullable
    protected transient String senderUserName;
    @Nullable
    protected transient ChatUser chatUser;

    protected ChatMessage(String channelId,
                          String text,
                          Optional<QuotedMessage> quotedMessage,
                          NetworkId senderNetworkId,
                          long date,
                          ChannelType channelType,
                          boolean wasEdited) {
        this.channelId = channelId;
        this.text = text;
        this.quotedMessage = quotedMessage.orElse(null);
        this.senderNetworkId = senderNetworkId;
        this.date = date;
        this.channelType = channelType;

        //todo we need also entitlements
        chatUser = new ChatUser(senderNetworkId);
        this.wasEdited = wasEdited;
        this.senderUserName = chatUser.userName();
    }

    @Nullable
    public Optional<QuotedMessage> getQuotedMessage() {
        return Optional.ofNullable(quotedMessage);
    }

    public ChatUser getChatUser() {
        if (chatUser == null) {
            chatUser = new ChatUser(senderNetworkId);
        }
        return chatUser;
    }

    public String getSenderUserName() {
        if (senderUserName == null) {
            senderUserName = getChatUser().userName();
        }
        return senderUserName;
    }
}