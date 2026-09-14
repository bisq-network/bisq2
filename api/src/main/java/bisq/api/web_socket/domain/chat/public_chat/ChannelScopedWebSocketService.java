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


package bisq.api.web_socket.domain.chat.public_chat;

import bisq.api.chat.common.PublicChatChannels;
import bisq.api.chat.common.PublicChatDtoFactory;
import bisq.api.web_socket.domain.BaseWebSocketService;
import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.SubscriptionRequest;
import bisq.api.web_socket.subscription.Topic;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.common.util.StringUtils;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfileService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The public chat topics that take a channel id as their subscription parameter: a subscriber that
 * names one is served and pushed that channel only, one that names none gets every public channel.
 * Follows {@code OffersWebSocketService}, which scopes its topic by quote currency the same way.
 * <p>
 * An unknown id is rejected at subscribe rather than answered with an empty stream, so a client with a
 * stale channel id learns about it instead of waiting forever for messages.
 */
abstract class ChannelScopedWebSocketService extends BaseWebSocketService {
    protected final PublicChatChannels channels;
    protected final PublicChatDtoFactory dtoFactory;

    ChannelScopedWebSocketService(SubscriberRepository subscriberRepository,
                                  Topic topic,
                                  PublicChatChannels channels,
                                  UserProfileService userProfileService,
                                  BannedUserService bannedUserService) {
        super(subscriberRepository, topic);

        this.channels = channels;
        this.dtoFactory = new PublicChatDtoFactory(userProfileService, bannedUserService);
    }

    /**
     * Trimmed only. A channel id is an opaque token the client copied out of the channel list, not a
     * value with a canonical spelling like the currency code {@code OffersWebSocketService} lowercases,
     * and folding case here would accept ids that {@code GET /public-chat-channels/{channelId}/messages}
     * answers with a 404.
     */
    @Override
    public Optional<String> canonicalizeParameter(Optional<String> parameter) {
        return parameter.map(String::trim).filter(id -> !id.isEmpty());
    }

    @Override
    public void validate(SubscriptionRequest request) {
        Optional<String> channelId = canonicalizeParameter(StringUtils.toOptional(request.getParameter()));
        if (channelId.isPresent() && channels.findChannel(channelId.get()).isEmpty()) {
            throw new IllegalArgumentException("No channel found for channel ID " + channelId.get());
        }
    }

    /** The channels a subscription covers: the one it named, or all of them. */
    protected Stream<CommonPublicChatChannel> channelsOf(Optional<String> parameter) {
        return parameter.map(channelId -> channels.findChannel(channelId).stream())
                .orElseGet(channels::getChannels);
    }

    /** Who hears about something in this channel: the subscribers scoped to it, and those scoped to none. */
    protected List<Subscriber> findSubscribers(String channelId) {
        return Stream.concat(
                        subscriberRepository.findSubscribers(topic, canonicalizeParameter(Optional.of(channelId))).stream(),
                        subscriberRepository.findSubscribers(topic, Optional.empty()).stream())
                .toList();
    }
}
