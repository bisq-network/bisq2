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

package bisq.desktop.primary.overlay.createOffer.review;

import bisq.application.DefaultApplicationService;
import bisq.chat.ChatService;
import bisq.chat.trade.TradeChannelSelectionService;
import bisq.chat.trade.pub.PublicTradeChannel;
import bisq.chat.trade.pub.PublicTradeChannelService;
import bisq.chat.message.ChatMessage;
import bisq.chat.trade.pub.PublicTradeChatMessage;
import bisq.chat.trade.pub.TradeChatOffer;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.components.ChatMessagesListView;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.offer.spec.Direction;
import bisq.settings.SettingsService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public class ReviewOfferController implements Controller {
    private final ReviewOfferModel model;
    @Getter
    private final ReviewOfferView view;
    private final ChatService chatService;
    private final ReputationService reputationService;
    private final ChatMessagesListView myOfferListView;
    private final ChatMessagesListView takersListView;
    private final Runnable closeHandler;
    private final SettingsService settingsService;
    private final UserIdentityService userIdentityService;
    private final PublicTradeChannelService publicTradeChannelService;
    private final UserProfileService userProfileService;
    private final TradeChannelSelectionService tradeChannelSelectionService;

    public ReviewOfferController(DefaultApplicationService applicationService,
                                 Consumer<Boolean> buttonsVisibleHandler,
                                 Runnable closeHandler) {
        chatService = applicationService.getChatService();
        publicTradeChannelService = chatService.getPublicTradeChannelService();
        tradeChannelSelectionService = chatService.getTradeChannelSelectionService();
        reputationService = applicationService.getUserService().getReputationService();
        settingsService = applicationService.getSettingsService();
        userIdentityService = applicationService.getUserService().getUserIdentityService();
        userProfileService = applicationService.getUserService().getUserProfileService();
        myOfferListView = new ChatMessagesListView(applicationService,
                mentionUser -> {
                },
                showChatUserDetails -> {
                },
                onReply -> {
                },
                false,
                true,
                false,
                false);

        takersListView = new ChatMessagesListView(applicationService,
                mentionUser -> {
                },
                showChatUserDetails -> {
                },
                onReply -> {
                },
                false,
                true,
                true,
                false);
        this.closeHandler = closeHandler;

        model = new ReviewOfferModel();
        view = new ReviewOfferView(model, this, myOfferListView.getRoot(), takersListView.getRoot());

        myOfferListView.setCreateOfferCompleteHandler(() -> {
            model.getShowCreateOfferSuccess().set(true);
            buttonsVisibleHandler.accept(false);
        });
        takersListView.setTakeOfferCompleteHandler(() -> {
            model.getShowTakeOfferSuccess().set(true);
            buttonsVisibleHandler.accept(false);
        });
        takersListView.getSortedChatMessages().setComparator(Comparator.comparing(ChatMessagesListView.ChatMessageListItem::getReputationScore));
    }

    public void setDirection(Direction direction) {
        model.setDirection(direction);
    }

    public void setMarket(Market market) {
        if (market != null) {
            model.setMarket(market);
        }
    }

    public void setPaymentMethods(List<String> paymentMethods) {
        if (paymentMethods != null) {
            model.setPaymentMethods(paymentMethods);
        }
    }

    public void setBaseSideAmount(Monetary monetary) {
        if (monetary != null) {
            model.setBaseSideAmount(monetary);
        }
    }

    public void setQuoteSideAmount(Monetary monetary) {
        if (monetary != null) {
            model.setQuoteSideAmount(monetary);
        }
    }

    @Override
    public void onActivate() {
        model.getShowCreateOfferSuccess().set(false);
        model.getShowTakeOfferSuccess().set(false);
        myOfferListView.getFilteredChatMessages().setPredicate(item -> item.getChatMessage().equals(model.getMyOfferMessage().get()));

        UserIdentity userIdentity = userIdentityService.getSelectedUserProfile().get();
        TradeChatOffer tradeChatOffer = new TradeChatOffer(model.getDirection(),
                model.getMarket(),
                model.getBaseSideAmount().getValue(),
                model.getQuoteSideAmount().getValue(),
                new HashSet<>(model.getPaymentMethods()),
                userIdentity.getUserProfile().getTerms(),
                settingsService.getRequiredTotalReputationScore());

        PublicTradeChannel publicTradeChannel = publicTradeChannelService.getChannels().stream()
                .filter(channel -> model.getMarket().equals(channel.getMarket()))
                .findAny()
                .orElseThrow();
        publicTradeChannelService.showChannel(publicTradeChannel);
        tradeChannelSelectionService.selectChannel(publicTradeChannel);

        PublicTradeChatMessage myOfferMessage = new PublicTradeChatMessage(publicTradeChannel.getId(),
                userIdentity.getUserProfile().getId(),
                Optional.of(tradeChatOffer),
                Optional.empty(),
                Optional.empty(),
                new Date().getTime(),
                false);
        model.getMyOfferMessage().set(myOfferMessage);
        myOfferListView.getChatMessages().clear();
        myOfferListView.getChatMessages().add(new ChatMessagesListView.ChatMessageListItem<>(myOfferMessage, userProfileService, reputationService));

        takersListView.getChatMessages().setAll(takersListView.getFilteredChatMessages().stream()
                .limit(3)
                .collect(Collectors.toList()));

        takersListView.getFilteredChatMessages().setPredicate(getTakeOfferPredicate());
        model.getMatchingOffersFound().set(!takersListView.getFilteredChatMessages().isEmpty());
    }

    @Override
    public void onDeactivate() {
        takersListView.getFilteredChatMessages().setPredicate(null);
        takersListView.getChatMessages().clear();
    }

    void onOpenBisqEasy() {
        close();
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_CHAT);
    }

    void onOpenPrivateChat() {
        close();
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_CHAT);
    }

    private void close() {
        closeHandler.run();
        OverlayController.hide();
        // If we got started from initial onboarding we are still at Splash screen, so we need to move to main
        Navigation.navigateTo(NavigationTarget.MAIN);
    }

    @SuppressWarnings("RedundantIfStatement")
    private Predicate<? super ChatMessagesListView.ChatMessageListItem<? extends ChatMessage>> getTakeOfferPredicate() {
        return item ->
        {
            if (item.getSender().isEmpty()) {
                return false;
            }
            UserProfile peer = item.getSender().get();
            if (userProfileService.isChatUserIgnored(peer)) {
                return false;
            }
            if (userIdentityService.isUserIdentityPresent(item.getChatMessage().getAuthorId())) {
                return false;
            }
            if (!(item.getChatMessage() instanceof PublicTradeChatMessage)) {
                return false;
            }
            if (((PublicTradeChatMessage) item.getChatMessage()).getTradeChatOffer().isEmpty()) {
                return false;
            }
            if (model.getMyOfferMessage().get() == null) {
                return false;
            }
            if (model.getMyOfferMessage().get().getTradeChatOffer().isEmpty()) {
                return false;
            }

            TradeChatOffer myChatOffer = model.getMyOfferMessage().get().getTradeChatOffer().get();
            TradeChatOffer peersOffer = ((PublicTradeChatMessage) item.getChatMessage()).getTradeChatOffer().get();

            if (peersOffer.getDirection().equals(myChatOffer.getDirection())) {
                return false;
            }

            if (!peersOffer.getMarket().equals(myChatOffer.getMarket())) {
                return false;
            }

            if (peersOffer.getBaseSideAmount() < myChatOffer.getBaseSideAmount()) {
                return false;
            }

            Set<String> paymentMethods = peersOffer.getPaymentMethods();
            if (myChatOffer.getPaymentMethods().stream().noneMatch(paymentMethods::contains)) {
                return false;
            }

            if (reputationService.findReputationScore(peer)
                    .map(reputationScore -> reputationScore.getTotalScore() < myChatOffer.getRequiredTotalReputationScore())
                    .orElse(true)) {
                return false;
            }

            return true;
        };
    }
}
