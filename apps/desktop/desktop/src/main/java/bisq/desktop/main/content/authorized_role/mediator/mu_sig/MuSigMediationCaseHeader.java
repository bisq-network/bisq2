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

package bisq.desktop.main.content.authorized_role.mediator.mu_sig;

import bisq.chat.ChatService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.priv.LeavePrivateChatManager;
import bisq.common.data.Triple;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.authorized_role.mediator.mu_sig.close.MuSigMediationCaseCloseController;
import bisq.desktop.main.content.authorized_role.mediator.mu_sig.details.MuSigMediationCaseDetailsController;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.settings.DontShowAgainService;
import bisq.support.mediation.MediationCaseState;
import bisq.support.mediation.mu_sig.MuSigMediatorService;
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

import static bisq.settings.DontShowAgainKey.MEDIATOR_LEAVE_CHANNEL_WARNING;
import static bisq.settings.DontShowAgainKey.MEDIATOR_REMOVE_CASE_WARNING;

public class MuSigMediationCaseHeader {
    private final Controller controller;

    public MuSigMediationCaseHeader(ServiceProvider serviceProvider,
                                    Runnable onCloseHandler,
                                    Runnable onReOpenHandler) {
        controller = new Controller(serviceProvider, onCloseHandler, onReOpenHandler);
    }

    public HBox getRoot() {
        return controller.view.getRoot();
    }

    public void setMediationCaseListItem(MuSigMediationCaseListItem item) {
        controller.setMediationCaseListItem(item);
    }

    public void setShowClosedCases(boolean showClosedCases) {
        controller.model.getShowClosedCases().set(showClosedCases);
    }

    @Slf4j
    private static class Controller implements bisq.desktop.common.view.Controller {
        @Getter
        private final View view;
        private final Model model;
        private final MuSigMediatorService muSigMediatorService;
        private final Runnable onCloseHandler;
        private final Runnable onReOpenHandler;
        private final LeavePrivateChatManager leavePrivateChatManager;
        private final DontShowAgainService dontShowAgainService;

        private Controller(ServiceProvider serviceProvider, Runnable onCloseHandler, Runnable onReOpenHandler) {
            this.onCloseHandler = onCloseHandler;
            this.onReOpenHandler = onReOpenHandler;
            ChatService chatService = serviceProvider.getChatService();
            leavePrivateChatManager = chatService.getLeavePrivateChatManager();
            muSigMediatorService = serviceProvider.getSupportService().getMuSigMediatorService();
            dontShowAgainService = serviceProvider.getDontShowAgainService();

            model = new Model();
            view = new View(model, this);
        }

        private void setMediationCaseListItem(MuSigMediationCaseListItem item) {
            model.getMediationCaseListItem().set(item);
        }

        @Override
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
        }

        void onToggleOpenClose() {
            if (model.getShowClosedCases().get()) {
                doReOpen();
            } else {
                doClose();
                // TODO: move this eventually to Close Controller
//                if (dontShowAgainService.showAgain(MEDIATOR_CLOSE_WARNING)) {
//                    new Popup().warning(Res.get("authorizedRole.mediator.close.warning"))
//                            .dontShowAgainId(MEDIATOR_CLOSE_WARNING)
//                            .actionButtonText(Res.get("confirmation.yes"))
//                            .onAction(this::doClose)
//                            .closeButtonText(Res.get("confirmation.no"))
//                            .show();
//                } else {
//                    doClose();
//                }
            }
        }

        void onLeaveChannel() {
            if (dontShowAgainService.showAgain(MEDIATOR_LEAVE_CHANNEL_WARNING)) {
                new Popup().warning(Res.get("authorizedRole.mediator.leaveChannel.warning"))
                        .dontShowAgainId(MEDIATOR_LEAVE_CHANNEL_WARNING)
                        .actionButtonText(Res.get("confirmation.yes"))
                        .onAction(this::doLeave)
                        .closeButtonText(Res.get("confirmation.no"))
                        .show();
            } else {
                doLeave();
            }
        }

        void onRemoveCase() {
            if (dontShowAgainService.showAgain(MEDIATOR_REMOVE_CASE_WARNING)) {
                new Popup().warning(Res.get("authorizedRole.mediator.removeCase.warning"))
                        .dontShowAgainId(MEDIATOR_REMOVE_CASE_WARNING)
                        .actionButtonText(Res.get("confirmation.yes"))
                        .onAction(this::doRemoveCase)
                        .closeButtonText(Res.get("confirmation.no"))
                        .show();
            } else {
                doRemoveCase();
            }
        }

        void onShowDetails() {
            MuSigMediationCaseListItem item = model.getMediationCaseListItem().get();
            Navigation.navigateTo(NavigationTarget.MU_SIG_MEDIATION_CASE_DETAILS, new MuSigMediationCaseDetailsController.InitData(item));
        }

        private void doRemoveCase() {
            MuSigMediationCaseListItem listItem = model.getMediationCaseListItem().get();
            if (listItem != null) {
                if (listItem.getMuSigMediationCase().getMediationCaseState().get() != MediationCaseState.CLOSED) {
                    throw new RuntimeException("Only closed MuSig mediation cases can be removed.");
                }
                doLeave();
                muSigMediatorService.removeMediationCase(listItem.getMuSigMediationCase());
            }
        }

        private void doLeave() {
            MuSigMediationCaseListItem listItem = model.getMediationCaseListItem().get();
            if (listItem != null) {
                MuSigOpenTradeChannel channel = listItem.getChannel();
                if (channel != null) {
                    leavePrivateChatManager.leaveChannel(channel);
                }
            }
        }

        private void doClose() {
            MuSigMediationCaseListItem listItem = model.getMediationCaseListItem().get();
            if (listItem != null) {
                Navigation.navigateTo(NavigationTarget.MU_SIG_MEDIATION_CASE_CLOSE, new MuSigMediationCaseCloseController.InitData(listItem, onCloseHandler));
            }
        }

        private void doReOpen() {
            MuSigMediationCaseListItem listItem = model.getMediationCaseListItem().get();
            if (listItem != null) {
                muSigMediatorService.reOpenMediationCase(listItem.getMuSigMediationCase());
                onReOpenHandler.run();
            }
        }
    }

    @Slf4j
    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<MuSigMediationCaseListItem> mediationCaseListItem = new SimpleObjectProperty<>();
        private final BooleanProperty showClosedCases = new SimpleBooleanProperty();
    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private final static double HEIGHT = 61;

        private final Triple<Text, Text, VBox> tradeId;
        private final UserProfileDisplay makerProfileDisplay, takerProfileDisplay;
        private final Label directionalTitle;
        private final Button openCloseButton, leaveButton, removeButton, detailsButton;
        private Subscription mediationCaseListItemPin, showClosedCasesPin;

        private View(Model model, Controller controller) {
            super(new HBox(40), model, controller);

            root.setMinHeight(HEIGHT);
            root.setMaxHeight(HEIGHT);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(0, 0, 0, 30));
            root.getStyleClass().add("chat-container-header");

            tradeId = getElements(Res.get("bisqEasy.tradeState.header.tradeId"));

            Triple<Text, UserProfileDisplay, VBox> maker = getUserProfileElements(Res.get("authorizedRole.mediator.table.maker"));
            makerProfileDisplay = maker.getSecond();

            Triple<Text, UserProfileDisplay, VBox> taker = getUserProfileElements(Res.get("authorizedRole.mediator.table.taker"));
            takerProfileDisplay = taker.getSecond();

            directionalTitle = new Label();
            directionalTitle.setAlignment(Pos.CENTER);
            directionalTitle.setMinWidth(80);
            tradeId.getThird().setMinWidth(80);

            openCloseButton = new Button();
            openCloseButton.setDefaultButton(true);
            openCloseButton.setMinWidth(120);
            openCloseButton.setStyle("-fx-padding: 5 16 5 16");

            leaveButton = new Button(Res.get("authorizedRole.mediator.leave"));
            leaveButton.getStyleClass().add("outlined-button");
            leaveButton.setMinWidth(120);
            leaveButton.setStyle("-fx-padding: 5 16 5 16");

            removeButton = new Button(Res.get("authorizedRole.mediator.remove"));
            removeButton.setMinWidth(120);
            removeButton.setStyle("-fx-padding: 5 16 5 16");

            detailsButton = new Button(Res.get("authorizedRole.mediator.mediationCaseDetails.show"));
            detailsButton.getStyleClass().add("grey-transparent-outlined-button");
            detailsButton.setMinWidth(160);

            Region spacer = Spacer.fillHBox();
            HBox.setMargin(spacer, new Insets(0, -50, 0, 0));
            HBox.setMargin(directionalTitle, new Insets(10, -20, 0, -20));
            HBox.setMargin(leaveButton, new Insets(0, -20, 0, 0));
            HBox.setMargin(removeButton, new Insets(0, -20, 0, 0));
            HBox.setMargin(detailsButton, new Insets(0, -20, 0, 0));
            HBox.setMargin(openCloseButton, new Insets(0, -20, 0, 0));
            root.getChildren().addAll(maker.getThird(), directionalTitle, taker.getThird(), tradeId.getThird(), spacer,
                    detailsButton, removeButton, leaveButton, openCloseButton);
        }

        @Override
        protected void onViewAttached() {
            mediationCaseListItemPin = EasyBind.subscribe(model.getMediationCaseListItem(), item -> {
                if (item != null) {
                    makerProfileDisplay.getStyleClass().remove("mediator-header-requester");
                    takerProfileDisplay.getStyleClass().remove("mediator-header-requester");
                    makerProfileDisplay.setUserProfile(item.getMaker().getUserProfile());
                    makerProfileDisplay.setReputationScore(item.getMaker().getReputationScore());
                    boolean isMakerRequester = item.isMakerRequester();
                    if (isMakerRequester) {
                        makerProfileDisplay.getStyleClass().add("mediator-header-requester");
                    }
                    makerProfileDisplay.getTooltip().setText(Res.get("authorizedRole.mediator.hasRequested",
                            makerProfileDisplay.getTooltipText(),
                            isMakerRequester ? Res.get("confirmation.yes") : Res.get("confirmation.no")
                    ));

                    directionalTitle.setText(item.getDirectionalTitle());

                    takerProfileDisplay.setUserProfile(item.getTaker().getUserProfile());
                    takerProfileDisplay.setReputationScore(item.getTaker().getReputationScore());
                    if (!isMakerRequester) {
                        takerProfileDisplay.getStyleClass().add("mediator-header-requester");
                    }
                    takerProfileDisplay.getTooltip().setText(Res.get("authorizedRole.mediator.hasRequested",
                            takerProfileDisplay.getTooltipText(),
                            !isMakerRequester ? Res.get("confirmation.yes") : Res.get("confirmation.no")
                    ));

                    tradeId.getSecond().setText(item.getShortTradeId());
                } else {
                    makerProfileDisplay.dispose();
                    takerProfileDisplay.dispose();
                    directionalTitle.setText(null);
                    tradeId.getSecond().setText(null);
                }
            });

            showClosedCasesPin = EasyBind.subscribe(model.getShowClosedCases(),
                    showClosedCases -> {
                        leaveButton.setVisible(showClosedCases);
                        leaveButton.setManaged(showClosedCases);
                        removeButton.setVisible(showClosedCases);
                        removeButton.setManaged(showClosedCases);

                        openCloseButton.setText(showClosedCases ?
                                Res.get("authorizedRole.mediator.reOpen") :
                                Res.get("authorizedRole.mediator.close"))
                        ;
                    });
            openCloseButton.setOnAction(e -> controller.onToggleOpenClose());
            leaveButton.setOnAction(e -> controller.onLeaveChannel());
            removeButton.setOnAction(e -> controller.onRemoveCase());
            detailsButton.setOnAction(e -> controller.onShowDetails());
        }

        @Override
        protected void onViewDetached() {
            mediationCaseListItemPin.unsubscribe();
            showClosedCasesPin.unsubscribe();
            openCloseButton.setOnAction(null);
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
