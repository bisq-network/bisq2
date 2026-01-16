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

package bisq.api.web_socket;

import bisq.api.ApiConfig;
import bisq.api.web_socket.domain.OpenTradeItemsService;
import bisq.api.web_socket.rest_api_proxy.WebSocketRestApiService;
import bisq.api.web_socket.subscription.SubscriptionService;
import bisq.bisq_easy.BisqEasyService;
import bisq.bonded_roles.BondedRolesService;
import bisq.chat.ChatService;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.observable.collection.ObservableSet;
import bisq.trade.TradeService;
import bisq.user.UserService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class WebSocketService implements Service {
    public enum State {
        NEW,
        STARTING,
        RUNNING,
        STOPPING,
        TERMINATED
    }

    @Getter
    private final ApiConfig apiConfig;
    @Getter
    private final WebSocketConnectionHandler webSocketConnectionHandler;
    @Getter
    private final SubscriptionService subscriptionService;
    @Getter
    private final WebSocketRestApiService webSocketRestApiService;

    private final Observable<State> state = new Observable<>(State.NEW);

    public WebSocketService(ApiConfig apiConfig,
                            BondedRolesService bondedRolesService,
                            ChatService chatService,
                            TradeService tradeService,
                            UserService userService,
                            BisqEasyService bisqEasyService,
                            OpenTradeItemsService openTradeItemsService) {
        this.apiConfig = apiConfig;
        subscriptionService = new SubscriptionService(bondedRolesService,
                chatService,
                tradeService,
                userService,
                bisqEasyService,
                openTradeItemsService);
        webSocketRestApiService = new WebSocketRestApiService(apiConfig, apiConfig.getRestServerUrl());
        webSocketConnectionHandler = new WebSocketConnectionHandler(subscriptionService, webSocketRestApiService);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        setState(State.STARTING);
        return webSocketConnectionHandler.initialize()
                .thenCompose(r -> webSocketRestApiService.initialize())
                .thenCompose(r -> subscriptionService.initialize())
                .thenApply(r -> {
                    setState(State.RUNNING);
                    return true;
                });
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return subscriptionService.shutdown()
                .thenCompose(r -> webSocketRestApiService.shutdown())
                .thenCompose(r -> webSocketConnectionHandler.shutdown())
                .thenApply(r -> {
                    setState(State.TERMINATED);
                    return true;
                });
    }

    public ObservableSet<WebsocketClient1> getWebsocketClients() {
        return webSocketConnectionHandler.getWebsocketClients();
    }

    public ReadOnlyObservable<State> getState() {
        return state;
    }

    private void setState(State value) {
        log.info("New state: {}", value);
        state.set(value);
    }
}
