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

import bisq.common.asset.CryptoAsset;
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
    private final StringProperty headlineText = new SimpleStringProperty();
    private final StringProperty buyButtonText = new SimpleStringProperty();
    private final StringProperty sellButtonText = new SimpleStringProperty();

    private final ObjectProperty<MuSigCreateOfferDirectionAndMarketView.MarketListItem> selectedMarketListItem = new SimpleObjectProperty<>();
    private final StringProperty paymentCurrencySearchText = new SimpleStringProperty();
    private final ObjectProperty<Market> selectedMarket = new SimpleObjectProperty<>();
    private final ObservableList<MuSigCreateOfferDirectionAndMarketView.MarketListItem> marketListItems = FXCollections.observableArrayList();
    private final FilteredList<MuSigCreateOfferDirectionAndMarketView.MarketListItem> filteredMarketListItems = new FilteredList<>(marketListItems);
    private final SortedList<MuSigCreateOfferDirectionAndMarketView.MarketListItem> sortedMarketListItems = new SortedList<>(filteredMarketListItems);

    private final ObjectProperty<MuSigCreateOfferDirectionAndMarketView.BaseCryptoAssetListItem> selectedBaseCryptoAssetListItem = new SimpleObjectProperty<>();
    private final StringProperty cryptoCurrencySearchText = new SimpleStringProperty();
    private final ObjectProperty<CryptoAsset> selectedBaseCryptoAsset = new SimpleObjectProperty<>();
    private final ObservableList<MuSigCreateOfferDirectionAndMarketView.BaseCryptoAssetListItem> baseCryptoAssetListItems = FXCollections.observableArrayList();
    private final FilteredList<MuSigCreateOfferDirectionAndMarketView.BaseCryptoAssetListItem> filteredBaseCryptoAssetListItems = new FilteredList<>(baseCryptoAssetListItems);
    private final SortedList<MuSigCreateOfferDirectionAndMarketView.BaseCryptoAssetListItem> sortedBaseCryptoAssetListItems = new SortedList<>(filteredBaseCryptoAssetListItems);

    void reset() {
        direction.set(Direction.BUY);
        buyButtonDisabled.set(false);
        headlineText.set("");
        selectedMarketListItem.set(null);
        paymentCurrencySearchText.set(null);
        selectedMarket.set(null);
        marketListItems.clear();
        selectedBaseCryptoAssetListItem.set(null);
        cryptoCurrencySearchText.set(null);
        selectedBaseCryptoAsset.set(null);
        baseCryptoAssetListItems.clear();
    }
}
