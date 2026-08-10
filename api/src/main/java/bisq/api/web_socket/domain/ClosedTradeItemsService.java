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

package bisq.api.web_socket.domain;

import bisq.api.dto.presentation.closed_trades.ClosedTradeIndexedItem;
import bisq.api.dto.presentation.closed_trades.ClosedTradeListItemDtoFactory;
import bisq.common.application.Service;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableArray;
import bisq.trade.TradeService;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.trade.bisq_easy.protocol.BisqEasyClosedTrade;
import bisq.user.UserService;
import bisq.user.reputation.ReputationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class ClosedTradeItemsService implements Service {
    private final BisqEasyTradeService bisqEasyTradeService;
    private final ReputationService reputationService;

    @Getter
    private final ObservableArray<ClosedTradeIndexedItem> items = new ObservableArray<>();
    @Nullable
    private Pin closedTradesPin;

    public ClosedTradeItemsService(TradeService tradeService, UserService userService) {
        bisqEasyTradeService = tradeService.getBisqEasyTradeService();
        reputationService = userService.getReputationService();
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        closedTradesPin = bisqEasyTradeService.getClosedTrades().addObserver(new CollectionObserver<>() {
            @Override
            public void onAdded(BisqEasyClosedTrade closedTrade) {
                if (findListItem(closedTrade.trade().getId()).isEmpty()) {
                    items.add(ClosedTradeListItemDtoFactory.create(closedTrade, reputationService));
                }
            }

            @Override
            public void onRemoved(Object element) {
                if (element instanceof BisqEasyClosedTrade closedTrade) {
                    findListItem(closedTrade.trade().getId()).ifPresent(items::remove);
                }
            }

            @Override
            public void onCleared() {
                items.clear();
            }
        });
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        if (closedTradesPin != null) {
            closedTradesPin.unbind();
            closedTradesPin = null;
        }
        items.clear();
        return CompletableFuture.completedFuture(true);
    }

    private Optional<ClosedTradeIndexedItem> findListItem(String tradeId) {
        return items.stream()
                .filter(item -> item.dto().trade().id().equals(tradeId))
                .findAny();
    }
}
