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

package bisq.desktop.main.content.wallet.dashboard;

import bisq.common.monetary.Coin;
import bisq.desktop.common.view.Model;
import bisq.desktop.main.content.wallet.WalletTxListItem;
import bisq.presentation.formatters.AmountFormatter;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

@Slf4j
@Getter
public class WalletDashboardModel implements Model {
    private final ObjectProperty<Coin> balanceAsCoinProperty = new SimpleObjectProperty<>(Coin.fromValue(0, "BTC"));
    private final ObservableValue<String> formattedBalanceProperty = Bindings.createStringBinding(
            () -> AmountFormatter.formatBaseAmount(balanceAsCoinProperty.get()),
            balanceAsCoinProperty
    );
    private final ObjectProperty<Coin> availableBalanceAsCoinProperty = new SimpleObjectProperty<>(Coin.fromValue(0, "BTC"));
    private final ObservableValue<String> formattedAvailableBalanceProperty = Bindings.createStringBinding(
            () -> AmountFormatter.formatBaseAmount(availableBalanceAsCoinProperty.get()),
            availableBalanceAsCoinProperty
    );
    private final ObjectProperty<Coin> reservedFundsAsCoinProperty = new SimpleObjectProperty<>(Coin.fromValue(0, "BTC"));
    private final ObservableValue<String> formattedReservedFundsProperty = Bindings.createStringBinding(
            () -> AmountFormatter.formatBaseAmount(reservedFundsAsCoinProperty.get()),
            reservedFundsAsCoinProperty
    );
    private final ObjectProperty<Coin> lockedFundsAsCoinProperty = new SimpleObjectProperty<>(Coin.fromValue(0, "BTC"));
    private final ObservableValue<String> formattedLockedFundsProperty = Bindings.createStringBinding(
            () -> AmountFormatter.formatBaseAmount(lockedFundsAsCoinProperty.get()),
            lockedFundsAsCoinProperty
    );
    private final ObservableList<WalletTxListItem> listItems = FXCollections.observableArrayList();
    private final FilteredList<WalletTxListItem> filteredListItems = new FilteredList<>(listItems);
    private final SortedList<WalletTxListItem> sortedList = new SortedList<>(filteredListItems, Comparator.comparingLong(WalletTxListItem::getDate).reversed());

    public WalletDashboardModel() {
    }
}
