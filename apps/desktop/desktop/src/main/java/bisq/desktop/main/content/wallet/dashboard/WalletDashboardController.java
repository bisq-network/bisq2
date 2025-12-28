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

import bisq.common.market.MarketRepository;
import bisq.desktop.main.content.wallet.WalletTxListItem;
import bisq.desktop.navigation.NavigationTarget;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.settings.SettingsService;
import bisq.wallet.WalletService;
import bisq.wallet.vo.Transaction;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.monetary.Coin;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class WalletDashboardController implements Controller {
    private static final List<Market> AVAILABLE_MARKETS_FOR_CURRENCY_CONVERSION;
    static {
        List<Market> list = new ArrayList<>(MarketRepository.getAllFiatMarkets());
        list.add(MarketRepository.getXmrBtcMarket());
        AVAILABLE_MARKETS_FOR_CURRENCY_CONVERSION = list;
    }

    @Getter
    private final WalletDashboardView view;
    private final WalletDashboardModel model;
    private final WalletService walletService;
    private final MarketPriceService marketPriceService;
    private final SettingsService settingsService;
    private Pin balancePin, transactionsPin, marketPriceByCurrencyMapPin, selectedMarketPin;
    private Subscription balanceAsCoinPin, selectedMarketItemPin;

    public WalletDashboardController(ServiceProvider serviceProvider) {
        walletService = serviceProvider.getWalletService().orElseThrow();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        settingsService = serviceProvider.getSettingsService();

        model = new WalletDashboardModel();
        view = new WalletDashboardView(model, this);
    }

    @Override
    public void onActivate() {
        model.getMarketItems().setAll(AVAILABLE_MARKETS_FOR_CURRENCY_CONVERSION.stream()
                .map(market -> new MarketItem(market, marketPriceService))
                .collect(Collectors.toList()));

        balancePin = FxBindings.bind(model.getBalanceAsCoinProperty())
                .to(walletService.getBalance());

        transactionsPin = FxBindings.<Transaction, WalletTxListItem>bind(model.getWalletTxListItems())
                .map(WalletTxListItem::new)
                .to(walletService.getTransactions());

        balanceAsCoinPin = EasyBind.subscribe(model.getBalanceAsCoinProperty(), balance ->
                UIThread.run(() -> {
                    updateCurrencyConverterBalance();
                    updateMarketItems();
                }));

        selectedMarketItemPin = EasyBind.subscribe(model.getSelectedMarketItem(), selectedMarketItem ->
                UIThread.run(() -> {
                    updateSelectedMarket(selectedMarketItem);
                    updateCurrencyConverterBalance();
                }));

        marketPriceByCurrencyMapPin = marketPriceService.getMarketPriceByCurrencyMap().addObserver(() ->
                UIThread.run(() -> {
                    model.setMarketPricePredicate(item -> marketPriceService.getMarketPriceByCurrencyMap().isEmpty() ||
                            marketPriceService.getMarketPriceByCurrencyMap().containsKey(item.getMarket()));
                    updateFilteredMarketListItems();
                    updateMarketItems();
                }));

        selectedMarketPin = FxBindings.bindBiDir(model.getSelectedMarket())
                .to(settingsService.getSelectedWalletMarket(), settingsService::setSelectedWalletMarket);

        walletService.requestBalance().whenComplete((balance, throwable) -> {
                    if (throwable == null) {
                        UIThread.run(() -> model.getBalanceAsCoinProperty().set(balance));
                    }
                });
        walletService.requestTransactions();

        setSelectedMarket();
    }

    @Override
    public void onDeactivate() {
        balancePin.unbind();
        transactionsPin.unbind();
        balanceAsCoinPin.unsubscribe();
        selectedMarketItemPin.unsubscribe();
        marketPriceByCurrencyMapPin.unbind();
        selectedMarketPin.unbind();
    }

    void onSend() {
        Navigation.navigateTo(NavigationTarget.WALLET_SEND);
    }

    void onReceive() {
        Navigation.navigateTo(NavigationTarget.WALLET_RECEIVE);
    }

    void onSelectMarket(MarketItem marketItem) {
        model.getSelectedMarketItem().set(marketItem);
    }

    private void updateCurrencyConverterBalance() {
        Coin btcBalance = model.getBalanceAsCoinProperty().get();
        if (btcBalance == null) {
            resetCurrencyConverterBalance();
            return;
        }

        MarketItem selectedMarket = model.getSelectedMarketItem().get();
        if (selectedMarket == null) {
            resetCurrencyConverterBalance();
            return;
        }

        marketPriceService.findMarketPrice(selectedMarket.getMarket()).ifPresentOrElse(
                marketPrice -> {
                    String code = WalletMarketUtil.getMarketCode(marketPrice.getMarket());
                    model.getCurrencyConverterCodeProperty().set(code);
                    String amount = WalletMarketUtil.getFormattedConvertedAmount(btcBalance, marketPrice, false);
                    model.getFormattedCurrencyConverterAmountProperty().set(amount);
                },
                this::resetCurrencyConverterBalance
        );
    }

    private void resetCurrencyConverterBalance() {
        model.getCurrencyConverterCodeProperty().set("");
        model.getFormattedCurrencyConverterAmountProperty().set("");
    }

    private void updateMarketItems() {
        Coin btcBalance = model.getBalanceAsCoinProperty().get();
        model.getMarketItems().forEach(availableMarket -> availableMarket.updateFormattedAmount(btcBalance));
    }

    private void updateFilteredMarketListItems() {
        model.getFilteredMarketListItems().setPredicate(null);
        model.getFilteredMarketListItems().setPredicate(model.getMarketListItemsPredicate());
    }

    private void setSelectedMarket() {
        Market selectedMarket = model.getSelectedMarket().get();
        if (selectedMarket != null) {
            model.getMarketItems().stream()
                    .filter(item -> item.getMarket().equals(selectedMarket))
                    .findAny()
                    .ifPresent(item -> model.getSelectedMarketItem().set(item));
        }
    }

    private void updateSelectedMarket(MarketItem marketItem) {
        if (marketItem != null) {
            model.getSelectedMarket().set(marketItem.getMarket());
        }
    }
}
