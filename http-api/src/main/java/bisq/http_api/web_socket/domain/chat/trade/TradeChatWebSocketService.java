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
import bisq.http_api.web_socket.domain.BaseWebSocketService;
import bisq.http_api.web_socket.subscription.ModificationType;
import bisq.http_api.web_socket.subscription.SubscriberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bisq.http_api.web_socket.subscription.Topic.TRADE_CHATS;

@Slf4j
public class TradeChatWebSocketService extends BaseWebSocketService {
    private final BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService;

    private Pin channelsPin;
    private final Map<String, Pin> messagesByChannelIdPins = new HashMap<>();

    public TradeChatWebSocketService(ObjectMapper objectMapper,
                                     SubscriberRepository subscriberRepository,
                                     BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService) {
        super(objectMapper, subscriberRepository, TRADE_CHATS);

        this.bisqEasyOpenTradeChannelService = bisqEasyOpenTradeChannelService;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        channelsPin = bisqEasyOpenTradeChannelService.getChannels().addObserver(new CollectionObserver<>() {
            @Override
            public void add(BisqEasyOpenTradeChannel channel) {
                String channelId = channel.getId();
                if (messagesByChannelIdPins.containsKey(channelId)) {
                    messagesByChannelIdPins.get(channelId).unbind();
                }
                Pin pin = channel.getChatMessages().addObserver(new CollectionObserver<>() {
                    @Override
                    public void add(BisqEasyOpenTradeMessage message) {
                        send(message, channelId);
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
                messagesByChannelIdPins.put(channelId, pin);
            }

            @Override
            public void remove(Object element) {
                if (element instanceof BisqEasyOpenTradeChannel channel) {
                    String channelId = channel.getId();
                    if (messagesByChannelIdPins.containsKey(channelId)) {
                        messagesByChannelIdPins.get(channelId).unbind();
                        messagesByChannelIdPins.remove(channelId);
                    }
                }
            }

            @Override
            public void clear() {
                messagesByChannelIdPins.values().forEach(Pin::unbind);
            }
        });
        return CompletableFuture.completedFuture(true);
    }


    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (channelsPin != null) {
            channelsPin.unbind();
        }
        messagesByChannelIdPins.values().forEach(Pin::unbind);
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
                                        return DtoMappings.BisqEasyOpenTradeMessageMapping.fromBisq2Model(message);
                                    } catch (Exception e) {
                                        log.error("Failed to create BisqEasyOpenTradeMessageDto", e);
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull))
                .collect(Collectors.toCollection(ArrayList::new));
        return toJson(payload);
    }

    private void send(BisqEasyOpenTradeMessage message, String channelId) {
        BisqEasyOpenTradeMessageDto dto = DtoMappings.BisqEasyOpenTradeMessageMapping.fromBisq2Model(message);
        send(Collections.singletonList(dto), channelId);
    }

    private void send(List<BisqEasyOpenTradeMessageDto> dtos, String channelId) {
        // The payload is defined as a list to support batch data delivery at subscribe.
        subscriberRepository.findSubscribers(topic).ifPresent(subscribers -> {
            toJson(dtos).ifPresent(json -> {
                subscribers.forEach(subscriber -> send(json, subscriber, ModificationType.ADDED));
            });
        });
    }
}
