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

package bisq.chat.channels;

import bisq.chat.messages.ChatMessage;
import bisq.common.observable.ObservableSet;
import bisq.network.NetworkService;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public abstract class ChannelService<T extends Channel<?>> {
    @Getter
    protected final UserIdentityService userIdentityService;
    protected final NetworkService networkService;

    public ChannelService(NetworkService networkService,
                          UserIdentityService userIdentityService) {
        this.networkService = networkService;
        this.userIdentityService = userIdentityService;
    }

    public boolean isMyMessage(ChatMessage chatMessage) {
        String authorId = chatMessage.getAuthorId();
        return userIdentityService.getUserIdentities().stream()
                .anyMatch(userIdentity -> userIdentity.getUserProfile().getId().equals(authorId));
    }

    protected Optional<T> findChannel(String channelId) {
        return getChannels().stream()
                .filter(channel -> channel.getId().equals(channelId))
                .findAny();
    }

    abstract public ObservableSet<T> getChannels();
}