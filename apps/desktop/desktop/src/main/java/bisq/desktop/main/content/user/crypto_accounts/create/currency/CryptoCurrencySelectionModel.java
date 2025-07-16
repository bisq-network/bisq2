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

package bisq.desktop.main.content.user.crypto_accounts.create.currency;

import bisq.account.payment_method.CryptoPaymentMethod;
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
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static bisq.desktop.main.content.user.crypto_accounts.create.currency.CryptoCurrencySelectionView.CryptoCurrencyItem;

@Slf4j
@Getter
public class CryptoCurrencySelectionModel implements Model {
    private final ObservableList<CryptoCurrencyItem> list = FXCollections.observableArrayList();
    private final FilteredList<CryptoCurrencyItem> filteredList = new FilteredList<>(list);
    private final SortedList<CryptoCurrencyItem> sortedList = new SortedList<>(filteredList);
    private final ObjectProperty<CryptoCurrencyItem> selectedItem = new SimpleObjectProperty<>();
    private final ObjectProperty<CryptoPaymentMethod> selectedCryptoPaymentMethod = new SimpleObjectProperty<>();
    private final StringProperty searchText = new SimpleStringProperty("");

    public CryptoCurrencySelectionModel(List<CryptoCurrencyItem> list) {
        this.list.setAll(list);
    }
}