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

import bisq.desktop.common.view.TabViewChild;
import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class WalletUtxosView extends View<VBox, WalletUtxosModel, WalletUtxosController> implements TabViewChild {
    private final TableView<WalletUtxoListItem> tableView;

    public WalletUtxosView(WalletUtxosModel model, WalletUtxosController controller) {
        super(new VBox(), model, controller);

        tableView = new TableView<>(model.listItems);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        createAndBindColumns();

        root.getChildren().add(tableView);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
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
