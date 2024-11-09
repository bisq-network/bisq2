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

import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.NavigationView;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class TradeDetailsView extends NavigationView<VBox, TradeDetailsModel, TradeDetailsController> {
    private final Button closeButton;
    private final Label tradeDateLabel, meLabel, peerLabel, offerTypeLabel, marketLabel, fiatAmountLabel,
        fiatCurrencyLabel, btcAmountLabel, priceLabel, priceCodesLabel, priceSpecLabel, paymentMethodLabel,
        settlementMethodLabel, tradeIdLabel, peerNetworkAddressLabel, btcPaymentAddressTitleLabel, btcPaymentAddressDetailsLabel,
        paymentAccountDataLabel, assignedMediatorLabel;
    private final HBox assignedMediatorBox;
    private final BisqMenuItem tradersAndRoleCopyButton, tradeIdCopyButton, peerNetworkAddressCopyButton,
        btcPaymentAddressCopyButton, paymentAccountDataCopyButton;

    public TradeDetailsView(TradeDetailsModel model, TradeDetailsController controller) {
        super(new VBox(), model, controller);

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
        Label mePrefixLabel = new Label(Res.get("bisqEasy.openTrades.tradeDetails.tradersAndRole.me"));
        mePrefixLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        meLabel = new Label();
        meLabel.getStyleClass().addAll("text-fill-white", "normal-text");
        Label offerTypeAndRoleSlashLabel = new Label("/");
        offerTypeAndRoleSlashLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        Label peerPrefixLabel = new Label(Res.get("bisqEasy.openTrades.tradeDetails.tradersAndRole.peer"));
        peerPrefixLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        peerLabel = new Label();
        peerLabel.getStyleClass().addAll("text-fill-white", "normal-text");
        HBox tradersAndRoleDetailsHBox = new HBox(5, mePrefixLabel, meLabel, offerTypeAndRoleSlashLabel, peerPrefixLabel, peerLabel);
        tradersAndRoleDetailsHBox.setAlignment(Pos.BASELINE_LEFT);
        tradersAndRoleCopyButton = new BisqMenuItem("copy-grey", "copy-white");
        tradersAndRoleCopyButton.setTooltip(Res.get("bisqEasy.openTrades.tradeDetails.tradersAndRole.copy"));
        HBox tradersAndRoleBox = createAndGetTitleAndDetailsBox("bisqEasy.openTrades.tradeDetails.tradersAndRole",
                tradersAndRoleDetailsHBox, Optional.of(tradersAndRoleCopyButton));

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
        paymentMethodsDetailsHBox.setAlignment(Pos.BASELINE_LEFT);
        HBox paymentMethodsBox = createAndGetTitleAndDetailsBox("bisqEasy.openTrades.tradeDetails.paymentAndSettlementMethods",
                paymentMethodsDetailsHBox);

        // Trade ID
        tradeIdLabel = new Label();
        tradeIdLabel.getStyleClass().addAll("text-fill-white", "normal-text");
        tradeIdCopyButton = new BisqMenuItem("copy-grey", "copy-white");
        tradeIdCopyButton.setTooltip(Res.get("bisqEasy.openTrades.tradeDetails.tradeId.copy"));
        HBox tradeIdBox = createAndGetTitleAndDetailsBox("bisqEasy.openTrades.tradeDetails.tradeId", tradeIdLabel,
                Optional.of(tradeIdCopyButton));

        // Peer network address
        peerNetworkAddressLabel = new Label();
        peerNetworkAddressLabel.getStyleClass().addAll("text-fill-white", "normal-text");
        peerNetworkAddressCopyButton = new BisqMenuItem("copy-grey", "copy-white");
        peerNetworkAddressCopyButton.setTooltip(Res.get("bisqEasy.openTrades.tradeDetails.peerNetworkAddress.copy"));
        HBox peerNetworkAddressBox = createAndGetTitleAndDetailsBox("bisqEasy.openTrades.tradeDetails.peerNetworkAddress",
                peerNetworkAddressLabel, Optional.of(peerNetworkAddressCopyButton));

        // BTC payment address
        btcPaymentAddressTitleLabel = new Label();
        btcPaymentAddressDetailsLabel = new Label();
        btcPaymentAddressDetailsLabel.getStyleClass().addAll("normal-text");
        btcPaymentAddressCopyButton = new BisqMenuItem("copy-grey", "copy-white");
        HBox btcPaymentAddressBox = createAndGetTitleAndDetailsBox(btcPaymentAddressTitleLabel,
                btcPaymentAddressDetailsLabel, Optional.of(btcPaymentAddressCopyButton));

        // Payment account data
        paymentAccountDataLabel = new Label();
        paymentAccountDataLabel.getStyleClass().addAll("normal-text");
        paymentAccountDataCopyButton = new BisqMenuItem("copy-grey", "copy-white");
        paymentAccountDataCopyButton.setTooltip(Res.get("bisqEasy.openTrades.tradeDetails.paymentAccountData.copy"));
        HBox paymentAccountDataBox = createAndGetTitleAndDetailsBox("bisqEasy.openTrades.tradeDetails.paymentAccountData",
                paymentAccountDataLabel, Optional.of(paymentAccountDataCopyButton));

        // Assigned mediator
        assignedMediatorLabel = new Label();
        assignedMediatorLabel.getStyleClass().addAll("text-fill-white", "normal-text");
        assignedMediatorBox = createAndGetTitleAndDetailsBox("bisqEasy.openTrades.tradeDetails.assignedMediator", assignedMediatorLabel);

        VBox content = new VBox(20,
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
        content.setAlignment(Pos.CENTER);

        VBox.setMargin(headline, new Insets(-5, 0, -5, 0));
        VBox.setMargin(line, new Insets(0, 0, 0, 0));
        VBox.setMargin(content, new Insets(-40, 80, 0, 80));
        VBox.setVgrow(content, Priority.ALWAYS);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);
        root.getChildren().addAll(closeButtonRow, content);
    }

    private HBox createAndGetTitleAndDetailsBox(String title, Node detailsNode) {
        Label titleLabel = new Label(Res.get(title));
        return createAndGetTitleAndDetailsBox(titleLabel, detailsNode, Optional.empty());
    }

    private HBox createAndGetTitleAndDetailsBox(String title, Node detailsNode, Optional<BisqMenuItem> button) {
        Label titleLabel = new Label(Res.get(title));
        return createAndGetTitleAndDetailsBox(titleLabel, detailsNode, button);
    }

    private HBox createAndGetTitleAndDetailsBox(Label titleLabel, Node detailsNode, Optional<BisqMenuItem> button) {
        double width = 180;
        titleLabel.setMaxWidth(width);
        titleLabel.setMinWidth(width);
        titleLabel.setPrefWidth(width);
        titleLabel.getStyleClass().addAll("text-fill-grey-dimmed", "medium-text");

        HBox hBox = new HBox(titleLabel, detailsNode);
        hBox.setAlignment(Pos.BASELINE_LEFT);

        if (button.isPresent()) {
            button.get().useIconOnly(17);
            HBox.setMargin(button.get(), new Insets(0, 0, 0, 40));
            hBox.getChildren().addAll(Spacer.fillHBox(), button.get());
        }
        return hBox;
    }

    @Override
    protected void onViewAttached() {
        tradeDateLabel.setText(model.getTradeDate());
        meLabel.setText(model.getMe());
        peerLabel.setText(model.getPeer());
        offerTypeLabel.setText(model.getOfferType());
        marketLabel.setText(model.getMarket());
        fiatAmountLabel.setText(model.getFiatAmount());
        fiatCurrencyLabel.setText(model.getFiatCurrency());
        btcAmountLabel.setText(model.getBtcAmount());
        priceLabel.setText(model.getPrice());
        priceCodesLabel.setText(model.getPriceCodes());
        priceSpecLabel.setText(model.getPriceSpec());
        paymentMethodLabel.setText(model.getPaymentMethod());
        settlementMethodLabel.setText(model.getSettlementMethod());
        tradeIdLabel.setText(model.getTradeId());
        peerNetworkAddressLabel.setText(model.getPeerNetworkAddress());
        btcPaymentAddressTitleLabel.setText(model.isOnChainSettlement()
                ? Res.get("bisqEasy.openTrades.tradeDetails.btcPaymentAddress")
                : Res.get("bisqEasy.openTrades.tradeDetails.lightningInvoice"));
        btcPaymentAddressDetailsLabel.setText(model.getBtcPaymentAddress());
        btcPaymentAddressCopyButton.setTooltip(model.isOnChainSettlement()
                ? Res.get("bisqEasy.openTrades.tradeDetails.btcPaymentAddress.copy")
                : Res.get("bisqEasy.openTrades.tradeDetails.lightningInvoice.copy"));
        paymentAccountDataLabel.setText(model.getPaymentAccountData());
        assignedMediatorLabel.setText(model.getAssignedMediator());
        assignedMediatorBox.setVisible(model.isHasMediatorBeenAssigned());
        assignedMediatorBox.setManaged(model.isHasMediatorBeenAssigned());

        if (model.isBtcPaymentDataEmpty()) {
            btcPaymentAddressDetailsLabel.getStyleClass().remove("text-fill-grey-dimmed");
            btcPaymentAddressDetailsLabel.getStyleClass().remove("text-fill-white");
            btcPaymentAddressDetailsLabel.getStyleClass().add(model.isBtcPaymentDataEmpty()
                    ? "text-fill-grey-dimmed"
                    : "text-fill-white");
        }
        if (model.isPaymentAccountDataEmpty()) {
            paymentAccountDataLabel.getStyleClass().remove("text-fill-grey-dimmed");
            paymentAccountDataLabel.getStyleClass().remove("text-fill-white");
            paymentAccountDataLabel.getStyleClass().add(model.isPaymentAccountDataEmpty()
                    ? "text-fill-grey-dimmed"
                    : "text-fill-white");
        }

        closeButton.setOnAction(e -> controller.onClose());
        tradersAndRoleCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getPeer()));
        tradeIdCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getTradeId()));
        peerNetworkAddressCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getPeerNetworkAddress()));
        btcPaymentAddressCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getBtcPaymentAddress()));
        paymentAccountDataCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getPaymentAccountData()));
    }

    @Override
    protected void onViewDetached() {
        closeButton.setOnAction(null);
        tradersAndRoleCopyButton.setOnAction(null);
        tradeIdCopyButton.setOnAction(null);
        peerNetworkAddressCopyButton.setOnAction(null);
        btcPaymentAddressCopyButton.setOnAction(null);
        paymentAccountDataCopyButton.setOnAction(null);
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
