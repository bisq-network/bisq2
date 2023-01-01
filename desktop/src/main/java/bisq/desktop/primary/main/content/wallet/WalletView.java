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

package bisq.desktop.primary.main.content.wallet;

import bisq.common.data.Triple;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.i18n.Res;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletView extends View<VBox, WalletModel, WalletController> {

    private final Label balanceLabel;
    private final BisqTableView<WalletTransactionListItem> tableView;
    private final TextField receiveAddressTextField = new TextField();

    public WalletView(WalletModel model, WalletController controller) {
        super(new VBox(20), model, controller);

        Triple<VBox, Label, Label> balanceTriple = createBalanceBox(Res.get("wallet.balance"));
        VBox balanceBox = balanceTriple.getFirst();
        balanceLabel = balanceTriple.getSecond();

        HBox topHBox = new HBox(100);
        Pane receiveAddressBox = createReceiveAddressBox();
        topHBox.getChildren().addAll(balanceBox, receiveAddressBox);

        Button withdrawButton = new Button(Res.get("wallet.withdrawFromWallet"));
        withdrawButton.setOnMouseClicked(event -> controller.onWithdrawButtonClicked());

        Label tableHeader = new Label(Res.get("wallet.transaction.history"));
        tableHeader.getStyleClass().add("bisq-text-headline-2");

        tableView = new BisqTableView<>(new SortedList<>(model.getTransactionHistoryList()));
        VBox.setVgrow(tableView, Priority.ALWAYS);
        addColumns();

        root.getChildren().addAll(topHBox, withdrawButton, tableHeader, tableView);
    }

    @Override
    protected void onViewAttached() {
        balanceLabel.textProperty().bind(model.getFormattedBalanceProperty());
        receiveAddressTextField.textProperty().bind(model.getReceiveAddressProperty());
    }

    @Override
    protected void onViewDetached() {
        balanceLabel.textProperty().unbind();
        receiveAddressTextField.textProperty().unbind();
    }

    private Triple<VBox, Label, Label> createBalanceBox(String title) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().addAll("bisq-text-7", "bisq-text-grey-9");

        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("bisq-text-headline-3");

        Label codeLabel = new Label("BTC");
        codeLabel.getStyleClass().addAll("bisq-text-12");

        HBox hBox = new HBox(9, valueLabel, codeLabel);
        hBox.setAlignment(Pos.BASELINE_RIGHT);
        VBox box = new VBox(titleLabel, hBox);
        box.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(box, Priority.ALWAYS);
        return new Triple<>(box, valueLabel, codeLabel);
    }

    private Pane createReceiveAddressBox() {
        VBox rootVBox = new VBox();
        Label receiveAddressLabel = new Label(Res.get("wallet.receive.address"));

        receiveAddressTextField.setEditable(false);
        receiveAddressTextField.setMinWidth(311);

        Button receiveAddressCopyButton = new Button(Res.get("wallet.receive.copy"));
        receiveAddressCopyButton.setOnMouseClicked(event -> ClipboardUtil.copyToClipboard(model.getReceiveAddress()));

        HBox receiveAddressHBox = new HBox(receiveAddressTextField, receiveAddressCopyButton);
        receiveAddressHBox.setAlignment(Pos.CENTER_LEFT);

        rootVBox.getChildren().addAll(receiveAddressLabel, receiveAddressHBox);
        return rootVBox;
    }

    private void addColumns() {
        tableView.getColumns().add(new BisqTableColumn.Builder<WalletTransactionListItem>()
                .title(Res.get("wallet.transaction.history.description"))
                .minWidth(150)
                .valuePropertySupplier(WalletTransactionListItem::descriptionProperty)
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
