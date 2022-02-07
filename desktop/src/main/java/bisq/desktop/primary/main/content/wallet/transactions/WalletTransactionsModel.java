package bisq.desktop.primary.main.content.wallet.transactions;

import bisq.desktop.common.view.Model;
import bisq.wallets.model.Transaction;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

import java.util.List;

public class WalletTransactionsModel implements Model {
    @Getter
    private final ObservableList<WalletTransactionListItem> listItems = FXCollections.observableArrayList();

    public void addTransactions(List<Transaction> transactions) {
        transactions.stream()
                .map(WalletTransactionListItem::new)
                .forEach(listItems::add);
    }
}
