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

package bisq.desktop.primary.main.content.wallet.transactions;

import bisq.desktop.common.view.Model;
import bisq.wallets.model.Transaction;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;

import java.util.List;

public class WalletTransactionsModel implements Model {
    @Getter
    private final ObservableList<WalletTransactionListItem> listItems = FXCollections.observableArrayList();
    @Getter
    private final SortedList<WalletTransactionListItem> sortedList = new SortedList<>(listItems);

    public void addTransactions(List<Transaction> transactions) {
        transactions.stream()
                .map(WalletTransactionListItem::new)
                .forEach(listItems::add);
    }
}
