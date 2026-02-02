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

package bisq.desktop.main.content.bisq_easy.history;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.trade.bisq_easy.protocol.BisqEasyClosedTrade;
import bisq.user.reputation.ReputationService;
import lombok.Getter;

import java.util.Optional;

public class BisqEasyHistoryController implements Controller {
    @Getter
    private final BisqEasyHistoryView view;
    private final BisqEasyHistoryModel model;
    private final BisqEasyTradeService bisqEasyTradeService;
    private final ReputationService reputationService;
    private final MarketPriceService marketPriceService;
    private Pin closedTradesPin;

    public BisqEasyHistoryController(ServiceProvider serviceProvider) {
        model = new BisqEasyHistoryModel();
        view = new BisqEasyHistoryView(model, this);
        bisqEasyTradeService = serviceProvider.getTradeService().getBisqEasyTradeService();
        reputationService = serviceProvider.getUserService().getReputationService();
        marketPriceService = serviceProvider.getBisqEasyService().getMarketPriceService();
    }

    @Override
    public void onActivate() {
        closedTradesPin = bisqEasyTradeService.getClosedTrades().addObserver(new CollectionObserver<>() {
            @Override
            public void onAdded(BisqEasyClosedTrade closedTrade) {
                if (findListItem(closedTrade.trade().getId()).isEmpty()) {
                    model.getBisqEasyTradeHistoryListItems().add(
                            new BisqEasyTradeHistoryListItem(closedTrade, reputationService, marketPriceService));
                }
            }

            @Override
            public void onRemoved(Object element) {
                if (element instanceof BisqEasyTrade trade) {
                    model.getBisqEasyTradeHistoryListItems().stream()
                            .filter(item -> item.getTrade().equals(trade))
                            .findFirst()
                            .ifPresent(item -> model.getBisqEasyTradeHistoryListItems().remove(item));
                }
            }

            @Override
            public void onCleared() {
                model.getBisqEasyTradeHistoryListItems().clear();
            }
        });
    }

    @Override
    public void onDeactivate() {
        closedTradesPin.unbind();
    }

    void applySearchPredicate(String searchText) {
        String string = searchText == null ? "" : searchText.toLowerCase();
//        model.setSearchStringPredicate(item ->
//                StringUtils.isEmpty(string)
//                        || item.getMarket().getMarketDisplayName().toLowerCase().contains(string)
//                        || item.getMakerUserProfile().getUserName().toLowerCase().contains(string)
//                        || item.getOfferId().toLowerCase().contains(string)
//                        || item.getOfferDate().toLowerCase().contains(string)
//                        || item.getBaseAmountAsString().contains(string)
//                        || item.getQuoteAmountAsString().contains(string)
//                        || item.getPrice().contains(string)
//                        || item.getPaymentMethodsAsString().toLowerCase().contains(string));
        applyPredicates();
    }

    private void applyPredicates() {
        model.getFilteredBisqEasyTradeHistoryListItems().setPredicate(null);
        model.getFilteredBisqEasyTradeHistoryListItems().setPredicate(model.getBisqEasyTradeHistoryListItemsPredicate());
    }

    private Optional<BisqEasyTradeHistoryListItem> findListItem(String tradeId) {
        return model.getBisqEasyTradeHistoryListItems().stream()
                .filter(item -> item.getTrade().getId().equals(tradeId))
                .findAny();
    }
}
