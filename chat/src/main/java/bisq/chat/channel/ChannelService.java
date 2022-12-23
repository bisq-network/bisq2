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

import bisq.chat.message.ChatMessage;
import bisq.common.application.Service;
import bisq.common.observable.ObservableArray;
import bisq.network.NetworkService;
import bisq.persistence.PersistableStore;
import bisq.persistence.PersistenceClient;
import bisq.user.identity.UserIdentityService;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public abstract class ChannelService<M extends ChatMessage, C extends Channel<M>, S extends PersistableStore<S>>
        implements Service, PersistenceClient<S> {
    protected final NetworkService networkService;
    protected final UserIdentityService userIdentityService;
    protected final ChannelDomain channelDomain;

    public ChannelService(NetworkService networkService, UserIdentityService userIdentityService, ChannelDomain channelDomain) {
        this.networkService = networkService;
        this.userIdentityService = userIdentityService;
        this.channelDomain = channelDomain;
    }

    public void setNotificationSetting(Channel<? extends ChatMessage> channel, ChannelNotificationType channelNotificationType) {
        channel.getChannelNotificationType().set(channelNotificationType);
        persist();
    }

    public Optional<C> findChannel(String channelId) {
        return getChannels().stream()
                .filter(channel -> channel.getId().equals(channelId))
                .findAny();
    }

    protected abstract ObservableArray<C> getChannels();

    protected void addMessage(M message, C channel) {
        synchronized (getPersistableStore()) {
            channel.addChatMessage(message);
        }
        persist();
    }
}