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

package bisq.desktop.primary.overlay.bisq_easy.create_offer.review;

import bisq.application.DefaultApplicationService;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.channel.BisqEasyChatChannelSelectionService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannelService;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannel;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannelService;
import bisq.chat.bisqeasy.message.BisqEasyPublicChatMessage;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelSelectionService;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.offer.Direction;
import bisq.offer.bisq_easy.BisqEasyOffer;
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

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class ReviewOfferController implements Controller {
    private final ReviewOfferModel model;
    @Getter
    private final ReviewOfferView view;
    private final ReputationService reputationService;
    private final Runnable resetHandler;
    private final SettingsService settingsService;
    private final UserIdentityService userIdentityService;
    private final BisqEasyPublicChatChannelService bisqEasyPublicChatChannelService;
    private final UserProfileService userProfileService;
    private final BisqEasyChatChannelSelectionService bisqEasyChatChannelSelectionService;
    private final Consumer<Boolean> mainButtonsVisibleHandler;
    private final BisqEasyPrivateTradeChatChannelService bisqEasyPrivateTradeChatChannelService;
    private final MediationService mediationService;
    private final ChatService chatService;

    public ReviewOfferController(DefaultApplicationService applicationService,
                                 Consumer<Boolean> mainButtonsVisibleHandler,
                                 Runnable resetHandler) {
        this.mainButtonsVisibleHandler = mainButtonsVisibleHandler;
        chatService = applicationService.getChatService();
        bisqEasyPublicChatChannelService = chatService.getBisqEasyPublicChatChannelService();
        bisqEasyChatChannelSelectionService = chatService.getBisqEasyChatChannelSelectionService();
        reputationService = applicationService.getUserService().getReputationService();
        settingsService = applicationService.getSettingsService();
        userIdentityService = applicationService.getUserService().getUserIdentityService();
        userProfileService = applicationService.getUserService().getUserProfileService();
        bisqEasyPrivateTradeChatChannelService = chatService.getBisqEasyPrivateTradeChatChannelService();
        mediationService = applicationService.getSupportService().getMediationService();
        this.resetHandler = resetHandler;

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

    public void setPaymentMethodNames(List<String> paymentMethodNames) {
        if (paymentMethodNames != null) {
            model.setPaymentMethodNames(paymentMethodNames);
        }
    }

    public void setBaseSideMinAmount(Monetary monetary) {
        if (monetary != null) {
            model.setBaseSideMinAmount(monetary);
        }
    }

    public void setBaseSideMaxAmount(Monetary monetary) {
        if (monetary != null) {
            model.setBaseSideMaxAmount(monetary);
        }
    }

    public void setQuoteSideMinAmount(Monetary monetary) {
        if (monetary != null) {
            model.setQuoteSideMinAmount(monetary);
        }
    }

    public void setQuoteSideMaxAmount(Monetary monetary) {
        if (monetary != null) {
            model.setQuoteSideMaxAmount(monetary);
        }
    }

    public void setShowMatchingOffers(boolean showMatchingOffers) {
        model.setShowMatchingOffers(showMatchingOffers);
    }

    public void setIsMinAmountEnabled(boolean isMinAmountEnabled) {
        model.setMinAmountEnabled(isMinAmountEnabled);
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        BisqEasyPublicChatChannel channel = bisqEasyPublicChatChannelService.findChannel(model.getMarket()).orElseThrow();
        model.setSelectedChannel(channel);

        model.getShowCreateOfferSuccess().set(false);
        model.getShowTakeOfferSuccess().set(false);

        UserIdentity userIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());

        // todo
        double sellerPremiumAsPercentage = 0.1;

        long baseSideMinAmount = model.getBaseSideMinAmount().getValue();
        long baseSideMaxAmount = model.getBaseSideMaxAmount().getValue();
        long quoteSideMinAmount = model.getQuoteSideMinAmount().getValue();
        long quoteSideMaxAmount = model.getQuoteSideMaxAmount().getValue();
        boolean isMinAmountEnabled = model.isMinAmountEnabled();

        BisqEasyOffer bisqEasyOffer = new BisqEasyOffer(StringUtils.createUid(),
                System.currentTimeMillis(),
                userIdentity.getUserProfile().getNetworkId(),
                model.getDirection(),
                model.getMarket(),
                isMinAmountEnabled,
                baseSideMinAmount,
                baseSideMaxAmount,
                quoteSideMinAmount,
                quoteSideMaxAmount,
                new ArrayList<>(model.getPaymentMethodNames()),
                userIdentity.getUserProfile().getTerms(),
                settingsService.getRequiredTotalReputationScore().get(),
                sellerPremiumAsPercentage);
        model.setMyOfferText(bisqEasyOffer.getChatMessageText());

        bisqEasyPublicChatChannelService.joinChannel(channel);
        bisqEasyChatChannelSelectionService.selectChannel(channel);

        BisqEasyPublicChatMessage myOfferMessage = new BisqEasyPublicChatMessage(channel.getId(),
                userIdentity.getUserProfile().getId(),
                Optional.of(bisqEasyOffer),
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

        model.getMatchingOffersVisible().set(model.isShowMatchingOffers() && !model.getMatchingOffers().isEmpty());
    }

    @Override
    public void onDeactivate() {
    }

    void onTakeOffer(ReviewOfferView.ListItem listItem) {
        BisqEasyPublicChatMessage chatMessage = listItem.getChatMessage();
        Optional<UserProfile> mediator = mediationService.takerSelectMediator(chatMessage);
        BisqEasyPrivateTradeChatChannelService bisqEasyPrivateTradeChatChannelService = chatService.getBisqEasyPrivateTradeChatChannelService();
        ChatChannelSelectionService chatChannelSelectionService = chatService.getChatChannelSelectionService(ChatChannelDomain.BISQ_EASY);
        bisqEasyPrivateTradeChatChannelService.sendTakeOfferMessage(chatMessage, mediator)
                .thenAccept(result -> UIThread.run(() -> {
                    bisqEasyPrivateTradeChatChannelService.findChannel(chatMessage.getBisqEasyOffer().orElseThrow())
                            .ifPresent(chatChannelSelectionService::selectChannel);
                    model.getShowTakeOfferSuccess().set(true);
                    mainButtonsVisibleHandler.accept(false);
                }));
    }

    void onCreateOffer() {
        UserIdentity userIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
        bisqEasyPublicChatChannelService.publishChatMessage(model.getMyOfferMessage(), userIdentity)
                .thenAccept(result -> UIThread.run(() -> {
                    model.getShowCreateOfferSuccess().set(true);
                    mainButtonsVisibleHandler.accept(false);
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
        resetHandler.run();
        OverlayController.hide();
        // If we got started from initial onboarding we are still at Splash screen, so we need to move to main
        Navigation.navigateTo(NavigationTarget.MAIN);
    }

    @SuppressWarnings("RedundantIfStatement")
    private Predicate<? super ReviewOfferView.ListItem> getTakeOfferPredicate() {
        return item ->
        {
            if (item.getAuthorUserProfileId().isEmpty()) {
                return false;
            }
            UserProfile authorUserProfile = item.getAuthorUserProfileId().get();
            if (userProfileService.isChatUserIgnored(authorUserProfile)) {
                return false;
            }
            if (userIdentityService.getUserIdentities().stream()
                    .map(userIdentity -> userIdentity.getUserProfile().getId())
                    .anyMatch(userProfileId -> userProfileId.equals(authorUserProfile.getId()))) {
                return false;
            }
            if (item.getChatMessage().getBisqEasyOffer().isEmpty()) {
                return false;
            }
            if (model.getMyOfferMessage() == null) {
                return false;
            }
            if (model.getMyOfferMessage().getBisqEasyOffer().isEmpty()) {
                return false;
            }

            BisqEasyOffer myChatOffer = model.getMyOfferMessage().getBisqEasyOffer().get();
            BisqEasyOffer peersOffer = item.getChatMessage().getBisqEasyOffer().get();

            if (peersOffer.getDirection().equals(myChatOffer.getDirection())) {
                return false;
            }

            if (!peersOffer.getMarket().equals(myChatOffer.getMarket())) {
                return false;
            }

            if (myChatOffer.getQuoteSideMinAmount().getValue() > peersOffer.getQuoteSideMaxAmount().getValue()) {
                return false;
            }

            if (myChatOffer.getQuoteSideMaxAmount().getValue() < peersOffer.getQuoteSideMinAmount().getValue()) {
                return false;
            }

            List<String> paymentMethods = peersOffer.getQuoteSideSettlementMethodNames();
            if (myChatOffer.getQuoteSideSettlementMethodNames().stream().noneMatch(paymentMethods::contains)) {
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
