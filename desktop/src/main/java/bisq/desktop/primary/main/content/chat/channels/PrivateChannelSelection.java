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
import bisq.chat.ChannelKind;
import bisq.chat.ChatService;
import bisq.chat.channel.PrivateChannel;
import bisq.chat.discuss.DiscussionChannelSelectionService;
import bisq.chat.discuss.priv.PrivateDiscussionChannel;
import bisq.chat.discuss.priv.PrivateDiscussionChannelService;
import bisq.chat.events.EventsChannelSelectionService;
import bisq.chat.events.priv.PrivateEventsChannel;
import bisq.chat.events.priv.PrivateEventsChannelService;
import bisq.chat.support.SupportChannelSelectionService;
import bisq.chat.support.priv.PrivateSupportChannel;
import bisq.chat.support.priv.PrivateSupportChannelService;
import bisq.chat.trade.TradeChannelSelectionService;
import bisq.chat.trade.priv.PrivateTradeChannel;
import bisq.chat.trade.priv.PrivateTradeChannelService;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.robohash.RoboHash;
import bisq.i18n.Res;
import bisq.user.profile.UserProfile;
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
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class PrivateChannelSelection extends ChannelSelection {
    private final Controller controller;

    public PrivateChannelSelection(DefaultApplicationService applicationService, ChannelKind channelKind) {
        controller = new Controller(applicationService.getChatService(), channelKind);
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
        private final PrivateTradeChannelService privateTradeChannelService;
        private final TradeChannelSelectionService tradeChannelSelectionService;
        private final PrivateDiscussionChannelService privateDiscussionChannelService;
        private final DiscussionChannelSelectionService discussionChannelSelectionService;
        private final PrivateEventsChannelService privateEventsChannelService;
        private final PrivateSupportChannelService privateSupportChannelService;
        private final EventsChannelSelectionService eventsChannelSelectionService;
        private final SupportChannelSelectionService supportChannelSelectionService;

        protected Controller(ChatService chatService, ChannelKind channelKind) {
            super(chatService);

            privateTradeChannelService = chatService.getPrivateTradeChannelService();
            tradeChannelSelectionService = chatService.getTradeChannelSelectionService();

            privateDiscussionChannelService = chatService.getPrivateDiscussionChannelService();
            discussionChannelSelectionService = chatService.getDiscussionChannelSelectionService();

            privateEventsChannelService = chatService.getPrivateEventsChannelService();
            eventsChannelSelectionService = chatService.getEventsChannelSelectionService();

            privateSupportChannelService = chatService.getPrivateSupportChannelService();
            supportChannelSelectionService = chatService.getSupportChannelSelectionService();

            model = new Model(channelKind);
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

            if (model.channelKind == ChannelKind.TRADE) {
                channelsPin = FxBindings.<PrivateTradeChannel, ChannelSelection.View.ChannelItem>bind(model.channelItems)
                        .map(ChannelSelection.View.ChannelItem::new)
                        .to(privateTradeChannelService.getChannels());

                selectedChannelPin = FxBindings.subscribe(tradeChannelSelectionService.getSelectedChannel(),
                        channel -> {
                            if (channel instanceof PrivateTradeChannel) {
                                model.selectedChannelItem.set(new ChannelSelection.View.ChannelItem(channel));
                            }
                        });
            } else if (model.channelKind == ChannelKind.DISCUSSION) {
                channelsPin = FxBindings.<PrivateDiscussionChannel, ChannelSelection.View.ChannelItem>bind(model.channelItems)
                        .map(ChannelSelection.View.ChannelItem::new)
                        .to(privateDiscussionChannelService.getChannels());

                selectedChannelPin = FxBindings.subscribe(discussionChannelSelectionService.getSelectedChannel(),
                        channel -> {
                            if (channel instanceof PrivateDiscussionChannel) {
                                model.selectedChannelItem.set(new ChannelSelection.View.ChannelItem(channel));
                            }
                        });
            } else if (model.channelKind == ChannelKind.EVENTS) {
                channelsPin = FxBindings.<PrivateEventsChannel, ChannelSelection.View.ChannelItem>bind(model.channelItems)
                        .map(ChannelSelection.View.ChannelItem::new)
                        .to(privateEventsChannelService.getChannels());

                selectedChannelPin = FxBindings.subscribe(eventsChannelSelectionService.getSelectedChannel(),
                        channel -> {
                            if (channel instanceof PrivateEventsChannel) {
                                model.selectedChannelItem.set(new ChannelSelection.View.ChannelItem(channel));
                            }
                        });
            } else if (model.channelKind == ChannelKind.SUPPORT) {
                channelsPin = FxBindings.<PrivateSupportChannel, ChannelSelection.View.ChannelItem>bind(model.channelItems)
                        .map(ChannelSelection.View.ChannelItem::new)
                        .to(privateSupportChannelService.getChannels());

                selectedChannelPin = FxBindings.subscribe(supportChannelSelectionService.getSelectedChannel(),
                        channel -> {
                            if (channel instanceof PrivateSupportChannel) {
                                model.selectedChannelItem.set(new ChannelSelection.View.ChannelItem(channel));
                            }
                        });
            }
        }

        @Override
        protected void onSelected(ChannelSelection.View.ChannelItem channelItem) {
            if (channelItem == null) {
                return;
            }
            if (model.channelKind == ChannelKind.TRADE) {
                tradeChannelSelectionService.selectChannel(channelItem.getChannel());
            } else if (model.channelKind == ChannelKind.DISCUSSION) {
                discussionChannelSelectionService.selectChannel(channelItem.getChannel());
            } else if (model.channelKind == ChannelKind.EVENTS) {
                eventsChannelSelectionService.selectChannel(channelItem.getChannel());
            } else if (model.channelKind == ChannelKind.SUPPORT) {
                supportChannelSelectionService.selectChannel(channelItem.getChannel());
            }
        }

        public void deSelectChannel() {
            model.selectedChannelItem.set(null);
        }
    }

    protected static class Model extends ChannelSelection.Model {
        private final ChannelKind channelKind;

        public Model(ChannelKind channelKind) {
            this.channelKind = channelKind;
        }
    }

    protected static class View extends ChannelSelection.View<Model, Controller> {
        protected View(Model model, Controller controller) {
            super(model, controller);
        }

        @Override
        protected String getHeadlineText() {
            return Res.get("social.privateChannels");
        }

        protected ListCell<ChannelItem> getListCell() {
            return new ListCell<>() {
                private Subscription widthSubscription;
                final Label label = new Label();
                final HBox hBox = new HBox();
                final Tooltip tooltip = new BisqTooltip();
                final ImageView roboIcon = new ImageView();

                {
                    setCursor(Cursor.HAND);
                    setPrefHeight(40);
                    setPadding(new Insets(0, 0, -20, 0));

                    label.setTooltip(tooltip);

                    roboIcon.setFitWidth(35);
                    roboIcon.setFitHeight(35);

                    hBox.setSpacing(10);
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    hBox.getChildren().addAll(roboIcon, label);
                }

                @Override
                protected void updateItem(ChannelItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null && !empty && item.getChannel() instanceof PrivateChannel) {
                        UserProfile peer = ((PrivateChannel<?>) item.getChannel()).getPeer();
                        roboIcon.setImage(RoboHash.getImage(peer.getPubKeyHash()));
                        tooltip.setText(peer.getTooltipString());
                        label.setText(item.getChannel().getDisplayString());
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