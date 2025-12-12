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

package bisq.desktop.main.content.authorized_role.mediator.details;

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
public class MediationCaseDetailsView extends NavigationView<VBox, MediationCaseDetailsModel, MediationCaseDetailsController> {
    private final Button closeButton;
    private final Label tradeDateLabel, offerTypeLabel, marketLabel, fiatAmountLabel,
            fiatCurrencyLabel, btcAmountLabel, priceLabel, priceCodesLabel, priceSpecLabel, paymentMethodLabel,
            settlementMethodLabel, tradeIdLabel, buyerNetworkAddressLabel, sellerNetworkAddressLabel,
            buyerUserNameLabel, sellerUserNameLabel;
    private final BisqMenuItem tradeIdCopyButton, buyerNetworkAddressCopyButton, sellerNetworkAddressCopyButton,
            buyerUserNameCopyButton, sellerUserNameCopyButton;

    public MediationCaseDetailsView(MediationCaseDetailsModel model, MediationCaseDetailsController controller) {
        super(new VBox(), model, controller);

        closeButton = BisqIconButton.createIconButton("close");
        HBox closeButtonRow = new HBox(Spacer.fillHBox(), closeButton);
        closeButtonRow.setPadding(new Insets(15, 15, 0, 0));

        Label headline = new Label(Res.get("authorizedRole.mediator.mediationCaseDetails.headline"));
        headline.getStyleClass().add("bisq-text-17");
        headline.setAlignment(Pos.CENTER);
        headline.setMaxWidth(Double.MAX_VALUE);

        // UserNames
        buyerUserNameLabel = getValueLabel();
        buyerUserNameCopyButton = getTradeIdCopyButton(Res.get("authorizedRole.mediator.mediationCaseDetails.buyerUserName.copy"));
        HBox buyerUserNameBox = createAndGetDescriptionAndValueBox("authorizedRole.mediator.mediationCaseDetails.buyerUserName",
                buyerUserNameLabel, buyerUserNameCopyButton);

        sellerUserNameLabel = getValueLabel();
        sellerUserNameCopyButton = getTradeIdCopyButton(Res.get("authorizedRole.mediator.mediationCaseDetails.sellerUserName.copy"));
        HBox sellerUserNameBox = createAndGetDescriptionAndValueBox("authorizedRole.mediator.mediationCaseDetails.sellerUserName",
                sellerUserNameLabel, sellerUserNameCopyButton);


        // Trade date
        tradeDateLabel = getValueLabel();
        HBox tradeDateBox = createAndGetDescriptionAndValueBox("bisqEasy.openTrades.tradeDetails.tradeDate", tradeDateLabel);


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

        // Network addresses
        buyerNetworkAddressLabel = getValueLabel();
        buyerNetworkAddressCopyButton = getTradeIdCopyButton(Res.get("authorizedRole.mediator.mediationCaseDetails.buyerNetworkAddress.copy"));
        HBox peerNetworkAddressBox = createAndGetDescriptionAndValueBox("authorizedRole.mediator.mediationCaseDetails.buyerNetworkAddress",
                buyerNetworkAddressLabel, buyerNetworkAddressCopyButton);

        sellerNetworkAddressLabel = getValueLabel();
        sellerNetworkAddressCopyButton = getTradeIdCopyButton(Res.get("authorizedRole.mediator.mediationCaseDetails.sellerNetworkAddress.copy"));
        HBox sellerNetworkAddressBox = createAndGetDescriptionAndValueBox("authorizedRole.mediator.mediationCaseDetails.sellerNetworkAddress",
                sellerNetworkAddressLabel, sellerNetworkAddressCopyButton);

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
                buyerUserNameBox,
                sellerUserNameBox,
                amountAndPriceBox,
                paymentMethodsBox,
                detailsLabel,
                detailsLine,
                tradeIdBox,
                tradeDateBox,
                offerTypeAndMarketBox,
                peerNetworkAddressBox,
                sellerNetworkAddressBox
        );
        content.setAlignment(Pos.CENTER_LEFT);

        root.setAlignment(Pos.TOP_CENTER);
        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT - 60);

        VBox.setMargin(content, new Insets(-40, 80, 0, 80));
        VBox.setVgrow(content, Priority.ALWAYS);
        root.getChildren().addAll(closeButtonRow, content);
    }

    @Override
    protected void onViewAttached() {
        buyerUserNameLabel.setText(model.getBuyerUserName());
        sellerUserNameLabel.setText(model.getSellerUserName());
        tradeDateLabel.setText(model.getTradeDate());
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
        buyerNetworkAddressLabel.setText(model.getBuyerNetworkAddress());
        sellerNetworkAddressLabel.setText(model.getSellerNetworkAddress());

        closeButton.setOnAction(e -> controller.onClose());
        buyerUserNameCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getBuyerUserName()));
        sellerUserNameCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getSellerUserName()));
        tradeIdCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getTradeId()));
        buyerNetworkAddressCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getBuyerNetworkAddress()));
        sellerNetworkAddressCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getSellerNetworkAddress()));
    }

    @Override
    protected void onViewDetached() {
        closeButton.setOnAction(null);
        buyerUserNameCopyButton.setOnAction(null);
        sellerUserNameCopyButton.setOnAction(null);
        tradeIdCopyButton.setOnAction(null);
        buyerNetworkAddressCopyButton.setOnAction(null);
        sellerNetworkAddressCopyButton.setOnAction(null);
    }

    private static BisqMenuItem getTradeIdCopyButton(String tooltip) {
        BisqMenuItem bisqMenuItem = new BisqMenuItem("copy-grey", "copy-white");
        bisqMenuItem.setTooltip(tooltip);
        return bisqMenuItem;
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
