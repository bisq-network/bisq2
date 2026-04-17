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

package bisq.desktop.main.content.mu_sig.offer.create_offer.backup.amount_and_price.amount;

import bisq.account.payment_method.PaymentMethod;
import bisq.bonded_roles.market_price.MarketBasedAmountConversion;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.MonetaryRange;
import bisq.common.monetary.PriceQuote;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.mu_sig.offer.components.amount_selection.MuSigAmountSelectionController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.mu_sig.MuSigTradeAmountLimits;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.amount.spec.BaseSideAmountSpec;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.amount.spec.BaseSideRangeAmountSpec;
import bisq.offer.mu_sig.draft.CreateOfferDraftWorkflow;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import bisq.user.identity.UserIdentityService;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static bisq.mu_sig.MuSigTradeAmountLimits.MAX_USD_TRADE_AMOUNT;
import static bisq.mu_sig.MuSigTradeAmountLimits.MAX_USD_TRADE_AMOUNT_WITHOUT_REPUTATION;
import static bisq.mu_sig.MuSigTradeAmountLimits.MIN_USD_TRADE_AMOUNT;
import static bisq.mu_sig.MuSigTradeAmountLimits.withTolerance;
import static bisq.presentation.formatters.AmountFormatter.formatQuoteAmountWithCode;

@Slf4j
public class MuSigCreateOfferAmountController implements Controller {
    private static final PriceSpec MARKET_PRICE_SPEC = new MarketPriceSpec();

    private final MuSigCreateOfferAmountModel model;
    @Getter
    private final MuSigCreateOfferAmountView view;
    private final MuSigAmountSelectionController amountSelectionController;
    private final SettingsService settingsService;
    private final MarketPriceService marketPriceService;
    private final Region owner;
    private final ReputationService reputationService;
    private final UserIdentityService userIdentityService;
    private final Consumer<Boolean> navigationButtonsVisibleHandler;
    private final Consumer<NavigationTarget> closeAndNavigateToHandler;
    private final Set<Subscription> subscriptions = new HashSet<>();

    public MuSigCreateOfferAmountController(ServiceProvider serviceProvider,
                                            CreateOfferDraftWorkflow createOfferDraftWorkflow,
                                            Region owner,
                                            Consumer<Boolean> navigationButtonsVisibleHandler,
                                            Consumer<NavigationTarget> closeAndNavigateToHandler) {
        settingsService = serviceProvider.getSettingsService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        reputationService = serviceProvider.getUserService().getReputationService();
        this.owner = owner;
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;
        this.closeAndNavigateToHandler = closeAndNavigateToHandler;
        model = new MuSigCreateOfferAmountModel();

        amountSelectionController = new MuSigAmountSelectionController(serviceProvider, createOfferDraftWorkflow);
        view = new MuSigCreateOfferAmountView(model, this, amountSelectionController.getView().getRoot());
    }


    /* --------------------------------------------------------------------- */
    // Lifecycle
    /* --------------------------------------------------------------------- */

    @Override
    public void onActivate() {
        model.getShouldShowWarningIcon().set(false);
        applyQuoteSideMinMaxRange();

        if (model.getPriceQuote().get() == null && amountSelectionController.getQuote().get() != null) {
            model.getPriceQuote().set(amountSelectionController.getQuote().get());
        }

        Boolean cookieValue = settingsService.getCookie().asBoolean(CookieKey.CREATE_MU_SIG_OFFER_IS_MIN_AMOUNT_ENABLED).orElse(false);
        model.getIsRangeAmountEnabled().set(cookieValue);
        model.getShouldShowHowToBuildReputationButton().set(model.getDisplayDirection().isSell());

        subscriptions.add(EasyBind.subscribe(amountSelectionController.getMinBaseSideAmount(),
                value -> {
                    if (model.getIsRangeAmountEnabled().get()) {
                        if (value != null && amountSelectionController.getMaxOrFixedBaseSideAmount().get() != null &&
                                value.getValue() > amountSelectionController.getMaxOrFixedBaseSideAmount().get().getValue()) {
                            amountSelectionController.setMaxOrFixedBaseSideAmount(value);
                        }
                    }
                }));
        subscriptions.add(EasyBind.subscribe(amountSelectionController.getMaxOrFixedBaseSideAmount(),
                value -> {
                    if (value != null &&
                            model.getIsRangeAmountEnabled().get() &&
                            amountSelectionController.getMinBaseSideAmount().get() != null &&
                            value.getValue() < amountSelectionController.getMinBaseSideAmount().get().getValue()) {
                        amountSelectionController.setMinBaseSideAmount(value);
                    }
                }));

        subscriptions.add(EasyBind.subscribe(amountSelectionController.getMinQuoteSideAmount(),
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
                }));
        subscriptions.add(EasyBind.subscribe(amountSelectionController.getMaxOrFixedQuoteSideAmount(),
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
                }));

        subscriptions.add(EasyBind.subscribe(model.getIsRangeAmountEnabled(), isRangeAmountEnabled -> {
            applyAmountSpec();
            amountSelectionController.setUseRangeAmount(isRangeAmountEnabled);
        }));
        applyAmountSpec();

        subscriptions.add(EasyBind.subscribe(amountSelectionController.getIsDefaultAmountInputBtc(), isDefaultAmountInputBtc -> {
            String quoteCode = model.getPriceQuote().get().getMarket().getQuoteCurrencyCode();
            model.getPriceTooltip().set(amountSelectionController.isDefaultAmountInputBtc()
                    ? Res.get("muSig.offer.wizard.amount.quoteSide.tooltip.fiatAmount.selectedPrice", quoteCode)
                    : Res.get("muSig.offer.wizard.amount.baseSide.tooltip.btcAmount.selectedPrice"));
        }));

        subscriptions.add(EasyBind.subscribe(model.getPriceTooltip(), priceTooltip -> {
            if (priceTooltip != null) {
                amountSelectionController.setTooltip(priceTooltip);
            }
        }));
    }

    @Override
    public void onDeactivate() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        navigationButtonsVisibleHandler.accept(true);
        model.getIsOverlayVisible().set(false);
    }


    /* --------------------------------------------------------------------- */
    // Public API
    /* --------------------------------------------------------------------- */

    public void setDisplayDirection(Direction displayDirection) {
        if (displayDirection == null) {
            return;
        }
        model.setDisplayDirection(displayDirection);
        amountSelectionController.setDirection(displayDirection);
    }

    public void setMarket(Market market) {
        if (market == null) {
            return;
        }
        amountSelectionController.setMarket(market);
        model.setMarket(market);
        applyQuoteSideMinMaxRange();
        applyTradeAmountLimitsInUsd();
    }

    public void setPaymentMethods(List<PaymentMethod<?>> paymentMethods) {
        if (paymentMethods == null) {
            return;
        }
        model.getPaymentMethods().clear();
        model.getPaymentMethods().addAll(paymentMethods);

        applyTradeAmountLimitsInUsd();
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

    public void updateBaseSideAmountSpecWithPriceSpec(PriceSpec priceSpec) {
        if (priceSpec == null) {
            return;
        }

        BaseSideAmountSpec amountSpec = model.getBaseSideAmountSpec().get();
        if (amountSpec == null) {
            return;
        }
        Market market = model.getMarket();
        if (market == null) {
            log.warn("market is null at updateBaseSideAmountSpecWithPriceSpec");
            return;
        }
        Optional<PriceQuote> priceQuote = PriceUtil.findQuote(marketPriceService, priceSpec, market);
        if (priceQuote.isEmpty()) {
            log.warn("priceQuote is empty at updateBaseSideAmountSpecWithPriceSpec");
            return;
        }
        model.getPriceQuote().set(priceQuote.get());
        amountSelectionController.setQuote(priceQuote.get());

        OfferAmountUtil.updateBaseSideAmountSpecWithPriceSpec(marketPriceService, amountSpec, priceSpec, market)
                .ifPresent(baseSideAmountSpec -> model.getBaseSideAmountSpec().set(baseSideAmountSpec));
    }

    public void reset() {
        amountSelectionController.reset();
        model.reset();
    }

    public ReadOnlyObjectProperty<BaseSideAmountSpec> getBaseSideAmountSpec() {
        return model.getBaseSideAmountSpec();
    }

    public ReadOnlyBooleanProperty getIsOverlayVisible() {
        return model.getIsOverlayVisible();
    }


    /* --------------------------------------------------------------------- */
    // UI handlers
    /* --------------------------------------------------------------------- */

    void onKeyPressedWhileShowingOverlay(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {
        });
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onCloseOverlay);
    }

    void onShowOverlay() {
        if (!model.getIsOverlayVisible().get()) {
            navigationButtonsVisibleHandler.accept(false);
            model.getIsOverlayVisible().set(true);
        }
    }

    void onCloseOverlay() {
        if (model.getIsOverlayVisible().get()) {
            navigationButtonsVisibleHandler.accept(true);
            model.getIsOverlayVisible().set(false);
        }
    }

    void onLearnHowToBuildReputation() {
        closeAndNavigateToHandler.accept(NavigationTarget.BUILD_REPUTATION);
    }

    void onOpenWiki(String url) {
        Browser.open(url);
    }

    void onSelectFixedAmount() {
        updateIsRangeAmountEnabled(false);
    }

    void onSelectRangeAmount() {
        updateIsRangeAmountEnabled(true);
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void applyTradeAmountLimitsInUsd() {
        List<PaymentMethod<?>> paymentMethods = model.getPaymentMethods();
        Fiat maxTradeLimitInUsd = paymentMethods.stream()
                .map(PaymentMethod::getPaymentRail)
                .map(MuSigTradeAmountLimits::getMaxTradeLimitInUsd)
                .min(Comparator.naturalOrder())
                .orElse(MAX_USD_TRADE_AMOUNT);
        MonetaryRange tradeAmountLimitsInUsd = new MonetaryRange(MuSigTradeAmountLimits.MIN_USD_TRADE_AMOUNT, maxTradeLimitInUsd);
        amountSelectionController.setTradeAmountLimitsInUsd(tradeAmountLimitsInUsd);
    }


    private void updateIsRangeAmountEnabled(boolean useRangeAmount) {
        model.getIsRangeAmountEnabled().set(useRangeAmount);
        quoteSideAmountsChanged(!useRangeAmount);
        settingsService.setCookie(CookieKey.CREATE_MU_SIG_OFFER_IS_MIN_AMOUNT_ENABLED, useRangeAmount);
    }

    private void applyAmountSpec() {
        Long maxOrFixAmount = getAmountValue(amountSelectionController.getMaxOrFixedBaseSideAmount());
        if (maxOrFixAmount == null) {
            return;
        }

        if (model.getIsRangeAmountEnabled().get()) {
            Long minAmount = getAmountValue(amountSelectionController.getMinBaseSideAmount());
            if (minAmount == null) {
                return;
            }
            if (maxOrFixAmount.compareTo(minAmount) < 0) {
                amountSelectionController.setMinBaseSideAmount(amountSelectionController.getMaxOrFixedBaseSideAmount().get());
                minAmount = getAmountValue(amountSelectionController.getMinBaseSideAmount());
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
            model.getBaseSideAmountSpec().set(new BaseSideFixedAmountSpec(maxOrFixAmount));
        }
    }

    private void applyRangeAmountSpec(long minAmount, long maxOrFixAmount) {
        if (minAmount > 0 && maxOrFixAmount > 0) {
            model.getBaseSideAmountSpec().set(new BaseSideRangeAmountSpec(minAmount, maxOrFixAmount));
        }
    }

    private Optional<PriceQuote> getMarketPriceQuote() {
        return marketPriceService.findMarketPriceQuote(model.getMarket());
    }

    private void quoteSideAmountsChanged(boolean maxAmountChanged) {
        boolean isSeller = model.getDisplayDirection().isSell();
        if (isSeller) {
            return;
        }

        Monetary minQuoteSideAmount = amountSelectionController.getMinQuoteSideAmount().get();
        Monetary maxOrFixedQuoteSideAmount = amountSelectionController.getMaxOrFixedQuoteSideAmount().get();
        // Prevent NPE: nothing to calculate until both ends of the range are set
        if (minQuoteSideAmount == null || maxOrFixedQuoteSideAmount == null) {
            return;
        }

        Market market = model.getMarket();
        long requiredReputationScoreForMaxOrFixedAmount = MuSigTradeAmountLimits.findRequiredReputationScoreByFiatAmount(marketPriceService, market, maxOrFixedQuoteSideAmount).orElse(0L);
        long requiredReputationScoreForMinAmount = MuSigTradeAmountLimits.findRequiredReputationScoreByFiatAmount(marketPriceService, market, minQuoteSideAmount).orElse(0L);
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
        model.getLearnMoreVisible().set(true);
        if (model.getIsRangeAmountEnabled().get()) {
            // At range amount we use the min amount
            String numSellers = Res.getPluralization("muSig.offer.create.amount.numSellers.buyer", numPotentialTakersForMinAmount);
            model.getAmountLimitInfo().set(Res.get("muSig.offer.create.amount.limitInfo.buyer", numSellers));
            model.setAmountLimitInfoLink(Res.get("muSig.offer.create.amount.limitInfo.learnMore.buyer"));

            String formattedMinAmount = formatQuoteAmountWithCode(minQuoteSideAmount);
            String firstPart = Res.get("muSig.offer.create.amount.limitInfo.overlay.info.firstPart.buyer", formattedMinAmount, requiredReputationScoreForMinAmount);
            String secondPart;
            if (numPotentialTakersForMinAmount == 0) {
                model.getShouldShowWarningIcon().set(true);
                secondPart = Res.get("muSig.offer.create.amount.limitInfo.overlay.info.secondPart.noSellers.buyer");
            } else {
                secondPart = numPotentialTakersForMinAmount == 1
                        ? Res.get("muSig.offer.create.amount.limitInfo.overlay.info.secondPart.singular.buyer", numSellers)
                        : Res.get("muSig.offer.create.amount.limitInfo.overlay.info.secondPart.plural.buyer", numSellers);
            }
            model.getAmountLimitInfoOverlayInfo().set(firstPart + "\n\n" + secondPart + "\n\n");
        } else {
            // Fixed amount
            String numSellers = Res.getPluralization("muSig.offer.create.amount.numSellers.buyer", numPotentialTakersForMaxOrFixedAmount);
            model.getAmountLimitInfo().set(Res.get("muSig.offer.create.amount.limitInfo.buyer", numSellers));
            model.setAmountLimitInfoLink(Res.get("muSig.offer.create.amount.limitInfo.learnMore.buyer"));
            String firstPart = Res.get("muSig.offer.create.amount.limitInfo.overlay.info.firstPart.buyer", formattedMaxOrFixedAmount, requiredReputationScoreForMaxOrFixedAmount);
            String secondPart;
            if (numPotentialTakersForMaxOrFixedAmount == 0) {
                model.getShouldShowWarningIcon().set(true);
                secondPart = Res.get("muSig.offer.create.amount.limitInfo.overlay.info.secondPart.noSellers.buyer");
            } else {
                secondPart = numPotentialTakersForMaxOrFixedAmount == 1
                        ? Res.get("muSig.offer.create.amount.limitInfo.overlay.info.secondPart.singular.buyer", numSellers)
                        : Res.get("muSig.offer.create.amount.limitInfo.overlay.info.secondPart.plural.buyer", numSellers);
            }
            model.getAmountLimitInfoOverlayInfo().set(firstPart + "\n\n" + secondPart + "\n\n");
        }
    }

    private void applyQuoteSideMinMaxRange() {
        Market market = model.getMarket();

        Monetary minRangeValue = market.isCrypto()
                ? MarketBasedAmountConversion.usdToBtc(marketPriceService, MIN_USD_TRADE_AMOUNT).orElseThrow()
                : MarketBasedAmountConversion.usdToFiat(marketPriceService, market, MIN_USD_TRADE_AMOUNT)
                .orElseThrow().round(0);

        Monetary maxRangeValue = market.isCrypto()
                ? MarketBasedAmountConversion.usdToBtc(marketPriceService, MAX_USD_TRADE_AMOUNT).orElseThrow()
                : MarketBasedAmountConversion.usdToFiat(marketPriceService, market, MAX_USD_TRADE_AMOUNT)
                .orElseThrow().round(0);

        applyMaxAmountBasedOnReputation();

        Fiat defaultUsdAmount = MAX_USD_TRADE_AMOUNT_WITHOUT_REPUTATION.multiply(2);
        Monetary defaultAmount = market.isCrypto()
                ? MarketBasedAmountConversion.usdToBtc(marketPriceService, defaultUsdAmount).orElseThrow()
                : MarketBasedAmountConversion.usdToFiat(marketPriceService, market, defaultUsdAmount)
                .orElseThrow().round(0);
        boolean isBuyer = model.getDisplayDirection().isBuy();
        Monetary reputationBasedMaxAmount = model.getReputationBasedMaxAmount().round(0);
        amountSelectionController.setMaxAllowedLimitation(maxRangeValue);
        model.getLearnMoreVisible().set(true);

        // We keep the feedback and overlay code for now as we might have usage later for it.
        // If not, we can remove all related code. Currently, it's just a copy of Bisq Easy...
        model.getShouldShowAmountLimitInfo().set(false);

        if (isBuyer) {
            //model.getShouldShowAmountLimitInfo().set(true);
            amountSelectionController.setQuoteSideTradeAmountLimits(new MonetaryRange(minRangeValue, maxRangeValue));
        } else {
            boolean hasNotReachedAmountLimit = reputationBasedMaxAmount.getValue() < maxRangeValue.getValue();
            //model.getShouldShowAmountLimitInfo().set(hasNotReachedAmountLimit);
            if (reputationBasedMaxAmount.getValue() < minRangeValue.getValue()) {
                minRangeValue = reputationBasedMaxAmount;
            }
            if (reputationBasedMaxAmount.getValue() < maxRangeValue.getValue()) {
                maxRangeValue = reputationBasedMaxAmount;
            }
            amountSelectionController.setQuoteSideTradeAmountLimits(new MonetaryRange(minRangeValue, maxRangeValue));
        }

        if (isBuyer) {
            // Buyer case
            model.setLinkToWikiText(Res.get("muSig.offer.create.amount.limitInfo.overlay.linkToWikiText.buyer"));
            model.setAmountLimitInfoLink(Res.get("muSig.offer.create.amount.limitInfo.learnMore.buyer"));

            long highestScore = reputationService.getScoreByUserProfileId().entrySet().stream()
                    .filter(e -> userIdentityService.findUserIdentity(e.getKey()).isEmpty())
                    .mapToLong(Map.Entry::getValue)
                    .max()
                    .orElse(0L);
            Monetary highestPossibleAmountFromSellers = MuSigTradeAmountLimits.getReputationBasedQuoteSideAmount(marketPriceService, market, highestScore)
                    .orElseGet(() -> Monetary.from(0, market.getQuoteCurrencyCode()));
            amountSelectionController.setRightMarkerQuoteSideValue(highestPossibleAmountFromSellers);
            if (amountSelectionController.getMaxOrFixedQuoteSideAmount().get() == null) {
                amountSelectionController.setMaxOrFixedQuoteSideAmount(defaultAmount);
            }
        } else {
            // Seller case
            model.setLinkToWikiText(Res.get("muSig.offer.wizard.amount.limitInfo.overlay.linkToWikiText.seller"));
            model.setAmountLimitInfoLink(Res.get("muSig.offer.wizard.amount.limitInfo.link.seller"));
            Monetary reputationBasedQuoteSideAmount = model.getReputationBasedMaxAmount();
            long myReputationScore = model.getMyReputationScore();
            String formattedAmount = formatQuoteAmountWithCode(reputationBasedQuoteSideAmount.round(0));
            model.getAmountLimitInfo().set(Res.get("muSig.offer.create.amount.limitInfo.seller", formattedAmount));
            model.getAmountLimitInfoOverlayInfo().set(Res.get("muSig.offer.create.amount.limitInfo.overlay.seller", myReputationScore, formattedAmount));
            amountSelectionController.setRightMarkerQuoteSideValue(reputationBasedQuoteSideAmount);
            applyReputationBasedQuoteSideAmount();
        }
    }

    private void applyMaxAmountBasedOnReputation() {
        String myProfileId = userIdentityService.getSelectedUserIdentity().getUserProfile().getId();
        long myReputationScore = reputationService.getReputationScore(myProfileId).getTotalScore();
        model.setMyReputationScore(myReputationScore);
        model.setReputationBasedMaxAmount(MuSigTradeAmountLimits.getReputationBasedQuoteSideAmount(marketPriceService, model.getMarket(), myReputationScore)
                .orElseGet(() -> Fiat.fromValue(0, model.getMarket().getQuoteCurrencyCode()))
        );
    }

    private void applyReputationBasedQuoteSideAmount() {
        amountSelectionController.setMaxOrFixedQuoteSideAmount(amountSelectionController.getRightMarkerQuoteSideValue().round(0));
    }
}
