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

package bisq.desktop.primary.overlay.bisq_easy.take_offer.review;

import bisq.application.DefaultApplicationService;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.channel.BisqEasyChatChannelSelectionService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannelService;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannelService;
import bisq.common.monetary.Monetary;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.presentation.formatters.AmountFormatter;
import bisq.security.KeyPairService;
import bisq.settings.SettingsService;
import bisq.support.MediationService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class TakeOfferReviewController implements Controller {
    private final TakeOfferReviewModel model;
    @Getter
    private final TakeOfferReviewView view;
    private final ReputationService reputationService;
    private final SettingsService settingsService;
    private final UserIdentityService userIdentityService;
    private final BisqEasyPublicChatChannelService bisqEasyPublicChatChannelService;
    private final UserProfileService userProfileService;
    private final BisqEasyChatChannelSelectionService bisqEasyChatChannelSelectionService;
    private final BisqEasyPrivateTradeChatChannelService bisqEasyPrivateTradeChatChannelService;
    private final MediationService mediationService;
    private final ChatService chatService;
    private final KeyPairService keyPairService;

    public TakeOfferReviewController(DefaultApplicationService applicationService) {
        chatService = applicationService.getChatService();
        bisqEasyPublicChatChannelService = chatService.getBisqEasyPublicChatChannelService();
        bisqEasyChatChannelSelectionService = chatService.getBisqEasyChatChannelSelectionService();
        reputationService = applicationService.getUserService().getReputationService();
        settingsService = applicationService.getSettingsService();
        userIdentityService = applicationService.getUserService().getUserIdentityService();
        userProfileService = applicationService.getUserService().getUserProfileService();
        bisqEasyPrivateTradeChatChannelService = chatService.getBisqEasyPrivateTradeChatChannelService();
        mediationService = applicationService.getSupportService().getMediationService();
        keyPairService = applicationService.getSecurityService().getKeyPairService();

        model = new TakeOfferReviewModel();
        view = new TakeOfferReviewView(model, this);
    }

    public void setBisqEasyOffer(BisqEasyOffer bisqEasyOffer) {
        model.setBisqEasyOffer(bisqEasyOffer);
        String makersDirection = bisqEasyOffer.getMakersDirectionAsDisplayString();
        String takersDirection = bisqEasyOffer.getTakersDirectionAsDisplayString();
        userProfileService.findUserProfile(bisqEasyOffer.getMakersUserProfileId())
                .ifPresent(peersUserProfile -> {
                    model.setPeersUserProfile(peersUserProfile);
                    String peersUserName = peersUserProfile.getUserName();
                    if (isMyOffer(bisqEasyOffer)) {
                        model.getHeadline().set(Res.get("tradeAssistant.offer.maker.headline", makersDirection, peersUserName));
                        model.getOfferTitle().set(Res.get("tradeAssistant.offer.maker.offerTitle"));
                    } else {
                        model.getHeadline().set(Res.get("tradeAssistant.offer.taker.headline", peersUserName, takersDirection));
                        model.getOfferTitle().set(Res.get("tradeAssistant.offer.taker.offerTitle", peersUserName));
                    }
                });
    }

    private boolean isMyOffer(BisqEasyOffer bisqEasyOffer) {
        return keyPairService.findKeyPair(bisqEasyOffer.getMakerNetworkId().getPubKey().getKeyId()).isPresent();
    }

    public void setPaymentMethodName(String methodName) {
        if (methodName != null) {
            model.getPaymentMethods().set(Res.has(methodName) ? Res.get(methodName) : methodName);
        } else {
            model.getPaymentMethods().set(null);
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
            model.getAmount().set(AmountFormatter.formatAmountWithCode(monetary, true));
        }
    }

    @Override
    public void onActivate() {
        //  BisqEasyPublicChatChannel channel = bisqEasyPublicChatChannelService.findChannel(model.getMarket()).orElseThrow();
        //   model.setSelectedChannel(channel);

     /*   model.getShowCreateOfferSuccess().set(false);
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
        model.setMyOfferMessage(myOfferMessage);*/
    }

    @Override
    public void onDeactivate() {
    }

    void onCreateOffer() {
        UserIdentity userIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
        bisqEasyPublicChatChannelService.publishChatMessage(model.getMyOfferMessage(), userIdentity)
                .thenAccept(result -> UIThread.run(() -> {
                    model.getShowCreateOfferSuccess().set(true);
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
        OverlayController.hide();
        // If we got started from initial onboarding we are still at Splash screen, so we need to move to main
        Navigation.navigateTo(NavigationTarget.MAIN);
    }
}
