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

package bisq.desktop.main.content.bisq_easy.trade_wizard.market;

import bisq.common.currency.Market;
import bisq.desktop.common.view.Model;
import bisq.offer.Direction;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.Setter;

@Getter
public class TradeWizardMarketModel implements Model {
    @Setter
    private Direction direction;
    @Setter
    private String headline;
    private final ObjectProperty<TradeWizardMarketView.MarketListItem> selectedMarketListItem = new SimpleObjectProperty<>();
    private final StringProperty searchText = new SimpleStringProperty();
    private final ObjectProperty<Market> selectedMarket = new SimpleObjectProperty<>();
    private final ObservableList<TradeWizardMarketView.MarketListItem> listItems = FXCollections.observableArrayList();
    private final FilteredList<TradeWizardMarketView.MarketListItem> filteredList = new FilteredList<>(listItems);
    private final SortedList<TradeWizardMarketView.MarketListItem> sortedList = new SortedList<>(filteredList);


    void reset() {
        selectedMarketListItem.set(null);
        searchText.set(null);
        selectedMarket.set(null);
        listItems.clear();
    }
}