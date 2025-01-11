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
import bisq.dto.presentation.open_trades.TradeItemPresentationDto;
import bisq.http_api.web_socket.domain.OpenTradeItemsService;
import bisq.http_api.web_socket.domain.BaseWebSocketService;
import bisq.http_api.web_socket.subscription.ModificationType;
import bisq.http_api.web_socket.subscription.SubscriberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static bisq.http_api.web_socket.subscription.Topic.TRADES;

@Slf4j
public class TradesWebSocketService extends BaseWebSocketService {
    private final OpenTradeItemsService openTradeItemsService;

    private Pin tradesPin;

    public TradesWebSocketService(ObjectMapper objectMapper,
                                  SubscriberRepository subscriberRepository,
                                  OpenTradeItemsService openTradeItemsService) {
        super(objectMapper, subscriberRepository, TRADES);

        this.openTradeItemsService = openTradeItemsService;
    }


    @Override
    public CompletableFuture<Boolean> initialize() {
        tradesPin = openTradeItemsService.getItems().addObserver(new CollectionObserver<>() {
            @Override
            public void add(TradeItemPresentationDto item) {
                send(item, ModificationType.ADDED);
            }

            @Override
            public void remove(Object element) {
                if (element instanceof TradeItemPresentationDto item) {
                    send(item, ModificationType.REMOVED);
                }
            }

            @Override
            public void clear() {
                send(openTradeItemsService.getItems().getList(), ModificationType.REMOVED);
            }
        });
        return CompletableFuture.completedFuture(true);
    }


    @Override
    public CompletableFuture<Boolean> shutdown() {
        tradesPin.unbind();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public Optional<String> getJsonPayload() {
        return toJson(openTradeItemsService.getItems().getList());
    }

    private void send(TradeItemPresentationDto item, ModificationType modificationType) {
        send(Collections.singletonList(item), modificationType);
    }

    private void send(List<TradeItemPresentationDto> items, ModificationType modificationType) {
        // The payload is defined as a list to support batch data delivery at subscribe.
        toJson(items).ifPresent(json -> {
            subscriberRepository.findSubscribers(topic)
                    .ifPresent(subscribers -> subscribers
                            .forEach(subscriber -> send(json, subscriber, modificationType)));
        });
    }
}
