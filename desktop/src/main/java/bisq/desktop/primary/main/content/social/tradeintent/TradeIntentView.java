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
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.primary.main.content.social.components.UserProfileDisplay;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TradeIntentView extends View<VBox, TradeIntentModel, TradeIntentController> {
    private final BisqTableView<TradeIntentListItem> tableView;
    private final BisqGridPane gridPane;
    private final Label addDataResultLabel;
    private final ChangeListener<TradeIntentListItem> dataTableSelectedItemListener;
    private Subscription selectedUserIdSubscription;

    public TradeIntentView(TradeIntentModel model, TradeIntentController controller, UserProfileDisplay.View userProfileView) {
        super(new VBox(), model, controller);
        root.setSpacing(20);

        gridPane = new BisqGridPane();
        gridPane.setPadding(new Insets(20, 20, 20, 0));

        Pane userProfileViewRoot = userProfileView.getRoot();
        StackPane.setAlignment(userProfileViewRoot, Pos.TOP_RIGHT);
        userProfileViewRoot.setPadding(new Insets(10, 0, 0, 10));
        root.getChildren().addAll(userProfileViewRoot, gridPane);

        gridPane.startSection(Res.get("tradeIntent.create.title"));
        TextField askTextField = gridPane.addTextField(Res.get("tradeIntent.create.ask"), "I want 0.01 BTC");
        TextField bidTextField = gridPane.addTextField(Res.get("tradeIntent.create.bid"), "Pay EUR via SEPA at market rate");
        Pair<BisqButton, Label> addDataButtonPair = gridPane.addButton(Res.get("publish"));
        Button addDataButton = addDataButtonPair.first();
        addDataResultLabel = addDataButtonPair.second();
        addDataButton.setOnAction(e -> {
            controller.onCreateTradeIntent(askTextField.getText(), bidTextField.getText());

        });
        gridPane.endSection();

        gridPane.startSection(Res.get("tradeIntent.table.title"));
        tableView = new BisqTableView<>(model.getSortedItems());
        tableView.setMinHeight(200);
        gridPane.addTableView(tableView);
        configDataTableView();
        gridPane.endSection();

        dataTableSelectedItemListener = (observable, oldValue, newValue) -> {
        };
    }

    @Override
    public void onViewAttached() {
        tableView.getSelectionModel().selectedItemProperty().addListener(dataTableSelectedItemListener);
        addDataResultLabel.textProperty().bind(model.getAddDataResultProperty());
    }

    @Override
    protected void onViewDetached() {
        tableView.getSelectionModel().selectedItemProperty().removeListener(dataTableSelectedItemListener);
        addDataResultLabel.textProperty().unbind();
    }

    private void configDataTableView() {
        var dateColumn = new BisqTableColumn.Builder<TradeIntentListItem>()
                .title(Res.get("date"))
                .minWidth(180)
                .maxWidth(180)
                .valueSupplier(TradeIntentListItem::getDateString)
                .comparator(TradeIntentListItem::compareDate)
                .build();
        tableView.getColumns().add(dateColumn);
        dateColumn.setSortType(TableColumn.SortType.DESCENDING);
        tableView.getSortOrder().add(dateColumn);

        tableView.getColumns().add(new BisqTableColumn.Builder<TradeIntentListItem>()
                .title(Res.get("social.userName"))
                .minWidth(120)
                .valueSupplier(TradeIntentListItem::getUserName)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<TradeIntentListItem>()
                .title(Res.get("ask"))
                .minWidth(150)
                .valueSupplier(TradeIntentListItem::getAsk)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<TradeIntentListItem>()
                .minWidth(150)
                .title(Res.get("bid"))
                .valueSupplier(TradeIntentListItem::getBid)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<TradeIntentListItem>()
                .minWidth(80)
                .valueSupplier(model::getActionButtonTitle)
                .cellFactory(BisqTableColumn.CellFactory.BUTTON)
                .actionHandler(controller::onActionButtonClicked)
                .build());
    }
}
