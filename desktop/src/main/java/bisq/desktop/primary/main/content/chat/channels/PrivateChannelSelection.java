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
import bisq.chat.channel.*;
import bisq.chat.discuss.DiscussionChannelSelectionService;
import bisq.chat.events.EventsChannelSelectionService;
import bisq.chat.support.SupportChannelSelectionService;
import bisq.chat.trade.TradeChannelSelectionService;
import bisq.chat.trade.priv.PrivateTradeChannel;
import bisq.chat.trade.priv.PrivateTradeChannelService;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.Icons;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.components.robohash.RoboHash;
import bisq.i18n.Res;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PrivateChannelSelection extends ChannelSelection {
    private final Controller controller;

    public PrivateChannelSelection(DefaultApplicationService applicationService, ChannelDomain channelDomain) {
        controller = new Controller(applicationService, channelDomain);
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
        private final PrivateTwoPartyChannelService privateDiscussionChannelService;
        private final DiscussionChannelSelectionService discussionChannelSelectionService;
        private final PrivateTwoPartyChannelService privateEventsChannelService;
        private final PrivateTwoPartyChannelService privateSupportChannelService;
        private final EventsChannelSelectionService eventsChannelSelectionService;
        private final SupportChannelSelectionService supportChannelSelectionService;
        private final UserIdentityService userIdentityService;
        private Pin inMediationPin;

        protected Controller(DefaultApplicationService applicationService, ChannelDomain channelDomain) {
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

            model = new Model(channelDomain);
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

            if (model.channelDomain == ChannelDomain.TRADE) {
                channelsPin = FxBindings.<PrivateTradeChannel, ChannelSelection.View.ChannelItem>bind(model.channelItems)
                        .map(e -> new ChannelSelection.View.ChannelItem(e, userIdentityService))
                        .to(privateTradeChannelService.getChannels());

                selectedChannelPin = FxBindings.subscribe(tradeChannelSelectionService.getSelectedChannel(),
                        channel -> {
                            if (channel instanceof PrivateTradeChannel) {
                                model.selectedChannelItem.set(new ChannelSelection.View.ChannelItem(channel, userIdentityService));
                                userIdentityService.selectChatUserIdentity(((PrivateTradeChannel) channel).getMyUserIdentity());
                                if (inMediationPin != null) {
                                    inMediationPin.unbind();
                                }
                                inMediationPin = FxBindings.bind(model.mediationActivated).to(((PrivateTradeChannel) channel).getInMediation());
                            }
                        });
            } else if (model.channelDomain == ChannelDomain.DISCUSSION) {
                channelsPin = FxBindings.<PrivateTwoPartyChannel, ChannelSelection.View.ChannelItem>bind(model.channelItems)
                        .map(e -> new ChannelSelection.View.ChannelItem(e, userIdentityService))
                        .to(privateDiscussionChannelService.getChannels());

                selectedChannelPin = FxBindings.subscribe(discussionChannelSelectionService.getSelectedChannel(),
                        channel -> {
                            if (channel instanceof PrivateTwoPartyChannel) {
                                model.selectedChannelItem.set(new ChannelSelection.View.ChannelItem(channel, userIdentityService));
                                userIdentityService.selectChatUserIdentity(((PrivateTwoPartyChannel) channel).getMyUserIdentity());
                            }
                        });
            } else if (model.channelDomain == ChannelDomain.EVENTS) {
                channelsPin = FxBindings.<PrivateTwoPartyChannel, ChannelSelection.View.ChannelItem>bind(model.channelItems)
                        .map(e -> new ChannelSelection.View.ChannelItem(e, userIdentityService))
                        .to(privateEventsChannelService.getChannels());

                selectedChannelPin = FxBindings.subscribe(eventsChannelSelectionService.getSelectedChannel(),
                        channel -> {
                            if (channel instanceof PrivateTwoPartyChannel) {
                                model.selectedChannelItem.set(new ChannelSelection.View.ChannelItem(channel, userIdentityService));
                                userIdentityService.selectChatUserIdentity(((PrivateTwoPartyChannel) channel).getMyUserIdentity());
                            }
                        });
            } else if (model.channelDomain == ChannelDomain.SUPPORT) {
                channelsPin = FxBindings.<PrivateTwoPartyChannel, ChannelSelection.View.ChannelItem>bind(model.channelItems)
                        .map(e -> new ChannelSelection.View.ChannelItem(e, userIdentityService))
                        .to(privateSupportChannelService.getChannels());

                selectedChannelPin = FxBindings.subscribe(supportChannelSelectionService.getSelectedChannel(),
                        channel -> {
                            if (channel instanceof PrivateTwoPartyChannel) {
                                model.selectedChannelItem.set(new ChannelSelection.View.ChannelItem(channel, userIdentityService));
                                userIdentityService.selectChatUserIdentity(((PrivateTwoPartyChannel) channel).getMyUserIdentity());
                            }
                        });
            }
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
            if (inMediationPin != null) {
                inMediationPin.unbind();
            }
        }

        @Override
        protected void onSelected(ChannelSelection.View.ChannelItem channelItem) {
            if (channelItem == null) {
                return;
            }
            if (model.channelDomain == ChannelDomain.TRADE) {
                tradeChannelSelectionService.selectChannel(channelItem.getChannel());
            } else if (model.channelDomain == ChannelDomain.DISCUSSION) {
                discussionChannelSelectionService.selectChannel(channelItem.getChannel());
            } else if (model.channelDomain == ChannelDomain.EVENTS) {
                eventsChannelSelectionService.selectChannel(channelItem.getChannel());
            } else if (model.channelDomain == ChannelDomain.SUPPORT) {
                supportChannelSelectionService.selectChannel(channelItem.getChannel());
            }
        }

        public void deSelectChannel() {
            model.selectedChannelItem.set(null);
        }

        public void onLeaveChannel(PrivateChannel<?> privateChannel) {
            new Popup().warning(Res.get("social.privateChannel.leave.warning", privateChannel.getMyUserIdentity().getUserName()))
                    .closeButtonText(Res.get("cancel"))
                    .actionButtonText(Res.get("social.privateChannel.leave"))
                    .onAction(() -> doLeaveChannel(privateChannel))
                    .show();
        }

        public void doLeaveChannel(PrivateChannel<?> privateChannel) {
            switch (privateChannel.getChannelDomain()) {
                case TRADE -> {
                    privateTradeChannelService.leaveChannel((PrivateTradeChannel) privateChannel);
                }
                case DISCUSSION -> {
                    //todo
                }
                case EVENTS -> {
                    //todo
                }
                case SUPPORT -> {
                    //todo
                }
            }
            if (!model.sortedList.isEmpty()) {
                tradeChannelSelectionService.selectChannel(model.sortedList.get(0).getChannel());
            } else {
                tradeChannelSelectionService.selectChannel(null);
            }
        }
    }

    protected static class Model extends ChannelSelection.Model {
        private final ChannelDomain channelDomain;
        private final BooleanProperty mediationActivated = new SimpleBooleanProperty();

        public Model(ChannelDomain channelDomain) {
            this.channelDomain = channelDomain;
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
                final Label removeIcon = Icons.getIcon(AwesomeIcon.MINUS_SIGN_ALT, "14");
                final Label label = new Label();
                final HBox hBox = new HBox();
                final Tooltip tooltip = new BisqTooltip();
                final ImageView roboIcon = new ImageView();
                final ImageView secondaryRoboIcon = new ImageView();
                final ColorAdjust nonSelectedEffect = new ColorAdjust();
                final ColorAdjust hoverEffect = new ColorAdjust();
                @Nullable
                private Pin inMediationPin;

                {
                    setCursor(Cursor.HAND);
                    setPrefHeight(40);
                    setPadding(new Insets(0, 0, -20, 0));

                    roboIcon.setFitWidth(35);
                    roboIcon.setFitHeight(35);

                    secondaryRoboIcon.setFitWidth(35);
                    secondaryRoboIcon.setFitHeight(35);
                    HBox.setMargin(secondaryRoboIcon, new Insets(0, 0, 0, -20));

                    hBox.setSpacing(10);
                    hBox.setAlignment(Pos.CENTER_LEFT);

                    removeIcon.setCursor(Cursor.HAND);
                    removeIcon.setId("icon-label-grey");
                    HBox.setMargin(removeIcon, new Insets(0, 12, 0, -20));

                    nonSelectedEffect.setSaturation(-0.4);
                    nonSelectedEffect.setBrightness(-0.6);

                    hoverEffect.setSaturation(0.2);
                    hoverEffect.setBrightness(0.2);
                }

                @Override
                protected void updateItem(ChannelItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item == null || empty) {
                        label.setGraphic(null);
                        removeIcon.setOnMouseClicked(null);
                        hBox.setOnMouseClicked(null);
                        hBox.setOnMouseEntered(null);
                        hBox.setOnMouseExited(null);
                        hBox.getChildren().clear();
                        if (widthSubscription != null) {
                            widthSubscription.unsubscribe();
                        }
                        if (inMediationPin != null) {
                            inMediationPin.unbind();
                        }
                        Tooltip.install(this, null);
                        setGraphic(null);
                        return;
                    }

                    Channel<?> channel = item.getChannel();
                    if (!(channel instanceof PrivateChannel)) {
                        return;
                    }

                    PrivateChannel<?> privateChannel = (PrivateChannel<?>) item.getChannel();
                    UserProfile peer = privateChannel.getPeer();
                    roboIcon.setImage(RoboHash.getImage(peer.getPubKeyHash()));
                    Tooltip.install(this, tooltip);
                    List<ImageView> icons = new ArrayList<>();
                    icons.add(roboIcon);

                    if (privateChannel instanceof PrivateTradeChannel) {
                        PrivateTradeChannel privateTradeChannel = (PrivateTradeChannel) privateChannel;
                        if (inMediationPin != null) {
                            inMediationPin.unbind();
                        }
                        inMediationPin = privateTradeChannel.getInMediation().addObserver(e ->
                        {
                            UIThread.run(() -> {
                                hBox.getChildren().clear();
                                hBox.getChildren().add(roboIcon);

                                if (privateTradeChannel.getMediator().isPresent() &&
                                        privateTradeChannel.getInMediation().get()) {
                                    if (privateTradeChannel.isMediator()) {
                                        // We are the mediator
                                        UserProfile trader1 = privateTradeChannel.getPeerOrTrader1();
                                        UserProfile trader2 = privateTradeChannel.getMyUserProfileOrTrader2();
                                        roboIcon.setImage(RoboHash.getImage(trader1.getPubKeyHash()));
                                        secondaryRoboIcon.setImage(RoboHash.getImage(trader2.getPubKeyHash()));
                                        tooltip.setText(trader1.getTooltipString() + "\n\n" + trader2.getTooltipString());
                                    } else {
                                        UserProfile mediator = privateTradeChannel.getMediator().get();
                                        secondaryRoboIcon.setImage(RoboHash.getImage(mediator.getPubKeyHash()));
                                        tooltip.setText(peer.getTooltipString() + "\n\n" +
                                                Res.get("mediator") + ":\n" + mediator.getTooltipString());
                                    }
                                    hBox.getChildren().add(secondaryRoboIcon);
                                    icons.add(secondaryRoboIcon);
                                } else {
                                    tooltip.setText(peer.getTooltipString());
                                }
                                label.setText(privateTradeChannel.getChannelSelectionDisplayString());
                                hBox.getChildren().addAll(label, Spacer.fillHBox(), removeIcon);

                                if (widthSubscription != null) {
                                    widthSubscription.unsubscribe();
                                }
                                widthSubscription = EasyBind.subscribe(widthProperty(), w -> {
                                    if (w.doubleValue() > 0) {
                                        if (secondaryRoboIcon.getImage() != null) {
                                            label.setMaxWidth(getWidth() - 120);
                                        } else {
                                            label.setMaxWidth(getWidth() - 95);
                                        }
                                    }
                                });
                            });
                        });
                    } else {
                        hBox.getChildren().clear();
                        label.setText(item.getDisplayString());
                        tooltip.setText(peer.getTooltipString());
                        hBox.getChildren().addAll(roboIcon, label, Spacer.fillHBox(), removeIcon);
                        widthSubscription = EasyBind.subscribe(widthProperty(), w -> {
                            if (w.doubleValue() > 0) {
                                label.setMaxWidth(getWidth() - 75);
                            }
                        });
                    }
                    removeIcon.setOpacity(0);
                    removeIcon.setOnMouseClicked(e -> controller.onLeaveChannel(privateChannel));
                    setOnMouseClicked(e -> {
                        Transitions.fadeIn(removeIcon);
                    });
                    setOnMouseEntered(e -> {
                        Transitions.fadeIn(removeIcon);
                        applyEffect(icons, item.isSelected(), true);
                    });
                    setOnMouseExited(e -> {
                        Transitions.fadeOut(removeIcon);
                        applyEffect(icons, item.isSelected(), false);
                    });
                    applyEffect(icons, item.isSelected(), false);
                    setGraphic(hBox);
                }

                private void applyEffect(List<ImageView> icons, boolean isSelected, boolean isHover) {
                    icons.forEach(icon -> {
                        if (isSelected) {
                            icon.setEffect(null);
                        } else {
                            if (isHover) {
                                icon.setEffect(hoverEffect);
                            } else {
                                icon.setEffect(nonSelectedEffect);
                            }
                        }
                    });
                }
            };
        }
    }
}