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

package bisq.desktop.primary.main.content.wallet;

import bisq.common.monetary.Coin;
import bisq.desktop.common.view.Model;
import bisq.presentation.formatters.AmountFormatter;
import bisq.wallets.core.model.Transaction;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Getter
public class WalletModel implements Model {

    private final ObjectProperty<Coin> balanceAsCoinProperty = new SimpleObjectProperty<>(Coin.of(0, "BTC"));
    private final ObservableValue<String> formattedBalanceProperty = Bindings.createStringBinding(
            () -> AmountFormatter.formatAmount(balanceAsCoinProperty.get()),
            balanceAsCoinProperty
    );

    @Getter
    private final ObservableList<WalletTransactionListItem> transactionHistoryList = FXCollections.observableArrayList();

    private final StringProperty receiveAddressProperty = new SimpleStringProperty(this, "receiveAddress");

    public WalletModel() {
    }

    public void addTransactions(List<? extends Transaction> transactions) {
        transactions.stream()
                .map(WalletTransactionListItem::new)
                .forEach(transactionHistoryList::add);
    }

    public String getReceiveAddress() {
        return receiveAddressProperty.get();
    }
}
