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

package bisq.desktop.primary.overlay.onboarding.offer.market;

import bisq.common.currency.Market;
import bisq.desktop.common.view.Model;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;

@Getter
public class MarketModel implements Model {
    private final ObjectProperty<Market> market = new SimpleObjectProperty<>();
    protected final ObservableList<MarketListItem> observableList = FXCollections.observableArrayList();
    protected final SortedList<MarketListItem> sortedList = new SortedList<>(observableList);
    

    protected void fillObservableList() {
        //todo mocked data - setup real
        observableList.setAll(
                new MarketListItem("BTC", "USD", "256", "32"),
                new MarketListItem("BTC", "EUR","123", "34"),
                new MarketListItem("BTC", "CAD","108", "26"),
                new MarketListItem("BTC", "XMR","105", "22"),
                new MarketListItem("BTC", "LTC","105", "22"),
                new MarketListItem("BTC", "ETH","105", "22"),
                new MarketListItem("BTC", "GBP","105", "22")
        );
    }
    
}