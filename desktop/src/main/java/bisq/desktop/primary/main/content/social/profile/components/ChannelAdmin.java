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

import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqLabel;
import bisq.desktop.components.controls.BisqTextField;
import bisq.i18n.Res;
import bisq.social.chat.ChatService;
import bisq.social.user.UserProfile;
import bisq.social.user.UserProfileService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class ChannelAdmin {

    private final Controller controller;

    public ChannelAdmin(UserProfileService userProfileService, ChatService chatService) {
        controller = new Controller(userProfileService, chatService);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final UserProfileService userProfileService;
        private final ChatService chatService;
        private Pin selectedUserProfilePin;

        private Controller(UserProfileService userProfileService, ChatService chatService) {
            this.userProfileService = userProfileService;
            this.chatService = chatService;

            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onViewAttached() {
            selectedUserProfilePin = FxBindings.bind(model.selectedUserProfile)
                    .to(userProfileService.getPersistableStore().getSelectedUserProfile());
        }

        @Override
        public void onViewDetached() {
            selectedUserProfilePin.unbind();
        }

        public void onSelected(UserProfile value) {
            if (value != null) {
                userProfileService.selectUserProfile(value);
            }
        }

        public void addChannel() {
            chatService.addChannel(model.selectedUserProfile.get(), model.channelName.get())
                    .whenComplete((publicChannel, throwable) -> {
                        if (throwable == null && publicChannel.isPresent()) {
                            log.error("publicChannel " + publicChannel);
                        }
                    });
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        final StringProperty channelName = new SimpleStringProperty();
        ObjectProperty<UserProfile> selectedUserProfile = new SimpleObjectProperty<>();

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final BisqButton addChannelButton;
        private final BisqTextField channelNameField;
        private Subscription subscription;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);
            root.setSpacing(10);

            Label headline = new BisqLabel(Res.get("social.channelAdmin.headline"));
            headline.getStyleClass().add("titled-group-bg-label-active");

            channelNameField = new BisqTextField();
            addChannelButton = new BisqButton(Res.get("social.channelAdmin.addChannel"));

            root.getChildren().addAll(headline, channelNameField, addChannelButton);
        }

        @Override
        public void onViewAttached() {
            addChannelButton.setOnAction(e -> controller.addChannel());
            channelNameField.textProperty().bindBidirectional(model.channelName);
        }

        @Override
        protected void onViewDetached() {
            channelNameField.textProperty().unbindBidirectional(model.channelName);
        }
    }
}