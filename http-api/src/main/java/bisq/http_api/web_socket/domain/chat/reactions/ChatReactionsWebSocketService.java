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

package bisq.http_api.web_socket.domain.chat.reactions;

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.chat.reactions.BisqEasyOpenTradeMessageReaction;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.dto.DtoMappings;
import bisq.dto.chat.reactions.BisqEasyOpenTradeMessageReactionDto;
import bisq.http_api.web_socket.domain.BaseWebSocketService;
import bisq.http_api.web_socket.subscription.ModificationType;
import bisq.http_api.web_socket.subscription.SubscriberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bisq.http_api.web_socket.subscription.Topic.CHAT_REACTIONS;

@Slf4j
public class ChatReactionsWebSocketService extends BaseWebSocketService {
    private final BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService;

    private Pin channelsPin;
    private final Map<String, Pin> chatMessagesPinsByChannelId = new ConcurrentHashMap<>();
    private final Map<String, Pin> chatMessageReactionsPinsByMessageId = new ConcurrentHashMap<>();

    public ChatReactionsWebSocketService(ObjectMapper objectMapper,
                                         SubscriberRepository subscriberRepository,
                                         BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService) {
        super(objectMapper, subscriberRepository, CHAT_REACTIONS);

        this.bisqEasyOpenTradeChannelService = bisqEasyOpenTradeChannelService;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        channelsPin = bisqEasyOpenTradeChannelService.getChannels().addObserver(new CollectionObserver<>() {
            @Override
            public void add(BisqEasyOpenTradeChannel channel) {
                String channelId = channel.getId();
                // Atomic operation
                chatMessagesPinsByChannelId.compute(channelId, (key, oldPin) -> {
                    if (oldPin != null) {
                        oldPin.unbind();
                    }

                    return channel.getChatMessages().addObserver(new CollectionObserver<>() {
                        @Override
                        public void add(BisqEasyOpenTradeMessage message) {
                            String messageId = message.getId();

                            chatMessageReactionsPinsByMessageId.compute(messageId, (key, oldReactionPin) -> {
                                if (oldReactionPin != null) {
                                    oldReactionPin.unbind();
                                }

                                return message.getChatMessageReactions().addObserver(new CollectionObserver<>() {
                                    @Override
                                    public void add(BisqEasyOpenTradeMessageReaction reaction) {
                                        handleReaction(reaction, ModificationType.ADDED);
                                    }

                                    @Override
                                    public void remove(Object element) {
                                        if (element instanceof BisqEasyOpenTradeMessageReaction reaction) {
                                            handleReaction(reaction, ModificationType.REMOVED);
                                        }
                                    }

                                    @Override
                                    public void clear() {
                                        throw new UnsupportedOperationException("Clear method is not supported for chatMessageReactions.");
                                    }
                                });
                            });
                        }

                        @Override
                        public void remove(Object element) {
                            // Messages cannot be removed
                        }

                        @Override
                        public void clear() {
                            // Messages cannot be cleared
                        }
                    });
                });
            }

            @Override
            public void remove(Object element) {
                if (element instanceof BisqEasyOpenTradeChannel channel) {
                    String channelId = channel.getId();
                    // Atomic operation
                    chatMessagesPinsByChannelId.computeIfPresent(channelId, (key, pin) -> {
                        pin.unbind();
                        return null;  // returning null removes the key
                    });
                }
            }

            @Override
            public void clear() {
                new ArrayList<>(chatMessagesPinsByChannelId.values()).forEach(Pin::unbind);
            }
        });
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (channelsPin != null) {
            channelsPin.unbind();
        }
        new ArrayList<>(chatMessagesPinsByChannelId.values()).forEach(Pin::unbind);
        new ArrayList<>(chatMessageReactionsPinsByMessageId.values()).forEach(Pin::unbind);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public Optional<String> getJsonPayload() {
        return getJsonPayload(bisqEasyOpenTradeChannelService.getChannels().stream());
    }

    private Optional<String> getJsonPayload(Stream<BisqEasyOpenTradeChannel> channels) {
        ArrayList<BisqEasyOpenTradeMessageReactionDto> payload = channels
                .flatMap(channel ->
                        channel.getChatMessages().stream()
                                .flatMap(message -> message.getChatMessageReactions().stream()
                                        .filter(reaction -> !reaction.isRemoved())
                                        .map(reaction -> {
                                            try {
                                                return toDto(reaction);
                                            } catch (Exception e) {
                                                log.error("Failed to create BisqEasyOpenTradeMessageReactionDto", e);
                                                return null;
                                            }
                                        })
                                        .filter(Objects::nonNull)))
                .collect(Collectors.toCollection(ArrayList::new));
        return toJson(payload);
    }

    private void handleReaction(BisqEasyOpenTradeMessageReaction reaction, ModificationType modificationType) {
        BisqEasyOpenTradeMessageReactionDto dto = toDto(reaction);
        handleReaction(Collections.singletonList(dto), modificationType);
    }

    private void handleReaction(List<BisqEasyOpenTradeMessageReactionDto> reactions,
                                ModificationType modificationType) {
        // The payload is defined as a list to support batch data delivery at subscribe.
        subscriberRepository.findSubscribers(topic).ifPresent(subscribers -> {
            toJson(reactions).ifPresent(json -> {
                subscribers.forEach(subscriber -> send(json, subscriber, modificationType));
            });
        });
    }

    private BisqEasyOpenTradeMessageReactionDto toDto(BisqEasyOpenTradeMessageReaction reaction) {
        return DtoMappings.BisqEasyOpenTradeMessageReactionMapping.fromBisq2Model(reaction);
    }
}
