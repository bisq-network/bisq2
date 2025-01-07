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

package bisq.http_api.web_socket.domain.trades;

import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.dto.DtoMappings;
import bisq.dto.trade.bisq_easy.protocol.BisqEasyTradeStateDto;
import bisq.http_api.web_socket.domain.BaseWebSocketService;
import bisq.http_api.web_socket.domain.OpenTradeItemsService;
import bisq.http_api.web_socket.subscription.ModificationType;
import bisq.http_api.web_socket.subscription.SubscriberRepository;
import bisq.trade.TradeService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static bisq.http_api.web_socket.subscription.Topic.TRADE_STATE_BY_TRADE_ID;

@Slf4j
public class TradeStateByTradeIdWebSocketService extends BaseWebSocketService {
    private final OpenTradeItemsService openTradeItemsService;
    private final BisqEasyTradeService bisqEasyTradeService;

    private Pin tradesPin;
    private final Map<String, Pin> pinByTradeId = new HashMap<>();

    public TradeStateByTradeIdWebSocketService(ObjectMapper objectMapper,
                                               SubscriberRepository subscriberRepository,
                                               TradeService tradeService,
                                               OpenTradeItemsService openTradeItemsService) {
        super(objectMapper, subscriberRepository, TRADE_STATE_BY_TRADE_ID);
        bisqEasyTradeService = tradeService.getBisqEasyTradeService();

        this.openTradeItemsService = openTradeItemsService;
    }


    @Override
    public CompletableFuture<Boolean> initialize() {
        tradesPin = bisqEasyTradeService.getTrades().addObserver(new CollectionObserver<>() {
            @Override
            public void add(BisqEasyTrade bisqEasyTrade) {
                String tradeId = bisqEasyTrade.getId();
                if (!pinByTradeId.containsKey(tradeId)) {
                    Pin pin = bisqEasyTrade.stateObservable().addObserver(state -> {
                        if (state instanceof BisqEasyTradeState bisqEasyTradeState) {
                            BisqEasyTradeStateDto stateDto = DtoMappings.BisqEasyTradeStateMapping.fromBisq2Model(bisqEasyTradeState);
                            send(new HashMap<>(Map.of(tradeId, stateDto)));
                        }
                    });
                    pinByTradeId.put(tradeId, pin);
                }
            }

            @Override
            public void remove(Object element) {
                if (element instanceof BisqEasyTrade bisqEasyTrade) {
                    String tradeId = bisqEasyTrade.getId();
                    Optional.ofNullable(pinByTradeId.remove(tradeId)).ifPresent(Pin::unbind);
                }
            }

            @Override
            public void clear() {
                pinByTradeId.values().forEach(Pin::unbind);
                pinByTradeId.clear();
            }
        });
        return CompletableFuture.completedFuture(true);
    }


    @Override
    public CompletableFuture<Boolean> shutdown() {
        tradesPin.unbind();
        pinByTradeId.values().forEach(Pin::unbind);
        pinByTradeId.clear();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public Optional<String> getJsonPayload() {
        List<Map<String, BisqEasyTradeStateDto>> maps = bisqEasyTradeService.getTrades().stream()
                .map(bisqEasyTrade -> {
                            String tradeId = bisqEasyTrade.getId();
                            var state = bisqEasyTrade.getState();
                            if (state instanceof BisqEasyTradeState bisqEasyTradeState) {
                                BisqEasyTradeStateDto stateDto = DtoMappings.BisqEasyTradeStateMapping.fromBisq2Model(bisqEasyTradeState);
                                return Map.of(tradeId, stateDto);
                            } else {
                                return null;
                            }
                        }
                ).filter(Objects::nonNull)
                .collect(Collectors.toList());
        return toJson(maps);
    }

    private void send(HashMap<String, BisqEasyTradeStateDto> map) {
        send(Collections.singletonList(map));
    }

    private void send(List<HashMap<String, BisqEasyTradeStateDto>> maps) {
        // The payloadEncoded is defined as a list to support batch data delivery at subscribe.
        toJson(maps).ifPresent(json -> {
            subscriberRepository.findSubscribers(topic)
                    .ifPresent(subscribers -> subscribers
                            .forEach(subscriber -> send(json, subscriber, ModificationType.REPLACE)));
        });
    }
}
