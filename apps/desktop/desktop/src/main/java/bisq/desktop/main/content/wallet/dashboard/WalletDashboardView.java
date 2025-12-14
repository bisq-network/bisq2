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

package bisq.desktop.main.content.wallet.dashboard;

import bisq.common.data.Triple;
import bisq.desktop.common.Layout;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.main.content.wallet.WalletTxListItem;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletDashboardView extends View<VBox, WalletDashboardModel, WalletDashboardController> {
    private static final double TABLE_CELL_HEIGHT = 70;

    private final Button send, receive;
    private final Label balanceLabel, availableBalanceValueLabel, reservedFundsValueLabel, lockedFundsValueLabel;
    private final BisqTableView<WalletTxListItem> latestTxsTableView;
    private final ChangeListener<Number> tableViewHeightListener;

    public WalletDashboardView(WalletDashboardModel model, WalletDashboardController controller) {
        super(new VBox(20), model, controller);

        // Header
        Triple<HBox, Label, Label> balanceTriple = createBalanceHBox();
        HBox balanceHBox = balanceTriple.getFirst();
        balanceLabel = balanceTriple.getSecond();

        Label headlineLabel = new Label(Res.get("wallet.dashboard.headline"));
        headlineLabel.getStyleClass().addAll("dashboard-headline", "bisq-text-green");
        VBox balanceVBox = new VBox(20, headlineLabel, balanceHBox);

        Triple<HBox, Label, Label> availableBalanceTriple = createSummaryRow(Res.get("wallet.dashboard.availableBalance"), "btcoins-grey");
        HBox availableBalanceHBox = availableBalanceTriple.getFirst();
        availableBalanceValueLabel = availableBalanceTriple.getThird();

        Triple<HBox, Label, Label> reservedFundsTriple = createSummaryRow(Res.get("wallet.dashboard.reservedFunds"), "interchangeable-grey");
        HBox reservedFundsHBox = reservedFundsTriple.getFirst();
        reservedFundsValueLabel = reservedFundsTriple.getThird();

        Triple<HBox, Label, Label> lockedFundsTriple = createSummaryRow(Res.get("wallet.dashboard.lockedFunds"), "lock-icon-grey");
        HBox lockedFundsHBox = lockedFundsTriple.getFirst();
        lockedFundsValueLabel = lockedFundsTriple.getThird();

        double buttonsWidth = 220;
        send = new Button(Res.get("wallet.dashboard.sendBtc"));
        send.setDefaultButton(true);
        send.setMinWidth(buttonsWidth);
        send.setMaxWidth(buttonsWidth);
        receive = new Button(Res.get("wallet.dashboard.receiveBtc"));
        receive.getStyleClass().add("outlined-button");
        receive.setMinWidth(buttonsWidth);
        receive.setMaxWidth(buttonsWidth);

        double summaryAndButtonsBoxWidth = 500;
        HBox buttonsHBox = new HBox(receive, Spacer.fillHBox(), send);
        buttonsHBox.setMaxWidth(summaryAndButtonsBoxWidth);
        buttonsHBox.setMinWidth(summaryAndButtonsBoxWidth);

        VBox summaryAndButtonsVBox = new VBox(10, availableBalanceHBox, reservedFundsHBox, lockedFundsHBox, buttonsHBox);
        summaryAndButtonsVBox.setMaxWidth(summaryAndButtonsBoxWidth);
        summaryAndButtonsVBox.setMinWidth(summaryAndButtonsBoxWidth);
        VBox.setMargin(buttonsHBox, new Insets(30, 0, 0, 0));

        HBox headerHBox = new HBox(balanceVBox, Spacer.fillHBox(), summaryAndButtonsVBox);
        headerHBox.setPadding(new Insets(0, 50, 0, 50));

        // Latest txs
        Label latestTxsHeadline = new Label(Res.get("wallet.dashboard.latestTxs.headline"));
        latestTxsHeadline.getStyleClass().addAll("dashboard-headline", "bisq-grey-dimmed");

        latestTxsTableView = new BisqTableView<>(model.getVisibleListItems(), false);
        latestTxsTableView.getStyleClass().add("latest-txs-table");
        latestTxsTableView.setFixedCellSize(TABLE_CELL_HEIGHT);
        latestTxsTableView.hideVerticalScrollbar();
        configLatestTxsTable();

        VBox latestTxsVBox = new VBox(20, latestTxsHeadline, latestTxsTableView);
        latestTxsVBox.setPadding(new Insets(0, 50, 0, 50));

        VBox contentBox = new VBox(20);
        VBox.setMargin(headerHBox, new Insets(0, 0, 15, 0));
        contentBox.getChildren().addAll(headerHBox, getHLine(), latestTxsVBox);
        contentBox.getStyleClass().add("dashboard-bg");
        contentBox.setPadding(new Insets(50, 0, 0, 0));
        root.getChildren().addAll(contentBox);
        root.setPadding(new Insets(0, 40, 20, 40));
        root.getStyleClass().add("wallet-dashboard");
        root.setMinWidth(1050);
        VBox.setVgrow(contentBox, Priority.ALWAYS);

        tableViewHeightListener = (observable, oldValue, newValue) -> {
            double tableHeight = newValue.doubleValue();
            int numRows = (int) Math.floor((tableHeight - 35) / TABLE_CELL_HEIGHT); // 35 for the header
            int maxNumRows = Math.max(0, numRows);
            int numVisibleListItems = Math.min(model.getSortedListItems().size(), maxNumRows);
            model.getVisibleListItems().setAll(model.getSortedListItems().subList(0, numVisibleListItems));
        };
    }

    @Override
    protected void onViewAttached() {
        balanceLabel.textProperty().bind(model.getFormattedBalanceProperty());
        availableBalanceValueLabel.textProperty().bind(model.getFormattedAvailableBalanceProperty());
        reservedFundsValueLabel.textProperty().bind(model.getFormattedReservedFundsProperty());
        lockedFundsValueLabel.textProperty().bind(model.getFormattedLockedFundsProperty());

        latestTxsTableView.heightProperty().addListener(tableViewHeightListener);

        send.setOnAction(e -> controller.onSend());
        receive.setOnAction(e -> controller.onReceive());
    }

    @Override
    protected void onViewDetached() {
        balanceLabel.textProperty().unbind();
        availableBalanceValueLabel.textProperty().unbind();
        reservedFundsValueLabel.textProperty().unbind();
        lockedFundsValueLabel.textProperty().unbind();

        latestTxsTableView.heightProperty().removeListener(tableViewHeightListener);

        send.setOnAction(null);
        receive.setOnAction(null);
    }

    private Triple<HBox, Label, Label> createBalanceHBox() {
        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("bisq-text-headline-3");

        Label codeLabel = new Label("BTC");
        codeLabel.getStyleClass().addAll("bisq-text-12");

        HBox hBox = new HBox(10, valueLabel, codeLabel);
        hBox.setAlignment(Pos.BASELINE_CENTER);
        return new Triple<>(hBox, valueLabel, codeLabel);
    }

    private Triple<HBox, Label, Label> createSummaryRow(String title, String imageId) {
        Label titleLabel = new Label(title);
        titleLabel.setGraphic(ImageUtil.getImageViewById(imageId));
        titleLabel.getStyleClass().add("summary-title");
        titleLabel.setGraphicTextGap(15);

        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("summary-value");

        Label codeLabel = new Label("BTC");
        codeLabel.getStyleClass().addAll("summary-code");

        HBox hBox = new HBox(titleLabel, Spacer.fillHBox(), valueLabel, codeLabel);
        hBox.setAlignment(Pos.BASELINE_CENTER);
        HBox.setMargin(valueLabel, new Insets(0, 10, 0, 0));
        return new Triple<>(hBox, titleLabel, valueLabel);
    }

    private Region getHLine() {
        Region line = Layout.hLine();
        line.setPrefWidth(30);
        return line;
    }

    private void configLatestTxsTable() {
        latestTxsTableView.getColumns().add(new BisqTableColumn.Builder<WalletTxListItem>()
                .title(Res.get("wallet.txs.date"))
                .left()
                .minWidth(100)
                .valueSupplier(WalletTxListItem::getDateTimeString)
                .build());

        latestTxsTableView.getColumns().add(new BisqTableColumn.Builder<WalletTxListItem>()
                .title(Res.get("wallet.txs.type"))
                .minWidth(70)
                .left()
                .valueSupplier(WalletTxListItem::getType)
                .build());

        latestTxsTableView.getColumns().add(new BisqTableColumn.Builder<WalletTxListItem>()
                .title(Res.get("wallet.txs.address"))
                .minWidth(180)
                .left()
                .valueSupplier(WalletTxListItem::getDestinationAddress)
                .build());

        latestTxsTableView.getColumns().add(new BisqTableColumn.Builder<WalletTxListItem>()
                .title(Res.get("wallet.txs.txId"))
                .minWidth(280)
                .left()
                .valueSupplier(WalletTxListItem::getTxId)
                .build());

        latestTxsTableView.getColumns().add(new BisqTableColumn.Builder<WalletTxListItem>()
                .title(Res.get("wallet.txs.amount"))
                .minWidth(70)
                .valueSupplier(WalletTxListItem::getAmountAsString)
                .build());

        latestTxsTableView.getColumns().add(new BisqTableColumn.Builder<WalletTxListItem>()
                .title(Res.get("wallet.txs.confirmations"))
                .minWidth(70)
                .valueSupplier(WalletTxListItem::getNumConfirmationsAsString)
                .right()
                .build());
    }
}
