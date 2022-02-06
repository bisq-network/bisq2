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

package bisq.desktop.primary.main.content.social.profile.components;

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqInputTextField;
import bisq.desktop.components.controls.BisqLabel;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.i18n.Res;
import bisq.social.userprofile.Entitlement;
import bisq.social.userprofile.UserProfileService;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class EntitlementSelection {
    private final Controller controller;

    public EntitlementSelection(UserProfileService userProfileService, ObjectProperty<KeyPair> keyPair) {
        controller = new Controller(userProfileService, keyPair);
    }

    public View getView() {
        return controller.view;
    }

    public Set<Entitlement> getVerifiedEntitlements() {
        return controller.model.verifiedEntitlements;
    }

    @Slf4j
    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final UserProfileService userProfileService;


        private Controller(UserProfileService userProfileService, ObjectProperty<KeyPair> keyPair) {
            this.userProfileService = userProfileService;

            model = new Model(keyPair);
            view = new View(model, this);
        }

        @Override
        public void onViewAttached() {
            model.tableVisible.set(false);
        }

        @Override
        public void onViewDetached() {
        }


        private void onShowTable() {
            model.tableVisible.set(true);
        }

        private void onUpdateItemWithTxIdInputTextField(EntitlementItem entitlementItem, BisqInputTextField inputTextField) {
            if (entitlementItem == null) return;

            if (entitlementItem.getType().getProofTypes().contains(Entitlement.ProofType.PROOF_OF_BURN)) {
                //inputTextField.setPromptText("PROOF_OF_BURN");
            } else if (entitlementItem.getType().getProofTypes().contains(Entitlement.ProofType.BONDED_ROLE)) {
                // inputTextField.setPromptText(Res.common.get("social.createUserProfile.entitlement.table.header.proof.prompt.bondedRole"));
            }
            //todo show input validation result
        }

        private void onUpdateItemWithVerifyButton(EntitlementItem entitlementItem, Button button) {
            if (entitlementItem == null)
                return;

            entitlementItem.setButton(button);
        }

        private void onVerifyEntitlementItem(EntitlementItem entitlementItem) {
            if (entitlementItem == null || entitlementItem.getProof().get() == null || entitlementItem.getProof().get().isEmpty())
                return;

            String proof = entitlementItem.getProof().get();
            Button button = entitlementItem.getButton();
            Entitlement.Type entitlementType = entitlementItem.getType();
            userProfileService.verifyEntitlement(entitlementType, proof, model.keyPair.get().getPublic())
                    .whenComplete((result, throwable) -> {
                        UIThread.run(() -> {
                            if (throwable == null) {
                                if (result) {
                                    model.verifiedEntitlements.add(new Entitlement(entitlementType, proof));
                                    button.getStyleClass().add("action-button");
                                    entitlementItem.buttonText.set(Res.common.get("social.createUserProfile.table.entitlement.verify.success"));
                                } else {
                                    button.getStyleClass().remove("action-button");
                                    entitlementItem.buttonText.set(Res.common.get("social.createUserProfile.table.entitlement.verify.failed"));
                                    log.warn("Entitlement verification failed."); // todo 
                                }
                            } else {
                                log.warn("Error at entitlement verification."); // todo 
                            }
                        });
                    });
        }

        private void onShowInfo(EntitlementItem entitlementItem) {
            //todo
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final Set<Entitlement> verifiedEntitlements = new HashSet<>();
        private final ObservableList<EntitlementItem> observableList = FXCollections.observableArrayList(Stream.of(Entitlement.Type.values())
                .map(EntitlementItem::new)
                .collect(Collectors.toList()));
        private final SortedList<EntitlementItem> sortedList = new SortedList<>(observableList);
        private final BooleanProperty tableVisible = new SimpleBooleanProperty();
        private final ObjectProperty<KeyPair> keyPair;

        private Model(ObjectProperty<KeyPair> keyPair) {
            this.keyPair = keyPair;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final BisqTableView<EntitlementItem> tableView;
        private final BisqLabel headline;
        private final BisqButton button;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);
            root.setSpacing(10);

            button = new BisqButton(Res.common.get("social.createUserProfile.entitlement.headline"));
            button.setMinWidth(300);

            headline = new BisqLabel(Res.common.get("social.createUserProfile.entitlement.headline"));
            headline.getStyleClass().add("titled-group-bg-label-active");
            headline.setPadding(new Insets(20, 0, 0, 0));

            tableView = new BisqTableView<>(model.sortedList);
            tableView.setMaxHeight(300);
            configTableView();

            root.getChildren().addAll(button, headline, tableView);
        }

        @Override
        public void onViewAttached() {
            button.setOnAction(e -> controller.onShowTable());
            button.visibleProperty().bind(model.tableVisible.not());
            button.managedProperty().bind(model.tableVisible.not());
            headline.visibleProperty().bind(model.tableVisible);
            headline.managedProperty().bind(model.tableVisible);
            tableView.visibleProperty().bind(model.tableVisible);
            tableView.managedProperty().bind(model.tableVisible);
        }

        @Override
        protected void onViewDetached() {
            button.setOnAction(null);
            button.visibleProperty().unbind();
            button.managedProperty().unbind();
            headline.visibleProperty().unbind();
            headline.managedProperty().unbind();
            tableView.visibleProperty().unbind();
            tableView.managedProperty().unbind();
        }

        private void configTableView() {
            tableView.getColumns().add(new BisqTableColumn.Builder<EntitlementItem>()
                    .title(Res.common.get("social.createUserProfile.entitlement.table.header.typeName"))
                    .minWidth(400)
                    .valueSupplier(EntitlementItem::getTypeName)
                    .build());

            tableView.getColumns().add(new BisqTableColumn.Builder<EntitlementItem>()
                    .title(Res.common.get("social.createUserProfile.entitlement.table.header.proof"))
                    .minWidth(200)
                    .cellFactory(BisqTableColumn.CellFactory.TEXT_INPUT)
                    .updateItemWithInputTextFieldHandler(controller::onUpdateItemWithTxIdInputTextField)
                    .valuePropertyBiDirBindingSupplier(item -> item.proof)
                    .build());
            tableView.getColumns().add(new BisqTableColumn.Builder<EntitlementItem>()
                    .title(Res.common.get("social.createUserProfile.entitlement.table.header.verification"))
                    .fixWidth(160)
                    .cellFactory(BisqTableColumn.CellFactory.BUTTON)
                    .updateItemWithButtonHandler(controller::onUpdateItemWithVerifyButton)
                    .actionHandler(controller::onVerifyEntitlementItem)
                    .valuePropertyBiDirBindingSupplier(item -> item.buttonText)
                    .build());
            tableView.getColumns().add(new BisqTableColumn.Builder<EntitlementItem>()
                    .fixWidth(160)
                    .cellFactory(BisqTableColumn.CellFactory.BUTTON)
                    .actionHandler(controller::onShowInfo)
                    .value(Res.common.get("social.createUserProfile.entitlement.table.header.info"))
                    .build());
        }

    }

    @Getter
    private static class EntitlementItem implements TableItem {
        private final String typeName;
        private final Entitlement.Type type;
        private final StringProperty proof = new SimpleStringProperty();
        private final StringProperty proofPrompt = new SimpleStringProperty();
        private final StringProperty buttonText = new SimpleStringProperty(Res.common.get("social.createUserProfile.table.entitlement.verify"));
        @Setter
        private Button button;

        private EntitlementItem(Entitlement.Type type) {
            this.type = type;
            this.typeName = Res.common.get(type.name());
        }

        @Override
        public void activate() {
            proof.set("");
        }

        @Override
        public void deactivate() {
        }
    }
}