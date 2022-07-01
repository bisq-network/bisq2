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
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.components.ChatMessagesListView;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.offer.spec.Direction;
import bisq.settings.SettingsService;
import bisq.social.chat.ChatService;
import bisq.social.chat.channels.PublicTradeChannel;
import bisq.social.chat.messages.ChatMessage;
import bisq.social.chat.messages.PublicTradeChatMessage;
import bisq.social.offer.TradeChatOffer;
import bisq.social.user.ChatUser;
import bisq.social.user.ChatUserIdentity;
import bisq.social.user.reputation.ReputationService;
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

    public ReviewOfferController(DefaultApplicationService applicationService,
                                 Consumer<Boolean> buttonsVisibleHandler,
                                 Runnable closeHandler) {
        chatService = applicationService.getSocialService().getChatService();
        reputationService = applicationService.getSocialService().getReputationService();
        settingsService = applicationService.getSettingsService();

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

        ChatUserIdentity chatUserIdentity = chatService.getChatUserService().getSelectedChatUserIdentity().get();
        TradeChatOffer tradeChatOffer = new TradeChatOffer(model.getDirection(),
                model.getMarket(),
                model.getBaseSideAmount().getValue(),
                model.getQuoteSideAmount().getValue(),
                new HashSet<>(model.getPaymentMethods()),
                chatUserIdentity.getChatUser().getTerms(),
                settingsService.getRequiredTotalReputationScore());

        PublicTradeChannel channelForMarket = chatService.getPublicTradeChannels().stream()
                .filter(publicTradeChannel -> model.getMarket().equals(publicTradeChannel.getMarket().orElse(null)))
                .findAny()
                .orElseThrow();
        channelForMarket.setVisible(true);
        chatService.selectTradeChannel(channelForMarket);

        PublicTradeChatMessage myOfferMessage = new PublicTradeChatMessage(channelForMarket.getId(),
                chatUserIdentity.getChatUser().getId(),
                Optional.of(tradeChatOffer),
                Optional.empty(),
                Optional.empty(),
                new Date().getTime(),
                false);
        model.getMyOfferMessage().set(myOfferMessage);
        myOfferListView.getChatMessages().clear();
        myOfferListView.getChatMessages().add(new ChatMessagesListView.ChatMessageListItem<>(myOfferMessage, chatService, reputationService));

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
            if (item.getAuthor().isEmpty()) {
                return false;
            }
            ChatUser peer = item.getAuthor().get();
            if (chatService.isChatUserIgnored(peer)) {
                return false;
            }
            if (chatService.isMyMessage(item.getChatMessage())) {
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
