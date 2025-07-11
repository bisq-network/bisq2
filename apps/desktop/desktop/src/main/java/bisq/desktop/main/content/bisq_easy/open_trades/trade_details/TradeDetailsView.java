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
    private final Label tradeDateLabel, tradeDurationLabel, meLabel, peerLabel, offerTypeLabel, marketLabel, fiatAmountLabel,
            fiatCurrencyLabel, btcAmountLabel, priceLabel, priceCodesLabel, priceSpecLabel, paymentMethodLabel,
            settlementMethodLabel, tradeIdLabel, peerNetworkAddressLabel, btcPaymentAddressTitleLabel,
            btcPaymentAddressDetailsLabel, paymentProofTitleLabel, paymentProofDetailsLabel,
            paymentAccountDataLabel, assignedMediatorLabel;
    private final HBox assignedMediatorBox;
    private final BisqMenuItem tradersAndRoleCopyButton, tradeIdCopyButton, peerNetworkAddressCopyButton,
            btcPaymentAddressCopyButton, paymentProofCopyButton, paymentAccountDataCopyButton;
    private final HBox paymentProofBox, tradeDurationBox;

    public TradeDetailsView(TradeDetailsModel model, TradeDetailsController controller) {
        super(new VBox(), model, controller);

        closeButton = BisqIconButton.createIconButton("close");
        HBox closeButtonRow = new HBox(Spacer.fillHBox(), closeButton);
        closeButtonRow.setPadding(new Insets(15, 15, 0, 0));

        Label headline = new Label(Res.get("bisqEasy.openTrades.tradeDetails.headline"));
        headline.getStyleClass().add("bisq-text-17");
        headline.setAlignment(Pos.CENTER);
        headline.setMaxWidth(Double.MAX_VALUE);

        // Trade date
        tradeDateLabel = getValueLabel();
        HBox tradeDateBox = createAndGetDescriptionAndValueBox("bisqEasy.openTrades.tradeDetails.tradeDate", tradeDateLabel);

        // Trade duration
        tradeDurationLabel = getValueLabel();
        tradeDurationBox = createAndGetDescriptionAndValueBox("bisqEasy.openTrades.tradeDetails.tradeDuration", tradeDurationLabel);

        // Traders / Roles
        Label mePrefixLabel = new Label(Res.get("bisqEasy.openTrades.tradeDetails.tradersAndRole.me"));
        mePrefixLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        meLabel = getValueLabel();
        Label offerTypeAndRoleSlashLabel = new Label("/");
        offerTypeAndRoleSlashLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        Label peerPrefixLabel = new Label(Res.get("bisqEasy.openTrades.tradeDetails.tradersAndRole.peer"));
        peerPrefixLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        peerLabel = getValueLabel();
        HBox tradersAndRoleDetailsHBox = new HBox(5, mePrefixLabel, meLabel, offerTypeAndRoleSlashLabel, peerPrefixLabel, peerLabel);
        tradersAndRoleDetailsHBox.setAlignment(Pos.BASELINE_LEFT);
        tradersAndRoleCopyButton = getTradeIdCopyButton(Res.get("bisqEasy.openTrades.tradeDetails.tradersAndRole.copy"));
        HBox tradersAndRoleBox = createAndGetDescriptionAndValueBox("bisqEasy.openTrades.tradeDetails.tradersAndRole",
                tradersAndRoleDetailsHBox, tradersAndRoleCopyButton);

        // Offer type and market
        offerTypeLabel = getValueLabel();
        Label offerAndMarketslashLabel = new Label("/");
        offerAndMarketslashLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        marketLabel = getValueLabel();
        HBox offerTypeAndMarketDetailsHBox = new HBox(5, offerTypeLabel, offerAndMarketslashLabel, marketLabel);
        offerTypeAndMarketDetailsHBox.setAlignment(Pos.BASELINE_LEFT);
        HBox offerTypeAndMarketBox = createAndGetDescriptionAndValueBox("bisqEasy.openTrades.tradeDetails.offerTypeAndMarket",
                offerTypeAndMarketDetailsHBox);

        // Amount and price
        fiatAmountLabel = getValueLabel();
        fiatCurrencyLabel = new Label();
        fiatCurrencyLabel.getStyleClass().addAll("text-fill-white", "small-text");

        Label openParenthesisLabel = new Label("(");
        openParenthesisLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        btcAmountLabel = getValueLabel();
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
        priceLabel = getValueLabel();
        priceCodesLabel = new Label();
        priceCodesLabel.getStyleClass().addAll("text-fill-white", "small-text");
        priceSpecLabel = new Label();
        priceSpecLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        HBox amountAndPriceDetailsHBox = new HBox(5, fiatAmountLabel, fiatCurrencyLabel, btcAmountHBoxbtcAmountHBox,
                atLabel, priceLabel, priceCodesLabel, priceSpecLabel);
        amountAndPriceDetailsHBox.setAlignment(Pos.BASELINE_LEFT);
        HBox amountAndPriceBox = createAndGetDescriptionAndValueBox("bisqEasy.openTrades.tradeDetails.amountAndPrice", amountAndPriceDetailsHBox);

        // Payment and settlement methods
        paymentMethodLabel = getValueLabel();
        Label paymentMethodsSlashLabel = new Label("/");
        paymentMethodsSlashLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        settlementMethodLabel = getValueLabel();
        HBox paymentMethodsDetailsHBox = new HBox(5, paymentMethodLabel, paymentMethodsSlashLabel, settlementMethodLabel);
        paymentMethodsDetailsHBox.setAlignment(Pos.BASELINE_LEFT);
        HBox paymentMethodsBox = createAndGetDescriptionAndValueBox("bisqEasy.openTrades.tradeDetails.paymentAndSettlementMethods",
                paymentMethodsDetailsHBox);

        // Trade ID
        tradeIdLabel = getValueLabel();
        tradeIdCopyButton = getTradeIdCopyButton(Res.get("bisqEasy.openTrades.tradeDetails.tradeId.copy"));
        HBox tradeIdBox = createAndGetDescriptionAndValueBox("bisqEasy.openTrades.tradeDetails.tradeId",
                tradeIdLabel, tradeIdCopyButton);

        // Peer network address
        peerNetworkAddressLabel = getValueLabel();
        peerNetworkAddressCopyButton = getTradeIdCopyButton(Res.get("bisqEasy.openTrades.tradeDetails.peerNetworkAddress.copy"));
        HBox peerNetworkAddressBox = createAndGetDescriptionAndValueBox("bisqEasy.openTrades.tradeDetails.peerNetworkAddress",
                peerNetworkAddressLabel, peerNetworkAddressCopyButton);

        // Payment account data
        paymentAccountDataLabel = getValueLabel();
        paymentAccountDataCopyButton = getTradeIdCopyButton(Res.get("bisqEasy.openTrades.tradeDetails.paymentAccountData.copy"));
        HBox paymentAccountDataBox = createAndGetDescriptionAndValueBox("bisqEasy.openTrades.tradeDetails.paymentAccountData",
                paymentAccountDataLabel, paymentAccountDataCopyButton);

        // BTC payment address
        btcPaymentAddressTitleLabel = getDescriptionLabel("");
        btcPaymentAddressDetailsLabel = getValueLabel();
        btcPaymentAddressCopyButton = getTradeIdCopyButton("");
        HBox btcPaymentAddressBox = createAndGetDescriptionAndValueBox(btcPaymentAddressTitleLabel,
                btcPaymentAddressDetailsLabel, btcPaymentAddressCopyButton);

        // Payment proof (tx ID or optional LN pre-image)
        paymentProofTitleLabel = getDescriptionLabel("");
        paymentProofDetailsLabel = getValueLabel();
        paymentProofCopyButton = getTradeIdCopyButton("");
        paymentProofBox = createAndGetDescriptionAndValueBox(paymentProofTitleLabel,
                paymentProofDetailsLabel, paymentProofCopyButton);

        // Assigned mediator
        assignedMediatorLabel = getValueLabel();
        assignedMediatorBox = createAndGetDescriptionAndValueBox("bisqEasy.openTrades.tradeDetails.assignedMediator", assignedMediatorLabel);

        Region overviewLine = getLine();
        Label overviewLabel = new Label(Res.get("bisqEasy.openTrades.tradeDetails.overview").toUpperCase());
        overviewLabel.getStyleClass().addAll("text-fill-grey-dimmed", "font-light", "medium-text");
        Label detailsLabel = new Label(Res.get("bisqEasy.openTrades.tradeDetails.details").toUpperCase());
        detailsLabel.getStyleClass().addAll("text-fill-grey-dimmed", "font-light", "medium-text");
        Region detailsLine = getLine();

        VBox.setMargin(headline, new Insets(-5, 0, 5, 0));
        VBox.setMargin(overviewLabel, new Insets(0, 0, -5, 0));
        VBox.setMargin(detailsLabel, new Insets(15, 0, -5, 0));
        VBox content = new VBox(10,
                headline,
                overviewLabel,
                overviewLine,
                tradersAndRoleBox,
                amountAndPriceBox,
                paymentMethodsBox,
                paymentAccountDataBox,
                btcPaymentAddressBox,
                paymentProofBox,
                detailsLabel,
                detailsLine,
                tradeIdBox,
                tradeDateBox,
                tradeDurationBox,
                offerTypeAndMarketBox,
                peerNetworkAddressBox,
                assignedMediatorBox
        );
        content.setAlignment(Pos.CENTER_LEFT);

        root.setAlignment(Pos.TOP_CENTER);
        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);

        VBox.setMargin(content, new Insets(-40, 80, 0, 80));
        VBox.setVgrow(content, Priority.ALWAYS);
        root.getChildren().addAll(closeButtonRow, content);
    }

    private static BisqMenuItem getTradeIdCopyButton(String tooltip) {
        BisqMenuItem bisqMenuItem = new BisqMenuItem("copy-grey", "copy-white");
        bisqMenuItem.setTooltip(tooltip);
        return bisqMenuItem;
    }

    @Override
    protected void onViewAttached() {
        tradeDateLabel.setText(model.getTradeDate());

        tradeDurationBox.setVisible(model.getTradeDuration().isPresent());
        tradeDurationBox.setManaged(model.getTradeDuration().isPresent());
        tradeDurationLabel.setText(model.getTradeDuration().orElse(""));

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

        paymentProofTitleLabel.setText(model.isOnChainSettlement()
                ? Res.get("bisqEasy.openTrades.tradeDetails.txId")
                : Res.get("bisqEasy.openTrades.tradeDetails.lightningPreImage"));
        paymentProofDetailsLabel.setText(model.getPaymentProof());
        paymentProofCopyButton.setTooltip(model.isOnChainSettlement()
                ? Res.get("bisqEasy.openTrades.tradeDetails.txId.copy")
                : Res.get("bisqEasy.openTrades.tradeDetails.lightningPreImage.copy"));
        paymentProofBox.setVisible(model.isPaymentProofVisible());
        paymentProofBox.setManaged(model.isPaymentProofVisible());

        paymentAccountDataLabel.setText(model.getPaymentAccountData());
        assignedMediatorLabel.setText(model.getAssignedMediator());
        assignedMediatorBox.setVisible(model.isHasMediatorBeenAssigned());
        assignedMediatorBox.setManaged(model.isHasMediatorBeenAssigned());
        paymentAccountDataCopyButton.setVisible(!model.isPaymentAccountDataEmpty());
        paymentAccountDataCopyButton.setVisible(!model.isPaymentAccountDataEmpty());
        btcPaymentAddressCopyButton.setVisible(!model.isBtcPaymentDataEmpty());
        btcPaymentAddressCopyButton.setManaged(!model.isBtcPaymentDataEmpty());
        paymentProofCopyButton.setVisible(!model.isPaymentProofEmpty());
        paymentProofCopyButton.setVisible(!model.isPaymentProofEmpty());

        paymentAccountDataLabel.getStyleClass().clear();
        paymentAccountDataLabel.getStyleClass().add(model.isPaymentAccountDataEmpty()
                ? "text-fill-grey-dimmed"
                : "text-fill-white");
        paymentAccountDataLabel.getStyleClass().add("normal-text");

        btcPaymentAddressDetailsLabel.getStyleClass().clear();
        btcPaymentAddressDetailsLabel.getStyleClass().add(model.isBtcPaymentDataEmpty()
                ? "text-fill-grey-dimmed"
                : "text-fill-white");
        btcPaymentAddressDetailsLabel.getStyleClass().add("normal-text");

        paymentProofDetailsLabel.getStyleClass().clear();
        paymentProofDetailsLabel.getStyleClass().add(model.isPaymentProofEmpty()
                ? "text-fill-grey-dimmed"
                : "text-fill-white");
        paymentProofDetailsLabel.getStyleClass().add("normal-text");

        closeButton.setOnAction(e -> controller.onClose());
        tradersAndRoleCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getPeer()));
        tradeIdCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getTradeId()));
        peerNetworkAddressCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getPeerNetworkAddress()));
        paymentAccountDataCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getPaymentAccountData()));
        btcPaymentAddressCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getBtcPaymentAddress()));
        paymentProofCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getPaymentProof()));
    }

    @Override
    protected void onViewDetached() {
        closeButton.setOnAction(null);
        tradersAndRoleCopyButton.setOnAction(null);
        tradeIdCopyButton.setOnAction(null);
        peerNetworkAddressCopyButton.setOnAction(null);
        paymentAccountDataCopyButton.setOnAction(null);
        btcPaymentAddressCopyButton.setOnAction(null);
        paymentProofCopyButton.setOnAction(null);
    }

    private HBox createAndGetDescriptionAndValueBox(String descriptionKey, Node valueNode) {
        return createAndGetDescriptionAndValueBox(descriptionKey, valueNode, Optional.empty());
    }

    private HBox createAndGetDescriptionAndValueBox(String descriptionKey, Node detailsNode, BisqMenuItem button) {
        return createAndGetDescriptionAndValueBox(descriptionKey, detailsNode, Optional.of(button));
    }

    private HBox createAndGetDescriptionAndValueBox(String descriptionKey,
                                                    Node detailsNode,
                                                    Optional<BisqMenuItem> button) {
        return createAndGetDescriptionAndValueBox(getDescriptionLabel(Res.get(descriptionKey)), detailsNode, button);
    }

    private HBox createAndGetDescriptionAndValueBox(Label descriptionLabel,
                                                    Node detailsNode,
                                                    BisqMenuItem button) {
        return createAndGetDescriptionAndValueBox(descriptionLabel, detailsNode, Optional.of(button));
    }

    private HBox createAndGetDescriptionAndValueBox(Label descriptionLabel,
                                                    Node detailsNode,
                                                    Optional<BisqMenuItem> button) {
        double width = 180;
        descriptionLabel.setMaxWidth(width);
        descriptionLabel.setMinWidth(width);
        descriptionLabel.setPrefWidth(width);

        HBox hBox = new HBox(descriptionLabel, detailsNode);
        hBox.setAlignment(Pos.BASELINE_LEFT);

        if (button.isPresent()) {
            button.get().useIconOnly(17);
            HBox.setMargin(button.get(), new Insets(0, 0, 0, 40));
            hBox.getChildren().addAll(Spacer.fillHBox(), button.get());
        }
        return hBox;
    }

    private static Label getDescriptionLabel(String description) {
        Label label = new Label(description);
        label.getStyleClass().addAll("text-fill-grey-dimmed", "medium-text", "font-light");
        return label;
    }

    private static Label getValueLabel() {
        Label label = new Label();
        label.getStyleClass().addAll("text-fill-white", "normal-text", "font-light");
        return label;
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
