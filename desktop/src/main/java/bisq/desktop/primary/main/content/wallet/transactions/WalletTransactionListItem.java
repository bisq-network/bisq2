package bisq.desktop.primary.main.content.wallet.transactions;

import bisq.desktop.components.table.TableItem;
import bisq.wallets.model.Transaction;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class WalletTransactionListItem implements TableItem {
    private final StringProperty txId = new SimpleStringProperty(this, "wallet.column.txId");
    private final StringProperty address = new SimpleStringProperty(this, "address");
    private final StringProperty amount = new SimpleStringProperty(this, "amount");
    private final StringProperty confirmations = new SimpleStringProperty(this, "wallet.column.confirmations");

    public WalletTransactionListItem(Transaction transaction) {
        txId.set(transaction.txId());
        address.set(transaction.address());
        amount.set(String.valueOf(transaction.amount()));
        confirmations.set(String.valueOf(transaction.confirmations()));
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
