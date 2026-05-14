package bisq.offer.mu_sig.draft;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.MonetaryRange;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;
import bisq.common.monetary.TradeAmountConversion;
import bisq.common.monetary.TradeAmountRange;
import bisq.offer.Direction;
import bisq.offer.mu_sig.draft.dependencies.AccountsProvider;
import bisq.offer.mu_sig.draft.dependencies.CreateOfferDraftCookieStore;
import bisq.offer.mu_sig.draft.dependencies.CreateOfferDraftMarketData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateOfferDraftWorkflowTest {
    private Market defaultMarket;
    private Market usdBtcMarket;
    private Market xmrBtcMarket;
    private PriceQuote defaultMarketPriceQuote;
    private PriceQuote usdBtcPriceQuote;
    private PriceQuote xmrBtcPriceQuote;
    private TradeAmount defaultMarketDefaultTradeAmount;
    private TradeAmount usdBtcDefaultTradeAmount;
    private TradeAmount xmrBtcDefaultTradeAmount;
    private FakeMarketData marketData;
    private FakeCookieStore cookieStore;
    private FakeAccountsProvider accountsProvider;
    private CreateOfferDraftWorkflow workflow;

    @BeforeEach
    public void setUp() {
        defaultMarket = MarketRepository.getDefaultBtcFiatMarket();
        usdBtcMarket = MarketRepository.getUSDBitcoinMarket();
        xmrBtcMarket = MarketRepository.getXmrBtcMarket();

        defaultMarketPriceQuote = PriceQuote.fromFiatPrice(50000, defaultMarket.getQuoteCurrencyCode());
        usdBtcPriceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        xmrBtcPriceQuote = PriceQuote.fromPrice(0.005, "XMR", "BTC");

        defaultMarketDefaultTradeAmount = TradeAmountConversion.toTradeAmount(defaultMarket,
                defaultMarketPriceQuote,
                Fiat.fromFaceValue(500, defaultMarket.getQuoteCurrencyCode()));
        usdBtcDefaultTradeAmount = TradeAmountConversion.toTradeAmount(usdBtcMarket,
                usdBtcPriceQuote,
                Fiat.fromFaceValue(500, "USD"));
        xmrBtcDefaultTradeAmount = TradeAmountConversion.toTradeAmount(xmrBtcMarket,
                xmrBtcPriceQuote,
                Coin.asBtcFromFaceValue(0.01));

        marketData = new FakeMarketData(usdBtcPriceQuote);
        marketData.put(defaultMarket, defaultMarketPriceQuote, defaultMarketDefaultTradeAmount);
        marketData.put(usdBtcMarket, usdBtcPriceQuote, usdBtcDefaultTradeAmount);
        marketData.put(xmrBtcMarket, xmrBtcPriceQuote, xmrBtcDefaultTradeAmount);

        cookieStore = new FakeCookieStore(Direction.SELL, false, true, false);
        accountsProvider = new FakeAccountsProvider();
        workflow = new CreateOfferDraftWorkflow(marketData, cookieStore, accountsProvider);
    }

    @Test
    @DisplayName("initialize populates draft and updates payment methods")
    public void initialize_populates_draft_and_updates_payment_methods() {
        workflow.initialize(defaultMarket);

        assertEquals(defaultMarket, workflow.getMarket());
        assertEquals(Direction.SELL, workflow.getDirection());
        assertEquals(defaultMarketPriceQuote, workflow.getPriceQuote());
        assertEquals(defaultMarketDefaultTradeAmount, workflow.getFixTradeAmount());
        assertEquals(defaultMarketDefaultTradeAmount, workflow.getMinTradeAmount());
        assertEquals(defaultMarketDefaultTradeAmount, workflow.getMaxTradeAmount());
        assertNotNull(workflow.getTradeAmountLimits());
        assertNotNull(workflow.getInputAmountLimits());
        assertEquals(List.of(defaultMarket), accountsProvider.requestedMarkets);
    }

    @Test
    @DisplayName("set market resets price and amounts deterministically")
    public void set_market_resets_price_and_amounts_deterministically() {
        workflow.initialize(defaultMarket);
        workflow.setMarket(xmrBtcMarket);

        assertEquals(xmrBtcMarket, workflow.getMarket());
        assertEquals(xmrBtcPriceQuote, workflow.getPriceQuote());
        assertEquals(xmrBtcDefaultTradeAmount, workflow.getFixTradeAmount());
        assertEquals(xmrBtcDefaultTradeAmount, workflow.getMinTradeAmount());
        assertEquals(xmrBtcDefaultTradeAmount, workflow.getMaxTradeAmount());
        assertNotNull(workflow.getTradeAmountLimits());
        assertNotNull(workflow.getInputAmountLimits());
        assertEquals(List.of(defaultMarket, xmrBtcMarket), accountsProvider.requestedMarkets);
    }

    @Test
    @DisplayName("set price quote keeps quote input amount constant")
    public void set_price_quote_keeps_quote_input_amount_constant() {
        workflow.initialize(usdBtcMarket);
        workflow.setUseBaseCurrencyForAmountInput(false);
        workflow.setFixTradeAmountFromInputAmount(Fiat.fromFaceValue(500, "USD"));

        TradeAmount fixTradeAmountBefore = workflow.getFixTradeAmount();
        workflow.setPriceQuote(PriceQuote.fromFiatPrice(40000, "USD"));
        TradeAmount fixTradeAmountAfter = workflow.getFixTradeAmount();

        assertEquals(fixTradeAmountBefore.getQuoteSideAmount(), fixTradeAmountAfter.getQuoteSideAmount());
        assertEquals(Coin.asBtcFromFaceValue(0.0125), fixTradeAmountAfter.getBaseSideAmount());
    }

    @Test
    @DisplayName("set direction recomputes user specific limit and keeps amounts stable")
    public void set_direction_recomputes_user_specific_limit_and_keeps_amounts_stable() {
        workflow.initialize(usdBtcMarket);
        TradeAmount fixTradeAmountBefore = workflow.getFixTradeAmount();

        workflow.setDirection(Direction.BUY);
        Optional<TradeAmount> buyLimit = workflow.getUserSpecificTradeAmountLimit();

        workflow.setDirection(Direction.SELL);

        assertTrue(buyLimit.isPresent());
        assertTrue(workflow.getUserSpecificTradeAmountLimit().isEmpty());
        assertEquals(fixTradeAmountBefore, workflow.getFixTradeAmount());
        assertEquals(List.of(Direction.BUY, Direction.SELL), cookieStore.persistedDirections);
    }

    @Test
    @DisplayName("selected accounts use most restrictive payment rail limit")
    public void selected_accounts_use_most_restrictive_payment_rail_limit() {
        workflow.initialize(usdBtcMarket);

        PaymentMethod<?> veryLowRiskMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ADVANCED_CASH);
        PaymentMethod<?> moderateRiskMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER);
        Account<?, ?> veryLowRiskAccount = createAccount(veryLowRiskMethod);
        Account<?, ?> moderateRiskAccount = createAccount(moderateRiskMethod);

        workflow.putSelectedAccountByPaymentMethod(veryLowRiskMethod, veryLowRiskAccount);
        assertEquals(Fiat.fromFaceValue(10000, "USD"),
                workflow.getTradeAmountLimits().getMax().getQuoteSideAmount());

        workflow.putSelectedAccountByPaymentMethod(moderateRiskMethod, moderateRiskAccount);
        assertEquals(Fiat.fromFaceValue(5000, "USD"),
                workflow.getTradeAmountLimits().getMax().getQuoteSideAmount());
    }

    @Test
    @DisplayName("selected account limit change clamps existing amounts")
    public void selected_account_limit_change_clamps_existing_amounts() {
        workflow.initialize(usdBtcMarket);
        workflow.setUseBaseCurrencyForAmountInput(false);
        workflow.setFixTradeAmountFromInputAmount(Fiat.fromFaceValue(9000, "USD"));

        PaymentMethod<?> veryLowRiskMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ADVANCED_CASH);
        PaymentMethod<?> moderateRiskMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER);
        Account<?, ?> veryLowRiskAccount = createAccount(veryLowRiskMethod);
        Account<?, ?> moderateRiskAccount = createAccount(moderateRiskMethod);

        workflow.putSelectedAccountByPaymentMethod(veryLowRiskMethod, veryLowRiskAccount);
        assertEquals(Fiat.fromFaceValue(9000, "USD"), workflow.getFixTradeAmount().getQuoteSideAmount());

        workflow.putSelectedAccountByPaymentMethod(moderateRiskMethod, moderateRiskAccount);
        assertEquals(Fiat.fromFaceValue(5000, "USD"), workflow.getFixTradeAmount().getQuoteSideAmount());
    }

    @Test
    @DisplayName("set direction with current value is no op")
    public void set_direction_with_current_value_is_no_op() {
        workflow.initialize(usdBtcMarket);

        TradeAmount fixTradeAmountBefore = workflow.getFixTradeAmount();
        workflow.setDirection(Direction.SELL);

        assertEquals(fixTradeAmountBefore, workflow.getFixTradeAmount());
        assertTrue(cookieStore.persistedDirections.isEmpty());
    }

    @Test
    @DisplayName("set price quote with current value is no op")
    public void set_price_quote_with_current_value_is_no_op() {
        workflow.initialize(usdBtcMarket);
        int recalculationCountBefore = marketData.btcUsdPriceQuoteRequests;

        workflow.setPriceQuote(workflow.getPriceQuote());

        assertEquals(recalculationCountBefore, marketData.btcUsdPriceQuoteRequests);
    }

    @Test
    @DisplayName("set market with current value is no op")
    public void set_market_with_current_value_is_no_op() {
        workflow.initialize(defaultMarket);

        workflow.setMarket(defaultMarket);

        assertEquals(List.of(defaultMarket), accountsProvider.requestedMarkets);
    }

    @Test
    @DisplayName("selecting same account twice does not recalculate constraints")
    public void selecting_same_account_twice_does_not_recalculate_constraints() {
        workflow.initialize(usdBtcMarket);

        PaymentMethod<?> moderateRiskMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER);
        Account<?, ?> moderateRiskAccount = createAccount(moderateRiskMethod);

        workflow.putSelectedAccountByPaymentMethod(moderateRiskMethod, moderateRiskAccount);
        int recalculationCountAfterFirstSelection = marketData.btcUsdPriceQuoteRequests;

        workflow.putSelectedAccountByPaymentMethod(moderateRiskMethod, moderateRiskAccount);

        assertEquals(recalculationCountAfterFirstSelection, marketData.btcUsdPriceQuoteRequests);
    }

    @Test
    @DisplayName("set use base currency for amount input with current value is no op")
    public void set_use_base_currency_for_amount_input_with_current_value_is_no_op() {
        workflow.initialize(usdBtcMarket);

        workflow.setUseBaseCurrencyForAmountInput(false);

        assertTrue(cookieStore.persistedInputModes.isEmpty());
    }

    @Test
    @DisplayName("set use base currency for amount input with different value persists preference")
    public void set_use_base_currency_for_amount_input_with_different_value_persists_preference() {
        workflow.initialize(usdBtcMarket);

        workflow.setUseBaseCurrencyForAmountInput(true);

        assertEquals(List.of(new InputModePreference(usdBtcMarket, true)), cookieStore.persistedInputModes);
    }

    @Test
    @DisplayName("set use range amount with current value is no op")
    public void set_use_range_amount_with_current_value_is_no_op() {
        workflow.initialize(usdBtcMarket);

        workflow.setUseRangeAmount(false);

        assertTrue(cookieStore.persistedUseRangeAmountValues.isEmpty());
    }

    @Test
    @DisplayName("set use range amount with different value persists preference")
    public void set_use_range_amount_with_different_value_persists_preference() {
        workflow.initialize(usdBtcMarket);

        workflow.setUseRangeAmount(true);

        assertEquals(List.of(true), cookieStore.persistedUseRangeAmountValues);
    }

    @Test
    @DisplayName("get amount spec throws when market is null")
    public void get_amount_spec_throws_when_market_is_null() {
        try {
            workflow.getAmountSpec();
            throw new AssertionError("Expected NullPointerException");
        } catch (NullPointerException e) {
            assertEquals("market must not be null", e.getMessage());
        }
    }

    @Test
    @DisplayName("to input amount and to passive amount are consistent")
    public void to_input_amount_and_to_passive_amount_are_consistent() {
        workflow.initialize(usdBtcMarket);
        workflow.setUseBaseCurrencyForAmountInput(false);

        TradeAmount tradeAmount = workflow.getFixTradeAmount();
        var inputAmount = workflow.toInputAmount(tradeAmount, true);
        var passiveAmount = workflow.toPassiveAmount(tradeAmount, true);

        assertEquals(tradeAmount.getQuoteSideAmount(), inputAmount);
        assertEquals(tradeAmount.getBaseSideAmount(), passiveAmount);
    }

    @Test
    @DisplayName("set fix trade amount from slider value updates amount")
    public void set_fix_trade_amount_from_slider_value_updates_amount() {
        workflow.initialize(usdBtcMarket);
        workflow.setUseBaseCurrencyForAmountInput(false);

        workflow.setFixTradeAmountFromSliderValue(0.0);
        Fiat minAmount = (Fiat) workflow.getFixTradeAmount().getQuoteSideAmount();

        workflow.setFixTradeAmountFromSliderValue(1.0);
        Fiat maxAmount = (Fiat) workflow.getFixTradeAmount().getQuoteSideAmount();

        assertTrue(minAmount.getValue() < maxAmount.getValue());
    }

    @Test
    @DisplayName("set min and max trade amount from slider value works correctly")
    public void set_min_and_max_trade_amount_from_slider_value_works_correctly() {
        workflow.initialize(usdBtcMarket);
        workflow.setUseRangeAmount(true);

        workflow.setMinTradeAmountFromSliderValue(0.2);
        workflow.setMaxTradeAmountFromSliderValue(0.8);

        TradeAmount minAmount = workflow.getMinTradeAmount();
        TradeAmount maxAmount = workflow.getMaxTradeAmount();

        assertTrue(minAmount.getQuoteSideAmount().getValue() < maxAmount.getQuoteSideAmount().getValue());
    }

    @Test
    @DisplayName("clear accounts by payment method removes all accounts")
    public void clear_accounts_by_payment_method_removes_all_accounts() {
        workflow.initialize(usdBtcMarket);
        PaymentMethod<?> achMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER);
        Account<?, ?> achAccount = createAccount(achMethod);

        workflow.putAccountsByPaymentMethod(achMethod, List.of(achAccount));
        assertEquals(1, workflow.getAccountsByPaymentMethod().size());

        workflow.clearAccountsByPaymentMethod();
        assertEquals(0, workflow.getAccountsByPaymentMethod().size());
    }

    @Test
    @DisplayName("remove accounts by payment method removes specific method")
    public void remove_accounts_by_payment_method_removes_specific_method() {
        workflow.initialize(usdBtcMarket);
        PaymentMethod<?> achMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER);
        PaymentMethod<?> advancedCashMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ADVANCED_CASH);
        Account<?, ?> achAccount = createAccount(achMethod);
        Account<?, ?> advancedCashAccount = createAccount(advancedCashMethod);

        workflow.putAccountsByPaymentMethod(achMethod, List.of(achAccount));
        workflow.putAccountsByPaymentMethod(advancedCashMethod, List.of(advancedCashAccount));
        assertEquals(2, workflow.getAccountsByPaymentMethod().size());

        workflow.removeAccountsByPaymentMethod(achMethod);
        assertEquals(1, workflow.getAccountsByPaymentMethod().size());
        assertTrue(workflow.getAccountsByPaymentMethod().containsKey(advancedCashMethod));
    }

    @Test
    @DisplayName("put all accounts by payment method replaces all accounts")
    public void put_all_accounts_by_payment_method_replaces_all_accounts() {
        workflow.initialize(usdBtcMarket);
        PaymentMethod<?> achMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER);
        Account<?, ?> achAccount = createAccount(achMethod);

        Map<PaymentMethod<?>, List<Account<?, ?>>> accountsMap = Map.of(achMethod, List.of(achAccount));
        workflow.putAllAccountsByPaymentMethod(accountsMap);

        assertEquals(1, workflow.getAccountsByPaymentMethod().size());
        assertEquals(List.of(achAccount), workflow.getAccountsByPaymentMethod().get(achMethod));
    }

    @Test
    @DisplayName("clear selected account by payment method removes all selected accounts")
    public void clear_selected_account_by_payment_method_removes_all_selected_accounts() {
        workflow.initialize(usdBtcMarket);
        PaymentMethod<?> achMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER);
        Account<?, ?> achAccount = createAccount(achMethod);

        workflow.putSelectedAccountByPaymentMethod(achMethod, achAccount);
        assertEquals(1, workflow.getSelectedAccountByPaymentMethod().size());

        workflow.clearSelectedAccountByPaymentMethod();
        assertEquals(0, workflow.getSelectedAccountByPaymentMethod().size());
    }

    @Test
    @DisplayName("put all selected account by payment method replaces all selected accounts")
    public void put_all_selected_account_by_payment_method_replaces_all_selected_accounts() {
        workflow.initialize(usdBtcMarket);
        PaymentMethod<?> achMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER);
        Account<?, ?> achAccount = createAccount(achMethod);

        Map<PaymentMethod<?>, Account<?, ?>> selectedAccountsMap = Map.of(achMethod, achAccount);
        workflow.putAllSelectedAccountByPaymentMethod(selectedAccountsMap);

        assertEquals(1, workflow.getSelectedAccountByPaymentMethod().size());
        assertEquals(achAccount, workflow.getSelectedAccountByPaymentMethod().get(achMethod));
    }

    @Test
    @DisplayName("set trade amount limits updates limits")
    public void set_trade_amount_limits_updates_limits() {
        workflow.initialize(usdBtcMarket);
        TradeAmountRange currentLimits = workflow.getTradeAmountLimits();

        TradeAmount doubledMax = TradeAmountConversion.toTradeAmount(usdBtcMarket,
                usdBtcPriceQuote,
                currentLimits.getMin().getQuoteSideAmount().multiply(2));
        TradeAmountRange newLimits = new TradeAmountRange(
                currentLimits.getMin(),
                doubledMax
        );
        workflow.setTradeAmountLimits(newLimits);

        assertEquals(newLimits, workflow.getTradeAmountLimits());
    }

    @Test
    @DisplayName("set user specific trade amount limit updates limit")
    public void set_user_specific_trade_amount_limit_updates_limit() {
        workflow.initialize(usdBtcMarket);
        TradeAmount customLimit = TradeAmountConversion.toTradeAmount(usdBtcMarket,
                usdBtcPriceQuote,
                Fiat.fromFaceValue(3000, "USD"));

        workflow.setUserSpecificTradeAmountLimit(Optional.of(customLimit));

        assertEquals(Optional.of(customLimit), workflow.getUserSpecificTradeAmountLimit());
    }

    @Test
    @DisplayName("set input amount limits updates limits")
    public void set_input_amount_limits_updates_limits() {
        workflow.initialize(usdBtcMarket);
        var currentLimits = workflow.getInputAmountLimits();

        var newLimits = new MonetaryRange(
                currentLimits.getMin(),
                currentLimits.getMin().multiply(1.5)
        );
        workflow.setInputAmountLimits(newLimits);

        assertEquals(newLimits, workflow.getInputAmountLimits());
    }

    @Test
    @DisplayName("set use range amount updates slider values")
    public void set_use_range_amount_updates_slider_values() {
        workflow.initialize(defaultMarket);
        workflow.setUseRangeAmount(true);

        assertTrue(workflow.getUseRangeAmount());
        assertNotNull(workflow.getMinAmountSliderValue());
        assertNotNull(workflow.getMaxAmountSliderValue());
        assertEquals(List.of(true), cookieStore.persistedUseRangeAmountValues);
    }

    private static class FakeMarketData implements CreateOfferDraftMarketData {
        private final Map<Market, PriceQuote> priceQuoteByMarket = new HashMap<>();
        private final Map<Market, TradeAmount> defaultTradeAmountByMarket = new HashMap<>();
        private final PriceQuote btcUsdPriceQuote;
        private int btcUsdPriceQuoteRequests;

        private FakeMarketData(PriceQuote btcUsdPriceQuote) {
            this.btcUsdPriceQuote = btcUsdPriceQuote;
        }

        private void put(Market market, PriceQuote priceQuote, TradeAmount defaultTradeAmount) {
            priceQuoteByMarket.put(market, priceQuote);
            defaultTradeAmountByMarket.put(market, defaultTradeAmount);
        }

        @Override
        public PriceQuote getMarketPriceQuote(Market market) {
            return Optional.ofNullable(priceQuoteByMarket.get(market))
                    .orElseThrow(() -> new IllegalStateException("Market price quote not available for " + market));
        }

        @Override
        public PriceQuote getBtcUsdPriceQuote() {
            btcUsdPriceQuoteRequests++;
            return btcUsdPriceQuote;
        }

        @Override
        public TradeAmount getTradeAmountFromUsd(Market market, Fiat usdAmount) {
            return Optional.ofNullable(defaultTradeAmountByMarket.get(market))
                    .orElseThrow(() -> new IllegalStateException("Default trade amount not available for " + market));
        }
    }

    private static class FakeCookieStore implements CreateOfferDraftCookieStore {
        private final Direction defaultDirection;
        private final boolean defaultUseBaseForFiatMarkets;
        private final boolean defaultUseBaseForOtherMarkets;
        private final boolean defaultUseRangeAmount;
        private final List<Direction> persistedDirections = new ArrayList<>();
        private final List<InputModePreference> persistedInputModes = new ArrayList<>();
        private final List<Boolean> persistedUseRangeAmountValues = new ArrayList<>();

        private FakeCookieStore(Direction defaultDirection,
                                boolean defaultUseBaseForFiatMarkets,
                                boolean defaultUseBaseForOtherMarkets,
                                boolean defaultUseRangeAmount) {
            this.defaultDirection = defaultDirection;
            this.defaultUseBaseForFiatMarkets = defaultUseBaseForFiatMarkets;
            this.defaultUseBaseForOtherMarkets = defaultUseBaseForOtherMarkets;
            this.defaultUseRangeAmount = defaultUseRangeAmount;
        }

        @Override
        public Direction getDirection() {
            return defaultDirection;
        }

        @Override
        public boolean getUseBaseCurrencyForAmountInput(Market market) {
            return market.isBtcFiatMarket() ? defaultUseBaseForFiatMarkets : defaultUseBaseForOtherMarkets;
        }

        @Override
        public boolean getUseRangeAmount() {
            return defaultUseRangeAmount;
        }

        @Override
        public void persistDirection(Direction direction) {
            persistedDirections.add(direction);
        }

        @Override
        public void persistUseBaseCurrencyForAmountInput(Market market, boolean useBaseCurrencyForAmountInput) {
            persistedInputModes.add(new InputModePreference(market, useBaseCurrencyForAmountInput));
        }

        @Override
        public void persistUseRangeAmount(boolean useRangeAmount) {
            persistedUseRangeAmountValues.add(useRangeAmount);
        }
    }

    private static class FakeAccountsProvider implements AccountsProvider {
        private final List<Market> requestedMarkets = new ArrayList<>();

        @Override
        public List<Account<?, ?>> findAccountsForMarket(Market market) {
            requestedMarkets.add(market);
            return List.of();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Account<?, ?> createAccount(PaymentMethod<?> paymentMethod) {
        Account account = mock(Account.class);
        when(account.getPaymentMethod()).thenReturn(paymentMethod);
        return account;
    }

    private record InputModePreference(Market market, boolean useBaseCurrencyForAmountInput) {
    }
}
