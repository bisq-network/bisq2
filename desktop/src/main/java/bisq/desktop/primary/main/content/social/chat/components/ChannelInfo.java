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
import bisq.i18n.Res;
import bisq.social.chat.Channel;
import bisq.social.chat.PublicChannel;
import bisq.social.user.ChatUser;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class ChannelInfo {
    private final Controller controller;

    public ChannelInfo() {
        controller = new Controller();
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void setChannel(Channel channel) {
        controller.model.setChannel(channel);
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
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private ObjectProperty<Channel> channel = new SimpleObjectProperty<>();
        private String channelName;
        private Optional<String> description = Optional.empty();
        private final ObservableList<ChatUserDisplay> moderators = FXCollections.observableArrayList();
        private Optional<ChatUserDisplay> adminProfile = Optional.empty();
        private final ObservableList<ChatUserDisplay> members = FXCollections.observableArrayList();

        private Model() {
        }

        private void setChannel(Channel channel) {
            channelName = channel.getChannelName();
            members.setAll(channel.getChatMessages().stream()
                    .map(chatMessage -> new ChatUserDisplay(new ChatUser(chatMessage.getSenderNetworkId())))
                    .sorted()
                    .collect(Collectors.toList()));
            if (channel instanceof PublicChannel publicChannel) {
                description = Optional.of(publicChannel.getDescription());
                adminProfile = Optional.of(new ChatUserDisplay(publicChannel.getChannelAdmin()));
                moderators.setAll(publicChannel.getChannelModerators().stream()
                        .map(ChatUserDisplay::new)
                        .sorted()
                        .collect(Collectors.toList()));

            } else {
                description = Optional.empty();
                adminProfile = Optional.empty();
                moderators.clear();
            }

            this.channel.set(channel);
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {

        private final ChangeListener<Channel> channelChangeListener;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            root.setSpacing(5);

            channelChangeListener = (observable, oldValue, newValue) -> update();
            update();
        }

        private void update() {
            if (model.channel.get() == null) {
                return;
            }

            root.getChildren().clear();

            BisqLabel channelName = new BisqLabel(model.channelName);
            channelName.getStyleClass().add("channel-settings-headline");
            root.getChildren().add(channelName);

            model.description.ifPresent(description -> root.getChildren().add(new BisqLabel(description)));

            model.adminProfile.ifPresent(adminProfile -> {
                BisqLabel adminHeadLine = new BisqLabel(Res.get("social.channel.settings.admin"));
                adminHeadLine.setPadding(new Insets(20, 0, -10, 0));
                adminHeadLine.getStyleClass().add("accent-headline");
                root.getChildren().addAll(adminHeadLine, adminProfile.getRoot());
            });

            if (!model.moderators.isEmpty()) {
                BisqLabel moderatorsHeadLine = new BisqLabel(Res.get("social.channel.settings.moderators"));
                moderatorsHeadLine.setPadding(new Insets(20, 0, -10, 0));
                moderatorsHeadLine.getStyleClass().add("accent-headline");
                root.getChildren().add(moderatorsHeadLine);
                root.getChildren().addAll(model.moderators.stream()
                        .map(ChatUserDisplay::getRoot)
                        .collect(Collectors.toList()));
            }

            ListView<ChatUserDisplay> members = new ListView<>();
            members.setFocusTraversable(false);
            VBox.setVgrow(members, Priority.ALWAYS);
            members.setCellFactory(new Callback<>() {
                @Override
                public ListCell<ChatUserDisplay> call(ListView<ChatUserDisplay> list) {
                    return new ListCell<>() {
                        BisqLabel label = new BisqLabel();

                        @Override
                        public void updateItem(final ChatUserDisplay item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item != null && !empty) {
                                setGraphic(item.getRoot());
                            } else {
                                setGraphic(null);
                            }
                        }
                    };
                }
            });
            members.setItems(model.members);

            BisqLabel membersHeadLine = new BisqLabel(Res.get("social.channel.settings.members"));
            membersHeadLine.setPadding(new Insets(20, 0, 0, 0));
            membersHeadLine.getStyleClass().add("accent-headline");
            root.getChildren().addAll(membersHeadLine, members);
        }

        @Override
        public void onViewAttached() {
            model.channel.addListener(channelChangeListener);
        }

        @Override
        protected void onViewDetached() {
            model.channel.removeListener(channelChangeListener);
        }
    }
}