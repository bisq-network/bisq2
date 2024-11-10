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

package bisq.desktop.main.content.bisq_easy.open_trades.trade_details;

import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.view.NavigationView;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.CopyOnClickLabel;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeDetailsView extends NavigationView<VBox, TradeDetailsModel, TradeDetailsController> {

    private final Label headline, processHeadline, processDescription, myRoleLabel, amountLabel, paymentMethodLabel;
    private final Label tradePriceLabel, tradeIdLabel, peerUsernameLabel, dateLabel, mediatorLabel, peerNetworkAddressLabel;
    private final CopyOnClickLabel tradeAmountFiat, tradeAmountBtc, tradeId;
    private final Text makerTakerRole, buySellRole, btcPaymentMethod, currency, fiatPaymentMethod, btcLabel;
    private final Text tradePriceAmount, priceSpec, peerUsername, tradeDate, mediator, peerNetworkAddress;
    private final Button closeButton;
    MaterialTextField btcPaymentAddress, paymentAccountData;

    public TradeDetailsView(TradeDetailsModel model, TradeDetailsController controller) {
        super(new VBox(), model, controller);

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);
        closeButton = BisqIconButton.createIconButton("close");
        HBox closeButtonRow = new HBox(Spacer.fillHBox(), closeButton);
        closeButtonRow.setPadding(new Insets(0, 10, 0, 10));
        closeButtonRow.getStyleClass().add("trade-details-close-button-row");
        closeButtonRow.setAlignment(Pos.CENTER_RIGHT);
        root.getChildren().add(closeButtonRow);

        Region emptySpace = new Region();
        emptySpace.setMinHeight(30);
        emptySpace.setMaxHeight(30);
        headline = createLabel("trade-details-headline");
        VBox headlineBox = new VBox(5, emptySpace, headline);
        headlineBox.setAlignment(Pos.CENTER);
        Region line1 = getLine();
        headlineBox.getChildren().add(line1);
        root.getChildren().add(headlineBox);

        int numColumns = 3;
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setAlignment(Pos.BOTTOM_LEFT);
        GridPaneUtil.setGridPaneMultiColumnsConstraints(gridPane, numColumns);

        int rowIndex = 0;
        Insets topInset = new Insets(0, 0, -14, 0);
        myRoleLabel = createLabel("trade-details-small-grey");
        GridPane.setMargin(myRoleLabel, topInset);
        gridPane.add(myRoleLabel, 0, rowIndex);
        amountLabel = createLabel("trade-details-small-grey");
        GridPane.setMargin(amountLabel, topInset);
        gridPane.add(amountLabel, 1, rowIndex);
        paymentMethodLabel = createLabel("trade-details-small-grey");
        GridPane.setMargin(paymentMethodLabel, topInset);
        gridPane.add(paymentMethodLabel, 2, rowIndex);

        rowIndex++;
        makerTakerRole = createText("trade-details-large-light");
        gridPane.add(makerTakerRole, 0, rowIndex);
        tradeAmountFiat = createCopyOnClickLabel("trade-details-large-light");
        currency = createText("trade-details-medium-grey");
        HBox fiatText = new HBox(5, tradeAmountFiat, currency);
        fiatText.setAlignment(Pos.BASELINE_LEFT);
        gridPane.add(fiatText, 1, rowIndex);
        fiatPaymentMethod = createText("trade-details-large-light");
        gridPane.add(fiatPaymentMethod, 2, rowIndex);

        rowIndex++;
        buySellRole = createText("trade-details-large-light");
        gridPane.add(buySellRole, 0, rowIndex);
        tradeAmountBtc = createCopyOnClickLabel("trade-details-large-light");
        btcLabel = createText("trade-details-medium-grey");
        HBox btcText = new HBox(5, tradeAmountBtc, btcLabel);
        btcText.setAlignment(Pos.BASELINE_LEFT);
        gridPane.add(btcText, 1, rowIndex);
        btcPaymentMethod = createText("trade-details-large-light");
        gridPane.add(btcPaymentMethod, 2, rowIndex);

        rowIndex++;
        Region emptyRow1 = new Region();
        GridPane.setColumnSpan(emptyRow1, numColumns);
        emptyRow1.setPrefHeight(40);
        gridPane.add(emptyRow1, 0, rowIndex);
        rowIndex++;
        Region line3 = getLine();
        GridPane.setColumnSpan(line3, numColumns);
        gridPane.add(line3, 0, rowIndex);

        rowIndex++;
        tradePriceLabel = createLabel("trade-details-medium-grey");
        tradePriceLabel.setAlignment(Pos.BOTTOM_LEFT);
        gridPane.add(tradePriceLabel, 0, rowIndex);
        tradePriceAmount = createText("trade-details-medium-light");
        priceSpec = createText("trade-details-medium-grey");
        HBox tradePriceBox = new HBox(5, tradePriceAmount, priceSpec);
        tradePriceBox.setAlignment(Pos.BOTTOM_LEFT);
        GridPane.setColumnSpan(tradePriceBox, 2);
        gridPane.add(tradePriceBox, 1, rowIndex);
        rowIndex++;
        tradeIdLabel = createLabel("trade-details-medium-grey");
        gridPane.add(tradeIdLabel, 0, rowIndex);
        tradeId = createCopyOnClickLabel("trade-details-medium-light");
        GridPane.setColumnSpan(tradeId, 2);
        gridPane.add(tradeId, 1, rowIndex);
        rowIndex++;
        dateLabel = createLabel("trade-details-medium-grey");
        gridPane.add(dateLabel, 0, rowIndex);
        tradeDate = createText("trade-details-medium-light");
        GridPane.setColumnSpan(tradeDate, 2);
        gridPane.add(tradeDate, 1, rowIndex);
        rowIndex++;
        peerUsernameLabel = createLabel("trade-details-medium-grey");
        gridPane.add(peerUsernameLabel, 0, rowIndex);
        peerUsername = createText("trade-details-medium-light");
        GridPane.setColumnSpan(peerUsername, 2);
        gridPane.add(peerUsername, 1, rowIndex);
        rowIndex++;
        peerNetworkAddressLabel = createLabel("trade-details-medium-grey");
        gridPane.add(peerNetworkAddressLabel, 0, rowIndex);
        peerNetworkAddress = createText("trade-details-medium-light");
        GridPane.setColumnSpan(peerNetworkAddress, 2);
        gridPane.add(peerNetworkAddress, 1, rowIndex);
        rowIndex++;
        mediatorLabel = createLabel("trade-details-medium-grey");
        gridPane.add(mediatorLabel, 0, rowIndex);
        mediator = createText("trade-details-medium-light");
        GridPane.setColumnSpan(mediator, 2);
        gridPane.add(mediator, 1, rowIndex);

        // process information section
        rowIndex++;
        Region emptyRow2 = new Region();
        GridPane.setColumnSpan(emptyRow2, numColumns);
        emptyRow2.setPrefHeight(40);
        gridPane.add(emptyRow2, 0, rowIndex);
        rowIndex++;
        processHeadline = createLabel("trade-details-medium-light");
        GridPane.setColumnSpan(processHeadline, numColumns);
        gridPane.add(processHeadline, 0, rowIndex);
        rowIndex++;
        processDescription = createLabel("trade-details-small-grey");
        processDescription.setPadding(new Insets(-10, 0, 0, 0));
        GridPane.setColumnSpan(processDescription, numColumns);
        gridPane.add(processDescription, 0, rowIndex);
        rowIndex++;
        Region line2 = getLine();
        GridPane.setColumnSpan(line2, numColumns);
        gridPane.add(line2, 0, rowIndex);
        rowIndex++;
        btcPaymentAddress = new MaterialTextField(Res.get("bisqEasy.openTrades.tradeDetails.btcPaymentAddress"));
        btcPaymentAddress.setEditable(false);
        btcPaymentAddress.showCopyIcon();
        GridPane.setColumnSpan(btcPaymentAddress, numColumns);
        gridPane.add(btcPaymentAddress, 0, rowIndex);
        rowIndex++;
        paymentAccountData = new MaterialTextField(Res.get("bisqEasy.openTrades.tradeDetails.paymentAccountData"));
        paymentAccountData.setEditable(false);
        paymentAccountData.showCopyIcon();
        GridPane.setColumnSpan(paymentAccountData, numColumns);
        gridPane.add(paymentAccountData, 0, rowIndex);

        gridPane.setPadding(new Insets(10, 40, 10, 20));
        ScrollPane scrollPane = new ScrollPane(gridPane);
        scrollPane.setFitToWidth(true);
        VBox container = new VBox(scrollPane);
        container.setPadding(new Insets(10, 22, 10, 20));
        root.getChildren().addAll(container);
    }

    @Override
    protected void onViewAttached() {
        headline.setText(Res.get("bisqEasy.openTrades.tradeDetails.headline"));
        processHeadline.setText(Res.get("bisqEasy.openTrades.tradeDetails.processSection"));
        myRoleLabel.setText(Res.get("bisqEasy.openTrades.tradeDetails.myRole"));
        amountLabel.setText(Res.get("bisqEasy.openTrades.tradeDetails.amount"));
        paymentMethodLabel.setText(Res.get("bisqEasy.openTrades.tradeDetails.paymentMethod"));
        processDescription.setText(Res.get("bisqEasy.openTrades.tradeDetails.processDescription"));
        btcLabel.setText("BTC");
        tradePriceLabel.setText(Res.get("bisqEasy.openTrades.tradeDetails.tradePrice"));
        tradeIdLabel.setText(Res.get("bisqEasy.openTrades.table.tradeId"));
        peerUsernameLabel.setText(Res.get("bisqEasy.openTrades.tradeDetails.peerUsername"));
        peerNetworkAddressLabel.setText(Res.get("bisqEasy.openTrades.tradeDetails.peerNetworkAddress"));
        dateLabel.setText(Res.get("bisqEasy.openTrades.tradeDetails.offerTakenDate"));
        mediatorLabel.setText(Res.get("bisqEasy.openTrades.tradeDetails.selectedMediator"));

        buySellRole.textProperty().bind(model.getMySellBuyRole());
        makerTakerRole.textProperty().bind(model.getMyMakerTakerRole());
        tradeAmountFiat.textProperty().bind(model.getAmountInFiat());
        currency.textProperty().bind(model.getCurrency());
        tradeAmountBtc.textProperty().bind(model.getAmountInBTC());
        btcPaymentMethod.textProperty().bind(model.getBitcoinPaymentMethod());
        fiatPaymentMethod.textProperty().bind(model.getFiatPaymentMethod());
        btcPaymentAddress.textProperty().bind(model.getBitcoinPaymentAddress());
        paymentAccountData.textProperty().bind(model.getPaymentAccountData());
        tradePriceAmount.textProperty().bind(model.getTradePrice());
        priceSpec.textProperty().bind(model.getPriceSpec());
        tradeId.textProperty().bind(model.getTradeId());
        peerUsername.textProperty().bind(model.getPeerUsername());
        peerNetworkAddress.textProperty().bind(model.getPeerNetworkAddress());
        tradeDate.textProperty().bind(model.getOfferTakenDateTime());
        mediator.textProperty().bind(model.getMediator());

        closeButton.setOnAction(e -> controller.onClose());
    }

    @Override
    protected void onViewDetached() {
        buySellRole.textProperty().unbind();
        makerTakerRole.textProperty().unbind();
        tradeAmountFiat.textProperty().unbind();
        currency.textProperty().unbind();
        tradeAmountBtc.textProperty().unbind();
        btcPaymentMethod.textProperty().unbind();
        fiatPaymentMethod.textProperty().unbind();
        btcPaymentAddress.textProperty().unbind();
        paymentAccountData.textProperty().unbind();
        tradePriceAmount.textProperty().unbind();
        priceSpec.textProperty().unbind();
        tradeId.textProperty().unbind();
        peerUsername.textProperty().unbind();
        peerNetworkAddress.textProperty().unbind();
        tradeDate.textProperty().unbind();
        mediator.textProperty().unbind();

        closeButton.setOnAction(null);
    }

    private Label createLabel(String style) {
        Label label = new Label();
        label.getStyleClass().add(style);
        return label;
    }

    private Text createText(String style) {
        Text text = new Text();
        text.getStyleClass().add(style);
        return text;
    }

    private CopyOnClickLabel createCopyOnClickLabel(String style) {
        CopyOnClickLabel text = new CopyOnClickLabel("");
        text.getStyleClass().add(style);
        return text;
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