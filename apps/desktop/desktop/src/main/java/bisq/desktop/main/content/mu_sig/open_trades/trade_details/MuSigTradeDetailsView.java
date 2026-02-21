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

package bisq.desktop.main.content.mu_sig.open_trades.trade_details;

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
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MuSigTradeDetailsView extends NavigationView<VBox, MuSigTradeDetailsModel, MuSigTradeDetailsController> {
    private final Button closeButton;
    private final Label tradeDateLabel, tradeDurationLabel, meLabel, peerLabel, offerTypeLabel, marketLabel, nonBtcAmountLabel,
            nonBtcCurrencyLabel, btcAmountLabel, priceLabel, priceCodesLabel, priceSpecLabel, paymentMethodValue,
            tradeIdLabel, peerNetworkAddressLabel,
            peersPaymentAccountData, depositTxDetailsLabel, peersAccountPayloadDescription,
            assignedMediatorLabel;
    private final BisqMenuItem tradersAndRoleCopyButton, tradeIdCopyButton, peerNetworkAddressCopyButton,
            depositTxCopyButton, peersAccountDataCopyButton;
    private final HBox assignedMediatorBox, depositTxBox, tradeDurationBox, paymentMethodsBox;

    public MuSigTradeDetailsView(MuSigTradeDetailsModel model, MuSigTradeDetailsController controller) {
        super(new VBox(10), model, controller);

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);

        closeButton = BisqIconButton.createIconButton("close");
        HBox closeButtonRow = new HBox(Spacer.fillHBox(), closeButton);
        closeButtonRow.setPadding(new Insets(15, 15, 0, 0));

        Label headline = new Label(Res.get("bisqEasy.openTrades.tradeDetails.headline"));
        headline.getStyleClass().add("bisq-text-17");
        headline.setAlignment(Pos.CENTER);
        headline.setMaxWidth(Double.MAX_VALUE);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setMargin(scrollPane, new Insets(0, 80, 40, 80));
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        root.getChildren().addAll(closeButtonRow, headline, scrollPane);


        // Content

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
        nonBtcAmountLabel = getValueLabel();
        nonBtcCurrencyLabel = new Label();
        nonBtcCurrencyLabel.getStyleClass().addAll("text-fill-white", "small-text");

        Label openParenthesisLabel = new Label("(");
        openParenthesisLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        btcAmountLabel = getValueLabel();
        btcAmountLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        btcAmountLabel.setPadding(new Insets(0, 5, 0, 0));
        Label btcLabel = new Label("BTC");
        btcLabel.getStyleClass().addAll("text-fill-grey-dimmed", "small-text");
        Label closingParenthesisLabel = new Label(")");
        closingParenthesisLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        HBox btcAmountHBox = new HBox(openParenthesisLabel, btcAmountLabel, btcLabel, closingParenthesisLabel);
        btcAmountHBox.setAlignment(Pos.BASELINE_LEFT);
        Label atLabel = new Label("@");
        atLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        priceLabel = getValueLabel();
        priceCodesLabel = new Label();
        priceCodesLabel.getStyleClass().addAll("text-fill-white", "small-text");
        priceSpecLabel = new Label();
        priceSpecLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        HBox amountAndPriceDetailsHBox = new HBox(5, nonBtcAmountLabel, nonBtcCurrencyLabel, btcAmountHBox,
                atLabel, priceLabel, priceCodesLabel, priceSpecLabel);
        amountAndPriceDetailsHBox.setAlignment(Pos.BASELINE_LEFT);
        HBox amountAndPriceBox = createAndGetDescriptionAndValueBox("bisqEasy.openTrades.tradeDetails.amountAndPrice", amountAndPriceDetailsHBox);

        // Payment method
        paymentMethodValue = getValueLabel();
        paymentMethodsBox = createAndGetDescriptionAndValueBox("muSig.openTrades.tradeDetails.paymentAndSettlementMethod", paymentMethodValue);

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
        peersAccountPayloadDescription = getDescriptionLabel("");
        peersPaymentAccountData = getValueLabel();
        peersAccountDataCopyButton = getTradeIdCopyButton(Res.get("bisqEasy.openTrades.tradeDetails.paymentAccountData.copy"));
        HBox paymentAccountDataBox = createAndGetDescriptionAndValueBox(peersAccountPayloadDescription,
                peersPaymentAccountData, peersAccountDataCopyButton);

        // DepositTx
        Label depositTxTitleLabel = getDescriptionLabel(Res.get("muSig.openTrades.tradeDetails.depositTxId"));
        depositTxDetailsLabel = getValueLabel();
        depositTxCopyButton = getTradeIdCopyButton(Res.get("muSig.openTrades.tradeDetails.depositTxId.copy"));
        depositTxBox = createAndGetDescriptionAndValueBox(depositTxTitleLabel,
                depositTxDetailsLabel, depositTxCopyButton);

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
                overviewLabel,
                overviewLine,
                tradersAndRoleBox,
                amountAndPriceBox,
                paymentMethodsBox,
                paymentAccountDataBox,
                depositTxBox,
                detailsLabel,
                detailsLine,
                tradeIdBox,
                tradeDateBox,
                tradeDurationBox,
                offerTypeAndMarketBox,
                peerNetworkAddressBox,
                assignedMediatorBox
        );
        content.setPadding(new Insets(0, 20, 0, 0));

        scrollPane.setContent(content);
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
        tradeDurationLabel.setText(model.getTradeDuration().orElse(Res.get("data.na")));

        meLabel.setText(model.getMe());
        peerLabel.setText(model.getPeer());
        offerTypeLabel.setText(model.getOfferType());
        marketLabel.setText(model.getMarket());
        nonBtcAmountLabel.setText(model.getNonBtcAmount());
        nonBtcCurrencyLabel.setText(model.getNonBtcCurrency());
        btcAmountLabel.setText(model.getBtcAmount());
        priceLabel.setText(model.getPrice());
        priceCodesLabel.setText(model.getPriceCodes());
        priceSpecLabel.setText(model.getPriceSpec());
        paymentMethodValue.setText(model.getPaymentMethod());
        paymentMethodsBox.setVisible(model.isPaymentMethodsBoxVisible());
        paymentMethodsBox.setManaged(model.isPaymentMethodsBoxVisible());
        tradeIdLabel.setText(model.getTradeId());
        peerNetworkAddressLabel.setText(model.getPeerNetworkAddress());

        depositTxDetailsLabel.setText(model.getDepositTxId());
        depositTxBox.setVisible(model.isDepositTxIdVisible());
        depositTxBox.setManaged(model.isDepositTxIdVisible());

        peersAccountPayloadDescription.setText(model.getPeersPaymentAccountDataDescription());
        peersPaymentAccountData.setText(model.getPeersPaymentAccountData());
        peersAccountDataCopyButton.setVisible(!model.isPaymentAccountDataEmpty());
        peersAccountDataCopyButton.setManaged(!model.isPaymentAccountDataEmpty());

        assignedMediatorLabel.setText(model.getAssignedMediator());
        assignedMediatorBox.setVisible(model.isHasMediatorBeenAssigned());
        assignedMediatorBox.setManaged(model.isHasMediatorBeenAssigned());


        depositTxCopyButton.setVisible(!model.isDepositTxIdEmpty());
        depositTxCopyButton.setManaged(!model.isDepositTxIdEmpty());

        peersPaymentAccountData.getStyleClass().clear();
        peersPaymentAccountData.getStyleClass().add(model.isPaymentAccountDataEmpty()
                ? "text-fill-grey-dimmed"
                : "text-fill-white");
        peersPaymentAccountData.getStyleClass().add("normal-text");

        depositTxDetailsLabel.getStyleClass().clear();
        depositTxDetailsLabel.getStyleClass().add(model.isDepositTxIdEmpty()
                ? "text-fill-grey-dimmed"
                : "text-fill-white");
        depositTxDetailsLabel.getStyleClass().add("normal-text");

        closeButton.setOnAction(e -> controller.onClose());
        tradersAndRoleCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getPeer()));
        tradeIdCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getTradeId()));
        peerNetworkAddressCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getPeerNetworkAddress()));
        peersAccountDataCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getPeersPaymentAccountData()));
        depositTxCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getDepositTxId()));
    }

    @Override
    protected void onViewDetached() {
        closeButton.setOnAction(null);
        tradersAndRoleCopyButton.setOnAction(null);
        tradeIdCopyButton.setOnAction(null);
        peerNetworkAddressCopyButton.setOnAction(null);
        peersAccountDataCopyButton.setOnAction(null);
        depositTxCopyButton.setOnAction(null);
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
        line.getStyleClass().add("separator-line");

        line.setPadding(new Insets(9, 0, 8, 0));
        return line;
    }
}
