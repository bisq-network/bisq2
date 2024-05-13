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

package bisq.desktop.main.content.bisq_easy.trade_wizard.amount;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.bisq_easy.BisqEasyService;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.bisq_easy.BisqEasyServiceUtil;
import bisq.desktop.main.content.bisq_easy.components.AmountComponent;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.Offer;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.amount.spec.AmountSpecUtil;
import bisq.offer.amount.spec.QuoteSideAmountSpec;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.amount.spec.QuoteSideRangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodSpecUtil;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.PriceFormatter;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class TradeWizardAmountController implements Controller {
    private static final PriceSpec MARKET_PRICE_SPEC = new MarketPriceSpec();

    private final TradeWizardAmountModel model;
    @Getter
    private final TradeWizardAmountView view;
    private final AmountComponent minAmountComponent, maxOrFixAmountComponent;
    private final SettingsService settingsService;
    private final MarketPriceService marketPriceService;
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final Region owner;
    private final UserProfileService userProfileService;
    private final ReputationService reputationService;
    private final UserIdentityService userIdentityService;
    private final BisqEasyService bisqEasyService;
    private Subscription isMinAmountEnabledPin, maxOrFixAmountCompBaseSideAmountPin, minAmountCompBaseSideAmountPin,
            maxAmountCompQuoteSideAmountPin, minAmountCompQuoteSideAmountPin, priceTooltipPin;

    public TradeWizardAmountController(ServiceProvider serviceProvider, Region owner) {
        settingsService = serviceProvider.getSettingsService();
        bisqEasyService = serviceProvider.getBisqEasyService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        reputationService = serviceProvider.getUserService().getReputationService();
        bisqEasyOfferbookChannelService = serviceProvider.getChatService().getBisqEasyOfferbookChannelService();
        this.owner = owner;
        model = new TradeWizardAmountModel();

        minAmountComponent = new AmountComponent(serviceProvider, true);
        minAmountComponent.setDescription(Res.get("bisqEasy.tradeWizard.amount.description.minAmount"));
        maxOrFixAmountComponent = new AmountComponent(serviceProvider, true);

        view = new TradeWizardAmountView(model, this,
                minAmountComponent,
                maxOrFixAmountComponent);
    }

    public void setIsCreateOfferMode(boolean isCreateOfferMode) {
        model.setCreateOfferMode(isCreateOfferMode);
        model.getShowRangeAmounts().set(isCreateOfferMode);
        if (!isCreateOfferMode) {
            model.getIsMinAmountEnabled().set(false);
        }
    }

    public void setDirection(Direction direction) {
        if (direction == null) {
            return;
        }
        model.setDirection(direction);
        minAmountComponent.setDirection(direction);
        maxOrFixAmountComponent.setDirection(direction);
    }

    public void setMarket(Market market) {
        if (market == null) {
            return;
        }
        minAmountComponent.setMarket(market);
        maxOrFixAmountComponent.setMarket(market);
        model.setMarket(market);
    }

    public void setFiatPaymentMethods(List<FiatPaymentMethod> fiatPaymentMethods) {
        if (fiatPaymentMethods != null) {
            model.setFiatPaymentMethods(fiatPaymentMethods);
        }
    }

    public boolean validate() {
        // No errorMessage set yet... We reset the amount to a valid value in case input is invalid
        if (model.getErrorMessage().get() == null) {
            return true;
        } else {
            new Popup().invalid(model.getErrorMessage().get())
                    .owner(owner)
                    .show();
            return false;
        }
    }

    public void updateQuoteSideAmountSpecWithPriceSpec(PriceSpec priceSpec) {
        if (priceSpec == null) {
            return;
        }

        QuoteSideAmountSpec amountSpec = model.getQuoteSideAmountSpec().get();
        if (amountSpec == null) {
            return;
        }
        Market market = model.getMarket();
        if (market == null) {
            log.warn("market is null at updateQuoteSideAmountSpecWithPriceSpec");
            return;
        }
        Optional<PriceQuote> priceQuote = PriceUtil.findQuote(marketPriceService, priceSpec, market);
        if (priceQuote.isEmpty()) {
            log.warn("priceQuote is empty at updateQuoteSideAmountSpecWithPriceSpec");
            return;
        }
        model.getPriceQuote().set(priceQuote.get());
        minAmountComponent.setQuote(priceQuote.get());
        maxOrFixAmountComponent.setQuote(priceQuote.get());

        OfferAmountUtil.updateQuoteSideAmountSpecWithPriceSpec(marketPriceService, amountSpec, priceSpec, market)
                .ifPresent(quoteSideAmountSpec -> {
                    // TODO we do not change min/max amounts if price has changed as its confusing to user.
                    // we prefer that users could breach the min/max defined in the AmountComponent
                    // the min/max as btc amount is anyway not great. better would be a market specific min/max.
                    //todo Move AmountComponent.MIN_RANGE_BASE_SIDE_VALUE to config
                  /*  long minQuoteSideValueValue =  PriceUtil.findQuote(marketPriceService, priceSpec, market)
                            .map(priceQuote -> priceQuote.toQuoteSideMonetary(AmountComponent.MIN_RANGE_BASE_SIDE_VALUE))
                            .map(Monetary::getValue)
                            .orElseThrow();
                    long maxQuoteSideValueValue =  PriceUtil.findQuote(marketPriceService, priceSpec, market)
                            .map(priceQuote -> priceQuote.toQuoteSideMonetary(AmountComponent.MAX_RANGE_BASE_SIDE_VALUE))
                            .map(Monetary::getValue)
                            .orElseThrow();
                    if (quoteSideAmountSpec instanceof QuoteSideFixedAmountSpec) {
                        QuoteSideFixedAmountSpec fixedAmountSpec = (QuoteSideFixedAmountSpec) quoteSideAmountSpec;
                        if (fixedAmountSpec.getAmount() < minQuoteSideValueValue) {
                            quoteSideAmountSpec = new QuoteSideFixedAmountSpec(minQuoteSideValueValue);
                        } else if (fixedAmountSpec.getAmount() > maxQuoteSideValueValue) {
                            quoteSideAmountSpec = new QuoteSideFixedAmountSpec(maxQuoteSideValueValue);
                        }
                    } else if (quoteSideAmountSpec instanceof QuoteSideRangeAmountSpec) {
                        QuoteSideRangeAmountSpec rangeAmountSpec = (QuoteSideRangeAmountSpec) quoteSideAmountSpec;
                        long minAmount = Math.max(minQuoteSideValueValue, rangeAmountSpec.getMinAmount());
                        long maxAmount = Math.min(maxQuoteSideValueValue, rangeAmountSpec.getMaxAmount());
                        checkArgument(minAmount <= maxAmount);
                        quoteSideAmountSpec = new QuoteSideRangeAmountSpec(minAmount, maxAmount);
                    } else {
                        throw new RuntimeException("Unsupported amountSpec: {}" + quoteSideAmountSpec);
                    }*/
                    model.getQuoteSideAmountSpec().set(quoteSideAmountSpec);
                });
    }

    public void reset() {
        minAmountComponent.reset();
        maxOrFixAmountComponent.reset();
        model.reset();
    }

    public ReadOnlyObjectProperty<QuoteSideAmountSpec> getQuoteSideAmountSpec() {
        return model.getQuoteSideAmountSpec();
    }

    public ReadOnlyBooleanProperty getIsMinAmountEnabled() {
        return model.getIsMinAmountEnabled();
    }

    @Override
    public void onActivate() {
        if (model.getPriceQuote().get() == null && minAmountComponent.getQuote().get() != null) {
            model.getPriceQuote().set(minAmountComponent.getQuote().get());
        }
        model.setHeadline(model.getDirection().isBuy() ?
                Res.get("bisqEasy.tradeWizard.amount.headline.buyer") :
                Res.get("bisqEasy.tradeWizard.amount.headline.seller"));

        Boolean cookieValue = settingsService.getCookie().asBoolean(CookieKey.CREATE_BISQ_EASY_OFFER_IS_MIN_AMOUNT_ENABLED).orElse(false);
        model.getIsMinAmountEnabled().set(cookieValue && model.getShowRangeAmounts().get());

        minAmountCompBaseSideAmountPin = EasyBind.subscribe(minAmountComponent.getBaseSideAmount(),
                value -> {
                    if (model.getIsMinAmountEnabled().get()) {
                        if (value != null && maxOrFixAmountComponent.getBaseSideAmount().get() != null &&
                                value.getValue() > maxOrFixAmountComponent.getBaseSideAmount().get().getValue()) {
                            maxOrFixAmountComponent.setBaseSideAmount(value);
                        }
                    }
                });
        maxOrFixAmountCompBaseSideAmountPin = EasyBind.subscribe(maxOrFixAmountComponent.getBaseSideAmount(),
                value -> {
                    if (model.getIsMinAmountEnabled().get() &&
                            value != null && minAmountComponent.getBaseSideAmount().get() != null &&
                            value.getValue() < minAmountComponent.getBaseSideAmount().get().getValue()) {
                        minAmountComponent.setBaseSideAmount(value);
                    }
                });

        minAmountCompQuoteSideAmountPin = EasyBind.subscribe(minAmountComponent.getQuoteSideAmount(),
                value -> {
                    if (value != null) {
                        if (model.getIsMinAmountEnabled().get() &&
                                maxOrFixAmountComponent.getQuoteSideAmount().get() != null &&
                                value.getValue() > maxOrFixAmountComponent.getQuoteSideAmount().get().getValue()) {
                            maxOrFixAmountComponent.setQuoteSideAmount(value);
                        }
                        applyAmountSpec();
                    }
                });
        maxAmountCompQuoteSideAmountPin = EasyBind.subscribe(maxOrFixAmountComponent.getQuoteSideAmount(),
                value -> {
                    if (value != null) {
                        if (model.getIsMinAmountEnabled().get() &&
                                minAmountComponent.getQuoteSideAmount().get() != null &&
                                value.getValue() < minAmountComponent.getQuoteSideAmount().get().getValue()) {
                            minAmountComponent.setQuoteSideAmount(value);
                        }
                        applyAmountSpec();
                    }
                });

        isMinAmountEnabledPin = EasyBind.subscribe(model.getIsMinAmountEnabled(), isMinAmountEnabled -> {
            model.getToggleButtonText().set(isMinAmountEnabled ?
                    Res.get("bisqEasy.tradeWizard.amount.removeMinAmountOption") :
                    Res.get("bisqEasy.tradeWizard.amount.addMinAmountOption"));

            maxOrFixAmountComponent.setDescription(isMinAmountEnabled ?
                    Res.get("bisqEasy.tradeWizard.amount.description.maxAmount") :
                    Res.get("bisqEasy.tradeWizard.amount.description.fixAmount"));

            applyAmountSpec();
        });

        applyAmountSpec();

        if (model.isCreateOfferMode()) {
            Optional<PriceQuote> marketPriceQuote = getMarketPriceQuote();
            if (model.getPriceQuote().get() != null &&
                    marketPriceQuote.isPresent() &&
                    !model.getPriceQuote().get().equals(marketPriceQuote.get())) {
                model.getPriceTooltip().set(Res.get("bisqEasy.component.amount.baseSide.tooltip.btcAmount.selectedPrice"));
            } else {
                model.getPriceTooltip().set(Res.get("bisqEasy.component.amount.baseSide.tooltip.btcAmount.marketPrice"));
            }
        } else {
            // Use best price of matching offer if any match found, otherwise market price.
            Optional<String> bestOffersPriceTooltip = findBestOfferQuote()
                    .map(bestOffersPrice -> Res.get("bisqEasy.component.amount.baseSide.tooltip.bestOfferPrice",
                            PriceFormatter.formatWithCode(bestOffersPrice)));
            String marketPriceTooltipWithMaybeBuyerInfo = String.format("%s%s",
                    Res.get("bisqEasy.component.amount.baseSide.tooltip.btcAmount.marketPrice"),
                    model.getDirection().isSell() ? "" : "\n" + Res.get("bisqEasy.component.amount.baseSide.tooltip.buyerInfo"));

            //todo
            model.getPriceTooltip().set(bestOffersPriceTooltip.orElse(marketPriceTooltipWithMaybeBuyerInfo));
        }

        priceTooltipPin = EasyBind.subscribe(model.getPriceTooltip(), priceTooltip -> {
            if (priceTooltip != null) {
                minAmountComponent.setTooltip(priceTooltip);
                maxOrFixAmountComponent.setTooltip(priceTooltip);
            }
        });
    }

    @Override
    public void onDeactivate() {
        isMinAmountEnabledPin.unsubscribe();
        maxOrFixAmountCompBaseSideAmountPin.unsubscribe();
        maxAmountCompQuoteSideAmountPin.unsubscribe();
        minAmountCompBaseSideAmountPin.unsubscribe();
        minAmountCompQuoteSideAmountPin.unsubscribe();
        priceTooltipPin.unsubscribe();
    }

    void onToggleMinAmountVisibility() {
        boolean value = !model.getIsMinAmountEnabled().get();
        model.getIsMinAmountEnabled().set(value);
        settingsService.setCookie(CookieKey.CREATE_BISQ_EASY_OFFER_IS_MIN_AMOUNT_ENABLED, value);
    }

    private void applyAmountSpec() {
        Long maxOrFixAmount = getAmountValue(maxOrFixAmountComponent.getQuoteSideAmount());
        if (maxOrFixAmount == null) {
            return;
        }

        if (model.getIsMinAmountEnabled().get()) {
            Long minAmount = getAmountValue(minAmountComponent.getQuoteSideAmount());
            checkNotNull(minAmount);
            if (maxOrFixAmount.compareTo(minAmount) < 0) {
                minAmountComponent.setQuoteSideAmount(maxOrFixAmountComponent.getQuoteSideAmount().get());
                minAmount = getAmountValue(minAmountComponent.getQuoteSideAmount());
            }
            applyRangeOrFixedAmountSpec(minAmount, maxOrFixAmount);
        } else {
            applyFixedAmountSpec(maxOrFixAmount);
        }
    }

    private Long getAmountValue(ReadOnlyObjectProperty<Monetary> amountProperty) {
        if (amountProperty.get() == null) {
            return null;
        }
        return amountProperty.get().getValue();
    }

    private void applyRangeOrFixedAmountSpec(Long minAmount, long maxOrFixAmount) {
        if (minAmount != null) {
            if (minAmount.equals(maxOrFixAmount)) {
                applyFixedAmountSpec(maxOrFixAmount);
            } else {
                applyRangeAmountSpec(minAmount, maxOrFixAmount);
            }
        }
    }

    private void applyFixedAmountSpec(long maxOrFixAmount) {
        model.getQuoteSideAmountSpec().set(new QuoteSideFixedAmountSpec(maxOrFixAmount));
    }

    private void applyRangeAmountSpec(long minAmount, long maxOrFixAmount) {
        model.getQuoteSideAmountSpec().set(new QuoteSideRangeAmountSpec(minAmount, maxOrFixAmount));
    }

    private Optional<PriceQuote> findBestOfferQuote() {
        // Only used in wizard mode where we do not show min/max amounts
        Optional<BisqEasyOfferbookChannel> optionalChannel = bisqEasyOfferbookChannelService.findChannel(model.getMarket());
        if (optionalChannel.isEmpty() || model.getMarket() == null) {
            return Optional.empty();
        }

        List<BisqEasyOffer> bisqEasyOffers = optionalChannel.get().getChatMessages().stream()
                .filter(chatMessage -> chatMessage.getBisqEasyOffer().isPresent())
                .map(chatMessage -> chatMessage.getBisqEasyOffer().get())
                .filter(this::filterOffers)
                .collect(Collectors.toList());
        boolean isSellOffer = bisqEasyOffers.stream()
                .map(Offer::getDirection)
                .map(Direction::isSell)
                .findAny()
                .orElse(false);
        Stream<PriceQuote> priceQuoteStream = bisqEasyOffers.stream()
                .map(Offer::getPriceSpec)
                .flatMap(priceSpec -> PriceUtil.findQuote(marketPriceService, priceSpec, model.getMarket()).or(this::getMarketPriceQuote).stream());
        Optional<PriceQuote> bestOffersPrice = isSellOffer
                ? priceQuoteStream.min(Comparator.comparing(PriceQuote::getValue))
                : priceQuoteStream.max(Comparator.comparing(PriceQuote::getValue));
        if (bestOffersPrice.isPresent()) {
            maxOrFixAmountComponent.setQuote(bestOffersPrice.get());
        } else {
            getMarketPriceQuote().ifPresent(maxOrFixAmountComponent::setQuote);
        }
        AmountSpecUtil.findQuoteSideFixedAmountFromSpec(model.getQuoteSideAmountSpec().get(), model.getMarket().getQuoteCurrencyCode())
                .ifPresent(amount -> UIThread.runOnNextRenderFrame(() -> maxOrFixAmountComponent.setQuoteSideAmount(amount)));

        return bestOffersPrice;
    }

    // Used for finding best price quote of available matching offers
    private boolean filterOffers(BisqEasyOffer peersOffer) {
        try {
            Optional<UserProfile> optionalMakersUserProfile = userProfileService.findUserProfile(peersOffer.getMakersUserProfileId());
            if (optionalMakersUserProfile.isEmpty()) {
                return false;
            }
            UserProfile makersUserProfile = optionalMakersUserProfile.get();
            if (userProfileService.isChatUserIgnored(makersUserProfile)) {
                return false;
            }
            if (userIdentityService.getUserIdentities().stream()
                    .map(userIdentity -> userIdentity.getUserProfile().getId())
                    .anyMatch(userProfileId -> userProfileId.equals(optionalMakersUserProfile.get().getId()))) {
                return false;
            }

            if (peersOffer.getDirection().equals(model.getDirection())) {
                return false;
            }

            if (!peersOffer.getMarket().equals(model.getMarket())) {
                return false;
            }

            Optional<Monetary> myQuoteSideMinOrFixedAmount = OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, model.getQuoteSideAmountSpec().get(), MARKET_PRICE_SPEC, model.getMarket());
            Optional<Monetary> peersQuoteSideMaxOrFixedAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, peersOffer);
            if (myQuoteSideMinOrFixedAmount.orElseThrow().getValue() > peersQuoteSideMaxOrFixedAmount.orElseThrow().getValue()) {
                return false;
            }

            Optional<Monetary> myQuoteSideMaxOrFixedAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, model.getQuoteSideAmountSpec().get(), MARKET_PRICE_SPEC, model.getMarket());
            Optional<Monetary> peersQuoteSideMinOrFixedAmount = OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, peersOffer);
            if (myQuoteSideMaxOrFixedAmount.orElseThrow().getValue() < peersQuoteSideMinOrFixedAmount.orElseThrow().getValue()) {
                return false;
            }

            List<String> paymentMethodNames = PaymentMethodSpecUtil.getPaymentMethodNames(peersOffer.getQuoteSidePaymentMethodSpecs());
            List<FiatPaymentMethodSpec> quoteSidePaymentMethodSpecs = PaymentMethodSpecUtil.createFiatPaymentMethodSpecs(model.getFiatPaymentMethods());
            List<String> quoteSidePaymentMethodNames = PaymentMethodSpecUtil.getPaymentMethodNames(quoteSidePaymentMethodSpecs);
            if (quoteSidePaymentMethodNames.stream().noneMatch(paymentMethodNames::contains)) {
                return false;
            }

            return BisqEasyServiceUtil.offerMatchesMinRequiredReputationScore(reputationService,
                    bisqEasyService,
                    userIdentityService,
                    userProfileService,
                    peersOffer);
        } catch (Throwable t) {
            log.error("Error at TakeOfferPredicate", t);
            return false;
        }
    }

    private Optional<PriceQuote> getMarketPriceQuote() {
        return marketPriceService.findMarketPriceQuote(model.getMarket());
    }
}
