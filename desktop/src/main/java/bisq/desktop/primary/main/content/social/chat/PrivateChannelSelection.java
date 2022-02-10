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

package bisq.desktop.primary.main.content.social.chat;

import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.components.controls.BisqLabel;
import bisq.i18n.Res;
import bisq.social.chat.Channel;
import bisq.social.chat.ChatService;
import bisq.social.chat.PrivateChannel;
import bisq.social.chat.PublicChannel;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;


@Slf4j
public class PrivateChannelSelection {
    private final Controller controller;

    public PrivateChannelSelection(ChatService chatService) {
        controller = new Controller(chatService);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final ChatService chatService;
        private Pin selectedChannelPin;
        private Pin channelsPin;

        private Controller(ChatService chatService) {
            this.chatService = chatService;

            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onViewAttached() {
            selectedChannelPin = FxBindings.subscribe(chatService.getPersistableStore().getSelectedChannel(),
                    channel -> {
                        if (channel instanceof PrivateChannel privateChannel) {
                            model.selectedChannel.set(new ListItem(privateChannel));
                        }
                    });
            channelsPin = FxBindings.<PrivateChannel, ListItem>bind(model.channels)
                    .map(ListItem::new)
                    .to(chatService.getPersistableStore().getPrivateChannels());
        }

        @Override
        public void onViewDetached() {
            selectedChannelPin.unbind();
            channelsPin.unbind();
        }

        private void onSelected(ListItem selectedItem) {
            if (selectedItem == null) return;
            chatService.selectChannel(selectedItem.channel);
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        ObjectProperty<ListItem> selectedChannel = new SimpleObjectProperty<>();
        ObservableList<ListItem> channels = FXCollections.observableArrayList();

        private Model() {
        }
    }


    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ListView<ListItem> listView;
        private Subscription subscription;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);
            root.setSpacing(10);

            BisqLabel headline = new BisqLabel(Res.get("social.privateChannels"));

            listView = new ListView<>();
            listView.setItems(model.channels);
            listView.setFocusTraversable(false);
            listView.setCellFactory(new Callback<>() {
                @Override
                public ListCell<ListItem> call(ListView<ListItem> list) {
                    return new ListCell<>() {
                        final BisqLabel label = new BisqLabel();

                        @Override
                        public void updateItem(final ListItem item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item != null && !empty) {
                                label.setText(item.channelName);
                                setGraphic(label);
                            } else {
                                setGraphic(null);
                            }
                        }
                    };
                }
            });
            root.getChildren().addAll(headline, listView);
        }

        @Override
        public void onViewAttached() {
            subscription = EasyBind.subscribe(listView.getSelectionModel().selectedItemProperty(), controller::onSelected);
        }

        @Override
        protected void onViewDetached() {
            subscription.unsubscribe();
        }
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class ListItem {
        private final PrivateChannel channel;
        private final String channelName;
        @EqualsAndHashCode.Include
        private final String id;

        private ListItem(PrivateChannel channel) {
            this.channel = channel;
            channelName = channel.getChannelName();
            id = channel.getId();
        }
    }
}