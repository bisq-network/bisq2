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

import bisq.desktop.common.view.NavigationView;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.CopyOnClickLabel;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public class TradeDetailsView extends NavigationView<VBox, TradeDetailsModel, TradeDetailsController> {
    private final Button closeButton;
    private final Label tradeDateLabel, meLabel, peerLabel, offerTypeLabel, marketLabel, fiatAmountLabel,
        fiatCurrencyLabel, btcAmountLabel, priceLabel, priceCodesLabel, priceSpecLabel, paymentMethodLabel,
        settlementMethodLabel, tradeIdLabel, peerNetworkAddressLabel, btcPaymentAddressLabel,
        paymentAccountDataLabel, assignedMediatorLabel;
    private Subscription isBtcPaymentAddressEmptyPin, isPaymentAccountDataEmptyPin;

    public TradeDetailsView(TradeDetailsModel model, TradeDetailsController controller) {
        super(new VBox(20), model, controller);

        closeButton = BisqIconButton.createIconButton("close");
        HBox closeButtonRow = new HBox(Spacer.fillHBox(), closeButton);
        closeButtonRow.setPadding(new Insets(15, 15, 0, 0));

        Label headline = new Label(Res.get("bisqEasy.openTrades.tradeDetails.headline"));
        headline.getStyleClass().add("bisq-text-17");

        Region line = getLine();

        // Trade date
        tradeDateLabel = new Label();
        tradeDateLabel.getStyleClass().addAll("text-fill-white", "normal-text");
        HBox tradeDateBox = createAndGetTitleAndDetailsBox("bisqEasy.openTrades.tradeDetails.tradeDate",
                tradeDateLabel);

        // Traders / Roles
        Label mePrefixLabel = new Label(Res.get("bisqEasy.openTrades.tradeDetails.tradersAndRole.Me"));
        mePrefixLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        meLabel = new Label();
        meLabel.getStyleClass().addAll("text-fill-white", "normal-text");
        Label peerPrefixLabel = new Label(Res.get("bisqEasy.openTrades.tradeDetails.tradersAndRole.Peer"));
        peerPrefixLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        peerLabel = new Label();
        peerLabel.getStyleClass().addAll("text-fill-white", "normal-text");
        Label offerTypeAndRoleSlashLabel = new Label("/");
        offerTypeAndRoleSlashLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        HBox tradersAndRoleDetailsHBox = new HBox(5, mePrefixLabel, meLabel, offerTypeAndRoleSlashLabel, peerPrefixLabel, peerLabel);
        tradersAndRoleDetailsHBox.setAlignment(Pos.BASELINE_LEFT);
        HBox tradersAndRoleBox = createAndGetTitleAndDetailsBox("bisqEasy.openTrades.tradeDetails.tradersAndRole",
                tradersAndRoleDetailsHBox);

        // Offer type and market
        offerTypeLabel = new Label();
        offerTypeLabel.getStyleClass().addAll("text-fill-white", "normal-text");
        Label offerAndMarketslashLabel = new Label("/");
        offerAndMarketslashLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        marketLabel = new Label();
        marketLabel.getStyleClass().addAll("text-fill-white", "normal-text");
        HBox offerTypeAndMarketDetailsHBox = new HBox(5, offerTypeLabel, offerAndMarketslashLabel, marketLabel);
        offerTypeAndMarketDetailsHBox.setAlignment(Pos.BASELINE_LEFT);
        HBox offerTypeAndMarketBox = createAndGetTitleAndDetailsBox("bisqEasy.openTrades.tradeDetails.offerTypeAndMarket",
                offerTypeAndMarketDetailsHBox);

        // Amount and price
        fiatAmountLabel = new Label();
        fiatAmountLabel.getStyleClass().addAll("text-fill-white", "normal-text");
        fiatCurrencyLabel = new Label();
        fiatCurrencyLabel.getStyleClass().addAll("text-fill-white", "small-text");

        Label openParenthesisLabel = new Label("(");
        openParenthesisLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        btcAmountLabel = new Label();
        btcAmountLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        btcAmountLabel.setPadding(new Insets(0, 5, 0, 0));
        Label btcLabel = new Label("BTC");
        btcLabel.getStyleClass().addAll("text-fill-grey-dimmed", "small-text");
        Label closingParenthesisLabel = new Label(")");
        closingParenthesisLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        HBox btcAmountHBoxbtcAmountHBox = new HBox(openParenthesisLabel, btcAmountLabel, btcLabel, closingParenthesisLabel);
        btcAmountHBoxbtcAmountHBox.setAlignment(Pos.BASELINE_LEFT);
        Label atLabel = new Label("@");
        atLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        priceLabel = new Label();
        priceLabel.getStyleClass().addAll("text-fill-white", "normal-text");
        priceCodesLabel = new Label();
        priceCodesLabel.getStyleClass().addAll("text-fill-white", "small-text");
        priceSpecLabel = new Label();
        priceSpecLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        HBox amountAndPriceDetailsHBox = new HBox(5, fiatAmountLabel, fiatCurrencyLabel, btcAmountHBoxbtcAmountHBox,
                atLabel, priceLabel, priceCodesLabel, priceSpecLabel);
        amountAndPriceDetailsHBox.setAlignment(Pos.BASELINE_LEFT);
        HBox amountAndPriceBox = createAndGetTitleAndDetailsBox("bisqEasy.openTrades.tradeDetails.amountAndPrice", amountAndPriceDetailsHBox);

        // Payment and settlement methods
        paymentMethodLabel = new Label();
        paymentMethodLabel.getStyleClass().addAll("text-fill-white", "normal-text");
        Label paymentMethodsSlashLabel = new Label("/");
        paymentMethodsSlashLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        settlementMethodLabel = new Label();
        settlementMethodLabel.getStyleClass().addAll("text-fill-white", "normal-text");
        HBox paymentMethodsDetailsHBox = new HBox(5, paymentMethodLabel, paymentMethodsSlashLabel, settlementMethodLabel);
        HBox paymentMethodsBox = createAndGetTitleAndDetailsBox("bisqEasy.openTrades.tradeDetails.paymentAndSettlementMethods",
                paymentMethodsDetailsHBox);

        // Trade ID
        tradeIdLabel = new Label();
        tradeIdLabel.getStyleClass().addAll("text-fill-white", "normal-text");
        HBox tradeIdBox = createAndGetTitleAndDetailsBox("bisqEasy.openTrades.tradeDetails.tradeId", tradeIdLabel);

        // Peer network address
        peerNetworkAddressLabel = new Label();
        peerNetworkAddressLabel.getStyleClass().addAll("text-fill-white", "normal-text");
        HBox peerNetworkAddressBox = createAndGetTitleAndDetailsBox("bisqEasy.openTrades.tradeDetails.peerNetworkAddress", peerNetworkAddressLabel);

        // BTC payment address
        btcPaymentAddressLabel = new Label();
        btcPaymentAddressLabel.getStyleClass().addAll("normal-text");
        HBox btcPaymentAddressBox = createAndGetTitleAndDetailsBox("bisqEasy.openTrades.tradeDetails.btcPaymentAddress", btcPaymentAddressLabel);

        // Payment account data
        paymentAccountDataLabel = new Label();
        paymentAccountDataLabel.getStyleClass().addAll("normal-text");
        HBox paymentAccountDataBox = createAndGetTitleAndDetailsBox("bisqEasy.openTrades.tradeDetails.paymentAccountData", paymentAccountDataLabel);

        // Assigned mediator
        assignedMediatorLabel = new Label();
        assignedMediatorLabel.getStyleClass().addAll("text-fill-white", "normal-text");
        HBox assignedMediatorBox = createAndGetTitleAndDetailsBox("bisqEasy.openTrades.tradeDetails.assignedMediator", assignedMediatorLabel);

        VBox.setMargin(headline, new Insets(-20, 0, -5, 0));
        VBox.setMargin(line, new Insets(0, 80, 0, 80));
        root.setAlignment(Pos.TOP_CENTER);
        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);
        root.getChildren().addAll(closeButtonRow,
                headline,
                line,
                tradeDateBox,
                tradersAndRoleBox,
                offerTypeAndMarketBox,
                amountAndPriceBox,
                paymentMethodsBox,
                tradeIdBox,
                peerNetworkAddressBox,
                btcPaymentAddressBox,
                paymentAccountDataBox,
                assignedMediatorBox);
    }

    private HBox createAndGetTitleAndDetailsBox(String title, Node detailsNode) {
        return createAndGetTitleAndDetailsBox(title, detailsNode, Optional.empty());
    }

    private HBox createAndGetTitleAndDetailsBox(String title, Node detailsNode, Optional<BisqMenuItem> button) {
        Label titleLabel = new Label(Res.get(title));
        double width = 180;
        titleLabel.setMaxWidth(width);
        titleLabel.setMinWidth(width);
        titleLabel.setPrefWidth(width);
        titleLabel.getStyleClass().addAll("text-fill-grey-dimmed", "medium-text");

        HBox hBox = new HBox(titleLabel, detailsNode);
        hBox.setAlignment(Pos.BASELINE_LEFT);
        VBox.setMargin(hBox, new Insets(0, 80, 0, 80));
        return hBox;
    }

    @Override
    protected void onViewAttached() {
        tradeDateLabel.textProperty().bind(model.getTradeDate());
        meLabel.textProperty().bind(model.getMe());
        peerLabel.textProperty().bind(model.getPeer());
        offerTypeLabel.textProperty().bind(model.getOfferType());
        marketLabel.textProperty().bind(model.getMarket());
        fiatAmountLabel.textProperty().bind(model.getFiatAmount());
        fiatCurrencyLabel.textProperty().bind(model.getFiatCurrency());
        btcAmountLabel.textProperty().bind(model.getBtcAmount());
        priceLabel.textProperty().bind(model.getPrice());
        priceCodesLabel.textProperty().bind(model.getPriceCodes());
        priceSpecLabel.textProperty().bind(model.getPriceSpec());
        paymentMethodLabel.textProperty().bind(model.getPaymentMethod());
        settlementMethodLabel.textProperty().bind(model.getSettlementMethod());
        tradeIdLabel.textProperty().bind(model.getTradeId());
        peerNetworkAddressLabel.textProperty().bind(model.getPeerNetworkAddress());
        btcPaymentAddressLabel.textProperty().bind(model.getBtcPaymentAddress());
        paymentAccountDataLabel.textProperty().bind(model.getPaymentAccountData());
        assignedMediatorLabel.textProperty().bind(model.getAssignedMediator());

        isBtcPaymentAddressEmptyPin = EasyBind.subscribe(model.getIsBtcPaymentDataEmpty(), isEmpty ->
            btcPaymentAddressLabel.getStyleClass().add(isEmpty ? "text-fill-grey-dimmed" : "text-fill-white"));
        isPaymentAccountDataEmptyPin = EasyBind.subscribe(model.getIsPaymentAccountDataEmpty(), isEmpty ->
            paymentAccountDataLabel.getStyleClass().add(isEmpty ? "text-fill-grey-dimmed" : "text-fill-white"));

        closeButton.setOnAction(e -> controller.onClose());
    }

    @Override
    protected void onViewDetached() {
        tradeDateLabel.textProperty().unbind();
        meLabel.textProperty().unbind();
        peerLabel.textProperty().unbind();
        offerTypeLabel.textProperty().unbind();
        marketLabel.textProperty().unbind();
        fiatAmountLabel.textProperty().unbind();
        fiatCurrencyLabel.textProperty().unbind();
        btcAmountLabel.textProperty().unbind();
        priceLabel.textProperty().unbind();
        priceCodesLabel.textProperty().unbind();
        priceSpecLabel.textProperty().unbind();
        paymentMethodLabel.textProperty().unbind();
        settlementMethodLabel.textProperty().unbind();
        tradeIdLabel.textProperty().unbind();
        peerNetworkAddressLabel.textProperty().unbind();
        btcPaymentAddressLabel.textProperty().unbind();
        paymentAccountDataLabel.textProperty().unbind();
        assignedMediatorLabel.textProperty().unbind();

        isBtcPaymentAddressEmptyPin.unsubscribe();
        isPaymentAccountDataEmptyPin.unsubscribe();

        closeButton.setOnAction(null);
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
