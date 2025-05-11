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

package bisq.http_api.web_socket.domain.chat.trade;

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.dto.DtoMappings;
import bisq.dto.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageDto;
import bisq.dto.user.profile.UserProfileDto;
import bisq.http_api.web_socket.domain.BaseWebSocketService;
import bisq.http_api.web_socket.subscription.ModificationType;
import bisq.http_api.web_socket.subscription.SubscriberRepository;
import bisq.user.profile.UserProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static bisq.http_api.web_socket.subscription.Topic.TRADE_CHAT_MESSAGES;

@Slf4j
public class TradeChatMessagesWebSocketService extends BaseWebSocketService {
    private final BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService;
    private final UserProfileService userProfileService;
    @Nullable
    private Pin channelsPin;
    private final Map<String, Pin> messagesByChannelIdPins = new ConcurrentHashMap<>();

    public TradeChatMessagesWebSocketService(ObjectMapper objectMapper,
                                             SubscriberRepository subscriberRepository,
                                             BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService,
                                             UserProfileService userProfileService) {
        super(objectMapper, subscriberRepository, TRADE_CHAT_MESSAGES);

        this.bisqEasyOpenTradeChannelService = bisqEasyOpenTradeChannelService;
        this.userProfileService = userProfileService;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        channelsPin = bisqEasyOpenTradeChannelService.getChannels().addObserver(new CollectionObserver<>() {
            @Override
            public void add(BisqEasyOpenTradeChannel channel) {
                String channelId = channel.getId();
                // Atomic operation
                messagesByChannelIdPins.compute(channelId, (key, oldPin) -> {
                    if (oldPin != null) {
                        oldPin.unbind();
                    }

                    return channel.getChatMessages().addObserver(new CollectionObserver<>() {
                        @Override
                        public void add(BisqEasyOpenTradeMessage message) {
                            handleAddedMessage(message, channelId);
                        }

                        @Override
                        public void remove(Object element) {
                            // BisqEasyOpenTradeMessages cannot be removed
                        }

                        @Override
                        public void clear() {
                            // BisqEasyOpenTradeMessages cannot be removed
                        }
                    });
                });
            }

            @Override
            public void remove(Object element) {
                if (element instanceof BisqEasyOpenTradeChannel channel) {
                    String channelId = channel.getId();
                    // Atomic operation
                    messagesByChannelIdPins.computeIfPresent(channelId, (key, pin) -> {
                        pin.unbind();
                        return null;  // returning null removes the key
                    });
                }
            }

            @Override
            public void clear() {
                new ArrayList<>(messagesByChannelIdPins.values()).forEach(Pin::unbind);

            }
        });
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (channelsPin != null) {
            channelsPin.unbind();
            channelsPin = null;
        }
        new ArrayList<>(messagesByChannelIdPins.values()).forEach(Pin::unbind);
        messagesByChannelIdPins.clear();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public Optional<String> getJsonPayload() {
        return getJsonPayload(bisqEasyOpenTradeChannelService.getChannels().stream());
    }

    private Optional<String> getJsonPayload(Stream<BisqEasyOpenTradeChannel> channels) {
        ArrayList<BisqEasyOpenTradeMessageDto> payload = channels
                .flatMap(channel ->
                        channel.getChatMessages().stream()
                                .map(message -> {
                                    try {
                                        return toDto(message);
                                    } catch (Exception e) {
                                        log.error("Failed to create BisqEasyOpenTradeMessageDto", e);
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull))
                .collect(Collectors.toCollection(ArrayList::new));
        return toJson(payload);
    }

    private void handleAddedMessage(BisqEasyOpenTradeMessage message, String channelId) {
        BisqEasyOpenTradeMessageDto dto = toDto(message);
        handleAddedMessage(Collections.singletonList(dto), channelId);
    }

    private void handleAddedMessage(List<BisqEasyOpenTradeMessageDto> messages, String channelId) {
        // The payload is defined as a list to support batch data delivery at subscribe.
        subscriberRepository.findSubscribers(topic).ifPresent(subscribers -> {
            toJson(messages).ifPresent(json -> {
                subscribers.forEach(subscriber -> send(json, subscriber, ModificationType.ADDED));
            });
        });
    }

    private BisqEasyOpenTradeMessageDto toDto(BisqEasyOpenTradeMessage message) {
        Optional<UserProfileDto> citationAuthorUserProfile = message.getCitation()
                .flatMap(citation -> userProfileService.findUserProfile(citation.getAuthorUserProfileId()))
                .map(DtoMappings.UserProfileMapping::fromBisq2Model);
        return DtoMappings.BisqEasyOpenTradeMessageMapping.fromBisq2Model(message, citationAuthorUserProfile);
    }
}
