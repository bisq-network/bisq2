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

package bisq.desktop.primary.main.content.offerbook_old;

import bisq.application.DefaultServiceProvider;
import bisq.common.monetary.Market;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.NavigationModel;
import bisq.oracle.marketprice.MarketPrice;
import bisq.oracle.marketprice.MarketPriceService;
import io.reactivex.disposables.Disposable;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class OfferbookModel extends NavigationModel implements MarketPriceService.Listener {

    static final String SHOW_ALL = "Show all";

    // Exposed for filter model
    final ObservableList<OfferListItem> offerItems = FXCollections.observableArrayList();
    final Set<Predicate<OfferListItem>> listFilterPredicates = new CopyOnWriteArraySet<>();
    private final DefaultServiceProvider serviceProvider;
    Predicate<OfferListItem> askCurrencyPredicate = e -> true;
    Predicate<OfferListItem> bidCurrencyPredicate = e -> true;
    String baseCurrency, quoteCurrency;

    private final FilteredList<OfferListItem> filteredItems = new FilteredList<>(offerItems);

    // exposed for view
    @Getter
    private final SortedList<OfferListItem> sortedItems = new SortedList<>(filteredItems);
    @Getter
    private final StringProperty selectedAskCurrencyProperty = new SimpleStringProperty();
    @Getter
    private final StringProperty selectedBidCurrencyProperty = new SimpleStringProperty();
    @Getter
    private final ObjectProperty<Map<String, MarketPrice>> marketPriceByCurrencyMapProperty = new SimpleObjectProperty<>();
    @Getter
    private final ObservableList<String> currenciesProperty = FXCollections.observableArrayList(SHOW_ALL, "BTC", "USD", "EUR", "XMR", "USDT");
    @Getter
    private final RangeFilterModel amountFilterModel;
    @Getter
    private final StringProperty offeredAmountHeaderProperty = new SimpleStringProperty("Offered amount");
    @Getter
    private final StringProperty askedAmountHeaderProperty = new SimpleStringProperty("Asked amount");
    @Getter
    private final StringProperty priceHeaderProperty = new SimpleStringProperty("Price");
    private final BooleanProperty showAllAskCurrencies = new SimpleBooleanProperty();
    private final BooleanProperty showAllBidCurrencies = new SimpleBooleanProperty();

    private Disposable offerEntityAddedDisposable, offerEntityRemovedDisposable;

    public OfferbookModel(DefaultServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;

        amountFilterModel = new RangeFilterModel(this);
    }

    @Override
    public NavigationTarget getDefaultNavigationTarget() {
        return NavigationTarget.NONE;
    }

    public void initialize() {
        selectedAskCurrencyProperty.set("BTC");
        selectedBidCurrencyProperty.set("USD");
        amountFilterModel.initialize();
    }


    public void onViewAttached() {
        serviceProvider.getOfferPresentationService().activate();

        offerItems.clear();
        offerItems.addAll(serviceProvider.getOfferPresentationService().getOfferEntities().stream()
                .map(OfferListItem -> new OfferListItem(OfferListItem.getOffer(),
                        OfferListItem.getMarketPriceSubject(),
                        showAllAskCurrencies,
                        showAllBidCurrencies))
                .collect(Collectors.toList()));

        applyBaseCurrency();

        resetFilter();

        setSelectAskCurrency("BTC");
        setSelectBidCurrency("USD");

        amountFilterModel.activate();

        offerEntityAddedDisposable = serviceProvider.getOfferPresentationService().getOfferEntityAddedSubject().subscribe(offerEntity -> {
            offerItems.add(new OfferListItem(offerEntity.getOffer(),
                    offerEntity.getMarketPriceSubject(),
                    showAllAskCurrencies,
                    showAllBidCurrencies));
        }, Throwable::printStackTrace);

        offerEntityRemovedDisposable = serviceProvider.getOfferPresentationService().getOfferEntityRemovedSubject().subscribe(offerEntity -> {
            offerItems.stream()
                    .filter(e -> e.getOffer().equals(offerEntity.getOffer()))
                    .findAny()
                    .ifPresent(offerItems::remove);
        }, Throwable::printStackTrace);
        serviceProvider.getMarketPriceService().addListener(this);
       //todo
        // marketPriceByCurrencyMapProperty.set(serviceProvider.getMarketPriceService().getMarketPriceByCurrencyMap());
    }

    public void onViewDetached() {
        serviceProvider.getOfferPresentationService().deactivate();
        amountFilterModel.deactivate();

        if (offerEntityAddedDisposable != null) {
            offerEntityAddedDisposable.dispose();
        }
        if (offerEntityRemovedDisposable != null) {
            offerEntityRemovedDisposable.dispose();
        }

        serviceProvider.getMarketPriceService().removeListener(this);
    }

    @Override
    public void onMarketPriceUpdate(Map<Market, MarketPrice> map) {
        //todo
       // marketPriceByCurrencyMapProperty.set(map);
    }

    @Override
    public void onMarketPriceSelected(MarketPrice selected) {
        
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void resetFilter() {
        clearFilterPredicates();
        amountFilterModel.reset();
    }

    public void setSelectAskCurrency(String currency) {
        if (SHOW_ALL.equals(currency)) {
            selectedAskCurrencyProperty.set(null);
            setAskCurrencyPredicate(item -> true);
            showAllBidCurrencies.set(true);
        } else {
            selectedAskCurrencyProperty.set(currency);
            setAskCurrencyPredicate(item -> item.getOffer().getBidSwapSide().code().equals(currency));
            showAllBidCurrencies.set(false);
        }
        updateHeaders();
    }

    public void setSelectBidCurrency(String currency) {
        if (SHOW_ALL.equals(currency)) {
            selectedBidCurrencyProperty.set(null);
            setBidCurrencyPredicate(item -> true);
            showAllAskCurrencies.set(true);
        } else {
            selectedBidCurrencyProperty.set(currency);
            setBidCurrencyPredicate(item -> item.getOffer().getAskCode().equals(currency));
            showAllAskCurrencies.set(false);
        }
        updateHeaders();
    }

    private void updateHeaders() {
        if (selectedAskCurrencyProperty.get() == null || selectedBidCurrencyProperty.get() == null) {
            priceHeaderProperty.set("Price");
        } else {
            priceHeaderProperty.set("Price " + baseCurrency + "/" + quoteCurrency);
        }

        if (selectedAskCurrencyProperty.get() == null) {
            offeredAmountHeaderProperty.set("Offered amount");
        } else {
            offeredAmountHeaderProperty.set("Offered amount " + selectedAskCurrencyProperty.get());
        }

        if (selectedBidCurrencyProperty.get() == null) {
            askedAmountHeaderProperty.set("Asked amount");
        } else {
            askedAmountHeaderProperty.set("Asked amount " + selectedBidCurrencyProperty.get());
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Package private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    void clearFilterPredicates() {
        listFilterPredicates.clear();

        amountFilterModel.clearFilterPredicates();
        applyListFilterPredicates();
    }

    void applyListFilterPredicates() {
        listFilterPredicates.stream().reduce(Predicate::and)
                .ifPresent(filteredItems::setPredicate);
    }

    void applyBaseCurrency() {
        filteredItems.stream().findAny().ifPresent(o -> {
            baseCurrency = o.getOffer().getBaseCode();
            quoteCurrency = o.getOffer().getQuoteCode();
        });
    }

    private void setAskCurrencyPredicate(Predicate<OfferListItem> predicate) {
        //clearFilterPredicates();
        listFilterPredicates.remove(askCurrencyPredicate);
        listFilterPredicates.add(predicate);
        askCurrencyPredicate = predicate;
        applyListFilterPredicates();
    }

    private void setBidCurrencyPredicate(Predicate<OfferListItem> predicate) {
        //clearFilterPredicates();
        listFilterPredicates.remove(bidCurrencyPredicate);
        listFilterPredicates.add(predicate);
        bidCurrencyPredicate = predicate;
        applyListFilterPredicates();
    }
}
