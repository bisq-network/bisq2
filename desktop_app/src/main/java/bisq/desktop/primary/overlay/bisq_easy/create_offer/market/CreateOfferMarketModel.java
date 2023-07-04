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

package bisq.desktop.primary.overlay.bisq_easy.create_offer.market;

import bisq.common.currency.Market;
import bisq.desktop.common.view.Model;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;

@Getter
public class CreateOfferMarketModel implements Model {
    private final ObjectProperty<CreateOfferMarketView.MarketListItem> selectedMarketListItem = new SimpleObjectProperty<>();
    private final StringProperty searchText = new SimpleStringProperty();
    private final ObjectProperty<Market> selectedMarket = new SimpleObjectProperty<>();
    private final ObservableList<CreateOfferMarketView.MarketListItem> listItems = FXCollections.observableArrayList();
    private final FilteredList<CreateOfferMarketView.MarketListItem> filteredList = new FilteredList<>(listItems);
    private final SortedList<CreateOfferMarketView.MarketListItem> sortedList = new SortedList<>(filteredList);

    void reset() {
        selectedMarketListItem.set(null);
        searchText.set(null);
        selectedMarket.set(null);
        listItems.clear();
    }
}