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

package bisq.desktop.primary.main.content.wallet.utxos;

import bisq.wallets.model.Utxo;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class WalletUtxoListItem {
    private final StringProperty txId = new SimpleStringProperty(this, "wallet.column.txId");
    private final StringProperty address = new SimpleStringProperty(this, "address");
    private final StringProperty amount = new SimpleStringProperty(this, "amount");
    private final StringProperty confirmations = new SimpleStringProperty(this, "wallet.column.confirmations");
    private final BooleanProperty reused = new SimpleBooleanProperty(this, "wallet.column.reused");

    public WalletUtxoListItem(Utxo utxo) {
        txId.set(utxo.getTxId());
        address.set(utxo.getAddress());
        amount.set(String.valueOf(utxo.getAmount()));
        confirmations.set(String.valueOf(utxo.getConfirmations()));
        reused.set(utxo.isReused());
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

    public BooleanProperty getReusedProperty() {
        return reused;
    }
}
