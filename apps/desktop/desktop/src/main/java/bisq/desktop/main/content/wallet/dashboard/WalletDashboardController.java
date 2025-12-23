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

package bisq.desktop.main.content.wallet.dashboard;

import bisq.desktop.main.content.wallet.WalletTxListItem;
import bisq.desktop.navigation.NavigationTarget;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.wallet.WalletService;
import bisq.wallet.vo.Transaction;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.presentation.formatters.AmountFormatter;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Coin;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class WalletDashboardController implements Controller {
    @Getter
    private final WalletDashboardView view;
    private final WalletDashboardModel model;
    private final WalletService walletService;
    private final MarketPriceService marketPriceService;
    private Pin balancePin, transactionsPin, selectedMarketPin, marketPriceByCurrencyMapPin;
    private Subscription balanceAsCoinPin;

    public WalletDashboardController(ServiceProvider serviceProvider) {
        walletService = serviceProvider.getWalletService().orElseThrow();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        model = new WalletDashboardModel();
        view = new WalletDashboardView(model, this);
    }

    @Override
    public void onActivate() {
        balancePin = FxBindings.bind(model.getBalanceAsCoinProperty())
                .to(walletService.getBalance());

        transactionsPin = FxBindings.<Transaction, WalletTxListItem>bind(model.getListItems())
                .map(WalletTxListItem::new)
                .to(walletService.getTransactions());

        balanceAsCoinPin = EasyBind.subscribe(model.getBalanceAsCoinProperty(), balance -> UIThread.run(this::updateCurrencyConverterBalance));

        // TODO: Allow changing market
        selectedMarketPin = marketPriceService.getSelectedMarket().addObserver(selectedMarket -> UIThread.run(this::updateCurrencyConverterBalance));

        marketPriceByCurrencyMapPin = marketPriceService.getMarketPriceByCurrencyMap().addObserver(() -> {
                // TODO: update fiat balance when market price changes
                UIThread.run(() -> {});
        });

        walletService.requestBalance().whenComplete((balance, throwable) -> {
            if (throwable == null) {
                UIThread.run(() -> model.getBalanceAsCoinProperty().set(balance));
            }
        });
        walletService.requestTransactions();
    }

    @Override
    public void onDeactivate() {
        balancePin.unbind();
        transactionsPin.unbind();
        balanceAsCoinPin.unsubscribe();
        selectedMarketPin.unbind();
        marketPriceByCurrencyMapPin.unbind();
    }

    void onSend() {
        Navigation.navigateTo(NavigationTarget.WALLET_SEND);
    }

    void onReceive() {
        Navigation.navigateTo(NavigationTarget.WALLET_RECEIVE);
    }

    private void updateCurrencyConverterBalance() {
        Coin btcBalance = model.getBalanceAsCoinProperty().get();
        if (btcBalance == null) {
            resetCurrencyConverterBalance();
            return;
        }

        Market selectedMarket = marketPriceService.getSelectedMarket().get();
        marketPriceService.findMarketPrice(selectedMarket).ifPresentOrElse(
                marketPrice -> {
                    double value = btcBalance.asDouble() * marketPrice.getPriceQuote().asDouble();
                    String code = marketPrice.getMarket().getQuoteCurrencyCode();
                    Fiat fiat = Fiat.fromFaceValue(value, code);
                    model.getCurrencyConverterCodeProperty().set(code);
                    model.getFormattedCurrencyConverterValueProperty().set(AmountFormatter.formatAmount(fiat, true));
                },
                this::resetCurrencyConverterBalance
        );
    }

    private void resetCurrencyConverterBalance() {
        model.getCurrencyConverterCodeProperty().set("");
        model.getFormattedCurrencyConverterValueProperty().set("");
    }
}
