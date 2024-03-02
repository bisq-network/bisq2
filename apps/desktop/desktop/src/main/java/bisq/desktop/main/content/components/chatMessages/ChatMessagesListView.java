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

package bisq.desktop.main.content.components.chatMessages;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.*;
import bisq.chat.bisqeasy.BisqEasyOfferMessage;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.CommonPublicChatChannelService;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.notifications.ChatNotificationService;
import bisq.chat.priv.PrivateChatChannel;
import bisq.chat.priv.PrivateChatChannelService;
import bisq.chat.priv.PrivateChatMessage;
import bisq.chat.pub.PublicChatChannel;
import bisq.chat.pub.PublicChatMessage;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.controls.*;
import bisq.desktop.components.list_view.ListViewUtil;
import bisq.desktop.components.list_view.NoSelectionModel;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.bisq_easy.BisqEasyServiceUtil;
import bisq.desktop.main.content.bisq_easy.take_offer.TakeOfferController;
import bisq.desktop.main.content.chat.ChatUtil;
import bisq.desktop.main.content.components.ReportToModeratorWindow;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.options.OfferOptionUtil;
import bisq.settings.SettingsService;
import bisq.trade.Trade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class ChatMessagesListView {
    private final Controller controller;

    public ChatMessagesListView(ServiceProvider serviceProvider,
                                Consumer<UserProfile> mentionUserHandler,
                                Consumer<ChatMessage> showChatUserDetailsHandler,
                                Consumer<ChatMessage> replyHandler,
                                ChatChannelDomain chatChannelDomain) {
        controller = new Controller(serviceProvider,
                mentionUserHandler,
                showChatUserDetailsHandler,
                replyHandler,
                chatChannelDomain);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void setSearchPredicate(Predicate<? super ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> predicate) {
        controller.setSearchPredicate(predicate);
    }

    public void setBisqEasyOffersFilerPredicate(Predicate<? super ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> predicate) {
        controller.setBisqEasyOffersFilerPredicate(predicate);
    }

    public void setBisqEasyReputationsFilerPredicate(Predicate<? super ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> predicate) {
        controller.setBisqEasyReputationsFilerPredicate(predicate);
    }

    public void refreshMessages() {
        controller.refreshMessages();
    }

    public static class Controller implements bisq.desktop.common.view.Controller {
        private final ChatService chatService;
        private final UserIdentityService userIdentityService;
        private final UserProfileService userProfileService;
        private final ReputationService reputationService;
        private final SettingsService settingsService;
        private final Consumer<UserProfile> mentionUserHandler;
        private final Consumer<ChatMessage> replyHandler;
        private final Consumer<ChatMessage> showChatUserDetailsHandler;
        private final Model model;
        @Getter
        private final View view;
        private final ChatNotificationService chatNotificationService;
        private final BisqEasyTradeService bisqEasyTradeService;
        private final BannedUserService bannedUserService;
        private final NetworkService networkService;
        private Pin selectedChannelPin, chatMessagesPin, offerOnlySettingsPin;
        private Subscription selectedChannelSubscription, focusSubscription, scrollValuePin, scrollBarVisiblePin;

        private Controller(ServiceProvider serviceProvider,
                           Consumer<UserProfile> mentionUserHandler,
                           Consumer<ChatMessage> showChatUserDetailsHandler,
                           Consumer<ChatMessage> replyHandler,
                           ChatChannelDomain chatChannelDomain) {
            chatService = serviceProvider.getChatService();
            chatNotificationService = chatService.getChatNotificationService();
            userIdentityService = serviceProvider.getUserService().getUserIdentityService();
            userProfileService = serviceProvider.getUserService().getUserProfileService();
            reputationService = serviceProvider.getUserService().getReputationService();
            settingsService = serviceProvider.getSettingsService();
            bisqEasyTradeService = serviceProvider.getTradeService().getBisqEasyTradeService();
            bannedUserService = serviceProvider.getUserService().getBannedUserService();
            networkService = serviceProvider.getNetworkService();
            this.mentionUserHandler = mentionUserHandler;
            this.showChatUserDetailsHandler = showChatUserDetailsHandler;
            this.replyHandler = replyHandler;

            model = new Model(userIdentityService, chatChannelDomain);
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            model.getSortedChatMessages().setComparator(ChatMessageListItem::compareTo);

            offerOnlySettingsPin = FxBindings.subscribe(settingsService.getOffersOnly(), offerOnly -> UIThread.run(this::applyPredicate));

            if (selectedChannelPin != null) {
                selectedChannelPin.unbind();
            }

            ChatChannelSelectionService selectionService = chatService.getChatChannelSelectionServices().get(model.getChatChannelDomain());

            selectedChannelPin = selectionService.getSelectedChannel().addObserver(this::selectedChannelChanged);

            scrollValuePin = EasyBind.subscribe(model.getScrollValue(), scrollValue -> {
                if (scrollValue != null) {
                    applyScrollValue(scrollValue.doubleValue());
                }
            });

            scrollBarVisiblePin = EasyBind.subscribe(model.scrollBarVisible, scrollBarVisible -> {
                if (scrollBarVisible != null && scrollBarVisible) {
                    applyScrollValue(1);
                }
            });

            applyScrollValue(1);
        }

        @Override
        public void onDeactivate() {
            if (offerOnlySettingsPin != null) {
                offerOnlySettingsPin.unbind();
            }
            if (selectedChannelPin != null) {
                selectedChannelPin.unbind();
                selectedChannelPin = null;
            }
            if (chatMessagesPin != null) {
                chatMessagesPin.unbind();
                chatMessagesPin = null;
            }
            if (focusSubscription != null) {
                focusSubscription.unsubscribe();
            }
            if (selectedChannelSubscription != null) {
                selectedChannelSubscription.unsubscribe();
            }

            scrollValuePin.unsubscribe();
            scrollBarVisiblePin.unsubscribe();

            model.chatMessages.forEach(ChatMessageListItem::dispose);
            model.chatMessages.clear();
        }

        private void selectedChannelChanged(ChatChannel<? extends ChatMessage> channel) {
            UIThread.run(() -> {
                model.selectedChannel.set(channel);
                model.isPublicChannel.set(channel instanceof PublicChatChannel);

                if (chatMessagesPin != null) {
                    chatMessagesPin.unbind();
                }

                // Clear and call dispose on the current messages when we change the channel.
                model.chatMessages.forEach(ChatMessageListItem::dispose);
                model.chatMessages.clear();

                if (channel instanceof BisqEasyOfferbookChannel) {
                    chatMessagesPin = bindChatMessages((BisqEasyOfferbookChannel) channel);
                } else if (channel instanceof BisqEasyOpenTradeChannel) {
                    chatMessagesPin = bindChatMessages((BisqEasyOpenTradeChannel) channel);
                } else if (channel instanceof CommonPublicChatChannel) {
                    chatMessagesPin = bindChatMessages((CommonPublicChatChannel) channel);
                } else if (channel instanceof TwoPartyPrivateChatChannel) {
                    chatMessagesPin = bindChatMessages((TwoPartyPrivateChatChannel) channel);
                }

                if (focusSubscription != null) {
                    focusSubscription.unsubscribe();
                }
                if (selectedChannelSubscription != null) {
                    selectedChannelSubscription.unsubscribe();
                }
                if (channel != null) {
                    Scene scene = view.getRoot().getScene();
                    if (scene != null) {
                        focusSubscription = EasyBind.subscribe(scene.getWindow().focusedProperty(),
                                focused -> {
                                    if (focused && model.getSelectedChannel().get() != null) {
                                        chatNotificationService.consume(model.getSelectedChannel().get().getId());
                                    }
                                });
                    }

                    selectedChannelSubscription = EasyBind.subscribe(model.selectedChannel,
                            selectedChannel -> {
                                if (selectedChannel != null) {
                                    chatNotificationService.consume(model.getSelectedChannel().get().getId());
                                }
                            });
                }
            });
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // API - called from client
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void refreshMessages() {
            model.chatMessages.setAll(new ArrayList<>(model.chatMessages));
        }

        private void setSearchPredicate(Predicate<? super ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> predicate) {
            model.setSearchPredicate(Objects.requireNonNullElseGet(predicate, () -> e -> true));
            applyPredicate();
        }

        private void setBisqEasyOffersFilerPredicate(Predicate<? super ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> predicate) {
            model.setBisqEasyOffersFilerPredicate(Objects.requireNonNullElseGet(predicate, () -> e -> true));
            applyPredicate();
        }

        private void setBisqEasyReputationsFilerPredicate(Predicate<? super ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> predicate) {
            model.setBisqEasyReputationsFilerPredicate(Objects.requireNonNullElseGet(predicate, () -> e -> true));
            applyPredicate();
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // UI - delegate to client
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        public void onMention(UserProfile userProfile) {
            mentionUserHandler.accept(userProfile);
        }

        public void onShowChatUserDetails(ChatMessage chatMessage) {
            showChatUserDetailsHandler.accept(chatMessage);
        }

        public void onReply(ChatMessage chatMessage) {
            replyHandler.accept(chatMessage);
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // UI - handler
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        public void onTakeOffer(BisqEasyOfferbookMessage bisqEasyOfferbookMessage) {
            checkArgument(bisqEasyOfferbookMessage.getBisqEasyOffer().isPresent(), "message must contain offer");
            checkArgument(userIdentityService.getSelectedUserIdentity() != null,
                    "userIdentityService.getSelectedUserIdentity() must not be null");
            checkArgument(!model.isMyMessage(bisqEasyOfferbookMessage), "tradeChatMessage must not be mine");

            UserProfile userProfile = userIdentityService.getSelectedUserIdentity().getUserProfile();
            NetworkId takerNetworkId = userProfile.getNetworkId();
            BisqEasyOffer bisqEasyOffer = bisqEasyOfferbookMessage.getBisqEasyOffer().get();
            String tradeId = Trade.createId(bisqEasyOffer.getId(), takerNetworkId.getId());
            if (bisqEasyTradeService.tradeExists(tradeId)) {
                new Popup().information(Res.get("chat.message.offer.offerAlreadyTaken.warn")).show();
                return;
            }

            if (!BisqEasyServiceUtil.offerMatchesMinRequiredReputationScore(reputationService,
                    settingsService,
                    userIdentityService,
                    userProfileService,
                    bisqEasyOffer)) {
                if (bisqEasyOffer.getDirection().isSell()) {
                    long makerAsSellersScore = userProfileService.findUserProfile(bisqEasyOffer.getMakersUserProfileId())
                            .map(reputationService::getReputationScore)
                            .map(ReputationScore::getTotalScore)
                            .orElse(0L);
                    long myMinRequiredScore = settingsService.getMinRequiredReputationScore().get();
                    new Popup().information(Res.get("chat.message.takeOffer.makersReputationScoreTooLow.warn",
                            myMinRequiredScore, makerAsSellersScore)).show();
                } else {
                    long myScoreAsSeller = reputationService.getReputationScore(userIdentityService.getSelectedUserIdentity().getUserProfile()).getTotalScore();
                    long offersRequiredScore = OfferOptionUtil.findRequiredTotalReputationScore(bisqEasyOffer).orElse(0L);
                    new Popup().information(Res.get("chat.message.takeOffer.myReputationScoreTooLow.warn",
                            offersRequiredScore, myScoreAsSeller)).show();
                }
                return;
            }

            if (bannedUserService.isUserProfileBanned(bisqEasyOfferbookMessage.getAuthorUserProfileId()) ||
                    bannedUserService.isUserProfileBanned(userProfile)) {
                return;
            }

            Navigation.navigateTo(NavigationTarget.TAKE_OFFER, new TakeOfferController.InitData(bisqEasyOffer));
        }

        public void onDeleteMessage(ChatMessage chatMessage) {
            String authorUserProfileId = chatMessage.getAuthorUserProfileId();
            userIdentityService.findUserIdentity(authorUserProfileId)
                    .ifPresent(authorUserIdentity -> {
                        if (authorUserIdentity.equals(userIdentityService.getSelectedUserIdentity())) {
                            boolean isBisqEasyPublicChatMessageWithOffer =
                                    chatMessage instanceof BisqEasyOfferbookMessage
                                            && ((BisqEasyOfferMessage) chatMessage).hasBisqEasyOffer();
                            if (isBisqEasyPublicChatMessageWithOffer) {
                                new Popup().warning(Res.get("bisqEasy.offerbook.chatMessage.deleteOffer.confirmation"))
                                        .actionButtonText(Res.get("confirmation.yes"))
                                        .onAction(() -> doDeleteMessage(chatMessage, authorUserIdentity))
                                        .closeButtonText(Res.get("confirmation.no"))
                                        .show();
                            } else {
                                doDeleteMessage(chatMessage, authorUserIdentity);
                            }
                        } else {
                            new Popup().warning(Res.get("chat.message.delete.differentUserProfile.warn"))
                                    .closeButtonText(Res.get("confirmation.no"))
                                    .actionButtonText(Res.get("confirmation.yes"))
                                    .onAction(() -> {
                                        userIdentityService.selectChatUserIdentity(authorUserIdentity);
                                        doDeleteMessage(chatMessage, authorUserIdentity);
                                    })
                                    .show();
                        }
                    });
        }

        private void doDeleteMessage(ChatMessage chatMessage, UserIdentity userIdentity) {
            checkArgument(chatMessage instanceof PublicChatMessage);

            if (chatMessage instanceof BisqEasyOfferbookMessage) {
                BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) chatMessage;
                chatService.getBisqEasyOfferbookChannelService().deleteChatMessage(bisqEasyOfferbookMessage, userIdentity.getNetworkIdWithKeyPair())
                        .whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                log.error("We got an error at doDeleteMessage: " + throwable);
                            }
                        });
            } else if (chatMessage instanceof CommonPublicChatMessage) {
                CommonPublicChatChannelService commonPublicChatChannelService = chatService.getCommonPublicChatChannelServices().get(model.chatChannelDomain);
                CommonPublicChatMessage commonPublicChatMessage = (CommonPublicChatMessage) chatMessage;
                commonPublicChatChannelService.findChannel(chatMessage)
                        .ifPresent(channel -> commonPublicChatChannelService.deleteChatMessage(commonPublicChatMessage, userIdentity.getNetworkIdWithKeyPair()));
            }
        }

        public void onOpenPrivateChannel(ChatMessage chatMessage) {
            checkArgument(!model.isMyMessage(chatMessage));

            userProfileService.findUserProfile(chatMessage.getAuthorUserProfileId())
                    .ifPresent(this::createAndSelectTwoPartyPrivateChatChannel);
        }

        public void onSaveEditedMessage(ChatMessage chatMessage, String editedText) {
            checkArgument(chatMessage instanceof PublicChatMessage);
            checkArgument(model.isMyMessage(chatMessage));

            if (editedText.length() > ChatMessage.MAX_TEXT_LENGTH) {
                new Popup().warning(Res.get("validation.tooLong", ChatMessage.MAX_TEXT_LENGTH)).show();
                return;
            }

            UserIdentity userIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
            if (chatMessage instanceof BisqEasyOfferbookMessage) {
                BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) chatMessage;
                chatService.getBisqEasyOfferbookChannelService().publishEditedChatMessage(bisqEasyOfferbookMessage, editedText, userIdentity);
            } else if (chatMessage instanceof CommonPublicChatMessage) {
                CommonPublicChatMessage commonPublicChatMessage = (CommonPublicChatMessage) chatMessage;
                chatService.getCommonPublicChatChannelServices().get(model.chatChannelDomain).publishEditedChatMessage(commonPublicChatMessage, editedText, userIdentity);
            }
        }

        void onOpenMoreOptions(Node owner, ChatMessage chatMessage, Runnable onClose) {
            if (chatMessage.equals(model.selectedChatMessageForMoreOptionsPopup.get())) {
                return;
            }
            model.selectedChatMessageForMoreOptionsPopup.set(chatMessage);

            List<BisqPopupMenuItem> items = new ArrayList<>();
            items.add(new BisqPopupMenuItem(Res.get("chat.message.contextMenu.copyMessage"),
                    () -> onCopyMessage(chatMessage)));
            if (!model.isMyMessage(chatMessage)) {
                if (chatMessage instanceof PublicChatMessage) {
                    items.add(new BisqPopupMenuItem(Res.get("chat.message.contextMenu.ignoreUser"),
                            () -> onIgnoreUser(chatMessage)));
                }
                items.add(new BisqPopupMenuItem(Res.get("chat.message.contextMenu.reportUser"),
                        () -> onReportUser(chatMessage)));
            }

            BisqPopupMenu menu = new BisqPopupMenu(items, onClose);
            menu.setAlignment(BisqPopup.Alignment.LEFT);
            menu.show(owner);
        }

        public void onReportUser(ChatMessage chatMessage) {
            ChatChannelDomain chatChannelDomain = model.getSelectedChannel().get().getChatChannelDomain();
            if (chatMessage instanceof PrivateChatMessage) {
                PrivateChatMessage privateChatMessage = (PrivateChatMessage) chatMessage;
                Navigation.navigateTo(NavigationTarget.REPORT_TO_MODERATOR,
                        new ReportToModeratorWindow.InitData(privateChatMessage.getSenderUserProfile(), chatChannelDomain));
            } else {
                userProfileService.findUserProfile(chatMessage.getAuthorUserProfileId())
                        .ifPresent(accusedUserProfile -> Navigation.navigateTo(NavigationTarget.REPORT_TO_MODERATOR,
                                new ReportToModeratorWindow.InitData(accusedUserProfile, chatChannelDomain)));
            }
        }

        public void onIgnoreUser(ChatMessage chatMessage) {
            new Popup().warning(Res.get("chat.ignoreUser.warn"))
                    .actionButtonText(Res.get("chat.ignoreUser.confirm"))
                    .onAction(() -> doIgnoreUser(chatMessage))
                    .closeButtonText(Res.get("action.cancel"))
                    .show();
        }

        public void doIgnoreUser(ChatMessage chatMessage) {
            userProfileService.findUserProfile(chatMessage.getAuthorUserProfileId())
                    .ifPresent(userProfileService::ignoreUserProfile);
        }

        public void onCopyMessage(ChatMessage chatMessage) {
            ClipboardUtil.copyToClipboard(chatMessage.getText());
        }

        public void onLeaveChannel() {
            ChatChannel<?> chatChannel = model.getSelectedChannel().get();
            checkArgument(chatChannel instanceof PrivateChatChannel,
                    "Not possible to leave a channel which is not a private chat.");

            new Popup().information(Res.get("chat.leave.info"))
                    .actionButtonText(Res.get("chat.leave.confirmLeaveChat"))
                    .onAction(this::doLeaveChannel)
                    .closeButtonText(Res.get("action.cancel"))
                    .show();
        }

        public void doLeaveChannel() {
            ChatChannel<?> chatChannel = model.getSelectedChannel().get();
            checkArgument(chatChannel instanceof PrivateChatChannel,
                    "Not possible to leave a channel which is not a private chat.");

            chatService.findChatChannelService(chatChannel)
                    .filter(service -> service instanceof PrivateChatChannelService)
                    .map(service -> (PrivateChatChannelService<?, ?, ?>) service).stream()
                    .findAny()
                    .ifPresent(service -> {
                        service.leaveChannel(chatChannel.getId());
                        chatService.getChatChannelSelectionServices().get(model.getChatChannelDomain()).maybeSelectFirstChannel();
                    });
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // Scrolling
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void applyScrollValue(double scrollValue) {
            model.scrollValue.set(scrollValue);
            model.hasUnreadMessages.set(model.numReadMessages < model.getChatMessages().size());
            boolean isAtBottom = scrollValue == 1d;
            model.showScrolledDownButton.set(!isAtBottom && model.scrollBarVisible.get());
            model.autoScrollToBottom = isAtBottom;
            if (isAtBottom) {
                model.numReadMessages = model.getChatMessages().size();
            }

            int numUnReadMessages = model.getChatMessages().size() - model.numReadMessages;
            model.numUnReadMessages.set(numUnReadMessages > 0 ? String.valueOf(numUnReadMessages) : "");
        }

        private void maybeScrollDownOnNewItemAdded() {
            if (model.autoScrollToBottom) {
                // The 100 ms delay is needed as when the item gets added to the listview it updates the scroll property
                // to a value < 1. After the render process is done we set it to 1.
                UIScheduler.run(() -> applyScrollValue(1)).after(100);
            } else {
                applyScrollValue(model.scrollValue.get());
            }
        }

        void onScrollToBottom() {
            applyScrollValue(1);
        }

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // Private
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void createAndSelectTwoPartyPrivateChatChannel(UserProfile peer) {
            chatService.createAndSelectTwoPartyPrivateChatChannel(model.getChatChannelDomain(), peer)
                    .ifPresent(channel -> {
                        if (model.getChatChannelDomain() == ChatChannelDomain.BISQ_EASY_OFFERBOOK) {
                            Navigation.navigateTo(NavigationTarget.BISQ_EASY_PRIVATE_CHAT);
                        }
                        if (model.getChatChannelDomain() == ChatChannelDomain.DISCUSSION) {
                            Navigation.navigateTo(NavigationTarget.DISCUSSION_PRIVATECHATS);
                        }
                        if (model.getChatChannelDomain() == ChatChannelDomain.EVENTS) {
                            Navigation.navigateTo(NavigationTarget.EVENTS_PRIVATECHATS);
                        }
                        if (model.getChatChannelDomain() == ChatChannelDomain.SUPPORT) {
                            Navigation.navigateTo(NavigationTarget.SUPPORT_PRIVATECHATS);
                        }
                    });
        }

        private void applyPredicate() {
            boolean offerOnly = settingsService.getOffersOnly().get();
            Predicate<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> predicate = item -> {
                Optional<UserProfile> senderUserProfile = item.getSenderUserProfile();
                if (senderUserProfile.isEmpty()) {
                    return false;
                }
                if (bannedUserService.isUserProfileBanned(item.getChatMessage().getAuthorUserProfileId()) ||
                        bannedUserService.isUserProfileBanned(senderUserProfile.get())) {
                    return false;
                }

                boolean offerOnlyPredicate = true;
                if (item.getChatMessage() instanceof BisqEasyOfferbookMessage) {
                    BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) item.getChatMessage();
                    offerOnlyPredicate = !offerOnly || bisqEasyOfferbookMessage.hasBisqEasyOffer();
                }
                // We do not display the take offer message as it has no text and is used only for sending the offer
                // to the peer and signalling the take offer event.
                if (item.getChatMessage().getChatMessageType() == ChatMessageType.TAKE_BISQ_EASY_OFFER) {
                    return false;
                }

                return offerOnlyPredicate &&
                        !userProfileService.getIgnoredUserProfileIds().contains(senderUserProfile.get().getId()) &&
                        userProfileService.findUserProfile(senderUserProfile.get().getId()).isPresent();
            };
            model.filteredChatMessages.setPredicate(item -> model.getSearchPredicate().test(item)
                    && model.getBisqEasyOffersFilerPredicate().test(item)
                    && model.getBisqEasyReputationsFilerPredicate().test(item)
                    && predicate.test(item));
        }

        private <M extends ChatMessage, C extends ChatChannel<M>> Pin bindChatMessages(C channel) {
            // We clear and fill the list at channel change. The addObserver triggers the add method for each item,
            // but as we have a contains() check there it will not have any effect.
            model.chatMessages.clear();
            model.chatMessages.addAll(channel.getChatMessages().stream().map(chatMessage -> new ChatMessageListItem<>(chatMessage, channel, userProfileService, reputationService,
                            bisqEasyTradeService, userIdentityService, networkService))
                    .collect(Collectors.toSet()));
            maybeScrollDownOnNewItemAdded();

            return channel.getChatMessages().addObserver(new CollectionObserver<>() {
                @Override
                public void add(M chatMessage) {
                    // TODO (low prio) Delaying to the next render frame can cause duplicated items in case we get the channel
                    //  change called 2 times in short interval (should be avoid as well).
                    // @namloan Could you re-test the performance issues with testing if using UIThread.run makes a difference?
                    // There have been many changes in the meantime, so maybe the performance issue was fixed by other changes.
                    UIThread.runOnNextRenderFrame(() -> {
                        ChatMessageListItem<M, C> item = new ChatMessageListItem<>(chatMessage, channel, userProfileService, reputationService,
                                bisqEasyTradeService, userIdentityService, networkService);
                        // As long as we use runOnNextRenderFrame we need to check to avoid adding duplicates
                        // The model is updated async in stages, verify that messages belong to the selected channel
                        if (!model.chatMessages.contains(item) && channel.equals(model.selectedChannel.get())) {
                            model.chatMessages.add(item);
                            maybeScrollDownOnNewItemAdded();
                        }
                    });
                }

                @Override
                public void remove(Object element) {
                    if (element instanceof ChatMessage) {
                        UIThread.runOnNextRenderFrame(() -> {
                            ChatMessage chatMessage = (ChatMessage) element;
                            Optional<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> toRemove =
                                    model.chatMessages.stream()
                                            .filter(item -> item.getChatMessage().getId().equals(chatMessage.getId()))
                                            .findAny();
                            toRemove.ifPresent(item -> {
                                item.dispose();
                                model.chatMessages.remove(item);
                            });
                        });
                    }
                }

                @Override
                public void clear() {
                    UIThread.runOnNextRenderFrame(() -> {
                        model.chatMessages.forEach(ChatMessageListItem::dispose);
                        model.chatMessages.clear();
                    });
                }
            });
        }

        public String getUserName(String userProfileId) {
            return userProfileService.findUserProfile(userProfileId)
                    .map(UserProfile::getUserName)
                    .orElse(Res.get("data.na"));
        }
    }

    @Getter
    public static class Model implements bisq.desktop.common.view.Model {
        private final UserIdentityService userIdentityService;
        private final ObjectProperty<ChatChannel<?>> selectedChannel = new SimpleObjectProperty<>();
        private final ObservableList<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> chatMessages = FXCollections.observableArrayList();
        private final FilteredList<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> filteredChatMessages = new FilteredList<>(chatMessages);
        private final SortedList<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> sortedChatMessages = new SortedList<>(filteredChatMessages);
        private final BooleanProperty isPublicChannel = new SimpleBooleanProperty();
        private final ObjectProperty<ChatMessage> selectedChatMessageForMoreOptionsPopup = new SimpleObjectProperty<>(null);
        private final ChatChannelDomain chatChannelDomain;
        @Setter
        private Predicate<? super ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> searchPredicate = e -> true;
        @Setter
        private Predicate<? super ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> BisqEasyOffersFilerPredicate = e -> true;
        @Setter
        private Predicate<? super ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> BisqEasyReputationsFilerPredicate = e -> true;

        private boolean autoScrollToBottom;
        private int numReadMessages;
        private final BooleanProperty hasUnreadMessages = new SimpleBooleanProperty();
        private final StringProperty numUnReadMessages = new SimpleStringProperty();
        private final BooleanProperty showScrolledDownButton = new SimpleBooleanProperty();
        private final BooleanProperty scrollBarVisible = new SimpleBooleanProperty();
        private final DoubleProperty scrollValue = new SimpleDoubleProperty();

        private Model(UserIdentityService userIdentityService,
                      ChatChannelDomain chatChannelDomain) {
            this.userIdentityService = userIdentityService;
            this.chatChannelDomain = chatChannelDomain;
        }

        boolean isMyMessage(ChatMessage chatMessage) {
            return chatMessage.isMyMessage(userIdentityService);
        }
    }


    @Slf4j
    private static class View extends bisq.desktop.common.view.View<StackPane, Model, Controller> {
        private final ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> listView;
        private final ImageView scrollDownImageView;
        private final Badge scrollDownBadge;
        private final BisqTooltip scrollDownTooltip;
        private final Label placeholderTitle = new Label("");
        private final Label placeholderDescription = new Label("");
        private Optional<ScrollBar> scrollBar = Optional.empty();
        private Subscription hasUnreadMessagesPin, showScrolledDownButtonPin;
        private Timeline fadeInScrollDownBadgeTimeline;

        private View(Model model, Controller controller) {
            super(new StackPane(), model, controller);

            listView = new ListView<>(model.getSortedChatMessages());
            listView.getStyleClass().add("chat-messages-list-view");

            VBox placeholder = ChatUtil.createEmptyChatPlaceholder(placeholderTitle, placeholderDescription);
            listView.setPlaceholder(placeholder);

            listView.setCellFactory(new ChatMessageListCellFactory(controller, model));

            // https://stackoverflow.com/questions/20621752/javafx-make-listview-not-selectable-via-mouse
            listView.setSelectionModel(new NoSelectionModel<>());
            VBox.setVgrow(listView, Priority.ALWAYS);

            scrollDownImageView = new ImageView();

            scrollDownBadge = new Badge(scrollDownImageView);
            scrollDownBadge.setMaxSize(25, 25);
            scrollDownBadge.getStyleClass().add("chat-messages-badge");
            scrollDownBadge.setPosition(Pos.BOTTOM_RIGHT);
            scrollDownBadge.setBadgeInsets(new Insets(20, 10, 0, 50));
            scrollDownBadge.setCursor(Cursor.HAND);

            scrollDownTooltip = new BisqTooltip(Res.get("chat.listView.scrollDown"));
            Tooltip.install(scrollDownBadge, scrollDownTooltip);

            StackPane.setAlignment(scrollDownBadge, Pos.BOTTOM_CENTER);
            StackPane.setMargin(scrollDownBadge, new Insets(0, 0, 10, 0));
            root.setAlignment(Pos.CENTER);
            root.getChildren().addAll(listView, scrollDownBadge);
        }

        @Override
        protected void onViewAttached() {
            ListViewUtil.findScrollbarAsync(listView, Orientation.VERTICAL, 1000).whenComplete((scrollBar, throwable) -> {
                if (throwable != null) {
                    log.error("Find scrollbar failed", throwable);
                    return;
                }
                this.scrollBar = scrollBar;
                if (scrollBar.isPresent()) {
                    scrollBar.get().valueProperty().bindBidirectional(model.getScrollValue());
                    model.scrollBarVisible.bind(scrollBar.get().visibleProperty());
                    controller.onScrollToBottom();
                } else {
                    log.error("scrollBar is empty");
                }
            });

            scrollDownBadge.textProperty().bind(model.numUnReadMessages);

            scrollDownBadge.setOpacity(0);
            showScrolledDownButtonPin = EasyBind.subscribe(model.showScrolledDownButton, showScrolledDownButton -> {
                if (showScrolledDownButton == null) {
                    return;
                }
                if (fadeInScrollDownBadgeTimeline != null) {
                    fadeInScrollDownBadgeTimeline.stop();
                }
                if (showScrolledDownButton) {
                    fadeInScrollDownBadge();
                } else {
                    scrollDownBadge.setOpacity(0);
                }
            });
            hasUnreadMessagesPin = EasyBind.subscribe(model.hasUnreadMessages, hasUnreadMessages -> {
                if (hasUnreadMessages) {
                    scrollDownImageView.setOpacity(1);
                    scrollDownImageView.setId("scroll-down-green");
                    scrollDownTooltip.setText(Res.get("chat.listView.scrollDown.newMessages"));
                } else {
                    scrollDownImageView.setOpacity(0.5);
                    scrollDownImageView.setId("scroll-down-white");
                    scrollDownTooltip.setText(Res.get("chat.listView.scrollDown"));
                }
            });

            scrollDownBadge.setOnMouseClicked(e -> controller.onScrollToBottom());

            if (ChatUtil.isCommonChat(model.getChatChannelDomain()) && model.isPublicChannel.get()) {
                placeholderTitle.setText(Res.get("chat.messagebox.noChats.placeholder.title"));
                placeholderDescription.setText(Res.get("chat.messagebox.noChats.placeholder.description",
                        model.getSelectedChannel().get().getDisplayString()));
            } else {
                placeholderTitle.setText("");
                placeholderDescription.setText("");
            }
        }

        @Override
        protected void onViewDetached() {
            scrollBar.ifPresent(scrollbar -> scrollbar.valueProperty().unbindBidirectional(model.getScrollValue()));
            model.scrollBarVisible.unbind();
            scrollDownBadge.textProperty().unbind();
            hasUnreadMessagesPin.unsubscribe();
            showScrolledDownButtonPin.unsubscribe();

            scrollDownBadge.setOnMouseClicked(null);
            if (fadeInScrollDownBadgeTimeline != null) {
                fadeInScrollDownBadgeTimeline.stop();
                fadeInScrollDownBadgeTimeline = null;
                scrollDownBadge.setOpacity(0);
            }
        }

        private void fadeInScrollDownBadge() {
            if (!Transitions.getUseAnimations()) {
                scrollDownBadge.setOpacity(1);
                return;
            }

            fadeInScrollDownBadgeTimeline = new Timeline();
            scrollDownBadge.setOpacity(0);
            ObservableList<KeyFrame> keyFrames = fadeInScrollDownBadgeTimeline.getKeyFrames();
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(scrollDownBadge.opacityProperty(), 0, Interpolator.LINEAR)
            ));
            // Add a delay before starting fade-in to deal with a render delay when adding a
            // list item.
            keyFrames.add(new KeyFrame(Duration.millis(100),
                    new KeyValue(scrollDownBadge.opacityProperty(), 0, Interpolator.EASE_OUT)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(400),
                    new KeyValue(scrollDownBadge.opacityProperty(), 1, Interpolator.EASE_OUT)
            ));
            fadeInScrollDownBadgeTimeline.play();
        }
    }
}
