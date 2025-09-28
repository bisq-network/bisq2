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

package bisq.desktop.main.content.components;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.user.profile_card.ProfileCardController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import bisq.support.moderator.ReportToModeratorMessage;
import bisq.user.contact_list.ContactListEntry;
import bisq.user.contact_list.ContactListService;
import bisq.user.contact_list.ContactReason;
import bisq.user.profile.UserProfile;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.Optional;

public class AddToContactsListWindow {
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final UserProfile userProfile;

        public InitData(UserProfile userProfile) {
            this.userProfile = userProfile;
        }
    }

    @Getter
    private final Controller controller;

    public AddToContactsListWindow(ServiceProvider serviceProvider) {
        controller = new Controller(serviceProvider);
    }

    @Slf4j
    private static class Controller implements InitWithDataController<InitData> {
        @Getter
        private final View view;
        private final Model model;
        private final ContactListService contactListService;

        private Controller(ServiceProvider serviceProvider) {
            contactListService = serviceProvider.getUserService().getContactListService();
            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void initWithData(AddToContactsListWindow.InitData initData) {
            model.setUserProfile(initData.getUserProfile());
        }

        @Override
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
        }

        void onAddToContacts() {
            String notes = model.getNotes().get();
            if (notes.length() > ReportToModeratorMessage.MAX_MESSAGE_LENGTH) {
                new Popup().warning(Res.get("validation.tooLong", ReportToModeratorMessage.MAX_MESSAGE_LENGTH)).show();
                return;
            }
            // todo check the rest of the properties

            // trim and check for empty fields
            contactListService.addContactListEntry(new ContactListEntry(model.getUserProfile(),
                    System.currentTimeMillis(),
                    ContactReason.MANUALLY_ADDED,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()));
            OverlayController.hide(() ->
                    Navigation.navigateTo(NavigationTarget.PROFILE_CARD_MY_NOTES,
                            new ProfileCardController.InitData(model.getUserProfile())));
        }

        void onCancel() {
            OverlayController.hide();
        }
    }

    @Slf4j
    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        public final StringProperty tag = new SimpleStringProperty("");
        public final StringProperty trustScore = new SimpleStringProperty("");
        public final StringProperty notes = new SimpleStringProperty("");
        @Setter
        private UserProfile userProfile;
    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final Button cancelButton, addToContactsButton;
        private final MaterialTextField tag, trustScore;
        private final MaterialTextArea notes;

        private View(Model model, Controller controller) {
            super(new VBox(30), model, controller);

            root.setAlignment(Pos.TOP_LEFT);
            root.setPadding(new Insets(0, 50, 0, 50));
            root.setPrefWidth(OverlayModel.WIDTH);
            root.setPrefHeight(OverlayModel.HEIGHT);

            Label headline = new Label(Res.get("user.addToContactsList.popup.title"));
            headline.getStyleClass().addAll("bisq-text-headline-2");
            HBox headlineBox = new HBox(Spacer.fillHBox(), headline, Spacer.fillHBox());

            Label info = new Label(Res.get("user.addToContactsList.popup.info"));
            info.setWrapText(true);
            info.getStyleClass().addAll("bisq-text-3");
            info.setMinHeight(45);

            tag = new MaterialTextField(
                    Res.get("user.addToContactsList.popup.tag.description").toUpperCase(Locale.ROOT),
                    Res.get("user.addToContactsList.popup.tag.prompt"));
            tag.showEditIcon();
            trustScore = new MaterialTextField(
                    Res.get("user.addToContactsList.popup.trustScore.description").toUpperCase(Locale.ROOT),
                    Res.get("user.addToContactsList.popup.trustScore.prompt"));
            trustScore.showEditIcon();
            notes = new MaterialTextArea(
                    Res.get("user.addToContactsList.popup.notes.description").toUpperCase(Locale.ROOT),
                    Res.get("user.addToContactsList.popup.notes.prompt"));
            notes.showEditIcon();
            VBox inputFieldsBox = new VBox(15, tag, trustScore, notes);

            cancelButton = new Button(Res.get("action.cancel"));
            addToContactsButton = new Button(Res.get("user.addToContactsList.popup.addToContactsButton"));
            addToContactsButton.setDefaultButton(true);
            HBox buttons = new HBox(20, cancelButton, addToContactsButton);
            buttons.setAlignment(Pos.CENTER);

            root.getChildren().setAll(Spacer.fillVBox(), headlineBox, info, inputFieldsBox, buttons, Spacer.fillVBox());
        }

        @Override
        protected void onViewAttached() {
            tag.textProperty().bindBidirectional(model.getTag());
            trustScore.textProperty().bindBidirectional(model.getTrustScore());
            notes.textProperty().bindBidirectional(model.getNotes());

            addToContactsButton.setOnAction(e -> controller.onAddToContacts());
            cancelButton.setOnAction(e -> controller.onCancel());
        }

        @Override
        protected void onViewDetached() {
            tag.textProperty().unbindBidirectional(model.getTag());
            trustScore.textProperty().unbindBidirectional(model.getTrustScore());
            notes.textProperty().unbindBidirectional(model.getNotes());

            addToContactsButton.setOnAction(null);
            cancelButton.setOnAction(null);
        }
    }
}
