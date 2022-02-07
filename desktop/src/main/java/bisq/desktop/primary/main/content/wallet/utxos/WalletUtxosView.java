package bisq.desktop.primary.main.content.wallet.utxos;

import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class WalletUtxosView extends View<VBox, WalletUtxosModel, WalletUtxosController> {
    private final TableView<WalletUtxoListItem> tableView;

    public WalletUtxosView(WalletUtxosModel model, WalletUtxosController controller) {
        super(new VBox(), model, controller);

        tableView = new TableView<>(model.listItems);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        createAndBindColumns();

        root.getChildren().add(tableView);
    }

    private void createAndBindColumns() {
        TableColumn<WalletUtxoListItem, String> txIdColumn = new TableColumn<>(Res.get("wallet.column.txId"));
        txIdColumn.setCellValueFactory(param -> param.getValue().txIdProperty());
        tableView.getColumns().add(txIdColumn);

        TableColumn<WalletUtxoListItem, String> addressColumn = new TableColumn<>(Res.get("address"));
        addressColumn.setCellValueFactory(param -> param.getValue().addressProperty());
        tableView.getColumns().add(addressColumn);

        TableColumn<WalletUtxoListItem, String> amountColumn = new TableColumn<>(Res.get("amount"));
        amountColumn.setCellValueFactory(param -> param.getValue().amountProperty());
        tableView.getColumns().add(amountColumn);

        TableColumn<WalletUtxoListItem, String> confirmationsColumn = new TableColumn<>(Res.get("wallet.column.confirmations"));
        confirmationsColumn.setCellValueFactory(param -> param.getValue().confirmationsProperty());
        tableView.getColumns().add(confirmationsColumn);

        TableColumn<WalletUtxoListItem, Boolean> reusedColumn = new TableColumn<>(Res.get("wallet.column.reused"));
        reusedColumn.setCellValueFactory(param -> param.getValue().getReusedProperty());
        tableView.getColumns().add(reusedColumn);
    }
}
