package bisq.desktop.main.content.chat.message_container.list;

import bisq.bisq_easy.BisqEasyService;
import bisq.bisq_easy.NavigationTarget;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatChannelSelectionService;
import bisq.chat.ChatMessage;
import bisq.chat.ChatMessageType;
import bisq.chat.ChatService;
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
import bisq.chat.reactions.ChatMessageReaction;
import bisq.chat.reactions.Reaction;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.bisq_easy.BisqEasyServiceUtil;
import bisq.desktop.main.content.bisq_easy.take_offer.TakeOfferController;
import bisq.desktop.main.content.components.ReportToModeratorWindow;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.confidential.resend.ResendMessageService;
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
import javafx.scene.Scene;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class ChatMessagesListController implements bisq.desktop.common.view.Controller {
    private final ChatService chatService;
    private final UserIdentityService userIdentityService;
    private final UserProfileService userProfileService;
    private final ReputationService reputationService;
    private final SettingsService settingsService;
    private final Consumer<UserProfile> mentionUserHandler;
    private final Consumer<ChatMessage> replyHandler;
    private final Consumer<ChatMessage> showChatUserDetailsHandler;
    private final ChatMessagesListModel model;
    @Getter
    private final ChatMessagesListView view;
    private final ChatNotificationService chatNotificationService;
    private final BisqEasyTradeService bisqEasyTradeService;
    private final BannedUserService bannedUserService;
    private final NetworkService networkService;
    private final Optional<ResendMessageService> resendMessageService;
    private final BisqEasyService bisqEasyService;
    private final MarketPriceService marketPriceService;
    private Pin selectedChannelPin, chatMessagesPin;
    private Subscription selectedChannelSubscription, focusSubscription, scrollValuePin, scrollBarVisiblePin,
            layoutChildrenDonePin;

    public ChatMessagesListController(ServiceProvider serviceProvider,
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
        bisqEasyService = serviceProvider.getBisqEasyService();
        bisqEasyTradeService = serviceProvider.getTradeService().getBisqEasyTradeService();
        bannedUserService = serviceProvider.getUserService().getBannedUserService();
        networkService = serviceProvider.getNetworkService();
        resendMessageService = serviceProvider.getNetworkService().getResendMessageService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        this.mentionUserHandler = mentionUserHandler;
        this.showChatUserDetailsHandler = showChatUserDetailsHandler;
        this.replyHandler = replyHandler;

        model = new ChatMessagesListModel(userIdentityService, chatChannelDomain);
        view = new ChatMessagesListView(model, this);
    }

    @Override
    public void onActivate() {
        model.getSortedChatMessages().setComparator(ChatMessageListItem::compareTo);

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

        scrollBarVisiblePin = EasyBind.subscribe(model.getScrollBarVisible(), scrollBarVisible -> {
            if (scrollBarVisible != null && scrollBarVisible) {
                applyScrollValue(1);
            }
        });

        layoutChildrenDonePin = EasyBind.subscribe(model.getLayoutChildrenDone(), layoutChildrenDone -> {
            handleScrollValueChanged();
        });

        applyScrollValue(1);
    }

    @Override
    public void onDeactivate() {
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

        layoutChildrenDonePin.unsubscribe();
        scrollValuePin.unsubscribe();
        scrollBarVisiblePin.unsubscribe();

        model.getChatMessages().forEach(ChatMessageListItem::dispose);
        model.getChatMessages().clear();
        model.getChatMessageIds().clear();
    }

    private void selectedChannelChanged(ChatChannel<? extends ChatMessage> channel) {
        UIThread.run(() -> {
            model.getSelectedChannel().set(channel);
            model.getIsPublicChannel().set(channel instanceof PublicChatChannel);

            if (chatMessagesPin != null) {
                chatMessagesPin.unbind();
            }

            // Clear and call dispose on the current messages when we change the channel.
            model.getChatMessages().forEach(ChatMessageListItem::dispose);
            model.getChatMessages().clear();
            model.getChatMessageIds().clear();
            model.setAutoScrollToBottom(true);

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

                selectedChannelSubscription = EasyBind.subscribe(model.getSelectedChannel(),
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

    public void refreshMessages() {
        model.getChatMessages().setAll(new ArrayList<>(model.getChatMessages()));
        model.getChatMessageIds().clear();
        model.getChatMessageIds().addAll(model.getChatMessages().stream()
                .map(e -> e.getChatMessage().getId())
                .collect(Collectors.toSet()));
    }

    public void setSearchPredicate(Predicate<? super ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> predicate) {
        model.setSearchPredicate(Objects.requireNonNullElseGet(predicate, () -> e -> true));
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
                bisqEasyService,
                userIdentityService,
                userProfileService,
                bisqEasyOffer)) {
            if (bisqEasyOffer.getDirection().isSell()) {
                long makerAsSellersScore = userProfileService.findUserProfile(bisqEasyOffer.getMakersUserProfileId())
                        .map(reputationService::getReputationScore)
                        .map(ReputationScore::getTotalScore)
                        .orElse(0L);
                long myMinRequiredScore = bisqEasyService.getMinRequiredReputationScore().get();
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
            CommonPublicChatChannelService commonPublicChatChannelService = chatService.getCommonPublicChatChannelServices().get(model.getChatChannelDomain());
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

        UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity();
        if (chatMessage instanceof BisqEasyOfferbookMessage) {
            BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) chatMessage;
            chatService.getBisqEasyOfferbookChannelService().publishEditedChatMessage(bisqEasyOfferbookMessage, editedText, userIdentity);
        } else if (chatMessage instanceof CommonPublicChatMessage) {
            CommonPublicChatMessage commonPublicChatMessage = (CommonPublicChatMessage) chatMessage;
            chatService.getCommonPublicChatChannelServices().get(model.getChatChannelDomain()).publishEditedChatMessage(commonPublicChatMessage, editedText, userIdentity);
        }
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

    public void onReactMessage(ChatMessage chatMessage, Reaction reaction) {
        checkArgument(chatMessage instanceof CommonPublicChatMessage, "Not possible to react to a message which is not a CommonPublicChatMessage.");

        UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity();
        CommonPublicChatMessage commonPublicChatMessage = (CommonPublicChatMessage) chatMessage;
        Optional<ChatMessageReaction> chatMessageReaction = commonPublicChatMessage.getChatMessageReactions().stream()
                .filter(chatReaction -> Objects.equals(chatReaction.getUserProfileId(), userIdentity.getId())
                        && chatReaction.getReactionId() == reaction.ordinal())
                .findAny();

        if (chatMessageReaction.isPresent()) {
            chatService.getCommonPublicChatChannelServices().get(model.getChatChannelDomain())
                    .deleteChatMessageReaction(chatMessageReaction.get(), userIdentity.getNetworkIdWithKeyPair());
        } else {
            chatService.getCommonPublicChatChannelServices().get(model.getChatChannelDomain())
                    .publishChatMessageReaction(commonPublicChatMessage, reaction, userIdentity);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Scrolling
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void applyScrollValue(double scrollValue) {
        model.getScrollValue().set(scrollValue);
        handleScrollValueChanged();
    }

    private void handleScrollValueChanged() {
        double scrollValue = model.getScrollValue().get();
        model.getHasUnreadMessages().set(model.getNumReadMessages() < model.getChatMessages().size());
        boolean isAtBottom = scrollValue == 1d;
        model.getShowScrolledDownButton().set(!isAtBottom && model.getScrollBarVisible().get());
        model.setAutoScrollToBottom(isAtBottom);
        if (isAtBottom) {
            model.setNumReadMessages(model.getChatMessages().size());
        }

        int numUnReadMessages = model.getChatMessages().size() - model.getNumReadMessages();
        model.getNumUnReadMessages().set(numUnReadMessages > 0 ? String.valueOf(numUnReadMessages) : "");
    }

    private void maybeScrollDownOnNewItemAdded() {
        if (model.isAutoScrollToBottom()) {
            // The 100 ms delay is needed as when the item gets added to the listview it updates the scroll property
            // to a value < 1. After the render process is done we set it to 1.
            UIScheduler.run(() -> applyScrollValue(1)).after(100);
        } else {
            applyScrollValue(model.getScrollValue().get());
        }
    }

    void onScrollToBottom() {
        applyScrollValue(1);
    }

    public void highlightOfferChatMessage(@Nullable ChatMessage message) {
        model.getChatMessages().stream()
                .filter(ChatMessageListItem::isBisqEasyPublicChatMessageWithOffer)
                .forEach(item -> {
                    boolean shouldHighlightMessage = message != null && Objects.equals(item.getChatMessage(), message);
                    item.getShowHighlighted().set(shouldHighlightMessage);
                    if (shouldHighlightMessage) {
                        view.scrollToChatMessage(item);
                    }
                });
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
        Predicate<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> predicate = item -> {
            Optional<UserProfile> senderUserProfile = item.getSenderUserProfile();
            if (senderUserProfile.isEmpty()) {
                return false;
            }
            if (bannedUserService.isUserProfileBanned(item.getChatMessage().getAuthorUserProfileId()) ||
                    bannedUserService.isUserProfileBanned(senderUserProfile.get())) {
                return false;
            }
            // We do not display the take offer message as it has no text and is used only for sending the offer
            // to the peer and signalling the take offer event.
            if (item.getChatMessage().getChatMessageType() == ChatMessageType.TAKE_BISQ_EASY_OFFER) {
                return false;
            }

            return !userProfileService.getIgnoredUserProfileIds().contains(senderUserProfile.get().getId()) &&
                    userProfileService.findUserProfile(senderUserProfile.get().getId()).isPresent();
        };
        model.getFilteredChatMessages().setPredicate(item -> model.getSearchPredicate().test(item)
                && predicate.test(item));
    }

    private <M extends ChatMessage, C extends ChatChannel<M>> Pin bindChatMessages(C channel) {
        // We clear and fill the list at channel change. The addObserver triggers the add method for each item,
        // but as we have a contains() check there it will not have any effect.
        model.getChatMessages().setAll(channel.getChatMessages().stream()
                .map(chatMessage -> new ChatMessageListItem<>(chatMessage,
                        channel,
                        marketPriceService,
                        userProfileService,
                        reputationService,
                        bisqEasyTradeService,
                        userIdentityService,
                        networkService,
                        resendMessageService))
                .collect(Collectors.toSet()));
        model.getChatMessageIds().clear();
        model.getChatMessageIds().addAll(model.getChatMessages().stream()
                .map(e -> e.getChatMessage().getId())
                .collect(Collectors.toSet()));
        maybeScrollDownOnNewItemAdded();

        return channel.getChatMessages().addObserver(new CollectionObserver<>() {
            @Override
            public void add(M chatMessage) {
                UIThread.run(() -> {
                    if (model.getChatMessageIds().contains(chatMessage.getId())) {
                        return;
                    }
                    ChatMessageListItem<M, C> item = new ChatMessageListItem<>(chatMessage,
                            channel,
                            marketPriceService,
                            userProfileService,
                            reputationService,
                            bisqEasyTradeService,
                            userIdentityService,
                            networkService,
                            resendMessageService);
                    model.getChatMessages().add(item);
                    maybeScrollDownOnNewItemAdded();
                });
            }

            @Override
            public void remove(Object element) {
                if (element instanceof ChatMessage) {
                    UIThread.run(() -> {
                        ChatMessage chatMessage = (ChatMessage) element;
                        Optional<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> toRemove =
                                model.getChatMessages().stream()
                                        .filter(item -> item.getChatMessage().getId().equals(chatMessage.getId()))
                                        .findAny();
                        toRemove.ifPresent(item -> {
                            item.dispose();
                            model.getChatMessages().remove(item);
                            model.getChatMessageIds().remove(item.getChatMessage().getId());
                        });
                    });
                }
            }

            @Override
            public void clear() {
                UIThread.run(() -> {
                    model.getChatMessages().forEach(ChatMessageListItem::dispose);
                    model.getChatMessages().clear();
                    model.getChatMessageIds().clear();
                });
            }
        });
    }

    public String getUserName(String userProfileId) {
        return userProfileService.findUserProfile(userProfileId)
                .map(UserProfile::getUserName)
                .orElse(Res.get("data.na"));
    }

    public void onResendMessage(String messageId) {
        resendMessageService.ifPresent(service -> service.manuallyResendMessage(messageId));
    }

    public boolean canResendMessage(String messageId) {
        return resendMessageService.map(service -> service.canManuallyResendMessage(messageId)).orElse(false);
    }
}
