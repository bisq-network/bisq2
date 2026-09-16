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


package bisq.api.chat.common;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatService;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.CommonPublicChatChannelService;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The public chat channels the API serves: Discussions and Support. Bisq 2 keeps one
 * {@link CommonPublicChatChannelService} per {@link ChatChannelDomain}, each owning a single channel;
 * the API addresses them by channel id instead, so this is where an id is turned back into the
 * service that has to publish for it.
 */
public class PublicChatChannels {
    private final Map<ChatChannelDomain, CommonPublicChatChannelService> servicesByDomain =
            new EnumMap<>(ChatChannelDomain.class);

    public PublicChatChannels(ChatService chatService) {
        this(chatService.getCommonPublicChatChannelServices());
    }

    /**
     * Copied rather than kept by reference: {@code ChatService.shutdown} empties its map. The copy is an
     * {@code EnumMap}, so the channels are listed in the domain's declaration order.
     */
    public PublicChatChannels(Map<ChatChannelDomain, CommonPublicChatChannelService> servicesByDomain) {
        this.servicesByDomain.putAll(servicesByDomain);
    }

    /**
     * One channel per domain, even on a node upgraded from before v2.1.1: the channels consolidated
     * away are still in its store, but {@link CommonPublicChatChannel#getId()} answers the migrated id
     * and the channel's {@code equals}/{@code hashCode} are that id, so the set they are read from
     * holds only one of them. Which one survived decides the channel's title, not its id.
     */
    public Stream<CommonPublicChatChannel> getChannels() {
        return servicesByDomain.values().stream().flatMap(service -> service.getChannels().stream());
    }

    public Optional<CommonPublicChatChannel> findChannel(String channelId) {
        return getChannels().filter(channel -> channel.getId().equals(channelId)).findFirst();
    }

    /**
     * The service that owns the channel, which must be one of {@link #getChannels()}. Asserted rather
     * than left implicit: without the check the caller gets the map's bare null and the endpoints turn
     * it into a 500 with nothing in it to say which domain went missing.
     */
    public CommonPublicChatChannelService serviceOf(CommonPublicChatChannel channel) {
        ChatChannelDomain chatChannelDomain = channel.getChatChannelDomain();
        return checkNotNull(servicesByDomain.get(chatChannelDomain),
                "No CommonPublicChatChannelService is registered for domain " + chatChannelDomain);
    }
}
