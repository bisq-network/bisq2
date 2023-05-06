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

package bisq.chat.channel.priv;

import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelNotificationType;
import bisq.chat.message.PrivateChatMessage;
import bisq.user.identity.UserIdentity;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public abstract class PrivateGroupChatChannel<M extends PrivateChatMessage> extends PrivateChatChannel<M> {

    protected PrivateGroupChatChannel(ChatChannelDomain chatChannelDomain,
                                      String channelName,
                                      UserIdentity myUserIdentity,
                                      List<M> chatMessages,
                                      ChatChannelNotificationType chatChannelNotificationType) {
        super(chatChannelDomain, channelName, myUserIdentity, chatMessages, chatChannelNotificationType);

        addChannelMember(new PrivateChatChannelMember(PrivateChatChannelMember.Type.SELF, myUserIdentity.getUserProfile()));
    }
}
