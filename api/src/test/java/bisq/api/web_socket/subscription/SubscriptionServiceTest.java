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

package bisq.api.web_socket.subscription;

import bisq.api.web_socket.domain.BaseWebSocketService;
import bisq.api.web_socket.domain.market_price.MarketPriceWebSocketService;
import bisq.api.web_socket.domain.OpenTradeItemsService;
import bisq.bisq_easy.BisqEasyService;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertNotificationsService;
import bisq.chat.ChatService;
import bisq.common.observable.collection.ObservableSet;
import bisq.trade.TradeService;
import bisq.user.UserService;
import org.glassfish.grizzly.impl.ReadyFutureImpl;
import org.glassfish.grizzly.websockets.WebSocket;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

class SubscriptionServiceTest {

    @Test
    void subscribeRemovesSubscriberWhenSendingInitialPayloadFails() throws Exception {
        AlertNotificationsService alertNotificationsService = mock(AlertNotificationsService.class);
        when(alertNotificationsService.getUnconsumedAlerts()).thenReturn(new ObservableSet<>());
        when(alertNotificationsService.getUnconsumedAlertsByAppType(AppType.MOBILE_CLIENT)).thenReturn(Stream.empty());

        SubscriptionService service = new SubscriptionService(
                mock(BondedRolesService.class, RETURNS_DEEP_STUBS),
                alertNotificationsService,
                mock(ChatService.class, RETURNS_DEEP_STUBS),
                mock(TradeService.class, RETURNS_DEEP_STUBS),
                mock(UserService.class, RETURNS_DEEP_STUBS),
                mock(BisqEasyService.class, RETURNS_DEEP_STUBS),
                mock(OpenTradeItemsService.class, RETURNS_DEEP_STUBS)
        );

        WebSocket webSocket = mock(WebSocket.class);
        doThrow(new RuntimeException("send failed"))
            .doReturn(null)
            .when(webSocket)
            .send(anyString());

        String requestJson = "{\"type\":\"SubscriptionRequest\",\"requestId\":\"request-1\",\"topic\":\"ALERT_NOTIFICATIONS\"}";

        service.onMessage(requestJson, webSocket);

        assertThat(getSubscriberRepository(service).findSubscribers(Topic.ALERT_NOTIFICATIONS)).isEmpty();
    }

    @Test
    void subscribeDeliversLiveEventEmittedDuringInitialSnapshot() throws Exception {
        SubscriptionService service = new SubscriptionService(
                mock(BondedRolesService.class, RETURNS_DEEP_STUBS),
                mock(AlertNotificationsService.class, RETURNS_DEEP_STUBS),
                mock(ChatService.class, RETURNS_DEEP_STUBS),
                mock(TradeService.class, RETURNS_DEEP_STUBS),
                mock(UserService.class, RETURNS_DEEP_STUBS),
                mock(BisqEasyService.class, RETURNS_DEEP_STUBS),
                mock(OpenTradeItemsService.class, RETURNS_DEEP_STUBS)
        );
        SubscriberRepository subscriberRepository = getSubscriberRepository(service);
        replaceWebSocketService(service, "marketPriceWebSocketService", new TestMarketPriceWebSocketService(subscriberRepository));

        List<String> sentMessages = Collections.synchronizedList(new ArrayList<>());
        WebSocket webSocket = mock(WebSocket.class, RETURNS_DEEP_STUBS);
        doAnswer(invocation -> {
            sentMessages.add(invocation.getArgument(0));
            return ReadyFutureImpl.create(null);
        }).when(webSocket).send(anyString());

        String requestJson = "{\"type\":\"SubscriptionRequest\",\"requestId\":\"request-1\",\"topic\":\"MARKET_PRICE\"}";

        service.onMessage(requestJson, webSocket);

        verify(webSocket, timeout(1000).times(2)).send(anyString());
        assertThat(sentMessages)
            .anySatisfy(message -> assertThat(message)
                .contains("\"type\":\"SubscriptionResponse\"")
                .contains("snapshot"));
        assertThat(sentMessages)
            .anySatisfy(message -> assertThat(message)
                .contains("\"type\":\"WebSocketEvent\"")
                .contains("live-event"));
    }

    private SubscriberRepository getSubscriberRepository(SubscriptionService service) throws NoSuchFieldException, IllegalAccessException {
        Field subscriberRepositoryField = SubscriptionService.class.getDeclaredField("subscriberRepository");
        subscriberRepositoryField.setAccessible(true);
        return (SubscriberRepository) subscriberRepositoryField.get(service);
    }

    private void replaceWebSocketService(SubscriptionService service,
                                         String fieldName,
                                         Object replacement) throws Exception {
        Field field = SubscriptionService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, replacement);
    }

    private static final class TestMarketPriceWebSocketService extends MarketPriceWebSocketService {
        private TestMarketPriceWebSocketService(SubscriberRepository subscriberRepository) {
            super(subscriberRepository, mock(BondedRolesService.class, RETURNS_DEEP_STUBS));
        }

        @Override
        public Optional<String> getJsonPayload() {
            return Optional.of("\"snapshot\"");
        }

        @Override
        public Optional<String> getJsonPayload(SubscriptionRequest request) {
            send(Optional.of("\"live-event\""), topic, ModificationType.REPLACE);
            return getJsonPayload();
        }
    }
}