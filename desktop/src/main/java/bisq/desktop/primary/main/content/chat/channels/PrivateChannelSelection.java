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
import bisq.chat.trade.TradeChannelSelectionService;
import bisq.chat.trade.priv.PrivateTradeChannel;
import bisq.chat.trade.priv.PrivateTradeChannelService;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.Icons;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Badge;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.components.robohash.RoboHash;
import bisq.i18n.Res;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.MapChangeListener;
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
import javafx.scene.layout.StackPane;
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
        private final TradeChannelSelectionService tradeChannelSelectionService;
        private final ChannelSelectionService discussionChannelSelectionService;
        private final ChannelSelectionService eventsChannelSelectionService;
        private final ChannelSelectionService supportChannelSelectionService;
        private final UserIdentityService userIdentityService;
        private Pin inMediationPin;
        private final PrivateChannelService<?, ?, ?> channelService;

        protected Controller(DefaultApplicationService applicationService, ChannelDomain channelDomain) {
            super(applicationService);

            switch (channelDomain) {
                case TRADE:
                    channelService = chatService.getPrivateTradeChannelService();
                    break;
                case DISCUSSION:
                    channelService = chatService.getPrivateDiscussionChannelService();
                    break;
                case EVENTS:
                    channelService = chatService.getPrivateEventsChannelService();
                    break;
                case SUPPORT:
                    channelService = chatService.getPrivateSupportChannelService();
                    break;
                default:
                    throw new RuntimeException("Unexpected channelDomain " + channelDomain);
            }

            tradeChannelSelectionService = chatService.getTradeChannelSelectionService();
            discussionChannelSelectionService = chatService.getDiscussionChannelSelectionService();
            eventsChannelSelectionService = chatService.getEventsChannelSelectionService();
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
        protected ChannelService<?, ?, ?> getChannelService() {
            return channelService;
        }

        @Override
        public void onActivate() {
            super.onActivate();

            if (model.channelDomain == ChannelDomain.TRADE) {
                channelsPin = FxBindings.<PrivateTradeChannel, ChannelSelection.View.ChannelItem>bind(model.channelItems)
                        .map(e -> new ChannelSelection.View.ChannelItem(e, userIdentityService))
                        .to(((PrivateTradeChannelService) channelService).getChannels());

                selectedChannelPin = FxBindings.subscribe(tradeChannelSelectionService.getSelectedChannel(),
                        channel -> {
                            if (channel instanceof PrivateTradeChannel) {
                                model.selectedChannelItem.set(new ChannelSelection.View.ChannelItem(channel, userIdentityService));
                                userIdentityService.selectChatUserIdentity(((PrivateTradeChannel) channel).getMyUserIdentity());
                                if (inMediationPin != null) {
                                    inMediationPin.unbind();
                                }
                                inMediationPin = FxBindings.bind(model.mediationActivated).to(((PrivateTradeChannel) channel).getIsInMediation());
                            }
                        });
            } else if (model.channelDomain == ChannelDomain.DISCUSSION) {
                channelsPin = FxBindings.<PrivateTwoPartyChannel, ChannelSelection.View.ChannelItem>bind(model.channelItems)
                        .map(e -> new ChannelSelection.View.ChannelItem(e, userIdentityService))
                        .to(((PrivateTwoPartyChannelService) channelService).getChannels());

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
                        .to(((PrivateTwoPartyChannelService) channelService).getChannels());

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
                        .to(((PrivateTwoPartyChannelService) channelService).getChannels());

                selectedChannelPin = FxBindings.subscribe(supportChannelSelectionService.getSelectedChannel(),
                        channel -> {
                            if (channel instanceof PrivateTwoPartyChannel) {
                                model.selectedChannelItem.set(new ChannelSelection.View.ChannelItem(channel, userIdentityService));
                                userIdentityService.selectChatUserIdentity(((PrivateTwoPartyChannel) channel).getMyUserIdentity());
                            }
                        });
            } else {
                throw new RuntimeException("Not supported channelDomain " + model.channelDomain);
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
                tradeChannelSelectionService.selectChannel(channelItem.getChatChannel());
            } else if (model.channelDomain == ChannelDomain.DISCUSSION) {
                discussionChannelSelectionService.selectChannel(channelItem.getChatChannel());
            } else if (model.channelDomain == ChannelDomain.EVENTS) {
                eventsChannelSelectionService.selectChannel(channelItem.getChatChannel());
            } else if (model.channelDomain == ChannelDomain.SUPPORT) {
                supportChannelSelectionService.selectChannel(channelItem.getChatChannel());
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
                case TRADE:
                    ((PrivateTradeChannelService) channelService).leaveChannel((PrivateTradeChannel) privateChannel);
                    model.sortedList.stream().filter(e -> !e.getChatChannel().getId().equals(privateChannel.getId()))
                            .findFirst()
                            .ifPresentOrElse(e -> tradeChannelSelectionService.selectChannel(e.getChatChannel()),
                                    () -> tradeChannelSelectionService.selectChannel(null));
                    break;
                case DISCUSSION:
                    ((PrivateTwoPartyChannelService) channelService).leaveChannel((PrivateTwoPartyChannel) privateChannel);
                    model.sortedList.stream().filter(e -> !e.getChatChannel().getId().equals(privateChannel.getId()))
                            .findFirst()
                            .ifPresentOrElse(e -> discussionChannelSelectionService.selectChannel(e.getChatChannel()),
                                    () -> discussionChannelSelectionService.selectChannel(null));
                    break;
                case EVENTS:
                    ((PrivateTwoPartyChannelService) channelService).leaveChannel((PrivateTwoPartyChannel) privateChannel);
                    model.sortedList.stream().filter(e -> !e.getChatChannel().getId().equals(privateChannel.getId()))
                            .findFirst()
                            .ifPresentOrElse(e -> eventsChannelSelectionService.selectChannel(e.getChatChannel()),
                                    () -> eventsChannelSelectionService.selectChannel(null));
                    break;
                case SUPPORT:
                    ((PrivateTwoPartyChannelService) channelService).leaveChannel((PrivateTwoPartyChannel) privateChannel);
                    model.sortedList.stream().filter(e -> !e.getChatChannel().getId().equals(privateChannel.getId()))
                            .findFirst()
                            .ifPresentOrElse(e -> supportChannelSelectionService.selectChannel(e.getChatChannel()),
                                    () -> supportChannelSelectionService.selectChannel(null));
                    break;
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
                final Label removeIcon = Icons.getIcon(AwesomeIcon.MINUS_SIGN_ALT, "14");
                final Label label = new Label();
                final HBox hBox = new HBox();
                final Badge numMessagesBadge = new Badge();
                final StackPane iconAndBadge = new StackPane();
                final Tooltip tooltip = new BisqTooltip();
                final ImageView roboIcon = new ImageView();
                final ImageView secondaryRoboIcon = new ImageView();
                final ColorAdjust nonSelectedEffect = new ColorAdjust();
                final ColorAdjust hoverEffect = new ColorAdjust();
                @Nullable
                private Subscription widthSubscription;
                @Nullable
                private Pin inMediationPin;
                @Nullable
                MapChangeListener<String, Integer> channelIdWithNumUnseenMessagesMapListener;

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

                    numMessagesBadge.setPosition(Pos.CENTER);

                    removeIcon.setCursor(Cursor.HAND);
                    removeIcon.setId("icon-label-grey");
                    HBox.setMargin(removeIcon, new Insets(0, 12, 0, -20));

                    iconAndBadge.getChildren().addAll(numMessagesBadge, removeIcon);
                    iconAndBadge.setAlignment(Pos.CENTER);
                    HBox.setMargin(iconAndBadge, new Insets(0, 12, 0, 0));

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
                        setOnMouseClicked(null);
                        setOnMouseEntered(null);
                        setOnMouseExited(null);
                        hBox.getChildren().clear();
                        if (widthSubscription != null) {
                            widthSubscription.unsubscribe();
                            widthSubscription = null;
                        }
                        if (channelIdWithNumUnseenMessagesMapListener != null) {
                            model.channelIdWithNumUnseenMessagesMap.removeListener(channelIdWithNumUnseenMessagesMapListener);
                            channelIdWithNumUnseenMessagesMapListener = null;
                        }
                        if (inMediationPin != null) {
                            inMediationPin.unbind();
                            inMediationPin = null;
                        }
                        Tooltip.install(this, null);
                        setGraphic(null);
                        return;
                    }

                    ChatChannel<?> chatChannel = item.getChatChannel();
                    if (!(chatChannel instanceof PrivateChannel)) {
                        return;
                    }

                    PrivateChannel<?> privateChannel = (PrivateChannel<?>) item.getChatChannel();
                    UserProfile peer;
                    List<ImageView> icons = new ArrayList<>();
                    if (privateChannel instanceof PrivateTwoPartyChannel) {
                        PrivateTwoPartyChannel privateTwoPartyChannel = (PrivateTwoPartyChannel) privateChannel;
                        peer = privateTwoPartyChannel.getPeer();
                        roboIcon.setImage(RoboHash.getImage(peer.getPubKeyHash()));
                        Tooltip.install(this, tooltip);
                        icons.add(roboIcon);
                    } else if (privateChannel instanceof PrivateGroupChannel<?>) {
                        //todo
                        PrivateGroupChannel<?> privateGroupChannel = (PrivateGroupChannel<?>) privateChannel;
                        List<UserProfile> peers = privateGroupChannel.getPeers();
                        peer = peers.get(0);
                        roboIcon.setImage(RoboHash.getImage(peer.getPubKeyHash()));
                        Tooltip.install(this, tooltip);
                        icons.add(roboIcon);
                    } else {
                        throw new RuntimeException("privateChannel expected to be " +
                                "PrivateTwoPartyChannel or PrivateGroupChannel");
                    }

                    if (privateChannel instanceof PrivateTradeChannel) {
                        PrivateTradeChannel privateTradeChannel = (PrivateTradeChannel) privateChannel;
                        if (inMediationPin != null) {
                            inMediationPin.unbind();
                        }
                        inMediationPin = privateTradeChannel.getIsInMediation().addObserver(e ->
                        {
                            UIThread.run(() -> {
                                hBox.getChildren().clear();
                                hBox.getChildren().add(roboIcon);

                                if (privateTradeChannel.findMediator().isPresent() &&
                                        privateTradeChannel.getIsInMediation().get()) {
                                    if (privateTradeChannel.isMediator()) {
                                        // We are the mediator
                                        UserProfile trader1 = privateTradeChannel.getPeer();
                                        UserProfile trader2 = privateTradeChannel.getPeers().get(1);
                                        roboIcon.setImage(RoboHash.getImage(trader1.getPubKeyHash()));
                                        secondaryRoboIcon.setImage(RoboHash.getImage(trader2.getPubKeyHash()));
                                        tooltip.setText(trader1.getTooltipString() + "\n\n" + trader2.getTooltipString());
                                    } else {
                                        UserProfile mediator = privateTradeChannel.findMediator().get();
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

                                hBox.getChildren().addAll(label, Spacer.fillHBox(), iconAndBadge);

                                if (widthSubscription != null) {
                                    widthSubscription.unsubscribe();
                                }
                                widthSubscription = EasyBind.subscribe(widthProperty(), w -> {
                                    if (w.doubleValue() > 0) {
                                        if (secondaryRoboIcon.getImage() != null) {
                                            label.setMaxWidth(getWidth() - 140);
                                        } else {
                                            label.setMaxWidth(getWidth() - 115);
                                        }
                                    }
                                });
                            });
                        });
                    } else {
                        hBox.getChildren().clear();
                        label.setText(item.getDisplayString());
                        tooltip.setText(peer.getTooltipString());
                        hBox.getChildren().addAll(roboIcon, label, Spacer.fillHBox(), iconAndBadge);
                        widthSubscription = EasyBind.subscribe(widthProperty(), w -> {
                            if (w.doubleValue() > 0) {
                                label.setMaxWidth(getWidth() - 115);
                            }
                        });
                    }
                    removeIcon.setOpacity(0);
                    removeIcon.setOnMouseClicked(e -> controller.onLeaveChannel(privateChannel));
                    setOnMouseClicked(e -> Transitions.fadeIn(removeIcon));
                    setOnMouseEntered(e -> {
                        Transitions.fadeIn(removeIcon);
                        Transitions.fadeOut(numMessagesBadge);
                        applyEffect(icons, item.isSelected(), true);
                    });
                    setOnMouseExited(e -> {
                        Transitions.fadeOut(removeIcon);
                        Transitions.fadeIn(numMessagesBadge);
                        applyEffect(icons, item.isSelected(), false);
                    });


                    applyEffect(icons, item.isSelected(), false);

                    channelIdWithNumUnseenMessagesMapListener = change -> onUnseenMessagesChanged(item, change.getKey(), numMessagesBadge);
                    model.channelIdWithNumUnseenMessagesMap.addListener(channelIdWithNumUnseenMessagesMapListener);
                    model.channelIdWithNumUnseenMessagesMap.keySet().forEach(key -> onUnseenMessagesChanged(item, key, numMessagesBadge));

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