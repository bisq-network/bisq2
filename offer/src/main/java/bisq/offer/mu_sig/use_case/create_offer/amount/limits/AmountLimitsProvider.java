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

package bisq.offer.mu_sig.use_case.create_offer.amount.limits;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.application.LifecycleScope;
import bisq.common.monetary.TradeAmount;
import bisq.common.monetary.TradeAmountRange;
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.offer.mu_sig.use_case.create_offer.direction.DirectionSelection;
import bisq.offer.mu_sig.use_case.create_offer.market.MarketSelection;
import bisq.offer.mu_sig.use_case.create_offer.payment_method.PaymentMethodSelection;
import bisq.offer.mu_sig.use_case.create_offer.price.PriceSelection;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class AmountLimitsProvider extends LifecycleScope {
    private final AbsoluteAmountLimitsProvider absoluteAmountLimitsProvider;
    private final PaymentMethodBasedAmountLimitsProvider paymentMethodSpecificAmountLimitsProvider;
    private final UserSpecificAmountLimitsProvider userSpecificAmountLimitsProvider;

    // Use null as marker that value has never been set yet
    private final Observable<Optional<TradeAmount>> userSpecificAmountLimit = new Observable<>(null);

    private final Observable<TradeAmountRange> effectiveTradeAmountLimits = new Observable<>();
    private final Observable<TradeAmountRange> potentialTradeAmountLimits = new Observable<>();

    private final Observable<Boolean> initialized = new Observable<>(false);

    public AmountLimitsProvider(MarketPriceService marketPriceService,
                                MarketSelection marketService,
                                DirectionSelection directionService,
                                PaymentMethodSelection paymentMethodService,
                                PriceSelection priceService) {
        checkNotNull(marketPriceService, "marketPriceService must not be null");
        checkNotNull(marketService, "marketService must not be null");
        checkNotNull(directionService, "directionService must not be null");
        checkNotNull(paymentMethodService, "paymentMethodService must not be null");
        checkNotNull(priceService, "priceService must not be null");

        absoluteAmountLimitsProvider = new AbsoluteAmountLimitsProvider(marketPriceService, marketService, priceService);
        paymentMethodSpecificAmountLimitsProvider = new PaymentMethodBasedAmountLimitsProvider(marketPriceService, marketService, paymentMethodService, priceService);
        userSpecificAmountLimitsProvider = new UserSpecificAmountLimitsProvider(marketPriceService, marketService, directionService, priceService);
    }

    @Override
    public void initialize() {
        absoluteAmountLimitsProvider.initialize();
        paymentMethodSpecificAmountLimitsProvider.initialize();
        userSpecificAmountLimitsProvider.initialize();

        addDisposable(absoluteAmountLimitsProvider.tradeAmountLimitsObservable().addObserver(tradeAmountLimits -> {
            update(tradeAmountLimits,
                    paymentMethodSpecificAmountLimitsProvider.getTradeAmountLimit(),
                    userSpecificAmountLimitsProvider.getTradeAmountLimit());
        }));
        addDisposable(paymentMethodSpecificAmountLimitsProvider.tradeAmountLimitObservable().addObserver(tradeAmountLimit -> {
            update(absoluteAmountLimitsProvider.getTradeAmountLimits(),
                    tradeAmountLimit,
                    userSpecificAmountLimitsProvider.getTradeAmountLimit());
        }));
        addDisposable(userSpecificAmountLimitsProvider.tradeAmountLimitObservable().addObserver(tradeAmountLimit -> {
            update(absoluteAmountLimitsProvider.getTradeAmountLimits(),
                    paymentMethodSpecificAmountLimitsProvider.getTradeAmountLimit(),
                    tradeAmountLimit);
        }));
    }

    @Override
    public void dispose() {
        super.dispose();
        absoluteAmountLimitsProvider.dispose();
        paymentMethodSpecificAmountLimitsProvider.dispose();
        userSpecificAmountLimitsProvider.dispose();
    }

    private void update(TradeAmountRange absoluteTradeAmountLimits,
                        TradeAmount paymentMethodSpecificAmountLimit,
                        Optional<TradeAmount> userSpecificAmountLimit) {

        if (dependenciesValid(absoluteTradeAmountLimits, paymentMethodSpecificAmountLimit, userSpecificAmountLimit)) {
            TradeAmount min = absoluteTradeAmountLimits.getMin();
            TradeAmount potentialLimit = paymentMethodSpecificAmountLimit.clamp(absoluteTradeAmountLimits);
            TradeAmountRange paymentMethodSpecificAmountLimits = new TradeAmountRange(min, potentialLimit);
            potentialTradeAmountLimits.set(new TradeAmountRange(min, potentialLimit));

            if (userSpecificAmountLimit.isPresent()) {
                TradeAmount userSpecificLimit = userSpecificAmountLimit.get().clamp(paymentMethodSpecificAmountLimits);
                this.userSpecificAmountLimit.set(Optional.of(userSpecificLimit));
                effectiveTradeAmountLimits.set(new TradeAmountRange(min, userSpecificLimit));
            } else {
                this.userSpecificAmountLimit.set(Optional.empty());
                effectiveTradeAmountLimits.set(new TradeAmountRange(min, potentialLimit));
            }

            initialized.set(true);
        } else if (Boolean.TRUE.equals(initialized.get())) {
            // A provider cleared its output on a market change; the combined limits must not
            // retain the previous market's ranges either. Consumers treat null as the
            // unseeded state.
            potentialTradeAmountLimits.set(null);
            effectiveTradeAmountLimits.set(null);
            this.userSpecificAmountLimit.set(Optional.empty());
        }
    }


    private static boolean dependenciesValid(TradeAmountRange absoluteTradeAmountLimits,
                                             TradeAmount paymentMethodSpecificAmountLimit,
                                             Optional<TradeAmount> userSpecificAmountLimit) {
        if (absoluteTradeAmountLimits == null ||
                paymentMethodSpecificAmountLimit == null ||
                userSpecificAmountLimit == null) {
            return false;
        }

        TradeAmount absoluteMin = absoluteTradeAmountLimits.getMin();
        TradeAmount absoluteMax = absoluteTradeAmountLimits.getMax();
        if (absoluteMin == null || absoluteMax == null) {
            return false;
        }

        if (!matchingMarket(absoluteMin, paymentMethodSpecificAmountLimit) ||
                !matchingMarket(absoluteMax, paymentMethodSpecificAmountLimit)) {
            return false;
        }

        return userSpecificAmountLimit.isEmpty() ||
                (matchingMarket(absoluteMin, userSpecificAmountLimit.get()) &&
                        matchingMarket(absoluteMax, userSpecificAmountLimit.get()));
    }

    private static boolean matchingMarket(TradeAmount left, TradeAmount right) {
        return left.getBaseSideAmount().getCode().equals(right.getBaseSideAmount().getCode()) &&
                left.getQuoteSideAmount().getCode().equals(right.getQuoteSideAmount().getCode());
    }


    /* --------------------------------------------------------------------- */
    // Getters
    /* --------------------------------------------------------------------- */

    public ReadOnlyObservable<TradeAmountRange> effectiveTradeAmountLimitsObservable() {
        return effectiveTradeAmountLimits;
    }

    public TradeAmountRange getEffectiveAmountLimits() {
        return effectiveTradeAmountLimits.get();
    }

    public ReadOnlyObservable<TradeAmountRange> potentialTradeAmountLimitsObservable() {
        return potentialTradeAmountLimits;
    }

    public TradeAmountRange getPotentialTradeAmountLimits() {
        return potentialTradeAmountLimits.get();
    }

    public ReadOnlyObservable<Optional<TradeAmount>> userSpecificAmountLimitObservable() {
        return userSpecificAmountLimit;
    }

    public Optional<TradeAmount> getUserSpecificAmountLimit() {
        return userSpecificAmountLimit.get();
    }

    public Observable<Boolean> initializedObservable() {
        return initialized;
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public TradeAmount clamp(TradeAmount tradeAmount) {
        checkNotNull(tradeAmount, "tradeAmount must not be null");
        checkArgument(isInitialized(), "AmountLimits must be initialized");
        return tradeAmount.clamp(getEffectiveAmountLimits());
    }
}
