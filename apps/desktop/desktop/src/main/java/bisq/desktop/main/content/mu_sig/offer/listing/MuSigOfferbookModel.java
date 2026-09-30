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

package bisq.desktop.main.content.mu_sig.offer.listing;

import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.common.market.Market;
import bisq.desktop.common.view.Model;
import bisq.desktop.main.content.mu_sig.offer.MuSigOfferListItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

@Slf4j
@Getter
public class MuSigOfferbookModel implements Model {
    private final StringProperty marketTitle = new SimpleStringProperty("");
    private final StringProperty marketDescription = new SimpleStringProperty("");
    private final StringProperty marketPrice = new SimpleStringProperty("");

    private final StringProperty baseCodeTitle = new SimpleStringProperty("");
    private final StringProperty quoteCodeTitle = new SimpleStringProperty("");
    private final StringProperty priceTitle = new SimpleStringProperty("");
    private final StringProperty marketsIconId = new SimpleStringProperty("");

    private final ObjectProperty<MuSigOfferListItem> selectedMuSigOfferListItem = new SimpleObjectProperty<>();
    private final ObjectProperty<Market> selectedMarket = new SimpleObjectProperty<>();
    private final Set<String> muSigOfferIds = new HashSet<>();
    private final ObservableList<MuSigOfferListItem> muSigOfferListItems = FXCollections.observableArrayList();
    private final FilteredList<MuSigOfferListItem> filteredMuSigOfferListItems = new FilteredList<>(muSigOfferListItems);
    private final SortedList<MuSigOfferListItem> sortedMuSigOfferListItems = new SortedList<>(filteredMuSigOfferListItems);
    private final ObjectProperty<MuSigFilters.MuSigOffersFilter> selectedMuSigOffersFilter = new SimpleObjectProperty<>();
    private final ObservableList<FiatPaymentMethod> availablePaymentMethods = FXCollections.observableArrayList();
    private final ObservableSet<FiatPaymentMethod> selectedPaymentMethods = FXCollections.observableSet();
    private final StringProperty paymentFilterTitle = new SimpleStringProperty("");
    private final IntegerProperty activeMarketPaymentsCount = new SimpleIntegerProperty();

    private final Predicate<MuSigOfferListItem> muSigOfferListItemsPredicate = item ->
            getMuSigOffersFilterPredicate().test(item)
                    && getMuSigMarketFilterPredicate().test(item)
                    && getPaymentMethodFilterPredicate().test(item);
    private final Predicate<MuSigOfferListItem> muSigMarketFilterPredicate = item ->
            getSelectedMarketItem().get() == null
                    || getSelectedMarketItem().get().getMarket() == null
                    || getSelectedMarketItem().get().getMarket().equals(item.getMarket());
    @Setter
    private Predicate<MuSigOfferListItem> muSigOffersFilterPredicate = item -> true;
    @Setter
    private Predicate<MuSigOfferListItem> paymentMethodFilterPredicate = item -> true;

    private final ObjectProperty<MarketType> selectedMarketType = new SimpleObjectProperty<>();
    private final ObservableList<MuSigMarketItem> marketItems = FXCollections.observableArrayList();
    private final FilteredList<MuSigMarketItem> filteredMarketItems = new FilteredList<>(marketItems);
    private final SortedList<MuSigMarketItem> sortedMarketItems = new SortedList<>(filteredMarketItems);
    private final FilteredList<MuSigMarketItem> favouriteMarketItems = new FilteredList<>(marketItems);
    private final SortedList<MuSigMarketItem> sortedFavouriteMarketItems = new SortedList<>(favouriteMarketItems, MuSigMarketItemUtil.sortByMarketNameAsc());
    private final ObjectProperty<MuSigMarketItem> selectedMarketItem = new SimpleObjectProperty<>();

    private final StringProperty marketListTitle = new SimpleStringProperty();
    private final StringProperty marketsSearchBoxText = new SimpleStringProperty();
    private final ObjectProperty<MuSigFilters.MarketFilter> selectedMarketsFilter = new SimpleObjectProperty<>();
    private final ObjectProperty<MuSigMarketSortType> selectedMarketSortType = new SimpleObjectProperty<>(MuSigMarketSortType.NUM_OFFERS);
    private final BooleanProperty shouldShowAppliedFilters = new SimpleBooleanProperty();
    private final BooleanProperty shouldShowFavouritesListView = new SimpleBooleanProperty();
    private final BooleanProperty favouritesListViewNeedsHeightUpdate = new SimpleBooleanProperty();

    private final Predicate<MuSigMarketItem> marketItemsPredicate = item ->
            getMarketFilterPredicate().test(item)
                    && getMarketSearchTextPredicate().test(item)
                    && getMarketPricePredicate().test(item)
                    && !item.getIsFavourite().get();
    private final Predicate<MuSigMarketItem> favouriteMarketItemsPredicate = item -> item.getIsFavourite().get();
    @Setter
    private Predicate<MuSigMarketItem> marketFilterPredicate = item -> true;
    @Setter
    private Predicate<MuSigMarketItem> marketSearchTextPredicate = item -> true;
    @Setter
    private Predicate<MuSigMarketItem> marketPricePredicate = item -> true;
}
