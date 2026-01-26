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
import bisq.http_api.push_notification.PushNotificationService;
import bisq.http_api.web_socket.domain.BaseWebSocketService;
import bisq.http_api.web_socket.domain.OpenTradeItemsService;
import bisq.http_api.web_socket.subscription.ModificationType;
import bisq.http_api.web_socket.subscription.SubscriberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static bisq.http_api.web_socket.subscription.Topic.TRADES;

@Slf4j
public class TradesWebSocketService extends BaseWebSocketService {
    private final OpenTradeItemsService openTradeItemsService;
    private final Optional<PushNotificationService> pushNotificationService;
    @Nullable
    private Pin tradesPin;

    public TradesWebSocketService(ObjectMapper objectMapper,
                                  SubscriberRepository subscriberRepository,
                                  OpenTradeItemsService openTradeItemsService,
                                  Optional<PushNotificationService> pushNotificationService) {
        super(objectMapper, subscriberRepository, TRADES);

        this.openTradeItemsService = openTradeItemsService;
        this.pushNotificationService = pushNotificationService;
    }


    @Override
    public CompletableFuture<Boolean> initialize() {
        tradesPin = openTradeItemsService.getItems().addObserver(new CollectionObserver<>() {
            @Override
            public void add(TradeItemPresentationDto item) {
                send(item, ModificationType.ADDED);
                // Note: Push notifications for new trades are handled by TradePropertiesWebSocketService
                // which observes trade state changes. This avoids duplicate notifications.
            }

            @Override
            public void remove(Object element) {
                if (element instanceof TradeItemPresentationDto item) {
                    send(item, ModificationType.REMOVED);
                    // Clean up notification records for this trade
                    pushNotificationService.ifPresent(service -> {
                        String tradeId = item.channel().tradeId();
                        service.removeNotificationsForTrade(tradeId);
                        log.debug("Cleaned up notification records for removed trade {}", tradeId);
                    });
                }
            }

            @Override
            public void clear() {
                // Note: Observers are notified BEFORE the collection is cleared,
                // so we can safely access the items here
                List<TradeItemPresentationDto> items = openTradeItemsService.getItems().getList();

                send(items, ModificationType.REMOVED);

                // Clean up notification records for all trades being cleared
                pushNotificationService.ifPresent(service -> {
                    items.forEach(item -> {
                        String tradeId = item.channel().tradeId();
                        service.removeNotificationsForTrade(tradeId);
                    });
                    log.debug("Cleaned up notification records for {} cleared trades", items.size());
                });
            }
        });
        return CompletableFuture.completedFuture(true);
    }


    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (tradesPin != null) {
            tradesPin.unbind();
            tradesPin = null;
        }
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
