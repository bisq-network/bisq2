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

package bisq.desktop.main.content.wallet.txs;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.RichTableView;
import bisq.desktop.main.content.wallet.WalletTxListItem;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;

@Slf4j
public class WalletTxsView extends View<VBox, WalletTxsModel, WalletTxsController> {
    private static final double SIDE_PADDING = 40;

    private final RichTableView<WalletTxListItem> richTableView;
    private final ToggleButton allTxToggleButton, reservedFundsToggleButton, lockedFundsToggleButton;
    private final ToggleGroup toggleGroup;
    private final ChangeListener<Toggle> toggleChangeListener;
    private Subscription selectedFilterPin;

    public WalletTxsView(WalletTxsModel model, WalletTxsController controller) {
        super(new VBox(), model, controller);

        allTxToggleButton = new ToggleButton(Res.get("wallet.txs.subheader.allTx"));
        reservedFundsToggleButton = new ToggleButton(Res.get("wallet.txs.subheader.reservedFunds"));
        lockedFundsToggleButton = new ToggleButton(Res.get("wallet.txs.subheader.lockedFunds"));
        toggleGroup = new ToggleGroup();
        toggleGroup.getToggles().addAll(allTxToggleButton, reservedFundsToggleButton, lockedFundsToggleButton);
        HBox toggleButtonHBox = new HBox(3, allTxToggleButton, reservedFundsToggleButton, lockedFundsToggleButton);
        toggleButtonHBox.getStyleClass().add("toggle-button-hbox");

        richTableView = new RichTableView<>(
                model.getSortedList(),
                Res.get("wallet.txs"));
        richTableView.getSearchBox().setManaged(true);
        richTableView.getSearchBox().setVisible(true);
        richTableView.getSubheaderBox().getChildren().setAll(toggleButtonHBox, richTableView.getSearchBox(), Spacer.fillHBox());
        configTableView();

        root.getChildren().add(richTableView);
        root.setPadding(new Insets(0, SIDE_PADDING, 0, SIDE_PADDING));

        toggleChangeListener = (observable, oldValue, newValue) -> {
            if (newValue == null) {
                updateSelectedFilter(model.getSelectedFilter().get());
            }
        };
    }

    @Override
    protected void onViewAttached() {
        richTableView.initialize();

        richTableView.getSearchBox().textProperty().bindBidirectional(model.getSearchText());

        selectedFilterPin = EasyBind.subscribe(model.getSelectedFilter(), this::updateSelectedFilter);

        allTxToggleButton.setOnAction(e -> model.getSelectedFilter().set(TxsFilter.ALL));
        reservedFundsToggleButton.setOnAction(e -> model.getSelectedFilter().set(TxsFilter.RESERVED_FUNDS));
        lockedFundsToggleButton.setOnAction(e -> model.getSelectedFilter().set(TxsFilter.LOCKED_FUNDS));

        toggleGroup.selectedToggleProperty().addListener(toggleChangeListener);
    }

    @Override
    protected void onViewDetached() {
        richTableView.dispose();

        richTableView.getSearchBox().textProperty().unbindBidirectional(model.getSearchText());

        selectedFilterPin.unsubscribe();

        allTxToggleButton.setOnAction(null);
        reservedFundsToggleButton.setOnAction(null);
        lockedFundsToggleButton.setOnAction(null);

        toggleGroup.selectedToggleProperty().removeListener(toggleChangeListener);
    }

    private void configTableView() {
        richTableView.getColumns().add(richTableView.getTableView().getSelectionMarkerColumn());

        BisqTableColumn<WalletTxListItem> dateColumn = new BisqTableColumn.Builder<WalletTxListItem>()
                .title(Res.get("wallet.txs.date"))
                .left()
                .minWidth(100)
                .comparator(Comparator.comparing(WalletTxListItem::getDate))
                .valueSupplier(WalletTxListItem::getDateTimeString)
                .sortType(TableColumn.SortType.DESCENDING)
                .build();
        richTableView.getColumns().add(dateColumn);
        richTableView.getSortOrder().add(dateColumn);

        richTableView.getColumns().add(new BisqTableColumn.Builder<WalletTxListItem>()
                .title(Res.get("wallet.txs.trade"))
                .minWidth(60)
                .left()
                .valueSupplier(WalletTxListItem::getTrade)
                .isSortable(true)
                .build());

        richTableView.getColumns().add(new BisqTableColumn.Builder<WalletTxListItem>()
                .title(Res.get("wallet.txs.type"))
                .minWidth(70)
                .left()
                .valueSupplier(WalletTxListItem::getType)
                .isSortable(true)
                .build());

        richTableView.getColumns().add(new BisqTableColumn.Builder<WalletTxListItem>()
                .title(Res.get("wallet.txs.address"))
                .minWidth(180)
                .left()
                .valueSupplier(WalletTxListItem::getDestinationAddress)
                .isSortable(true)
                .build());

        richTableView.getColumns().add(new BisqTableColumn.Builder<WalletTxListItem>()
                .title(Res.get("wallet.txs.txId"))
                .minWidth(200)
                .left()
                .valueSupplier(WalletTxListItem::getTxId)
                .isSortable(true)
                .build());

        richTableView.getColumns().add(new BisqTableColumn.Builder<WalletTxListItem>()
                .title(Res.get("wallet.txs.amount"))
                .minWidth(120)
                .valueSupplier(WalletTxListItem::getAmountAsString)
                .comparator(Comparator.comparing(WalletTxListItem::getAmount))
                .sortType(TableColumn.SortType.DESCENDING)
                .build());

        richTableView.getColumns().add(new BisqTableColumn.Builder<WalletTxListItem>()
                .title(Res.get("wallet.txs.confirmations"))
                .minWidth(120)
                .valueSupplier(WalletTxListItem::getNumConfirmationsAsString)
                .comparator(Comparator.comparing(WalletTxListItem::getNumConfirmations))
                .sortType(TableColumn.SortType.DESCENDING)
                .right()
                .build());
    }

    private void updateSelectedFilter(TxsFilter filter) {
        if (filter == TxsFilter.ALL) {
            allTxToggleButton.setSelected(true);
        } else if (filter == TxsFilter.RESERVED_FUNDS) {
            reservedFundsToggleButton.setSelected(true);
        } else if (filter == TxsFilter.LOCKED_FUNDS) {
            lockedFundsToggleButton.setSelected(true);
        }
    }
}
