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

package bisq.desktop.primary.main.content.chat.channels;

import bisq.application.DefaultApplicationService;
import bisq.chat.ChatService;
import bisq.chat.channel.ChannelSelectionService;
import bisq.chat.channel.PublicChannel;
import bisq.chat.channel.PublicChannelService;
import bisq.chat.trade.TradeChannelSelectionService;
import bisq.desktop.common.observable.FxBindings;
import bisq.i18n.Res;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
        private final PublicChannelService publicDiscussionChannelService;
        private final TradeChannelSelectionService tradeChannelSelectionService;
        private final ChannelSelectionService discussionChannelSelectionService;

        protected Controller(ChatService chatService) {
            super(chatService);

            publicDiscussionChannelService = chatService.getPublicDiscussionChannelService();
            tradeChannelSelectionService = chatService.getTradeChannelSelectionService();
            discussionChannelSelectionService = chatService.getDiscussionChannelSelectionService();

            model = new Model();
            view = new View(model, this);

            model.filteredList.setPredicate(item -> true);
        }

        @Override
        protected ChannelSelection.Model getChannelSelectionModel() {
            return model;
        }

        @Override
        public void onActivate() {
            super.onActivate();

            channelsPin = FxBindings.<PublicChannel, ChannelSelection.View.ChannelItem>bind(model.channelItems)
                    .map(ChannelSelection.View.ChannelItem::new)
                    .to(publicDiscussionChannelService.getChannels());

            selectedChannelPin = FxBindings.subscribe(discussionChannelSelectionService.getSelectedChannel(),
                    channel -> {
                        if (channel instanceof PublicChannel) {
                            model.selectedChannelItem.set(new ChannelSelection.View.ChannelItem(channel));
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
            model.selectedChannelItem.set(null);
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
                final ImageView iconImageView = new ImageView();
                final Label label = new Label();
                final HBox hBox = new HBox();

                {
                    initCell(this, label, iconImageView, hBox);
                }

                @Override
                protected void updateItem(ChannelItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null && !empty && item.getChannel() instanceof PublicChannel) {
                        widthSubscription = setupCellBinding(this, item, label, iconImageView);
                        updateCell(this, item, label, iconImageView);

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