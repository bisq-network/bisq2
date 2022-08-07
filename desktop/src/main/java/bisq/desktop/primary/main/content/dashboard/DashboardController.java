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

package bisq.desktop.primary.main.content.dashboard;

import bisq.application.DefaultApplicationService;
import bisq.common.currency.Market;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.oracle.marketprice.MarketPrice;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.presentation.formatters.QuoteFormatter;
import lombok.Getter;

public class DashboardController implements Controller {
    @Getter
    private final DashboardView view;
    private final MarketPriceService marketPriceService;
    private final DashboardModel model;
    private Pin selectedMarketPin, marketPriceUpdateFlagPin;

    public DashboardController(DefaultApplicationService applicationService) {
        marketPriceService = applicationService.getOracleService().getMarketPriceService();

        model = new DashboardModel();
        view = new DashboardView(model, this);
    }

    @Override
    public void onActivate() {
        selectedMarketPin = marketPriceService.getSelectedMarket().addObserver(selectedMarket -> updateMarketPrice());
        marketPriceUpdateFlagPin = marketPriceService.getMarketPriceUpdateFlag().addObserver(__ -> updateMarketPrice());
    }

    @Override
    public void onDeactivate() {
        selectedMarketPin.unbind();
        marketPriceUpdateFlagPin.unbind();
    }

    public void onLearn() {
        Navigation.navigateTo(NavigationTarget.ACADEMY_OVERVIEW);
    }

    public void onOpenTradeOverview() {
        Navigation.navigateTo(NavigationTarget.TRADE_OVERVIEW);
    }

    public void onOpenBisqEasy() {
        Navigation.navigateTo(NavigationTarget.BISQ_EASY);
    }

    private void updateMarketPrice() {
        Market selectedMarket = marketPriceService.getSelectedMarket().get();
        if (selectedMarket != null) {
            UIThread.run(() -> {
                MarketPrice marketPrice = marketPriceService.getMarketPriceByCurrencyMap().get(selectedMarket);
                model.getMarketPrice().set(QuoteFormatter.format(marketPrice.getQuote(), true));
                model.getMarketCode().set(marketPrice.getMarket().getMarketCodes());

            });
        }
    }
}
