package bisq.desktop.primary.main.content.wallet.transactions;

import bisq.desktop.common.view.View;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.i18n.Res;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class WalletTransactionsView extends View<VBox, WalletTransactionsModel, WalletTransactionsController> {
    private final BisqTableView<WalletTransactionListItem> tableView;

    public WalletTransactionsView(WalletTransactionsModel model, WalletTransactionsController controller) {
        super(new VBox(), model, controller);

        tableView = new BisqTableView<>(model.getSortedList());
        VBox.setVgrow(tableView, Priority.ALWAYS);

        addColumns();
        root.getChildren().addAll(tableView);
    }

    private void addColumns() {
        tableView.getColumns().add(new BisqTableColumn.Builder<WalletTransactionListItem>()
                .title(Res.get("wallet.column.txId"))
                .minWidth(150)
                .valuePropertySupplier(WalletTransactionListItem::txIdProperty)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<WalletTransactionListItem>()
                .title(Res.get("address"))
                .minWidth(150)
                .valuePropertySupplier(WalletTransactionListItem::addressProperty)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<WalletTransactionListItem>()
                .title(Res.get("amount"))
                .minWidth(120)
                .valuePropertySupplier(WalletTransactionListItem::amountProperty)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<WalletTransactionListItem>()
                .title(Res.get("wallet.column.confirmations"))
                .minWidth(80)
                .valuePropertySupplier(WalletTransactionListItem::confirmationsProperty)
                .build());
    }
}
