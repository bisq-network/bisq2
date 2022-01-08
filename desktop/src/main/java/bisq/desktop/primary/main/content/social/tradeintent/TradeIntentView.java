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

import java.util.UUID;

@Slf4j
public class TradeIntentView extends View<BisqGridPane, TradeIntentModel, TradeIntentController> {
    private final BisqTableView<TradeIntentListItem> dataTableView;
    private ChangeListener<TradeIntentListItem> dataTableSelectedItemListener;

    public TradeIntentView(TradeIntentModel model, TradeIntentController controller) {
        super(new BisqGridPane(), model, controller);

        root.setPadding(new Insets(20,20,20,0));
        
        root.startSection(Res.network.get("addData.title"));
        TextField dataContentTextField = root.addTextField(Res.network.get("addData.content"), "Test data");
        TextField idTextField = root.addTextField(Res.network.get("addData.id"), UUID.randomUUID().toString().substring(0, 8));
        Pair<Button, Label> addDataButtonPair = root.addButton(Res.network.get("addData.add"));
        Button addDataButton = addDataButtonPair.first();
        Label label = addDataButtonPair.second();
        addDataButton.setOnAction(e -> {
            addDataButton.setDisable(true);
            label.textProperty().unbind();
            label.setText("...");
            addDataButton.setDisable(false);
            StringProperty result = controller.addData(dataContentTextField.getText(), idTextField.getText());
            label.textProperty().bind(result);
        });
        root.endSection();

        root.startSection(Res.network.get("table.data.title"));
        dataTableView = new BisqTableView<>(model.getSortedTradeIntentListItems());
        dataTableView.setMinHeight(200);
        root.addTableView(dataTableView);
        configDataTableView();
        root.endSection();

        dataTableSelectedItemListener = (observable, oldValue, newValue) -> {
        };
    }

    @Override
    public void onViewAttached() {
        dataTableView.getSelectionModel().selectedItemProperty().addListener(dataTableSelectedItemListener);
    }

    @Override
    protected void onViewDetached() {
        dataTableView.getSelectionModel().selectedItemProperty().removeListener(dataTableSelectedItemListener);
    }

    private void configDataTableView() {
        var dateColumn = new BisqTableColumn.Builder<TradeIntentListItem>()
                .title(Res.network.get("table.data.header.received"))
                .minWidth(180)
                .maxWidth(180)
                .valueSupplier(TradeIntentListItem::getReceived)
                .comparator(TradeIntentListItem::compareDate)
                .build();
        dataTableView.getColumns().add(dateColumn);
        dateColumn.setSortType(TableColumn.SortType.DESCENDING);
        dataTableView.getSortOrder().add(dateColumn);
      
        dataTableView.getColumns().add(new BisqTableColumn.Builder<TradeIntentListItem>()
                .title(Res.network.get("table.data.header.content"))
                .minWidth(320)
                .valueSupplier(TradeIntentListItem::getContent)
                .build());
        dataTableView.getColumns().add(new BisqTableColumn.Builder<TradeIntentListItem>()
                .minWidth(320)
                .title(Res.network.get("table.data.header.nodeId"))
                .valueSupplier(TradeIntentListItem::getNodeId)
                .build());
    }

}
