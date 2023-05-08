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
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelSelectionService;
import bisq.chat.channel.ChatChannelService;
import bisq.chat.channel.priv.PrivateChatChannel;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannel;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannelService;
import bisq.desktop.common.observable.FxBindings;
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
public class TwoPartyPrivateChatChannelSelection extends ChannelSelection {
    private final Controller controller;

    public TwoPartyPrivateChatChannelSelection(DefaultApplicationService applicationService, ChatChannelDomain chatChannelDomain) {
        controller = new Controller(applicationService, chatChannelDomain);
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
        private final UserIdentityService userIdentityService;
        private final TwoPartyPrivateChatChannelService twoPartyPrivateChatChannelService;
        private final ChatChannelSelectionService channelSelectionService;

        protected Controller(DefaultApplicationService applicationService, ChatChannelDomain chatChannelDomain) {
            super(applicationService);
            userIdentityService = applicationService.getUserService().getUserIdentityService();
            twoPartyPrivateChatChannelService = chatService.getTwoPartyPrivateChatChannelService(chatChannelDomain);
            channelSelectionService = chatService.getChatChannelSelectionService(chatChannelDomain);

            model = new Model(chatChannelDomain);
            view = new View(model, this);

            model.filteredList.setPredicate(item -> true);
        }

        @Override
        protected ChannelSelection.Model getChannelSelectionModel() {
            return model;
        }

        @Override
        protected ChatChannelService<?, ?, ?> getChannelService() {
            return twoPartyPrivateChatChannelService;
        }

        @Override
        public void onActivate() {
            super.onActivate();

            channelsPin = FxBindings.<TwoPartyPrivateChatChannel, ChannelSelection.View.ChannelItem>bind(model.channelItems)
                    .map(chatChannel -> new ChannelSelection.View.ChannelItem(chatChannel, chatService.getChatChannelService(chatChannel)))
                    .to(twoPartyPrivateChatChannelService.getChannels());

            selectedChannelPin = FxBindings.subscribe(channelSelectionService.getSelectedChannel(),
                    chatChannel -> {
                        if (chatChannel instanceof TwoPartyPrivateChatChannel) {
                            model.selectedChannelItem.set(new ChannelSelection.View.ChannelItem(chatChannel, chatService.getChatChannelService(chatChannel)));
                            userIdentityService.selectChatUserIdentity(((TwoPartyPrivateChatChannel) chatChannel).getMyUserIdentity());
                        } else {
                            model.selectedChannelItem.set(null);
                        }
                    });
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
        }

        @Override
        protected void onSelected(ChannelSelection.View.ChannelItem channelItem) {
            if (channelItem == null) {
                return;
            }
            channelSelectionService.selectChannel(channelItem.getChatChannel());
        }

        public void deSelectChannel() {
            model.selectedChannelItem.set(null);
        }

        public void onLeaveChannel(PrivateChatChannel<?> privateChatChannel) {
            new Popup().warning(Res.get("social.privateChannel.leave.warning", privateChatChannel.getMyUserIdentity().getUserName()))
                    .closeButtonText(Res.get("cancel"))
                    .actionButtonText(Res.get("social.privateChannel.leave"))
                    .onAction(() -> doLeaveChannel(privateChatChannel))
                    .show();
        }

        public void doLeaveChannel(PrivateChatChannel<?> privateChatChannel) {
            ChatChannelSelectionService chatChannelSelectionService = chatService.getChatChannelSelectionService(model.chatChannelDomain);
            twoPartyPrivateChatChannelService.leaveChannel((TwoPartyPrivateChatChannel) privateChatChannel);
            model.sortedList.stream().filter(e -> !e.getChatChannel().getId().equals(privateChatChannel.getId()))
                    .findFirst()
                    .ifPresentOrElse(e -> chatChannelSelectionService.selectChannel(e.getChatChannel()),
                            () -> chatChannelSelectionService.selectChannel(null));
        }
    }

    protected static class Model extends ChannelSelection.Model {
        private final ChatChannelDomain chatChannelDomain;

        public Model(ChatChannelDomain chatChannelDomain) {
            this.chatChannelDomain = chatChannelDomain;
        }
    }

    protected static class View extends ChannelSelection.View<Model, Controller> {
        protected View(Model model, Controller controller) {
            super(model, controller);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();
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
                        Tooltip.install(this, null);
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
                    if (privateChatChannel instanceof TwoPartyPrivateChatChannel) {
                        TwoPartyPrivateChatChannel twoPartyPrivateChatChannel = (TwoPartyPrivateChatChannel) privateChatChannel;
                        peer = twoPartyPrivateChatChannel.getPeer();
                        roboIcon.setImage(RoboHash.getImage(peer.getPubKeyHash()));
                        Tooltip.install(this, tooltip);
                        icons.add(roboIcon);
                    } else {
                        throw new RuntimeException("privateChannel expected to be " +
                                "PrivateTwoPartyChannel");
                    }

                    hBox.getChildren().clear();
                    label.setText(item.getDisplayString());
                    tooltip.setText(peer.getTooltipString());
                    hBox.getChildren().addAll(roboIcon, label, Spacer.fillHBox(), iconAndBadge);
                    widthSubscription = EasyBind.subscribe(widthProperty(), w -> {
                        if (w.doubleValue() > 0) {
                            label.setMaxWidth(getWidth() - 115);
                        }
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