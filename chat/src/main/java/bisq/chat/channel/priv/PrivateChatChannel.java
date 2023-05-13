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

import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelNotificationType;
import bisq.chat.message.PrivateChatMessage;
import bisq.common.observable.collection.ObservableArray;
import bisq.common.observable.collection.ObservableSet;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public abstract class PrivateChatChannel<M extends PrivateChatMessage> extends ChatChannel<M> {
    protected final UserIdentity myUserIdentity;
    // We persist the messages as they are NOT persisted in the P2P data store.
    protected final ObservableSet<M> chatMessages = new ObservableSet<>();
    protected transient final ObservableSet<PrivateChatChannelMember> channelMembers = new ObservableSet<>();
    protected transient final ObservableArray<UserProfile> peers = new ObservableArray<>();

    public PrivateChatChannel(String id,
                              ChatChannelDomain chatChannelDomain,
                              UserIdentity myUserIdentity,
                              List<M> chatMessages,
                              ChatChannelNotificationType chatChannelNotificationType) {
        super(id, chatChannelDomain, chatChannelNotificationType);

        this.myUserIdentity = myUserIdentity;
        this.chatMessages.addAll(chatMessages);

        addChannelMember(new PrivateChatChannelMember(PrivateChatChannelMember.Type.SELF, myUserIdentity.getUserProfile()));
    }

    public void addChannelMember(PrivateChatChannelMember channelMember) {
        boolean changed = channelMembers.add(channelMember);
        if (changed && channelMember.getType() != PrivateChatChannelMember.Type.SELF) {
            peers.add(channelMember.getUserProfile());
        }
    }

    public void removeChannelMember(PrivateChatChannelMember channelMember) {
        boolean changed = channelMembers.remove(channelMember);
        if (changed && channelMember.getType() != PrivateChatChannelMember.Type.SELF) {
            peers.remove(channelMember.getUserProfile());
        }
    }

    public Optional<PrivateChatChannelMember> findChannelMember(PrivateChatChannelMember.Type type, UserProfile userProfile) {
        return channelMembers.stream()
                .filter(member -> member.getType() == type && member.getUserProfile().getId().equals(userProfile.getId()))
                .findFirst();
    }

    @Override
    public Set<String> getUserProfileIdsOfAllChannelMembers() {
        return channelMembers.stream()
                .map(PrivateChatChannelMember::getUserProfile)
                .map(UserProfile::getId)
                .collect(Collectors.toSet());
    }
}
