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

package bisq.desktop.main.content.bisq_easy.trade_wizard.amount_and_price.amount;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.bisq_easy.BisqEasyTradeAmountLimits;
import bisq.bisq_easy.NavigationTarget;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.common.currency.Market;
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
import bisq.desktop.main.content.bisq_easy.components.amount_selection.AmountSelectionController;
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
import java.util.function.Consumer;
import java.util.stream.Stream;

import static bisq.bisq_easy.BisqEasyTradeAmountLimits.*;
import static bisq.presentation.formatters.AmountFormatter.formatQuoteAmountWithCode;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class TradeWizardAmountController implements Controller {
    private static final PriceSpec MARKET_PRICE_SPEC = new MarketPriceSpec();

    private final TradeWizardAmountModel model;
    @Getter
    private final TradeWizardAmountView view;
    private final AmountSelectionController amountSelectionController;
    private final SettingsService settingsService;
    private final MarketPriceService marketPriceService;
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final Region owner;
    private final UserProfileService userProfileService;
    private final ReputationService reputationService;
    private final UserIdentityService userIdentityService;
    private final Consumer<Boolean> navigationButtonsVisibleHandler;
    private final Consumer<NavigationTarget> closeAndNavigateToHandler;
    private Subscription isRangeAmountEnabledPin, maxOrFixAmountCompBaseSideAmountPin, minAmountCompBaseSideAmountPin,
            maxAmountCompQuoteSideAmountPin, minAmountCompQuoteSideAmountPin, priceTooltipPin;

    public TradeWizardAmountController(ServiceProvider serviceProvider,
                                       Region owner,
                                       Consumer<Boolean> navigationButtonsVisibleHandler,
                                       Consumer<NavigationTarget> closeAndNavigateToHandler) {
        settingsService = serviceProvider.getSettingsService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        reputationService = serviceProvider.getUserService().getReputationService();
        bisqEasyOfferbookChannelService = serviceProvider.getChatService().getBisqEasyOfferbookChannelService();
        this.owner = owner;
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;
        this.closeAndNavigateToHandler = closeAndNavigateToHandler;
        model = new TradeWizardAmountModel();

        amountSelectionController = new AmountSelectionController(serviceProvider);
        view = new TradeWizardAmountView(model, this, amountSelectionController);
    }

    public void setIsCreateOfferMode(boolean isCreateOfferMode) {
        model.setCreateOfferMode(isCreateOfferMode);
        model.getShowRangeAmounts().set(isCreateOfferMode);
        if (!isCreateOfferMode) {
            model.getIsRangeAmountEnabled().set(false);
        }
    }

    public void setDirection(Direction direction) {
        if (direction == null) {
            return;
        }
        model.setDirection(direction);
        amountSelectionController.setDirection(direction);
    }

    public void setMarket(Market market) {
        if (market == null) {
            return;
        }
        amountSelectionController.setMarket(market);
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
        amountSelectionController.setQuote(priceQuote.get());

        OfferAmountUtil.updateQuoteSideAmountSpecWithPriceSpec(marketPriceService, amountSpec, priceSpec, market)
                .ifPresent(quoteSideAmountSpec -> model.getQuoteSideAmountSpec().set(quoteSideAmountSpec));
    }

    public void reset() {
        amountSelectionController.reset();
        model.reset();
    }

    public ReadOnlyObjectProperty<QuoteSideAmountSpec> getQuoteSideAmountSpec() {
        return model.getQuoteSideAmountSpec();
    }

    public ReadOnlyBooleanProperty getIsOverlayVisible() {
        return model.getIsOverlayVisible();
    }

    @Override
    public void onActivate() {
        model.getShouldShowWarningIcon().set(false);
        applyQuoteSideMinMaxRange();

        if (model.getPriceQuote().get() == null && amountSelectionController.getQuote().get() != null) {
            model.getPriceQuote().set(amountSelectionController.getQuote().get());
        }

        Boolean cookieValue = settingsService.getCookie().asBoolean(CookieKey.CREATE_BISQ_EASY_OFFER_IS_MIN_AMOUNT_ENABLED).orElse(false);
        model.getIsRangeAmountEnabled().set(cookieValue && model.getShowRangeAmounts().get());
        model.getShouldShowHowToBuildReputationButton().set(model.isCreateOfferMode() && model.getDirection().isSell());

        minAmountCompBaseSideAmountPin = EasyBind.subscribe(amountSelectionController.getMinBaseSideAmount(),
                value -> {
                    if (model.getIsRangeAmountEnabled().get()) {
                        if (value != null && amountSelectionController.getMaxOrFixedBaseSideAmount().get() != null &&
                                value.getValue() > amountSelectionController.getMaxOrFixedBaseSideAmount().get().getValue()) {
                            amountSelectionController.setMaxOrFixedBaseSideAmount(value);
                        }
                    }
                });
        maxOrFixAmountCompBaseSideAmountPin = EasyBind.subscribe(amountSelectionController.getMaxOrFixedBaseSideAmount(),
                value -> {
                    if (value != null &&
                            model.getIsRangeAmountEnabled().get() &&
                            amountSelectionController.getMinBaseSideAmount().get() != null &&
                            value.getValue() < amountSelectionController.getMinBaseSideAmount().get().getValue()) {
                        amountSelectionController.setMinBaseSideAmount(value);
                    }
                });

        minAmountCompQuoteSideAmountPin = EasyBind.subscribe(amountSelectionController.getMinQuoteSideAmount(),
                value -> {
                    if (value != null) {
                        if (model.getIsRangeAmountEnabled().get() &&
                                amountSelectionController.getMaxOrFixedQuoteSideAmount().get() != null &&
                                value.getValue() > amountSelectionController.getMaxOrFixedQuoteSideAmount().get().getValue()) {
                            amountSelectionController.setMaxOrFixedQuoteSideAmount(value);
                        }
                        applyAmountSpec();
                        quoteSideAmountsChanged(false);
                    }
                });
        maxAmountCompQuoteSideAmountPin = EasyBind.subscribe(amountSelectionController.getMaxOrFixedQuoteSideAmount(),
                value -> {
                    if (value != null) {
                        if (model.getIsRangeAmountEnabled().get() &&
                                amountSelectionController.getMinQuoteSideAmount().get() != null &&
                                value.getValue() < amountSelectionController.getMinQuoteSideAmount().get().getValue()) {
                            amountSelectionController.setMinQuoteSideAmount(value);
                        }
                        applyAmountSpec();
                        quoteSideAmountsChanged(true);
                    }
                });

        isRangeAmountEnabledPin = EasyBind.subscribe(model.getIsRangeAmountEnabled(), isRangeAmountEnabled -> {
            applyAmountSpec();
            amountSelectionController.setIsRangeAmountEnabled(isRangeAmountEnabled);
        });
        applyAmountSpec();

        if (model.isCreateOfferMode()) {
            model.getPriceTooltip().set(Res.get("bisqEasy.component.amount.baseSide.tooltip.btcAmount.selectedPrice"));
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
                amountSelectionController.setTooltip(priceTooltip);
            }
        });
    }

    @Override
    public void onDeactivate() {
        isRangeAmountEnabledPin.unsubscribe();
        maxOrFixAmountCompBaseSideAmountPin.unsubscribe();
        maxAmountCompQuoteSideAmountPin.unsubscribe();
        minAmountCompBaseSideAmountPin.unsubscribe();
        minAmountCompQuoteSideAmountPin.unsubscribe();
        priceTooltipPin.unsubscribe();
        view.getRoot().setOnKeyPressed(null);
        navigationButtonsVisibleHandler.accept(true);
        model.getIsOverlayVisible().set(false);
    }

    void onShowOverlay() {
        navigationButtonsVisibleHandler.accept(false);
        model.getIsOverlayVisible().set(true);
        view.getRoot().setOnKeyPressed(keyEvent -> {
            KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {
            });
            KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onCloseOverlay);
        });
    }

    void onCloseOverlay() {
        view.getRoot().setOnKeyPressed(null);
        navigationButtonsVisibleHandler.accept(true);
        model.getIsOverlayVisible().set(false);
    }

    void onLearnHowToBuildReputation() {
        closeAndNavigateToHandler.accept(NavigationTarget.BUILD_REPUTATION);
    }

    void onOpenWiki(String url) {
        Browser.open(url);
    }

    void useFixedAmount() {
        updateIsRangeAmountEnabled(false);
    }

    void useRangeAmount() {
        updateIsRangeAmountEnabled(true);
    }

    private void updateIsRangeAmountEnabled(boolean useRangeAmount) {
        model.getIsRangeAmountEnabled().set(useRangeAmount);
        quoteSideAmountsChanged(!useRangeAmount);
        settingsService.setCookie(CookieKey.CREATE_BISQ_EASY_OFFER_IS_MIN_AMOUNT_ENABLED, useRangeAmount);
    }

    private void applyAmountSpec() {
        Long maxOrFixAmount = getAmountValue(amountSelectionController.getMaxOrFixedQuoteSideAmount());
        if (maxOrFixAmount == null) {
            return;
        }

        if (model.getIsRangeAmountEnabled().get()) {
            Long minAmount = getAmountValue(amountSelectionController.getMinQuoteSideAmount());
            checkNotNull(minAmount);
            if (maxOrFixAmount.compareTo(minAmount) < 0) {
                amountSelectionController.setMinQuoteSideAmount(amountSelectionController.getMaxOrFixedQuoteSideAmount().get());
                minAmount = getAmountValue(amountSelectionController.getMinQuoteSideAmount());
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
        if (maxOrFixAmount > 0) {
            model.getQuoteSideAmountSpec().set(new QuoteSideFixedAmountSpec(maxOrFixAmount));
        }
    }

    private void applyRangeAmountSpec(long minAmount, long maxOrFixAmount) {
        if (minAmount > 0 && maxOrFixAmount > 0) {
            model.getQuoteSideAmountSpec().set(new QuoteSideRangeAmountSpec(minAmount, maxOrFixAmount));
        }
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
            amountSelectionController.setQuote(bestOffersPrice.get());
        } else {
            getMarketPriceQuote().ifPresent(amountSelectionController::setQuote);
        }
        AmountSpecUtil.findQuoteSideFixedAmountFromSpec(model.getQuoteSideAmountSpec().get(), model.getMarket().getQuoteCurrencyCode())
                .ifPresent(amount -> UIThread.runOnNextRenderFrame(() -> amountSelectionController.setMaxOrFixedQuoteSideAmount(amount)));

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
        boolean isBuyer = model.getDirection().isBuy();
        if (!isBuyer) {
            return;
        }

        Monetary minQuoteSideAmount = amountSelectionController.getMinQuoteSideAmount().get();
        Monetary maxOrFixedQuoteSideAmount = amountSelectionController.getMaxOrFixedQuoteSideAmount().get();

        Market market = model.getMarket();
        long requiredReputationScoreForMaxOrFixedAmount = BisqEasyTradeAmountLimits.findRequiredReputationScoreByFiatAmount(marketPriceService, market, maxOrFixedQuoteSideAmount).orElse(0L);
        long requiredReputationScoreForMinAmount = BisqEasyTradeAmountLimits.findRequiredReputationScoreByFiatAmount(marketPriceService, market, minQuoteSideAmount).orElse(0L);
        long numPotentialTakersForMaxOrFixedAmount = reputationService.getScoreByUserProfileId().entrySet().stream()
                .filter(e -> userIdentityService.findUserIdentity(e.getKey()).isEmpty())
                .filter(e -> withTolerance(e.getValue()) >= requiredReputationScoreForMaxOrFixedAmount)
                .count();
        long numPotentialTakersForMinAmount = reputationService.getScoreByUserProfileId().entrySet().stream()
                .filter(e -> userIdentityService.findUserIdentity(e.getKey()).isEmpty())
                .filter(e -> withTolerance(e.getValue()) >= requiredReputationScoreForMinAmount)
                .count();
        String formattedMaxOrFixedAmount = formatQuoteAmountWithCode(maxOrFixedQuoteSideAmount);
        model.getShouldShowWarningIcon().set(false);
        if (model.isCreateOfferMode()) {
            // Create offer
            if (model.getIsRangeAmountEnabled().get() && !maxAmountChanged) {
                // Use min amount
                String numSellers = Res.getPluralization("bisqEasy.tradeWizard.amount.buyer.numSellers", numPotentialTakersForMinAmount);
                model.getAmountLimitInfo().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo", numSellers));

                String formattedMinAmount = formatQuoteAmountWithCode(minQuoteSideAmount);
                if (numPotentialTakersForMinAmount == 0) {
                    model.getShouldShowWarningIcon().set(true);
                    model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.info.noSellers",
                            formattedMinAmount, requiredReputationScoreForMinAmount) + "\n\n");
                } else {
                    model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.info.wSellers",
                            formattedMinAmount, requiredReputationScoreForMinAmount, numSellers) + "\n\n");
                }
            } else {
                // Use maxOrFixed amount
                String numSellers = Res.getPluralization("bisqEasy.tradeWizard.amount.buyer.numSellers", numPotentialTakersForMaxOrFixedAmount);
                model.getAmountLimitInfo().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo", numSellers));

                if (numPotentialTakersForMaxOrFixedAmount == 0) {
                    model.getShouldShowWarningIcon().set(true);
                    model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.info.noSellers",
                            formattedMaxOrFixedAmount, requiredReputationScoreForMaxOrFixedAmount) + "\n\n");
                } else {
                    model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.info.wSellers",
                            formattedMaxOrFixedAmount, requiredReputationScoreForMaxOrFixedAmount, numSellers) + "\n\n");
                }
            }
        } else {
            // Wizard
            applyMarkerRange();

            long numMatchingOffers = getNumMatchingOffers(maxOrFixedQuoteSideAmount);
            String numOffers = Res.getPluralization("bisqEasy.tradeWizard.amount.numOffers", numMatchingOffers);

            boolean weakSecurity = maxOrFixedQuoteSideAmount.isLessThanOrEqual(MAX_USD_TRADE_AMOUNT_WITHOUT_REPUTATION);
            String formatted = formatQuoteAmountWithCode(MAX_USD_TRADE_AMOUNT_WITHOUT_REPUTATION);
            if (weakSecurity) {
                model.getAmountLimitInfo().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo.wizard.info", formatted));
            } else {
                model.getAmountLimitInfo().set(null);
            }
            model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo.wizard.overlay.info", formattedMaxOrFixedAmount, formatted) + "\n\n");
        }
    }

    private void applyQuoteSideMinMaxRange() {
        Market market = model.getMarket();
        Monetary maxRangeValue = BisqEasyTradeAmountLimits.usdToFiat(marketPriceService, market, MAX_USD_TRADE_AMOUNT)
                .orElseThrow().round(0);
        Monetary minRangeValue = BisqEasyTradeAmountLimits.usdToFiat(marketPriceService, market, DEFAULT_MIN_USD_TRADE_AMOUNT)
                .orElseThrow().round(0);

        applyMaxAmountBasedOnReputation();

        Fiat defaultUsdAmount = MAX_USD_TRADE_AMOUNT_WITHOUT_REPUTATION.multiply(2);
        Monetary defaultFiatAmount = BisqEasyTradeAmountLimits.usdToFiat(marketPriceService, market, defaultUsdAmount)
                .orElseThrow().round(0);
        boolean isCreateOfferMode = model.isCreateOfferMode();
        boolean isBuyer = model.getDirection().isBuy();
        Monetary reputationBasedMaxAmount = model.getReputationBasedMaxAmount().round(0);
        if (isCreateOfferMode) {
            amountSelectionController.setMaxAllowedLimitation(maxRangeValue);
            if (isBuyer) {
                model.getShouldShowAmountLimitInfo().set(true);
                amountSelectionController.setMinMaxRange(minRangeValue, maxRangeValue);
            } else {
                boolean hasNotReachedAmountLimit = reputationBasedMaxAmount.getValue() < maxRangeValue.getValue();
                model.getShouldShowAmountLimitInfo().set(hasNotReachedAmountLimit);
                if (reputationBasedMaxAmount.getValue() < minRangeValue.getValue()) {
                    minRangeValue = reputationBasedMaxAmount;
                }
                if (reputationBasedMaxAmount.getValue() < maxRangeValue.getValue()) {
                    maxRangeValue = reputationBasedMaxAmount;
                }
                amountSelectionController.setMinMaxRange(minRangeValue, maxRangeValue);
            }
        } else {
            // Wizard
            applyMarkerRange();

            if (isBuyer) {
                amountSelectionController.setMinMaxRange(minRangeValue, maxRangeValue);
            } else {
                model.getShouldShowAmountLimitInfo().set(reputationBasedMaxAmount.getValue() < maxRangeValue.getValue());
                amountSelectionController.setMinMaxRange(minRangeValue, reputationBasedMaxAmount);
            }

            if (amountSelectionController.getMaxOrFixedQuoteSideAmount().get() == null) {
                amountSelectionController.setMaxOrFixedQuoteSideAmount(defaultFiatAmount);
            }
        }

        if (isBuyer) {
            // Buyer case
            model.setLinkToWikiText(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.linkToWikiText"));
            model.setAmountLimitInfoLink(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo.learnMore"));

            long highestScore = reputationService.getScoreByUserProfileId().entrySet().stream()
                    .filter(e -> userIdentityService.findUserIdentity(e.getKey()).isEmpty())
                    .mapToLong(Map.Entry::getValue)
                    .max()
                    .orElse(0L);
            Monetary highestPossibleAmountFromSellers = BisqEasyTradeAmountLimits.getReputationBasedQuoteSideAmount(marketPriceService, market, highestScore)
                    .orElse(Fiat.from(0, market.getQuoteCurrencyCode()));
            if (isCreateOfferMode) {
                amountSelectionController.setRightMarkerQuoteSideValue(highestPossibleAmountFromSellers);
            }
            if (amountSelectionController.getMaxOrFixedQuoteSideAmount().get() == null) {
                amountSelectionController.setMaxOrFixedQuoteSideAmount(defaultFiatAmount);
            }
        } else {
            // Seller case
            model.setLinkToWikiText(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.overlay.linkToWikiText"));
            model.setAmountLimitInfoLink(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.link"));
            Monetary reputationBasedQuoteSideAmount = model.getReputationBasedMaxAmount();
            long myReputationScore = model.getMyReputationScore();
            String formattedAmount = formatQuoteAmountWithCode(reputationBasedQuoteSideAmount);
            model.getAmountLimitInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo", formattedAmount));
            model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.overlay", myReputationScore, formattedAmount));
            if (isCreateOfferMode) {
                amountSelectionController.setRightMarkerQuoteSideValue(reputationBasedQuoteSideAmount);
                applyReputationBasedQuoteSideAmount();
            } else {
                // Wizard
                amountSelectionController.setMaxOrFixedQuoteSideAmount(reputationBasedQuoteSideAmount);
                applyReputationBasedQuoteSideAmount();
            }
        }
    }

    private void applyMaxAmountBasedOnReputation() {
        String myProfileId = userIdentityService.getSelectedUserIdentity().getUserProfile().getId();
        long myReputationScore = reputationService.getReputationScore(myProfileId).getTotalScore();
        model.setMyReputationScore(myReputationScore);
        model.setReputationBasedMaxAmount(BisqEasyTradeAmountLimits.getReputationBasedQuoteSideAmount(marketPriceService, model.getMarket(), myReputationScore)
                .orElse(Fiat.fromValue(0, model.getMarket().getQuoteCurrencyCode()))
        );
    }

    private void applyMarkerRange() {
        Pair<Optional<Monetary>, Optional<Monetary>> availableOfferAmountRange = getLowestAndHighestAmountInAvailableOffers();
        amountSelectionController.setLeftMarkerQuoteSideValue(availableOfferAmountRange.getFirst().orElse(null));
        amountSelectionController.setRightMarkerQuoteSideValue(availableOfferAmountRange.getSecond().orElse(null));
    }

    private void applyReputationBasedQuoteSideAmount() {
        if (model.isCreateOfferMode()) {
            amountSelectionController.setMaxOrFixedQuoteSideAmount(amountSelectionController.getRightMarkerQuoteSideValue().round(0));
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

    /* --------------------------------------------------------------------- */
    // Filter
    /* --------------------------------------------------------------------- */

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
