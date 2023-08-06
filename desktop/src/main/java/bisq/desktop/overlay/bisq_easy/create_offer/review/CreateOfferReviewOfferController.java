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

package bisq.desktop.overlay.bisq_easy.create_offer.review;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannel;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannelService;
import bisq.chat.bisqeasy.message.BisqEasyPublicChatMessage;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.desktop.overlay.bisq_easy.take_offer.TakeOfferController;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.RangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.PaymentMethodSpecFormatter;
import bisq.offer.payment_method.PaymentMethodSpecUtil;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.settings.SettingsService;
import bisq.user.banned.BannedUserService;
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
public class CreateOfferReviewOfferController implements Controller {
    private final CreateOfferReviewOfferModel model;
    @Getter
    private final CreateOfferReviewOfferView view;
    private final ReputationService reputationService;
    private final Runnable resetHandler;
    private final SettingsService settingsService;
    private final UserIdentityService userIdentityService;
    private final BisqEasyPublicChatChannelService bisqEasyPublicChatChannelService;
    private final UserProfileService userProfileService;
    private final Consumer<Boolean> mainButtonsVisibleHandler;
    private final MarketPriceService marketPriceService;
    private final BannedUserService bannedUserService;

    public CreateOfferReviewOfferController(ServiceProvider serviceProvider,
                                            Consumer<Boolean> mainButtonsVisibleHandler,
                                            Runnable resetHandler) {
        this.mainButtonsVisibleHandler = mainButtonsVisibleHandler;
        ChatService chatService = serviceProvider.getChatService();
        bisqEasyPublicChatChannelService = chatService.getBisqEasyPublicChatChannelService();
        reputationService = serviceProvider.getUserService().getReputationService();
        settingsService = serviceProvider.getSettingsService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        bannedUserService = serviceProvider.getUserService().getBannedUserService();
        this.resetHandler = resetHandler;

        model = new CreateOfferReviewOfferModel();
        view = new CreateOfferReviewOfferView(model, this);
    }

    public void setDirection(Direction direction) {
        model.setDirection(direction);
    }

    public void setMarket(Market market) {
        if (market != null) {
            model.setMarket(market);
        }
    }

    public void setFiatPaymentMethods(List<FiatPaymentMethod> fiatPaymentMethods) {
        if (fiatPaymentMethods != null) {
            model.setFiatPaymentMethods(fiatPaymentMethods);
        }
    }

    public void setAmountSpec(AmountSpec amountSpec) {
        if (amountSpec != null) {
            model.setAmountSpec(amountSpec);
        }
    }

    public void setPriceSpec(PriceSpec priceSpec) {
        model.setPriceSpec(priceSpec);
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
        model.getShowCreateOfferSuccess().set(false);

        UserIdentity userIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
        String priceInfo;
        PriceSpec priceSpec = model.getPriceSpec();
        Direction direction = model.getDirection();
        if (direction.isSell()) {
            if (priceSpec instanceof FixPriceSpec) {
                FixPriceSpec fixPriceSpec = (FixPriceSpec) priceSpec;
                String price = PriceFormatter.formatWithCode(fixPriceSpec.getPriceQuote());
                priceInfo = Res.get("bisqEasy.createOffer.review.chatMessage.fixPrice", price);
            } else if (priceSpec instanceof FloatPriceSpec) {
                FloatPriceSpec floatPriceSpec = (FloatPriceSpec) priceSpec;
                String percent = PercentageFormatter.formatToPercentWithSymbol(floatPriceSpec.getPercentage());
                priceInfo = Res.get("bisqEasy.createOffer.review.chatMessage.floatPrice", percent);
            } else {
                priceInfo = Res.get("bisqEasy.createOffer.review.chatMessage.marketPrice");
            }
        } else {
            priceInfo = "";
        }

        String directionString = Res.get("offer." + direction.name().toLowerCase()).toUpperCase();
        AmountSpec amountSpec = model.getAmountSpec();
        boolean hasAmountRange = amountSpec instanceof RangeAmountSpec;
        String quoteAmountAsString = OfferAmountFormatter.formatQuoteAmount(marketPriceService, amountSpec, model.getPriceSpec(), model.getMarket(), hasAmountRange, true);
        model.setQuoteAmountAsString(quoteAmountAsString);
        String paymentMethodNames = PaymentMethodSpecFormatter.fromPaymentMethod(model.getFiatPaymentMethods(), true);
        String chatMessageText = Res.get("bisqEasy.createOffer.review.chatMessage",
                directionString,
                quoteAmountAsString,
                paymentMethodNames,
                priceInfo);

        model.setMyOfferText(chatMessageText);

        BisqEasyOffer bisqEasyOffer = new BisqEasyOffer(
                userIdentity.getUserProfile().getNetworkId(),
                direction,
                model.getMarket(),
                amountSpec,
                priceSpec,
                new ArrayList<>(model.getFiatPaymentMethods()),
                userIdentity.getUserProfile().getTerms(),
                settingsService.getRequiredTotalReputationScore().get(),
                new ArrayList<>(settingsService.getSupportedLanguageCodes()));
        model.setBisqEasyOffer(bisqEasyOffer);

        Optional<BisqEasyPublicChatChannel> optionalChannel = bisqEasyPublicChatChannelService.findChannel(model.getMarket());
        if (optionalChannel.isPresent()) {
            BisqEasyPublicChatChannel channel = optionalChannel.get();
            model.setSelectedChannel(channel);

            BisqEasyPublicChatMessage myOfferMessage = new BisqEasyPublicChatMessage(channel.getId(),
                    userIdentity.getUserProfile().getId(),
                    Optional.of(bisqEasyOffer),
                    Optional.of(chatMessageText),
                    Optional.empty(),
                    new Date().getTime(),
                    false);

            model.setMyOfferMessage(myOfferMessage);

            model.getMatchingOffers().setAll(channel.getChatMessages().stream()
                    .filter(chatMessage -> chatMessage.getBisqEasyOffer().isPresent())
                    .map(chatMessage -> new CreateOfferReviewOfferView.ListItem(chatMessage.getBisqEasyOffer().get(),
                            model,
                            userProfileService,
                            reputationService,
                            marketPriceService))
                    .filter(getTakeOfferPredicate())
                    .sorted(Comparator.comparing(CreateOfferReviewOfferView.ListItem::getReputationScore))
                    .limit(3)
                    .collect(Collectors.toList()));
        } else {
            log.warn("optionalChannel not present");
        }

        model.getMatchingOffersVisible().set(model.isShowMatchingOffers() && !model.getMatchingOffers().isEmpty());
    }

    @Override
    public void onDeactivate() {
    }

    void onTakeOffer(CreateOfferReviewOfferView.ListItem listItem) {
        UserIdentity myUserIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
        if (bannedUserService.isNetworkIdBanned(listItem.getBisqEasyOffer().getMakerNetworkId()) ||
                bannedUserService.isUserProfileBanned(myUserIdentity.getUserProfile())) {
            return;
        }

        OverlayController.hide(() -> {
                    TakeOfferController.InitData initData = new TakeOfferController.InitData(listItem.getBisqEasyOffer(),
                            Optional.of(model.getAmountSpec()),
                            model.getFiatPaymentMethods());
                    Navigation.navigateTo(NavigationTarget.TAKE_OFFER, initData);
                    resetHandler.run();
                }
        );
    }

    void onPublishOffer() {
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

    private void close() {
        resetHandler.run();
        OverlayController.hide();
    }

    @SuppressWarnings("RedundantIfStatement")
    private Predicate<? super CreateOfferReviewOfferView.ListItem> getTakeOfferPredicate() {
        return item ->
        {
            try {
                if (item.getAuthorUserProfile().isEmpty()) {
                    return false;
                }
                UserProfile authorUserProfile = item.getAuthorUserProfile().get();
                if (userProfileService.isChatUserIgnored(authorUserProfile)) {
                    return false;
                }
                if (userIdentityService.getUserIdentities().stream()
                        .map(userIdentity -> userIdentity.getUserProfile().getId())
                        .anyMatch(userProfileId -> userProfileId.equals(authorUserProfile.getId()))) {
                    return false;
                }
                if (model.getMyOfferMessage() == null) {
                    return false;
                }
                if (model.getMyOfferMessage().getBisqEasyOffer().isEmpty()) {
                    return false;
                }

                BisqEasyOffer bisqEasyOffer = model.getBisqEasyOffer();
                BisqEasyOffer peersOffer = item.getBisqEasyOffer();

                if (peersOffer.getDirection().equals(bisqEasyOffer.getDirection())) {
                    return false;
                }

                if (!peersOffer.getMarket().equals(bisqEasyOffer.getMarket())) {
                    return false;
                }
                Optional<Monetary> myQuoteSideMinOrFixedAmount = OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, bisqEasyOffer);
                Optional<Monetary> peersQuoteSideMaxOrFixedAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, peersOffer);
                if (myQuoteSideMinOrFixedAmount.orElseThrow().getValue() > peersQuoteSideMaxOrFixedAmount.orElseThrow().getValue()) {
                    return false;
                }

                Optional<Monetary> myQuoteSideMaxOrFixedAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, bisqEasyOffer);
                Optional<Monetary> peersQuoteSideMinOrFixedAmount = OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, peersOffer);
                if (myQuoteSideMaxOrFixedAmount.orElseThrow().getValue() < peersQuoteSideMinOrFixedAmount.orElseThrow().getValue()) {
                    return false;
                }

                List<String> paymentMethodNames = PaymentMethodSpecUtil.getPaymentMethodNames(peersOffer.getQuoteSidePaymentMethodSpecs());
                //  List<String> paymentMethodNames = PaymentMethodSpecUtil.getQuoteSidePaymentMethodNames(peersOffer);
                List<String> quoteSidePaymentMethodNames = PaymentMethodSpecUtil.getPaymentMethodNames(bisqEasyOffer.getQuoteSidePaymentMethodSpecs());
                if (quoteSidePaymentMethodNames.stream().noneMatch(paymentMethodNames::contains)) {
                    return false;
                }

                //todo
           /* if (reputationService.getReputationScore(senderUserProfile).getTotalScore() < myChatOffer.getRequiredTotalReputationScore()) {
                return false;
            }*/

                return true;
            } catch (Throwable t) {
                log.error("Error at TakeOfferPredicate", t);
                return false;
            }
        };
    }
}
