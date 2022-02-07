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
    private final BooleanProperty reused = new SimpleBooleanProperty(this, "wallet.column.txId");

    public WalletUtxoListItem(Utxo utxo) {
        txId.set(utxo.txId());
        address.set(utxo.address());
        amount.set(String.valueOf(utxo.amount()));
        confirmations.set(String.valueOf(utxo.confirmations()));
        reused.set(utxo.reused());
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
