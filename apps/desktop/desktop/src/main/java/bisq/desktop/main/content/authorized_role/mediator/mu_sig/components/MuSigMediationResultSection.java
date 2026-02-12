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

import bisq.desktop.ServiceProvider;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.main.content.authorized_role.mediator.mu_sig.MuSigMediationCaseListItem;
import bisq.i18n.Res;
import bisq.support.mediation.MediationCaseState;
import bisq.support.mediation.MediationResultReason;
import bisq.support.mediation.mu_sig.MuSigMediationCase;
import bisq.support.mediation.mu_sig.MuSigMediationResult;
import bisq.support.mediation.mu_sig.MuSigMediatorService;
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
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

public class MuSigMediationResultSection {

    private final Controller controller;

    public MuSigMediationResultSection(ServiceProvider serviceProvider) {
        this.controller = new Controller(serviceProvider);
    }

    public VBox getRoot() {
        return controller.view.getRoot();
    }

    public void setMediationCaseListItem(MuSigMediationCaseListItem item) {
        controller.setMediationCaseListItem(item);
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

        private final MuSigMediatorService muSigMediatorService;

        private Controller(ServiceProvider serviceProvider) {
            model = new Model();
            view = new View(new VBox(), model, this);
            muSigMediatorService = serviceProvider.getSupportService().getMuSigMediatorService();
            model.getHasRequiredSelections().bind(model.getSelectedReason().isNotNull());
        }

        private void setMediationCaseListItem(MuSigMediationCaseListItem item) {
            model.setMuSigMediationCaseListItem(item);
        }

        private ReadOnlyBooleanProperty getHasRequiredSelections() {
            return model.getHasRequiredSelections();
        }

        @Override
        public void onActivate() {
            MuSigMediationCase muSigMediationCase = model.getMuSigMediationCaseListItem().getMuSigMediationCase();
            Optional<MuSigMediationResult> muSigMediationResult = muSigMediationCase.getMuSigMediationResult().get();

            model.getPayoutTypes().setAll(Model.PayoutType.values());
            model.getReasons().setAll(MediationResultReason.values());

            model.getSelectedPayoutType().set(null);
            model.getSelectedReason().set(null);

            model.getSelectedReason().set(
                    muSigMediationResult.map(MuSigMediationResult::getMediationResultReason).orElse(null));

            model.getSummaryNotes().set(
                    muSigMediationResult
                            .map(i -> i.getSummaryNotes().orElse("")).orElse(""));
        }

        @Override
        public void onDeactivate() {
        }

        void onSelectReason(MediationResultReason reason) {
            model.getSelectedReason().set(reason);
        }

        void onSelectPayoutType(Model.PayoutType type) {
            model.getSelectedPayoutType().set(type);
        }

        void closeCase() {
            MuSigMediationCase muSigMediationCase = model.getMuSigMediationCaseListItem().getMuSigMediationCase();

            if (muSigMediationCase.getMediationCaseState().get() == MediationCaseState.OPEN) {
                String summaryNotes = model.getSummaryNotes().get();
                MuSigMediationResult muSigMediationResult = muSigMediatorService.createMuSigMediationResult(
                        model.getSelectedReason().get(), summaryNotes.isEmpty() ? Optional.empty() : Optional.of(summaryNotes));
                muSigMediatorService.closeMediationCase(muSigMediationCase, muSigMediationResult);
            } else {
                muSigMediatorService.closeReOpenedMediationCase(muSigMediationCase);
            }
        }
    }

    @Slf4j
    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        @Setter
        private MuSigMediationCaseListItem muSigMediationCaseListItem;
        private final ObjectProperty<PayoutType> selectedPayoutType = new SimpleObjectProperty<>();
        private final ObservableList<PayoutType> payoutTypes = FXCollections.observableArrayList();
        private final ObjectProperty<MediationResultReason> selectedReason = new SimpleObjectProperty<>();
        private final ObservableList<MediationResultReason> reasons = FXCollections.observableArrayList();

        private final StringProperty summaryNotes = new SimpleStringProperty("");
        private final BooleanProperty hasRequiredSelections = new SimpleBooleanProperty(false);

        enum PayoutType {
            CUSTOM_PAYOUT,
            BUYER_GETS_TRADE_AMOUNT,
            BUYER_GETS_TRADE_AMOUNT_PLUS_COMPENSATION,
            BUYER_GETS_TRADE_AMOUNT_MINUS_PENALTY,
            SELLER_GETS_TRADE_AMOUNT,
            SELLER_GETS_TRADE_AMOUNT_PLUS_COMPENSATION,
            SELLER_GETS_TRADE_AMOUNT_MINUS_PENALTY;
        }
    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {

        private final AutoCompleteComboBox<Model.PayoutType> payoutTypeSelection;
        private Subscription selectedPayoutTypePin;

        private final AutoCompleteComboBox<MediationResultReason> reasonSelection;
        private Subscription selectedReasonPin;

        private final MaterialTextArea summaryNotes;

        public View(VBox root, Model model, Controller controller) {
            super(root, model, controller);

            // payout types

            payoutTypeSelection = new AutoCompleteComboBox<>(model.getPayoutTypes(), Res.get("authorizedRole.mediator.mediationResult.selectPayoutType"));
            payoutTypeSelection.setPrefWidth(380);
            payoutTypeSelection.setConverter(new StringConverter<>() {
                @Override
                public String toString(Model.PayoutType payoutType) {
                    return payoutType != null ? Res.get("authorizedRole.mediator.mediationResult.payoutType." + payoutType.name()) : "";
                }

                @Override
                public Model.PayoutType fromString(String string) {
                    return null;
                }
            });

            // reason

            reasonSelection = new AutoCompleteComboBox<>(model.getReasons(), Res.get("authorizedRole.mediator.mediationResult.selectReason"));
            reasonSelection.setPrefWidth(380);
            reasonSelection.setConverter(new StringConverter<>() {
                @Override
                public String toString(MediationResultReason reason) {
                    return reason != null ? Res.get("authorizedRole.mediator.mediationResult.reason." + reason.name()) : "";
                }

                @Override
                public MediationResultReason fromString(String string) {
                    return null;
                }
            });

            // summary notes

            summaryNotes = new MaterialTextArea(Res.get("authorizedRole.mediator.mediationResult.summaryNotes"));
            summaryNotes.setFixedHeight(100);

            VBox content = new VBox(10,
                    payoutTypeSelection,
                    reasonSelection,
                    summaryNotes);

            content.setAlignment(Pos.CENTER_LEFT);
            root.getChildren().add(content);
        }

        @Override
        protected void onViewAttached() {
            payoutTypeSelection.setOnChangeConfirmed(e -> {
                if (payoutTypeSelection.getSelectionModel().getSelectedItem() == null) {
                    payoutTypeSelection.getSelectionModel().select(model.getSelectedPayoutType().get());
                    return;
                }
                controller.onSelectPayoutType(payoutTypeSelection.getSelectionModel().getSelectedItem());
            });

            selectedPayoutTypePin = EasyBind.subscribe(model.getSelectedPayoutType(),
                    payoutType -> {
                        if (payoutType != null) {
                            payoutTypeSelection.getSelectionModel().select(payoutType);
                        } else {
                            payoutTypeSelection.getSelectionModel().clearSelection();
                        }
                    });

            reasonSelection.setOnChangeConfirmed(e -> {
                if (reasonSelection.getSelectionModel().getSelectedItem() == null) {
                    reasonSelection.getSelectionModel().select(model.getSelectedReason().get());
                    return;
                }
                controller.onSelectReason(reasonSelection.getSelectionModel().getSelectedItem());
            });

            selectedReasonPin = EasyBind.subscribe(model.getSelectedReason(),
                    reason -> {
                        if (reason != null) {
                            reasonSelection.getSelectionModel().select(reason);
                        } else {
                            reasonSelection.getSelectionModel().clearSelection();
                        }
                    });

            summaryNotes.textProperty().bindBidirectional(model.getSummaryNotes());

            MediationCaseState mediationCaseState = model.muSigMediationCaseListItem.getMuSigMediationCase().getMediationCaseState().get();

            payoutTypeSelection.setDisable(mediationCaseState != MediationCaseState.OPEN);
            reasonSelection.setDisable(mediationCaseState != MediationCaseState.OPEN);
            summaryNotes.setEditable(mediationCaseState == MediationCaseState.OPEN);
        }

        @Override
        protected void onViewDetached() {
            payoutTypeSelection.setOnChangeConfirmed(null);
            selectedPayoutTypePin.unsubscribe();
            reasonSelection.setOnChangeConfirmed(null);
            selectedReasonPin.unsubscribe();

            summaryNotes.textProperty().unbindBidirectional(model.getSummaryNotes());
        }
    }
}
