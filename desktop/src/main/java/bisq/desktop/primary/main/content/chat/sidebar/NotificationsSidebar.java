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
import bisq.i18n.Res;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotificationsSidebar {

    private final Controller controller;

    public NotificationsSidebar() {
        controller = new Controller();
    }

    public void setChannel(Channel<? extends ChatMessage> channel) {
        controller.model.setChannel(channel);
    }

    public ReadOnlyObjectProperty<ChannelNotificationType> getSelected() {
        return controller.model.selected;
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
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
        }

        void onSelected(ChannelNotificationType type) {
            model.selected.set(type);
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<Channel<? extends ChatMessage>> channel = new SimpleObjectProperty<>();
        private final ObjectProperty<ChannelNotificationType> selected = new SimpleObjectProperty<>(ChannelNotificationType.MENTION);

        private Model() {
        }

        private void setChannel(Channel<? extends ChatMessage> channel) {
            this.selected.set(channel.getChannelNotificationType().get());

            this.channel.set(channel);
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ToggleGroup toggleGroup = new ToggleGroup();
        private final ChangeListener<Toggle> toggleListener;
        private final RadioButton all, mention, none;

        private final ChangeListener<Channel<? extends ChatMessage>> channelChangeListener;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            root.setSpacing(10);

            Label headline = new Label(Res.get("social.channel.notifications"));
            headline.setId("chat-sidebar-title");

            all = new RadioButton(Res.get("social.channel.notifications.all"));
            mention = new RadioButton(Res.get("social.channel.notifications.mention"));
            none = new RadioButton(Res.get("social.channel.notifications.never"));

            all.setToggleGroup(toggleGroup);
            mention.setToggleGroup(toggleGroup);
            none.setToggleGroup(toggleGroup);

            all.setUserData(ChannelNotificationType.ALL);
            mention.setUserData(ChannelNotificationType.MENTION);
            none.setUserData(ChannelNotificationType.NEVER);

            VBox vBox = new VBox(10, all, mention, none);
            vBox.setPadding(new Insets(10));
            vBox.getStyleClass().add("bisq-dark-bg");

            root.getChildren().addAll(headline, vBox);

            toggleListener = (observable, oldValue, newValue) -> controller.onSelected((ChannelNotificationType) newValue.getUserData());
            channelChangeListener = (observable, oldValue, newValue) -> update();

            update();
        }

        private void update() {
            switch (model.selected.get()) {
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
        }

        @Override
        protected void onViewDetached() {
            model.channel.removeListener(channelChangeListener);
            toggleGroup.selectedToggleProperty().removeListener(toggleListener);
        }
    }
}