package bisq.offer.mu_sig.draft;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.offer.mu_sig.draft.dependencies.AccountsProvider;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PaymentMethodSelectionServiceTest {

    @Test
    public void loadAccountsForMarketGroupsByPaymentMethod() {
        Market market = MarketRepository.getUSDBitcoinMarket();
        PaymentMethod<?> achMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER);
        PaymentMethod<?> advancedCashMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ADVANCED_CASH);
        Account<?, ?> achAccount1 = createAccount(achMethod);
        Account<?, ?> achAccount2 = createAccount(achMethod);
        Account<?, ?> advancedCashAccount = createAccount(advancedCashMethod);

        FakeAccountsProvider accountsProvider = new FakeAccountsProvider();
        accountsProvider.put(market, List.of(achAccount1, achAccount2, advancedCashAccount));
        PaymentMethodSelectionService service = new PaymentMethodSelectionService(accountsProvider);

        PaymentMethodSelectionService.MarketAccounts marketAccounts = service.loadAccountsForMarket(market);

        assertEquals(List.of(achAccount1, achAccount2, advancedCashAccount), marketAccounts.accountsForMarket());
        assertEquals(List.of(achAccount1, achAccount2), marketAccounts.accountsByPaymentMethod().get(achMethod));
        assertEquals(List.of(advancedCashAccount), marketAccounts.accountsByPaymentMethod().get(advancedCashMethod));
    }

    @Test
    public void findSelectedPaymentMethodsToRemoveReturnsMissingAccounts() {
        PaymentMethod<?> achMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER);
        PaymentMethod<?> advancedCashMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ADVANCED_CASH);
        Account<?, ?> achAccount = createAccount(achMethod);
        Account<?, ?> advancedCashAccount = createAccount(advancedCashMethod);

        PaymentMethodSelectionService service = new PaymentMethodSelectionService(market -> List.of());
        ImmutableMap<PaymentMethod<?>, Account<?, ?>> selectedAccounts = ImmutableMap.of(
                achMethod, achAccount,
                advancedCashMethod, advancedCashAccount);

        List<? extends PaymentMethod<?>> paymentMethodsToRemove = service.findSelectedPaymentMethodsToRemove(selectedAccounts,
                List.of(achAccount));

        assertEquals(List.of(advancedCashMethod), paymentMethodsToRemove);
    }

    @Test
    public void findAccountToAutoSelectReturnsSingleUnselectedAccount() {
        PaymentMethod<?> achMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER);
        Account<?, ?> achAccount = createAccount(achMethod);
        PaymentMethodSelectionService service = new PaymentMethodSelectionService(market -> List.of());

        assertSame(achAccount, service.findAccountToAutoSelect(List.of(achAccount), ImmutableMap.of()).orElseThrow());
        assertTrue(service.findAccountToAutoSelect(List.of(achAccount), ImmutableMap.of(achMethod, achAccount)).isEmpty());
    }

    @Test
    public void findMostRestrictiveSelectedPaymentRailReturnsLowestLimitRail() {
        PaymentMethod<?> veryLowRiskMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ADVANCED_CASH);
        PaymentMethod<?> moderateRiskMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER);
        Account<?, ?> veryLowRiskAccount = createAccount(veryLowRiskMethod);
        Account<?, ?> moderateRiskAccount = createAccount(moderateRiskMethod);

        PaymentMethodSelectionService service = new PaymentMethodSelectionService(market -> List.of());

        assertEquals(FiatPaymentRail.ACH_TRANSFER, service.findMostRestrictiveSelectedPaymentRail(ImmutableMap.of(
                veryLowRiskMethod, veryLowRiskAccount,
                moderateRiskMethod, moderateRiskAccount)));
    }

    private static class FakeAccountsProvider implements AccountsProvider {
        private final Map<Market, List<Account<?, ?>>> accountsByMarket = new HashMap<>();

        private void put(Market market, List<Account<?, ?>> accounts) {
            accountsByMarket.put(market, accounts);
        }

        @Override
        public List<Account<?, ?>> findAccountsForMarket(Market market) {
            return accountsByMarket.getOrDefault(market, List.of());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Account<?, ?> createAccount(PaymentMethod<?> paymentMethod) {
        Account account = mock(Account.class);
        when(account.getPaymentMethod()).thenReturn(paymentMethod);
        return account;
    }
}
