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
import bisq.contract.mu_sig.MuSigContract;
import bisq.desktop.ServiceProvider;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.validator.NumberValidator;
import bisq.desktop.components.controls.validator.TextMaxLengthValidator;
import bisq.desktop.main.content.authorized_role.arbitrator.mu_sig.MuSigArbitrationCaseListItem;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.parser.AmountParser;
import bisq.support.arbitration.ArbitrationCaseState;
import bisq.support.arbitration.ArbitrationPayoutDistributionType;
import bisq.support.arbitration.ArbitrationResultReason;
import bisq.support.arbitration.mu_sig.MuSigArbitrationCase;
import bisq.support.arbitration.mu_sig.MuSigArbitrationPayoutResolver;
import bisq.support.arbitration.mu_sig.MuSigArbitrationResult;
import bisq.support.arbitration.mu_sig.MuSigArbitratorService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static bisq.support.arbitration.mu_sig.MuSigArbitratorService.createMuSigArbitrationResult;

public class MuSigArbitrationResultSection {
    private final Controller controller;

    public MuSigArbitrationResultSection(ServiceProvider serviceProvider) {
        this.controller = new Controller(serviceProvider);
    }

    public VBox getRoot() {
        return controller.view.getRoot();
    }

    public void setArbitrationCaseListItem(MuSigArbitrationCaseListItem item) {
        controller.setArbitrationCaseListItem(item);
    }

    public void closeCase() {
        controller.closeCase();
    }

    public ReadOnlyBooleanProperty hasRequiredSelectionsProperty() {
        return controller.getHasRequiredSelections();
    }

    @Slf4j
    private static class Controller implements bisq.desktop.common.view.Controller {

        @Getter
        private final View view;
        private final Model model;

        private final MuSigArbitratorService muSigArbitratorService;
        private Optional<MuSigArbitrationPayoutResolver.PayoutContext> payoutContext = Optional.empty();
        private final Set<Subscription> subscriptions = new HashSet<>();

        private Controller(ServiceProvider serviceProvider) {
            model = new Model();
            view = new View(new VBox(), model, this);
            muSigArbitratorService = serviceProvider.getSupportService().getMuSigArbitratorService();
            model.getHasRequiredSelections().set(false);
        }

        private void setArbitrationCaseListItem(MuSigArbitrationCaseListItem item) {
            model.setMuSigArbitrationCaseListItem(item);
        }

        private ReadOnlyBooleanProperty getHasRequiredSelections() {
            return model.getHasRequiredSelections();
        }

        @Override
        public void onActivate() {
            MuSigArbitrationCase muSigArbitrationCase = model.getMuSigArbitrationCaseListItem().getMuSigArbitrationCase();
            Optional<MuSigArbitrationResult> muSigArbitrationResult = muSigArbitrationCase.getMuSigArbitrationResult();
            boolean caseOpen = muSigArbitrationCase.getArbitrationCaseState() == ArbitrationCaseState.OPEN;

            model.getPayoutDistributionTypes().setAll(ArbitrationPayoutDistributionType.values());
            model.getReasons().setAll(ArbitrationResultReason.values());

            model.getSelectedPayoutDistributionType().set(
                    muSigArbitrationResult.map(MuSigArbitrationResult::getArbitrationPayoutDistributionType).orElse(null));

            model.getSelectedReason().set(
                    muSigArbitrationResult.map(MuSigArbitrationResult::getArbitrationResultReason).orElse(null));

            model.getSummaryNotes().set(
                    muSigArbitrationResult
                            .map(i -> i.getSummaryNotes().orElse("")).orElse(""));
            model.getBuyerPayoutAmountAsCoin().set(
                    muSigArbitrationResult
                            .map(MuSigArbitrationResult::getBuyerPayoutAmount)
                            .map(Coin::asBtcFromValue)
                            .orElse(null));
            model.getSellerPayoutAmountAsCoin().set(
                    muSigArbitrationResult
                            .map(MuSigArbitrationResult::getSellerPayoutAmount)
                            .map(Coin::asBtcFromValue)
                            .orElse(null));
            model.getBuyerPayoutAmount().set(
                    muSigArbitrationResult
                            .map(MuSigArbitrationResult::getBuyerPayoutAmount)
                            .map(Controller::formatSatsAsBtc)
                            .orElse(""));
            model.getSellerPayoutAmount().set(
                    muSigArbitrationResult
                            .map(MuSigArbitrationResult::getSellerPayoutAmount)
                            .map(Controller::formatSatsAsBtc)
                            .orElse(""));
            model.getPayoutAmountsEditable().set(false);
            payoutContext = resolvePayoutContext();
            subscriptions.add(EasyBind.subscribe(
                    EasyBind.combine(
                            model.getSelectedReason(),
                            model.getSelectedPayoutDistributionType(),
                            model.getBuyerPayoutAmountAsCoin(),
                            model.getSellerPayoutAmountAsCoin(),
                            model.getSummaryNotes(),
                            (selectedReason, selectedPayoutDistributionType, buyerPayoutAmountAsCoin, sellerPayoutAmountAsCoin, summaryNotes) ->
                                    hasRequiredSelections()),
                    model.getHasRequiredSelections()::set));

            if (caseOpen) {
                subscriptions.add(EasyBind.subscribe(model.getSelectedPayoutDistributionType(), this::onPayoutDistributionTypeChanged));
                subscriptions.add(EasyBind.subscribe(model.getBuyerPayoutAmount(), this::onBuyerPayoutAmountChanged));
                subscriptions.add(EasyBind.subscribe(model.getSellerPayoutAmount(), this::onSellerPayoutAmountChanged));
            }
        }

        @Override
        public void onDeactivate() {
            subscriptions.forEach(Subscription::unsubscribe);
            subscriptions.clear();
            payoutContext = Optional.empty();
        }

        void onSelectReason(ArbitrationResultReason reason) {
            model.getSelectedReason().set(reason);
        }

        void onSelectPayoutDistributionType(ArbitrationPayoutDistributionType type) {
            model.getSelectedPayoutDistributionType().set(type);
        }

        void onBuyerPayoutAmountFocusChanged(boolean focused) {
            if (!focused) {
                resolveCustomPayoutAmounts(true);
            }
        }

        void onSellerPayoutAmountFocusChanged(boolean focused) {
            if (!focused) {
                resolveCustomPayoutAmounts(false);
            }
        }

        private void onPayoutDistributionTypeChanged(ArbitrationPayoutDistributionType payoutDistributionType) {
            if (payoutDistributionType == null) {
                return;
            }

            boolean caseOpen = model.getMuSigArbitrationCaseListItem().getMuSigArbitrationCase().getArbitrationCaseState() == ArbitrationCaseState.OPEN;
            model.getPayoutAmountsEditable().set(caseOpen && shouldAllowManualPayoutAmounts(payoutDistributionType));
            applyPayoutAmountsForType(payoutDistributionType);
        }

        private void resolveCustomPayoutAmounts(boolean buyerFieldEdited) {
            if (model.getSelectedPayoutDistributionType().get() != ArbitrationPayoutDistributionType.CUSTOM_PAYOUT) {
                return;
            }

            payoutContext
                    .flatMap(context -> MuSigArbitrationPayoutResolver.resolveCustomPayout(
                            context,
                            Optional.ofNullable(model.getBuyerPayoutAmountAsCoin().get()).map(Coin::getValue),
                            Optional.ofNullable(model.getSellerPayoutAmountAsCoin().get()).map(Coin::getValue),
                            buyerFieldEdited))
                    .ifPresent(this::setPayoutAmounts);
        }

        private void onBuyerPayoutAmountChanged(String value) {
            model.getBuyerPayoutAmountAsCoin().set(parseBtcAmount(value).orElse(null));
        }

        private void onSellerPayoutAmountChanged(String value) {
            model.getSellerPayoutAmountAsCoin().set(parseBtcAmount(value).orElse(null));
        }

        private void applyPayoutAmountsForType(ArbitrationPayoutDistributionType payoutDistributionType) {
            if (payoutDistributionType == ArbitrationPayoutDistributionType.CUSTOM_PAYOUT) {
                return;
            }

            payoutContext
                    .flatMap(context -> MuSigArbitrationPayoutResolver.calculateForType(
                            payoutDistributionType,
                            context))
                    .ifPresentOrElse(this::setPayoutAmounts, this::clearPayoutAmounts);
        }

        private static Optional<Coin> parseBtcAmount(String value) {
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            try {
                return Optional.of(Coin.asBtcFromValue(AmountParser.parse(value, "BTC").getValue()));
            } catch (Exception ignore) {
                return Optional.empty();
            }
        }

        private Optional<MuSigArbitrationPayoutResolver.PayoutContext> resolvePayoutContext() {
            MuSigContract contract = model.getMuSigArbitrationCaseListItem()
                    .getMuSigArbitrationCase()
                    .getMuSigArbitrationRequest()
                    .getContract();
            Optional<MuSigArbitrationPayoutResolver.PayoutContext> optionalPayoutContext =
                    MuSigArbitrationPayoutResolver.createPayoutContext(contract);
            if (optionalPayoutContext.isEmpty()) {
                log.warn("CollateralOption not found for tradeId={}",
                        model.getMuSigArbitrationCaseListItem().getMuSigArbitrationCase().getMuSigArbitrationRequest().getTradeId());
            }
            return optionalPayoutContext;
        }

        private void setPayoutAmounts(MuSigArbitrationPayoutResolver.PayoutAmounts payoutAmounts) {
            model.getBuyerPayoutAmountAsCoin().set(Coin.asBtcFromValue(payoutAmounts.buyerAmountAsSats()));
            model.getSellerPayoutAmountAsCoin().set(Coin.asBtcFromValue(payoutAmounts.sellerAmountAsSats()));
            model.getBuyerPayoutAmount().set(formatSatsAsBtc(payoutAmounts.buyerAmountAsSats()));
            model.getSellerPayoutAmount().set(formatSatsAsBtc(payoutAmounts.sellerAmountAsSats()));
        }

        private void clearPayoutAmounts() {
            model.getBuyerPayoutAmountAsCoin().set(null);
            model.getSellerPayoutAmountAsCoin().set(null);
            model.getBuyerPayoutAmount().set("");
            model.getSellerPayoutAmount().set("");
        }

        private boolean hasRequiredSelections() {
            ArbitrationPayoutDistributionType payoutDistributionType = model.getSelectedPayoutDistributionType().get();
            return model.getSelectedReason().get() != null &&
                    payoutDistributionType != null &&
                    hasValidSummaryNotesLength(model.getSummaryNotes().get()) &&
                    hasValidPayoutAmounts(
                            payoutDistributionType,
                            model.getBuyerPayoutAmountAsCoin().get(),
                            model.getSellerPayoutAmountAsCoin().get());
        }

        private static boolean shouldAllowManualPayoutAmounts(ArbitrationPayoutDistributionType payoutDistributionType) {
            return payoutDistributionType == ArbitrationPayoutDistributionType.CUSTOM_PAYOUT;
        }

        private boolean hasValidPayoutAmounts(ArbitrationPayoutDistributionType payoutDistributionType,
                                              Coin buyerPayoutAmountAsCoin,
                                              Coin sellerPayoutAmountAsCoin) {
            if (buyerPayoutAmountAsCoin == null || sellerPayoutAmountAsCoin == null) {
                return false;
            }

            return payoutContext
                    .map(context -> {
                        try {
                            MuSigArbitrationPayoutResolver.checkPayoutAmounts(
                                    payoutDistributionType,
                                    context,
                                    buyerPayoutAmountAsCoin.getValue(),
                                    sellerPayoutAmountAsCoin.getValue());
                            return true;
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    })
                    .orElse(false);
        }

        private static boolean hasValidSummaryNotesLength(String summaryNotes) {
            return summaryNotes != null && summaryNotes.length() <= MuSigArbitrationResult.MAX_SUMMARY_NOTES_LENGTH;
        }

        private static String formatSatsAsBtc(long sats) {
            return AmountFormatter.formatBaseAmount(Coin.asBtcFromValue(sats));
        }

        void closeCase() {
            MuSigArbitrationCase muSigArbitrationCase = model.getMuSigArbitrationCaseListItem().getMuSigArbitrationCase();

            if (muSigArbitrationCase.getArbitrationCaseState() == ArbitrationCaseState.OPEN) {
                if (!hasRequiredSelections()) {
                    log.warn("Cannot close MuSig arbitration case due to incomplete or invalid arbitration result data");
                    return;
                }

                ArbitrationResultReason selectedReason = model.getSelectedReason().get();
                ArbitrationPayoutDistributionType selectedPayoutDistributionType = model.getSelectedPayoutDistributionType().get();
                long buyerPayoutAmount = model.getBuyerPayoutAmountAsCoin().get().getValue();
                long sellerPayoutAmount = model.getSellerPayoutAmountAsCoin().get().getValue();

                String summaryNotes = model.getSummaryNotes().get();
                MuSigArbitrationResult muSigArbitrationResult = createMuSigArbitrationResult(
                        muSigArbitrationCase.getMuSigArbitrationRequest().getContract(),
                        selectedReason,
                        selectedPayoutDistributionType,
                        buyerPayoutAmount,
                        sellerPayoutAmount,
                        summaryNotes.isEmpty() ? Optional.empty() : Optional.of(summaryNotes));
                muSigArbitratorService.closeArbitrationCase(muSigArbitrationCase, muSigArbitrationResult);
            }
        }
    }

    @Slf4j
    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        @Setter
        private MuSigArbitrationCaseListItem muSigArbitrationCaseListItem;
        private final ObjectProperty<ArbitrationPayoutDistributionType> selectedPayoutDistributionType = new SimpleObjectProperty<>();
        private final ObservableList<ArbitrationPayoutDistributionType> payoutDistributionTypes = FXCollections.observableArrayList();
        private final ObjectProperty<ArbitrationResultReason> selectedReason = new SimpleObjectProperty<>();
        private final ObservableList<ArbitrationResultReason> reasons = FXCollections.observableArrayList();

        private final StringProperty summaryNotes = new SimpleStringProperty("");
        private final StringProperty buyerPayoutAmount = new SimpleStringProperty("");
        private final StringProperty sellerPayoutAmount = new SimpleStringProperty("");
        private final ObjectProperty<Coin> buyerPayoutAmountAsCoin = new SimpleObjectProperty<>();
        private final ObjectProperty<Coin> sellerPayoutAmountAsCoin = new SimpleObjectProperty<>();
        private final BooleanProperty payoutAmountsEditable = new SimpleBooleanProperty(false);
        private final BooleanProperty hasRequiredSelections = new SimpleBooleanProperty(false);
        private final TextMaxLengthValidator summaryNotesMaxLengthValidator = new TextMaxLengthValidator(
                Res.get("validation.tooLong", MuSigArbitrationResult.MAX_SUMMARY_NOTES_LENGTH),
                MuSigArbitrationResult.MAX_SUMMARY_NOTES_LENGTH);
    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private static final double PAYOUT_AMOUNT_PREF_WIDTH = 240;

        private final AutoCompleteComboBox<ArbitrationPayoutDistributionType> payoutDistributionTypeSelection;
        private final MaterialTextField payoutDistributionTypeDisplay;

        private final AutoCompleteComboBox<ArbitrationResultReason> reasonSelection;
        private final MaterialTextField reasonDisplay;
        private final Set<Subscription> subscriptions = new HashSet<>();

        private final MaterialTextField buyerPayoutAmount;
        private final MaterialTextField sellerPayoutAmount;

        private final MaterialTextArea summaryNotes;

        public View(VBox root, Model model, Controller controller) {
            super(root, model, controller);

            // payout types

            payoutDistributionTypeSelection = new AutoCompleteComboBox<>(model.getPayoutDistributionTypes(), Res.get("authorizedRole.disputeActor.disputeResult.selectPayoutDistributionType"));
            payoutDistributionTypeSelection.setPrefWidth(364);
            payoutDistributionTypeSelection.setConverter(new StringConverter<>() {
                @Override
                public String toString(ArbitrationPayoutDistributionType payoutDistributionType) {
                    return payoutDistributionType != null ? Res.get("authorizedRole.arbitrator.arbitrationResult.payoutDistributionType." + payoutDistributionType.name()) : "";
                }

                @Override
                public ArbitrationPayoutDistributionType fromString(String string) {
                    return null;
                }
            });
            payoutDistributionTypeDisplay = new MaterialTextField(Res.get("authorizedRole.disputeActor.disputeResult.selectPayoutDistributionType"));
            payoutDistributionTypeDisplay.setEditable(false);
            payoutDistributionTypeDisplay.setPrefWidth(364);
            payoutDistributionTypeDisplay.setMaxWidth(Double.MAX_VALUE);

            // reason

            reasonSelection = new AutoCompleteComboBox<>(model.getReasons(), Res.get("authorizedRole.disputeActor.disputeResult.selectReason"));
            reasonSelection.setPrefWidth(364);
            reasonSelection.setConverter(new StringConverter<>() {
                @Override
                public String toString(ArbitrationResultReason reason) {
                    return reason != null ? Res.get("authorizedRole.disputeActor.disputeResult.reason." + reason.name()) : "";
                }

                @Override
                public ArbitrationResultReason fromString(String string) {
                    return null;
                }
            });
            reasonDisplay = new MaterialTextField(Res.get("authorizedRole.disputeActor.disputeResult.selectReason"));
            reasonDisplay.setEditable(false);
            reasonDisplay.setPrefWidth(364);
            reasonDisplay.setMaxWidth(Double.MAX_VALUE);

            // payout details

            buyerPayoutAmount = new MaterialTextField(Res.get("authorizedRole.disputeActor.disputeResult.buyerPayoutAmount"));
            sellerPayoutAmount = new MaterialTextField(Res.get("authorizedRole.disputeActor.disputeResult.sellerPayoutAmount"));
            buyerPayoutAmount.setValidators(new NumberValidator(Res.get("validation.invalidNumber"), true));
            sellerPayoutAmount.setValidators(new NumberValidator(Res.get("validation.invalidNumber"), true));

            buyerPayoutAmount.setPrefWidth(PAYOUT_AMOUNT_PREF_WIDTH);
            sellerPayoutAmount.setPrefWidth(PAYOUT_AMOUNT_PREF_WIDTH);
            buyerPayoutAmount.setMaxWidth(Double.MAX_VALUE);
            sellerPayoutAmount.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(buyerPayoutAmount, Priority.ALWAYS);
            HBox.setHgrow(sellerPayoutAmount, Priority.ALWAYS);

            HBox payoutAmounts = new HBox(10, buyerPayoutAmount, sellerPayoutAmount);

            // summary notes

            summaryNotes = new MaterialTextArea(Res.get("authorizedRole.disputeActor.disputeResult.summaryNotes"));
            summaryNotes.setValidator(model.getSummaryNotesMaxLengthValidator());
            summaryNotes.setFixedHeight(70);

            VBox content = new VBox(10,
                    payoutDistributionTypeSelection,
                    payoutDistributionTypeDisplay,
                    payoutAmounts,
                    reasonSelection,
                    reasonDisplay,
                    summaryNotes);

            content.setAlignment(Pos.CENTER_LEFT);
            root.getChildren().add(content);
        }

        @Override
        protected void onViewAttached() {
            ArbitrationCaseState arbitrationCaseState = model.muSigArbitrationCaseListItem.getMuSigArbitrationCase().getArbitrationCaseState();
            boolean caseOpen = arbitrationCaseState == ArbitrationCaseState.OPEN;

            if (caseOpen) {
                payoutDistributionTypeSelection.setOnChangeConfirmed(e -> {
                    if (payoutDistributionTypeSelection.getSelectionModel().getSelectedItem() == null) {
                        payoutDistributionTypeSelection.getSelectionModel().select(model.getSelectedPayoutDistributionType().get());
                        return;
                    }
                    controller.onSelectPayoutDistributionType(payoutDistributionTypeSelection.getSelectionModel().getSelectedItem());
                });

                subscriptions.add(EasyBind.subscribe(model.getSelectedPayoutDistributionType(),
                        payoutDistributionType -> {
                            if (payoutDistributionType != null) {
                                payoutDistributionTypeSelection.getSelectionModel().select(payoutDistributionType);
                            } else {
                                payoutDistributionTypeSelection.getSelectionModel().clearSelection();
                            }
                            updatePayoutDistributionTypeDisplay(payoutDistributionType);
                        }));

                reasonSelection.setOnChangeConfirmed(e -> {
                    if (reasonSelection.getSelectionModel().getSelectedItem() == null) {
                        reasonSelection.getSelectionModel().select(model.getSelectedReason().get());
                        return;
                    }
                    controller.onSelectReason(reasonSelection.getSelectionModel().getSelectedItem());
                });

                subscriptions.add(EasyBind.subscribe(model.getSelectedReason(),
                        reason -> {
                            if (reason != null) {
                                reasonSelection.getSelectionModel().select(reason);
                            } else {
                                reasonSelection.getSelectionModel().clearSelection();
                            }
                            updateReasonDisplay(reason);
                        }));
                subscriptions.add(EasyBind.subscribe(buyerPayoutAmount.textInputFocusedProperty(),
                        controller::onBuyerPayoutAmountFocusChanged));
                subscriptions.add(EasyBind.subscribe(sellerPayoutAmount.textInputFocusedProperty(),
                        controller::onSellerPayoutAmountFocusChanged));
                subscriptions.add(EasyBind.subscribe(model.getPayoutAmountsEditable(),
                        editable -> {
                            buyerPayoutAmount.setEditable(editable);
                            sellerPayoutAmount.setEditable(editable);
                        }));
            }

            updatePayoutDistributionTypeDisplay(model.getSelectedPayoutDistributionType().get());
            updateReasonDisplay(model.getSelectedReason().get());

            summaryNotes.textProperty().bindBidirectional(model.getSummaryNotes());
            buyerPayoutAmount.textProperty().bindBidirectional(model.getBuyerPayoutAmount());
            sellerPayoutAmount.textProperty().bindBidirectional(model.getSellerPayoutAmount());

            payoutDistributionTypeSelection.setVisible(caseOpen);
            payoutDistributionTypeSelection.setManaged(caseOpen);
            reasonSelection.setVisible(caseOpen);
            reasonSelection.setManaged(caseOpen);
            payoutDistributionTypeDisplay.setVisible(!caseOpen);
            payoutDistributionTypeDisplay.setManaged(!caseOpen);
            reasonDisplay.setVisible(!caseOpen);
            reasonDisplay.setManaged(!caseOpen);

            payoutDistributionTypeSelection.setDisable(!caseOpen);
            reasonSelection.setDisable(!caseOpen);
            summaryNotes.setEditable(caseOpen);
            buyerPayoutAmount.setEditable(model.getPayoutAmountsEditable().get());
            sellerPayoutAmount.setEditable(model.getPayoutAmountsEditable().get());
        }

        @Override
        protected void onViewDetached() {
            payoutDistributionTypeSelection.setOnChangeConfirmed(null);
            reasonSelection.setOnChangeConfirmed(null);
            subscriptions.forEach(Subscription::unsubscribe);
            subscriptions.clear();

            summaryNotes.textProperty().unbindBidirectional(model.getSummaryNotes());
            buyerPayoutAmount.textProperty().unbindBidirectional(model.getBuyerPayoutAmount());
            sellerPayoutAmount.textProperty().unbindBidirectional(model.getSellerPayoutAmount());
            payoutDistributionTypeSelection.setVisible(true);
            payoutDistributionTypeSelection.setManaged(true);
            reasonSelection.setVisible(true);
            reasonSelection.setManaged(true);
            payoutDistributionTypeDisplay.setVisible(false);
            payoutDistributionTypeDisplay.setManaged(false);
            reasonDisplay.setVisible(false);
            reasonDisplay.setManaged(false);
        }

        private void updatePayoutDistributionTypeDisplay(ArbitrationPayoutDistributionType payoutDistributionType) {
            payoutDistributionTypeDisplay.setText(payoutDistributionType == null
                    ? ""
                    : Res.get("authorizedRole.arbitrator.arbitrationResult.payoutDistributionType." + payoutDistributionType.name()));
        }

        private void updateReasonDisplay(ArbitrationResultReason reason) {
            reasonDisplay.setText(reason == null
                    ? ""
                    : Res.get("authorizedRole.disputeActor.disputeResult.reason." + reason.name()));
        }
    }
}
