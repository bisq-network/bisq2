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

package bisq.desktop.main.content.chat.message_container.list;

import bisq.bisq_easy.BisqEasySellersReputationBasedTradeAmountService;
import bisq.bisq_easy.BisqEasyTradeAmountLimits;
import bisq.bisq_easy.NavigationTarget;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.*;
import bisq.chat.bisq_easy.BisqEasyOfferMessage;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.CommonPublicChatChannelService;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.notifications.ChatNotificationService;
import bisq.chat.priv.LeavePrivateChatManager;
import bisq.chat.priv.PrivateChatChannel;
import bisq.chat.priv.PrivateChatMessage;
import bisq.chat.pub.PublicChatChannel;
import bisq.chat.pub.PublicChatMessage;
import bisq.chat.reactions.*;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.bisq_easy.take_offer.TakeOfferController;
import bisq.desktop.main.content.components.ReportToModeratorWindow;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.confidential.resend.ResendMessageService;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.spec.RangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
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

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static bisq.chat.ChatMessageType.TAKE_BISQ_EASY_OFFER;
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
    private final MarketPriceService marketPriceService;
    private final BisqEasySellersReputationBasedTradeAmountService bisqEasySellersReputationBasedTradeAmountService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final LeavePrivateChatManager leavePrivateChatManager;
    private Pin selectedChannelPin, chatMessagesPin, bisqEasyOfferbookMessageTypeFilterPin, highlightedMessagePin;
    private Subscription selectedChannelSubscription, focusSubscription, scrollValuePin, scrollBarVisiblePin,
            layoutChildrenDonePin;

    public ChatMessagesListController(ServiceProvider serviceProvider,
                                      Consumer<UserProfile> mentionUserHandler,
                                      Consumer<ChatMessage> showChatUserDetailsHandler,
                                      Consumer<ChatMessage> replyHandler,
                                      ChatChannelDomain chatChannelDomain) {
        chatService = serviceProvider.getChatService();
        chatNotificationService = chatService.getChatNotificationService();
        leavePrivateChatManager = chatService.getLeavePrivateChatManager();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        reputationService = serviceProvider.getUserService().getReputationService();
        settingsService = serviceProvider.getSettingsService();
        bisqEasyTradeService = serviceProvider.getTradeService().getBisqEasyTradeService();
        bannedUserService = serviceProvider.getUserService().getBannedUserService();
        networkService = serviceProvider.getNetworkService();
        resendMessageService = serviceProvider.getNetworkService().getResendMessageService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        bisqEasySellersReputationBasedTradeAmountService = serviceProvider.getBisqEasyService().getBisqEasySellersReputationBasedTradeAmountService();
        authorizedBondedRolesService = serviceProvider.getBondedRolesService().getAuthorizedBondedRolesService();

        this.mentionUserHandler = mentionUserHandler;
        this.showChatUserDetailsHandler = showChatUserDetailsHandler;
        this.replyHandler = replyHandler;

        model = new ChatMessagesListModel(userIdentityService, chatChannelDomain);
        view = new ChatMessagesListView(model, this);
    }

    @Override
    public void onActivate() {
        model.getSortedChatMessages().setComparator(ChatMessageListItem::compareTo);

        bisqEasyOfferbookMessageTypeFilterPin = settingsService.getBisqEasyOfferbookMessageTypeFilter()
                .addObserver(filter -> UIThread.run(this::applyPredicate));

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

        layoutChildrenDonePin = EasyBind.subscribe(model.getLayoutChildrenDone(), layoutChildrenDone -> handleScrollValueChanged());

        applyScrollValue(1);
    }

    @Override
    public void onDeactivate() {
        if (bisqEasyOfferbookMessageTypeFilterPin != null) {
            bisqEasyOfferbookMessageTypeFilterPin.unbind();
        }
        if (selectedChannelPin != null) {
            selectedChannelPin.unbind();
            selectedChannelPin = null;
        }
        if (chatMessagesPin != null) {
            chatMessagesPin.unbind();
            chatMessagesPin = null;
        }
        if (highlightedMessagePin != null) {
            highlightedMessagePin.unbind();
            highlightedMessagePin = null;
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

            if (channel instanceof BisqEasyOfferbookChannel bisqEasyOfferbookChannel) {
                chatMessagesPin = bindChatMessages(bisqEasyOfferbookChannel);
            } else if (channel instanceof BisqEasyOpenTradeChannel bisqEasyOpenTradeChannel) {
                chatMessagesPin = bindChatMessages(bisqEasyOpenTradeChannel);
            } else if (channel instanceof CommonPublicChatChannel commonPublicChatChannel) {
                chatMessagesPin = bindChatMessages(commonPublicChatChannel);
            } else if (channel instanceof TwoPartyPrivateChatChannel twoPartyPrivateChatChannel) {
                chatMessagesPin = bindChatMessages(twoPartyPrivateChatChannel);
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
                                    chatNotificationService.consume(model.getSelectedChannel().get());
                                }
                            });
                }

                selectedChannelSubscription = EasyBind.subscribe(model.getSelectedChannel(),
                        selectedChannel -> {
                            if (selectedChannel != null) {
                                chatNotificationService.consume(model.getSelectedChannel().get());
                            }
                        });
            }
        });

        if (channel instanceof PublicChatChannel<?> publicChatChannel) {
            if (highlightedMessagePin != null) {
                highlightedMessagePin.unbind();
            }
            highlightedMessagePin = publicChatChannel.getHighlightedMessage().addObserver(highlightedMessage -> {
                if (highlightedMessage != null) {
                    model.getChatMessages()
                            .forEach(item -> {
                                boolean shouldHighlightMessage = item.getChatMessage().getId().equals(highlightedMessage.getId());
                                item.getShowHighlighted().set(shouldHighlightMessage);
                                if (shouldHighlightMessage) {
                                    // Need to delay this action otherwise the scroll to bottom functionality cancels this out
                                    UIScheduler.run(() -> view.scrollToChatMessage(item)).after(110);
                                }
                            });
                }
            });
        }
    }


    /* --------------------------------------------------------------------- */
    // API - called from client
    /* --------------------------------------------------------------------- */

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


    /* --------------------------------------------------------------------- */
    // UI - delegate to client
    /* --------------------------------------------------------------------- */

    public void onMention(UserProfile userProfile) {
        mentionUserHandler.accept(userProfile);
    }

    public void onShowChatUserDetails(ChatMessage chatMessage) {
        showChatUserDetailsHandler.accept(chatMessage);
    }

    public void onReply(ChatMessage chatMessage) {
        replyHandler.accept(chatMessage);
    }


    /* --------------------------------------------------------------------- */
    // UI - handler
    /* --------------------------------------------------------------------- */

    public void onTakeOffer(BisqEasyOfferbookMessage bisqEasyOfferbookMessage) {
        checkArgument(bisqEasyOfferbookMessage.getBisqEasyOffer().isPresent(), "message must contain offer");
        checkArgument(!model.isMyMessage(bisqEasyOfferbookMessage), "tradeChatMessage must not be mine");

        UserProfile userProfile = userIdentityService.getSelectedUserIdentity().getUserProfile();

        if (bannedUserService.isUserProfileBanned(bisqEasyOfferbookMessage.getAuthorUserProfileId()) ||
                bannedUserService.isUserProfileBanned(userProfile)) {
            return;
        }

        NetworkId takerNetworkId = userProfile.getNetworkId();
        BisqEasyOffer bisqEasyOffer = bisqEasyOfferbookMessage.getBisqEasyOffer().get();
        String tradeId = Trade.createId(bisqEasyOffer.getId(), takerNetworkId.getId());
        if (bisqEasyTradeService.tradeExists(tradeId)) {
            new Popup().information(Res.get("chat.message.offer.offerAlreadyTaken.warn")).show();
            return;
        }

        Optional<Long> requiredReputationScoreForMaxOrFixedAmount = BisqEasyTradeAmountLimits.findRequiredReputationScoreForMaxOrFixedAmount(marketPriceService, bisqEasyOffer);
        Optional<Long> requiredReputationScoreForMinAmount = BisqEasyTradeAmountLimits.findRequiredReputationScoreForMinAmount(marketPriceService, bisqEasyOffer);
        if (requiredReputationScoreForMaxOrFixedAmount.isPresent()) {
            long requiredReputationScoreForMaxOrFixed = requiredReputationScoreForMaxOrFixedAmount.get();
            long requiredReputationScoreForMinOrFixed = requiredReputationScoreForMinAmount.orElse(requiredReputationScoreForMaxOrFixed);
            String minFiatAmount = OfferAmountFormatter.formatQuoteSideMinOrFixedAmount(marketPriceService, bisqEasyOffer, true);
            String maxFiatAmount = OfferAmountFormatter.formatQuoteSideMaxOrFixedAmount(marketPriceService, bisqEasyOffer, true);
            boolean isAmountRangeOffer = bisqEasyOffer.getAmountSpec() instanceof RangeAmountSpec;
            long sellersScore;
            if (bisqEasyOffer.getTakersDirection().isBuy()) {
                // I am as taker the buyer. We check if seller has the required reputation
                sellersScore = userProfileService.findUserProfile(bisqEasyOffer.getMakersUserProfileId())
                        .map(reputationService::getReputationScore)
                        .map(ReputationScore::getTotalScore)
                        .orElse(0L);
                boolean canBuyerTakeOffer = sellersScore >= requiredReputationScoreForMinOrFixed;
                if (!canBuyerTakeOffer) {
                    new Popup()
                            .headline(Res.get("chat.message.takeOffer.buyer.invalidOffer.headline"))
                            .warning(Res.get(isAmountRangeOffer
                                            ? "chat.message.takeOffer.buyer.invalidOffer.rangeAmount.text"
                                            : "chat.message.takeOffer.buyer.invalidOffer.fixedAmount.text",
                                    sellersScore,
                                    isAmountRangeOffer ? requiredReputationScoreForMinOrFixed : requiredReputationScoreForMaxOrFixed,
                                    isAmountRangeOffer ? minFiatAmount : maxFiatAmount))
                            .show();
                } else {
                    Navigation.navigateTo(NavigationTarget.TAKE_OFFER, new TakeOfferController.InitData(bisqEasyOffer));
                }
            } else {
                //  I am as taker the seller. We check if my reputation permits to take the offer
                sellersScore = reputationService.getReputationScore(userIdentityService.getSelectedUserIdentity().getUserProfile()).getTotalScore();
                boolean canSellerTakeOffer = sellersScore >= requiredReputationScoreForMinOrFixed;
                if (!canSellerTakeOffer) {
                    new Popup()
                            .headline(Res.get("chat.message.takeOffer.seller.insufficientScore.headline"))
                            .warning(Res.get(isAmountRangeOffer
                                            ? "chat.message.takeOffer.seller.insufficientScore.rangeAmount.warning"
                                            : "chat.message.takeOffer.seller.insufficientScore.fixedAmount.warning",
                                    sellersScore,
                                    isAmountRangeOffer ? requiredReputationScoreForMinOrFixed : requiredReputationScoreForMaxOrFixed,
                                    isAmountRangeOffer ? minFiatAmount : maxFiatAmount))
                            .onAction(() -> Navigation.navigateTo(NavigationTarget.BUILD_REPUTATION))
                            .actionButtonText(Res.get("bisqEasy.offerbook.offerList.popup.offersWithInsufficientReputationWarning.buildReputation"))
                            .show();
                } else {
                    Navigation.navigateTo(NavigationTarget.TAKE_OFFER, new TakeOfferController.InitData(bisqEasyOffer));
                }
            }
        } else {
            log.warn("requiredReputationScoreForMaxOrFixedAmount is not present. requiredReputationScoreForMaxOrFixedAmount={}; requiredReputationScoreForMinAmount={}",
                    requiredReputationScoreForMaxOrFixedAmount, requiredReputationScoreForMinAmount);
        }
    }

    public void onDeleteMessage(ChatMessage chatMessage) {
        String authorUserProfileId = chatMessage.getAuthorUserProfileId();
        userIdentityService.findUserIdentity(authorUserProfileId)
                .ifPresent(authorUserIdentity -> {
                    if (authorUserIdentity.equals(userIdentityService.getSelectedUserIdentity())) {
                        boolean isBisqEasyPublicChatMessageWithOffer =
                                chatMessage instanceof BisqEasyOfferbookMessage
                                        && ((BisqEasyOfferMessage) chatMessage).hasBisqEasyOffer();
                        new Popup().warning(isBisqEasyPublicChatMessageWithOffer
                                        ? Res.get("bisqEasy.offerbook.chatMessage.deleteOffer.confirmation")
                                        : Res.get("bisqEasy.offerbook.chatMessage.deleteMessage.confirmation"))
                                .actionButtonText(Res.get("confirmation.yes"))
                                .onAction(() -> doDeleteMessage(chatMessage, authorUserIdentity))
                                .closeButtonText(Res.get("confirmation.no"))
                                .show();
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

        if (chatMessage instanceof BisqEasyOfferbookMessage bisqEasyOfferbookMessage) {
            chatService.getBisqEasyOfferbookChannelService().deleteChatMessage(bisqEasyOfferbookMessage, userIdentity.getNetworkIdWithKeyPair())
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.error("We got an error at doDeleteMessage", throwable);
                        }
                    });
        } else if (chatMessage instanceof CommonPublicChatMessage commonPublicChatMessage) {
            CommonPublicChatChannelService commonPublicChatChannelService = chatService.getCommonPublicChatChannelServices().get(model.getChatChannelDomain());
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
        if (chatMessage instanceof BisqEasyOfferbookMessage bisqEasyOfferbookMessage) {
            chatService.getBisqEasyOfferbookChannelService().publishEditedChatMessage(bisqEasyOfferbookMessage, editedText, userIdentity);
        } else if (chatMessage instanceof CommonPublicChatMessage commonPublicChatMessage) {
            chatService.getCommonPublicChatChannelServices().get(model.getChatChannelDomain()).publishEditedChatMessage(commonPublicChatMessage, editedText, userIdentity);
        }
    }

    public void onReportUser(ChatMessage chatMessage) {
        if (chatMessage instanceof PrivateChatMessage<?> privateChatMessage) {
            Navigation.navigateTo(NavigationTarget.REPORT_TO_MODERATOR,
                    new ReportToModeratorWindow.InitData(privateChatMessage.getSenderUserProfile()));
        } else {
            userProfileService.findUserProfile(chatMessage.getAuthorUserProfileId())
                    .ifPresent(accusedUserProfile -> Navigation.navigateTo(NavigationTarget.REPORT_TO_MODERATOR,
                            new ReportToModeratorWindow.InitData(accusedUserProfile)));
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
        ClipboardUtil.copyToClipboard(chatMessage.getTextOrNA());
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

        leavePrivateChatManager.leaveChannel((PrivateChatChannel<?>) chatChannel);
    }

    public void onReactMessage(ChatMessage chatMessage, Reaction reaction, ChatChannel<?> chatChannel) {
        checkArgument(chatMessage.canShowReactions(), "Not possible to react to a message of type %s.", chatMessage.getClass());

        UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity();
        Optional<ChatMessageReaction> chatMessageReaction = chatMessage.getChatMessageReactions().stream()
                .filter(chatReaction -> Objects.equals(chatReaction.getUserProfileId(), userIdentity.getId())
                        && chatReaction.getReactionId() == reaction.ordinal())
                .findAny();

        if (chatMessage instanceof PrivateChatMessage) {
            publishPrivateChatMessageReaction(chatMessage, chatChannel, reaction, chatMessageReaction);
        } else {
            chatMessageReaction.ifPresentOrElse(
                    messageReaction -> deleteChatMessageReaction(messageReaction, userIdentity),
                    () -> publishChatMessageReaction(chatMessage, reaction, userIdentity));
        }
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

    public void onLearnMoreAboutChatRules() {
        Navigation.navigateTo(NavigationTarget.CHAT_RULES);
    }


    /* --------------------------------------------------------------------- */
    // Scrolling
    /* --------------------------------------------------------------------- */

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


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void createAndSelectTwoPartyPrivateChatChannel(UserProfile peer) {
        // Private chats are all using the DISCUSSION ChatChannelDomain
        chatService.createAndSelectTwoPartyPrivateChatChannel(ChatChannelDomain.DISCUSSION, peer)
                .ifPresent(channel -> Navigation.navigateTo(NavigationTarget.CHAT_PRIVATE));
    }

    private void applyPredicate() {
        Predicate<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> predicate = item -> {
            Optional<UserProfile> senderUserProfile = item.getSenderUserProfile();
            if (senderUserProfile.isEmpty()) {
                return false;
            }
            ChatMessage chatMessage = item.getChatMessage();
            if (bannedUserService.isUserProfileBanned(chatMessage.getAuthorUserProfileId()) ||
                    bannedUserService.isUserProfileBanned(senderUserProfile.get())) {
                return false;
            }

            boolean messageTypePredicate = true; // messageTypeFilter == bisq.settings.ChatMessageType.ALL
            if (chatMessage instanceof BisqEasyOfferbookMessage bisqEasyOfferbookMessage) {
                bisq.settings.ChatMessageType messageTypeFilter = settingsService.getBisqEasyOfferbookMessageTypeFilter().get();
                if (messageTypeFilter == bisq.settings.ChatMessageType.TEXT) {
                    messageTypePredicate = !bisqEasyOfferbookMessage.hasBisqEasyOffer();
                } else if (messageTypeFilter == bisq.settings.ChatMessageType.OFFER) {
                    messageTypePredicate = bisqEasyOfferbookMessage.hasBisqEasyOffer();
                }
            }

            // We do not display the take offer message as it has no text and is used only for sending the offer
            // to the peer and signalling the take offer event.
            if (chatMessage.getChatMessageType() == ChatMessageType.TAKE_BISQ_EASY_OFFER) {
                return false;
            }

            if (!chatMessage.isMyMessage(userIdentityService) &&
                    !bisqEasySellersReputationBasedTradeAmountService.hasSellerSufficientReputation(chatMessage)) {
                return false;
            }

            return messageTypePredicate
                    && !userProfileService.getIgnoredUserProfileIds().contains(senderUserProfile.get().getId())
                    && userProfileService.findUserProfile(senderUserProfile.get().getId()).isPresent();
        };
        model.getFilteredChatMessages().setPredicate(item -> model.getSearchPredicate().test(item) &&
                predicate.test(item));
    }

    private <M extends ChatMessage, C extends ChatChannel<M>> Pin bindChatMessages(C channel) {
        // We clear and fill the list at channel change. The addObserver triggers the add method for each item,
        // but as we have a contains() check there it will not have any effect.
        model.getChatMessages().addAll(channel.getChatMessages().stream()
                .filter(chatMessage -> chatMessage.getChatMessageType() != TAKE_BISQ_EASY_OFFER)
                .map(chatMessage -> new ChatMessageListItem<>(chatMessage,
                        channel,
                        marketPriceService,
                        userProfileService,
                        reputationService,
                        bisqEasyTradeService,
                        userIdentityService,
                        networkService,
                        resendMessageService,
                        authorizedBondedRolesService))
                .collect(Collectors.toSet()));
        addChatRulesWarningChatMessageListItemInPrivateChats(channel);

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
                    if (chatMessage.getChatMessageType() == TAKE_BISQ_EASY_OFFER) {
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
                            resendMessageService,
                            authorizedBondedRolesService);
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

    private void publishChatMessageReaction(ChatMessage chatMessage, Reaction reaction, UserIdentity userIdentity) {
        if (chatMessage instanceof CommonPublicChatMessage) {
            chatService.getCommonPublicChatChannelServices().get(model.getChatChannelDomain())
                    .publishChatMessageReaction((CommonPublicChatMessage) chatMessage, reaction, userIdentity);
        } else if (chatMessage instanceof BisqEasyOfferbookMessage) {
            chatService.getBisqEasyOfferbookChannelService()
                    .publishChatMessageReaction((BisqEasyOfferbookMessage) chatMessage, reaction, userIdentity);
        }
    }

    private void publishPrivateChatMessageReaction(ChatMessage chatMessage,
                                                   ChatChannel<?> chatChannel,
                                                   Reaction reaction,
                                                   Optional<ChatMessageReaction> messageReaction) {
        boolean isRemoved;
        if (messageReaction.isPresent() &&
                messageReaction.get() instanceof PrivateChatMessageReaction privateChatReaction) {
            isRemoved = !privateChatReaction.isRemoved();
        } else {
            isRemoved = false;
        }

        if (chatMessage instanceof TwoPartyPrivateChatMessage) {
            checkArgument(chatChannel instanceof TwoPartyPrivateChatChannel, "Channel needs to be of type TwoPartyPrivateChatChannel.");
            TwoPartyPrivateChatChannel channel = (TwoPartyPrivateChatChannel) chatChannel;
            ChatChannelDomain chatChannelDomain = model.getChatChannelDomain();
            chatService.findTwoPartyPrivateChatChannelService(chatChannelDomain).ifPresent(service ->
                    service.sendTextMessageReaction((TwoPartyPrivateChatMessage) chatMessage, channel, reaction, isRemoved));
        } else if (chatMessage instanceof BisqEasyOpenTradeMessage) {
            checkArgument(chatChannel instanceof BisqEasyOpenTradeChannel, "Channel needs to be of type BisqEasyOpenTradeChannel.");
            BisqEasyOpenTradeChannel channel = (BisqEasyOpenTradeChannel) chatChannel;
            chatService.getBisqEasyOpenTradeChannelService()
                    .sendTextMessageReaction((BisqEasyOpenTradeMessage) chatMessage, channel, reaction, isRemoved);
        }
    }

    private void deleteChatMessageReaction(ChatMessageReaction messageReaction, UserIdentity userIdentity) {
        if (messageReaction instanceof CommonPublicChatMessageReaction) {
            chatService.getCommonPublicChatChannelServices().get(model.getChatChannelDomain())
                    .deleteChatMessageReaction((CommonPublicChatMessageReaction) messageReaction, userIdentity.getNetworkIdWithKeyPair());
        } else if (messageReaction instanceof BisqEasyOfferbookMessageReaction) {
            chatService.getBisqEasyOfferbookChannelService()
                    .deleteChatMessageReaction((BisqEasyOfferbookMessageReaction) messageReaction, userIdentity.getNetworkIdWithKeyPair());
        }
    }

    private <M extends ChatMessage, C extends ChatChannel<M>> void addChatRulesWarningChatMessageListItemInPrivateChats(C channel) {
        if (channel instanceof TwoPartyPrivateChatChannel twoPartyPrivateChatChannel) {
            TwoPartyPrivateChatMessage twoPartyPrivateChatMessage = createChatRulesWarningMessage(twoPartyPrivateChatChannel);
            model.getChatMessages().add(createChatMessageListItem(twoPartyPrivateChatMessage, twoPartyPrivateChatChannel));
        } else if (channel instanceof BisqEasyOpenTradeChannel bisqEasyOpenTradeChannel) {
            BisqEasyOpenTradeMessage bisqEasyOpenTradeMessage = createChatRulesWarningMessage(bisqEasyOpenTradeChannel);
            model.getChatMessages().add(createChatMessageListItem(bisqEasyOpenTradeMessage, bisqEasyOpenTradeChannel));
        }
    }

    private TwoPartyPrivateChatMessage createChatRulesWarningMessage(TwoPartyPrivateChatChannel channel) {
        UserProfile receiverUserProfile = channel.getMyUserIdentity().getUserProfile();
        UserProfile senderUserProfile = channel.getPeer();
        String text = Res.get("chat.private.chatRulesWarningMessage.text1");
        return new TwoPartyPrivateChatMessage(StringUtils.createUid(),
                channel.getChatChannelDomain(),
                channel.getId(),
                senderUserProfile,
                receiverUserProfile.getId(),
                receiverUserProfile.getNetworkId(),
                text,
                Optional.empty(),
                0L,
                false,
                ChatMessageType.CHAT_RULES_WARNING,
                new HashSet<>());
    }

    private BisqEasyOpenTradeMessage createChatRulesWarningMessage(BisqEasyOpenTradeChannel channel) {
        UserProfile receiverUserProfile = channel.getMyUserIdentity().getUserProfile();
        UserProfile senderUserProfile = channel.getPeer();
        String text = Res.get("chat.private.chatRulesWarningMessage.text1");
        return new BisqEasyOpenTradeMessage(channel.getTradeId(),
                StringUtils.createUid(),
                channel.getId(),
                senderUserProfile,
                receiverUserProfile.getId(),
                receiverUserProfile.getNetworkId(),
                text,
                Optional.empty(),
                0L,
                false,
                channel.getMediator(),
                ChatMessageType.CHAT_RULES_WARNING,
                Optional.empty(),
                new HashSet<>());
    }

    private <M extends ChatMessage, C extends ChatChannel<M>> ChatMessageListItem<M, C> createChatMessageListItem(M message, C channel) {
        return new ChatMessageListItem<>(message,
                        channel,
                        marketPriceService,
                        userProfileService,
                        reputationService,
                        bisqEasyTradeService,
                        userIdentityService,
                        networkService,
                        Optional.empty(),
                        authorizedBondedRolesService);
    }
}
