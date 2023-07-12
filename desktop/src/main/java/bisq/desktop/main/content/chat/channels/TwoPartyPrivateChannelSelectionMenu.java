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

import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelSelectionService;
import bisq.chat.channel.priv.PrivateChatChannel;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannel;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannelService;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Icons;
import bisq.desktop.common.Transitions;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Badge;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.robohash.RoboHash;
import bisq.i18n.Res;
import bisq.user.profile.UserProfile;
import de.jensd.fx.fontawesome.AwesomeIcon;
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
public class TwoPartyPrivateChannelSelectionMenu extends PrivateChannelSelectionMenu<
        TwoPartyPrivateChatChannel,
        TwoPartyPrivateChatChannelService,
        ChatChannelSelectionService
        > {
    @Getter
    private final Controller controller;

    public TwoPartyPrivateChannelSelectionMenu(ServiceProvider serviceProvider, ChatChannelDomain chatChannelDomain) {
        controller = new Controller(serviceProvider, chatChannelDomain);
    }

    protected static class Controller extends PrivateChannelSelectionMenu.Controller<
            View,
            Model,
            TwoPartyPrivateChatChannel,
            TwoPartyPrivateChatChannelService,
            ChatChannelSelectionService
            > {

        protected Controller(ServiceProvider serviceProvider, ChatChannelDomain chatChannelDomain) {
            super(serviceProvider, chatChannelDomain);
        }

        @Override
        protected TwoPartyPrivateChatChannelService createAndGetChatChannelService(ChatChannelDomain chatChannelDomain) {
            return chatService.getTwoPartyPrivateChatChannelServices().get(chatChannelDomain);
        }

        @Override
        protected ChatChannelSelectionService createAndGetChatChannelSelectionService(ChatChannelDomain chatChannelDomain) {
            return chatService.getChatChannelSelectionServices().get(chatChannelDomain);
        }

        @Override
        protected View createAndGetView() {
            return new View(model, this);
        }

        @Override
        protected Model createAndGetModel(ChatChannelDomain chatChannelDomain) {
            return new Model(chatChannelDomain);
        }
    }

    protected static class Model extends PrivateChannelSelectionMenu.Model {
        public Model(ChatChannelDomain chatChannelDomain) {
            super(chatChannelDomain);
        }
    }

    protected static class View extends PrivateChannelSelectionMenu.View<Model, Controller> {
        protected View(Model model, Controller controller) {
            super(model, controller);
        }


        protected ListCell<ChannelItem> getListCell() {
            return new ListCell<>() {
                final Label leaveChannelIcon = Icons.getIcon(AwesomeIcon.MINUS_SIGN_ALT, "14");
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

                    leaveChannelIcon.setCursor(Cursor.HAND);
                    leaveChannelIcon.setId("icon-label-grey");
                    Tooltip tooltip = new BisqTooltip(Res.get("bisqEasy.channelSelection.private.leave"));
                    tooltip.getStyleClass().add("dark-tooltip");
                    leaveChannelIcon.setTooltip(tooltip);
                    HBox.setMargin(leaveChannelIcon, new Insets(0, 12, 0, -20));

                    iconAndBadge.getChildren().addAll(numMessagesBadge, leaveChannelIcon);
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
                        leaveChannelIcon.setOnMouseClicked(null);
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
                        Tooltip.uninstall(roboIcon, tooltip);
                        Tooltip.uninstall(label, tooltip);
                        setGraphic(null);
                        return;
                    }

                    ChatChannel<?> chatChannel = item.getChatChannel();
                    if (!(chatChannel instanceof PrivateChatChannel)) {
                        return;
                    }

                    PrivateChatChannel<?> privateChatChannel = (PrivateChatChannel<?>) item.getChatChannel();
                    UserProfile peer;
                    List<ImageView> icons = new ArrayList<>();
                    checkArgument(privateChatChannel instanceof TwoPartyPrivateChatChannel);
                    TwoPartyPrivateChatChannel twoPartyPrivateChatChannel = (TwoPartyPrivateChatChannel) privateChatChannel;
                    peer = twoPartyPrivateChatChannel.getPeer();
                    roboIcon.setImage(RoboHash.getImage(peer.getPubKeyHash()));
                    Tooltip.install(roboIcon, tooltip);
                    Tooltip.install(label, tooltip);
                    icons.add(roboIcon);

                    hBox.getChildren().clear();
                    label.setText(item.getChannelTitle());
                    tooltip.setText(peer.getTooltipString());
                    hBox.getChildren().addAll(roboIcon, label, Spacer.fillHBox(), iconAndBadge);
                    widthSubscription = EasyBind.subscribe(widthProperty(), w -> {
                        if (w.doubleValue() > 0) {
                            label.setMaxWidth(getWidth() - 115);
                        }
                    });
                    leaveChannelIcon.setOpacity(0);
                    leaveChannelIcon.setOnMouseClicked(e -> controller.onLeaveChannel(privateChatChannel));
                    setOnMouseClicked(e -> Transitions.fadeIn(leaveChannelIcon));
                    setOnMouseEntered(e -> {
                        Transitions.fadeIn(leaveChannelIcon);
                        Transitions.fadeOut(numMessagesBadge);
                        applyEffect(icons, item.isSelected(), true);
                    });
                    setOnMouseExited(e -> {
                        Transitions.fadeOut(leaveChannelIcon);
                        Transitions.fadeIn(numMessagesBadge);
                        applyEffect(icons, item.isSelected(), false);
                    });


                    applyEffect(icons, item.isSelected(), false);

                    channelIdWithNumUnseenMessagesMapListener = change -> onUnseenMessagesChanged(item, change.getKey(), numMessagesBadge);
                    model.channelIdWithNumUnseenMessagesMap.addListener(channelIdWithNumUnseenMessagesMapListener);
                    model.channelIdWithNumUnseenMessagesMap.keySet().forEach(channelId -> onUnseenMessagesChanged(item, channelId, numMessagesBadge));

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