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

package bisq.desktop.primary.main.content.social.tradeintent;

import bisq.common.data.Pair;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.BisqGridPane;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.i18n.Res;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeIntentView extends View<BisqGridPane, TradeIntentModel, TradeIntentController> {
    private final BisqTableView<TradeIntentListItem> tableView;
    private ChangeListener<TradeIntentListItem> dataTableSelectedItemListener;

    public TradeIntentView(TradeIntentModel model, TradeIntentController controller) {
        super(new BisqGridPane(), model, controller);

        root.setPadding(new Insets(20, 20, 20, 0));

        root.startSection(Res.offerbook.get("tradeIntent.create.title"));
        TextField askTextField = root.addTextField(Res.offerbook.get("tradeIntent.create.ask"), "I want 0.01 BTC");
        TextField bidTextField = root.addTextField(Res.offerbook.get("tradeIntent.create.bid"), "Pay EUR via SEPA at market rate");
        Pair<Button, Label> addDataButtonPair = root.addButton(Res.common.get("publish"));
        Button addDataButton = addDataButtonPair.first();
        Label label = addDataButtonPair.second();
        addDataButton.setOnAction(e -> {
            addDataButton.setDisable(true);
            label.textProperty().unbind();
            label.setText("...");
            addDataButton.setDisable(false);
            StringProperty result = controller.addData(askTextField.getText(), bidTextField.getText());
            label.textProperty().bind(result);
        });
        root.endSection();

        root.startSection(Res.offerbook.get("tradeIntent.table.title"));
        tableView = new BisqTableView<>(model.getSortedTradeIntentListItems());
        tableView.setMinHeight(200);
        root.addTableView(tableView);
        configDataTableView();
        root.endSection();

        dataTableSelectedItemListener = (observable, oldValue, newValue) -> {
        };
    }

    @Override
    public void onViewAttached() {
        tableView.getSelectionModel().selectedItemProperty().addListener(dataTableSelectedItemListener);
    }

    @Override
    protected void onViewDetached() {
        tableView.getSelectionModel().selectedItemProperty().removeListener(dataTableSelectedItemListener);
    }

    private void configDataTableView() {
        var dateColumn = new BisqTableColumn.Builder<TradeIntentListItem>()
                .title(Res.common.get("date"))
                .minWidth(180)
                .maxWidth(180)
                .valueSupplier(TradeIntentListItem::getDateString)
                .comparator(TradeIntentListItem::compareDate)
                .build();
        tableView.getColumns().add(dateColumn);
        dateColumn.setSortType(TableColumn.SortType.DESCENDING);
        tableView.getSortOrder().add(dateColumn);

        tableView.getColumns().add(new BisqTableColumn.Builder<TradeIntentListItem>()
                .title(Res.common.get("ask"))
                .minWidth(320)
                .valueSupplier(TradeIntentListItem::getAsk)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<TradeIntentListItem>()
                .minWidth(320)
                .title(Res.common.get("bid"))
                .valueSupplier(TradeIntentListItem::getBid)
                .build());
    }

}
