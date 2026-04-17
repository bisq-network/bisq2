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

package bisq.desktop.main.content.mu_sig.offer.create_offer.direction_and_market;

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
import javafx.scene.layout.StackPane;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class MuSigCreateOfferDirectionAndMarketModel implements Model {
    static final Map<String, StackPane> MARKET_ICON_CACHE = new HashMap<>();
    private final ObjectProperty<Direction> direction = new SimpleObjectProperty<>();
    private final BooleanProperty buyButtonDisabled = new SimpleBooleanProperty();
    private final StringProperty headlineText = new SimpleStringProperty();
    private final StringProperty buyButtonText = new SimpleStringProperty();
    private final StringProperty sellButtonText = new SimpleStringProperty();

    private final ObjectProperty<MarketTypeListItem> selectedMarketTypeListItem = new SimpleObjectProperty<>();
    private final ObjectProperty<MarketListItem> selectedMarketListItem = new SimpleObjectProperty<>();
    private final StringProperty paymentCurrencySearchText = new SimpleStringProperty();
    private final ObjectProperty<StackPane> tradePairImage = new SimpleObjectProperty<>();
    private final ObservableList<MarketListItem> marketListItems = FXCollections.observableArrayList();
    private final FilteredList<MarketListItem> filteredMarketListItems = new FilteredList<>(marketListItems);
    private final SortedList<MarketListItem> sortedMarketListItems = new SortedList<>(filteredMarketListItems);

    private final ObservableList<MarketTypeListItem> marketTypeListItems = FXCollections.observableArrayList();
    private final FilteredList<MarketTypeListItem> filteredMarketTypeListItems = new FilteredList<>(marketTypeListItems);
    private final SortedList<MarketTypeListItem> sortedMarketTypeListItems = new SortedList<>(filteredMarketTypeListItems);


    public MuSigCreateOfferDirectionAndMarketModel(List<MarketTypeListItem> marketTypeListItems) {
        this.marketTypeListItems.addAll(marketTypeListItems);
    }

    void reset() {
        buyButtonDisabled.set(false);
        headlineText.set("");
        selectedMarketListItem.set(null);
        paymentCurrencySearchText.set(null);
        tradePairImage.set(null);
        marketListItems.clear();
    }
}
