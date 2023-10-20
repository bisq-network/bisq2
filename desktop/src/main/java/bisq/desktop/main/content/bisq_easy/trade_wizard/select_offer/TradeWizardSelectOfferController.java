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

package bisq.desktop.main.content.bisq_easy.trade_wizard.select_offer;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.i18n.Res;
import bisq.network.p2p.vo.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.RangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.payment_method.PaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodSpecFormatter;
import bisq.offer.payment_method.PaymentMethodSpecUtil;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.settings.SettingsService;
import bisq.trade.Trade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class TradeWizardSelectOfferController implements Controller {
    private final TradeWizardSelectOfferModel model;
    @Getter
    private final TradeWizardSelectOfferView view;
    private final ReputationService reputationService;
    private final SettingsService settingsService;
    private final UserIdentityService userIdentityService;
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final UserProfileService userProfileService;
    private final Runnable onBackHandler;
    private final Runnable onNextHandler;
    private final Consumer<NavigationTarget> closeAndNavigateToHandler;
    private final MarketPriceService marketPriceService;
    private final BannedUserService bannedUserService;
    private final BisqEasyTradeService bisqEasyTradeService;

    public TradeWizardSelectOfferController(ServiceProvider serviceProvider,
                                            Runnable onBackHandler,
                                            Runnable onNextHandler,
                                            Consumer<NavigationTarget> closeAndNavigateToHandler) {
        this.onBackHandler = onBackHandler;
        this.onNextHandler = onNextHandler;
        this.closeAndNavigateToHandler = closeAndNavigateToHandler;
        ChatService chatService = serviceProvider.getChatService();
        bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        reputationService = serviceProvider.getUserService().getReputationService();
        settingsService = serviceProvider.getSettingsService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        bannedUserService = serviceProvider.getUserService().getBannedUserService();
        bisqEasyTradeService = serviceProvider.getTradeService().getBisqEasyTradeService();

        model = new TradeWizardSelectOfferModel();
        view = new TradeWizardSelectOfferView(model, this);
    }

    public ReadOnlyObjectProperty<BisqEasyOffer> getSelectedBisqEasyOffer() {
        return model.getSelectedBisqEasyOffer();
    }

    public ReadOnlyBooleanProperty getIsBackButtonHighlighted() {
        return model.getIsBackButtonHighlighted();
    }

    public void setDirection(Direction direction) {
        if (direction != null) {
            model.setDirection(direction);
            resetSelectedOffer();
        }
    }

    public void setMarket(Market market) {
        if (market != null) {
            model.setMarket(market);
            resetSelectedOffer();
        }
    }

    public void setFiatPaymentMethods(List<FiatPaymentMethod> fiatPaymentMethods) {
        if (fiatPaymentMethods != null) {
            model.setFiatPaymentMethods(fiatPaymentMethods);
            resetSelectedOffer();
        }
    }

    public void setAmountSpec(AmountSpec amountSpec) {
        if (amountSpec != null) {
            model.setAmountSpec(amountSpec);
            resetSelectedOffer();
        }
    }

    public void setPriceSpec(PriceSpec priceSpec) {
        if (priceSpec != null) {
            model.setPriceSpec(priceSpec);
            resetSelectedOffer();
        }
    }

    public void setIsMinAmountEnabled(boolean isMinAmountEnabled) {
        model.setMinAmountEnabled(isMinAmountEnabled);
        resetSelectedOffer();
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
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


        String paymentMethodNames = PaymentMethodSpecFormatter.fromPaymentMethods(model.getFiatPaymentMethods());
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

        Optional<BisqEasyOfferbookChannel> optionalChannel = bisqEasyOfferbookChannelService.findChannel(model.getMarket());
        if (optionalChannel.isPresent()) {
            BisqEasyOfferbookChannel channel = optionalChannel.get();
            model.setSelectedChannel(channel);

            BisqEasyOfferbookMessage myOfferMessage = new BisqEasyOfferbookMessage(channel.getId(),
                    userIdentity.getUserProfile().getId(),
                    Optional.of(bisqEasyOffer),
                    Optional.of(chatMessageText),
                    Optional.empty(),
                    new Date().getTime(),
                    false);

            model.setMyOfferMessage(myOfferMessage);

            model.getMatchingOffers().setAll(channel.getChatMessages().stream()
                    .filter(chatMessage -> chatMessage.getBisqEasyOffer().isPresent())
                    .map(chatMessage -> new TradeWizardSelectOfferView.ListItem(chatMessage.getBisqEasyOffer().get(),
                            model,
                            userProfileService,
                            reputationService,
                            marketPriceService))
                    .collect(Collectors.toList()));
            model.getFilteredList().setPredicate(getPredicate());
        } else {
            log.warn("optionalChannel not present");
        }

        boolean showOffers = !model.getFilteredList().isEmpty();
        model.getShowOffers().set(showOffers);
        model.getIsBackButtonHighlighted().set(!showOffers);

        if (showOffers) {
            model.setHeadLine(model.getDirection().isBuy() ?
                    Res.get("bisqEasy.tradeWizard.selectOffer.headline.buyer", quoteAmountAsString) :
                    Res.get("bisqEasy.tradeWizard.selectOffer.headline.seller", quoteAmountAsString));
            model.setSubHeadLine(Res.get("bisqEasy.tradeWizard.selectOffer.subHeadline"));
        } else {
            model.setHeadLine(Res.get("bisqEasy.tradeWizard.selectOffer.noMatchingOffers.headline", quoteAmountAsString));
            model.setSubHeadLine(Res.get("bisqEasy.tradeWizard.selectOffer.noMatchingOffers.subHeadline"));
        }
    }

    @Override
    public void onDeactivate() {
        model.getIsBackButtonHighlighted().set(false);
    }

    void onSelectRow(TradeWizardSelectOfferView.ListItem listItem) {
        if (listItem == null) {
            selectListItem(listItem);
            return;
        }
        if (listItem.equals(model.getSelectedItem())) {
            onSelect(listItem);
        } else {
            selectListItem(listItem);
        }
    }

    void onSelect(TradeWizardSelectOfferView.ListItem listItem) {
        if (listItem == null) {
            return;
        }
        selectListItem(listItem);

        onNextHandler.run();
    }

    void onGoBack() {
        onBackHandler.run();
    }

    void onOpenOfferbook() {
        closeAndNavigateToHandler.accept(NavigationTarget.BISQ_EASY_OFFERBOOK);
    }

    private void selectListItem(TradeWizardSelectOfferView.ListItem listItem) {
        model.setSelectedItem(listItem);
        model.getSelectedBisqEasyOffer().set(listItem.getBisqEasyOffer());
    }

    @SuppressWarnings("RedundantIfStatement")
    private Predicate<? super TradeWizardSelectOfferView.ListItem> getPredicate() {
        return item ->
        {
            try {
                BisqEasyOffer bisqEasyOffer = item.getBisqEasyOffer();

                if (model.getMatchingOffers().isEmpty()) {
                    return false;
                }
                if (model.getDirection().equals(bisqEasyOffer.getDirection())) {
                    return false;
                }

                if (!model.getMarket().equals(bisqEasyOffer.getMarket())) {
                    return false;
                }

                if (item.getAuthorUserProfile().isEmpty()) {
                    return false;
                }
                UserProfile makerUserProfile = item.getAuthorUserProfile().get();
                if (userProfileService.isChatUserIgnored(makerUserProfile)) {
                    return false;
                }
                // Ignore own offers
                if (userIdentityService.getUserIdentities().stream()
                        .map(userIdentity -> userIdentity.getUserProfile().getId())
                        .anyMatch(userProfileId -> userProfileId.equals(makerUserProfile.getId()))) {
                    return false;
                }


                if (userIdentityService.getSelectedUserIdentity() == null ||
                        bannedUserService.isUserProfileBanned(userIdentityService.getSelectedUserIdentity().getUserProfile()) ||
                        bannedUserService.isNetworkIdBanned(makerUserProfile.getNetworkId()) ||
                        bannedUserService.isUserProfileBanned(makerUserProfile)) {
                    return false;
                }


                UserProfile myUserProfile = userIdentityService.getSelectedUserIdentity().getUserProfile();
                NetworkId myNetworkId = myUserProfile.getNetworkId();
                String tradeId = Trade.createId(bisqEasyOffer.getId(), myNetworkId.getId());
                if (bisqEasyTradeService.hadTrade(tradeId)) {
                    return false;
                }

                Optional<Monetary> myQuoteSideMinOrFixedAmount = OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, bisqEasyOffer);
                Optional<Monetary> peersQuoteSideMaxOrFixedAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, bisqEasyOffer);
                if (myQuoteSideMinOrFixedAmount.orElseThrow().getValue() > peersQuoteSideMaxOrFixedAmount.orElseThrow().getValue()) {
                    return false;
                }

                Optional<Monetary> myQuoteSideMaxOrFixedAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, bisqEasyOffer);
                Optional<Monetary> peersQuoteSideMinOrFixedAmount = OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, bisqEasyOffer);
                if (myQuoteSideMaxOrFixedAmount.orElseThrow().getValue() < peersQuoteSideMinOrFixedAmount.orElseThrow().getValue()) {
                    return false;
                }

                List<String> paymentMethodNames = PaymentMethodSpecUtil.getPaymentMethodNames(bisqEasyOffer.getQuoteSidePaymentMethodSpecs());
                List<String> quoteSidePaymentMethodNames = PaymentMethodSpecUtil.getPaymentMethodNames(bisqEasyOffer.getQuoteSidePaymentMethodSpecs());
                if (quoteSidePaymentMethodNames.stream().noneMatch(paymentMethodNames::contains)) {
                    return false;
                }

                Set<FiatPaymentMethod> takersPaymentMethodSet = new HashSet<>(model.getFiatPaymentMethods());
                List<FiatPaymentMethod> matchingFiatPaymentMethods = bisqEasyOffer.getQuoteSidePaymentMethodSpecs().stream()
                        .filter(e -> takersPaymentMethodSet.contains(e.getPaymentMethod()))
                        .map(PaymentMethodSpec::getPaymentMethod)
                        .collect(Collectors.toList());
                if (matchingFiatPaymentMethods.isEmpty()) {
                    return false;
                }

                if (bisqEasyOffer.getDirection().mirror().isBuy()) {
                    long makersScore = reputationService.getReputationScore(makerUserProfile).getTotalScore();
                    long myRequiredReputationScore = settingsService.getRequiredTotalReputationScore().get();
                    // Makers score must be > than my required score (as buyer)
                    if (makersScore < myRequiredReputationScore) {
                        return false;
                    }
                } else {
                    // My score (as seller) must be > as offers required score
                    long myScore = reputationService.getReputationScore(userIdentityService.getSelectedUserIdentity().getUserProfile()).getTotalScore();
                    long offersRequiredReputationScore = OfferOptionUtil.findRequiredTotalReputationScore(bisqEasyOffer).orElse(0L);
                    if (myScore < offersRequiredReputationScore) {
                        return false;
                    }
                }


                return true;
            } catch (Throwable t) {
                log.error("Error at getPredicate", t);
                return false;
            }
        };
    }

    private void resetSelectedOffer() {
        model.setSelectedItem(null);
        model.getSelectedBisqEasyOffer().set(null);
    }
}
