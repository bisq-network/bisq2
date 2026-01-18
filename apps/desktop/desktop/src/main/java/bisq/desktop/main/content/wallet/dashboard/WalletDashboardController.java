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
import bisq.common.util.StringUtils;
import bisq.desktop.main.content.wallet.WalletTxListItem;
import bisq.desktop.navigation.NavigationTarget;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.i18n.Res;
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

@Slf4j
public class WalletDashboardController implements Controller {
    private static final List<Market> AVAILABLE_FIAT_MARKETS_FOR_CURRENCY_CONVERSION =
            new ArrayList<>(MarketRepository.getAllFiatMarkets());
    private static final List<Market> AVAILABLE_CRYPTO_MARKETS_FOR_CURRENCY_CONVERSION =
            new ArrayList<>(List.of(MarketRepository.getXmrBtcMarket()));

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
        addCurrencyConverterListItems();

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
                    model.setMarketPricePredicate(item -> {
                        if (item instanceof MarketItem marketItem) {
                            return marketPriceService.getMarketPriceByCurrencyMap().isEmpty()
                                    || marketPriceService.getMarketPriceByCurrencyMap().containsKey(marketItem.getMarket());
                        } else {
                            return true;
                        }
                    });
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

    void applySearchPredicate(String searchText) {
        String string = searchText == null ? "" : searchText.toLowerCase();
        model.setSearchStringPredicate(item -> {
                    if (!(item instanceof MarketItem marketItem)) {
                        return true;
                    }
                    return StringUtils.isEmpty(string)
                            || marketItem.getAmountCode().toLowerCase().contains(string)
                            || marketItem.getMarket().getMarketDisplayName().toLowerCase().contains(string);
                });
        updateFilteredMarketListItems();
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
        model.getCurrencyConverterListItems().forEach(item -> {
            if (item instanceof MarketItem marketItem) {
                marketItem.updateFormattedAmount(btcBalance);
            }
        });
    }

    private void updateFilteredMarketListItems() {
        model.getFilteredMarketListItems().setPredicate(null);
        model.getFilteredMarketListItems().setPredicate(model.getMarketListItemsPredicate());
        model.getFilteredCurrencyConverterListItems().setPredicate(null);
        model.getFilteredCurrencyConverterListItems().setPredicate(model.getHeaderPredicate());
    }

    private void setSelectedMarket() {
        Market selectedMarket = model.getSelectedMarket().get();
        if (selectedMarket != null) {
            model.getCurrencyConverterListItems().stream()
                    .filter(item -> item instanceof MarketItem marketItem
                            && marketItem.getMarket().equals(selectedMarket))
                    .map(MarketItem.class::cast)
                    .findAny()
                    .ifPresent(item -> model.getSelectedMarketItem().set(item));
        }
    }

    private void updateSelectedMarket(MarketItem marketItem) {
        if (marketItem != null) {
            model.getSelectedMarket().set(marketItem.getMarket());
        }
    }

    private void addCurrencyConverterListItems() {
        model.getCurrencyConverterListItems().clear();
        model.getCurrencyConverterListItems().add(new HeaderItem(Res.get("wallet.dashboard.currencyConverterMenu.cryptoCurrencies"), true));
        model.getCurrencyConverterListItems().addAll(AVAILABLE_CRYPTO_MARKETS_FOR_CURRENCY_CONVERSION.stream()
                .map(market -> new MarketItem(market, marketPriceService))
                .toList());
        model.getCurrencyConverterListItems().add(new HeaderItem(Res.get("wallet.dashboard.currencyConverterMenu.fiatCurrencies"), false));
        model.getCurrencyConverterListItems().addAll(AVAILABLE_FIAT_MARKETS_FOR_CURRENCY_CONVERSION.stream()
                .map(market -> new MarketItem(market, marketPriceService))
                .toList());
    }
}
