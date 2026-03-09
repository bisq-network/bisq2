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
import bisq.desktop.common.view.Navigation;
import bisq.desktop.main.content.bisq_easy.open_trades.trade_details.TradeDetailsController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.trade.bisq_easy.protocol.BisqEasyClosedTrade;
import bisq.user.reputation.ReputationService;
import lombok.Getter;

import java.util.Optional;
import java.util.function.Predicate;

public class BisqEasyHistoryController implements Controller {
    @Getter
    private final BisqEasyHistoryView view;
    private final BisqEasyHistoryModel model;
    private final BisqEasyTradeService bisqEasyTradeService;
    private final ReputationService reputationService;
    private final MarketPriceService marketPriceService;
    private Pin closedTradesPin;
    private String searchText = "";

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
                    updatePlaceholderText();
                }
            }

            @Override
            public void onRemoved(Object element) {
                if (element instanceof BisqEasyClosedTrade closedTrade) {
                    model.getBisqEasyTradeHistoryListItems().stream()
                            .filter(item -> item.getTrade().equals(closedTrade.trade()))
                            .findFirst()
                            .ifPresent(item -> model.getBisqEasyTradeHistoryListItems().remove(item));
                    updatePlaceholderText();
                }
            }

            @Override
            public void onCleared() {
                model.getBisqEasyTradeHistoryListItems().clear();
                updatePlaceholderText();
            }
        });
        updatePlaceholderText();
    }

    @Override
    public void onDeactivate() {
        closedTradesPin.unbind();
    }

    void applySearchPredicate(String searchText) {
        this.searchText = searchText == null ? "" : searchText.trim().toLowerCase();
        model.setSearchStringPredicate(createSearchPredicate());
        applyPredicates();
        updatePlaceholderText();
    }

    void onShowTradeDetails(BisqEasyTradeHistoryListItem item) {
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_TRADE_DETAILS,
                new TradeDetailsController.InitData(item.getTrade(), item.getMyUserProfile(), item.getPeerProfile(),
                        item.getTrade().getContract().getMediator()));
    }

    private void applyPredicates() {
        model.getFilteredBisqEasyTradeHistoryListItems().setPredicate(null);
        model.getFilteredBisqEasyTradeHistoryListItems().setPredicate(model.getBisqEasyTradeHistoryListItemsPredicate());
    }

    private Predicate<BisqEasyTradeHistoryListItem> createSearchPredicate() {
        return item -> searchText.isEmpty()
                || item.getMarket().getMarketDisplayName().toLowerCase().contains(searchText)
                || item.getMarket().getBaseCurrencyCode().toLowerCase().contains(searchText)
                || item.getMarket().getQuoteCurrencyCode().toLowerCase().contains(searchText)
                || item.getTradeId().toLowerCase().contains(searchText)
                || item.getDateString().toLowerCase().contains(searchText)
                || item.getBaseAmountAsString().toLowerCase().contains(searchText)
                || item.getQuoteAmountAsString().toLowerCase().contains(searchText)
                || item.getPaymentAsString().toLowerCase().contains(searchText)
                || item.getMyRole().toLowerCase().contains(searchText)
                || item.getMyUserProfile().getNickName().toLowerCase().contains(searchText)
                || item.getPeerProfile().getNickName().toLowerCase().contains(searchText);
    }

    private void updatePlaceholderText() {
        if (model.getBisqEasyTradeHistoryListItems().isEmpty()) {
            model.getPlaceholderText().set(Res.get("bisqEasy.history.noTrades"));
        } else if (model.getFilteredBisqEasyTradeHistoryListItems().isEmpty()) {
            model.getPlaceholderText().set(Res.get("bisqEasy.history.noMatchingTrades"));
        }
    }

    private Optional<BisqEasyTradeHistoryListItem> findListItem(String tradeId) {
        return model.getBisqEasyTradeHistoryListItems().stream()
                .filter(item -> item.getTrade().getId().equals(tradeId))
                .findAny();
    }
}
