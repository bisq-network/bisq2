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

package bisq.desktop.main.content.bisq_easy.open_trades.trade_state.states;

import bisq.common.data.Pair;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.i18n.Res;
import bisq.user.profile.UserProfile;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import lombok.Getter;

import java.util.Optional;

public class TradeCompletedTable extends VBox {
    private final GridPane headerGridPane, bodyGridPane;
    // @Getter
    // private final BisqMenuItem  copyTxIdButton;
    @Getter
    private final Button blockExplorerButton, copyTxIdButton;

    public TradeCompletedTable() {
        Label title = new Label(Res.get("bisqEasy.tradeCompleted.title"));
        title.setGraphic(ImageUtil.getImageViewById("complete-trade"));
        title.setGraphicTextGap(30);
        title.getStyleClass().add("trade-completed-title");

        Label tableTitle = new Label(Res.get("bisqEasy.tradeCompleted.tableTitle").toUpperCase());
        tableTitle.getStyleClass().add("trade-completed-table-title");

        // Header
        headerGridPane = GridPaneUtil.getGridPane(10, 10, new Insets(0));
        headerGridPane.setMouseTransparent(true);
        GridPaneUtil.setGridPaneMultiColumnsConstraints(headerGridPane, 5);

        // Body
        bodyGridPane = GridPaneUtil.getGridPane(10, 10, new Insets(0));
        ColumnConstraints titleCol = new ColumnConstraints();
        titleCol.setPercentWidth(25);
        bodyGridPane.getColumnConstraints().add(titleCol);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setPercentWidth(75);
        bodyGridPane.getColumnConstraints().add(valueCol);

        blockExplorerButton = BisqIconButton.createExternalLinkButton(Res.get("bisqEasy.tradeCompleted.body.txId.explorerLink"), Res.get("bisqEasy.tradeCompleted.body.txId.tooltip"));
        blockExplorerButton.getStyleClass().addAll("medium-text", "text-fill-grey-dimmed");
        blockExplorerButton.getGraphic().getStyleClass().add("text-fill-grey-dimmed");
        blockExplorerButton.setVisible(false);
        blockExplorerButton.setManaged(false);

        copyTxIdButton = BisqIconButton.createCopyIconButton(BisqTooltip.Style.DEFAULT, "text-fill-grey-dimmed");

       /* blockExplorerButton = new BisqMenuItem(Res.get("bisqEasy.tradeCompleted.body.txId.explorerLink")); // TODO: Add icon for external link instead
         blockExplorerButton.setTooltip(Res.get("bisqEasy.tradeCompleted.body.txId.tooltip"));*/

      /*  copyTxIdButton = new BisqMenuItem("copy-grey", "copy-white");
        copyTxIdButton.useIconOnly();
        copyTxIdButton.setTooltip(Res.get("action.copyToClipboard"));*/

        Region line1 = getLine();
        Region line2 = getLine();
        Region line3 = getLine();
        VBox.setMargin(title, new Insets(10, 0, 20, 0));
        VBox.setMargin(line1, new Insets(10, 0, 5, 0));
        VBox.setMargin(line2, new Insets(30, 0, 5, 0));
        VBox.setMargin(line3, new Insets(5, 0, 10, 0));
        getChildren().addAll(title, tableTitle, line1, headerGridPane, line2, bodyGridPane, line3);
        setAlignment(Pos.CENTER);
    }

    public void initialize(UserProfile userProfile,
                           boolean isBuyer,
                           String btcAmount,
                           String fiatAmount,
                           String fiatCurrency,
                           String paymentMethodUsed,
                           String tradeIdUsed,
                           String tradeDate,
                           String tradePriceUsed,
                           String tradePriceSymbolUsed,
                           Optional<Pair<String, String>> txIdDescriptionAndValue) {
        // Header
        int rowTitle = 0;
        int rowValue = 1;

        int col = 0;
        Label tradeWith = new Label(Res.get("bisqEasy.tradeCompleted.header.tradeWith").toUpperCase());
        tradeWith.getStyleClass().addAll("medium-text", "text-fill-grey-dimmed");
        UserProfileDisplay tradeWithValue = new UserProfileDisplay(userProfile);
        headerGridPane.add(tradeWith, col, rowTitle);
        headerGridPane.add(tradeWithValue, col, rowValue);

        ++col;
        Label myDirection = isBuyer
                ? new Label(Res.get("bisqEasy.tradeCompleted.header.myDirection.buyer").toUpperCase())
                : new Label(Res.get("bisqEasy.tradeCompleted.header.myDirection.seller").toUpperCase());
        myDirection.getStyleClass().addAll("medium-text", "text-fill-grey-dimmed");
        Label myDirectionValue = new Label(btcAmount);
        myDirectionValue.getStyleClass().add("medium-text");
        Label directionBtc = new Label(Res.get("bisqEasy.tradeCompleted.header.myDirection.btc").toUpperCase());
        directionBtc.getStyleClass().addAll("small-text", "text-fill-grey-dimmed");
        HBox btcBox = new HBox(5, myDirectionValue, directionBtc);
        btcBox.setAlignment(Pos.BASELINE_LEFT);
        headerGridPane.add(myDirection, col, rowTitle);
        headerGridPane.add(btcBox, col, rowValue);

        ++col;
        Label myOutcome = isBuyer
                ? new Label(Res.get("bisqEasy.tradeCompleted.header.myOutcome.buyer").toUpperCase())
                : new Label(Res.get("bisqEasy.tradeCompleted.header.myOutcome.seller").toUpperCase());
        myOutcome.getStyleClass().addAll("medium-text", "text-fill-grey-dimmed");
        Label myOutcomeValue = new Label(fiatAmount);
        myOutcomeValue.getStyleClass().add("medium-text");
        Label fiat = new Label(fiatCurrency.toUpperCase());
        fiat.getStyleClass().addAll("small-text", "text-fill-grey-dimmed");
        HBox fiatBox = new HBox(5, myOutcomeValue, fiat);
        fiatBox.setAlignment(Pos.BASELINE_LEFT);
        headerGridPane.add(myOutcome, col, rowTitle);
        headerGridPane.add(fiatBox, col, rowValue);

        ++col;
        Label paymentMethod = new Label(Res.get("bisqEasy.tradeCompleted.header.paymentMethod").toUpperCase());
        paymentMethod.getStyleClass().addAll("medium-text", "text-fill-grey-dimmed");
        Label paymentMethodValue = new Label(paymentMethodUsed);
        paymentMethodValue.getStyleClass().add("medium-text");
        GridPane.setValignment(paymentMethodValue, VPos.TOP);
        headerGridPane.add(paymentMethod, col, rowTitle);
        headerGridPane.add(paymentMethodValue, col, rowValue);

        ++col;
        Label tradeId = new Label(Res.get("bisqEasy.tradeCompleted.header.tradeId").toUpperCase());
        tradeId.getStyleClass().addAll("medium-text", "text-fill-grey-dimmed");
        Label tradeIdValue = new Label(tradeIdUsed);
        tradeIdValue.getStyleClass().add("medium-text");
        GridPane.setValignment(tradeIdValue, VPos.TOP);
        headerGridPane.add(tradeId, col, rowTitle);
        headerGridPane.add(tradeIdValue, col, rowValue);

        // Body
        int colTitle = 0;
        int colValue = 1;
        int row = 0;
        Label date = new Label(Res.get("bisqEasy.tradeCompleted.body.date"));
        date.getStyleClass().addAll("medium-text", "text-fill-grey-dimmed");
        Label dateValue = new Label(tradeDate);
        dateValue.getStyleClass().add("medium-text");
        bodyGridPane.add(date, colTitle, row);
        bodyGridPane.add(dateValue, colValue, row);

        ++row;
        Label tradePrice = new Label(Res.get("bisqEasy.tradeCompleted.body.tradePrice"));
        tradePrice.getStyleClass().addAll("medium-text", "text-fill-grey-dimmed");
        Label tradePriceValue = new Label(tradePriceUsed);
        tradePriceValue.getStyleClass().add("medium-text");
        Label tradePriceSymbol = new Label(tradePriceSymbolUsed.toUpperCase());
        tradePriceSymbol.getStyleClass().addAll("small-text", "text-fill-grey-dimmed");
        HBox tradePriceBox = new HBox(5, tradePriceValue, tradePriceSymbol);
        tradePriceBox.setAlignment(Pos.BOTTOM_LEFT);
        bodyGridPane.add(tradePrice, colTitle, row);
        bodyGridPane.add(tradePriceBox, colValue, row);

        ++row;
        Label tradeFee = new Label(Res.get("bisqEasy.tradeCompleted.body.tradeFee"));
        tradeFee.getStyleClass().addAll("medium-text", "text-fill-grey-dimmed");
        Label tradeFeeValue = new Label(Res.get("bisqEasy.tradeCompleted.body.tradeFee.value"));
        tradeFeeValue.getStyleClass().add("medium-text");
        bodyGridPane.add(tradeFee, colTitle, row);
        bodyGridPane.add(tradeFeeValue, colValue, row);

        if (txIdDescriptionAndValue.isPresent()) {
            ++row;
            Label txIdTitle = new Label(txIdDescriptionAndValue.get().getFirst());
            txIdTitle.getStyleClass().addAll("medium-text", "text-fill-grey-dimmed");
            String txId = txIdDescriptionAndValue.get().getSecond();
            Label txIdValue = new Label(txId);
            txIdValue.getStyleClass().addAll("medium-text", "text-fill-green");

            HBox txValueBox = new HBox(10, txIdValue, Spacer.fillHBox(), blockExplorerButton, copyTxIdButton);
            txValueBox.setAlignment(Pos.CENTER_LEFT);
            GridPane.setValignment(txIdTitle, VPos.CENTER);
            GridPane.setValignment(txIdValue, VPos.CENTER);
            copyTxIdButton.setOnAction(e -> ClipboardUtil.copyToClipboard(txId));
            bodyGridPane.add(txIdTitle, colTitle, row);
            bodyGridPane.add(txValueBox, colValue, row);
            GridPane.setMargin(txIdTitle, new Insets(-5, 0, 0, 0));
            GridPane.setMargin(txValueBox, new Insets(-5, 0, 0, 0));
        }
    }

    public void dispose() {
        blockExplorerButton.setOnAction(null);
        copyTxIdButton.setOnAction(null);
    }

    public void showBlockExplorerLink() {
        blockExplorerButton.setVisible(true);
        blockExplorerButton.setManaged(true);
    }

    private Region getLine() {
        Region line = new Region();
        line.setMinHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: -bisq-border-color-grey");
        line.setPadding(new Insets(9, 0, 8, 0));
        return line;
    }
}
