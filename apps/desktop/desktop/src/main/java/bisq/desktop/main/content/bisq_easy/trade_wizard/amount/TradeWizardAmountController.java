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

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.bisq_easy.BisqEasyService;
import bisq.bisq_easy.BisqEasyTradeAmountLimits;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.data.Pair;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.bisq_easy.components.AmountComponent;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.Offer;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.amount.spec.*;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static bisq.bisq_easy.BisqEasyTradeAmountLimits.*;
import static bisq.presentation.formatters.AmountFormatter.formatAmountWithCode;
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
        applyQuoteSideMinMaxRange();
    }

    public void setBitcoinPaymentMethods(List<BitcoinPaymentMethod> bitcoinPaymentMethods) {
        if (bitcoinPaymentMethods != null) {
            model.setBitcoinPaymentMethods(bitcoinPaymentMethods);
        }
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
                .ifPresent(quoteSideAmountSpec -> model.getQuoteSideAmountSpec().set(quoteSideAmountSpec));
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
        applyQuoteSideMinMaxRange();

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
                    if (value != null &&
                            model.getIsMinAmountEnabled().get() &&
                            minAmountComponent.getBaseSideAmount().get() != null &&
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
                        quoteSideAmountsChanged(false);
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
                        quoteSideAmountsChanged(true);
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
        view.getRoot().setOnKeyPressed(null);
        model.getIsAmountLimitInfoOverlayVisible().set(false);
    }

    void onSetReputationBasedAmount() {
        applyReputationBasedQuoteSideAmount();
    }

    void onShowAmountLimitInfoOverlay() {
        model.getIsAmountLimitInfoOverlayVisible().set(true);
        view.getRoot().setOnKeyPressed(keyEvent -> {
            KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {
            });
            KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onCloseAmountLimitInfoOverlay);
        });
    }

    void onCloseAmountLimitInfoOverlay() {
        view.getRoot().setOnKeyPressed(null);
        model.getIsAmountLimitInfoOverlayVisible().set(false);
    }

    void onOpenWiki(String url) {
        Browser.open(url);
    }

    void onToggleMinAmountVisibility() {
        boolean value = !model.getIsMinAmountEnabled().get();
        model.getIsMinAmountEnabled().set(value);
        quoteSideAmountsChanged(!value);
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
                .toList();
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

    private boolean filterOffersByAmounts(BisqEasyOffer peersOffer) {
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

            List<String> bitcoinPaymentMethodNames = PaymentMethodSpecUtil.getPaymentMethodNames(peersOffer.getBaseSidePaymentMethodSpecs());
            List<BitcoinPaymentMethodSpec> baseSidePaymentMethodSpecs = PaymentMethodSpecUtil.createBitcoinPaymentMethodSpecs(model.getBitcoinPaymentMethods());
            List<String> baseSidePaymentMethodNames = PaymentMethodSpecUtil.getPaymentMethodNames(baseSidePaymentMethodSpecs);
            if (baseSidePaymentMethodNames.stream().noneMatch(bitcoinPaymentMethodNames::contains)) {
                return false;
            }

            List<String> fiatPaymentMethodNames = PaymentMethodSpecUtil.getPaymentMethodNames(peersOffer.getQuoteSidePaymentMethodSpecs());
            List<FiatPaymentMethodSpec> quoteSidePaymentMethodSpecs = PaymentMethodSpecUtil.createFiatPaymentMethodSpecs(model.getFiatPaymentMethods());
            List<String> quoteSidePaymentMethodNames = PaymentMethodSpecUtil.getPaymentMethodNames(quoteSidePaymentMethodSpecs);
            if (quoteSidePaymentMethodNames.stream().noneMatch(fiatPaymentMethodNames::contains)) {
                return false;
            }

            return BisqEasyTradeAmountLimits.checkOfferAmountLimitForMaxOrFixedAmount(reputationService,
                            userIdentityService,
                            userProfileService,
                            marketPriceService,
                            peersOffer)
                    .map(BisqEasyTradeAmountLimits.Result::isValid)
                    .orElse(false);
        } catch (Throwable t) {
            log.error("Error at TakeOfferPredicate", t);
            return false;
        }
    }

    private Optional<PriceQuote> getMarketPriceQuote() {
        return marketPriceService.findMarketPriceQuote(model.getMarket());
    }

    private void quoteSideAmountsChanged(boolean maxAmountChanged) {
        Monetary minQuoteSideAmount = minAmountComponent.getQuoteSideAmount().get();
        Monetary maxOrFixedQuoteSideAmount = maxOrFixAmountComponent.getQuoteSideAmount().get();

        model.getIsAmountHyperLinkDisabled().set(false);
        boolean insecureValue = maxOrFixAmountComponent.getRightMarkerQuoteSideValue() != null &&
                maxOrFixedQuoteSideAmount.isGreaterThan(maxOrFixAmountComponent.getRightMarkerQuoteSideValue().round(0));
        model.getIsWarningIconVisible().set(insecureValue);

        long highestScore = reputationService.getScoreByUserProfileId().entrySet().stream()
                .filter(e -> userIdentityService.findUserIdentity(e.getKey()).isEmpty())
                .mapToLong(Map.Entry::getValue)
                .max()
                .orElse(0L);
        Market usdBitcoinMarket = MarketRepository.getUSDBitcoinMarket();
        long requiredReputationScoreForMaxOrFixedAmount = BisqEasyTradeAmountLimits.findRequiredReputationScoreByFiatAmount(marketPriceService, usdBitcoinMarket, maxOrFixedQuoteSideAmount).orElse(0L);
        long requiredReputationScoreForMinAmount = BisqEasyTradeAmountLimits.findRequiredReputationScoreByFiatAmount(marketPriceService, usdBitcoinMarket, minQuoteSideAmount).orElse(0L);
        long numPotentialTakersForMaxOrFixedAmount = reputationService.getScoreByUserProfileId().entrySet().stream()
                .filter(e -> userIdentityService.findUserIdentity(e.getKey()).isEmpty())
                .filter(e -> e.getValue() >= requiredReputationScoreForMaxOrFixedAmount || requiredReputationScoreForMaxOrFixedAmount <= MIN_REPUTAION_SCORE)
                .count();
        long numPotentialTakersForMinAmount = reputationService.getScoreByUserProfileId().entrySet().stream()
                .filter(e -> userIdentityService.findUserIdentity(e.getKey()).isEmpty())
                .filter(e -> e.getValue() >= requiredReputationScoreForMinAmount || requiredReputationScoreForMinAmount <= MIN_REPUTAION_SCORE)
                .count();
        model.getAmountLimitInfoLeadLine().set(null);
        Monetary amountWithoutReputationNeeded = BisqEasyTradeAmountLimits.usdToFiat(marketPriceService, model.getMarket(), MAX_USD_TRADE_AMOUNT_WITHOUT_REPUTATION)
                .orElseThrow();
        boolean noReputationNeededForMaxOrFixedAmount = maxOrFixedQuoteSideAmount.isLessThanOrEqual(amountWithoutReputationNeeded);
        if (model.getDirection().isBuy()) {
            // Buyer
            String formattedAmountWithoutReputationNeeded = formatAmountWithCode(amountWithoutReputationNeeded);
            String formattedMaxOrFixedAmount = formatAmountWithCode(maxOrFixedQuoteSideAmount);
            if (model.getIsMinAmountEnabled().get()) {
                // Amount range
                String formattedMinAmount = formatAmountWithCode(minQuoteSideAmount);
                boolean noReputationNeededForMinAmount = minQuoteSideAmount.isLessThanOrEqual(amountWithoutReputationNeeded);
                model.getIsWarningIconVisible().set(noReputationNeededForMaxOrFixedAmount || noReputationNeededForMinAmount);
                if (noReputationNeededForMaxOrFixedAmount) {
                    // max < 25 USD (inherently also min < 25 USD)
                    model.getAmountLimitInfoLeadLine().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo.noReputationNeededForMaxOrFixedAmount", formattedAmountWithoutReputationNeeded));
                    model.getAmountLimitInfo().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo.noReputationNeededForMaxOrFixedAmount.riskInfo", formattedAmountWithoutReputationNeeded));
                    model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.noReputationNeededForMaxOrFixedAmount.info",
                            formattedMaxOrFixedAmount, formattedAmountWithoutReputationNeeded, formattedAmountWithoutReputationNeeded) + "\n\n");
                } else if (noReputationNeededForMinAmount) {
                    // min < 25 USD, max > 25 USD
                    model.getAmountLimitInfoLeadLine().set(Res.get("bisqEasy.tradeWizard.amount.buyer.noReputationNeededForMinAmount.limitInfo.leadLine",
                            formattedAmountWithoutReputationNeeded));
                    String numSellers = Res.getPluralization("bisqEasy.tradeWizard.amount.buyer.numSellers", numPotentialTakersForMaxOrFixedAmount);
                    model.getAmountLimitInfo().set(Res.get("bisqEasy.tradeWizard.amount.buyer.noReputationNeededForMinAmount.limitInfo",
                            formattedMaxOrFixedAmount, numSellers));
                    model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.buyer.noReputationNeededForMinAmount.limitInfo.overlay.info",
                            formattedMinAmount, formattedAmountWithoutReputationNeeded, formattedMaxOrFixedAmount, requiredReputationScoreForMaxOrFixedAmount) + "\n\n");
                } else {
                    // min > 25 USD
                    model.getIsWarningIconVisible().set(numPotentialTakersForMaxOrFixedAmount == 0);
                    if (maxAmountChanged) {
                        String numSellers = Res.getPluralization("bisqEasy.tradeWizard.amount.buyer.numSellers", numPotentialTakersForMaxOrFixedAmount);
                        model.getAmountLimitInfo().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo", numSellers, formattedMaxOrFixedAmount));
                        model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.info", formattedMaxOrFixedAmount, requiredReputationScoreForMaxOrFixedAmount) + "\n\n");
                    } else {
                        String numSellers = Res.getPluralization("bisqEasy.tradeWizard.amount.buyer.numSellers", numPotentialTakersForMinAmount);
                        model.getAmountLimitInfo().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo", numSellers, formattedMinAmount));
                        model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.info", formattedMinAmount, requiredReputationScoreForMinAmount) + "\n\n");
                    }
                }
            } else {
                // Fixed amount
                if (model.isCreateOfferMode()) {
                    // Create offer
                    if (noReputationNeededForMaxOrFixedAmount) {
                        // max < 25 USD (inherently also min < 25 USD)
                        model.getAmountLimitInfoLeadLine().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo.noReputationNeededForMaxOrFixedAmount", formattedAmountWithoutReputationNeeded));
                        model.getAmountLimitInfo().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo.noReputationNeededForMaxOrFixedAmount.riskInfo", formattedAmountWithoutReputationNeeded));
                        model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.noReputationNeededForMaxOrFixedAmount.info",
                                formattedMaxOrFixedAmount, formattedAmountWithoutReputationNeeded, formattedAmountWithoutReputationNeeded) + "\n\n");
                        model.getIsWarningIconVisible().set(true);
                    } else {
                        // min > 25 USD
                        String numSellers = Res.getPluralization("bisqEasy.tradeWizard.amount.buyer.numSellers", numPotentialTakersForMaxOrFixedAmount);
                        model.getAmountLimitInfo().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo", numSellers, formattedMaxOrFixedAmount));
                        model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.info", formattedMaxOrFixedAmount, requiredReputationScoreForMaxOrFixedAmount) + "\n\n");
                        model.getIsWarningIconVisible().set(numPotentialTakersForMaxOrFixedAmount == 0);
                    }
                } else {
                    // Wizard
                    applyMarkerRange();

                    long numMatchingOffers = getNumMatchingOffers(maxOrFixedQuoteSideAmount);
                    String numOffers = Res.getPluralization("bisqEasy.tradeWizard.amount.numOffers", numMatchingOffers);

                    model.getAmountLimitInfoLeadLine().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo.wizard.info.leadLine", numOffers));
                    boolean weakSecurity = maxOrFixedQuoteSideAmount.isLessThanOrEqual(MAX_USD_TRADE_AMOUNT_WITHOUT_REPUTATION);
                    String formatted = formatAmountWithCode(MAX_USD_TRADE_AMOUNT_WITHOUT_REPUTATION);
                    if (weakSecurity) {
                        model.getAmountLimitInfo().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo.wizard.info", formatted));
                    } else {
                        model.getAmountLimitInfo().set(null);
                    }
                    model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo.wizard.overlay.info", formattedMaxOrFixedAmount, formatted) + "\n\n");
                    model.getIsLearnMoreVisible().set(weakSecurity);
                    model.getIsWarningIconVisible().set(weakSecurity || numMatchingOffers == 0);
                    model.getAmountLimitInfoAmount().set(null);
                }
            }
        } else {
            // Seller
            Monetary reputationBasedMaxAmount = model.getReputationBasedMaxAmount().round(0);
            if (model.isCreateOfferMode()) {
                // Create offer
                long myReputationScore = model.getMyReputationScore();
                String formattedReputationBasedMaxAmount = formatAmountWithCode(reputationBasedMaxAmount);
                String formattedAmountWithoutReputationNeeded = formatAmountWithCode(amountWithoutReputationNeeded);
                boolean isToleratedLowAmount = myReputationScore <= MIN_REPUTAION_SCORE;
                if (isToleratedLowAmount) {
                    if (noReputationNeededForMaxOrFixedAmount) {
                        model.getAmountLimitInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.noReputationNeededForMaxOrFixedAmount", formattedAmountWithoutReputationNeeded));
                        model.getAmountLimitInfoAmount().set("");
                        model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.noReputationNeededForMaxOrFixedAmount.overlay.info.scoreTooLow",
                                formattedAmountWithoutReputationNeeded, myReputationScore));
                    } else {
                        model.getAmountLimitInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.scoreTooLow", myReputationScore));
                        model.getAmountLimitInfoAmount().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfoAmount", formattedReputationBasedMaxAmount));
                        model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.overlay.info.scoreTooLow", myReputationScore));
                    }
                } else {
                    model.getAmountLimitInfoAmount().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfoAmount", formattedReputationBasedMaxAmount));
                    if (insecureValue) {
                        model.getAmountLimitInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.inSufficientScore", myReputationScore));
                        model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.overlay.info.inSufficientScore", myReputationScore, formattedReputationBasedMaxAmount));
                    } else {
                        model.getAmountLimitInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.sufficientScore", myReputationScore));
                        model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.overlay.info.sufficientScore", myReputationScore, formattedReputationBasedMaxAmount) + "\n\n");
                    }
                }
            } else {
                // Wizard
                applyMarkerRange();

                long numMatchingOffers = getNumMatchingOffers(maxOrFixedQuoteSideAmount);
                String numOffers = Res.getPluralization("bisqEasy.tradeWizard.amount.numOffers", numMatchingOffers);
                model.getAmountLimitInfoLeadLine().set(Res.get("bisqEasy.tradeWizard.amount.seller.wizard.numMatchingOffers.info", numOffers));
                model.getIsWarningIconVisible().set(numMatchingOffers == 0);

                long myReputationScore = model.getMyReputationScore();
                String formattedReputationBasedMaxAmount = formatAmountWithCode(reputationBasedMaxAmount);
                model.getAmountLimitInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.wizard.limitInfo", myReputationScore));
                model.getIsAmountHyperLinkDisabled().set(true);
                model.getAmountLimitInfoAmount().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfoAmount", formattedReputationBasedMaxAmount));
                model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.wizard.limitInfo.overlay.info", myReputationScore, formattedReputationBasedMaxAmount));
            }
        }
    }

    private void applyQuoteSideMinMaxRange() {
        Monetary maxRangeValue = BisqEasyTradeAmountLimits.usdToFiat(marketPriceService, model.getMarket(), MAX_USD_TRADE_AMOUNT)
                .orElseThrow().round(0);
        Monetary minRangeValue = BisqEasyTradeAmountLimits.usdToFiat(marketPriceService, model.getMarket(), DEFAULT_MIN_USD_TRADE_AMOUNT)
                .orElseThrow().round(0);

        if (model.getReputationBasedMaxAmount() == null) {
            String myProfileId = userIdentityService.getSelectedUserIdentity().getUserProfile().getId();
            long myReputationScore = reputationService.getReputationScore(myProfileId).getTotalScore();
            model.setMyReputationScore(myReputationScore);
            model.setReputationBasedMaxAmount(BisqEasyTradeAmountLimits.getReputationBasedQuoteSideAmount(marketPriceService, model.getMarket(), myReputationScore)
                    .orElse(Fiat.fromValue(0, model.getMarket().getQuoteCurrencyCode()))
            );
        }
        Fiat defaultUsdAmount = MAX_USD_TRADE_AMOUNT_WITHOUT_REPUTATION.multiply(2);
        Monetary defaultFiatAmount = BisqEasyTradeAmountLimits.usdToFiat(marketPriceService, model.getMarket(), defaultUsdAmount)
                .orElseThrow().round(0);
        boolean isCreateOfferMode = model.isCreateOfferMode();
        if (isCreateOfferMode) {
            model.getIsLearnMoreVisible().set(true);
            minAmountComponent.setMinMaxRange(minRangeValue, maxRangeValue);
            maxOrFixAmountComponent.setMinMaxRange(minRangeValue, maxRangeValue);
        } else {
            // Wizard
            applyMarkerRange();

            model.getIsLearnMoreVisible().set(model.getDirection().isSell());
            if (model.getDirection().isBuy()) {
                maxOrFixAmountComponent.setMinMaxRange(minRangeValue, maxRangeValue);
            } else {
                maxOrFixAmountComponent.setMinMaxRange(minRangeValue, model.getReputationBasedMaxAmount().round(0));
            }

            if (maxOrFixAmountComponent.getQuoteSideAmount().get() == null) {
                maxOrFixAmountComponent.setQuoteSideAmount(defaultFiatAmount);
            }
        }

        if (model.getDirection().isBuy()) {
            // Buyer case
            model.setAmountLimitInfoLink(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo.learnMore"));
            model.setLinkToWikiText(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.linkToWikiText"));
            model.getAmountLimitInfoAmount().set("");

            long highestScore = reputationService.getScoreByUserProfileId().entrySet().stream()
                    .filter(e -> userIdentityService.findUserIdentity(e.getKey()).isEmpty())
                    .mapToLong(Map.Entry::getValue)
                    .max()
                    .orElse(0L);
            Monetary highestPossibleUsdAmount = BisqEasyTradeAmountLimits.getUsdAmountFromReputationScore(highestScore);

            if (isCreateOfferMode) {
                minAmountComponent.setRightMarkerQuoteSideValue(highestPossibleUsdAmount);
                maxOrFixAmountComponent.setRightMarkerQuoteSideValue(highestPossibleUsdAmount);
            }
            if (maxOrFixAmountComponent.getQuoteSideAmount().get() == null) {
                maxOrFixAmountComponent.setQuoteSideAmount(defaultFiatAmount);
            }
        } else {
            // Seller case
            if (isCreateOfferMode) {
                model.setLinkToWikiText(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.overlay.linkToWikiText"));
                model.setAmountLimitInfoLink(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.link"));

                Monetary reputationBasedQuoteSideAmount = model.getReputationBasedMaxAmount();
                long myReputationScore = model.getMyReputationScore();
                model.getAmountLimitInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo", myReputationScore));
                minAmountComponent.setRightMarkerQuoteSideValue(reputationBasedQuoteSideAmount);
                maxOrFixAmountComponent.setRightMarkerQuoteSideValue(reputationBasedQuoteSideAmount);
                String formattedAmount = formatAmountWithCode(reputationBasedQuoteSideAmount);

                model.getAmountLimitInfoAmount().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfoAmount", formattedAmount));
                model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.overlay.info.scoreTooLow", myReputationScore, formattedAmount));

                applyReputationBasedQuoteSideAmount();
            } else {
                // Wizard
                model.setLinkToWikiText(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.overlay.linkToWikiText"));
                model.setAmountLimitInfoLink(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.link"));

                Monetary reputationBasedQuoteSideAmount = model.getReputationBasedMaxAmount();
                maxOrFixAmountComponent.setQuoteSideAmount(reputationBasedQuoteSideAmount);
                long myReputationScore = model.getMyReputationScore();
                model.getAmountLimitInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo", myReputationScore));
                String formattedAmount = formatAmountWithCode(reputationBasedQuoteSideAmount);

                model.getAmountLimitInfoAmount().set(Res.get("bisqEasy.tradeWizard.amount.seller.wizard.limitInfo", formattedAmount));
                model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.overlay.info.scoreTooLow", myReputationScore, formattedAmount));

                applyReputationBasedQuoteSideAmount();
            }
        }
    }

    private void applyMarkerRange() {
        Pair<Optional<Monetary>, Optional<Monetary>> availableOfferAmountRange = getLowestAndHighestAmountInAvailableOffers();
        maxOrFixAmountComponent.setLeftMarkerQuoteSideValue(availableOfferAmountRange.getFirst().orElse(null));
        maxOrFixAmountComponent.setRightMarkerQuoteSideValue(availableOfferAmountRange.getSecond().orElse(null));
    }

    private void applyReputationBasedQuoteSideAmount() {
        if (model.isCreateOfferMode()) {
            maxOrFixAmountComponent.setQuoteSideAmount(maxOrFixAmountComponent.getRightMarkerQuoteSideValue().round(0));
        }
    }

    private Pair<Optional<Monetary>, Optional<Monetary>> getLowestAndHighestAmountInAvailableOffers() {
        List<BisqEasyOffer> filteredOffers = bisqEasyOfferbookChannelService.findChannel(model.getMarket()).orElseThrow().getChatMessages().stream()
                .filter(chatMessage -> chatMessage.getBisqEasyOffer().isPresent())
                .map(chatMessage -> chatMessage.getBisqEasyOffer().get())
                .filter(offer -> {
                    if (!isValidDirection(offer)) {
                        return false;
                    }
                    if (!isValidMarket(offer)) {
                        return false;
                    }
                    if (!isValidMakerProfile(offer)) {
                        return false;
                    }

                    Optional<Result> result = checkOfferAmountLimitForMinAmount(reputationService,
                            userIdentityService,
                            userProfileService,
                            marketPriceService,
                            offer);
                    if (!result.map(Result::isValid).orElse(false)) {
                        return false;
                    }

                    return true;
                })
                .toList();
        Optional<Monetary> lowest = filteredOffers.stream()
                .map(offer -> OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, offer))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .min(Monetary::compareTo);

        Optional<Monetary> highest = filteredOffers.stream()
                .map(offer -> {
                    try {
                        Market market = offer.getMarket();
                        Monetary quoteSideMaxOrFixedFiatAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, offer).orElseThrow().round(0);
                        long sellersReputationScore = getSellersReputationScore(reputationService, userIdentityService, userProfileService, offer);
                        Monetary quoteSideMaxOrFixedUsdAmount = fiatToUsd(marketPriceService, market, quoteSideMaxOrFixedFiatAmount).orElseThrow().round(0);
                        long requiredReputationScoreByUsdAmount = getRequiredReputationScoreByUsdAmount(quoteSideMaxOrFixedUsdAmount);
                        long sellersReputationScoreWithTolerance = withTolerance(sellersReputationScore);
                        if (sellersReputationScoreWithTolerance >= requiredReputationScoreByUsdAmount) {
                            return Optional.of(quoteSideMaxOrFixedFiatAmount);
                        } else if (offer.getAmountSpec() instanceof FixedAmountSpec) {
                            // If we have not a range amount we know that offer is not valid, and we return a 0 entry
                            return Optional.<Monetary>empty();
                        }

                        // We have a range amount and max amount is higher as rep score. We use rep score based amount as result.
                        // Min amounts are handled by the filtered collection already.
                        Monetary usdAmountFromSellersReputationScore = getUsdAmountFromReputationScore(sellersReputationScore);
                        Monetary fiatAmountFromSellersReputationScore = usdToFiat(marketPriceService, market, usdAmountFromSellersReputationScore).orElseThrow();
                        return Optional.of(fiatAmountFromSellersReputationScore);
                    } catch (Exception e) {
                        return Optional.<Monetary>empty();
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Monetary::compareTo);
        return new Pair<>(lowest, highest);
    }

    private long getNumMatchingOffers(Monetary quoteSideAmount) {
        return bisqEasyOfferbookChannelService.findChannel(model.getMarket()).orElseThrow().getChatMessages().stream()
                .filter(chatMessage -> chatMessage.getBisqEasyOffer().isPresent())
                .map(chatMessage -> chatMessage.getBisqEasyOffer().get())
                .filter(offer -> {
                    if (!isValidDirection(offer)) {
                        return false;
                    }
                    if (!isValidMarket(offer)) {
                        return false;
                    }
                    if (!isValidMakerProfile(offer)) {
                        return false;
                    }
                    if (!isValidAmountRange(offer)) {
                        return false;
                    }

                    if (!isValidAmountLimit(offer, quoteSideAmount)) {
                        return false;
                    }

                    return true;
                })
                .count();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Filter
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isValidDirection(BisqEasyOffer peersOffer) {
        return peersOffer.getTakersDirection().equals(model.getDirection());
    }

    private boolean isValidMarket(BisqEasyOffer peersOffer) {
        return peersOffer.getMarket().equals(model.getMarket());
    }

    private boolean isValidMakerProfile(BisqEasyOffer peersOffer) {
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

        return true;
    }

    private boolean isValidPaymentMethods(BisqEasyOffer peersOffer) {
        List<String> bitcoinPaymentMethodNames = PaymentMethodSpecUtil.getPaymentMethodNames(peersOffer.getBaseSidePaymentMethodSpecs());
        List<BitcoinPaymentMethodSpec> baseSidePaymentMethodSpecs = PaymentMethodSpecUtil.createBitcoinPaymentMethodSpecs(model.getBitcoinPaymentMethods());
        List<String> baseSidePaymentMethodNames = PaymentMethodSpecUtil.getPaymentMethodNames(baseSidePaymentMethodSpecs);
        if (baseSidePaymentMethodNames.stream().noneMatch(bitcoinPaymentMethodNames::contains)) {
            return false;
        }

        List<String> fiatPaymentMethodNames = PaymentMethodSpecUtil.getPaymentMethodNames(peersOffer.getQuoteSidePaymentMethodSpecs());
        List<FiatPaymentMethodSpec> quoteSidePaymentMethodSpecs = PaymentMethodSpecUtil.createFiatPaymentMethodSpecs(model.getFiatPaymentMethods());
        List<String> quoteSidePaymentMethodNames = PaymentMethodSpecUtil.getPaymentMethodNames(quoteSidePaymentMethodSpecs);
        if (quoteSidePaymentMethodNames.stream().noneMatch(fiatPaymentMethodNames::contains)) {
            return false;
        }

        return true;
    }

    private boolean isValidAmountRange(BisqEasyOffer peersOffer) {
        Optional<Monetary> myQuoteSideMinOrFixedAmount = OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, model.getQuoteSideAmountSpec().get(), MARKET_PRICE_SPEC, model.getMarket());
        Optional<Monetary> peersQuoteSideMaxOrFixedAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, peersOffer);
        if (myQuoteSideMinOrFixedAmount.isEmpty() || peersQuoteSideMaxOrFixedAmount.isEmpty()) {
            return false;
        }
        if (myQuoteSideMinOrFixedAmount.get().round(0).getValue() > peersQuoteSideMaxOrFixedAmount.get().round(0).getValue()) {
            return false;
        }

        Optional<Monetary> myQuoteSideMaxOrFixedAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, model.getQuoteSideAmountSpec().get(), MARKET_PRICE_SPEC, model.getMarket());
        Optional<Monetary> peersQuoteSideMinOrFixedAmount = OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, peersOffer);
        if (myQuoteSideMaxOrFixedAmount.isEmpty() || peersQuoteSideMinOrFixedAmount.isEmpty()) {
            return false;
        }
        if (myQuoteSideMaxOrFixedAmount.get().round(0).getValue() < peersQuoteSideMinOrFixedAmount.get().round(0).getValue()) {
            return false;
        }

        return true;
    }

    private boolean isValidAmountLimit(BisqEasyOffer peersOffer, Monetary quoteSideAmount) {
        Optional<Result> result = BisqEasyTradeAmountLimits.checkOfferAmountLimitForGivenAmount(reputationService,
                userIdentityService,
                userProfileService,
                marketPriceService,
                model.getMarket(),
                quoteSideAmount,
                peersOffer);
        if (!result.map(Result::isValid).orElse(false)) {
            return false;
        }
        return true;
    }


    private boolean isValidAmountLimit(BisqEasyOffer peersOffer) {
        if (!BisqEasyTradeAmountLimits.checkOfferAmountLimitForMaxOrFixedAmount(reputationService,
                        userIdentityService,
                        userProfileService,
                        marketPriceService,
                        peersOffer)
                .map(BisqEasyTradeAmountLimits.Result::isValid)
                .orElse(false)) {
            return false;
        }

        return true;
    }

    // Used for finding best price quote of available matching offers
    private boolean filterOffers(BisqEasyOffer peersOffer) {
        if (!isValidDirection(peersOffer)) {
            return false;
        }
        if (!isValidMarket(peersOffer)) {
            return false;
        }
        if (!isValidPaymentMethods(peersOffer)) {
            return false;
        }
        if (!isValidMakerProfile(peersOffer)) {
            return false;
        }
        if (!isValidAmountRange(peersOffer)) {
            return false;
        }
        if (!isValidAmountLimit(peersOffer)) {
            return false;
        }
        return true;
    }
}
