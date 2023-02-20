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

package bisq.desktop.primary.main.content.wallet.txs;

import bisq.desktop.common.view.View;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

@Slf4j
public class WalletTxsView extends View<VBox, WalletTxsModel, WalletTxsController> {
    private final BisqTableView<WalletTransactionListItem> tableView;

    public WalletTxsView(WalletTxsModel model, WalletTxsController controller) {
        super(new VBox(20), model, controller);

        root.setPadding(new Insets(40, 0, 0, 0));

        tableView = new BisqTableView<>(model.getSortedList());
        tableView.setMinHeight(300);
        // Triggers to fill the available height
        tableView.setPrefHeight(2000);
        configTableView();

        root.getChildren().add(tableView);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }

    private void configTableView() {
        BisqTableColumn<WalletTransactionListItem> column = new BisqTableColumn.Builder<WalletTransactionListItem>()
                .title(Res.get("date"))
                .minWidth(200)
                .valueSupplier(WalletTransactionListItem::getDateAsString)
                .isSortable(true)
                .isFirst()
                .build();
        column.setComparator(Comparator.comparing(WalletTransactionListItem::getDateAsString));
        column.setSortType(TableColumn.SortType.DESCENDING);
        tableView.getSortOrder().add(column);
        tableView.getColumns().add(column);

        tableView.getColumns().add(new BisqTableColumn.Builder<WalletTransactionListItem>()
                .title(Res.get("wallet.txs.txId"))
                .minWidth(200)
                .valueSupplier(WalletTransactionListItem::getTxId)
                .isSortable(true)
                .build());

        column = new BisqTableColumn.Builder<WalletTransactionListItem>()
                .title(Res.get("wallet.txs.amount"))
                .minWidth(120)
                .valueSupplier(WalletTransactionListItem::getAmountAsString)
                .isSortable(true)
                .build();
        column.setComparator(Comparator.comparing(WalletTransactionListItem::getAmount));
        column.setSortType(TableColumn.SortType.DESCENDING);
        tableView.getColumns().add(column);

        column = new BisqTableColumn.Builder<WalletTransactionListItem>()
                .title(Res.get("wallet.txs.confirmations"))
                .minWidth(120)
                .valueSupplier(WalletTransactionListItem::getConfirmationsAsString)
                .isSortable(true)
                .isLast()
                .build();
        column.setComparator(Comparator.comparing(WalletTransactionListItem::getConfirmations));
        column.setSortType(TableColumn.SortType.DESCENDING);
        tableView.getColumns().add(column);
    }
}
