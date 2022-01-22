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

import bisq.account.settlement.Settlement;
import bisq.application.DefaultServiceProvider;
import bisq.common.currency.BisqCurrency;
import bisq.common.currency.CryptoCurrencyRepository;
import bisq.common.currency.CurrencyRepository;
import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.common.monetary.QuoteCodePair;
import bisq.desktop.common.view.Model;
import bisq.offer.protocol.SwapProtocolType;
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

    // Currencies
    private ObservableList<BisqCurrency> currencies = FXCollections.observableArrayList();
    private final StringProperty baseCurrencyCode = new SimpleStringProperty();
    private final StringProperty quoteCurrencyCode = new SimpleStringProperty();

    // Ask
    private ObjectProperty<Monetary> ask = new SimpleObjectProperty<>();
    private final ObjectProperty<BisqCurrency> selectedAskCurrency = new SimpleObjectProperty<>();

    // Bid
    private ObjectProperty<Monetary> bid = new SimpleObjectProperty<>();
    private final ObjectProperty<BisqCurrency> selectedBidCurrency = new SimpleObjectProperty<>();

    // Quote
    private final ObjectProperty<Quote> fixPriceQuote = new SimpleObjectProperty<>();
    private final ObjectProperty<QuoteCodePair> quoteCodePair = new SimpleObjectProperty<>();

    // Protocol
    private final ObservableList<SwapProtocolType> protocols = FXCollections.observableArrayList();
    private final ObjectProperty<SwapProtocolType> selectedProtocol = new SimpleObjectProperty<>();

    // Settlement
    private final ObservableList<Settlement.Method> askSettlementMethods = FXCollections.observableArrayList();
    private final ObservableList<Settlement.Method> bidSettlementMethods = FXCollections.observableArrayList();
    private final ObjectProperty<Settlement.Method> askSelectedSettlementMethod = new SimpleObjectProperty<>();
    private final ObjectProperty<Settlement.Method> bidSelectedSettlementMethod = new SimpleObjectProperty<>();

    public CreateOfferModel(DefaultServiceProvider serviceProvider) {
        marketPriceService = serviceProvider.getMarketPriceService();
    }

    public void onViewAttached() {
        currencies.clear();
        currencies.addAll(CurrencyRepository.getAllCurrencies());
        selectedAskCurrency.set(CryptoCurrencyRepository.getDefaultCurrency());
        selectedBidCurrency.set(FiatCurrencyRepository.getDefaultCurrency());

       // protocols.setAll(SwapProtocolType.values());
       // selectedProtocol.set(SwapProtocolType.REPUTATION);
    }

    public void onViewDetached() {
        currencies.clear();
    }
}
