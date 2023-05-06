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

package bisq.chat.channel;

import bisq.chat.message.BasePrivateChatMessage;
import bisq.common.observable.collection.ObservableArray;
import bisq.common.observable.collection.ObservableSet;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public abstract class PrivateChatChannel<T extends BasePrivateChatMessage> extends ChatChannel<T> {
    protected final UserIdentity myUserIdentity;

    // We persist the messages as they are NOT persisted in the P2P data store.
    protected final ObservableSet<T> chatMessages = new ObservableSet<>();
    protected final ObservableSet<ChannelMember> channelMembers = new ObservableSet<>();
    protected final ObservableArray<UserProfile> peers = new ObservableArray<>();

    public PrivateChatChannel(ChannelDomain channelDomain,
                              String channelName,
                              UserIdentity myUserIdentity,
                              List<T> chatMessages,
                              ChannelNotificationType channelNotificationType) {
        super(channelDomain, channelName, channelNotificationType);

        this.myUserIdentity = myUserIdentity;
        this.chatMessages.addAll(chatMessages);
    }

    public void addChannelMember(ChannelMember channelMember) {
        channelMembers.add(channelMember);
        if (channelMember.getType() != ChannelMember.Type.SELF) {
            peers.add(channelMember.getUserProfile());
        }
    }
/*
    public Set<String> getPeersProfileIds() {
        return peers.stream()
                .map(UserProfile::getId)
                .collect(Collectors.toSet());
    }*/

    @Override
    public Set<String> getMembers() {
        return channelMembers.stream()
                .map(ChannelMember::getUserProfile)
                .map(UserProfile::getId)
                .collect(Collectors.toSet());
    }
}
