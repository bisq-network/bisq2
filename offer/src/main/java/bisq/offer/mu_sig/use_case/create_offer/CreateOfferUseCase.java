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

package bisq.offer.mu_sig.use_case.create_offer;

import bisq.account.AccountService;
import bisq.account.payment_method.PaymentMethod;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.monetary.Fiat;
import bisq.common.observable.Observable;
import bisq.common.util.StringUtils;
import bisq.offer.Direction;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.mu_sig.use_case.DraftOfferUseCase;
import bisq.offer.mu_sig.use_case.create_offer.amount.AmountSelection;
import bisq.offer.mu_sig.use_case.create_offer.direction.DirectionSelection;
import bisq.offer.mu_sig.use_case.create_offer.market.MarketSelection;
import bisq.offer.mu_sig.use_case.create_offer.payment_method.PaymentMethodSelection;
import bisq.offer.mu_sig.use_case.create_offer.price.PriceSelection;
import bisq.offer.mu_sig.use_case.dependencies.AccountsProvider;
import bisq.offer.mu_sig.use_case.dependencies.CreateOfferDraftCookieStore;
import bisq.offer.mu_sig.use_case.dependencies.DefaultAccountsProvider;
import bisq.offer.mu_sig.use_case.dependencies.DefaultCreateOfferDraftCookieStore;
import bisq.offer.options.CollateralOption;
import bisq.offer.options.OfferOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.price.spec.PriceSpec;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
@Getter
public class CreateOfferUseCase extends DraftOfferUseCase {
    public static final Fiat DEFAULT_TRADE_AMOUNT_IN_USD = Fiat.fromFaceValue(500, "USD");

    // Serializes every draft state transition: market-price updates arrive on the market data
    // thread while user input arrives on the UI thread, and amount state is recomputed
    // synchronously inside price and market transitions.
    private final Object draftLock = new Object();
    private final MarketSelection marketSelection;
    private final DirectionSelection directionSelection;
    private final PaymentMethodSelection paymentMethodSelection;
    private final PriceSelection priceSelection;
    private final AmountSelection amountSelection;

    private final Observable<Boolean> initialized = new Observable<>(false);


    /* --------------------------------------------------------------------- */
    // Construction
    /* --------------------------------------------------------------------- */

    public CreateOfferUseCase(MarketPriceService marketPriceService,
                              SettingsService settingsService,
                              AccountService accountService) {
        this(checkNotNull(marketPriceService, "marketPriceService must not be null"),
                new DefaultCreateOfferDraftCookieStore(checkNotNull(settingsService, "settingsService must not be null")),
                new DefaultAccountsProvider(checkNotNull(accountService, "accountService must not be null")));
    }

    CreateOfferUseCase(MarketPriceService marketPriceService,
                       CreateOfferDraftCookieStore cookieStore,
                       AccountsProvider accountsProvider) {
        marketSelection = new MarketSelection(draftLock);
        directionSelection = new DirectionSelection(cookieStore, draftLock);
        paymentMethodSelection = new PaymentMethodSelection(marketSelection, accountsProvider, draftLock);
        priceSelection = new PriceSelection(marketPriceService, marketSelection, cookieStore, draftLock);
        amountSelection = new AmountSelection(marketPriceService,
                marketSelection,
                directionSelection,
                paymentMethodSelection,
                priceSelection,
                cookieStore,
                draftLock);
    }


    /* --------------------------------------------------------------------- */
    // Lifecycle
    /* --------------------------------------------------------------------- */

    @Override
    public void initialize() {
        synchronized (draftLock) {
            marketSelection.initialize();
            directionSelection.initialize();
            paymentMethodSelection.initialize();
            priceSelection.initialize();
            amountSelection.initialize();

            addDisposable(amountSelection.initializedObservable().addObserver(initialized -> {
                if (amountSelection.isInitialized()) {
                    setInitialized(true);
                }
            }));
        }
    }

    @Override
    public void dispose() {
        synchronized (draftLock) {
            super.dispose();

            marketSelection.dispose();
            directionSelection.dispose();
            paymentMethodSelection.dispose();
            priceSelection.dispose();
            amountSelection.dispose();
        }
    }

    /**
     * With the market price arriving late the fail-soft selections leave the amounts and the
     * price quote unset instead of failing. Such a draft cannot be materialized into a
     * snapshot; callers must not enter the review step until this lifts.
     */
    public boolean isDraftReadyForReview() {
        synchronized (draftLock) {
            return marketSelection.getMarket() != null
                    && directionSelection.getDisplayDirection() != null
                    && amountSelection.getFixTradeAmount() != null
                    && priceSelection.getPriceQuote() != null;
        }
    }

    /**
     * Everything the offer is built from, captured in one synchronized read together with the
     * snapshot the review displays, so the published offer and the displayed values cannot
     * diverge.
     */
    public record Handoff(String offerId,
                          Direction direction,
                          Market market,
                          AmountSpec amountSpec,
                          PriceSpec priceSpec,
                          List<PaymentMethod<?>> paymentMethods,
                          List<OfferOption> offerOptions,
                          DraftSnapshot snapshot) {
        public Handoff {
            checkNotNull(offerId, "offerId must not be null");
            checkNotNull(direction, "direction must not be null");
            checkNotNull(market, "market must not be null");
            checkNotNull(amountSpec, "amountSpec must not be null");
            checkNotNull(priceSpec, "priceSpec must not be null");
            checkNotNull(paymentMethods, "paymentMethods must not be null");
            checkNotNull(offerOptions, "offerOptions must not be null");
            checkNotNull(snapshot, "snapshot must not be null");
        }
    }

    // Empty while the draft is not ready for review.
    public Optional<Handoff> getHandoff() {
        synchronized (draftLock) {
            if (!isDraftReadyForReview()) {
                return Optional.empty();
            }
            return Optional.of(toHandoff(captureDraftSnapshot(), StringUtils.createUid()));
        }
    }

    // A pure function of the snapshot and the offer id, which salts the account data so the
    // same account is not recognizable across offers.
    static Handoff toHandoff(DraftSnapshot snapshot, String offerId) {
        Market market = snapshot.market();
        List<PaymentMethod<?>> paymentMethods = snapshot.accountByPaymentMethod().keySet().stream()
                .sorted(Comparator.comparing(PaymentMethod::getPaymentRailName))
                .toList();
        List<OfferOption> offerOptions = new ArrayList<>();
        snapshot.accountByPaymentMethod().values().stream()
                .sorted(Comparator.comparing(account -> account.getPaymentMethod().getPaymentRailName()))
                .map(account -> OfferOptionUtil.createAccountOption(account, offerId))
                .forEach(offerOptions::add);
        offerOptions.add(new CollateralOption(MuSigOffer.DEFAULT_BUYER_SECURITY_DEPOSIT,
                MuSigOffer.DEFAULT_SELLER_SECURITY_DEPOSIT));
        return new Handoff(offerId,
                Direction.displayDirectionToOfferDirection(snapshot.displayDirection(), market),
                market,
                snapshot.amountSpec(),
                snapshot.priceSpec(),
                paymentMethods,
                List.copyOf(offerOptions),
                snapshot);
    }

    /**
     * Captures the whole draft in one synchronized read so the review step works from mutually
     * consistent values.
     */
    public DraftSnapshot captureDraftSnapshot() {
        synchronized (draftLock) {
            Market market = checkNotNull(marketSelection.getMarket(), "market must not be null");
            return new DraftSnapshot(market,
                    directionSelection.getDisplayDirection(),
                    amountSelection.createAndGetAmountSpec(market),
                    priceSelection.createAndGetPriceSpec(),
                    paymentMethodSelection.getAccountByPaymentMethod(),
                    Optional.ofNullable(priceSelection.getPriceQuote()),
                    priceSelection.getObservedMarketPriceQuote(),
                    priceSelection.getPricePercentage());
        }
    }


    /* --------------------------------------------------------------------- */
    // initialized
    /* --------------------------------------------------------------------- */

    private void setInitialized(boolean value) {
        initialized.set(value);
    }

    public Observable<Boolean> initializedObservable() {
        return initialized;
    }

    public boolean isInitialized() {
        return initialized.get();
    }



    /* --------------------------------------------------------------------- */
    // Delegate read methods
    /* --------------------------------------------------------------------- */

    @Override
    // not used anymore
    public Market getMarket() {
        return marketSelection.getMarket();
    }
}
