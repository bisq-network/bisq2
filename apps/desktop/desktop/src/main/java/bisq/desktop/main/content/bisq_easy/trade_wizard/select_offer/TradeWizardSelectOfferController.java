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
import bisq.bisq_easy.BisqEasyService;
import bisq.bisq_easy.NavigationTarget;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.bisq_easy.BisqEasyServiceUtil;
import bisq.i18n.Res;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.RangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.PaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodSpecUtil;
import bisq.offer.price.spec.PriceSpec;
import bisq.settings.SettingsService;
import bisq.trade.Trade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
    private final BisqEasyService bisqEasyService;

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
        bisqEasyService = serviceProvider.getBisqEasyService();
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

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        Direction direction = model.getDirection();
        Market market = model.getMarket();
        AmountSpec amountSpec = model.getAmountSpec();
        PriceSpec priceSpec = model.getPriceSpec();

        boolean hasAmountRange = amountSpec instanceof RangeAmountSpec;
        String quoteAmountAsString = OfferAmountFormatter.formatQuoteAmount(marketPriceService, amountSpec, priceSpec, market, hasAmountRange, true);

        Optional<BisqEasyOfferbookChannel> optionalChannel = bisqEasyOfferbookChannelService.findChannel(market);
        if (optionalChannel.isPresent()) {
            BisqEasyOfferbookChannel channel = optionalChannel.get();
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
            model.setHeadline(direction.isBuy() ?
                    Res.get("bisqEasy.tradeWizard.selectOffer.headline.buyer", quoteAmountAsString) :
                    Res.get("bisqEasy.tradeWizard.selectOffer.headline.seller", quoteAmountAsString));
            model.setSubHeadLine(Res.get("bisqEasy.tradeWizard.selectOffer.subHeadline"));
        } else {
            model.setHeadline(Res.get("bisqEasy.tradeWizard.selectOffer.noMatchingOffers.headline", quoteAmountAsString));
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
                BisqEasyOffer peersOffer = item.getBisqEasyOffer();

                if (model.getMatchingOffers().isEmpty()) {
                    return false;
                }
                if (model.getDirection().equals(peersOffer.getDirection())) {
                    return false;
                }
                if (!model.getMarket().equals(peersOffer.getMarket())) {
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
                String tradeId = Trade.createId(peersOffer.getId(), myNetworkId.getId());
                if (bisqEasyTradeService.tradeExists(tradeId)) {
                    return false;
                }

                Optional<Monetary> myQuoteSideMinOrFixedAmount = OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, peersOffer);
                Optional<Monetary> peersQuoteSideMaxOrFixedAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, peersOffer);
                if (myQuoteSideMinOrFixedAmount.orElseThrow().getValue() > peersQuoteSideMaxOrFixedAmount.orElseThrow().getValue()) {
                    return false;
                }

                Optional<Monetary> myQuoteSideMaxOrFixedAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, peersOffer);
                Optional<Monetary> peersQuoteSideMinOrFixedAmount = OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, peersOffer);
                if (myQuoteSideMaxOrFixedAmount.orElseThrow().getValue() < peersQuoteSideMinOrFixedAmount.orElseThrow().getValue()) {
                    return false;
                }

                List<String> paymentMethodNames = PaymentMethodSpecUtil.getPaymentMethodNames(peersOffer.getQuoteSidePaymentMethodSpecs());
                List<String> quoteSidePaymentMethodNames = PaymentMethodSpecUtil.getPaymentMethodNames(peersOffer.getQuoteSidePaymentMethodSpecs());
                if (quoteSidePaymentMethodNames.stream().noneMatch(paymentMethodNames::contains)) {
                    return false;
                }

                Set<FiatPaymentMethod> takersPaymentMethodSet = new HashSet<>(model.getFiatPaymentMethods());
                List<FiatPaymentMethod> matchingFiatPaymentMethods = peersOffer.getQuoteSidePaymentMethodSpecs().stream()
                        .filter(e -> takersPaymentMethodSet.contains(e.getPaymentMethod()))
                        .map(PaymentMethodSpec::getPaymentMethod)
                        .collect(Collectors.toList());
                if (matchingFiatPaymentMethods.isEmpty()) {
                    return false;
                }

                if (!BisqEasyServiceUtil.offerMatchesMinRequiredReputationScore(reputationService,
                        bisqEasyService,
                        userIdentityService,
                        userProfileService,
                        peersOffer)) {
                    return false;
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
