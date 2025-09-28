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
import bisq.desktop.components.controls.validator.PercentageValidator;
import bisq.desktop.components.controls.validator.TextMaxLengthValidator;
import bisq.desktop.main.content.user.profile_card.ProfileCardController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import bisq.user.contact_list.ContactListEntry;
import bisq.user.contact_list.ContactListService;
import bisq.user.contact_list.ContactReason;
import bisq.user.profile.UserProfile;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
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
            model.getTag().set("");
            model.getTrustScore().set("");
            model.getNotes().set("");
        }

        void onAddToContacts() {
            Optional<String> tag = model.getTag().get().isBlank()
                    ? Optional.empty()
                    : Optional.of(model.getTag().get().trim());
            Optional<String> notes = model.getNotes().get().isBlank()
                    ? Optional.empty()
                    : Optional.of(model.getNotes().get());
            Optional<Double> trustScore = getTrustScore(model.getTrustScore().get());
            contactListService.addContactListEntry(new ContactListEntry(model.getUserProfile(),
                    System.currentTimeMillis(),
                    ContactReason.MANUALLY_ADDED,
                    trustScore,
                    tag,
                    notes));
            OverlayController.hide(() ->
                    Navigation.navigateTo(NavigationTarget.PROFILE_CARD_MY_NOTES,
                            new ProfileCardController.InitData(model.getUserProfile())));
        }

        void onCancel() {
            OverlayController.hide();
        }

        private Optional<Double> getTrustScore(String trustScoreText) {
            String trimmed = trustScoreText.trim().replace("%", "");
            try {
                double percent = Double.parseDouble(trimmed);
                double trustScore = percent / 100.0;
                return Optional.of(trustScore);
            } catch (NumberFormatException ignored) {
            }
            return Optional.empty();
        }
    }

    @Slf4j
    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        public final StringProperty tag = new SimpleStringProperty("");
        public final StringProperty trustScore = new SimpleStringProperty("");
        public final StringProperty notes = new SimpleStringProperty("");
        public final BooleanProperty isAddToContactsButtonDisabled = new SimpleBooleanProperty();
        @Setter
        private UserProfile userProfile;
    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private static final TextMaxLengthValidator TAG_MAX_LENGTH_VALIDATOR =
                new TextMaxLengthValidator(Res.get("user.profileCard.myNotes.transparentTextField.tag.maxLength",
                        ContactListService.CONTACT_LIST_ENTRY_MAX_TAG_LENGTH),
                        ContactListService.CONTACT_LIST_ENTRY_MAX_TAG_LENGTH);
        private static final PercentageValidator TRUST_SCORE_RANGE_VALIDATOR =
                new PercentageValidator(Res.get("user.profileCard.myNotes.transparentTextField.trustScore.range",
                        ContactListService.CONTACT_LIST_ENTRY_MIN_TRUST_SCORE * 100,
                        ContactListService.CONTACT_LIST_ENTRY_MAX_TRUST_SCORE * 100),
                        ContactListService.CONTACT_LIST_ENTRY_MIN_TRUST_SCORE,
                        ContactListService.CONTACT_LIST_ENTRY_MAX_TRUST_SCORE);
        private static final TextMaxLengthValidator NOTES_MAX_LENGTH_VALIDATOR =
                new TextMaxLengthValidator(Res.get("user.profileCard.myNotes.transparentTextField.notes.maxLength",
                        ContactListService.CONTACT_LIST_ENTRY_MAX_NOTES_LENGTH),
                        ContactListService.CONTACT_LIST_ENTRY_MAX_NOTES_LENGTH);

        private final Button cancelButton, addToContactsButton;
        private final MaterialTextField tagTextField, trustScoreTextField;
        private final MaterialTextArea notesTextArea;
        private final ChangeListener<Boolean> textFieldsValidityListener;

        private View(Model model, Controller controller) {
            super(new VBox(30), model, controller);

            root.setAlignment(Pos.TOP_LEFT);
            root.setPadding(new Insets(0, 50, 0, 50));
            root.setPrefWidth(OverlayModel.WIDTH);
            root.setPrefHeight(OverlayModel.HEIGHT + 50);

            Label headline = new Label(Res.get("user.addToContactsList.popup.title"));
            headline.getStyleClass().add("bisq-text-headline-2");
            HBox headlineBox = new HBox(Spacer.fillHBox(), headline, Spacer.fillHBox());

            Label info = new Label(Res.get("user.addToContactsList.popup.info"));
            info.getStyleClass().addAll("bisq-text-3", "wrap-text");
            info.setMinHeight(45);

            tagTextField = new MaterialTextField(
                    Res.get("user.addToContactsList.popup.tag.description").toUpperCase(Locale.ROOT),
                    Res.get("user.addToContactsList.popup.tag.prompt"));
            tagTextField.showEditIcon();
            tagTextField.setValidator(TAG_MAX_LENGTH_VALIDATOR);
            trustScoreTextField = new MaterialTextField(
                    Res.get("user.addToContactsList.popup.trustScore.description").toUpperCase(Locale.ROOT),
                    Res.get("user.addToContactsList.popup.trustScore.prompt"));
            trustScoreTextField.showEditIcon();
            trustScoreTextField.setValidator(TRUST_SCORE_RANGE_VALIDATOR);
            notesTextArea = new MaterialTextArea(
                    Res.get("user.addToContactsList.popup.notes.description").toUpperCase(Locale.ROOT),
                    Res.get("user.addToContactsList.popup.notes.prompt"));
            notesTextArea.showEditIcon();
            notesTextArea.setValidator(NOTES_MAX_LENGTH_VALIDATOR);
            VBox inputFieldsBox = new VBox(15, tagTextField, trustScoreTextField, notesTextArea);

            cancelButton = new Button(Res.get("action.cancel"));
            addToContactsButton = new Button(Res.get("user.addToContactsList.popup.addToContactsButton"));
            addToContactsButton.setDefaultButton(true);
            HBox buttons = new HBox(20, cancelButton, addToContactsButton);
            buttons.setAlignment(Pos.CENTER);

            root.getChildren().setAll(Spacer.fillVBox(), headlineBox, info, inputFieldsBox, buttons, Spacer.fillVBox());
            root.getStyleClass().add("add-to-contacts-list-window");

            textFieldsValidityListener = (obs, oldVal, newVal) -> updateIsAddToContactsButtonDisabled();
        }

        @Override
        protected void onViewAttached() {
            resetValidations();

            tagTextField.textProperty().bindBidirectional(model.getTag());
            trustScoreTextField.textProperty().bindBidirectional(model.getTrustScore());
            notesTextArea.textProperty().bindBidirectional(model.getNotes());
            addToContactsButton.disableProperty().bind(model.getIsAddToContactsButtonDisabled());

            tagTextField.isValidProperty().addListener(textFieldsValidityListener);
            trustScoreTextField.isValidProperty().addListener(textFieldsValidityListener);
            notesTextArea.isValidProperty().addListener(textFieldsValidityListener);
            updateIsAddToContactsButtonDisabled();

            addToContactsButton.setOnAction(e -> controller.onAddToContacts());
            cancelButton.setOnAction(e -> controller.onCancel());
        }

        @Override
        protected void onViewDetached() {
            resetValidations();

            tagTextField.textProperty().unbindBidirectional(model.getTag());
            trustScoreTextField.textProperty().unbindBidirectional(model.getTrustScore());
            notesTextArea.textProperty().unbindBidirectional(model.getNotes());
            addToContactsButton.disableProperty().unbind();

            tagTextField.isValidProperty().removeListener(textFieldsValidityListener);
            trustScoreTextField.isValidProperty().removeListener(textFieldsValidityListener);
            notesTextArea.isValidProperty().removeListener(textFieldsValidityListener);

            addToContactsButton.setOnAction(null);
            cancelButton.setOnAction(null);
        }

        private void updateIsAddToContactsButtonDisabled() {
            boolean disabled = !(tagTextField.isValidProperty().get()
                    && trustScoreTextField.isValidProperty().get()
                    && notesTextArea.isValidProperty().get());
            model.getIsAddToContactsButtonDisabled().set(disabled);
        }

        private void resetValidations() {
            tagTextField.validate();
            trustScoreTextField.validate();
            notesTextArea.validate();
        }
    }
}
