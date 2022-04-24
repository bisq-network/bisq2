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

package bisq.desktop.primary.main.content.social.components;

import bisq.application.DefaultApplicationService;
import bisq.common.data.ByteArray;
import bisq.common.util.StringUtils;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.components.robohash.RoboHash;
import bisq.i18n.Res;
import bisq.social.chat.ChatService;
import bisq.social.chat.channels.PrivateChannel;
import bisq.social.chat.channels.PrivateDiscussionChannel;
import bisq.social.chat.channels.PrivateTradeChannel;
import bisq.social.user.ChatUser;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrivateChannelSelection extends ChannelSelection {
    private final Controller controller;

    public PrivateChannelSelection(DefaultApplicationService applicationService, boolean isDiscussionsChat) {
        controller = new Controller(applicationService.getChatService(), isDiscussionsChat);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void deSelectChannel() {
        controller.deSelectChannel();
    }

    protected static class Controller extends bisq.desktop.primary.main.content.social.components.ChannelSelection.Controller {
        private final Model model;
        @Getter
        private final View view;

        protected Controller(ChatService chatService, boolean isDiscussionsChat) {
            super(chatService);

            model = new Model(isDiscussionsChat);
            view = new View(model, this);
        }

        @Override
        protected ChannelSelection.Model getChannelSelectionModel() {
            return model;
        }

        @Override
        public void onActivate() {
            super.onActivate();

            if (model.isDiscussionsChat) {
                channelsPin = FxBindings.<PrivateDiscussionChannel, ChannelSelection.View.ChannelItem>bind(model.channelItems)
                        .map(ChannelSelection.View.ChannelItem::new)
                        .to(chatService.getPrivateDiscussionChannels());

                selectedChannelPin = FxBindings.subscribe(chatService.getSelectedDiscussionChannel(),
                        channel -> {
                            if (channel instanceof PrivateDiscussionChannel) {
                                model.selectedChannel.set(new ChannelSelection.View.ChannelItem(channel));
                            }
                        });
            } else {
                channelsPin = FxBindings.<PrivateTradeChannel, ChannelSelection.View.ChannelItem>bind(model.channelItems)
                        .map(ChannelSelection.View.ChannelItem::new)
                        .to(chatService.getPrivateTradeChannels());

                selectedChannelPin = FxBindings.subscribe(chatService.getSelectedTradeChannel(),
                        channel -> {
                            if (channel instanceof PrivateTradeChannel) {
                                model.selectedChannel.set(new ChannelSelection.View.ChannelItem(channel));
                            }
                        });
            }
        }

        @Override
        protected void onSelected(ChannelSelection.View.ChannelItem channelItem) {
            if (channelItem == null) {
                return;
            }
            if (model.isDiscussionsChat) {
                chatService.selectDiscussionChannel(channelItem.getChannel());
            } else {
                chatService.selectTradeChannel(channelItem.getChannel());
            }
        }

        public void deSelectChannel() {
            model.selectedChannel.set(null);
        }
    }

    protected static class Model extends bisq.desktop.primary.main.content.social.components.ChannelSelection.Model {
        private final boolean isDiscussionsChat;

        public Model(boolean isDiscussionsChat) {
            this.isDiscussionsChat = isDiscussionsChat;
        }
    }

    protected static class View extends bisq.desktop.primary.main.content.social.components.ChannelSelection.View<Model, Controller> {
        protected View(Model model, Controller controller) {
            super(model, controller);
        }

        @Override
        protected String getHeadlineText() {
            return Res.get("social.privateChannels");
        }

        protected ListCell<ChannelItem> getListCell() {
            return new ListCell<>() {
                final Label label = new Label();
                final HBox hBox = new HBox();

                {
                    hBox.setSpacing(10);
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    setCursor(Cursor.HAND);
                    setPrefHeight(40);
                    setPadding(new Insets(0, 0, -20, 0));
                }

                @Override
                protected void updateItem(ChannelItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null && !empty && item.getChannel() instanceof PrivateChannel privateChannel) {
                        ChatUser peer = privateChannel.getPeer();
                        ImageView roboIcon = new ImageView(RoboHash.getImage(new ByteArray(peer.getPubKeyHash())));
                        roboIcon.setFitWidth(35);
                        roboIcon.setFitHeight(35);
                        hBox.getChildren().addAll(roboIcon, label);
                        String userName = peer.getUserName();
                        label.setText(StringUtils.truncate(userName, 20));
                        label.setTooltip(new Tooltip(peer.getTooltipString()));
                        setGraphic(hBox);
                    } else {
                        setGraphic(null);
                    }
                }
            };
        }
    }
}