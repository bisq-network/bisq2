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

package bisq.desktop.main.content.mu_sig.open_trades.trade_state.states;

import bisq.common.data.Pair;
import bisq.common.market.Market;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.BitcoinAmountDisplay;
import bisq.desktop.components.controls.WrappingText;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.desktop.main.content.mu_sig.components.MuSigWaitingAnimation;
import bisq.desktop.main.content.mu_sig.components.MuSigWaitingState;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MuSigTradeCompletedTable extends VBox {
    private final GridPane headerGridPane, bodyGridPane;
    @Getter
    private final BisqMenuItem copyTxIdButton, copyTxExplorerLinkButton, openTxExplorerButton;
    private final MuSigWaitingAnimation waitingAnimation;
    private final BitcoinAmountDisplay bitcoinAmountDisplay;
    private final Label btcSideDirection, nonBtcSideDirection, nonBtcAmountValue, nonBtcCode,
            paymentMethodLabel, paymentMethodValue;
    private final HBox btcAmountDisplayHBox, nonBtcHBox;

    public MuSigTradeCompletedTable() {
        waitingAnimation = new MuSigWaitingAnimation(MuSigWaitingState.TRADE_COMPLETED);

        WrappingText headline = MuSigFormUtils.getHeadline(Res.get("muSig.tradeCompleted.title"));
        WrappingText info = MuSigFormUtils.getInfo(Res.get("muSig.tradeCompleted.info"));

        HBox headerHBox = createWaitingInfo(waitingAnimation, headline, info);
        headerHBox.setAlignment(Pos.CENTER);

        Label tableTitle = new Label(Res.get("bisqEasy.tradeCompleted.tableTitle").toUpperCase());
        tableTitle.getStyleClass().addAll("trade-completed-table-title", "font-light");

        // Header
        headerGridPane = GridPaneUtil.getGridPane(10, 10, new Insets(0));

        // Body
        bodyGridPane = GridPaneUtil.getGridPane(10, 10, new Insets(0));
        ColumnConstraints titleCol = new ColumnConstraints();
        titleCol.setPercentWidth(25);
        bodyGridPane.getColumnConstraints().add(titleCol);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setPercentWidth(75);
        bodyGridPane.getColumnConstraints().add(valueCol);

        bitcoinAmountDisplay = new BitcoinAmountDisplay();
        btcAmountDisplayHBox = new HBox(5, bitcoinAmountDisplay);
        btcAmountDisplayHBox.setAlignment(Pos.BASELINE_LEFT);
        HBox.setMargin(bitcoinAmountDisplay, new Insets(1, 0, 0, 0));
        configureBitcoinAmountDisplay(bitcoinAmountDisplay);

        btcSideDirection = new Label();
        btcSideDirection.getStyleClass().addAll("dimmed-text");
        nonBtcSideDirection = new Label();
        nonBtcSideDirection.getStyleClass().addAll("dimmed-text");
        nonBtcAmountValue = new Label();
        nonBtcAmountValue.getStyleClass().add("medium-text");

        nonBtcCode = new Label();
        nonBtcCode.getStyleClass().addAll("small-text", "text-fill-grey-dimmed");
        nonBtcHBox = new HBox(nonBtcAmountValue, nonBtcCode);
        nonBtcHBox.setAlignment(Pos.BASELINE_LEFT);

        paymentMethodLabel = new Label();
        paymentMethodLabel.getStyleClass().addAll("dimmed-text");
        paymentMethodValue = new Label();
        paymentMethodValue.getStyleClass().add("medium-text");
        GridPane.setValignment(paymentMethodValue, VPos.TOP);

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

    public void initialize(Market market,
                           UserProfileDisplay tradeWithValue,
                           boolean isBuyer,
                           String btcAmount,
                           String nonBtcAmount,
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

        boolean isBaseCurrencyBitcoin = market.isBaseCurrencyBitcoin();
        int numColumns = isBaseCurrencyBitcoin ? 5 : 4;
        GridPaneUtil.setGridPaneMultiColumnsConstraints(headerGridPane, numColumns);

        Label tradeWith = new Label(Res.get("bisqEasy.tradeCompleted.header.tradeWith").toUpperCase());
        tradeWith.getStyleClass().addAll("dimmed-text");
        headerGridPane.add(tradeWith, col, rowTitle);
        headerGridPane.add(tradeWithValue, col, rowValue);

        nonBtcCode.setText("  " + market.getNonBtcCurrencyCode().toUpperCase());
        bitcoinAmountDisplay.setBtcAmount(btcAmount);
        nonBtcAmountValue.setText(nonBtcAmount);

        ++col;

        if (isBaseCurrencyBitcoin) {
            if (isBuyer) {
                btcSideDirection.setText(Res.get("bisqEasy.tradeCompleted.header.myDirection.buyer").toUpperCase());
                nonBtcSideDirection.setText(Res.get("bisqEasy.tradeCompleted.header.myOutcome.buyer").toUpperCase());
            } else {
                btcSideDirection.setText(Res.get("bisqEasy.tradeCompleted.header.myDirection.seller").toUpperCase());
                nonBtcSideDirection.setText(Res.get("bisqEasy.tradeCompleted.header.myOutcome.seller").toUpperCase());
            }
            headerGridPane.add(btcSideDirection, col, rowTitle);
            headerGridPane.add(btcAmountDisplayHBox, col, rowValue);
            ++col;
            headerGridPane.add(nonBtcSideDirection, col, rowTitle);
            headerGridPane.add(nonBtcHBox, col, rowValue);

        } else {
            if (isBuyer) {
                nonBtcSideDirection.setText(Res.get("bisqEasy.tradeCompleted.header.myDirection.buyer").toUpperCase());
                btcSideDirection.setText(Res.get("bisqEasy.tradeCompleted.header.myOutcome.buyer").toUpperCase());
            } else {
                nonBtcSideDirection.setText(Res.get("bisqEasy.tradeCompleted.header.myDirection.seller").toUpperCase());
                btcSideDirection.setText(Res.get("bisqEasy.tradeCompleted.header.myOutcome.seller").toUpperCase());
            }
            headerGridPane.add(nonBtcSideDirection, col, rowTitle);
            headerGridPane.add(nonBtcHBox, col, rowValue);
            ++col;
            headerGridPane.add(btcSideDirection, col, rowTitle);
            headerGridPane.add(btcAmountDisplayHBox, col, rowValue);
        }

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

        if (isBaseCurrencyBitcoin) {
            ++col;
            paymentMethodLabel.setText(Res.get("bisqEasy.tradeCompleted.header.paymentMethod").toUpperCase());
            paymentMethodValue.setText(paymentMethod);
            headerGridPane.add(paymentMethodLabel, col, rowTitle);
            headerGridPane.add(paymentMethodValue, col, rowValue);
        }

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

        // For completed trades before v2.1.4 we do not get the tradeDuration set
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
        Label tradeFee = new Label(Res.get("muSig.tradeCompleted.body.tradeFee"));
        tradeFee.getStyleClass().addAll("dimmed-text");
        Label tradeFeeValue = new Label(Res.get("muSig.tradeCompleted.body.tradeFee.value"));
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

    private void configureBitcoinAmountDisplay(BitcoinAmountDisplay btcText) {
        btcText.getIntegerPart().getStyleClass().add("medium-text");
        btcText.getSignificantDigits().getStyleClass().add("medium-text");
        btcText.getBtcCode().getStyleClass().addAll("small-text", "text-fill-grey-dimmed");
        btcText.getLeadingZeros().getStyleClass().addAll("medium-text", "text-fill-grey-dimmed");
        btcText.applyCompactConfig(13, 10, 24);
    }

    private Region getLine() {
        Region line = new Region();
        line.setMinHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: -bisq-border-color-grey");
        line.setPadding(new Insets(9, 0, 8, 0));
        return line;
    }

    private HBox createWaitingInfo(MuSigWaitingAnimation animation, WrappingText headline, WrappingText info) {
        animation.setAlignment(Pos.CENTER);
        VBox textBox = new VBox(10, headline, info);
        return new HBox(20, animation, textBox);
    }

}
