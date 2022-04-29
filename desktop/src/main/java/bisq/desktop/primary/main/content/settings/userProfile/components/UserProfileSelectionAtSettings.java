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

import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.i18n.Res;
import bisq.social.user.profile.ChatUserIdentity;
import bisq.social.user.profile.UserProfileService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

//todo replace with UserProfileSelection
@Slf4j
public class UserProfileSelectionAtSettings {

    private final Controller controller;

    public UserProfileSelectionAtSettings(UserProfileService userProfileService) {
        controller = new Controller(userProfileService);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public ReadOnlyObjectProperty<ChatUserIdentity> getSelectedUserProfile() {
        return controller.model.selectedUserProfile;
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final UserProfileService userProfileService;
        private Pin userProfilesPin, selectedUserProfilePin;

        private Controller(UserProfileService userProfileService) {
            this.userProfileService = userProfileService;

            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            userProfilesPin = FxBindings.<ChatUserIdentity, ChatUserIdentity>bind(model.chatUserIdentities).to(userProfileService.getUserProfiles());
            selectedUserProfilePin = FxBindings.bind(model.selectedUserProfile).to(userProfileService.getSelectedUserProfile());
        }

        @Override
        public void onDeactivate() {
            userProfilesPin.unbind();
            selectedUserProfilePin.unbind();
        }

        public void onSelected(ChatUserIdentity value) {
            if (value != null) {
                userProfileService.selectUserProfile(value);
            }
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        ObjectProperty<ChatUserIdentity> selectedUserProfile = new SimpleObjectProperty<>();
        ObservableList<ChatUserIdentity> chatUserIdentities = FXCollections.observableArrayList();

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final AutoCompleteComboBox<ChatUserIdentity> comboBox;
        private Subscription subscription;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);
            root.setSpacing(10);

            Label headline = new Label(Res.get("social.userProfileSelection.headline"));
            headline.getStyleClass().add("titled-group-bg-label-active");

            comboBox = new AutoCompleteComboBox<>(model.chatUserIdentities);
            comboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(ChatUserIdentity chatUserIdentity) {
                    return chatUserIdentity != null ? chatUserIdentity.getChatUser().getUserName() : "";
                }

                @Override
                public ChatUserIdentity fromString(String string) {
                    return null;
                }
            });

            root.getChildren().addAll(headline, comboBox);
        }

        @Override
        protected void onViewAttached() {
            comboBox.setOnAction(e -> controller.onSelected(comboBox.getSelectionModel().getSelectedItem()));
            subscription = EasyBind.subscribe(model.selectedUserProfile,
                    selected -> UIThread.runOnNextRenderFrame(() -> comboBox.getSelectionModel().select(selected)));
        }

        @Override
        protected void onViewDetached() {
            comboBox.setOnAction(null);
            subscription.unsubscribe();
        }
    }
}