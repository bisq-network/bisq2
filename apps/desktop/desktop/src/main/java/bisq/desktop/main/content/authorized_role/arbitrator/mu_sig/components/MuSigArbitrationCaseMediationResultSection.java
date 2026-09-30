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

import bisq.common.monetary.Coin;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.support.mediation.MediationPayoutDistributionType;
import bisq.support.mediation.MediationResultReason;
import bisq.support.mediation.mu_sig.MuSigMediationResult;
import bisq.user.profile.UserProfile;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static bisq.desktop.components.helpers.LabeledValueRowFactory.createAndGetDescriptionAndValueBox;
import static bisq.desktop.components.helpers.LabeledValueRowFactory.getDescriptionLabel;
import static bisq.desktop.components.helpers.LabeledValueRowFactory.getValueLabel;

public class MuSigArbitrationCaseMediationResultSection {

    private final Controller controller;

    public MuSigArbitrationCaseMediationResultSection() {
        this.controller = new Controller();
    }

    public VBox getRoot() {
        return controller.view.getRoot();
    }

    public void setMediationResult(MuSigMediationResult result) {
        controller.setMediationResult(result);
    }

    public void setMediator(UserProfile mediator) {
        controller.setMediator(mediator);
    }

    @Slf4j
    private static class Controller implements bisq.desktop.common.view.Controller {

        @Getter
        private final View view;
        private final Model model;

        public Controller() {
            model = new Model();
            view = new View(new VBox(), model, this);
        }

        private void setMediationResult(MuSigMediationResult result) {
            model.setMuSigMediationResult(result);
        }

        private void setMediator(UserProfile mediator) {
            model.setMediator(mediator);
        }

        @Override
        public void onActivate() {
            MuSigMediationResult result = model.getMuSigMediationResult();
            model.setMediatorUserName(model.getMediator().getUserName());
            model.setPayoutDistributionType(result.getMediationPayoutDistributionType());
            model.setReason(result.getMediationResultReason());
            model.setSummaryNotes(result.getSummaryNotes());
            model.setBuyerPayoutAmount(result.getProposedBuyerPayoutAmount().map(Controller::formatSatsAsBtc));
            model.setSellerPayoutAmount(result.getProposedSellerPayoutAmount().map(Controller::formatSatsAsBtc));
            model.setPayoutAdjustmentPercentage(result.getPayoutAdjustmentPercentage()
                    .map(value -> PercentageFormatter.formatToPercentWithSymbol(value, 0)));
        }

        @Override
        public void onDeactivate() {
        }

        private static String formatSatsAsBtc(long sats) {
            return AmountFormatter.formatBaseAmountWithCode(Coin.asBtcFromValue(sats));
        }
    }

    @Slf4j
    @Getter
    @Setter
    private static class Model implements bisq.desktop.common.view.Model {
        private MuSigMediationResult muSigMediationResult;
        private UserProfile mediator;
        private String mediatorUserName;
        private MediationPayoutDistributionType payoutDistributionType;
        private MediationResultReason reason;
        private Optional<String> summaryNotes;
        private Optional<String> buyerPayoutAmount;
        private Optional<String> sellerPayoutAmount;
        private Optional<String> payoutAdjustmentPercentage;

        public Model() {
        }
    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final Label mediatorUserNameLabel;
        private final Label payoutDistributionTypeLabel;
        private final Label reasonLabel;
        private final Label buyerPayoutAmountLabel;
        private final Label sellerPayoutAmountLabel;
        private final Label payoutAdjustmentPercentageLabel;
        private final HBox payoutAdjustmentPercentageBox;
        private final Label payoutAdjustmentPercentageDescriptionLabel;
        private final HBox buyerPayoutAmountBox;
        private final HBox sellerPayoutAmountBox;
        private final Label summaryNotesLabel;
        private final HBox summaryNotesBox;

        public View(VBox root, Model model, Controller controller) {
            super(root, model, controller);

            mediatorUserNameLabel = getValueLabel();
            HBox mediatorUserNameBox = createAndGetDescriptionAndValueBox("muSig.trade.details.assignedMediator", mediatorUserNameLabel);

            payoutDistributionTypeLabel = getValueLabel();
            HBox payoutDistributionTypeBox = createAndGetDescriptionAndValueBox(
                    getDescriptionLabel(Res.get("authorizedRole.disputeActor.disputeResult.selectPayoutDistributionType")),
                    payoutDistributionTypeLabel,
                    Optional.empty());

            reasonLabel = getValueLabel();
            HBox reasonBox = createAndGetDescriptionAndValueBox(
                    getDescriptionLabel(Res.get("authorizedRole.disputeActor.disputeResult.selectReason")),
                    reasonLabel,
                    Optional.empty());

            buyerPayoutAmountLabel = getValueLabel();
            buyerPayoutAmountBox = createAndGetDescriptionAndValueBox(
                    getDescriptionLabel(Res.get("authorizedRole.disputeActor.disputeResult.buyerPayoutAmount")),
                    buyerPayoutAmountLabel,
                    Optional.empty());

            sellerPayoutAmountLabel = getValueLabel();
            sellerPayoutAmountBox = createAndGetDescriptionAndValueBox(
                    getDescriptionLabel(Res.get("authorizedRole.disputeActor.disputeResult.sellerPayoutAmount")),
                    sellerPayoutAmountLabel,
                    Optional.empty());

            payoutAdjustmentPercentageLabel = getValueLabel();
            payoutAdjustmentPercentageDescriptionLabel = getDescriptionLabel(Res.get("authorizedRole.mediator.mediationResult.compensationPercentage"));
            payoutAdjustmentPercentageBox = createAndGetDescriptionAndValueBox(
                    payoutAdjustmentPercentageDescriptionLabel,
                    payoutAdjustmentPercentageLabel,
                    Optional.empty());

            summaryNotesLabel = getValueLabel();
            summaryNotesLabel.setWrapText(true);
            summaryNotesLabel.setMaxWidth(Double.MAX_VALUE);
            summaryNotesBox = createAndGetDescriptionAndValueBox(
                    getDescriptionLabel(Res.get("authorizedRole.disputeActor.disputeResult.summaryNotes")),
                    summaryNotesLabel,
                    Optional.empty());

            root.setAlignment(Pos.CENTER_LEFT);
            root.setSpacing(10);
            root.getChildren().addAll(
                    mediatorUserNameBox,
                    payoutDistributionTypeBox,
                    reasonBox,
                    buyerPayoutAmountBox,
                    sellerPayoutAmountBox,
                    payoutAdjustmentPercentageBox,
                    summaryNotesBox
            );
        }

        @Override
        protected void onViewAttached() {
            mediatorUserNameLabel.setText(model.getMediatorUserName());
            payoutDistributionTypeLabel.setText(Res.get("authorizedRole.mediator.mediationResult.payoutDistributionType." +
                    model.getPayoutDistributionType().name()));
            reasonLabel.setText(Res.get("authorizedRole.disputeActor.disputeResult.reason." + model.getReason().name()));
            updatePayoutAdjustmentPercentageDescription(model.getPayoutDistributionType());

            applyOptionalRow(buyerPayoutAmountBox, buyerPayoutAmountLabel, model.getBuyerPayoutAmount());
            applyOptionalRow(sellerPayoutAmountBox, sellerPayoutAmountLabel, model.getSellerPayoutAmount());
            applyOptionalRow(payoutAdjustmentPercentageBox, payoutAdjustmentPercentageLabel, model.getPayoutAdjustmentPercentage());
            applyOptionalRow(summaryNotesBox, summaryNotesLabel, model.getSummaryNotes());
        }

        @Override
        protected void onViewDetached() {
        }

        private static void applyOptionalRow(HBox row, Label valueLabel, Optional<String> optionalValue) {
            optionalValue.ifPresent(valueLabel::setText);
            boolean visible = optionalValue.isPresent();
            row.setVisible(visible);
            row.setManaged(visible);
        }
        private void updatePayoutAdjustmentPercentageDescription(MediationPayoutDistributionType payoutDistributionType) {
            payoutAdjustmentPercentageDescriptionLabel.setText(Res.get(shouldUsePenaltyDescription(payoutDistributionType)
                    ? "authorizedRole.mediator.mediationResult.penaltyPercentage"
                    : "authorizedRole.mediator.mediationResult.compensationPercentage"));
        }

        private static boolean shouldUsePenaltyDescription(MediationPayoutDistributionType payoutDistributionType) {
            return payoutDistributionType == MediationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT_MINUS_PENALTY ||
                    payoutDistributionType == MediationPayoutDistributionType.SELLER_GETS_TRADE_AMOUNT_MINUS_PENALTY;
        }
    }
}
