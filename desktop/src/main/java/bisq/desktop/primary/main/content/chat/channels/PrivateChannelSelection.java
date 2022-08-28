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
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.robohash.RoboHash;
import bisq.i18n.Res;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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

import javax.annotation.Nullable;

@Slf4j
public class PrivateChannelSelection extends ChannelSelection {
    private final Controller controller;

    public PrivateChannelSelection(DefaultApplicationService applicationService, ChannelKind channelKind) {
        controller = new Controller(applicationService, channelKind);
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
        private final UserIdentityService userIdentityService;
        private Pin mediationActivatedPin;

        protected Controller(DefaultApplicationService applicationService, ChannelKind channelKind) {
            super(applicationService.getChatService());

            privateTradeChannelService = chatService.getPrivateTradeChannelService();
            tradeChannelSelectionService = chatService.getTradeChannelSelectionService();

            privateDiscussionChannelService = chatService.getPrivateDiscussionChannelService();
            discussionChannelSelectionService = chatService.getDiscussionChannelSelectionService();

            privateEventsChannelService = chatService.getPrivateEventsChannelService();
            eventsChannelSelectionService = chatService.getEventsChannelSelectionService();

            privateSupportChannelService = chatService.getPrivateSupportChannelService();
            supportChannelSelectionService = chatService.getSupportChannelSelectionService();

            userIdentityService = applicationService.getUserService().getUserIdentityService();

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
                        .map(e -> new ChannelSelection.View.ChannelItem(e, userIdentityService))
                        .to(privateTradeChannelService.getChannels());

                selectedChannelPin = FxBindings.subscribe(tradeChannelSelectionService.getSelectedChannel(),
                        channel -> {
                            if (channel instanceof PrivateTradeChannel) {
                                model.selectedChannelItem.set(new ChannelSelection.View.ChannelItem(channel, userIdentityService));
                                userIdentityService.selectChatUserIdentity(((PrivateTradeChannel) channel).getMyUserIdentity());
                                if (mediationActivatedPin != null) {
                                    mediationActivatedPin.unbind();
                                }
                                mediationActivatedPin = FxBindings.bind(model.mediationActivated).to(((PrivateTradeChannel) channel).getMediationActivated());
                            }
                        });
            } else if (model.channelKind == ChannelKind.DISCUSSION) {
                channelsPin = FxBindings.<PrivateDiscussionChannel, ChannelSelection.View.ChannelItem>bind(model.channelItems)
                        .map(e -> new ChannelSelection.View.ChannelItem(e, userIdentityService))
                        .to(privateDiscussionChannelService.getChannels());

                selectedChannelPin = FxBindings.subscribe(discussionChannelSelectionService.getSelectedChannel(),
                        channel -> {
                            if (channel instanceof PrivateDiscussionChannel) {
                                model.selectedChannelItem.set(new ChannelSelection.View.ChannelItem(channel, userIdentityService));
                                userIdentityService.selectChatUserIdentity(((PrivateDiscussionChannel) channel).getMyUserIdentity());
                            }
                        });
            } else if (model.channelKind == ChannelKind.EVENTS) {
                channelsPin = FxBindings.<PrivateEventsChannel, ChannelSelection.View.ChannelItem>bind(model.channelItems)
                        .map(e -> new ChannelSelection.View.ChannelItem(e, userIdentityService))
                        .to(privateEventsChannelService.getChannels());

                selectedChannelPin = FxBindings.subscribe(eventsChannelSelectionService.getSelectedChannel(),
                        channel -> {
                            if (channel instanceof PrivateEventsChannel) {
                                model.selectedChannelItem.set(new ChannelSelection.View.ChannelItem(channel, userIdentityService));
                                userIdentityService.selectChatUserIdentity(((PrivateEventsChannel) channel).getMyUserIdentity());
                            }
                        });
            } else if (model.channelKind == ChannelKind.SUPPORT) {
                channelsPin = FxBindings.<PrivateSupportChannel, ChannelSelection.View.ChannelItem>bind(model.channelItems)
                        .map(e -> new ChannelSelection.View.ChannelItem(e, userIdentityService))
                        .to(privateSupportChannelService.getChannels());

                selectedChannelPin = FxBindings.subscribe(supportChannelSelectionService.getSelectedChannel(),
                        channel -> {
                            if (channel instanceof PrivateSupportChannel) {
                                model.selectedChannelItem.set(new ChannelSelection.View.ChannelItem(channel, userIdentityService));
                                userIdentityService.selectChatUserIdentity(((PrivateSupportChannel) channel).getMyUserIdentity());
                            }
                        });
            }
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
            if (mediationActivatedPin != null) {
                mediationActivatedPin.unbind();
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
        private final BooleanProperty mediationActivated = new SimpleBooleanProperty();

        public Model(ChannelKind channelKind) {
            this.channelKind = channelKind;
        }
    }

    protected static class View extends ChannelSelection.View<Model, Controller> {
        private Subscription mediationActivatedPin;

        protected View(Model model, Controller controller) {
            super(model, controller);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            mediationActivatedPin = EasyBind.subscribe(model.mediationActivated, mediationActivated ->
                    UIThread.runOnNextRenderFrame(listView::refresh));
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();
            mediationActivatedPin.unsubscribe();
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
                final ImageView mediatorsRoboIcon = new ImageView();
                @Nullable
                private Pin mediationActivatedPin;

                {
                    setCursor(Cursor.HAND);
                    setPrefHeight(40);
                    setPadding(new Insets(0, 0, -20, 0));

                    roboIcon.setFitWidth(35);
                    roboIcon.setFitHeight(35);

                    mediatorsRoboIcon.setFitWidth(35);
                    mediatorsRoboIcon.setFitHeight(35);

                    hBox.setSpacing(10);
                    hBox.setAlignment(Pos.CENTER_LEFT);
                }

                @Override
                protected void updateItem(ChannelItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null && !empty && item.getChannel() instanceof PrivateChannel) {
                        UserProfile peer = ((PrivateChannel<?>) item.getChannel()).getPeer();
                        roboIcon.setImage(RoboHash.getImage(peer.getPubKeyHash()));
                        Tooltip.install(this, tooltip);
                        if (item.getChannel() instanceof PrivateTradeChannel) {
                            PrivateTradeChannel privateTradeChannel = (PrivateTradeChannel) item.getChannel();
                            if (mediationActivatedPin != null) {
                                mediationActivatedPin.unbind();
                            }
                            mediationActivatedPin = privateTradeChannel.getMediationActivated().addObserver(e ->
                            {
                                UIThread.run(() -> {
                                    hBox.getChildren().clear();
                                    hBox.getChildren().add(roboIcon);

                                    boolean mediationActivated = privateTradeChannel.getMediator().isPresent() &&
                                            privateTradeChannel.getMediationActivated().get();
                                    if (mediationActivated) {
                                        String displayString = privateTradeChannel.getPeer().getUserName() + ", " +
                                                privateTradeChannel.getMediator().get().getUserName();
                                        if (item.isHasMultipleProfiles()) {
                                            // If we have more than 1 user profiles we add our profile as well
                                            displayString += " [" + privateTradeChannel.getMyUserIdentity().getUserName() + "]";
                                        }
                                        label.setText(displayString);

                                        UserProfile mediator = privateTradeChannel.getMediator().orElseThrow();
                                        mediatorsRoboIcon.setImage(RoboHash.getImage(mediator.getPubKeyHash()));
                                        hBox.getChildren().add(mediatorsRoboIcon);
                                        tooltip.setText(peer.getTooltipString() + "\n\n" +
                                                Res.get("mediator") + ":\n" + mediator.getTooltipString());
                                    } else {
                                        String displayString = privateTradeChannel.getPeer().getUserName();
                                        if (item.isHasMultipleProfiles()) {
                                            // If we have more than 1 user profiles we add our profile as well
                                            displayString += " [" + privateTradeChannel.getMyUserIdentity().getUserName() + "]";
                                        }
                                        label.setText(displayString);
                                        tooltip.setText(peer.getTooltipString());
                                    }
                                    hBox.getChildren().add(label);

                                    if (widthSubscription != null) {
                                        widthSubscription.unsubscribe();
                                    }
                                    widthSubscription = EasyBind.subscribe(widthProperty(), w -> {
                                        if (w.doubleValue() > 0) {
                                            if (mediatorsRoboIcon.getImage() != null) {
                                                label.setMaxWidth(getWidth() - 120);
                                            } else {
                                                label.setMaxWidth(getWidth() - 75);
                                            }
                                        }
                                    });
                                });
                            });
                        } else {
                            hBox.getChildren().clear();
                            hBox.getChildren().add(roboIcon);
                            label.setText(item.getDisplayString());
                            tooltip.setText(peer.getTooltipString());
                            hBox.getChildren().add(label);
                            widthSubscription = EasyBind.subscribe(widthProperty(), w -> {
                                if (w.doubleValue() > 0) {
                                    label.setMaxWidth(getWidth() - 75);
                                }
                            });
                        }
                        setGraphic(hBox);
                    } else {
                        setGraphic(null);
                        hBox.getChildren().clear();
                        Tooltip.install(this, null);
                        if (widthSubscription != null) {
                            widthSubscription.unsubscribe();
                        }
                        if (mediationActivatedPin != null) {
                            mediationActivatedPin.unbind();
                        }
                    }
                }
            };
        }
    }
}