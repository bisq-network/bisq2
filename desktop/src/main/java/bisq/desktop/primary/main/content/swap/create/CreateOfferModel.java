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

package bisq.desktop.primary.main.content.swap.create;

import bisq.application.DefaultServiceProvider;
import bisq.common.currency.BisqCurrency;
import bisq.common.currency.CryptoCurrencyRepository;
import bisq.common.currency.CurrencyRepository;
import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.common.monetary.QuoteCodePair;
import bisq.desktop.common.view.Model;
import bisq.oracle.marketprice.MarketPriceService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class CreateOfferModel implements Model {
    private final MarketPriceService marketPriceService;
    private ObservableList<BisqCurrency> currencies = FXCollections.observableArrayList();

    private ObjectProperty<Monetary> ask = new SimpleObjectProperty<>();
    private final ObjectProperty<BisqCurrency> selectedAskCurrency = new SimpleObjectProperty<>();

    private ObjectProperty<Monetary> bid = new SimpleObjectProperty<>();
    private final ObjectProperty<BisqCurrency> selectedBidCurrency = new SimpleObjectProperty<>();
    private final ObjectProperty<Quote> fixPriceQuote = new SimpleObjectProperty<>();
    private final ObjectProperty<QuoteCodePair> quoteCodePair = new SimpleObjectProperty<>();
    private final StringProperty baseCurrencyCode = new SimpleStringProperty();
    private final StringProperty quoteCurrencyCode = new SimpleStringProperty();
    
    public CreateOfferModel(DefaultServiceProvider serviceProvider) {
        marketPriceService = serviceProvider.getMarketPriceService();
    }

    public void onViewAttached() {
        currencies.clear();
        currencies.addAll(CurrencyRepository.getAllCurrencies());
        selectedAskCurrency.set(CryptoCurrencyRepository.getDefaultCurrency());
        selectedBidCurrency.set(FiatCurrencyRepository.getDefaultCurrency());

      /*  selectedBidCurrency.set(CryptoCurrencyRepository.getDefaultCurrency());
        selectedAskCurrency.set(FiatCurrencyRepository.getDefaultCurrency());*/
    }

    public void onViewDetached() {
        currencies.clear();
    }
}
