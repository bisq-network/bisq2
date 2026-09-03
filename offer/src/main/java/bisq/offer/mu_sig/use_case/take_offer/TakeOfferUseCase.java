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

package bisq.offer.mu_sig.use_case.take_offer;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.MonetaryRange;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmountConversion;
import bisq.common.monetary.TradeAmountRange;
import bisq.common.observable.Pin;
import bisq.common.monetary.TradeAmount;
import bisq.identity.IdentityService;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.AmountSpecUtil;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.mu_sig.use_case.DraftOfferUseCase;
import bisq.offer.mu_sig.use_case.create_offer.amount.AmountSelection;
import bisq.offer.mu_sig.use_case.create_offer.amount.limits.AbsoluteAmountLimitsProvider;
import bisq.offer.mu_sig.use_case.create_offer.amount.limits.PaymentMethodBasedAmountLimitsProvider;
import bisq.offer.mu_sig.use_case.create_offer.amount.limits.TradeAmountLimitUtils;
import bisq.offer.mu_sig.use_case.create_offer.amount.limits.UserSpecificAmountLimitsProvider;
import bisq.offer.mu_sig.use_case.create_offer.payment_method.PaymentMethodSelection;
import bisq.offer.mu_sig.use_case.create_offer.price.limits.PriceLimits;
import bisq.offer.mu_sig.use_case.take_offer.TakeOfferValidationException.Reason;
import bisq.offer.mu_sig.use_case.take_offer.payment_method.PaymentMethodSelectionService;
import bisq.offer.options.AccountOption;
import bisq.offer.options.CollateralOption;
import bisq.offer.options.OfferOption;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.offer.mu_sig.use_case.dependencies.AccountsProvider;
import bisq.offer.mu_sig.use_case.dependencies.DefaultAccountsProvider;
import bisq.offer.mu_sig.use_case.dependencies.DefaultTakeOfferDraftCookieStore;
import bisq.offer.mu_sig.use_case.dependencies.TakeOfferDraftCookieStore;
import bisq.offer.mu_sig.use_case.take_offer.amount.TakeOfferAmountService;
import bisq.offer.mu_sig.use_case.take_offer.direction.TakeOfferDirectionService;
import bisq.offer.mu_sig.use_case.take_offer.market.TakeOfferMarketService;
import bisq.offer.mu_sig.use_case.take_offer.payment_method.TakeOfferPaymentMethodService;
import bisq.offer.mu_sig.use_case.take_offer.price.TakeOfferPriceService;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class TakeOfferUseCase extends DraftOfferUseCase {
    @Getter
    private final TakeOfferMarketService marketService;
    @Getter
    private final TakeOfferDirectionService directionService;
    @Getter
    private final TakeOfferPriceService priceService;
    @Getter
    private final TakeOfferAmountService amountService;
    private final TakeOfferDraftCookieStore cookieStore;
    @Getter
    private final TakeOfferPaymentMethodService paymentMethodService;
    private final MarketPriceService marketPriceService;
    private final IdentityService identityService;
    @Nullable
    private MuSigOffer muSigOffer;
    @Nullable
    private Pin marketPricePin;
    private boolean rangeCollapsed;
    // True while the published amount constraints could not be refreshed (empty intersection or
    // incomputable limits): user edits must not re-validate against the stale published ranges.
    private boolean amountConstraintsStale;
    // The published amount validity has two independent causes: the constraints cause is owned
    // by initialization and recomputation, the user-input cause solely by the input entry
    // points. Splitting them keeps a cleared or unapplicable input blocking across background
    // recomputations that would otherwise re-validate the retained pair.
    private boolean amountConstraintsCauseValid = true;
    private boolean userAmountInputCauseValid = true;


    /* --------------------------------------------------------------------- */
    // Construction
    /* --------------------------------------------------------------------- */

    public TakeOfferUseCase(MarketPriceService marketPriceService,
                            IdentityService identityService,
                            SettingsService settingsService,
                            AccountService accountService) {
        this(marketPriceService,
                identityService,
                new DefaultTakeOfferDraftCookieStore(settingsService),
                new DefaultAccountsProvider(accountService));
    }

    TakeOfferUseCase(MarketPriceService marketPriceService,
                     IdentityService identityService,
                     TakeOfferDraftCookieStore cookieStore,
                     AccountsProvider accountsProvider) {
        marketService = new TakeOfferMarketService();
        directionService = new TakeOfferDirectionService();
        priceService = new TakeOfferPriceService();
        amountService = new TakeOfferAmountService();
        this.cookieStore = checkNotNull(cookieStore, "cookieStore must not be null");
        checkNotNull(accountsProvider, "accountsProvider must not be null");
        this.marketPriceService = checkNotNull(marketPriceService, "marketPriceService must not be null");
        this.identityService = checkNotNull(identityService, "identityService must not be null");

        PaymentMethodSelectionService paymentMethodSelectionService = new PaymentMethodSelectionService(accountsProvider);


        paymentMethodService = new TakeOfferPaymentMethodService(paymentMethodSelectionService);
        // A payment method selection change is a user-initiated change of the effective amount
        // limits (take-offer.md, "Amount limits", user-initiated class). The selection map is
        // mutated on the JavaFX thread outside the use case monitor: a background update can
        // read the rail mid-change and compute once without the method limit, but this handler
        // recomputes on the same JavaFX call stack right after the mutation - no FX event (and
        // so no confirmation) can run in between, and the recomputation republishes with the
        // completed selection.
        paymentMethodService.setTradeAmountConstraintsRecalculationHandler(() -> recalculateAmountConstraints(true, false));
    }

    private PaymentRail getSelectedPaymentRail() {
        return paymentMethodService.getSelectedPaymentRail();
    }


    /* --------------------------------------------------------------------- */
    // Lifecycle
    /* --------------------------------------------------------------------- */

    @Override
    public void initialize() {
        throw new UnsupportedOperationException("Use initialize(MuSigOffer)");
    }

    public synchronized void initialize(MuSigOffer muSigOffer) {
        checkNotNull(muSigOffer, "muSigOffer must not be null");
        try {
            validate(muSigOffer);
            // Single market price lookup: the presence requirement and the quote resolution must
            // not diverge, and no service state is touched before all checks have passed.
            PriceQuote marketPriceQuote = findPositiveMarketPriceQuote(muSigOffer)
                    .orElseThrow(() -> new TakeOfferValidationException(Reason.NO_MARKET_PRICE,
                            "No market price available for market " + muSigOffer.getMarket().getMarketCodes()));
            PriceQuote priceQuote = resolvePriceQuote(muSigOffer, marketPriceQuote);

            this.muSigOffer = muSigOffer;
            marketService.initialize(muSigOffer);
            directionService.initialize(muSigOffer);
            // Dependencies before triggers: UI refreshes are driven by the quote and deviation
            // observables and read the market quote, so it must be published first.
            priceService.setMarketPriceQuote(marketPriceQuote);
            priceService.setPriceDeviation(calculateDeviation(priceQuote, marketPriceQuote));
            priceService.setPriceQuote(priceQuote);
            paymentMethodService.updatePaymentMethods(muSigOffer);
            // After the payment concern so a preselected method's limit participates from the start.
            initializeAmount(muSigOffer, priceQuote);
            if (marketPricePin != null) {
                marketPricePin.unbind();
            }
            // The map observer fires at registration, which reconciles any market price change that
            // happened between the initialization lookup and this registration. Quote, market price
            // and deviation are always set together from one lookup, so every update is internally
            // consistent; at an unchanged resolved quote the recompute keeps the published amount
            // pair and only re-validates.
            marketPricePin = marketPriceService.getMarketPriceByCurrencyMap().addObserver(this::handleMarketPriceUpdate);
            addDisposable(marketPricePin);
        } catch (TakeOfferValidationException e) {
            // A rejected initialization must not leave state behind, also when a previous
            // initialization on this instance had succeeded.
            resetState();
            throw e;
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        // UI callbacks queued before disposal still run afterwards and re-read the price and
        // amount state; they must find it cleared, not the closed session's values.
        resetState();
    }

    // Every read-compute-publish sequence over the price and amount state shares the instance
    // monitor (initialization, the market-price update, the amount mutators, the constraint
    // recomputation, the handoff and this reset): a mutator that read the resolved quote can
    // never publish its result after a concurrent update replaced the price, and an in-flight
    // update completes before the state is cleared here.
    private synchronized void resetState() {
        muSigOffer = null;
        marketService.initialize(null);
        directionService.initialize(null);
        priceService.setPriceQuote(null);
        priceService.setMarketPriceQuote(null);
        priceService.setPriceDeviation(null);
        paymentMethodService.reset();
        amountService.reset();
        amountConstraintsCauseValid = true;
        userAmountInputCauseValid = true;
        rangeCollapsed = false;
        amountConstraintsStale = false;
        if (marketPricePin != null) {
            marketPricePin.unbind();
            marketPricePin = null;
        }
    }

    /**
     * The payment step is skipped when both side specification lists contain exactly one payment
     * method and exactly one eligible account exists for the taker-side method; the method and
     * account are then applied automatically (take-offer.md, "Payment method", step bypass).
     */
    public boolean shouldShowPaymentStep() {
        checkNotNull(muSigOffer, "shouldShowPaymentStep must not be called before initialize");
        boolean isSinglePaymentMethod = muSigOffer.getBaseSidePaymentMethodSpecs().size() == 1
                && muSigOffer.getQuoteSidePaymentMethodSpecs().size() == 1;
        if (!isSinglePaymentMethod) {
            return true;
        }
        PaymentMethod<?> takerSideMethod = paymentMethodService.getTakerSidePaymentMethodSpecs().get(0).getPaymentMethod();
        List<Account<?, ?>> accounts = paymentMethodService.getAccountsByPaymentMethod().get(takerSideMethod);
        return accounts == null || accounts.size() != 1;
    }

    /**
     * The amount step is shown only for range offers whose effective range did not collapse to a
     * single value at initialization (take-offer.md, "Amount", collapse rule).
     */
    public boolean shouldShowAmountStep() {
        checkNotNull(muSigOffer, "shouldShowAmountStep must not be called before initialize");
        return muSigOffer.hasAmountRange() && !rangeCollapsed;
    }

    public Optional<Account<?, ?>> getSelectedAccount() {
        ImmutableMap<PaymentMethod<?>, Account<?, ?>> selected = paymentMethodService.getSelectedAccountByPaymentMethod();
        return selected.size() == 1
                ? Optional.of(selected.values().iterator().next())
                : Optional.empty();
    }

    public Optional<PaymentMethodSpec<?>> getSelectedPaymentMethodSpec() {
        ImmutableMap<PaymentMethod<?>, Account<?, ?>> selected = paymentMethodService.getSelectedAccountByPaymentMethod();
        return selected.size() == 1
                ? paymentMethodService.findTakerSidePaymentMethodSpec(selected.keySet().iterator().next())
                : Optional.empty();
    }

    /**
     * The atomic snapshot handed to the trade protocol: the trade amounts and the market price
     * they were validated against, read together. handleMarketPriceUpdate mutates these on the
     * market-price thread in several steps, so the confirming thread must capture them under the
     * same monitor to avoid a torn amounts-vs-price pair (take-offer.md, "Handoff").
     */
    public record Handoff(Monetary baseSideAmount, Monetary quoteSideAmount, long marketPrice) {
    }

    public synchronized Optional<Handoff> getHandoff() {
        PriceQuote marketPriceQuote = priceService.getMarketPriceQuote();
        TradeAmount fixTradeAmount = amountService.getFixTradeAmount();
        if (marketPriceQuote == null || fixTradeAmount == null || !amountService.isAmountValid()
                || !hasPositiveSides(fixTradeAmount)) {
            return Optional.empty();
        }
        return Optional.of(new Handoff(fixTradeAmount.getBaseSideAmount(),
                fixTradeAmount.getQuoteSideAmount(),
                marketPriceQuote.getValue()));
    }

    private synchronized void handleMarketPriceUpdate() {
        MuSigOffer offer = this.muSigOffer;
        if (offer == null) {
            return;
        }
        findPositiveMarketPriceQuote(offer)
                .ifPresentOrElse(marketPriceQuote -> {
                    PriceQuote priceQuote = resolvePriceQuote(offer, marketPriceQuote);
                    boolean quoteChanged = !priceQuote.equals(priceService.getPriceQuote());
                    // Same publish order as at initialization: dependencies before triggers.
                    priceService.setMarketPriceQuote(marketPriceQuote);
                    priceService.setPriceDeviation(calculateDeviation(priceQuote, marketPriceQuote));
                    priceService.setPriceQuote(priceQuote);
                    // Background class: limits recompute, the selected amount is never clamped
                    // (take-offer.md, "Amount limits", background changes).
                    recalculateAmountConstraints(false, quoteChanged);
                }, () -> {
                    // The market price disappeared (or is non-positive) while the take process is
                    // open. Keep the resolved quote for display but mark market price and deviation
                    // unknown; blocking the confirmation is handled with the review concern.
                    priceService.setMarketPriceQuote(null);
                    priceService.setPriceDeviation(null);
                });
    }

    // A non-positive market price is treated as no price: it cannot drive a deviation
    // (getPercentageToMarketPrice requires a positive value) and must never be resolved into a
    // quote or persisted into a contract.
    private Optional<PriceQuote> findPositiveMarketPriceQuote(MuSigOffer offer) {
        return PriceUtil.findMarketPriceQuote(marketPriceService, offer.getMarket())
                .filter(marketPriceQuote -> marketPriceQuote.getValue() > 0);
    }

    // Both quotes come from the same lookup, so the deviation always matches the resolved quote.
    private static double calculateDeviation(PriceQuote priceQuote, PriceQuote marketPriceQuote) {
        return PriceUtil.getPercentageToMarketPrice(marketPriceQuote, priceQuote);
    }

    private static PriceQuote resolvePriceQuote(MuSigOffer offer, PriceQuote marketPriceQuote) {
        PriceSpec priceSpec = offer.getPriceSpec();
        if (priceSpec instanceof FixPriceSpec fixPriceSpec) {
            return fixPriceSpec.getPriceQuote();
        } else if (priceSpec instanceof FloatPriceSpec floatPriceSpec) {
            return PriceUtil.fromMarketPriceMarkup(marketPriceQuote, floatPriceSpec.getPercentage());
        } else if (priceSpec instanceof MarketPriceSpec) {
            return marketPriceQuote;
        } else {
            throw new IllegalStateException("Not supported priceSpec. priceSpec=" + priceSpec);
        }
    }


    /* --------------------------------------------------------------------- */
    // Trust-boundary validation (take-offer.md, "Offer as root input")
    /* --------------------------------------------------------------------- */

    private void validate(MuSigOffer offer) {
        if (!offer.getProtocolTypes().contains(TradeProtocolType.MU_SIG)) {
            throw new TakeOfferValidationException(Reason.PROTOCOL_TYPE_NOT_SUPPORTED,
                    "Offer " + offer.getId() + " does not support the MuSig trade protocol. protocolTypes="
                            + offer.getProtocolTypes());
        }
        // Any local identity, including retired ones: deleting a user profile retires the
        // identity but its offers survive, and this client still owns the maker key.
        if (identityService.findAnyIdentityByNetworkId(offer.getMakerNetworkId()).isPresent()) {
            throw new TakeOfferValidationException(Reason.OWN_OFFER,
                    "Offer " + offer.getId() + " was created by one of our own identities");
        }
        Market market = offer.getMarket();
        PriceSpec priceSpec = offer.getPriceSpec();
        if (priceSpec instanceof FloatPriceSpec floatPriceSpec) {
            double percentage = floatPriceSpec.getPercentage();
            if (percentage < PriceLimits.MIN_PERCENTAGE_FROM_MARKET_PRICE
                    || percentage > PriceLimits.MAX_PERCENTAGE_FROM_MARKET_PRICE) {
                throw new TakeOfferValidationException(Reason.FLOAT_PRICE_OUT_OF_BOUNDS,
                        "Floating price percentage " + percentage + " of offer " + offer.getId()
                                + " is outside the create-offer bounds; such an offer could not have been created legitimately");
            }
        } else if (priceSpec instanceof FixPriceSpec fixPriceSpec) {
            if (!market.equals(fixPriceSpec.getPriceQuote().getMarket())) {
                throw new TakeOfferValidationException(Reason.FIXED_PRICE_MARKET_MISMATCH,
                        "Fixed price quote market " + fixPriceSpec.getPriceQuote().getMarket().getMarketCodes()
                                + " does not match the offer market " + market.getMarketCodes());
            }
        }
        List<PaymentMethodSpec<?>> takerSideSpecs = getTakerSidePaymentMethodSpecs(offer);
        if (takerSideSpecs.isEmpty() || takerSideSpecs.size() > PaymentMethodSelection.MAX_NUM_PAYMENT_METHODS) {
            throw new TakeOfferValidationException(Reason.INVALID_PAYMENT_METHOD_SPECS,
                    "The taker-selectable side of offer " + offer.getId() + " must contain between 1 and "
                            + PaymentMethodSelection.MAX_NUM_PAYMENT_METHODS + " payment method specifications but contains "
                            + takerSideSpecs.size());
        }
        long distinctTakerSideMethods = takerSideSpecs.stream()
                .map(PaymentMethodSpec::getPaymentMethod)
                .distinct()
                .count();
        if (distinctTakerSideMethods != takerSideSpecs.size()) {
            throw new TakeOfferValidationException(Reason.INVALID_PAYMENT_METHOD_SPECS,
                    "The taker-selectable side of offer " + offer.getId()
                            + " contains duplicate payment methods");
        }
        List<PaymentMethodSpec<?>> bitcoinSideSpecs = getBitcoinSidePaymentMethodSpecs(offer);
        if (bitcoinSideSpecs.size() != 1
                || bitcoinSideSpecs.get(0).getPaymentMethod().getPaymentRail() != BitcoinPaymentRail.MAIN_CHAIN) {
            throw new TakeOfferValidationException(Reason.INVALID_PAYMENT_METHOD_SPECS,
                    "The Bitcoin side of offer " + offer.getId()
                            + " must contain exactly one Bitcoin main-chain payment method specification");
        }
        validateOfferOptions(offer, takerSideSpecs);
    }

    private static List<PaymentMethodSpec<?>> getTakerSidePaymentMethodSpecs(MuSigOffer offer) {
        return offer.getMarket().isBaseCurrencyBitcoin()
                ? offer.getQuoteSidePaymentMethodSpecs()
                : offer.getBaseSidePaymentMethodSpecs();
    }

    private static List<PaymentMethodSpec<?>> getBitcoinSidePaymentMethodSpecs(MuSigOffer offer) {
        return offer.getMarket().isBaseCurrencyBitcoin()
                ? offer.getBaseSidePaymentMethodSpecs()
                : offer.getQuoteSidePaymentMethodSpecs();
    }

    private static void validateOfferOptions(MuSigOffer offer, List<PaymentMethodSpec<?>> takerSideSpecs) {
        List<OfferOption> offerOptions = offer.getOfferOptions();
        List<CollateralOption> collateralOptions = offerOptions.stream()
                .filter(CollateralOption.class::isInstance)
                .map(CollateralOption.class::cast)
                .collect(Collectors.toList());
        if (collateralOptions.size() != 1) {
            throw new TakeOfferValidationException(Reason.INVALID_OFFER_OPTIONS,
                    "Offer " + offer.getId() + " must contain exactly one CollateralOption but contains "
                            + collateralOptions.size());
        }
        CollateralOption collateralOption = collateralOptions.get(0);
        double buyerSecurityDeposit = collateralOption.getBuyerSecurityDeposit();
        double sellerSecurityDeposit = collateralOption.getSellerSecurityDeposit();
        // Finiteness, the 0-1 range and canonical zero are intrinsic invariants enforced by
        // CollateralOption.verify() at construction and deserialization; only the take-specific
        // requirements are checked here.
        if (Double.compare(buyerSecurityDeposit, sellerSecurityDeposit) != 0) {
            // Asymmetric deposits are not supported by the current protocol
            // (OfferOptionUtil.findSymmetricSecurityDepositPercent throws downstream); the
            // comparison matches that helper's Double.compare semantics.
            throw new TakeOfferValidationException(Reason.INVALID_OFFER_OPTIONS,
                    "Offer " + offer.getId() + " has asymmetric security deposit percentages: buyer="
                            + buyerSecurityDeposit + ", seller=" + sellerSecurityDeposit);
        }
        // Count on the raw list: OfferOptionUtil.findAccountOptions returns a Set, which would
        // collapse exactly equal duplicates before they can be rejected.
        Map<PaymentMethod<?>, Long> accountOptionCountByPaymentMethod = offerOptions.stream()
                .filter(AccountOption.class::isInstance)
                .map(AccountOption.class::cast)
                .collect(Collectors.groupingBy(AccountOption::getPaymentMethod, Collectors.counting()));
        for (PaymentMethodSpec<?> paymentMethodSpec : takerSideSpecs) {
            long count = accountOptionCountByPaymentMethod.getOrDefault(paymentMethodSpec.getPaymentMethod(), 0L);
            if (count != 1) {
                throw new TakeOfferValidationException(Reason.INVALID_OFFER_OPTIONS,
                        "Offer " + offer.getId() + " must contain exactly one AccountOption for payment method "
                                + paymentMethodSpec.getPaymentMethod().getPaymentRailName() + " but contains " + count);
            }
        }
        Set<PaymentMethod<?>> takerSideMethods = takerSideSpecs.stream()
                .map(spec -> (PaymentMethod<?>) spec.getPaymentMethod())
                .collect(Collectors.toSet());
        if (!takerSideMethods.containsAll(accountOptionCountByPaymentMethod.keySet())) {
            throw new TakeOfferValidationException(Reason.INVALID_OFFER_OPTIONS,
                    "Offer " + offer.getId() + " contains AccountOptions for payment methods it does not offer");
        }
    }

    /* --------------------------------------------------------------------- */
    // Amount concern (take-offer.md, "Amount" and "Amount limits")
    /* --------------------------------------------------------------------- */

    // The effective range intersects offer, absolute, payment-method and user-specific limits;
    // the pre-user range omits the user cap and is what the slider spans. A null effectiveRange
    // means the intersection is empty.
    private record AmountConstraints(@Nullable TradeAmountRange preUserRange,
                                     @Nullable TradeAmountRange effectiveRange,
                                     Optional<TradeAmount> userSpecificLimit) {
    }

    private void initializeAmount(MuSigOffer offer, PriceQuote resolvedQuote) {
        rangeCollapsed = false;
        amountConstraintsStale = false;
        // A fresh initialization publishes a fresh selection; a cleared-input cause from a
        // previously initialized offer must not survive it - a fixed offer skips the amount
        // step and would stay blocked with no field to recover from.
        userAmountInputCauseValid = true;
        amountConstraintsCauseValid = true;
        // The persisted input-side preference (shared with the create-offer flow) must be
        // restored before any input-side range or slider value is published, as those are
        // denominated in the input side.
        amountService.setUseBaseCurrencyForAmountInput(cookieStore.getUseBaseCurrencyForAmountInput(offer.getMarket()));
        try {
            initializeAmountValidated(offer, resolvedQuote);
        } catch (TakeOfferValidationException e) {
            throw e;
        } catch (ArithmeticException e) {
            // Exact conversions fail instead of wrapping when an offer amount or a limit
            // overflows a long at the resolved price; nothing derived from a wrapped value may
            // be compared or published, so the take fails closed.
            throw new TakeOfferValidationException(Reason.INVALID_OFFER,
                    "The amounts or limits of offer " + offer.getId() + " overflow when converted at the resolved price");
        } catch (RuntimeException e) {
            // The USD-defined limits need the BTC/USD market price, which can be missing while
            // the offer market's price is present; surfaced like any other missing price.
            throw new TakeOfferValidationException(Reason.NO_MARKET_PRICE,
                    "The amount limits for offer " + offer.getId() + " cannot be computed: " + e.getMessage());
        }
    }

    private void initializeAmountValidated(MuSigOffer offer, PriceQuote resolvedQuote) {
        // The offer is untakeable only if NO offered method can cover its amounts; a single
        // inadmissible method (including a preselected one) leaves the others selectable.
        boolean anyMethodAdmissible = getTakerSidePaymentMethodSpecs(offer).stream()
                .map(spec -> (PaymentMethod<?>) spec.getPaymentMethod())
                .anyMatch(this::isPaymentMethodAdmissible);
        if (!anyMethodAdmissible) {
            throw new TakeOfferValidationException(Reason.AMOUNT_OUTSIDE_LIMITS,
                    "No payment method offered by " + offer.getId() + " can cover the offer amounts within the taker's limits");
        }
        ImmutableMap<PaymentMethod<?>, Account<?, ?>> selected = paymentMethodService.getSelectedAccountByPaymentMethod();
        if (selected.size() == 1 && !isPaymentMethodAdmissible(selected.keySet().iterator().next())) {
            log.info("Dropping the preselected payment method of offer {}: its rail limit cannot cover the offer amounts",
                    offer.getId());
            paymentMethodService.clearSelectedAccountByPaymentMethod();
        }
        AmountConstraints constraints = computeAmountConstraints(offer, resolvedQuote);
        TradeAmountRange effectiveRange = constraints.effectiveRange();
        if (effectiveRange == null) {
            throw new TakeOfferValidationException(Reason.AMOUNT_OUTSIDE_LIMITS,
                    "The amounts of offer " + offer.getId() + " do not intersect the taker's amount limits");
        }
        // A valid price can still be absurd enough to round an in-range quote amount to
        // 0 sats on the base side; nothing derived from such a range is takeable.
        if (!hasPositiveSides(effectiveRange.getMin()) || !hasPositiveSides(effectiveRange.getMax())) {
            throw new TakeOfferValidationException(Reason.AMOUNT_OUTSIDE_LIMITS,
                    "The amounts of offer " + offer.getId() + " convert to a non-positive amount at the resolved price");
        }
        amountService.setAmountSpec(offer.getAmountSpec());
        if (!offer.hasAmountRange()) {
            TradeAmount fixTradeAmount = resolveFixedTradeAmount(offer, resolvedQuote);
            // A fixed offer amount is never clamped, as that would change the maker's offer.
            // Membership is judged on the stored side: the offer-bound endpoints carry the
            // stored amount verbatim, so an offer inside its own bounds matches exactly.
            if (!isWithinRangeOnStoredSide(offer, fixTradeAmount, effectiveRange) || !hasPositiveSides(fixTradeAmount)) {
                throw new TakeOfferValidationException(Reason.AMOUNT_OUTSIDE_LIMITS,
                        "The fixed amount of offer " + offer.getId() + " lies outside the taker's amount limits");
            }
            if (!publishAmountConstraints(constraints)) {
                throw new TakeOfferValidationException(Reason.AMOUNT_OUTSIDE_LIMITS,
                        "The amounts of offer " + offer.getId() + " convert to a non-positive amount at the resolved price");
            }
            boolean published = publishFixTradeAmount(fixTradeAmount);
            setConstraintsCauseValid(published);
            return;
        }
        // Collapse rule: a point intersection, or bounds indistinguishable at the display
        // precision of the non-Bitcoin side in Bitcoin-Fiat markets, leaves nothing to select;
        // the amount is treated as fixed and the amount step is skipped.
        rangeCollapsed = isCollapsed(effectiveRange, offer.getMarket(), isQuoteSideStored(offer));
        if (!publishAmountConstraints(constraints)) {
            throw new TakeOfferValidationException(Reason.AMOUNT_OUTSIDE_LIMITS,
                    "The amounts of offer " + offer.getId() + " convert to a non-positive amount at the resolved price");
        }
        boolean published = rangeCollapsed
                ? publishFixTradeAmount(effectiveRange.getMax())
                : publishFixTradeAmount(midpointOf(offer, effectiveRange, resolvedQuote));
        setConstraintsCauseValid(published);
    }

    private void setConstraintsCauseValid(boolean value) {
        amountConstraintsCauseValid = value;
        publishAmountValid();
    }

    private void setUserAmountInputCauseValid(boolean value) {
        userAmountInputCauseValid = value;
        publishAmountValid();
    }

    private void publishAmountValid() {
        amountService.setAmountValid(amountConstraintsCauseValid && userAmountInputCauseValid);
    }

    private synchronized void recalculateAmountConstraints(boolean userInitiated, boolean quoteChanged) {
        MuSigOffer offer = this.muSigOffer;
        if (offer == null || amountService.getAmountSpec() == null) {
            // The amount concern initializes after the payment concern; selection changes made
            // during initialization are covered by the initial computation.
            return;
        }
        PriceQuote resolvedQuote = priceService.getPriceQuote();
        if (resolvedQuote == null || priceService.getMarketPriceQuote() == null) {
            // Without a market price the USD-defined limits cannot be converted; confirmation is
            // already blocked by the missing-market-price gate, the amount state stays untouched.
            return;
        }
        try {
            recalculateAmountConstraintsValidated(offer, resolvedQuote, userInitiated, quoteChanged);
        } catch (RuntimeException e) {
            // Confirmation is only allowed while the amounts are computable against current
            // prices (e.g. the BTC/USD price needed for the USD-defined limits can vanish while
            // the offer market's price is still present, or an exact conversion can overflow).
            // The block lifts with the next successful recomputation.
            log.warn("Amount recomputation failed, blocking confirmation: {}", e.getMessage());
            amountConstraintsStale = true;
            setConstraintsCauseValid(false);
        } finally {
            // Published projections stay equal on many recomputations (equal values are
            // suppressed, an empty intersection publishes nothing) while derived state such as
            // per-method admissibility may still have changed - it depends on the BTC/USD leg
            // of the limit conversions, which moves independently of the offer market's quote.
            // The revision is the completion signal admissibility consumers observe.
            amountService.markConstraintsRecomputed();
        }
    }

    private void recalculateAmountConstraintsValidated(MuSigOffer offer,
                                                       PriceQuote resolvedQuote,
                                                       boolean userInitiated,
                                                       boolean quoteChanged) {
        AmountConstraints constraints = computeAmountConstraints(offer, resolvedQuote);
        TradeAmountRange effectiveRange = constraints.effectiveRange();
        if (effectiveRange == null) {
            // The flow is never closed and nothing is clamped: the previously published ranges
            // stay visible and confirmation is blocked until a later update restores validity.
            amountConstraintsStale = true;
            setConstraintsCauseValid(false);
            return;
        }
        // The collapse state must be current before the ranges are published: observers of the
        // limits read shouldShowAmountStep (dependencies before triggers). Background updates
        // deliberately never change the step structure of an open flow.
        if (offer.hasAmountRange() && userInitiated) {
            rangeCollapsed = isCollapsed(effectiveRange, offer.getMarket(), isQuoteSideStored(offer));
        }
        if (!publishAmountConstraints(constraints)) {
            return;
        }
        amountConstraintsStale = false;
        if (!offer.hasAmountRange()) {
            // A fixed offer amount is never clamped; it becomes invalid instead - both for
            // background updates and for a later payment method selection.
            TradeAmount fixTradeAmount = resolveFixedTradeAmount(offer, resolvedQuote);
            boolean published = publishFixTradeAmount(fixTradeAmount);
            setConstraintsCauseValid(published && isWithinRangeOnStoredSide(offer, fixTradeAmount, effectiveRange));
            return;
        }
        TradeAmount current = amountService.getFixTradeAmount();
        if (current == null) {
            setConstraintsCauseValid(
                    publishFixTradeAmount(midpointOf(offer, effectiveRange, resolvedQuote)));
            return;
        }
        if (userInitiated) {
            // User-initiated changes clamp the selection visibly into the new effective range;
            // a collapsed range is treated as fixed at its single value. The published value is
            // a visible fresh selection, so it supersedes a previously cleared input field -
            // which may no longer exist when the collapse removed the amount step.
            boolean publishedSelection = publishFixTradeAmount(rangeCollapsed
                    ? effectiveRange.getMax()
                    : clampToRangeOnStoredSide(isQuoteSideStored(offer), current, effectiveRange));
            setConstraintsCauseValid(publishedSelection);
            if (publishedSelection) {
                setUserAmountInputCauseValid(true);
            }
        } else {
            // Background changes keep the input side stable, recompute the passive side from the
            // resolved quote and re-validate without clamping.
            // The passive side is re-derived only when the resolved quote actually changed: a
            // recompute triggered by anything else (a limits change, the at-registration
            // reconciliation) must keep the published pair, as re-deriving the stored side of a
            // base-stored offer from the quote side is lossy at sub-unit prices.
            // An amount sitting on an endpoint is restored to the whole endpoint pair, else the
            // re-derivation (a different conversion path than a limit endpoint's own) could flip
            // a boundary amount invalid without any change.
            TradeAmount basis = quoteChanged
                    ? refreshPassiveSideExact(current, resolvedQuote, amountService.getUseBaseCurrencyForAmountInput())
                    : current;
            TradeAmount refreshed = alignToRangeEndpoints(basis, effectiveRange, isQuoteSideStored(offer));
            boolean published = publishFixTradeAmount(refreshed);
            setConstraintsCauseValid(published && isWithinRangeOnStoredSide(offer, refreshed, effectiveRange));
        }
    }

    private static TradeAmount refreshPassiveSideExact(TradeAmount current,
                                                       PriceQuote resolvedQuote,
                                                       boolean useBaseCurrencyForAmountInput) {
        if (useBaseCurrencyForAmountInput) {
            Monetary baseSideAmount = current.getBaseSideAmount();
            return new TradeAmount(baseSideAmount, resolvedQuote.toQuoteSideMonetaryExact(baseSideAmount));
        }
        Monetary quoteSideAmount = current.getQuoteSideAmount();
        return new TradeAmount(resolvedQuote.toBaseSideMonetaryExact(quoteSideAmount), quoteSideAmount);
    }

    private AmountConstraints computeAmountConstraints(MuSigOffer offer, PriceQuote resolvedQuote) {
        return computeAmountConstraints(offer, resolvedQuote, getSelectedPaymentRail());
    }

    private AmountConstraints computeAmountConstraints(MuSigOffer offer,
                                                       PriceQuote resolvedQuote,
                                                       @Nullable PaymentRail selectedRail) {
        Market market = offer.getMarket();
        // The intersection runs on the side the offer's amounts are stored in, and endpoints
        // are selected as whole TradeAmount pairs: the stored side is never reconstructed from
        // the derived side (the round trip is lossy at sub-unit prices), and a selected limit
        // endpoint keeps the pair its own conversion chain produced. The published limit pair
        // IS the enforced cap; it can deviate from the USD-defined ideal by one rounding unit
        // of the derived side, matching what is displayed.
        boolean quoteSideStored = isQuoteSideStored(offer);
        TradeAmountRange offerRange = resolveOfferRange(offer, resolvedQuote, quoteSideStored);
        // The USD-defined limits convert to the market's stable side via the market price; the
        // Bitcoin side follows the resolved quote (same split as the create-offer limits). One
        // captured rate snapshot serves every limit of this computation, so no limit mixes two
        // reads of a map the poller writes concurrently.
        TradeAmountLimitUtils.Rates rates = TradeAmountLimitUtils.findRates(marketPriceService, market)
                .orElseThrow(() -> new IllegalStateException("The market prices needed for the amount limits of "
                        + market.getMarketCodes() + " are missing"));
        TradeAmount absoluteMin = TradeAmountLimitUtils.toTradeAmountLimitExact(rates, market, resolvedQuote,
                AbsoluteAmountLimitsProvider.MIN_TRADE_AMOUNT_IN_USD);
        TradeAmount absoluteMax = TradeAmountLimitUtils.toTradeAmountLimitExact(rates, market, resolvedQuote,
                AbsoluteAmountLimitsProvider.MAX_TRADE_AMOUNT_IN_USD);
        TradeAmount minEndpoint = maxOnStoredSide(quoteSideStored, offerRange.getMin(), absoluteMin);
        TradeAmount maxEndpoint = minOnStoredSide(quoteSideStored, offerRange.getMax(), absoluteMax);
        if (selectedRail != null) {
            TradeAmount methodLimit = TradeAmountLimitUtils.toTradeAmountLimitExact(rates, market, resolvedQuote,
                    PaymentMethodBasedAmountLimitsProvider.evaluateLimitInUsd(selectedRail));
            maxEndpoint = minOnStoredSide(quoteSideStored, maxEndpoint, methodLimit);
        }
        // The user-specific cap applies when the taker is the Bitcoin buyer in a Bitcoin-Fiat
        // market (taken offer direction SELL). A seller-side cap is deliberately absent.
        Optional<TradeAmount> userSpecificLimit = market.isBtcFiatMarket() && offer.getDirection().isSell()
                ? Optional.of(TradeAmountLimitUtils.toTradeAmountLimitExact(rates, market, resolvedQuote,
                UserSpecificAmountLimitsProvider.getUserSpecificLimitInUsd()))
                : Optional.empty();
        if (storedSideValue(quoteSideStored, minEndpoint) > storedSideValue(quoteSideStored, maxEndpoint)) {
            return new AmountConstraints(null, null, userSpecificLimit);
        }
        // Mixed-provenance endpoints can invert on the derived side by one rounding unit when
        // the range is only rounding-distance wide; such a range is empty rather than
        // published (a collapse could publish a pair below a hard limit).
        TradeAmountRange preUserRange = toOrderedRange(minEndpoint, maxEndpoint);
        if (preUserRange == null) {
            return new AmountConstraints(null, null, userSpecificLimit);
        }
        TradeAmount effectiveMaxEndpoint = maxEndpoint;
        if (userSpecificLimit.isPresent()) {
            TradeAmount cap = userSpecificLimit.get();
            if (storedSideValue(quoteSideStored, cap) < storedSideValue(quoteSideStored, minEndpoint)) {
                // The limit is never relaxed to meet the minimum; the intersection is empty.
                return new AmountConstraints(preUserRange, null, userSpecificLimit);
            }
            effectiveMaxEndpoint = minOnStoredSide(quoteSideStored, effectiveMaxEndpoint, cap);
        }
        TradeAmountRange effectiveRange = toOrderedRange(minEndpoint, effectiveMaxEndpoint);
        if (effectiveRange == null) {
            return new AmountConstraints(preUserRange, null, userSpecificLimit);
        }
        return new AmountConstraints(preUserRange, effectiveRange, userSpecificLimit);
    }

    // Collapse test: a point intersection, or bounds indistinguishable at the display precision
    // of the non-Bitcoin side in Bitcoin-Fiat markets.
    private static boolean isCollapsed(TradeAmountRange effectiveRange, Market market, boolean quoteSideStored) {
        // A point intersection is judged on the offer's stored side: the derived side is lossy,
        // so two distinct stored amounts can share a derived value (e.g. 1.100000 and 1.100001
        // XMR both convert to 276_357 sats), which must not be treated as collapsed.
        Monetary minStored = quoteSideStored ? effectiveRange.getMin().getQuoteSideAmount() : effectiveRange.getMin().getBaseSideAmount();
        Monetary maxStored = quoteSideStored ? effectiveRange.getMax().getQuoteSideAmount() : effectiveRange.getMax().getBaseSideAmount();
        if (minStored.getValue() == maxStored.getValue()) {
            return true;
        }
        // Bitcoin-Fiat markets additionally collapse when the fiat (non-Bitcoin, quote) bounds
        // are indistinguishable at the fiat display precision.
        Monetary minQuote = effectiveRange.getMin().getQuoteSideAmount();
        Monetary maxQuote = effectiveRange.getMax().getQuoteSideAmount();
        return market.isBtcFiatMarket() && minQuote.isEqual(maxQuote, minQuote.getLowPrecision());
    }

    /**
     * A payment method is selectable only when its rail limit leaves a non-empty effective range
     * that admits the offer's fixed amount (take-offer.md, "Amount limits", method
     * selectability). Methods that cannot be evaluated count as admissible - the confirmation
     * gate covers them.
     */
    public synchronized boolean isPaymentMethodAdmissible(PaymentMethod<?> paymentMethod) {
        MuSigOffer offer = this.muSigOffer;
        PriceQuote resolvedQuote = priceService.getPriceQuote();
        if (offer == null || resolvedQuote == null) {
            return true;
        }
        AmountConstraints constraints;
        try {
            constraints = computeAmountConstraints(offer, resolvedQuote, paymentMethod.getPaymentRail());
        } catch (RuntimeException e) {
            return true;
        }
        TradeAmountRange effectiveRange = constraints.effectiveRange();
        if (effectiveRange == null) {
            return false;
        }
        if (!offer.hasAmountRange()) {
            return isWithinRangeOnStoredSide(offer, resolveFixedTradeAmount(offer, resolvedQuote), effectiveRange);
        }
        return true;
    }

    // The side the maker's amounts are stored in; the other side is derived and lossy.
    private static boolean isQuoteSideStored(MuSigOffer offer) {
        return AmountSpecUtil.findQuoteSideMinOrFixedAmountFromSpec(offer.getAmountSpec(),
                offer.getMarket().getQuoteCurrencyCode()).isPresent();
    }

    // The offer's endpoints keep the stored Monetary verbatim; the other side is derived
    // exactly once. The derivation direction matches resolveFixedTradeAmount, so an offer
    // bound and the offer's fixed amount produce identical pairs.
    private static TradeAmountRange resolveOfferRange(MuSigOffer offer,
                                                      PriceQuote resolvedQuote,
                                                      boolean quoteSideStored) {
        Market market = offer.getMarket();
        AmountSpec amountSpec = offer.getAmountSpec();
        if (quoteSideStored) {
            Monetary minQuote = AmountSpecUtil.findQuoteSideMinOrFixedAmountFromSpec(amountSpec, market.getQuoteCurrencyCode())
                    .orElseThrow(() -> new IllegalStateException("Unsupported amount spec: " + amountSpec));
            Monetary maxQuote = AmountSpecUtil.findQuoteSideMaxOrFixedAmountFromSpec(amountSpec, market.getQuoteCurrencyCode())
                    .orElseThrow(() -> new IllegalStateException("Unsupported amount spec: " + amountSpec));
            return new TradeAmountRange(
                    new TradeAmount(resolvedQuote.toBaseSideMonetaryExact(minQuote), minQuote),
                    new TradeAmount(resolvedQuote.toBaseSideMonetaryExact(maxQuote), maxQuote));
        }
        Monetary minBase = AmountSpecUtil.findBaseSideMinOrFixedAmountFromSpec(amountSpec, market.getBaseCurrencyCode())
                .orElseThrow(() -> new IllegalStateException("Unsupported amount spec: " + amountSpec));
        Monetary maxBase = AmountSpecUtil.findBaseSideMaxOrFixedAmountFromSpec(amountSpec, market.getBaseCurrencyCode())
                .orElseThrow(() -> new IllegalStateException("Unsupported amount spec: " + amountSpec));
        return new TradeAmountRange(
                new TradeAmount(minBase, resolvedQuote.toQuoteSideMonetaryExact(minBase)),
                new TradeAmount(maxBase, resolvedQuote.toQuoteSideMonetaryExact(maxBase)));
    }

    private static long storedSideValue(boolean quoteSideStored, TradeAmount amount) {
        return storedSideAmount(quoteSideStored, amount).getValue();
    }

    private static Monetary storedSideAmount(boolean quoteSideStored, TradeAmount amount) {
        return quoteSideStored ? amount.getQuoteSideAmount() : amount.getBaseSideAmount();
    }

    // Stored-side ties select the LIMIT endpoint (the second argument at every call site): a
    // limit pair satisfies its constraint on the constraint's own basis, while an offer pair
    // with the same stored value can sit one derived rounding unit outside it - selecting the
    // offer pair on a tie would publish, e.g., a quote side one unit below the absolute
    // minimum. The resulting pair inversion then makes the intersection empty instead.
    private static TradeAmount minOnStoredSide(boolean quoteSideStored, TradeAmount candidate, TradeAmount limit) {
        return storedSideValue(quoteSideStored, candidate) < storedSideValue(quoteSideStored, limit) ? candidate : limit;
    }

    private static TradeAmount maxOnStoredSide(boolean quoteSideStored, TradeAmount candidate, TradeAmount limit) {
        return storedSideValue(quoteSideStored, candidate) > storedSideValue(quoteSideStored, limit) ? candidate : limit;
    }

    @Nullable
    private static TradeAmountRange toOrderedRange(TradeAmount min, TradeAmount max) {
        if (min.getBaseSideAmount().getValue() > max.getBaseSideAmount().getValue()
                || min.getQuoteSideAmount().getValue() > max.getQuoteSideAmount().getValue()) {
            return null;
        }
        return new TradeAmountRange(min, max);
    }

    private static boolean isWithinRangeOnStoredSide(MuSigOffer offer, TradeAmount amount, TradeAmountRange range) {
        boolean quoteSideStored = isQuoteSideStored(offer);
        long value = storedSideValue(quoteSideStored, amount);
        return value >= storedSideValue(quoteSideStored, range.getMin())
                && value <= storedSideValue(quoteSideStored, range.getMax());
    }

    // Out-of-range amounts snap to the WHOLE endpoint pair: clamping the sides independently
    // could recombine values from different conversion chains into a pair matching no price.
    // Interior amounts keep the pair they were authored with.
    private static TradeAmount clampToRangeOnStoredSide(boolean quoteSideStored,
                                                        TradeAmount amount,
                                                        TradeAmountRange range) {
        TradeAmount aligned = alignToRangeEndpoints(amount, range, quoteSideStored);
        if (aligned != amount) {
            return aligned;
        }
        long value = storedSideValue(quoteSideStored, amount);
        if (value < storedSideValue(quoteSideStored, range.getMin())) {
            return range.getMin();
        }
        if (value > storedSideValue(quoteSideStored, range.getMax())) {
            return range.getMax();
        }
        return amount;
    }

    // An amount matching an endpoint on either side is that endpoint. The match window is one
    // rounding unit of the other side, economically negligible on both regimes.
    private static TradeAmount alignToRangeEndpoints(TradeAmount amount, TradeAmountRange range, boolean quoteSideStored) {
        // Identify an endpoint by its stored side only: the derived side is lossy and can
        // coincide between distinct endpoints (two XMR amounts sharing a sat value), which would
        // otherwise snap a selected maximum onto the minimum.
        if (matchesOnStoredSide(amount, range.getMin(), quoteSideStored)) {
            return range.getMin();
        }
        if (matchesOnStoredSide(amount, range.getMax(), quoteSideStored)) {
            return range.getMax();
        }
        return amount;
    }

    private static boolean matchesOnStoredSide(TradeAmount amount, TradeAmount endpoint, boolean quoteSideStored) {
        return storedSideValue(quoteSideStored, amount) == storedSideValue(quoteSideStored, endpoint);
    }

    private static TradeAmount resolveFixedTradeAmount(MuSigOffer offer, PriceQuote resolvedQuote) {
        Market market = offer.getMarket();
        Monetary fixedAmount = AmountSpecUtil.findQuoteSideFixedAmountFromSpec(offer.getAmountSpec(), market.getQuoteCurrencyCode())
                .or(() -> AmountSpecUtil.findBaseSideFixedAmountFromSpec(offer.getAmountSpec(), market.getBaseCurrencyCode()))
                .orElseThrow(() -> new IllegalStateException("Fixed amount spec expected but was " + offer.getAmountSpec()));
        return TradeAmountConversion.toTradeAmountExact(market, resolvedQuote, fixedAmount);
    }

    private static TradeAmount midpointOf(MuSigOffer offer, TradeAmountRange effectiveRange, PriceQuote resolvedQuote) {
        Market market = offer.getMarket();
        boolean quoteSideStored = isQuoteSideStored(offer);
        // Bitcoin-Fiat markets default to a whole fiat amount, whichever side the offer stores:
        // the midpoint is taken on the fiat (quote) side and rounded to whole units - at fiat
        // scale the conversion round trip is absorbed by that rounding. Everywhere else the
        // midpoint is computed on the stored side: on the derived side both endpoints can round
        // to nearby (or equal) values, so a midpoint taken there converts back to a default off
        // the middle of the stored range, or below its minimum.
        Monetary midpoint;
        if (market.isBtcFiatMarket()) {
            Monetary min = effectiveRange.getMin().getQuoteSideAmount();
            Monetary max = effectiveRange.getMax().getQuoteSideAmount();
            midpoint = Monetary.from(min, min.getValue() + (max.getValue() - min.getValue()) / 2).round(0);
        } else {
            Monetary min = storedSideAmount(quoteSideStored, effectiveRange.getMin());
            Monetary max = storedSideAmount(quoteSideStored, effectiveRange.getMax());
            midpoint = Monetary.from(min, min.getValue() + (max.getValue() - min.getValue()) / 2);
        }
        return clampToRangeOnStoredSide(quoteSideStored,
                TradeAmountConversion.toTradeAmountExact(market, resolvedQuote, midpoint), effectiveRange);
    }

    // Publish order: ranges, then the user marker, then amount and slider value
    // (dependencies before triggers - the desktop slider controllers clamp incoming values
    // against the last-emitted marker and feed the result back into the domain).
    private boolean publishAmountConstraints(AmountConstraints constraints) {
        TradeAmountRange effectiveRange = checkNotNull(constraints.effectiveRange());
        TradeAmountRange preUserRange = checkNotNull(constraints.preUserRange());
        // Invariant: no range endpoint with a non-positive side reaches the amount concern.
        // Currently unreachable (an absurd price also collapses the absolute limits into an
        // empty intersection), but the limit providers must not be able to change that.
        if (!hasPositiveSides(effectiveRange.getMin()) || !hasPositiveSides(effectiveRange.getMax())
                || !hasPositiveSides(preUserRange.getMin()) || !hasPositiveSides(preUserRange.getMax())) {
            log.warn("Refusing to publish amount limits with a non-positive side, blocking confirmation");
            amountConstraintsStale = true;
            setConstraintsCauseValid(false);
            return false;
        }
        amountService.setTradeAmountLimits(effectiveRange);
        MonetaryRange inputAmountLimits = toInputSideRange(preUserRange);
        amountService.setInputAmountLimits(inputAmountLimits);
        amountService.setUserSpecificTradeAmountLimit(constraints.userSpecificLimit());
        amountService.setUserSpecificTradeAmountLimitAsSliderValue(constraints.userSpecificLimit()
                .map(limit -> AmountSelection.toSliderValueFromAmount(toInputAmount(limit), inputAmountLimits)));
        return true;
    }

    private boolean publishFixTradeAmount(TradeAmount tradeAmount) {
        // Invariant: a trade amount with a non-positive side never reaches the amount concern,
        // whatever path produced it; publishing is refused and confirmation stays blocked.
        if (!hasPositiveSides(tradeAmount)) {
            log.warn("Refusing to publish a non-positive trade amount, blocking confirmation: {}", tradeAmount);
            amountConstraintsStale = true;
            setConstraintsCauseValid(false);
            return false;
        }
        amountService.setFixTradeAmount(tradeAmount);
        MonetaryRange inputAmountLimits = amountService.getInputAmountLimits();
        if (inputAmountLimits != null) {
            amountService.setFixAmountSliderValue(
                    AmountSelection.toSliderValueFromAmount(toInputAmount(tradeAmount), inputAmountLimits));
        }
        return true;
    }

    private static boolean hasPositiveSides(TradeAmount tradeAmount) {
        return tradeAmount.getBaseSideAmount().getValue() > 0 && tradeAmount.getQuoteSideAmount().getValue() > 0;
    }

    private MonetaryRange toInputSideRange(TradeAmountRange range) {
        return new MonetaryRange(toInputAmount(range.getMin()), toInputAmount(range.getMax()));
    }


    /* --------------------------------------------------------------------- */
    // Amount input entry points
    /* --------------------------------------------------------------------- */

    public synchronized void setFixTradeAmountFromInputAmount(@Nullable Monetary amount) {
        // Every edit re-derives the user-input cause: it turns valid only when the input could
        // actually be applied. Empty input (null), an overflowing conversion and missing
        // limits or price all leave it false with the previous pair retained, so a cleared or
        // unapplicable field can never hand off the invisible previous amount - and a later
        // background recomputation only refreshes the constraints cause, never this one.
        setUserAmountInputCauseValid(false);
        if (amount == null) {
            return;
        }
        TradeAmountRange effectiveRange = amountService.getTradeAmountLimits();
        PriceQuote resolvedQuote = priceService.getPriceQuote();
        if (effectiveRange == null || resolvedQuote == null) {
            return;
        }
        TradeAmount tradeAmount;
        try {
            tradeAmount = TradeAmountConversion.toTradeAmountExact(getMarket(), resolvedQuote, amount);
        } catch (ArithmeticException e) {
            // A wrapped conversion followed by the clamp would publish a base and quote side
            // that no longer belong to the same price.
            log.warn("Blocking an amount input whose conversion overflows: {}", amount);
            return;
        }
        if (!publishFixTradeAmount(clampToRangeOnStoredSide(isQuoteSideStored(muSigOffer), tradeAmount, effectiveRange))) {
            return;
        }
        // A clamp against stale published limits proves nothing; the block stays until the
        // limits could be recomputed.
        setConstraintsCauseValid(!amountConstraintsStale);
        setUserAmountInputCauseValid(true);
    }

    public synchronized void setFixTradeAmountFromSliderValue(double sliderValue) {
        checkArgument(sliderValue >= 0 && sliderValue <= 1,
                "sliderValue must be within [0, 1] but was %s", sliderValue);
        setUserAmountInputCauseValid(false);
        MonetaryRange inputAmountLimits = amountService.getInputAmountLimits();
        TradeAmountRange effectiveRange = amountService.getTradeAmountLimits();
        PriceQuote resolvedQuote = priceService.getPriceQuote();
        if (inputAmountLimits == null || effectiveRange == null || resolvedQuote == null) {
            return;
        }
        long amountValue = AmountSelection.getAmountValueFromSliderValue(inputAmountLimits, sliderValue);
        Monetary inputAmount = Monetary.from(inputAmountLimits.getMin(), amountValue);
        TradeAmount tradeAmount;
        try {
            tradeAmount = TradeAmountConversion.toTradeAmountExact(getMarket(), resolvedQuote, inputAmount);
        } catch (ArithmeticException e) {
            // The slider spans the published input limits, but the resolved quote may have moved
            // since they were published; a wrapping conversion must not reach the clamp.
            log.warn("Ignoring a slider amount whose conversion overflows: {}", inputAmount);
            return;
        }
        // The slider spans the pre-user range; the clamp caps the value at the user-specific
        // limit (snapping to the endpoint pair) and the corrected slider value is re-emitted.
        if (!publishFixTradeAmount(clampToRangeOnStoredSide(isQuoteSideStored(muSigOffer), tradeAmount, effectiveRange))) {
            setUserAmountInputCauseValid(false);
            return;
        }
        setConstraintsCauseValid(!amountConstraintsStale);
        setUserAmountInputCauseValid(true);
    }


    /* --------------------------------------------------------------------- */
    // Amount conversion
    /* --------------------------------------------------------------------- */

    public Monetary toInputAmount(TradeAmount tradeAmount) {
        return amountService.getUseBaseCurrencyForAmountInput()
                ? tradeAmount.getBaseSideAmount()
                : tradeAmount.getQuoteSideAmount();
    }

    public Monetary toPassiveAmount(TradeAmount tradeAmount) {
        return amountService.getUseBaseCurrencyForAmountInput()
                ? tradeAmount.getQuoteSideAmount()
                : tradeAmount.getBaseSideAmount();
    }


    /* --------------------------------------------------------------------- */
    // Mutation API
    /* --------------------------------------------------------------------- */

    public synchronized void setUseBaseCurrencyForAmountInput(boolean value) {
        Optional<Market> market = marketService.findMarket();
        if (market.isEmpty()) {
            // A queued UI callback can arrive after dispose; the session's state is cleared
            // and must stay cleared.
            return;
        }
        if (amountService.getUseBaseCurrencyForAmountInput() == value) {
            return;
        }
        amountService.setUseBaseCurrencyForAmountInput(value);
        cookieStore.persistUseBaseCurrencyForAmountInput(market.get(), value);
        // An input-side switch is a user-initiated change: input range, marker and slider
        // mapping are recomputed on the new side (the amount itself is unchanged).
        recalculateAmountConstraints(true, false);
    }


    /* --------------------------------------------------------------------- */
    // Derived read model
    /* --------------------------------------------------------------------- */

    public AmountSpec getAmountSpec() {
        return amountService.getAmountSpec();
    }

    @Override
    public Market getMarket() {
        return marketService.getMarket();
    }

}
