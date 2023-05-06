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
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.trade.channel.TradeChannelSelectionService;
import bisq.chat.trade.channel.priv.PrivateTradeChannelService;
import bisq.chat.trade.channel.pub.PublicTradeChannel;
import bisq.chat.trade.channel.pub.PublicTradeChannelService;
import bisq.chat.trade.message.PublicTradeChatMessage;
import bisq.chat.trade.message.TradeChatOffer;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.helpers.TakeOfferHelper;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.offer.spec.Direction;
import bisq.settings.SettingsService;
import bisq.support.MediationService;
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
    private final ReputationService reputationService;
    private final Runnable closeHandler;
    private final SettingsService settingsService;
    private final UserIdentityService userIdentityService;
    private final PublicTradeChannelService publicTradeChannelService;
    private final UserProfileService userProfileService;
    private final TradeChannelSelectionService tradeChannelSelectionService;
    private final Consumer<Boolean> buttonsVisibleHandler;
    private final PrivateTradeChannelService privateTradeChannelService;
    private final MediationService mediationService;

    public ReviewOfferController(DefaultApplicationService applicationService,
                                 Consumer<Boolean> buttonsVisibleHandler,
                                 Runnable closeHandler) {
        this.buttonsVisibleHandler = buttonsVisibleHandler;
        ChatService chatService = applicationService.getChatService();
        publicTradeChannelService = chatService.getPublicTradeChannelService();
        tradeChannelSelectionService = chatService.getTradeChannelSelectionService();
        reputationService = applicationService.getUserService().getReputationService();
        settingsService = applicationService.getSettingsService();
        userIdentityService = applicationService.getUserService().getUserIdentityService();
        userProfileService = applicationService.getUserService().getUserProfileService();
        privateTradeChannelService = chatService.getPrivateTradeChannelService();
        mediationService = applicationService.getSupportService().getMediationService();
        this.closeHandler = closeHandler;

        model = new ReviewOfferModel();
        view = new ReviewOfferView(model, this);
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

    public void setShowMatchingOffers(boolean showMatchingOffers) {
        model.setShowMatchingOffers(showMatchingOffers);
    }

    @Override
    public void onActivate() {
        PublicTradeChannel channel = publicTradeChannelService.findChannel(ChatChannelDomain.TRADE, PublicTradeChannel.getChannelName(model.getMarket())).orElseThrow();
        model.setSelectedChannel(channel);

        model.getShowCreateOfferSuccess().set(false);
        model.getShowTakeOfferSuccess().set(false);

        UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity().get();
        TradeChatOffer tradeChatOffer = new TradeChatOffer(StringUtils.createUid(),
                model.getDirection(),
                model.getMarket(),
                model.getBaseSideAmount().getValue(),
                model.getQuoteSideAmount().getValue(),
                new ArrayList<>(model.getPaymentMethods()),
                userIdentity.getUserProfile().getTerms(),
                settingsService.getRequiredTotalReputationScore().get());
        model.setMyOfferText(StringUtils.truncate(tradeChatOffer.getChatMessageText(), 100));

        publicTradeChannelService.showChannel(channel);
        tradeChannelSelectionService.selectChannel(channel);

        PublicTradeChatMessage myOfferMessage = new PublicTradeChatMessage(channel.getChannelName(),
                userIdentity.getUserProfile().getId(),
                Optional.of(tradeChatOffer),
                Optional.empty(),
                Optional.empty(),
                new Date().getTime(),
                false);
        model.setMyOfferMessage(myOfferMessage);

        model.getMatchingOffers().setAll(channel.getChatMessages().stream()
                .map(chatMessage -> new ReviewOfferView.ListItem(chatMessage, userProfileService, reputationService))
                .filter(getTakeOfferPredicate())
                .sorted(Comparator.comparing(ReviewOfferView.ListItem::getReputationScore))
                .limit(3)
                .collect(Collectors.toList()));

        model.getMatchingOffersFound().set(model.isShowMatchingOffers() && !model.getMatchingOffers().isEmpty());
    }

    @Override
    public void onDeactivate() {
    }

    void onTakeOffer(ReviewOfferView.ListItem listItem) {
        PublicTradeChatMessage chatMessage = listItem.getChatMessage();
        TakeOfferHelper.sendTakeOfferMessage(userProfileService, userIdentityService, mediationService, privateTradeChannelService, chatMessage).thenAccept(result -> UIThread.run(() -> {
            model.getShowTakeOfferSuccess().set(true);
            buttonsVisibleHandler.accept(false);
        }));
    }

    void onCreateOffer() {
        UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity().get();
        publicTradeChannelService.publishChatMessage(model.getMyOfferMessage(), userIdentity)
                .thenAccept(result -> UIThread.run(() -> {
                    model.getShowCreateOfferSuccess().set(true);
                    buttonsVisibleHandler.accept(false);
                }));
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
    private Predicate<? super ReviewOfferView.ListItem> getTakeOfferPredicate() {
        return item ->
        {
            if (item.getSenderUserProfile().isEmpty()) {
                return false;
            }
            UserProfile senderUserProfile = item.getSenderUserProfile().get();
            if (userProfileService.isChatUserIgnored(senderUserProfile)) {
                return false;
            }
            if (userProfileService.findUserProfile(item.getChatMessage().getAuthorId()).isEmpty()) {
                return false;
            }
            if (item.getChatMessage().getTradeChatOffer().isEmpty()) {
                return false;
            }
            if (model.getMyOfferMessage() == null) {
                return false;
            }
            if (model.getMyOfferMessage().getTradeChatOffer().isEmpty()) {
                return false;
            }

            TradeChatOffer myChatOffer = model.getMyOfferMessage().getTradeChatOffer().get();
            TradeChatOffer peersOffer = item.getChatMessage().getTradeChatOffer().get();

            if (peersOffer.getDirection().equals(myChatOffer.getDirection())) {
                return false;
            }

            if (!peersOffer.getMarket().equals(myChatOffer.getMarket())) {
                return false;
            }

            if (peersOffer.getQuoteSideAmount() < myChatOffer.getQuoteSideAmount()) {
                return false;
            }

            List<String> paymentMethods = peersOffer.getPaymentMethods();
            if (myChatOffer.getPaymentMethods().stream().noneMatch(paymentMethods::contains)) {
                return false;
            }

            //todo
           /* if (reputationService.getReputationScore(senderUserProfile).getTotalScore() < myChatOffer.getRequiredTotalReputationScore()) {
                return false;
            }*/

            return true;
        };
    }
}
