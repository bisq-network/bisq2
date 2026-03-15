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

package bisq.desktop.main.content.mu_sig.trade.pending.trade_details;

import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.NavigationView;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.main.content.mu_sig.trade.pending.trade_details.MuSigTradeDetailsRecords.SecurityDepositInfo;
import bisq.desktop.main.content.mu_sig.trade.pending.trade_details.MuSigTradeDetailsRecords.TradeFeeInfo;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

import static bisq.desktop.main.content.mu_sig.trade.pending.trade_details.MuSigTradeDetailsViewHelper.ValueLabelStyle.DIMMED;
import static bisq.desktop.main.content.mu_sig.trade.pending.trade_details.MuSigTradeDetailsViewHelper.ValueLabelStyle.SMALL;
import static bisq.desktop.main.content.mu_sig.trade.pending.trade_details.MuSigTradeDetailsViewHelper.ValueLabelStyle.SMALL_DIMMED;
import static bisq.desktop.main.content.mu_sig.trade.pending.trade_details.MuSigTradeDetailsViewHelper.createAndGetDescriptionAndValueBox;
import static bisq.desktop.main.content.mu_sig.trade.pending.trade_details.MuSigTradeDetailsViewHelper.getCopyButton;
import static bisq.desktop.main.content.mu_sig.trade.pending.trade_details.MuSigTradeDetailsViewHelper.getDescriptionLabel;
import static bisq.desktop.main.content.mu_sig.trade.pending.trade_details.MuSigTradeDetailsViewHelper.createSeparatorLine;
import static bisq.desktop.main.content.mu_sig.trade.pending.trade_details.MuSigTradeDetailsViewHelper.getValueBox;
import static bisq.desktop.main.content.mu_sig.trade.pending.trade_details.MuSigTradeDetailsViewHelper.getValueLabel;

@Slf4j
class MuSigTradeDetailsView extends NavigationView<VBox, MuSigTradeDetailsModel, MuSigTradeDetailsController> {
    private final Button closeButton;
    private final Label tradeDateLabel, tradeDurationLabel, meLabel, peerLabel, offerTypeLabel, marketLabel, nonBtcAmountLabel,
            nonBtcCurrencyLabel, btcAmountLabel, priceLabel, priceCodesLabel, priceSpecLabel, paymentMethodValue,
            securityDepositLabel, securityDepositPercentLabel, securityDepositOpenParenthesisLabel, securityDepositClosingParenthesisLabel,
            feeAmountLabel, feePercentLabel,
            tradeIdLabel, peerNetworkAddressLabel, peersPaymentAccountData, depositTxDetailsLabel, peersAccountPayloadDescription,
            assignedMediatorLabel;
    private final BisqMenuItem tradersAndRoleCopyButton, tradeIdCopyButton, peerNetworkAddressCopyButton,
            depositTxCopyButton, depositTxExplorerLinkButton, depositTxExplorerButton, peersAccountDataCopyButton;
    private final HBox assignedMediatorBox, depositTxBox, tradeDurationBox, paymentMethodsBox;


    public MuSigTradeDetailsView(MuSigTradeDetailsModel model, MuSigTradeDetailsController controller) {
        super(new VBox(10), model, controller);

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);

        closeButton = BisqIconButton.createIconButton("close");
        HBox closeButtonRow = new HBox(Spacer.fillHBox(), closeButton);
        closeButtonRow.setPadding(new Insets(15, 15, 0, 0));

        Label headline = new Label(Res.get("muSig.trade.details.headline"));
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
        HBox tradeDateBox = createAndGetDescriptionAndValueBox("muSig.trade.details.tradeDate", tradeDateLabel);

        // Trade duration
        tradeDurationLabel = getValueLabel();
        tradeDurationBox = createAndGetDescriptionAndValueBox("muSig.trade.details.tradeDuration", tradeDurationLabel);

        // Traders / Roles
        Label mePrefixLabel = getValueLabel(DIMMED, Res.get("muSig.trade.details.tradersAndRole.me"));
        meLabel = getValueLabel();
        Label offerTypeAndRoleSlashLabel = getValueLabel(DIMMED, "/");
        Label peerPrefixLabel = getValueLabel(DIMMED, Res.get("muSig.trade.details.tradersAndRole.peer"));
        peerLabel = getValueLabel();
        HBox tradersAndRoleDetailsHBox = getValueBox(mePrefixLabel, meLabel, offerTypeAndRoleSlashLabel, peerPrefixLabel, peerLabel);
        tradersAndRoleCopyButton = getCopyButton(Res.get("muSig.trade.details.tradersAndRole.copy"));
        HBox tradersAndRoleBox = createAndGetDescriptionAndValueBox("muSig.trade.details.tradersAndRole",
                tradersAndRoleDetailsHBox, tradersAndRoleCopyButton);

        // Offer type and market
        offerTypeLabel = getValueLabel();
        Label offerAndMarketslashLabel = getValueLabel(DIMMED, "/");
        marketLabel = getValueLabel();
        HBox offerTypeAndMarketDetailsHBox = getValueBox(offerTypeLabel, offerAndMarketslashLabel, marketLabel);
        HBox offerTypeAndMarketBox = createAndGetDescriptionAndValueBox( "muSig.trade.details.offerTypeAndMarket", offerTypeAndMarketDetailsHBox);

        // Amount and price
        nonBtcAmountLabel = getValueLabel();
        nonBtcCurrencyLabel = getValueLabel(SMALL);

        Label openParenthesisLabel = getValueLabel(DIMMED, "(");
        btcAmountLabel = getValueLabel(DIMMED);
        btcAmountLabel.setPadding(new Insets(0, 5, 0, 0));
        Label btcLabel = getValueLabel(SMALL_DIMMED, "BTC");
        Label closingParenthesisLabel = getValueLabel(DIMMED, ")");
        HBox btcAmountHBox = getValueBox(openParenthesisLabel, btcAmountLabel, btcLabel, closingParenthesisLabel);
        Label atLabel = getValueLabel(DIMMED, "@");
        priceLabel = getValueLabel();
        priceCodesLabel = getValueLabel(SMALL);
        priceSpecLabel = getValueLabel(DIMMED);
        HBox amountAndPriceDetailsHBox = getValueBox(nonBtcAmountLabel, nonBtcCurrencyLabel, btcAmountHBox,
                atLabel, priceLabel, priceCodesLabel, priceSpecLabel);
        HBox amountAndPriceBox = createAndGetDescriptionAndValueBox(
                "muSig.trade.details.amountAndPrice", amountAndPriceDetailsHBox);

        // Security deposits
        securityDepositLabel = getValueLabel();
        securityDepositOpenParenthesisLabel = getValueLabel(DIMMED, "(");
        securityDepositPercentLabel = getValueLabel(DIMMED);
        securityDepositClosingParenthesisLabel = getValueLabel(DIMMED, ")");
        HBox securityDepositPercentBox = new HBox(securityDepositOpenParenthesisLabel,
                securityDepositPercentLabel,
                securityDepositClosingParenthesisLabel);
        securityDepositPercentBox.setAlignment(Pos.BASELINE_LEFT);
        HBox securityDepositValueBox = getValueBox(securityDepositLabel, securityDepositPercentBox);
        HBox securityDepositBox = createAndGetDescriptionAndValueBox("muSig.trade.details.securityDeposit", securityDepositValueBox);

        // Fee
        feeAmountLabel = getValueLabel();
        feePercentLabel = getValueLabel(DIMMED);
        HBox feeValueBox = getValueBox(feeAmountLabel, feePercentLabel);
        HBox feeBox = createAndGetDescriptionAndValueBox("muSig.offer.wizard.review.feeDescription", feeValueBox);

        // Payment method
        paymentMethodValue = getValueLabel();
        paymentMethodsBox = createAndGetDescriptionAndValueBox("muSig.trade.details.paymentAndSettlementMethod", paymentMethodValue);

        // Trade ID
        tradeIdLabel = getValueLabel();
        tradeIdCopyButton = getCopyButton(Res.get("muSig.trade.details.tradeId.copy"));
        HBox tradeIdBox = createAndGetDescriptionAndValueBox("muSig.trade.details.tradeId",
                tradeIdLabel, tradeIdCopyButton);

        // Peer network address
        peerNetworkAddressLabel = getValueLabel();
        peerNetworkAddressCopyButton = getCopyButton(Res.get("muSig.trade.details.peerNetworkAddress.copy"));
        HBox peerNetworkAddressBox = createAndGetDescriptionAndValueBox("muSig.trade.details.peerNetworkAddress",
                peerNetworkAddressLabel, peerNetworkAddressCopyButton);

        // Payment account data
        peersAccountPayloadDescription = getDescriptionLabel("");
        peersPaymentAccountData = getValueLabel();
        peersAccountDataCopyButton = getCopyButton(Res.get("muSig.trade.details.paymentAccountData.copy"));
        HBox paymentAccountDataBox = createAndGetDescriptionAndValueBox(peersAccountPayloadDescription,
                peersPaymentAccountData, peersAccountDataCopyButton);

        // DepositTx
        Label depositTxTitleLabel = getDescriptionLabel(Res.get("muSig.trade.details.depositTxId"));
        depositTxDetailsLabel = getValueLabel();
        depositTxCopyButton = getCopyButton(Res.get("muSig.trade.details.depositTxId.copy"));
        depositTxExplorerLinkButton = new BisqMenuItem("link-grey", "link-white");
        depositTxExplorerLinkButton.setTooltip(Res.get("muSig.trade.details.depositTxId.copy.explorerLink.tooltip"));
        depositTxExplorerButton = new BisqMenuItem("open-link-grey", "open-link-white");
        depositTxExplorerButton.setTooltip(Res.get("muSig.trade.details.depositTxId.open.explorerLink.tooltip"));
        depositTxBox = createAndGetDescriptionAndValueBox(depositTxTitleLabel, depositTxDetailsLabel,
                List.of(depositTxCopyButton, depositTxExplorerLinkButton, depositTxExplorerButton));

        // Assigned mediator
        assignedMediatorLabel = getValueLabel();
        assignedMediatorBox = createAndGetDescriptionAndValueBox("muSig.trade.details.assignedMediator", assignedMediatorLabel);

        Region overviewLine = createSeparatorLine();
        Label overviewLabel = new Label(Res.get("muSig.trade.details.overview").toUpperCase());
        overviewLabel.getStyleClass().addAll("text-fill-grey-dimmed", "font-light", "medium-text");
        Label detailsLabel = new Label(Res.get("muSig.trade.details.details").toUpperCase());
        detailsLabel.getStyleClass().addAll("text-fill-grey-dimmed", "font-light", "medium-text");
        Region detailsLine = createSeparatorLine();

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
                securityDepositBox,
                feeBox,
                offerTypeAndMarketBox,
                peerNetworkAddressBox,
                assignedMediatorBox
        );
        content.setPadding(new Insets(0, 20, 0, 0));

        scrollPane.setContent(content);
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
        showBlockExplorerLink(model.isBlockExplorerLinkVisible());

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

        Optional<SecurityDepositInfo> depositInfo = model.getSecurityDepositInfo();
        boolean showDepositPercent = depositInfo.map(SecurityDepositInfo::isMatching).orElse(false);
        securityDepositLabel.setText(depositInfo.map(SecurityDepositInfo::amountText).orElse(Res.get("data.na")));
        securityDepositPercentLabel.setText(depositInfo.map(SecurityDepositInfo::percentText).orElse(Res.get("data.na")));
        securityDepositPercentLabel.setVisible(showDepositPercent);
        securityDepositPercentLabel.setManaged(showDepositPercent);
        securityDepositOpenParenthesisLabel.setVisible(showDepositPercent);
        securityDepositOpenParenthesisLabel.setManaged(showDepositPercent);
        securityDepositClosingParenthesisLabel.setVisible(showDepositPercent);
        securityDepositClosingParenthesisLabel.setManaged(showDepositPercent);

        Optional<TradeFeeInfo> feeInfo = model.getTradeFeeInfo();
        feeAmountLabel.setText(feeInfo.map(TradeFeeInfo::amountText).orElse(Res.get("data.na")));
        boolean showFeePercent = feeInfo.map(TradeFeeInfo::showPercent).orElse(false);
        feePercentLabel.setText(feeInfo.map(TradeFeeInfo::percentText).orElse(Res.get("data.na")));
        feePercentLabel.setVisible(showFeePercent);
        feePercentLabel.setManaged(showFeePercent);

        closeButton.setOnAction(e -> controller.onClose());
        tradersAndRoleCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getPeer()));
        tradeIdCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getTradeId()));
        peerNetworkAddressCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getPeerNetworkAddress()));
        peersAccountDataCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getPeersPaymentAccountData()));
        depositTxCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getDepositTxId()));
        depositTxExplorerLinkButton.setOnAction(e -> controller.onCopyExplorerLink());
        depositTxExplorerButton.setOnAction(e -> controller.openExplorer());
    }

    @Override
    protected void onViewDetached() {
        closeButton.setOnAction(null);
        tradersAndRoleCopyButton.setOnAction(null);
        tradeIdCopyButton.setOnAction(null);
        peerNetworkAddressCopyButton.setOnAction(null);
        peersAccountDataCopyButton.setOnAction(null);
        depositTxCopyButton.setOnAction(null);
        depositTxExplorerLinkButton.setOnAction(null);
        depositTxExplorerButton.setOnAction(null);
    }

    private void showBlockExplorerLink(boolean value) {
        depositTxExplorerLinkButton.setVisible(value);
        depositTxExplorerLinkButton.setManaged(value);
        depositTxExplorerButton.setVisible(value);
        depositTxExplorerButton.setManaged(value);
    }
}
