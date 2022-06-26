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

package bisq.desktop.primary.main.content.settings.userProfile.old.components;

import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.i18n.Res;
import bisq.social.chat.ChatService;
import bisq.social.user.ChatUserIdentity;
import bisq.social.user.ChatUserService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChannelAdmin {

    private final Controller controller;

    public ChannelAdmin(ChatUserService chatUserService, ChatService chatService) {
        controller = new Controller(chatUserService, chatService);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final ChatUserService chatUserService;
        private final ChatService chatService;
        private Pin selectedUserProfilePin;

        private Controller(ChatUserService chatUserService, ChatService chatService) {
            this.chatUserService = chatUserService;
            this.chatService = chatService;

            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            selectedUserProfilePin = FxBindings.bind(model.selectedUserProfile)
                    .to(chatUserService.getSelectedChatUserIdentity());
        }

        @Override
        public void onDeactivate() {
            selectedUserProfilePin.unbind();
        }

        public void onSelected(ChatUserIdentity value) {
            if (value != null) {
                chatUserService.selectUserProfile(value);
            }
        }

        public void addChannel() {
           /* chatService.addPublicDiscussionChannel(model.selectedUserProfile.get(), model.channelName.get(), model.description.get())
                    .whenComplete((publicChannel, throwable) -> {
                        if (throwable == null) {
                            if (publicChannel.isPresent()) {
                                log.info("Added publicChannel {}", publicChannel);
                            } else {
                                log.error("AddChannel failed");
                            }
                        } else {
                            log.error("Error at addChannel", throwable);
                        }
                    });*/
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        final StringProperty channelName = new SimpleStringProperty();
        final StringProperty description = new SimpleStringProperty();
        ObjectProperty<ChatUserIdentity> selectedUserProfile = new SimpleObjectProperty<>();

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final Button addChannelButton;
        private final TextField channelNameField;
        private final TextField descriptionField;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);
            root.setSpacing(10);

            Label headline = new Label(Res.get("social.channelAdmin.headline"));
            headline.getStyleClass().add("titled-group-bg-label-active");

            channelNameField = new TextField();
            descriptionField = new TextField();
            addChannelButton = new Button(Res.get("social.channelAdmin.addChannel"));

            root.getChildren().addAll(headline, channelNameField, descriptionField, addChannelButton);
        }

        @Override
        protected void onViewAttached() {
            addChannelButton.setOnAction(e -> controller.addChannel());
            channelNameField.textProperty().bindBidirectional(model.channelName);
            descriptionField.textProperty().bindBidirectional(model.description);
        }

        @Override
        protected void onViewDetached() {
            channelNameField.textProperty().unbindBidirectional(model.channelName);
            descriptionField.textProperty().unbindBidirectional(model.description);
        }
    }
}