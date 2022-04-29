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

package bisq.desktop.primary.main.content.settings.userProfile.components;

import bisq.common.encoding.Hex;
import bisq.common.monetary.Coin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.BisqTextFieldWithCopyIcon;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.desktop.overlay.Popup;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.security.DigestUtil;
import bisq.social.user.entitlement.Role;
import bisq.social.user.proof.Proof;
import bisq.social.user.proof.ProofOfBurnProof;
import bisq.social.user.UserProfileService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//todo missing validations
@Slf4j
public class EntitlementSelection {
    private final Controller controller;

    public EntitlementSelection(UserProfileService userProfileService) {
        controller = new Controller(userProfileService);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void reset() {
        controller.reset();
    }

    public void show(KeyPair keyPair) {
        controller.model.tableVisible.set(true);
        controller.model.keyPair = keyPair;
    }

    public Set<Role> getVerifiedEntitlements() {
        return controller.model.verifiedRoles;
    }

    @Slf4j
    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final UserProfileService userProfileService;

        private Controller(UserProfileService userProfileService) {
            this.userProfileService = userProfileService;

            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            model.tableVisible.set(false);
        }

        private void reset() {
            model.verifiedRoles.clear();
            model.tableVisible.set(false);
        }

        @Override
        public void onDeactivate() {
        }

        private CompletableFuture<Optional<ProofOfBurnProof>> onVerifyProofOfBurn(EntitlementItem entitlementItem,
                                                                                  String pubKeyHash,
                                                                                  String proofOfBurnTxId) {
            return userProfileService.verifyProofOfBurn(entitlementItem.getType(), proofOfBurnTxId, pubKeyHash)
                    .whenComplete((proof, throwable) -> {
                        UIThread.run(() -> {
                            if (throwable == null && proof.isPresent()) {
                                model.verifiedRoles.add(new Role(entitlementItem.getType(), proof.get()));
                            } else {
                                log.warn("Error at type verification."); // todo 
                            }
                        });
                    });
        }

        private CompletableFuture<Optional<Proof>> onVerifyBondedRole(EntitlementItem entitlementItem, String bondedRoleTxId, String pubKeyHash, String bondedRoleSig) {
            return userProfileService.verifyBondedRole(bondedRoleTxId,
                            bondedRoleSig,
                            pubKeyHash)
                    .whenComplete((proof, throwable) -> {
                        UIThread.run(() -> {
                            if (throwable == null) {
                                if (proof.isPresent()) {
                                    model.verifiedRoles.add(new Role(entitlementItem.getType(), proof.get()));
                                } else {
                                    log.warn("Entitlement verification failed."); // todo 
                                }
                            } else {
                                log.warn("Error at type verification."); // todo 
                            }
                        });
                    });
        }

        private CompletableFuture<Optional<Proof>> onVerifyModerator(EntitlementItem entitlementItem, String invitationCode) {
            return userProfileService.verifyModerator(invitationCode, model.keyPair.getPublic())
                    .whenComplete((proof, throwable) -> {
                        UIThread.run(() -> {
                            if (throwable == null) {
                                if (proof.isPresent()) {
                                    model.verifiedRoles.add(new Role(entitlementItem.getType(), proof.get()));
                                } else {
                                    log.warn("Entitlement verification failed."); // todo 
                                }
                            } else {
                                log.warn("Error at type verification."); // todo 
                            }
                        });
                    });
        }

        private void onShowInfo(EntitlementItem entitlementItem) {
            //todo
        }

        public void onOpenProofWindow(EntitlementItem entitlementItem) {
            model.minBurnAmount = AmountFormatter.formatAmountWithCode(Coin.of(userProfileService.getMinBurnAmount(entitlementItem.getType()), "BSQ"));
            if (model.keyPair != null) {
                new ProofPopup(this, model, entitlementItem).show();
            }
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final Set<Role> verifiedRoles = new HashSet<>();
        private final ObservableList<EntitlementItem> observableList = FXCollections.observableArrayList(Stream.of(Role.Type.values())
                .map(EntitlementItem::new)
                .collect(Collectors.toList()));
        private final SortedList<EntitlementItem> sortedList = new SortedList<>(observableList);
        private final BooleanProperty tableVisible = new SimpleBooleanProperty();
        private KeyPair keyPair;
        private String minBurnAmount;

        private String getPubKeyHash() {
            return Hex.encode(DigestUtil.hash(keyPair.getPublic().getEncoded()));
        }

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final BisqTableView<EntitlementItem> tableView;
        private final Label headline;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);
            root.setSpacing(10);

            headline = new Label(Res.get("social.createUserProfile.entitlement.headline"));
            headline.getStyleClass().add("titled-group-bg-label-active");
            headline.setPadding(new Insets(20, 0, 0, 0));

            tableView = new BisqTableView<>(model.sortedList);
            tableView.setMaxHeight(300);
            configTableView();

            root.getChildren().addAll(headline, tableView);
        }

        @Override
        protected void onViewAttached() {
            headline.visibleProperty().bind(model.tableVisible);
            headline.managedProperty().bind(model.tableVisible);
            tableView.visibleProperty().bind(model.tableVisible);
            tableView.managedProperty().bind(model.tableVisible);
        }

        @Override
        protected void onViewDetached() {
            headline.visibleProperty().unbind();
            headline.managedProperty().unbind();
            tableView.visibleProperty().unbind();
            tableView.managedProperty().unbind();
        }

        private void configTableView() {
            tableView.getColumns().add(new BisqTableColumn.Builder<EntitlementItem>()
                    .title(Res.get("social.createUserProfile.entitlement.table.header.typeName"))
                    .minWidth(400)
                    .valueSupplier(EntitlementItem::getTypeName)
                    .build());
            tableView.getColumns().add(new BisqTableColumn.Builder<EntitlementItem>()
                    .title(Res.get("social.createUserProfile.entitlement.table.header.proof"))
                    .fixWidth(160)
                    .cellFactory(BisqTableColumn.DefaultCellFactories.BUTTON)
                    .actionHandler(controller::onOpenProofWindow)
                    .value(Res.get("social.createUserProfile.entitlement.table.proof.button"))
                    .build());
            tableView.getColumns().add(new BisqTableColumn.Builder<EntitlementItem>()
                    .fixWidth(120)
                    .cellFactory(BisqTableColumn.DefaultCellFactories.BUTTON)
                    .actionHandler(controller::onShowInfo)
                    .value(Res.get("social.createUserProfile.entitlement.table.header.info"))
                    .build());
        }
    }

    @Getter
    private static class EntitlementItem implements TableItem {
        private final String typeName;
        private final Role.Type type;
        @Setter
        private Button button;

        private EntitlementItem(Role.Type type) {
            this.type = type;
            String info = switch (type) {
                case LIQUIDITY_PROVIDER -> Res.get("social.createUserProfile.liquidityProvider.info");
                case CHANNEL_ADMIN -> Res.get("social.createUserProfile.administrator.info");
                case CHANNEL_MODERATOR -> Res.get("social.createUserProfile.moderator.info");
                case MEDIATOR -> Res.get("social.createUserProfile.mediator.info");
            };
            this.typeName = Res.get(type.name()) + "\n" + info;
        }

        @Override
        public void activate() {
        }

        @Override
        public void deactivate() {
        }
    }

    private static class ProofPopup extends Popup {
        private final Controller controller;
        private final Model model;
        private final EntitlementItem entitlementItem;
        private TextField firstField, secondField;
        private BisqTextFieldWithCopyIcon pubKeyHashField;

        //todo missing validations
        public ProofPopup(Controller controller, Model model, EntitlementItem entitlementItem) {
            super();
            this.controller = controller;
            this.model = model;
            this.entitlementItem = entitlementItem;
            headLine(Res.get("social.createUserProfile.entitlement.popup.headline"));
            if (entitlementItem.getType().getTypes().contains(Proof.Type.CHANNEL_ADMIN_INVITATION)) {
                message(Res.get("social.createUserProfile.entitlement.popup.moderator.message"));
                actionButtonText(Res.get("social.createUserProfile.table.entitlement.verify"));
            } else if (entitlementItem.getType().getTypes().contains(Proof.Type.PROOF_OF_BURN)) {
                message(Res.get("social.createUserProfile.entitlement.popup.proofOfBurn.message"));
                actionButtonText(Res.get("social.createUserProfile.table.entitlement.liquidityProvider.confirmProofOfBurn"));
            } else if (entitlementItem.getType().getTypes().contains(Proof.Type.BONDED_ROLE)) {
                message(Res.get("social.createUserProfile.entitlement.popup.bondedRole.message"));
                actionButtonText(Res.get("social.createUserProfile.table.entitlement.verify"));
            }
            actionButtonText(Res.get("social.createUserProfile.table.entitlement.verify"));
            doCloseOnAction(false);
        }

        @Override
        protected void addContent() {
            super.addContent();

            GridPane.setMargin(messageLabel, new Insets(0, 0, 20, 0));
            gridPane.addTextFieldWithCopyIcon(Res.get("social.createUserProfile.entitlement.popup.minBurnAmount"), model.minBurnAmount);

            // For dev testing we set UserProfileService.USE_DEV_TEST_POB_VALUES = true to override any input values 
            // with hard coded values which have been used for a real POB tx 
            String pubKeyHash = model.getPubKeyHash();
            gridPane.addTextFieldWithCopyIcon(Res.get("social.createUserProfile.entitlement.popup.pubKeyHash"), pubKeyHash);
            firstField = gridPane.addTextField("", "");
            switch (entitlementItem.getType()) {
                case LIQUIDITY_PROVIDER -> {
                    firstField.setPromptText(Res.get("social.createUserProfile.entitlement.popup.proofOfBurn"));
                    onAction(() -> {
                                actionButton.setDisable(true); //todo add busy animation
                                controller.onVerifyProofOfBurn(entitlementItem, pubKeyHash, firstField.getText())
                                        .whenComplete((proof, throwable) -> {
                                            UIThread.run(() -> {
                                                if (throwable == null && proof.isPresent()) {
                                                    //todo hide button and show feedback text instead
                                                    actionButton.setDefaultButton(true);
                                                    actionButton.setText(Res.get("social.createUserProfile.table.entitlement.verify.success"));
                                                } else {
                                                    actionButton.setDefaultButton(false);
                                                    actionButton.setText(Res.get("social.createUserProfile.table.entitlement.verify.failed"));
                                                }
                                            });
                                        });
                            }
                    );
                }
                case MEDIATOR, CHANNEL_ADMIN -> {
                    firstField.setPromptText(Res.get("social.createUserProfile.entitlement.popup.bondedRole.txId"));
                    secondField = new TextField();
                    secondField = gridPane.addTextField(Res.get("social.createUserProfile.entitlement.popup.bondedRole.sig"), "");
                    onAction(() -> {
                                actionButton.setDisable(true); //todo add busy animation
                                controller.onVerifyBondedRole(entitlementItem, firstField.getText(), model.getPubKeyHash(), secondField.getText())
                                        .whenComplete((proof, throwable) -> {
                                            UIThread.run(() -> {
                                                if (throwable == null && proof.isPresent()) {
                                                    //todo hide button and show feedback text instead
                                                    actionButton.setDisable(false);
                                                    actionButton.setDefaultButton(true);
                                                    actionButton.setText(Res.get("social.createUserProfile.table.entitlement.verify.success"));
                                                } else {
                                                    actionButton.setDefaultButton(false);
                                                    actionButton.setText(Res.get("social.createUserProfile.table.entitlement.verify.failed"));
                                                }
                                            });
                                        });
                            }
                    );
                }
                case CHANNEL_MODERATOR -> {
                    firstField.setPromptText(Res.get("social.createUserProfile.entitlement.popup.moderator.code"));
                    onAction(() -> {
                                actionButton.setDisable(true); //todo add busy animation
                                controller.onVerifyModerator(entitlementItem, firstField.getText())
                                        .whenComplete((proof, throwable) -> {
                                            UIThread.run(() -> {
                                                if (throwable == null && proof.isPresent()) {
                                                    //todo hide button and show feedback text instead
                                                    actionButton.setDisable(false);
                                                    actionButton.setDefaultButton(true);
                                                    actionButton.setText(Res.get("social.createUserProfile.table.entitlement.verify.success"));
                                                } else {
                                                    actionButton.setDefaultButton(false);
                                                    actionButton.setText(Res.get("social.createUserProfile.table.entitlement.verify.failed"));
                                                }
                                            });
                                        });
                            }
                    );
                }
            }
        }
    }
}