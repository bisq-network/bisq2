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

package bisq.desktop.main.content.authorized_role.arbitrator.mu_sig.components;

import bisq.common.encoding.Hex;
import bisq.common.monetary.Monetary;
import bisq.contract.ContractService;
import bisq.contract.mu_sig.MuSigContract;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.main.content.authorized_role.arbitrator.mu_sig.MuSigArbitrationCaseListItem;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.CollateralOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.support.arbitration.mu_sig.MuSigArbitrationCase;
import bisq.support.arbitration.mu_sig.MuSigArbitrationRequest;
import bisq.trade.mu_sig.MuSigTradeUtils;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static bisq.desktop.components.helpers.LabeledValueRowFactory.createAndGetDescriptionAndValueBox;
import static bisq.desktop.components.helpers.LabeledValueRowFactory.getCopyButton;
import static bisq.desktop.components.helpers.LabeledValueRowFactory.getDescriptionLabel;
import static bisq.desktop.components.helpers.LabeledValueRowFactory.getValueLabel;

public class MuSigArbitrationCaseDetailSection {

    private final Controller controller;

    public MuSigArbitrationCaseDetailSection(boolean isCompactView) {
        this.controller = new Controller(isCompactView);
    }

    public VBox getRoot() {
        return controller.view.getRoot();
    }

    public void setArbitrationCaseListItem(MuSigArbitrationCaseListItem item) {
        controller.setArbitrationCaseListItem(item);
    }

    @Slf4j
    private static class Controller implements bisq.desktop.common.view.Controller {

        @Getter
        private final View view;
        private final Model model;

        private Controller(boolean isCompactView) {
            model = new Model();
            model.setCompactView(isCompactView);
            view = new View(new VBox(), model, this);
        }

        private void setArbitrationCaseListItem(MuSigArbitrationCaseListItem item) {
            model.setMuSigArbitrationCaseListItem(item);
        }

        @Override
        public void onActivate() {
            MuSigArbitrationCaseListItem muSigArbitrationCaseListItem = model.getMuSigArbitrationCaseListItem();
            MuSigArbitrationCase muSigArbitrationCase = muSigArbitrationCaseListItem.getMuSigArbitrationCase();
            MuSigArbitrationRequest muSigArbitrationRequest = muSigArbitrationCase.getMuSigArbitrationRequest();
            MuSigContract contract = muSigArbitrationRequest.getContract();
            MuSigOffer offer = contract.getOffer();
            String tradeId = muSigArbitrationRequest.getTradeId();

            Optional<CollateralOption> collateralOption = OfferOptionUtil.findCollateralOption(offer.getOfferOptions());
            if (collateralOption.isEmpty()) {
                log.warn("CollateralOption not found in offer options. tradeId={}", tradeId);
                model.setSecurityDepositInfo(Optional.empty());
            } else if (collateralOption.get().getBuyerSecurityDeposit() != collateralOption.get().getSellerSecurityDeposit()) {
                log.warn("Buyer and seller security deposits do not match. tradeId={}", tradeId);
                String mismatch = Res.get("authorizedRole.disputeActor.disputeCaseDetails.securityDepositMismatch");
                model.setSecurityDepositInfo(Optional.of(new Model.SecurityDepositInfo(
                        0,
                        mismatch,
                        mismatch,
                        false)));
            } else {
                double securityDeposit = collateralOption.get().getBuyerSecurityDeposit();
                model.setSecurityDepositInfo(Optional.of(new Model.SecurityDepositInfo(
                        securityDeposit,
                        calculateSecurityDeposit(contract, securityDeposit),
                        PercentageFormatter.formatToPercentWithSymbol(securityDeposit, 0),
                        true)));
            }

            MuSigArbitrationCaseListItem.Trader maker = muSigArbitrationCaseListItem.getMaker();
            MuSigArbitrationCaseListItem.Trader taker = muSigArbitrationCaseListItem.getTaker();
            Direction displayDirection = offer.getDisplayDirection();
            MuSigArbitrationCaseListItem.Trader buyer = displayDirection.isBuy() ? maker : taker;
            MuSigArbitrationCaseListItem.Trader seller = displayDirection.isSell() ? maker : taker;

            model.setTradeId(tradeId);
            model.setTradeDate(DateFormatter.formatDateTime(contract.getTakeOfferDate()));

            byte[] requesterContractHash = ContractService.getContractHash(contract);
            model.setContractHash(Hex.encode(requesterContractHash));

            model.setOfferType(displayDirection.isBuy()
                    ? Res.get("bisqEasy.openTrades.tradeDetails.offerTypeAndMarket.buyOffer")
                    : Res.get("bisqEasy.openTrades.tradeDetails.offerTypeAndMarket.sellOffer"));
            model.setMarket(Res.get("bisqEasy.openTrades.tradeDetails.offerTypeAndMarket.fiatMarket",
                    offer.getMarket().getRelevantCurrencyCode()));

            model.setBuyerNetworkAddress(buyer.getUserProfile().getAddressByTransportDisplayString(50));
            model.setSellerNetworkAddress(seller.getUserProfile().getAddressByTransportDisplayString(50));
        }

        @Override
        public void onDeactivate() {
            model.setSecurityDepositInfo(Optional.empty());
        }

        private static String calculateSecurityDeposit(MuSigContract contract,
                                                       double securityDepositAsPercent) {
            Monetary securityDeposit = OfferAmountUtil.calculateSecurityDepositAsBTC(
                    MuSigTradeUtils.getBtcSideMonetary(contract), securityDepositAsPercent);
            return OfferAmountFormatter.formatDepositAmountAsBTC(securityDeposit);
        }
    }

    @Slf4j
    @Getter
    @Setter
    private static class Model implements bisq.desktop.common.view.Model {
        private MuSigArbitrationCaseListItem muSigArbitrationCaseListItem;

        private boolean isCompactView;

        private String tradeId;
        private String tradeDate;

        private String offerType;
        private String market;
        private String contractHash;
        private Optional<SecurityDepositInfo> securityDepositInfo = Optional.empty();
        private String buyerNetworkAddress;
        private String sellerNetworkAddress;

        private record SecurityDepositInfo(double percentValue, String amountText, String percentText,
                                           boolean isMatching) {
        }
    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {

        private final Label tradeDateLabel, securityDepositLabel, securityDepositPercentLabel;
        private Label tradeIdLabel, offerTypeLabel, marketLabel, contractHashLabel, buyerNetworkAddressLabel, sellerNetworkAddressLabel;
        private BisqMenuItem tradeIdCopyButton, buyerNetworkAddressCopyButton, sellerNetworkAddressCopyButton;

        public View(VBox root, Model model, Controller controller) {
            super(root, model, controller);

            // Trade date
            tradeDateLabel = getValueLabel();
            HBox tradeDateBox = createAndGetDescriptionAndValueBox("bisqEasy.openTrades.tradeDetails.tradeDate", tradeDateLabel);

            // Security deposits
            securityDepositLabel = getValueLabel();
            securityDepositPercentLabel = new Label();
            securityDepositPercentLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
            Label openParenthesisLabel = new Label("(");
            openParenthesisLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
            Label closingParenthesisLabel = new Label(")");
            closingParenthesisLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
            HBox securityDepositPercentBox = new HBox(openParenthesisLabel,
                    securityDepositPercentLabel,
                    closingParenthesisLabel);
            securityDepositPercentBox.setAlignment(Pos.BASELINE_LEFT);
            HBox securityDepositValueBox = new HBox(5, securityDepositLabel, securityDepositPercentBox);
            securityDepositValueBox.setAlignment(Pos.BASELINE_LEFT);
            HBox securityDepositBox = createAndGetDescriptionAndValueBox(
                    getDescriptionLabel(Res.get("authorizedRole.disputeActor.disputeCaseDetails.securityDeposit")),
                    securityDepositValueBox,
                    Optional.empty()
            );

            VBox content;

            if (!model.isCompactView()) {
                // Trade ID
                tradeIdLabel = getValueLabel();
                tradeIdCopyButton = getCopyButton(Res.get("bisqEasy.openTrades.tradeDetails.tradeId.copy"));
                HBox tradeIdBox = createAndGetDescriptionAndValueBox("bisqEasy.openTrades.tradeDetails.tradeId",
                        tradeIdLabel, tradeIdCopyButton);

                // Offer type and market
                offerTypeLabel = getValueLabel();
                Label offerAndMarketslashLabel = new Label("/");
                offerAndMarketslashLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
                marketLabel = getValueLabel();
                HBox offerTypeAndMarketDetailsHBox = new HBox(5, offerTypeLabel, offerAndMarketslashLabel, marketLabel);
                offerTypeAndMarketDetailsHBox.setAlignment(Pos.BASELINE_LEFT);
                HBox offerTypeAndMarketBox = createAndGetDescriptionAndValueBox("bisqEasy.openTrades.tradeDetails.offerTypeAndMarket",
                        offerTypeAndMarketDetailsHBox);

                contractHashLabel = getValueLabel();
                HBox contractHashDetailsBox = new HBox(6, contractHashLabel);
                contractHashDetailsBox.setAlignment(Pos.BASELINE_LEFT);
                HBox contractHashBox = createAndGetDescriptionAndValueBox(
                        "authorizedRole.disputeActor.disputeCaseDetails.contractHash",
                        contractHashDetailsBox
                );

                // Network addresses
                buyerNetworkAddressLabel = getValueLabel();
                buyerNetworkAddressCopyButton = getCopyButton(Res.get("authorizedRole.disputeActor.disputeCaseDetails.buyerNetworkAddress.copy"));
                HBox buyerNetworkAddressBox = createAndGetDescriptionAndValueBox("authorizedRole.disputeActor.disputeCaseDetails.buyerNetworkAddress",
                        buyerNetworkAddressLabel, buyerNetworkAddressCopyButton);

                sellerNetworkAddressLabel = getValueLabel();
                sellerNetworkAddressCopyButton = getCopyButton(Res.get("authorizedRole.disputeActor.disputeCaseDetails.sellerNetworkAddress.copy"));
                HBox sellerNetworkAddressBox = createAndGetDescriptionAndValueBox("authorizedRole.disputeActor.disputeCaseDetails.sellerNetworkAddress",
                        sellerNetworkAddressLabel, sellerNetworkAddressCopyButton);
                content = new VBox(10,
                        tradeIdBox,
                        tradeDateBox,
                        offerTypeAndMarketBox,
                        securityDepositBox,
                        buyerNetworkAddressBox,
                        sellerNetworkAddressBox,
                        contractHashBox);
            } else {
                content = new VBox(10,
                        tradeDateBox,
                        securityDepositBox);
            }

            content.setAlignment(Pos.CENTER_LEFT);
            root.getChildren().add(content);
        }

        @Override
        protected void onViewAttached() {
            tradeDateLabel.setText(model.getTradeDate());

            Optional<Model.SecurityDepositInfo> info = model.getSecurityDepositInfo();
            securityDepositLabel.setText(info.map(Model.SecurityDepositInfo::amountText).orElse(Res.get("data.na")));
            securityDepositPercentLabel.setText(info.map(Model.SecurityDepositInfo::percentText).orElse(Res.get("data.na")));

            if (!model.isCompactView()) {
                tradeIdLabel.setText(model.getTradeId());
                tradeIdCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getTradeId()));
                offerTypeLabel.setText(model.getOfferType());
                marketLabel.setText(model.getMarket());
                contractHashLabel.setText(model.getContractHash());
                buyerNetworkAddressLabel.setText(model.getBuyerNetworkAddress());
                sellerNetworkAddressLabel.setText(model.getSellerNetworkAddress());
                buyerNetworkAddressCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getBuyerNetworkAddress()));
                sellerNetworkAddressCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getSellerNetworkAddress()));
            }
        }

        @Override
        protected void onViewDetached() {
            if (!model.isCompactView()) {
                tradeIdCopyButton.setOnAction(null);
                buyerNetworkAddressCopyButton.setOnAction(null);
                sellerNetworkAddressCopyButton.setOnAction(null);
            }
        }
    }
}
