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

package bisq.desktop.primary.main.content.social.chat.components;

import bisq.desktop.components.controls.BisqLabel;
import bisq.desktop.components.controls.BisqRadioButton;
import bisq.i18n.Res;
import bisq.social.chat.Channel;
import bisq.social.chat.ChatMessage;
import bisq.social.chat.NotificationSetting;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotificationsSettings {

    private final Controller controller;

    public NotificationsSettings() {
        controller = new Controller();
    }

    public void setChannel(Channel<? extends ChatMessage> channel) {
        controller.model.setChannel(channel);
    }

    public ReadOnlyObjectProperty<NotificationSetting> getNotificationSetting() {
        return controller.model.notificationSetting;
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;

        private Controller() {
            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onViewAttached() {
        }

        @Override
        public void onViewDetached() {
        }

        public void onSelected(NotificationSetting notificationSetting) {
            model.notificationSetting.set(notificationSetting);
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<Channel<? extends ChatMessage>> channel = new SimpleObjectProperty<>();
        private final ObjectProperty<NotificationSetting> notificationSetting = new SimpleObjectProperty<>(NotificationSetting.MENTION);

        private Model() {
        }

        private void setChannel(Channel<? extends ChatMessage> channel) {
            this.notificationSetting.set(channel.getNotificationSetting().get());

            this.channel.set(channel);
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ToggleGroup toggleGroup = new ToggleGroup();
        private final ChangeListener<Toggle> toggleListener;
        private final BisqRadioButton all, mention, none;

        private final ChangeListener<Channel<? extends ChatMessage>> channelChangeListener;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            root.setSpacing(5);
            
            BisqLabel headline = new BisqLabel(Res.get("social.channel.notifications"));
            headline.getStyleClass().add("channel-settings-headline");
            headline.setPadding(new Insets(0, 40, 0, 0));

            all = new BisqRadioButton(Res.get("social.channel.notifications.all"));
            mention = new BisqRadioButton(Res.get("social.channel.notifications.mention"));
            none = new BisqRadioButton(Res.get("social.channel.notifications.never"));

            all.setToggleGroup(toggleGroup);
            mention.setToggleGroup(toggleGroup);
            none.setToggleGroup(toggleGroup);

            all.setUserData(NotificationSetting.ALL);
            mention.setUserData(NotificationSetting.MENTION);
            none.setUserData(NotificationSetting.NEVER);

            root.getChildren().addAll(headline, all, mention, none);

            toggleListener = (observable, oldValue, newValue) -> controller.onSelected((NotificationSetting) newValue.getUserData());
            channelChangeListener = (observable, oldValue, newValue) -> update();

            update();
        }

        private void update() {
            switch (model.notificationSetting.get()) {
                case ALL -> {
                    toggleGroup.selectToggle(all);
                }
                case MENTION -> {
                    toggleGroup.selectToggle(mention);
                }
                case NEVER -> {
                    toggleGroup.selectToggle(none);
                }
            }
        }


        @Override
        public void onViewAttached() {
            model.channel.addListener(channelChangeListener);
            toggleGroup.selectedToggleProperty().addListener(toggleListener);
        }

        @Override
        protected void onViewDetached() {
            model.channel.removeListener(channelChangeListener);
            toggleGroup.selectedToggleProperty().removeListener(toggleListener);
        }
    }
}