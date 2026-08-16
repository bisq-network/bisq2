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

package bisq.api.web_socket.domain.chat.private_chat;

import bisq.api.dto.DtoMappings;
import bisq.api.dto.chat.two_party.TwoPartyPrivateChatMessageDto;
import bisq.api.dto.mappings.chat.two_party.TwoPartyPrivateChatMessageDtoMapping;
import bisq.api.dto.user.profile.UserProfileDto;
import bisq.api.web_socket.domain.BaseWebSocketService;
import bisq.api.web_socket.subscription.ModificationType;
import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfileService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bisq.api.web_socket.subscription.Topic.PRIVATE_CHAT_MESSAGES;

/**
 * Pushes two-party private chat (DM) messages. Structurally the trade-chat sibling
 * {@code TradeChatMessagesWebSocketService}, with one addition: messages from banned senders are
 * dropped, matching what Bisq 2 desktop and the mobile node facade do.
 * <p>
 * Beware that this filter and the channel's unread count disagree. The count comes from
 * {@code ChatNotificationService}, which drops *ignored* senders and keeps banned ones — the opposite
 * choice. So a peer banned after their messages arrived leaves a channel reporting unread messages
 * this stream no longer sends. Ignored senders are kept on purpose: the client hides them against its
 * own list of ignored ids, so un-ignoring brings the conversation back.
 */
@Slf4j
public class PrivateChatMessagesWebSocketService extends BaseWebSocketService {
    private final TwoPartyPrivateChatChannelService channelService;
    private final UserProfileService userProfileService;
    private final BannedUserService bannedUserService;
    @Nullable
    private Pin channelsPin;
    private final Map<String, Pin> messagesByChannelIdPins = new ConcurrentHashMap<>();

    public PrivateChatMessagesWebSocketService(SubscriberRepository subscriberRepository,
                                               TwoPartyPrivateChatChannelService channelService,
                                               UserProfileService userProfileService,
                                               BannedUserService bannedUserService) {
        super(subscriberRepository, PRIVATE_CHAT_MESSAGES);

        this.channelService = channelService;
        this.userProfileService = userProfileService;
        this.bannedUserService = bannedUserService;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        channelsPin = channelService.getChannels().addObserver(new CollectionObserver<>() {
            @Override
            public void onAdded(TwoPartyPrivateChatChannel channel) {
                String channelId = channel.getId();
                // Atomic operation
                messagesByChannelIdPins.compute(channelId, (key, oldPin) -> {
                    if (oldPin != null) {
                        oldPin.unbind();
                    }

                    return channel.getChatMessages().addObserver(new CollectionObserver<>() {
                        @Override
                        public void onAdded(TwoPartyPrivateChatMessage message) {
                            handleAddedMessage(message);
                        }

                        @Override
                        public void onRemoved(Object element) {
                            // Private chat messages cannot be removed
                        }

                        @Override
                        public void onCleared() {
                            // Private chat messages cannot be removed
                        }
                    });
                });
            }

            @Override
            public void onRemoved(Object element) {
                if (element instanceof TwoPartyPrivateChatChannel channel) {
                    String channelId = channel.getId();
                    // Atomic operation
                    messagesByChannelIdPins.computeIfPresent(channelId, (key, pin) -> {
                        pin.unbind();
                        return null;  // returning null removes the key
                    });
                }
            }

            @Override
            public void onCleared() {
                unbindAllMessagePins();
            }
        });
        return CompletableFuture.completedFuture(true);
    }

    private void unbindAllMessagePins() {
        new ArrayList<>(messagesByChannelIdPins.values()).forEach(Pin::unbind);
        messagesByChannelIdPins.clear();
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (channelsPin != null) {
            channelsPin.unbind();
            channelsPin = null;
        }
        unbindAllMessagePins();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public Optional<String> getJsonPayload() {
        return getJsonPayload(channelService.getChannels().stream());
    }

    private Optional<String> getJsonPayload(Stream<TwoPartyPrivateChatChannel> channels) {
        ArrayList<TwoPartyPrivateChatMessageDto> payload = channels
                .flatMap(channel ->
                        channel.getChatMessages().stream()
                                .filter(this::isNotFromBannedUser)
                                .map(message -> {
                                    try {
                                        return toDto(message);
                                    } catch (Exception e) {
                                        log.error("Failed to create TwoPartyPrivateChatMessageDto", e);
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull))
                .collect(Collectors.toCollection(ArrayList::new));
        return toJson(payload);
    }

    private void handleAddedMessage(TwoPartyPrivateChatMessage message) {
        if (!isNotFromBannedUser(message)) {
            return;
        }
        TwoPartyPrivateChatMessageDto dto;
        try {
            dto = toDto(message);
        } catch (Exception e) {
            // Mirrors getJsonPayload: this runs inside a CollectionObserver callback, where an escaping
            // exception can take the observer down for every later message.
            log.error("Failed to create TwoPartyPrivateChatMessageDto", e);
            return;
        }
        handleAddedMessages(Collections.singletonList(dto));
    }

    private void handleAddedMessages(List<TwoPartyPrivateChatMessageDto> messages) {
        // The payload is defined as a list to support batch data delivery at subscribe.
        List<Subscriber> subscribers = findSubscribers();
        if (subscribers.isEmpty()) {
            return;
        }
        toJson(messages).ifPresent(json ->
                subscribers.forEach(subscriber -> send(json, subscriber, ModificationType.ADDED)));
    }

    /**
     * Bisq 2 already rejects banned senders on the inbound path, so this only covers a peer banned
     * *after* their messages arrived — which is why desktop re-checks it too.
     */
    private boolean isNotFromBannedUser(TwoPartyPrivateChatMessage message) {
        return !bannedUserService.isUserProfileBanned(message.getSenderUserProfile());
    }

    private TwoPartyPrivateChatMessageDto toDto(TwoPartyPrivateChatMessage message) {
        Optional<UserProfileDto> citationAuthorUserProfile = message.getCitation()
                .flatMap(citation -> userProfileService.findUserProfile(citation.getAuthorUserProfileId()))
                .map(DtoMappings.UserProfileMapping::fromBisq2Model);
        return TwoPartyPrivateChatMessageDtoMapping.fromBisq2Model(message, citationAuthorUserProfile);
    }
}
