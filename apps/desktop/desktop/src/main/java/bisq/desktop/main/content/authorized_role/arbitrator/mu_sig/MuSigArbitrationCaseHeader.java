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

package bisq.desktop.main.content.authorized_role.arbitrator.mu_sig;

import bisq.common.data.Triple;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.authorized_role.arbitrator.mu_sig.close.MuSigArbitrationCaseCloseController;
import bisq.desktop.main.content.authorized_role.arbitrator.mu_sig.details.MuSigArbitrationCaseDetailsController;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.settings.DontShowAgainService;
import bisq.support.arbitration.ArbitrationCaseState;
import bisq.support.arbitration.mu_sig.MuSigArbitratorService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;

import static bisq.settings.DontShowAgainKey.ARBITRATOR_LEAVE_CHANNEL_WARNING;
import static bisq.settings.DontShowAgainKey.ARBITRATOR_REMOVE_CASE_WARNING;

public class MuSigArbitrationCaseHeader {
    private final Controller controller;

    public MuSigArbitrationCaseHeader(ServiceProvider serviceProvider,
                                      Runnable onCloseHandler) {
        controller = new Controller(serviceProvider, onCloseHandler);
    }

    public HBox getRoot() {
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
        private final MuSigArbitratorService muSigArbitratorService;
        private final Runnable onCloseHandler;
        private final DontShowAgainService dontShowAgainService;
        private Subscription arbitrationCaseListItemPin;
        private Pin arbitrationCaseStatePin;
        private Pin arbitratorHasLeftChatPin;

        private Controller(ServiceProvider serviceProvider, Runnable onCloseHandler) {
            this.onCloseHandler = onCloseHandler;
            muSigArbitratorService = serviceProvider.getSupportService().getMuSigArbitratorService();
            dontShowAgainService = serviceProvider.getDontShowAgainService();

            model = new Model();
            view = new View(model, this);
        }

        private void setArbitrationCaseListItem(MuSigArbitrationCaseListItem item) {
            model.getArbitrationCaseListItem().set(item);
        }

        @Override
        public void onActivate() {
            arbitrationCaseListItemPin = EasyBind.subscribe(model.getArbitrationCaseListItem(), item -> {
                if (arbitrationCaseStatePin != null) {
                    arbitrationCaseStatePin.unbind();
                    arbitrationCaseStatePin = null;
                }
                if (arbitratorHasLeftChatPin != null) {
                    arbitratorHasLeftChatPin.unbind();
                    arbitratorHasLeftChatPin = null;
                }

                if (item == null) {
                    model.getIsClosedCase().set(false);
                    model.getShowLeaveButton().set(false);
                    return;
                }

                arbitrationCaseStatePin = item.getMuSigArbitrationCase().arbitrationCaseStateObservable().addObserver(state -> {
                    model.getIsClosedCase().set(state == ArbitrationCaseState.CLOSED);
                    model.getShowLeaveButton().set(state == ArbitrationCaseState.CLOSED && !item.getMuSigArbitrationCase().hasArbitratorLeftChat());
                });
                arbitratorHasLeftChatPin = item.getMuSigArbitrationCase().arbitratorHasLeftChatObservable().addObserver(
                        arbitratorHasLeftChat -> {
                            model.getShowLeaveButton().set(model.getIsClosedCase().get() && !arbitratorHasLeftChat);
                        });
            });
        }

        @Override
        public void onDeactivate() {
            arbitrationCaseListItemPin.unsubscribe();
            if (arbitrationCaseStatePin != null) {
                arbitrationCaseStatePin.unbind();
                arbitrationCaseStatePin = null;
            }
            if (arbitratorHasLeftChatPin != null) {
                arbitratorHasLeftChatPin.unbind();
                arbitratorHasLeftChatPin = null;
            }
        }

        void onCloseCase() {
            doClose();
            // TODO: move this eventually to Close Controller
            //                if (dontShowAgainService.showAgain(ARBITRATOR_CLOSE_WARNING)) {
            //                    new Popup().warning(Res.get("authorizedRole.disputeActor.close.warning"))
            //                            .dontShowAgainId(ARBITRATOR_CLOSE_WARNING)
            //                            .actionButtonText(Res.get("confirmation.yes"))
            //                            .onAction(this::doClose)
            //                            .closeButtonText(Res.get("confirmation.no"))
            //                            .show();
            //                } else {
            //                    doClose();
            //                }
        }

        void onLeaveChannel() {
            if (dontShowAgainService.showAgain(ARBITRATOR_LEAVE_CHANNEL_WARNING)) {
                new Popup().warning(Res.get("authorizedRole.disputeActor.leaveChannel.warning"))
                        .dontShowAgainId(ARBITRATOR_LEAVE_CHANNEL_WARNING)
                        .actionButtonText(Res.get("confirmation.yes"))
                        .onAction(this::doLeave)
                        .closeButtonText(Res.get("confirmation.no"))
                        .show();
            } else {
                doLeave();
            }
        }

        void onRemoveCase() {
            if (dontShowAgainService.showAgain(ARBITRATOR_REMOVE_CASE_WARNING)) {
                new Popup().warning(Res.get("authorizedRole.arbitrator.removeCase.warning"))
                        .dontShowAgainId(ARBITRATOR_REMOVE_CASE_WARNING)
                        .actionButtonText(Res.get("confirmation.yes"))
                        .onAction(this::doRemoveCase)
                        .closeButtonText(Res.get("confirmation.no"))
                        .show();
            } else {
                doRemoveCase();
            }
        }

        void onShowDetails() {
            MuSigArbitrationCaseListItem item = model.getArbitrationCaseListItem().get();
            Navigation.navigateTo(NavigationTarget.MU_SIG_ARBITRATION_CASE_DETAILS,
                    new MuSigArbitrationCaseDetailsController.InitData(item));
        }

        private void doRemoveCase() {
            MuSigArbitrationCaseListItem listItem = model.getArbitrationCaseListItem().get();
            if (listItem != null) {
                if (listItem.getMuSigArbitrationCase().getArbitrationCaseState() != ArbitrationCaseState.CLOSED) {
                    throw new RuntimeException("Only closed MuSig arbitration cases can be removed.");
                }
                muSigArbitratorService.removeArbitrationCase(listItem.getMuSigArbitrationCase());
            }
        }

        private void doLeave() {
            MuSigArbitrationCaseListItem listItem = model.getArbitrationCaseListItem().get();
            if (listItem != null) {
                muSigArbitratorService.leaveChat(listItem.getMuSigArbitrationCase());
            }
        }

        private void doClose() {
            MuSigArbitrationCaseListItem listItem = model.getArbitrationCaseListItem().get();
            if (listItem != null) {
                Navigation.navigateTo(NavigationTarget.MU_SIG_ARBITRATION_CASE_CLOSE,
                        new MuSigArbitrationCaseCloseController.InitData(listItem, onCloseHandler));
            }
        }
    }

    @Slf4j
    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<MuSigArbitrationCaseListItem> arbitrationCaseListItem = new SimpleObjectProperty<>();
        private final BooleanProperty isClosedCase = new SimpleBooleanProperty();
        private final BooleanProperty showLeaveButton = new SimpleBooleanProperty();
    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private final static double HEIGHT = 61;

        private final Triple<Text, Text, VBox> tradeId;
        private final UserProfileDisplay makerProfileDisplay, takerProfileDisplay;
        private final Label directionalTitle;
        private final Button closeButton, leaveButton, removeButton, detailsButton;
        private Subscription arbitrationCaseListItemPin, isClosedCasePin, showLeaveButtonPin;

        private View(Model model, Controller controller) {
            super(new HBox(40), model, controller);

            root.setMinHeight(HEIGHT);
            root.setMaxHeight(HEIGHT);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(0, 0, 0, 30));
            root.getStyleClass().add("chat-container-header");

            tradeId = getElements(Res.get("bisqEasy.tradeState.header.tradeId"));

            Triple<Text, UserProfileDisplay, VBox> maker = getUserProfileElements(Res.get("authorizedRole.disputeActor.table.maker"));
            makerProfileDisplay = maker.getSecond();

            Triple<Text, UserProfileDisplay, VBox> taker = getUserProfileElements(Res.get("authorizedRole.disputeActor.table.taker"));
            takerProfileDisplay = taker.getSecond();

            directionalTitle = new Label();
            directionalTitle.setAlignment(Pos.CENTER);
            directionalTitle.setMinWidth(80);
            tradeId.getThird().setMinWidth(80);

            closeButton = new Button(Res.get("authorizedRole.disputeActor.close"));
            closeButton.setDefaultButton(true);
            closeButton.setMinWidth(120);
            closeButton.setStyle("-fx-padding: 5 16 5 16");

            leaveButton = new Button(Res.get("authorizedRole.disputeActor.leave"));
            leaveButton.getStyleClass().add("outlined-button");
            leaveButton.setMinWidth(120);
            leaveButton.setStyle("-fx-padding: 5 16 5 16");

            removeButton = new Button(Res.get("authorizedRole.disputeActor.remove"));
            removeButton.setMinWidth(120);
            removeButton.setStyle("-fx-padding: 5 16 5 16");

            detailsButton = new Button(Res.get("authorizedRole.disputeActor.disputeCaseDetails.show"));
            detailsButton.getStyleClass().add("grey-transparent-outlined-button");
            detailsButton.setMinWidth(160);

            Region spacer = Spacer.fillHBox();
            HBox.setMargin(spacer, new Insets(0, -50, 0, 0));
            HBox.setMargin(directionalTitle, new Insets(10, -20, 0, -20));
            HBox.setMargin(leaveButton, new Insets(0, -20, 0, 0));
            HBox.setMargin(removeButton, new Insets(0, -20, 0, 0));
            HBox.setMargin(detailsButton, new Insets(0, -20, 0, 0));
            HBox.setMargin(closeButton, new Insets(0, -20, 0, 0));
            root.getChildren().addAll(maker.getThird(), directionalTitle, taker.getThird(), tradeId.getThird(), spacer,
                    detailsButton, removeButton, leaveButton, closeButton);
        }

        @Override
        protected void onViewAttached() {
            arbitrationCaseListItemPin = EasyBind.subscribe(model.getArbitrationCaseListItem(), item -> {
                if (item != null) {
                    makerProfileDisplay.setVisible(true);
                    makerProfileDisplay.setManaged(true);
                    takerProfileDisplay.setVisible(true);
                    takerProfileDisplay.setManaged(true);
                    makerProfileDisplay.getStyleClass().remove("mediator-header-requester");
                    takerProfileDisplay.getStyleClass().remove("mediator-header-requester");
                    makerProfileDisplay.setUserProfile(item.getMaker().getUserProfile());
                    makerProfileDisplay.setReputationScore(item.getMaker().getReputationScore());
                    boolean isMakerRequester = item.isMakerRequester();
                    if (isMakerRequester) {
                        makerProfileDisplay.getStyleClass().add("mediator-header-requester");
                    }
                    makerProfileDisplay.getTooltip().setText(Res.get("authorizedRole.arbitrator.hasRequested",
                            makerProfileDisplay.getTooltipText(),
                            isMakerRequester ? Res.get("confirmation.yes") : Res.get("confirmation.no")
                    ));

                    directionalTitle.setText(item.getDirectionalTitle());

                    takerProfileDisplay.setUserProfile(item.getTaker().getUserProfile());
                    takerProfileDisplay.setReputationScore(item.getTaker().getReputationScore());
                    if (!isMakerRequester) {
                        takerProfileDisplay.getStyleClass().add("mediator-header-requester");
                    }
                    takerProfileDisplay.getTooltip().setText(Res.get("authorizedRole.arbitrator.hasRequested",
                            takerProfileDisplay.getTooltipText(),
                            !isMakerRequester ? Res.get("confirmation.yes") : Res.get("confirmation.no")
                    ));

                    tradeId.getSecond().setText(item.getShortTradeId());
                } else {
                    makerProfileDisplay.setVisible(false);
                    makerProfileDisplay.setManaged(false);
                    takerProfileDisplay.setVisible(false);
                    takerProfileDisplay.setManaged(false);
                    directionalTitle.setText(null);
                    tradeId.getSecond().setText(null);
                }
            });

            isClosedCasePin = EasyBind.subscribe(model.getIsClosedCase(),
                    isClosedCase -> {
                        removeButton.setVisible(isClosedCase);
                        removeButton.setManaged(isClosedCase);
                        closeButton.setVisible(!isClosedCase);
                        closeButton.setManaged(!isClosedCase);
                    });
            showLeaveButtonPin = EasyBind.subscribe(model.getShowLeaveButton(),
                    showLeaveButton -> {
                        leaveButton.setVisible(showLeaveButton);
                        leaveButton.setManaged(showLeaveButton);
                    });
            closeButton.setOnAction(e -> controller.onCloseCase());
            leaveButton.setOnAction(e -> controller.onLeaveChannel());
            removeButton.setOnAction(e -> controller.onRemoveCase());
            detailsButton.setOnAction(e -> controller.onShowDetails());
        }

        @Override
        protected void onViewDetached() {
            arbitrationCaseListItemPin.unsubscribe();
            isClosedCasePin.unsubscribe();
            showLeaveButtonPin.unsubscribe();
            closeButton.setOnAction(null);
            leaveButton.setOnAction(null);
            removeButton.setOnAction(null);
            detailsButton.setOnAction(null);

            makerProfileDisplay.dispose();
            takerProfileDisplay.dispose();
        }

        private Triple<Text, UserProfileDisplay, VBox> getUserProfileElements(@Nullable String description) {
            Text descriptionLabel = description == null ? new Text() : new Text(description.toUpperCase());
            descriptionLabel.getStyleClass().add("bisq-easy-open-trades-header-description");
            UserProfileDisplay userProfileDisplay = new UserProfileDisplay(25);
            userProfileDisplay.setPadding(new Insets(0, -15, 0, 0));
            userProfileDisplay.setMinWidth(200);
            VBox vBox = new VBox(2, descriptionLabel, userProfileDisplay);
            vBox.setAlignment(Pos.CENTER_LEFT);
            return new Triple<>(descriptionLabel, userProfileDisplay, vBox);
        }

        private Triple<Text, Text, VBox> getElements(@Nullable String description) {
            Text descriptionLabel = description == null ? new Text() : new Text(description.toUpperCase());
            descriptionLabel.getStyleClass().add("bisq-easy-open-trades-header-description");
            Text valueLabel = new Text();
            valueLabel.getStyleClass().add("bisq-easy-open-trades-header-value");
            VBox.setMargin(descriptionLabel, new Insets(2, 0, 1.5, 0));
            VBox vBox = new VBox(descriptionLabel, valueLabel);
            vBox.setAlignment(Pos.CENTER_LEFT);
            vBox.setMinHeight(HEIGHT);
            vBox.setMaxHeight(HEIGHT);
            return new Triple<>(descriptionLabel, valueLabel, vBox);
        }
    }
}
