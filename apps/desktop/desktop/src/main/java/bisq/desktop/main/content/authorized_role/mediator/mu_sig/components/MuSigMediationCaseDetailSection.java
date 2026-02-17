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

package bisq.desktop.main.content.authorized_role.mediator.mu_sig.components;

import bisq.common.market.Market;
import bisq.common.monetary.Monetary;
import bisq.contract.mu_sig.MuSigContract;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.main.content.authorized_role.mediator.mu_sig.MuSigMediationCaseListItem;
import bisq.i18n.Res;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.CollateralOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.support.mediation.mu_sig.MuSigMediationCase;
import bisq.support.mediation.mu_sig.MuSigMediationRequest;
import bisq.trade.mu_sig.MuSigTradeUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static bisq.desktop.main.content.authorized_role.mediator.mu_sig.components.MuSigMediationCaseDetailsViewHelper.createAndGetDescriptionAndValueBox;
import static bisq.desktop.main.content.authorized_role.mediator.mu_sig.components.MuSigMediationCaseDetailsViewHelper.getCopyButton;
import static bisq.desktop.main.content.authorized_role.mediator.mu_sig.components.MuSigMediationCaseDetailsViewHelper.getDescriptionLabel;
import static bisq.desktop.main.content.authorized_role.mediator.mu_sig.components.MuSigMediationCaseDetailsViewHelper.getLine;
import static bisq.desktop.main.content.authorized_role.mediator.mu_sig.components.MuSigMediationCaseDetailsViewHelper.getValueLabel;

public class MuSigMediationCaseDetailSection {

    private final Controller controller;

    public MuSigMediationCaseDetailSection(ServiceProvider serviceProvider, boolean isCompactView) {
        this.controller = new Controller(serviceProvider, isCompactView);
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

        private Controller(ServiceProvider serviceProvider, boolean isCompactView) {
            model = new Model();
            model.setCompactView(isCompactView);
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

            Optional<CollateralOption> collateralOption = OfferOptionUtil.findCollateralOption(offer.getOfferOptions());
            if (collateralOption.isEmpty()) {
                log.warn("CollateralOption not found in offer options. tradeId={}", tradeId);
                model.setSecurityDepositInfo(Optional.empty());
            } else if (collateralOption.get().getBuyerSecurityDeposit() != collateralOption.get().getSellerSecurityDeposit()) {
                log.warn("Buyer and seller security deposits do not match. tradeId={}", tradeId);
                String mismatch = Res.get("authorizedRole.mediator.mediationCaseDetails.securityDepositMismatch");
                model.setSecurityDepositInfo(Optional.of(new Model.SecurityDepositInfo(
                        0,
                        mismatch,
                        mismatch,
                        false)));
            } else {
                double securityDeposit = collateralOption.get().getBuyerSecurityDeposit();
                model.setSecurityDepositInfo(Optional.of(new Model.SecurityDepositInfo(
                        securityDeposit,
                        calculateSecurityDeposit(offer.getMarket(), contract, securityDeposit),
                        PercentageFormatter.formatToPercentWithSymbol(securityDeposit, 0),
                        true)));
            }

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

            model.setBuyerNetworkAddress(buyer.getUserProfile().getAddressByTransportDisplayString(50));
            model.setSellerNetworkAddress(seller.getUserProfile().getAddressByTransportDisplayString(50));
        }

        @Override
        public void onDeactivate() {
            model.setSecurityDepositInfo(Optional.empty());
        }

        private static String calculateSecurityDeposit(Market market,
                                                       MuSigContract contract,
                                                       double securityDepositAsPercent) {
            Monetary baseSideMonetary = MuSigTradeUtils.getBaseSideMonetary(contract);
            Monetary quoteSideMonetary = MuSigTradeUtils.getQuoteSideMonetary(contract);
            Monetary securityDeposit = OfferAmountUtil.calculateSecurityDepositAsBTC(market, baseSideMonetary, quoteSideMonetary, securityDepositAsPercent);
            return OfferAmountFormatter.formatDepositAmountAsBTC(securityDeposit);
        }
    }

    @Slf4j
    @Getter
    @Setter
    private static class Model implements bisq.desktop.common.view.Model {
        private MuSigMediationCaseListItem muSigMediationCaseListItem;

        private boolean isCompactView;

        private String tradeId;
        private String tradeDate;

        private String offerType;
        private String market;

        private Optional<SecurityDepositInfo> securityDepositInfo = Optional.empty();

        private record SecurityDepositInfo(double percentValue, String amountText, String percentText,
                                           boolean isMatching) {
        }

        private String buyerNetworkAddress;
        private String sellerNetworkAddress;
    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {

        private final Label tradeIdLabel, tradeDateLabel, offerTypeLabel, marketLabel;
        private Label securityDepositLabel, securityDepositPercentLabel, openParenthesisLabel, closingParenthesisLabel,
                buyerNetworkAddressLabel, sellerNetworkAddressLabel;
        private final BisqMenuItem tradeIdCopyButton;
        private BisqMenuItem buyerNetworkAddressCopyButton, sellerNetworkAddressCopyButton;

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

            VBox content;

            if (!model.isCompactView()) {
                Label detailsLabel = new Label(Res.get("bisqEasy.openTrades.tradeDetails.details").toUpperCase());
                detailsLabel.getStyleClass().addAll("text-fill-grey-dimmed", "font-light", "medium-text");
                Region detailsLine = getLine();

                VBox.setMargin(detailsLabel, new Insets(15, 0, -5, 0));

                // Security deposits
                securityDepositLabel = getValueLabel();
                securityDepositPercentLabel = new Label();
                securityDepositPercentLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
                openParenthesisLabel = new Label("(");
                openParenthesisLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
                closingParenthesisLabel = new Label(")");
                closingParenthesisLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
                HBox securityDepositPercentBox = new HBox(openParenthesisLabel,
                        securityDepositPercentLabel,
                        closingParenthesisLabel);
                securityDepositPercentBox.setAlignment(Pos.BASELINE_LEFT);
                HBox securityDepositValueBox = new HBox(5, securityDepositLabel, securityDepositPercentBox);
                securityDepositValueBox.setAlignment(Pos.BASELINE_LEFT);
                HBox securityDepositBox = createAndGetDescriptionAndValueBox(
                        getDescriptionLabel(Res.get("authorizedRole.mediator.mediationCaseDetails.securityDeposit")),
                        securityDepositValueBox,
                        Optional.empty()
                );

                // Network addresses
                buyerNetworkAddressLabel = getValueLabel();
                buyerNetworkAddressCopyButton = getCopyButton(Res.get("authorizedRole.mediator.mediationCaseDetails.buyerNetworkAddress.copy"));
                HBox peerNetworkAddressBox = createAndGetDescriptionAndValueBox("authorizedRole.mediator.mediationCaseDetails.buyerNetworkAddress",
                        buyerNetworkAddressLabel, buyerNetworkAddressCopyButton);

                sellerNetworkAddressLabel = getValueLabel();
                sellerNetworkAddressCopyButton = getCopyButton(Res.get("authorizedRole.mediator.mediationCaseDetails.sellerNetworkAddress.copy"));
                HBox sellerNetworkAddressBox = createAndGetDescriptionAndValueBox("authorizedRole.mediator.mediationCaseDetails.sellerNetworkAddress",
                        sellerNetworkAddressLabel, sellerNetworkAddressCopyButton);
                content = new VBox(10,
                        detailsLabel,
                        detailsLine,
                        tradeIdBox,
                        tradeDateBox,
                        offerTypeAndMarketBox,
                        securityDepositBox,
                        peerNetworkAddressBox,
                        sellerNetworkAddressBox);
            } else {
                content = new VBox(10,
                        tradeIdBox,
                        tradeDateBox,
                        offerTypeAndMarketBox);
            }

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
            if (!model.isCompactView()) {
                Optional<Model.SecurityDepositInfo> info = model.getSecurityDepositInfo();
                boolean showPercent = info.map(Model.SecurityDepositInfo::isMatching).orElse(false);
                securityDepositLabel.setText(info.map(Model.SecurityDepositInfo::amountText).orElse(Res.get("data.na")));
                securityDepositPercentLabel.setText(info.map(Model.SecurityDepositInfo::percentText).orElse(Res.get("data.na")));
                securityDepositPercentLabel.setVisible(showPercent);
                securityDepositPercentLabel.setManaged(showPercent);
                openParenthesisLabel.setVisible(showPercent);
                openParenthesisLabel.setManaged(showPercent);
                closingParenthesisLabel.setVisible(showPercent);
                closingParenthesisLabel.setManaged(showPercent);
                buyerNetworkAddressLabel.setText(model.getBuyerNetworkAddress());
                sellerNetworkAddressLabel.setText(model.getSellerNetworkAddress());
                buyerNetworkAddressCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getBuyerNetworkAddress()));
                sellerNetworkAddressCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getSellerNetworkAddress()));
            }
        }

        @Override
        protected void onViewDetached() {
            tradeIdCopyButton.setOnAction(null);
            if (!model.isCompactView()) {
                buyerNetworkAddressCopyButton.setOnAction(null);
                sellerNetworkAddressCopyButton.setOnAction(null);
            }
        }
    }
}
