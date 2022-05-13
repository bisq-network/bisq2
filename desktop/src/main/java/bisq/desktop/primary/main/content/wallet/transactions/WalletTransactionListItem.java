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

import bisq.desktop.components.table.TableItem;
import bisq.wallets.core.model.Transaction;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class WalletTransactionListItem implements TableItem {
    private final StringProperty txId = new SimpleStringProperty(this, "wallet.column.txId");
    private final StringProperty address = new SimpleStringProperty(this, "address");
    private final StringProperty amount = new SimpleStringProperty(this, "amount");
    private final StringProperty confirmations = new SimpleStringProperty(this, "wallet.column.confirmations");

    public WalletTransactionListItem(Transaction transaction) {
        txId.set(transaction.getTxId());
        address.set(transaction.getAddress());
        amount.set(String.valueOf(transaction.getAmount()));
        confirmations.set(String.valueOf(transaction.getConfirmations()));
    }

    public StringProperty txIdProperty() {
        return txId;
    }

    public StringProperty addressProperty() {
        return address;
    }

    public StringProperty amountProperty() {
        return amount;
    }

    public StringProperty confirmationsProperty() {
        return confirmations;
    }
}
