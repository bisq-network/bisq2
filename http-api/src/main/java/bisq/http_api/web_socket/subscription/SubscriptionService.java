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

package bisq.http_api.web_socket.subscription;


import bisq.bonded_roles.BondedRolesService;
import bisq.chat.ChatService;
import bisq.common.application.Service;
import bisq.http_api.web_socket.domain.BaseWebSocketService;
import bisq.http_api.web_socket.domain.OpenTradeItemsService;
import bisq.http_api.web_socket.domain.chat.trade.TradeChatWebSocketService;
import bisq.http_api.web_socket.domain.market_price.MarketPriceWebSocketService;
import bisq.http_api.web_socket.domain.offers.NumOffersWebSocketService;
import bisq.http_api.web_socket.domain.offers.OffersWebSocketService;
import bisq.http_api.web_socket.domain.trades.TradePropertiesWebSocketService;
import bisq.http_api.web_socket.domain.trades.TradesWebSocketService;
import bisq.http_api.web_socket.util.JsonUtil;
import bisq.trade.TradeService;
import bisq.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.websockets.WebSocket;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class SubscriptionService implements Service {
    private final ObjectMapper objectMapper;
    private final SubscriberRepository subscriberRepository;
    private final MarketPriceWebSocketService marketPriceWebSocketService;
    private final NumOffersWebSocketService numOffersWebSocketService;
    private final OffersWebSocketService offersWebSocketService;
    private final TradesWebSocketService tradesWebSocketService;
    private final TradePropertiesWebSocketService tradePropertiesWebSocketService;
    private final TradeChatWebSocketService tradeChatWebSocketService;

    public SubscriptionService(ObjectMapper objectMapper,
                               BondedRolesService bondedRolesService,
                               ChatService chatService,
                               TradeService tradeService,
                               UserService userService,
                               OpenTradeItemsService openTradeItemsService) {
        this.objectMapper = objectMapper;
        subscriberRepository = new SubscriberRepository();

        marketPriceWebSocketService = new MarketPriceWebSocketService(objectMapper, subscriberRepository, bondedRolesService);
        numOffersWebSocketService = new NumOffersWebSocketService(objectMapper, subscriberRepository, chatService, userService);
        offersWebSocketService = new OffersWebSocketService(objectMapper, subscriberRepository, chatService, userService, bondedRolesService);
        tradesWebSocketService = new TradesWebSocketService(objectMapper, subscriberRepository, openTradeItemsService);
        tradePropertiesWebSocketService = new TradePropertiesWebSocketService(objectMapper, subscriberRepository, tradeService);
        tradeChatWebSocketService = new TradeChatWebSocketService(objectMapper, subscriberRepository, chatService.getBisqEasyOpenTradeChannelService());
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return marketPriceWebSocketService.initialize()
                .thenCompose(e -> numOffersWebSocketService.initialize())
                .thenCompose(e -> offersWebSocketService.initialize())
                .thenCompose(e -> tradesWebSocketService.initialize())
                .thenCompose(e -> tradePropertiesWebSocketService.initialize())
                .thenCompose(e -> tradeChatWebSocketService.initialize());
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return marketPriceWebSocketService.shutdown()
                .thenCompose(e -> numOffersWebSocketService.shutdown())
                .thenCompose(e -> offersWebSocketService.shutdown())
                .thenCompose(e -> tradesWebSocketService.shutdown())
                .thenCompose(e -> tradePropertiesWebSocketService.shutdown())
                .thenCompose(e -> tradeChatWebSocketService.shutdown());
    }

    public void onConnectionClosed(WebSocket webSocket) {
        subscriberRepository.onConnectionClosed(webSocket);
    }

    public boolean canHandle(String json) {
        return JsonUtil.hasExpectedJsonClassName(SubscriptionRequest.class, json);
    }

    public void onMessage(String json, WebSocket webSocket) {
        SubscriptionRequest.fromJson(objectMapper, json)
                .ifPresent(subscriptionRequest ->
                        subscribe(subscriptionRequest, webSocket));
    }

    private void subscribe(SubscriptionRequest request, WebSocket webSocket) {
        log.info("Received subscription request: {}", request);
        subscriberRepository.add(request, webSocket);
        findWebSocketService(request.getTopic())
                .flatMap(BaseWebSocketService::getJsonPayload)
                .flatMap(json -> new SubscriptionResponse(request.getRequestId(), json, null)
                        .toJson(objectMapper))
                .ifPresent(json -> {
                    log.info("Send SubscriptionResponse json: {}", json);
                    webSocket.send(json);
                });
    }

    public void unSubscribe(Topic topic, String subscriberId) {
        subscriberRepository.remove(topic, subscriberId);
    }

    private Optional<BaseWebSocketService> findWebSocketService(Topic topic) {
        switch (topic) {
            case MARKET_PRICE -> {
                return Optional.of(marketPriceWebSocketService);
            }
            case NUM_OFFERS -> {
                return Optional.of(numOffersWebSocketService);
            }
            case OFFERS -> {
                return Optional.of(offersWebSocketService);
            }
            case TRADES -> {
                return Optional.of(tradesWebSocketService);
            }
            case TRADE_PROPERTIES -> {
                return Optional.of(tradePropertiesWebSocketService);
            }
            case TRADE_CHATS -> {
                return Optional.of(tradeChatWebSocketService);
            }
        }
        log.warn("No WebSocketService for topic {} found", topic);
        return Optional.empty();
    }
}
