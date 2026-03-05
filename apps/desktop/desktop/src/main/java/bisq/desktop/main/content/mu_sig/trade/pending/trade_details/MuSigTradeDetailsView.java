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

import static bisq.desktop.components.helpers.LabeledValueRowFactory.createAndGetDescriptionAndValueBox;
import static bisq.desktop.components.helpers.LabeledValueRowFactory.createSeparatorLine;
import static bisq.desktop.components.helpers.LabeledValueRowFactory.getCopyButton;
import static bisq.desktop.components.helpers.LabeledValueRowFactory.getDescriptionLabel;
import static bisq.desktop.components.helpers.LabeledValueRowFactory.getValueLabel;

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
        Label mePrefixLabel = new Label(Res.get("muSig.trade.details.tradersAndRole.me"));
        mePrefixLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        meLabel = getValueLabel();
        Label offerTypeAndRoleSlashLabel = new Label("/");
        offerTypeAndRoleSlashLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        Label peerPrefixLabel = new Label(Res.get("muSig.trade.details.tradersAndRole.peer"));
        peerPrefixLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        peerLabel = getValueLabel();
        HBox tradersAndRoleDetailsHBox = new HBox(5, mePrefixLabel, meLabel, offerTypeAndRoleSlashLabel, peerPrefixLabel, peerLabel);
        tradersAndRoleDetailsHBox.setAlignment(Pos.BASELINE_LEFT);
        tradersAndRoleCopyButton = getCopyButton(Res.get("muSig.trade.details.tradersAndRole.copy"));
        HBox tradersAndRoleBox = createAndGetDescriptionAndValueBox("muSig.trade.details.tradersAndRole",
                tradersAndRoleDetailsHBox, tradersAndRoleCopyButton);

        // Offer type and market
        offerTypeLabel = getValueLabel();
        Label offerAndMarketslashLabel = new Label("/");
        offerAndMarketslashLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        marketLabel = getValueLabel();
        HBox offerTypeAndMarketDetailsHBox = new HBox(5, offerTypeLabel, offerAndMarketslashLabel, marketLabel);
        offerTypeAndMarketDetailsHBox.setAlignment(Pos.BASELINE_LEFT);
        HBox offerTypeAndMarketBox = createAndGetDescriptionAndValueBox("muSig.trade.details.offerTypeAndMarket",
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
        HBox amountAndPriceBox = createAndGetDescriptionAndValueBox("muSig.trade.details.amountAndPrice", amountAndPriceDetailsHBox);

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
        depositTxBox = createAndGetDescriptionAndValueBox(depositTxTitleLabel,
                depositTxDetailsLabel, depositTxCopyButton);

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
}
