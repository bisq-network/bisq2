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

package bisq.desktop.primary.main.content.chat.sidebar;

import bisq.chat.channel.Channel;
import bisq.chat.channel.ChannelNotificationType;
import bisq.chat.message.ChatMessage;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.i18n.Res;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotificationsSidebar {

    private final Controller controller;

    public NotificationsSidebar(Runnable closeHandler) {
        controller = new Controller(closeHandler);
    }

    public void setChannel(Channel<? extends ChatMessage> channel) {
        controller.model.setChannel(channel);
    }

    public ReadOnlyObjectProperty<ChannelNotificationType> getNotificationSetting() {
        return controller.model.notificationSetting;
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final Runnable closeHandler;

        private Controller(Runnable closeHandler) {
            this.closeHandler = closeHandler;
            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
        }

        void onSelected(ChannelNotificationType channelNotificationType) {
            model.notificationSetting.set(channelNotificationType);
        }

        void onClose() {
            closeHandler.run();
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<Channel<? extends ChatMessage>> channel = new SimpleObjectProperty<>();
        private final ObjectProperty<ChannelNotificationType> notificationSetting = new SimpleObjectProperty<>(ChannelNotificationType.MENTION);

        private Model() {
        }

        private void setChannel(Channel<? extends ChatMessage> channel) {
            this.notificationSetting.set(channel.getChannelNotificationType().get());

            this.channel.set(channel);
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ToggleGroup toggleGroup = new ToggleGroup();
        private final ChangeListener<Toggle> toggleListener;
        private final RadioButton all, mention, none;
        private final Button closeButton;

        private final ChangeListener<Channel<? extends ChatMessage>> channelChangeListener;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            root.setSpacing(15);
            root.setMinWidth(260);
            root.setPadding(new Insets(0, 20, 20, 20));

            Label headline = new Label(Res.get("social.channel.notifications"));
            headline.setId("chat-sidebar-headline");

            closeButton = BisqIconButton.createIconButton("close");
            HBox.setMargin(headline, new Insets(18, 0, 0, 0));
            HBox.setMargin(closeButton, new Insets(10, 10, 0, 0));
            HBox topHBox = new HBox(headline, Spacer.fillHBox(), closeButton);

            all = new RadioButton(Res.get("social.channel.notifications.all"));
            mention = new RadioButton(Res.get("social.channel.notifications.mention"));
            none = new RadioButton(Res.get("social.channel.notifications.never"));

            all.setToggleGroup(toggleGroup);
            mention.setToggleGroup(toggleGroup);
            none.setToggleGroup(toggleGroup);

            all.setUserData(ChannelNotificationType.ALL);
            mention.setUserData(ChannelNotificationType.MENTION);
            none.setUserData(ChannelNotificationType.NEVER);
           
            VBox.setMargin(topHBox, new Insets(0, -20, 20, 0));
            root.getChildren().addAll(topHBox, all, mention, none);

            toggleListener = (observable, oldValue, newValue) -> controller.onSelected((ChannelNotificationType) newValue.getUserData());
            channelChangeListener = (observable, oldValue, newValue) -> update();

            update();
        }

        private void update() {
            switch (model.notificationSetting.get()) {
                case ALL: {
                    toggleGroup.selectToggle(all);
                    break;
                }
                case MENTION: {
                    toggleGroup.selectToggle(mention);
                    break;
                }
                case NEVER: {
                    toggleGroup.selectToggle(none);
                    break;
                }
            }
        }


        @Override
        protected void onViewAttached() {
            model.channel.addListener(channelChangeListener);
            toggleGroup.selectedToggleProperty().addListener(toggleListener);
            closeButton.setOnAction(e -> controller.onClose());
        }

        @Override
        protected void onViewDetached() {
            model.channel.removeListener(channelChangeListener);
            toggleGroup.selectedToggleProperty().removeListener(toggleListener);
            closeButton.setOnAction(null);
        }
    }
}