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

package bisq.desktop.main.content.wallet.txs;

import bisq.desktop.common.view.Model;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Predicate;

@Slf4j
@Getter
public class WalletTxsModel implements Model {
    private final ObservableList<WalletTransactionListItem> listItems = FXCollections.observableArrayList();
    private final FilteredList<WalletTransactionListItem> filteredListItems = new FilteredList<>(listItems);
    private final SortedList<WalletTransactionListItem> sortedList = new SortedList<>(filteredListItems);
    private final ObjectProperty<TxsFilter> selectedFilter = new SimpleObjectProperty<>();

    private final Predicate<WalletTransactionListItem> listItemsPredicate = item ->
            getFilterPredicate().test(item);
    @Setter
    private Predicate<WalletTransactionListItem> filterPredicate = item -> true;

    public WalletTxsModel() {
    }
}
