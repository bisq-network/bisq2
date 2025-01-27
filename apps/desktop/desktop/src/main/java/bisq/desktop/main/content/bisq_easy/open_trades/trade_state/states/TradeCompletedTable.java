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
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.WrappingText;
import bisq.desktop.main.content.bisq_easy.components.WaitingAnimation;
import bisq.desktop.main.content.bisq_easy.components.WaitingState;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.i18n.Res;
import bisq.user.profile.UserProfile;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class TradeCompletedTable extends VBox {
    private final GridPane headerGridPane, bodyGridPane;
    @Getter
    private final BisqMenuItem copyTxIdButton, copyTxExplorerLinkButton, openTxExplorerButton;
    private final WaitingAnimation waitingAnimation;

    public TradeCompletedTable() {
        waitingAnimation = new WaitingAnimation(WaitingState.TRADE_COMPLETED);

        WrappingText headline = FormUtils.getHeadline(Res.get("bisqEasy.tradeCompleted.title"));
        WrappingText info = FormUtils.getInfo(Res.get("bisqEasy.tradeCompleted.info"));

        HBox headerHBox = createWaitingInfo(waitingAnimation, headline, info);
        headerHBox.setAlignment(Pos.CENTER);

        Label tableTitle = new Label(Res.get("bisqEasy.tradeCompleted.tableTitle").toUpperCase());
        tableTitle.getStyleClass().addAll("trade-completed-table-title", "font-light");

        // Header
        headerGridPane = GridPaneUtil.getGridPane(10, 10, new Insets(0));
        GridPaneUtil.setGridPaneMultiColumnsConstraints(headerGridPane, 5);

        // Body
        bodyGridPane = GridPaneUtil.getGridPane(10, 10, new Insets(0));
        ColumnConstraints titleCol = new ColumnConstraints();
        titleCol.setPercentWidth(25);
        bodyGridPane.getColumnConstraints().add(titleCol);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setPercentWidth(75);
        bodyGridPane.getColumnConstraints().add(valueCol);

        copyTxIdButton = new BisqMenuItem("copy-grey", "copy-white");
        copyTxIdButton.useIconOnly();
        copyTxIdButton.setTooltip(Res.get("bisqEasy.tradeCompleted.body.copy.txId.tooltip"));

        copyTxExplorerLinkButton = new BisqMenuItem("link-grey", "link-white");
        copyTxExplorerLinkButton.useIconOnly();
        copyTxExplorerLinkButton.setTooltip(Res.get("bisqEasy.tradeCompleted.body.copy.explorerLink.tooltip"));
        copyTxExplorerLinkButton.setVisible(false);
        copyTxExplorerLinkButton.setManaged(false);

        openTxExplorerButton = new BisqMenuItem("open-link-grey", "open-link-white");
        openTxExplorerButton.useIconOnly();
        openTxExplorerButton.setTooltip(Res.get("bisqEasy.tradeCompleted.body.txId.tooltip"));
        openTxExplorerButton.setVisible(false);
        openTxExplorerButton.setManaged(false);

        Region line1 = getLine();
        Region line2 = getLine();
        Region line3 = getLine();
        VBox.setMargin(headerHBox, new Insets(10, 0, 30, 0));
        VBox.setMargin(line1, new Insets(10, 0, 5, 0));
        VBox.setMargin(line2, new Insets(30, 0, 5, 0));
        VBox.setMargin(line3, new Insets(5, 0, 10, 0));
        getChildren().addAll(headerHBox, tableTitle, line1, headerGridPane, line2, bodyGridPane, line3);
        setAlignment(Pos.CENTER);
    }

    public void initialize(UserProfile userProfile,
                           boolean isBuyer,
                           String btcAmount,
                           String fiatAmount,
                           String fiatCurrency,
                           String paymentMethod,
                           String tradeId,
                           String tradeDate,
                           String tradeDuration,
                           String tradePrice,
                           String tradePriceSymbol,
                           Optional<Pair<String, String>> txIdDescriptionAndValue) {
        waitingAnimation.play();

        // Header
        int rowTitle = 0;
        int rowValue = 1;

        int col = 0;
        Label tradeWith = new Label(Res.get("bisqEasy.tradeCompleted.header.tradeWith").toUpperCase());
        tradeWith.getStyleClass().addAll("dimmed-text");
        UserProfileDisplay tradeWithValue = new UserProfileDisplay(userProfile);
        headerGridPane.add(tradeWith, col, rowTitle);
        headerGridPane.add(tradeWithValue, col, rowValue);

        ++col;
        Label myDirection = isBuyer
                ? new Label(Res.get("bisqEasy.tradeCompleted.header.myDirection.buyer").toUpperCase())
                : new Label(Res.get("bisqEasy.tradeCompleted.header.myDirection.seller").toUpperCase());
        myDirection.getStyleClass().addAll("dimmed-text");
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
        myOutcome.getStyleClass().addAll("dimmed-text");
        Label myOutcomeValue = new Label(fiatAmount);
        myOutcomeValue.getStyleClass().add("medium-text");
        Label fiat = new Label(fiatCurrency.toUpperCase());
        fiat.getStyleClass().addAll("small-text", "text-fill-grey-dimmed");
        HBox fiatBox = new HBox(5, myOutcomeValue, fiat);
        fiatBox.setAlignment(Pos.BASELINE_LEFT);
        headerGridPane.add(myOutcome, col, rowTitle);
        headerGridPane.add(fiatBox, col, rowValue);

        ++col;
        Label tradePriceLabel = new Label(Res.get("bisqEasy.tradeCompleted.header.tradePrice").toUpperCase());
        tradePriceLabel.getStyleClass().addAll("dimmed-text");
        Label tradePriceValue = new Label(tradePrice);
        tradePriceValue.getStyleClass().add("medium-text");
        Label tradePriceSymbolLabel = new Label(tradePriceSymbol.toUpperCase());
        tradePriceSymbolLabel.getStyleClass().addAll("small-text", "text-fill-grey-dimmed");
        HBox tradePriceBox = new HBox(5, tradePriceValue, tradePriceSymbolLabel);
        tradePriceBox.setAlignment(Pos.BASELINE_LEFT);
        headerGridPane.add(tradePriceLabel, col, rowTitle);
        headerGridPane.add(tradePriceBox, col, rowValue);

        ++col;
        Label paymentMethodLabel = new Label(Res.get("bisqEasy.tradeCompleted.header.paymentMethod").toUpperCase());
        paymentMethodLabel.getStyleClass().addAll("dimmed-text");
        Label paymentMethodValue = new Label(paymentMethod);
        paymentMethodValue.getStyleClass().add("medium-text");
        GridPane.setValignment(paymentMethodValue, VPos.TOP);
        headerGridPane.add(paymentMethodLabel, col, rowTitle);
        headerGridPane.add(paymentMethodValue, col, rowValue);

        // Body
        int colTitle = 0;
        int colValue = 1;
        int row = 0;

        Label tradeDateLabel = new Label(Res.get("bisqEasy.tradeCompleted.body.date"));
        tradeDateLabel.getStyleClass().addAll("dimmed-text");
        Label dateValue = new Label(tradeDate);
        dateValue.getStyleClass().add("medium-text");
        bodyGridPane.add(tradeDateLabel, colTitle, row);
        bodyGridPane.add(dateValue, colValue, row);

        // For completed trades before v2.1.3 we do not get the tradeDuration set
        if (!tradeDuration.isEmpty()) {
            ++row;
            Label tradeDurationLabel = new Label(Res.get("bisqEasy.tradeCompleted.body.tradeDuration"));
            tradeDurationLabel.getStyleClass().addAll("dimmed-text");
            Label tradeDurationValue = new Label(tradeDuration);
            tradeDurationValue.getStyleClass().add("medium-text");
            bodyGridPane.add(tradeDurationLabel, colTitle, row);
            bodyGridPane.add(tradeDurationValue, colValue, row);
        }

        ++row;
        Label tradeIdLabel = new Label(Res.get("bisqEasy.tradeCompleted.body.tradeId"));
        tradeIdLabel.getStyleClass().addAll("dimmed-text");
        Label tradeIdValue = new Label(tradeId);
        tradeIdValue.getStyleClass().add("medium-text");
        bodyGridPane.add(tradeIdLabel, colTitle, row);
        bodyGridPane.add(tradeIdValue, colValue, row);

       /* ++row;
        Label tradeFee = new Label(Res.get("bisqEasy.tradeCompleted.body.tradeFee"));
        tradeFee.getStyleClass().addAll("dimmed-text");
        Label tradeFeeValue = new Label(Res.get("bisqEasy.tradeCompleted.body.tradeFee.value"));
        tradeFeeValue.getStyleClass().add("medium-text");
        bodyGridPane.add(tradeFee, colTitle, row);
        bodyGridPane.add(tradeFeeValue, colValue, row);*/

        if (txIdDescriptionAndValue.isPresent()) {
            ++row;
            Label txIdTitle = new Label(txIdDescriptionAndValue.get().getFirst());
            txIdTitle.getStyleClass().addAll("dimmed-text");
            String txId = txIdDescriptionAndValue.get().getSecond();
            Label txIdValue = new Label(txId);
            txIdValue.getStyleClass().addAll("medium-text"/*, "text-fill-green"*/);

            HBox txValueBox = new HBox(1, txIdValue, Spacer.fillHBox(), copyTxIdButton, copyTxExplorerLinkButton, openTxExplorerButton);
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
        copyTxIdButton.setOnAction(null);
        copyTxExplorerLinkButton.setOnAction(null);
        openTxExplorerButton.setOnAction(null);
    }

    public void showBlockExplorerLink() {
        copyTxExplorerLinkButton.setVisible(true);
        copyTxExplorerLinkButton.setManaged(true);
        openTxExplorerButton.setVisible(true);
        openTxExplorerButton.setManaged(true);
    }

    private Region getLine() {
        Region line = new Region();
        line.setMinHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: -bisq-border-color-grey");
        line.setPadding(new Insets(9, 0, 8, 0));
        return line;
    }

    private HBox createWaitingInfo(WaitingAnimation animation, WrappingText headline, WrappingText info) {
        animation.setAlignment(Pos.CENTER);
        VBox textBox = new VBox(10, headline, info);
        return new HBox(20, animation, textBox);
    }

}
