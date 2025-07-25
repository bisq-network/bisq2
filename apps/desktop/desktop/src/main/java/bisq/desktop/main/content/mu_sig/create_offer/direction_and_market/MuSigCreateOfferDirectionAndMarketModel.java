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

package bisq.desktop.main.content.mu_sig.create_offer.direction_and_market;

import bisq.common.market.Market;
import bisq.desktop.common.view.Model;
import bisq.offer.Direction;
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

@Getter
public class MuSigCreateOfferDirectionAndMarketModel implements Model {
    private final ObjectProperty<Direction> direction = new SimpleObjectProperty<>(Direction.BUY);
    private final BooleanProperty buyButtonDisabled = new SimpleBooleanProperty();
    private final ObjectProperty<MuSigCreateOfferDirectionAndMarketView.ListItem> selectedMarketListItem = new SimpleObjectProperty<>();
    private final StringProperty searchText = new SimpleStringProperty();
    private final ObjectProperty<Market> selectedMarket = new SimpleObjectProperty<>();
    private final ObservableList<MuSigCreateOfferDirectionAndMarketView.ListItem> listItems = FXCollections.observableArrayList();
    private final FilteredList<MuSigCreateOfferDirectionAndMarketView.ListItem> filteredList = new FilteredList<>(listItems);
    private final SortedList<MuSigCreateOfferDirectionAndMarketView.ListItem> sortedList = new SortedList<>(filteredList);

    void reset() {
        direction.set(Direction.BUY);
        buyButtonDisabled.set(false);
        selectedMarketListItem.set(null);
        searchText.set(null);
        selectedMarket.set(null);
        listItems.clear();
    }
}
