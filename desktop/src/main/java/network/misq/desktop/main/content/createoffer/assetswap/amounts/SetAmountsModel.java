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

package network.misq.desktop.main.content.createoffer.assetswap.amounts;


import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import network.misq.desktop.common.view.Model;

public class SetAmountsModel implements Model {
    @Getter
    private final StringProperty selectedAskCurrency = new SimpleStringProperty();
    @Getter
    private final StringProperty selectedBidCurrency = new SimpleStringProperty();
    @Getter
    private final ObservableList<String> currencies = FXCollections.observableArrayList("BTC", "USD", "EUR", "XMR", "USDT");

    public void setSelectAskCurrency(String currency) {
        selectedAskCurrency.set(currency);
    }

    public void setSelectBidCurrency(String currency) {
        selectedBidCurrency.set(currency);
    }

    public void flipCurrencies() {

    }
}
