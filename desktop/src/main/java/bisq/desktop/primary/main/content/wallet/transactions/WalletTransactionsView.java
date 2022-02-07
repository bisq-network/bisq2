package bisq.desktop.primary.main.content.wallet.transactions;

import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class WalletTransactionsView extends View<VBox, WalletTransactionsModel, WalletTransactionsController> {
    private final TableView<WalletTransactionListItem> tableView;

    public WalletTransactionsView(WalletTransactionsModel model, WalletTransactionsController controller) {
        super(new VBox(), model, controller);

        tableView = new TableView<>(model.getListItems());
        VBox.setVgrow(tableView, Priority.ALWAYS);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        createAndBindColumns();
        root.getChildren().addAll(tableView);
    }

    private void createAndBindColumns() {
        TableColumn<WalletTransactionListItem, String> txIdColumn = new TableColumn<>(Res.get("wallet.column.txId"));
        txIdColumn.setCellValueFactory(param -> param.getValue().txIdProperty());
        tableView.getColumns().add(txIdColumn);

        TableColumn<WalletTransactionListItem, String> addressColumn = new TableColumn<>(Res.get("address"));
        addressColumn.setCellValueFactory(param -> param.getValue().addressProperty());
        tableView.getColumns().add(addressColumn);

        TableColumn<WalletTransactionListItem, String> amountColumn = new TableColumn<>(Res.get("amount"));
        amountColumn.setCellValueFactory(param -> param.getValue().amountProperty());
        tableView.getColumns().add(amountColumn);

        TableColumn<WalletTransactionListItem, String> confirmationsColumn = new TableColumn<>(Res.get("wallet.column.confirmations"));
        confirmationsColumn.setCellValueFactory(param -> param.getValue().confirmationsProperty());
        tableView.getColumns().add(confirmationsColumn);
    }
}
