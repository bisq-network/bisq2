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

package bisq.desktop.primary.main.content.components;

import bisq.application.DefaultApplicationService;
import bisq.chat.discuss.DiscussionChannelSelectionService;
import bisq.chat.trade.TradeChannelSelectionService;
import bisq.chat.discuss.pub.PublicDiscussionChannelService;
import bisq.desktop.common.observable.FxBindings;
import bisq.i18n.Res;
import bisq.chat.ChatService;
import bisq.chat.discuss.pub.PublicDiscussionChannel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class PublicDiscussionChannelSelection extends ChannelSelection {
    private final Controller controller;

    public PublicDiscussionChannelSelection(DefaultApplicationService applicationService) {
        controller = new Controller(applicationService.getChatService());
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void deSelectChannel() {
        controller.deSelectChannel();
    }

    protected static class Controller extends ChannelSelection.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final PublicDiscussionChannelService publicDiscussionChannelService;
        private final TradeChannelSelectionService tradeChannelSelectionService;
        private final DiscussionChannelSelectionService discussionChannelSelectionService;

        protected Controller(ChatService chatService) {
            super(chatService);

            publicDiscussionChannelService = chatService.getPublicDiscussionChannelService();
            tradeChannelSelectionService = chatService.getTradeChannelSelectionService();
            discussionChannelSelectionService = chatService.getDiscussionChannelSelectionService();
            
            model = new Model();
            view = new View(model, this);
        }

        @Override
        protected ChannelSelection.Model getChannelSelectionModel() {
            return model;
        }

        @Override
        public void onActivate() {
            super.onActivate();

            channelsPin = FxBindings.<PublicDiscussionChannel, ChannelSelection.View.ChannelItem>bind(model.channelItems)
                    .map(ChannelSelection.View.ChannelItem::new)
                    .to(publicDiscussionChannelService.getChannels());

            selectedChannelPin = FxBindings.subscribe(discussionChannelSelectionService.getSelectedChannel(),
                    channel -> {
                        if (channel instanceof PublicDiscussionChannel) {
                            model.selectedChannel.set(new ChannelSelection.View.ChannelItem(channel));
                        }
                    });
        }

        @Override
        protected void onSelected(ChannelSelection.View.ChannelItem channelItem) {
            if (channelItem == null) {
                return;
            }

            discussionChannelSelectionService.selectChannel(channelItem.getChannel());
        }

        public void deSelectChannel() {
            model.selectedChannel.set(null);
        }
    }

    protected static class Model extends ChannelSelection.Model {
    }

    protected static class View extends ChannelSelection.View<Model, Controller> {
        protected View(Model model, Controller controller) {
            super(model, controller);
        }

        @Override
        protected String getHeadlineText() {
            return Res.get("social.publicChannels");
        }

        @Override
        protected ListCell<ChannelItem> getListCell() {
            return new ListCell<>() {
                private Subscription widthSubscription;
                final Label label = new Label();
                final HBox hBox = new HBox();

                {
                    setCursor(Cursor.HAND);
                    setPrefHeight(40);
                    setPadding(new Insets(0, 0, -20, 0));

                    hBox.setSpacing(10);
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    hBox.getChildren().add(label);
                }

                @Override
                protected void updateItem(ChannelItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null && !empty && item.getChannel() instanceof PublicDiscussionChannel) {
                        label.setText(item.getDisplayString());
                        widthSubscription = EasyBind.subscribe(widthProperty(), w -> {
                            if (w.doubleValue() > 0) {
                                label.setMaxWidth(getWidth() - 70);
                            }
                        });
                        setGraphic(hBox);
                    } else {
                        setGraphic(null);
                        if (widthSubscription != null) {
                            widthSubscription.unsubscribe();
                        }
                    }
                }
            };
        }

    }
}