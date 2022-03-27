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

import bisq.network.p2p.services.data.storage.MetaData;
import bisq.social.user.ChatUser;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@Getter
@ToString
@EqualsAndHashCode
public abstract class ChatMessage {
    protected final String channelId;
    protected final String text;
    protected ChatUser chatUser;
    protected final Optional<QuotedMessage> quotedMessage;
    protected final long date;
    protected final ChannelType channelType;
    protected final boolean wasEdited;
    protected final MetaData metaData;

    protected ChatMessage(String channelId,
                          ChatUser chatUser,
                          String text,
                          Optional<QuotedMessage> quotedMessage,
                          long date,
                          ChannelType channelType,
                          boolean wasEdited,
                          MetaData metaData) {
        this.channelId = channelId;
        this.chatUser = chatUser;
        this.text = text;
        this.quotedMessage = quotedMessage;
        this.date = date;
        this.channelType = channelType;
        this.wasEdited = wasEdited;
        this.metaData = metaData;
    }
}