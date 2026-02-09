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

package bisq.desktop.main.content.authorized_role.mediator.mu_sig.details;

import bisq.contract.mu_sig.MuSigContract;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.main.content.authorized_role.mediator.mu_sig.MuSigMediationCaseListItem;
import bisq.i18n.Res;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.presentation.formatters.DateFormatter;
import bisq.support.mediation.mu_sig.MuSigMediationCase;
import bisq.support.mediation.mu_sig.MuSigMediationRequest;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import static bisq.desktop.main.content.authorized_role.mediator.mu_sig.details.MuSigMediationCaseDetailsViewHelper.createAndGetDescriptionAndValueBox;
import static bisq.desktop.main.content.authorized_role.mediator.mu_sig.details.MuSigMediationCaseDetailsViewHelper.getCopyButton;
import static bisq.desktop.main.content.authorized_role.mediator.mu_sig.details.MuSigMediationCaseDetailsViewHelper.getLine;
import static bisq.desktop.main.content.authorized_role.mediator.mu_sig.details.MuSigMediationCaseDetailsViewHelper.getValueLabel;

public class MuSigMediationCaseDetailSection {

    private final Controller controller;

    public MuSigMediationCaseDetailSection(ServiceProvider serviceProvider) {
        this.controller = new Controller(serviceProvider);
    }

    public VBox getRoot() {
        return controller.view.getRoot();
    }

    public void setMediationCaseListItem(MuSigMediationCaseListItem item) {
        controller.setMediationCaseListItem(item);
    }

    @Slf4j
    private static class Controller implements bisq.desktop.common.view.Controller {

        @Getter
        private final View view;
        private final Model model;

        private Controller(ServiceProvider serviceProvider) {
            model = new Model();
            view = new View(new VBox(), model, this);
        }

        private void setMediationCaseListItem(MuSigMediationCaseListItem item) {
            model.setMuSigMediationCaseListItem(item);
        }

        @Override
        public void onActivate() {
            MuSigMediationCaseListItem muSigMediationCaseListItem = model.getMuSigMediationCaseListItem();
            MuSigMediationCase muSigMediationCase = muSigMediationCaseListItem.getMuSigMediationCase();
            MuSigMediationRequest muSigMediationRequest = muSigMediationCase.getMuSigMediationRequest();
            MuSigContract contract = muSigMediationRequest.getContract();
            MuSigOffer offer = contract.getOffer();
            String tradeId = muSigMediationRequest.getTradeId();

            MuSigMediationCaseListItem.Trader maker = muSigMediationCaseListItem.getMaker();
            MuSigMediationCaseListItem.Trader taker = muSigMediationCaseListItem.getTaker();
            MuSigMediationCaseListItem.Trader buyer = offer.getDirection().isBuy() ? maker : taker;
            MuSigMediationCaseListItem.Trader seller = offer.getDirection().isSell() ? maker : taker;

            model.setTradeId(tradeId);
            model.setTradeDate(DateFormatter.formatDateTime(contract.getTakeOfferDate()));

            model.setOfferType(offer.getDirection().isBuy()
                    ? Res.get("bisqEasy.openTrades.tradeDetails.offerTypeAndMarket.buyOffer")
                    : Res.get("bisqEasy.openTrades.tradeDetails.offerTypeAndMarket.sellOffer"));
            model.setMarket(Res.get("bisqEasy.openTrades.tradeDetails.offerTypeAndMarket.fiatMarket",
                    offer.getMarket().getQuoteCurrencyCode()));
            model.setBuyerSecurityDeposit(Res.get("bisqEasy.openTrades.tradeDetails.dataNotYetProvided"));
            model.setBuyerSecurityDepositEmpty(true);
            model.setSellerSecurityDeposit(Res.get("bisqEasy.openTrades.tradeDetails.dataNotYetProvided"));
            model.setSellerSecurityDepositEmpty(true);

            model.setBuyerNetworkAddress(buyer.getUserProfile().getAddressByTransportDisplayString(50));
            model.setSellerNetworkAddress(seller.getUserProfile().getAddressByTransportDisplayString(50));
        }

        @Override
        public void onDeactivate() {
        }
    }

    @Slf4j
    @Getter
    @Setter
    private static class Model implements bisq.desktop.common.view.Model {
        private MuSigMediationCaseListItem muSigMediationCaseListItem;

        private String tradeId;
        private String tradeDate;

        private String offerType;
        private String market;

        private String buyerSecurityDeposit;
        private boolean isBuyerSecurityDepositEmpty;
        private String sellerSecurityDeposit;
        private boolean isSellerSecurityDepositEmpty;

        private String buyerNetworkAddress;
        private String sellerNetworkAddress;
    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {

        private final Label tradeIdLabel, tradeDateLabel, offerTypeLabel, marketLabel,
                buyerSecurityDepositLabel, sellerSecurityDepositLabel,
                buyerNetworkAddressLabel, sellerNetworkAddressLabel;
        private final BisqMenuItem tradeIdCopyButton,
                buyerNetworkAddressCopyButton, sellerNetworkAddressCopyButton;

        public View(VBox root, Model model, Controller controller) {
            super(root, model, controller);

            // Trade ID
            tradeIdLabel = getValueLabel();
            tradeIdCopyButton = getCopyButton(Res.get("bisqEasy.openTrades.tradeDetails.tradeId.copy"));
            HBox tradeIdBox = createAndGetDescriptionAndValueBox("bisqEasy.openTrades.tradeDetails.tradeId",
                    tradeIdLabel, tradeIdCopyButton);

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

            // Security deposits
            buyerSecurityDepositLabel = getValueLabel();
            HBox buyerSecurityDepositBox = createAndGetDescriptionAndValueBox(
                    "authorizedRole.mediator.mediationCaseDetails.buyerSecurityDeposit",
                    buyerSecurityDepositLabel);
            sellerSecurityDepositLabel = getValueLabel();
            HBox sellerSecurityDepositBox = createAndGetDescriptionAndValueBox(
                    "authorizedRole.mediator.mediationCaseDetails.sellerSecurityDeposit",
                    sellerSecurityDepositLabel);

            // Network addresses
            buyerNetworkAddressLabel = getValueLabel();
            buyerNetworkAddressCopyButton = getCopyButton(Res.get("authorizedRole.mediator.mediationCaseDetails.buyerNetworkAddress.copy"));
            HBox peerNetworkAddressBox = createAndGetDescriptionAndValueBox("authorizedRole.mediator.mediationCaseDetails.buyerNetworkAddress",
                    buyerNetworkAddressLabel, buyerNetworkAddressCopyButton);

            sellerNetworkAddressLabel = getValueLabel();
            sellerNetworkAddressCopyButton = getCopyButton(Res.get("authorizedRole.mediator.mediationCaseDetails.sellerNetworkAddress.copy"));
            HBox sellerNetworkAddressBox = createAndGetDescriptionAndValueBox("authorizedRole.mediator.mediationCaseDetails.sellerNetworkAddress",
                    sellerNetworkAddressLabel, sellerNetworkAddressCopyButton);

            Label detailsLabel = new Label(Res.get("bisqEasy.openTrades.tradeDetails.details").toUpperCase());
            detailsLabel.getStyleClass().addAll("text-fill-grey-dimmed", "font-light", "medium-text");
            Region detailsLine = getLine();

            VBox.setMargin(detailsLabel, new Insets(15, 0, -5, 0));

            VBox content = new VBox(10,
                    detailsLabel,
                    detailsLine,
                    tradeIdBox,
                    tradeDateBox,
                    offerTypeAndMarketBox,
                    buyerSecurityDepositBox,
                    sellerSecurityDepositBox,
                    peerNetworkAddressBox,
                    sellerNetworkAddressBox);

            content.setAlignment(Pos.CENTER_LEFT);
            root.getChildren().add(content);
        }

        @Override
        protected void onViewAttached() {
            tradeIdLabel.setText(model.getTradeId());
            tradeIdCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getTradeId()));
            tradeDateLabel.setText(model.getTradeDate());
            offerTypeLabel.setText(model.getOfferType());
            marketLabel.setText(model.getMarket());
            buyerSecurityDepositLabel.setText(model.getBuyerSecurityDeposit());
            sellerSecurityDepositLabel.setText(model.getSellerSecurityDeposit());
            buyerSecurityDepositLabel.getStyleClass().clear();
            buyerSecurityDepositLabel.getStyleClass().add(model.isBuyerSecurityDepositEmpty()
                    ? "text-fill-grey-dimmed"
                    : "text-fill-white");
            buyerSecurityDepositLabel.getStyleClass().add("normal-text");
            sellerSecurityDepositLabel.getStyleClass().clear();
            sellerSecurityDepositLabel.getStyleClass().add(model.isSellerSecurityDepositEmpty()
                    ? "text-fill-grey-dimmed"
                    : "text-fill-white");
            sellerSecurityDepositLabel.getStyleClass().add("normal-text");
            buyerNetworkAddressLabel.setText(model.getBuyerNetworkAddress());
            sellerNetworkAddressLabel.setText(model.getSellerNetworkAddress());
            buyerNetworkAddressCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getBuyerNetworkAddress()));
            sellerNetworkAddressCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getSellerNetworkAddress()));
        }

        @Override
        protected void onViewDetached() {
            tradeIdCopyButton.setOnAction(null);
            buyerNetworkAddressCopyButton.setOnAction(null);
            sellerNetworkAddressCopyButton.setOnAction(null);
        }
    }
}
