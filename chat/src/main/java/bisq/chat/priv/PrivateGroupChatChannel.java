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
import bisq.chat.notifications.ChatChannelNotificationType;
import bisq.user.identity.UserIdentity;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Set;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public abstract class PrivateGroupChatChannel<M extends PrivateChatMessage<?>> extends PrivateChatChannel<M> {

    protected PrivateGroupChatChannel(String id,
                                      ChatChannelDomain chatChannelDomain,
                                      UserIdentity myUserIdentity,
                                      Set<M> chatMessages,
                                      ChatChannelNotificationType chatChannelNotificationType) {
        super(id, chatChannelDomain, myUserIdentity, chatMessages, chatChannelNotificationType);
    }
}
