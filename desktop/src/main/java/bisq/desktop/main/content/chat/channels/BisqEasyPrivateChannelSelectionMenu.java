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

package bisq.desktop.main.content.chat.channels;

import bisq.chat.bisqeasy.channel.BisqEasyChatChannelSelectionService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannelService;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.priv.PrivateChatChannel;
import bisq.chat.message.ChatMessage;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Icons;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Badge;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.robohash.RoboHash;
import bisq.i18n.Res;
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
import javafx.scene.layout.StackPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class BisqEasyPrivateChannelSelectionMenu extends PrivateChannelSelectionMenu<
        BisqEasyPrivateTradeChatChannel,
        BisqEasyPrivateTradeChatChannelService,
        BisqEasyChatChannelSelectionService
        > {
    @Getter
    private final Controller controller;

    public BisqEasyPrivateChannelSelectionMenu(ServiceProvider serviceProvider) {
        controller = new Controller(serviceProvider);
    }

    protected static class Controller extends PrivateChannelSelectionMenu.Controller<
            View,
            Model,
            BisqEasyPrivateTradeChatChannel,
            BisqEasyPrivateTradeChatChannelService,
            BisqEasyChatChannelSelectionService
            > {

        private Pin inMediationPin;

        protected Controller(ServiceProvider serviceProvider) {
            super(serviceProvider, ChatChannelDomain.BISQ_EASY);
        }

        @Override
        protected BisqEasyPrivateTradeChatChannelService createAndGetChatChannelService(ChatChannelDomain chatChannelDomain) {
            return chatService.getBisqEasyPrivateTradeChatChannelService();
        }

        @Override
        protected BisqEasyChatChannelSelectionService createAndGetChatChannelSelectionService(ChatChannelDomain chatChannelDomain) {
            return chatService.getBisqEasyChatChannelSelectionService();
        }

        @Override
        protected View createAndGetView() {
            return new View(model, this);
        }

        @Override
        protected Model createAndGetModel(ChatChannelDomain chatChannelDomain) {
            return new Model();
        }

        @Override
        public void onActivate() {
            super.onActivate();
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();

            if (inMediationPin != null) {
                inMediationPin.unbind();
                inMediationPin = null;
            }
        }

        @Override
        protected void handleSelectedChannelChange(ChatChannel<? extends ChatMessage> chatChannel) {
            super.handleSelectedChannelChange(chatChannel);

            if (isChannelExpectedInstance(chatChannel)) {
                BisqEasyPrivateTradeChatChannel bisqEasyPrivateTradeChatChannel = (BisqEasyPrivateTradeChatChannel) chatChannel;
                userIdentityService.selectChatUserIdentity(bisqEasyPrivateTradeChatChannel.getMyUserIdentity());
                if (inMediationPin != null) {
                    inMediationPin.unbind();
                }
                inMediationPin = FxBindings.bind(model.mediationActivated).to(bisqEasyPrivateTradeChatChannel.isInMediationObservable());
            }
        }

        @Override
        protected boolean isChannelExpectedInstance(ChatChannel<? extends ChatMessage> chatChannel) {
            return chatChannel instanceof BisqEasyPrivateTradeChatChannel;
        }

        public String getChannelTitle(BisqEasyPrivateTradeChatChannel chatChannel) {
            return chatService.findChatChannelService(chatChannel)
                    .map(service -> service.getChannelTitle(chatChannel))
                    .orElse("");
        }
    }

    protected static class Model extends PrivateChannelSelectionMenu.Model {
        private final BooleanProperty mediationActivated = new SimpleBooleanProperty();

        public Model() {
            super(ChatChannelDomain.BISQ_EASY);
        }
    }

    protected static class View extends PrivateChannelSelectionMenu.View<Model, Controller> {
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
            return Res.get("bisqEasy.channelSelection.private.headline");
        }

        protected ListCell<ChannelItem> getListCell() {
            return new ListCell<>() {
                final Label removeIcon = Icons.getIcon(AwesomeIcon.MINUS_SIGN_ALT, "14");
                final Label label = new Label();
                final HBox hBox = new HBox(10);
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
                    if (!(chatChannel instanceof PrivateChatChannel)) {
                        return;
                    }

                    BisqEasyPrivateTradeChatChannel privateChatChannel = (BisqEasyPrivateTradeChatChannel) item.getChatChannel();
                    List<ImageView> icons = new ArrayList<>();
                    UserProfile peer = privateChatChannel.getPeer();
                    roboIcon.setImage(RoboHash.getImage(peer.getPubKeyHash()));
                    Tooltip.install(this, tooltip);
                    icons.add(roboIcon);

                    if (inMediationPin != null) {
                        inMediationPin.unbind();
                    }
                    inMediationPin = privateChatChannel.isInMediationObservable().addObserver(e -> {
                        UIThread.run(() -> {
                            hBox.getChildren().clear();
                            hBox.getChildren().add(roboIcon);

                            if (privateChatChannel.getMediator().isPresent() &&
                                    privateChatChannel.isInMediation()) {
                                if (privateChatChannel.isMediator()) {
                                    // We are the mediator
                                    List<UserProfile> traders = new ArrayList<>(privateChatChannel.getTraders());
                                    checkArgument(traders.size() == 2);
                                    UserProfile trader1 = traders.get(0);
                                    UserProfile trader2 = traders.get(1);
                                    roboIcon.setImage(RoboHash.getImage(trader1.getPubKeyHash()));
                                    secondaryRoboIcon.setImage(RoboHash.getImage(trader2.getPubKeyHash()));
                                    tooltip.setText(trader1.getTooltipString() + "\n\n" + trader2.getTooltipString());
                                } else {
                                    UserProfile mediator = privateChatChannel.getMediator().get();
                                    secondaryRoboIcon.setImage(RoboHash.getImage(mediator.getPubKeyHash()));
                                    tooltip.setText(peer.getTooltipString() + "\n\n" +
                                            Res.get("bisqEasy.mediator") + ":\n" + mediator.getTooltipString());
                                }
                                hBox.getChildren().add(secondaryRoboIcon);
                                icons.add(secondaryRoboIcon);
                            } else {
                                tooltip.setText(peer.getTooltipString());
                            }
                            label.setText(controller.getChannelTitle(privateChatChannel));

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
                    removeIcon.setOpacity(0);
                    removeIcon.setOnMouseClicked(e -> controller.onLeaveChannel(privateChatChannel));
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