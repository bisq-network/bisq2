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

package bisq.desktop.main.content.mu_sig.offerbook;

import bisq.desktop.common.view.Model;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
    private final StringProperty marketIconId = new SimpleStringProperty("");

    private final Set<String> muSigOfferIds = new HashSet<>();
    private final ObservableList<MuSigOfferListItem> muSigOfferListItems = FXCollections.observableArrayList();
    private final FilteredList<MuSigOfferListItem> filteredMuSigOfferListItems = new FilteredList<>(muSigOfferListItems);
    private final SortedList<MuSigOfferListItem> sortedMuSigOfferListItems = new SortedList<>(filteredMuSigOfferListItems);

    private final ObservableList<MarketItem> marketItems = FXCollections.observableArrayList();
    private final FilteredList<MarketItem> filteredMarketItems = new FilteredList<>(marketItems);
    private final SortedList<MarketItem> sortedMarketItems = new SortedList<>(filteredMarketItems);
    private final FilteredList<MarketItem> favouriteMarketItems = new FilteredList<>(marketItems);
    private final ObjectProperty<MarketItem> selectedMarketItem = new SimpleObjectProperty<>();
    private final StringProperty marketsSearchBoxText = new SimpleStringProperty();
    private final ObjectProperty<MarketFilter> selectedMarketsFilter = new SimpleObjectProperty<>();
    private final ObjectProperty<MarketSortType> selectedMarketSortType = new SimpleObjectProperty<>(MarketSortType.NUM_OFFERS);
    private final BooleanProperty shouldShowAppliedFilters = new SimpleBooleanProperty();

    @Setter
    private BooleanProperty favouritesListViewNeedsHeightUpdate = new SimpleBooleanProperty();
    @Setter
    private Predicate<MarketItem> marketFilterPredicate = marketItem -> true;
    @Setter
    private Predicate<MarketItem> marketSearchTextPredicate = marketItem -> true;
    @Setter
    private Predicate<MarketItem> marketPricePredicate = marketItem -> true;
}
