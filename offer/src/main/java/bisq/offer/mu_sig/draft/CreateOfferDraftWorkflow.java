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

package bisq.offer.mu_sig.draft;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentRail;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.MonetaryRange;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;
import bisq.common.monetary.TradeAmountRange;
import bisq.offer.Direction;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.AmountSpecFactory;
import bisq.offer.mu_sig.draft.dependencies.AccountsProvider;
import bisq.offer.mu_sig.draft.dependencies.CreateOfferDraftCookieStore;
import bisq.offer.mu_sig.draft.dependencies.CreateOfferDraftMarketData;
import bisq.offer.mu_sig.draft.dependencies.DefaultAccountsProvider;
import bisq.offer.mu_sig.draft.dependencies.DefaultCreateOfferDraftCookieStore;
import bisq.offer.mu_sig.draft.dependencies.DefaultCreateOfferDraftMarketData;
import bisq.settings.SettingsService;
import com.google.common.collect.ImmutableMap;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * User-facing workflow facade for creating an offer draft.
 * <p>
 * Design: exposes stable UI/API mutation methods and persistence side effects (cookies), while
 * delegating transition ordering and derived-state recalculation to {@link CreateOfferDraftStateEngine}
 * and isolated domain services.
 */
@Slf4j
public class CreateOfferDraftWorkflow extends OfferDraftWorkflow<CreateOfferDraft> {
    public static final Fiat DEFAULT_TRADE_AMOUNT_IN_USD = Fiat.fromFaceValue(500, "USD");

    private final CreateOfferDraftCookieStore cookieStore;
    private final AmountMappingService amountMappingService;
    private final PaymentMethodSelectionService paymentMethodSelectionService;
    private final CreateOfferDraftStateEngine stateEngine;
    @Delegate
    protected CreateOfferDraft createOfferDraft;

    /* --------------------------------------------------------------------- */
    // Construction
    /* --------------------------------------------------------------------- */

    public CreateOfferDraftWorkflow(MarketPriceService marketPriceService,
                                    SettingsService settingsService,
                                    AccountService accountService) {
        this(new DefaultCreateOfferDraftMarketData(marketPriceService),
                new DefaultCreateOfferDraftCookieStore(settingsService),
                new DefaultAccountsProvider(accountService));
    }

    CreateOfferDraftWorkflow(CreateOfferDraftMarketData marketData,
                             CreateOfferDraftCookieStore cookieStore,
                             AccountsProvider accountsProvider) {
        super(new CreateOfferDraft());

        this.cookieStore = checkNotNull(cookieStore, "cookieStore must not be null");
        checkNotNull(accountsProvider, "accountsProvider must not be null");

        amountMappingService = new AmountMappingService();
        TradeAmountConstraintsService tradeAmountConstraintsService = new TradeAmountConstraintsService(checkNotNull(marketData,
                "marketData must not be null"));
        paymentMethodSelectionService = new PaymentMethodSelectionService(accountsProvider);

        createOfferDraft = offerDraft;
        stateEngine = new CreateOfferDraftStateEngine(createOfferDraft,
                marketData,
                tradeAmountConstraintsService,
                amountMappingService,
                this::getSelectedPaymentRail,
                this::updatePaymentMethods,
                DEFAULT_TRADE_AMOUNT_IN_USD);
    }


    /* --------------------------------------------------------------------- */
    // Lifecycle
    /* --------------------------------------------------------------------- */

    public void initialize(Market market) {
        checkNotNull(market, "Market must not be null");

        Direction direction = cookieStore.getDirection();
        boolean useBaseCurrencyForAmountInput = cookieStore.getUseBaseCurrencyForAmountInput(market);
        boolean useRangeAmount = cookieStore.getUseRangeAmount();

        stateEngine.initialize(market, direction, useBaseCurrencyForAmountInput, useRangeAmount);
    }


    /* --------------------------------------------------------------------- */
    // Amount input entry points
    /* --------------------------------------------------------------------- */

    public void setFixTradeAmountFromInputAmount(Monetary amount) {
        TradeAmount tradeAmount = stateEngine.toClampedTradeAmount(amount);
        setFixTradeAmount(tradeAmount);
    }

    public void setMinTradeAmountFromInputAmount(Monetary amount) {
        TradeAmount tradeAmount = stateEngine.toClampedTradeAmount(amount);
        setMinTradeAmount(tradeAmount);
    }

    public void setMaxTradeAmountFromInputAmount(Monetary amount) {
        TradeAmount tradeAmount = stateEngine.toClampedTradeAmount(amount);
        setMaxTradeAmount(tradeAmount);
    }

    public void setFixTradeAmountFromSliderValue(double sliderValue) {
        TradeAmount fixTradeAmount = checkNotNull(getFixTradeAmount(), "fixTradeAmount must not be null");
        TradeAmount tradeAmount = stateEngine.toTradeAmountFromSliderValue(fixTradeAmount, sliderValue);
        setFixTradeAmount(tradeAmount);
    }

    public void setMinTradeAmountFromSliderValue(double sliderValue) {
        TradeAmount minTradeAmount = checkNotNull(getMinTradeAmount(), "minTradeAmount must not be null");
        TradeAmount tradeAmount = stateEngine.toTradeAmountFromSliderValue(minTradeAmount, sliderValue);
        setMinTradeAmount(tradeAmount);
    }

    public void setMaxTradeAmountFromSliderValue(double sliderValue) {
        TradeAmount maxTradeAmount = checkNotNull(getMaxTradeAmount(), "maxTradeAmount must not be null");
        TradeAmount tradeAmount = stateEngine.toTradeAmountFromSliderValue(maxTradeAmount, sliderValue);
        setMaxTradeAmount(tradeAmount);
    }


    /* --------------------------------------------------------------------- */
    // Amount conversion
    /* --------------------------------------------------------------------- */

    public Monetary toInputAmount(TradeAmount tradeAmount, boolean includeUserSpecificTradeAmountLimit) {
        boolean useBaseCurrencyForAmountInput = getUseBaseCurrencyForAmountInput();
        TradeAmountRange limits = getClampLimits(includeUserSpecificTradeAmountLimit);
        return amountMappingService.toInputAmount(tradeAmount, limits, useBaseCurrencyForAmountInput);
    }

    public Monetary toPassiveAmount(TradeAmount tradeAmount, boolean includeUserSpecificTradeAmountLimit) {
        boolean useBaseCurrencyForAmountInput = getUseBaseCurrencyForAmountInput();
        TradeAmountRange limits = getClampLimits(includeUserSpecificTradeAmountLimit);
        return amountMappingService.toPassiveAmount(tradeAmount, limits, useBaseCurrencyForAmountInput);
    }


    /* --------------------------------------------------------------------- */
    // Mutation API
    /* --------------------------------------------------------------------- */

    // Core market/pricing state
    public void setMarket(Market market) {
        checkNotNull(market, "Market must not be null");
        if (market.equals(getMarket())) {
            return;
        }
        stateEngine.applyMarketChanged(market);
    }

    public void setDirection(Direction direction) {
        checkNotNull(direction, "Direction must not be null");
        if (direction.equals(getDirection())) {
            return;
        }

        if (stateEngine.applyDirectionChanged(direction)) {
            cookieStore.persistDirection(direction);
        }
    }

    public void setPriceQuote(PriceQuote priceQuote) {
        checkNotNull(priceQuote, "PriceQuote must not be null");
        if (priceQuote.equals(getPriceQuote())) {
            return;
        }
        stateEngine.applyPriceQuoteChanged(priceQuote);
    }

    public void setUseBaseCurrencyForAmountInput(boolean value) {
        if (value == getUseBaseCurrencyForAmountInput()) {
            return;
        }

        Market market = getMarket();
        if (market == null) {
            offerDraft.setUseBaseCurrencyForAmountInput(value);
            return;
        }

        if (stateEngine.applyUseBaseCurrencyForAmountInputChanged(value)) {
            cookieStore.persistUseBaseCurrencyForAmountInput(market, value);
        }
    }

    public void setUseRangeAmount(boolean useRangeAmount) {
        if (useRangeAmount == getUseRangeAmount()) {
            return;
        }

        if (stateEngine.applyUseRangeAmountChanged(useRangeAmount)) {
            cookieStore.persistUseRangeAmount(useRangeAmount);
        }
    }

    // Amount state
    public void setFixTradeAmount(TradeAmount tradeAmount) {
        stateEngine.setFixTradeAmount(tradeAmount);
    }

    public void setMinTradeAmount(TradeAmount tradeAmount) {
        stateEngine.setMinTradeAmount(tradeAmount);
    }

    public void setMaxTradeAmount(TradeAmount tradeAmount) {
        stateEngine.setMaxTradeAmount(tradeAmount);
    }

    public void setTradeAmountLimits(TradeAmountRange tradeAmountRange) {
        checkNotNull(tradeAmountRange, "TradeAmountRange must not be null");
        offerDraft.setTradeAmountLimits(tradeAmountRange);
    }

    public void setUserSpecificTradeAmountLimit(Optional<TradeAmount> tradeAmount) {
        tradeAmount.ifPresent(amount -> checkNotNull(amount, "tradeAmount must not be null"));
        offerDraft.setUserSpecificTradeAmountLimit(tradeAmount);
    }

    public void setInputAmountLimits(MonetaryRange inputAmountLimits) {
        checkNotNull(inputAmountLimits, "inputAmountLimits must not be null");
        offerDraft.setInputAmountLimits(inputAmountLimits);
    }

    // Payment account state
    public void putAccountsByPaymentMethod(PaymentMethod<?> paymentMethod, List<Account<?, ?>> account) {
        checkNotNull(paymentMethod, "paymentMethod must not be null");
        checkNotNull(account, "account must not be null");
        createOfferDraft.putAccountsByPaymentMethod(paymentMethod, account);
    }

    public void removeAccountsByPaymentMethod(PaymentMethod<?> paymentMethod) {
        offerDraft.removeAccountsByPaymentMethod(paymentMethod);
    }

    public void putAllAccountsByPaymentMethod(Map<PaymentMethod<?>, List<Account<?, ?>>> selectedAccountByPaymentMethod) {
        checkNotNull(selectedAccountByPaymentMethod, "selectedAccountByPaymentMethod must not be null");
        offerDraft.putAllAccountsByPaymentMethod(selectedAccountByPaymentMethod);
    }

    public void clearAccountsByPaymentMethod() {
        offerDraft.clearAccountsByPaymentMethod();
    }

    public void putSelectedAccountByPaymentMethod(PaymentMethod<?> paymentMethod, Account<?, ?> account) {
        checkNotNull(paymentMethod, "paymentMethod must not be null");
        checkNotNull(account, "account must not be null");
        putSelectedAccountByPaymentMethod(paymentMethod, account, true);
    }

    public void removeSelectedAccountByPaymentMethod(PaymentMethod<?> paymentMethod) {
        removeSelectedAccountByPaymentMethod(paymentMethod, true);
    }

    public void putAllSelectedAccountByPaymentMethod(Map<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod) {
        checkNotNull(selectedAccountByPaymentMethod, "selectedAccountByPaymentMethod must not be null");
        putAllSelectedAccountByPaymentMethod(selectedAccountByPaymentMethod, true);
    }

    public void clearSelectedAccountByPaymentMethod() {
        clearSelectedAccountByPaymentMethod(true);
    }


    /* --------------------------------------------------------------------- */
    // Derived read model
    /* --------------------------------------------------------------------- */

    public AmountSpec getAmountSpec() {
        Market market = checkNotNull(getMarket(), "market must not be null");
        boolean isBtcFiatMarket = market.isBtcFiatMarket();
        boolean useRangeAmount = getUseRangeAmount();
        return AmountSpecFactory.createAmountSpec(isBtcFiatMarket,
                useRangeAmount,
                getMinTradeAmount(),
                getMaxTradeAmount(),
                getFixTradeAmount());
    }


    /* --------------------------------------------------------------------- */
    // Internal helpers
    /* --------------------------------------------------------------------- */

    /* --------------------------------------------------------------------- */
    // PaymentMethods
    /* --------------------------------------------------------------------- */

    private void updatePaymentMethods() {
        Market market = getMarket();
        PaymentMethodSelectionService.MarketAccounts marketAccounts = paymentMethodSelectionService.loadAccountsForMarket(market);
        List<Account<?, ?>> accountsForMarket = marketAccounts.accountsForMarket();
        Map<PaymentMethod<?>, List<Account<?, ?>>> map = marketAccounts.accountsByPaymentMethod();
        if (!getAccountsByPaymentMethod().equals(map)) {
            clearAccountsByPaymentMethod();
            putAllAccountsByPaymentMethod(map);
        }

        boolean selectedAccountsChanged = false;

        // Remove payment methods which are not present in the eligible accounts
        ImmutableMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod = getSelectedAccountByPaymentMethod();
        List<? extends PaymentMethod<?>> paymentMethodsToRemove = paymentMethodSelectionService.findSelectedPaymentMethodsToRemove(selectedAccountByPaymentMethod,
                accountsForMarket);
        if (!paymentMethodsToRemove.isEmpty()) {
            selectedAccountsChanged = true;
            paymentMethodsToRemove.forEach(paymentMethod -> removeSelectedAccountByPaymentMethod(paymentMethod, false));
        }

        // If we have only one, we pre-select
        Optional<Account<?, ?>> accountToAutoSelect = paymentMethodSelectionService.findAccountToAutoSelect(accountsForMarket,
                getSelectedAccountByPaymentMethod());
        if (accountToAutoSelect.isPresent()) {
            Account<?, ?> account = accountToAutoSelect.get();
            selectedAccountsChanged |= putSelectedAccountByPaymentMethod(account.getPaymentMethod(), account, false);
        }

        if (selectedAccountsChanged) {
            stateEngine.recalculateTradeAmountConstraintsForSelectedPaymentRail();
        }
    }

    private boolean putSelectedAccountByPaymentMethod(PaymentMethod<?> paymentMethod,
                                                      Account<?, ?> account,
                                                      boolean recalculateTradeAmountConstraints) {
        Account<?, ?> existing = getSelectedAccountByPaymentMethod().get(paymentMethod);
        if (account.equals(existing)) {
            return false;
        }
        createOfferDraft.putSelectedAccountByPaymentMethod(paymentMethod, account);
        if (recalculateTradeAmountConstraints) {
            stateEngine.recalculateTradeAmountConstraintsForSelectedPaymentRail();
        }
        return true;
    }

    private boolean removeSelectedAccountByPaymentMethod(PaymentMethod<?> paymentMethod,
                                                         boolean recalculateTradeAmountConstraints) {
        if (!getSelectedAccountByPaymentMethod().containsKey(paymentMethod)) {
            return false;
        }
        offerDraft.removeSelectedAccountByPaymentMethod(paymentMethod);
        if (recalculateTradeAmountConstraints) {
            stateEngine.recalculateTradeAmountConstraintsForSelectedPaymentRail();
        }
        return true;
    }

    private boolean putAllSelectedAccountByPaymentMethod(Map<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod,
                                                         boolean recalculateTradeAmountConstraints) {
        if (selectedAccountByPaymentMethod.isEmpty()) {
            return clearSelectedAccountByPaymentMethod(recalculateTradeAmountConstraints);
        }
        ImmutableMap<PaymentMethod<?>, Account<?, ?>> existing = getSelectedAccountByPaymentMethod();
        boolean changed = selectedAccountByPaymentMethod.entrySet().stream()
                .anyMatch(entry -> !entry.getValue().equals(existing.get(entry.getKey())));
        if (!changed) {
            return false;
        }
        offerDraft.putAllSelectedAccountByPaymentMethod(selectedAccountByPaymentMethod);
        if (recalculateTradeAmountConstraints) {
            stateEngine.recalculateTradeAmountConstraintsForSelectedPaymentRail();
        }
        return true;
    }

    private boolean clearSelectedAccountByPaymentMethod(boolean recalculateTradeAmountConstraints) {
        if (getSelectedAccountByPaymentMethod().isEmpty()) {
            return false;
        }
        offerDraft.clearSelectedAccountByPaymentMethod();
        if (recalculateTradeAmountConstraints) {
            stateEngine.recalculateTradeAmountConstraintsForSelectedPaymentRail();
        }
        return true;
    }

    private PaymentRail getSelectedPaymentRail() {
        return paymentMethodSelectionService.findMostRestrictiveSelectedPaymentRail(getSelectedAccountByPaymentMethod());
    }

    private TradeAmountRange getClampLimits(boolean includeUserSpecificTradeAmountLimit) {
        return stateEngine.getClampLimits(includeUserSpecificTradeAmountLimit);
    }
}
