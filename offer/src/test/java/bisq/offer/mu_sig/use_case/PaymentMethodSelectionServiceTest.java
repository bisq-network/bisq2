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

package bisq.offer.mu_sig.use_case;

import bisq.account.accounts.Account;
import bisq.account.accounts.fiat.CountryBasedAccountPayload;
import bisq.account.accounts.fiat.BankAccountPayload;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.common.locale.Country;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.mu_sig.use_case.take_offer.payment_method.AccountCompatibilityMismatch;
import bisq.offer.mu_sig.use_case.take_offer.payment_method.OfferAccounts;
import bisq.offer.options.AccountOption;
import bisq.offer.options.OfferOption;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.offer.mu_sig.use_case.create_offer.payment_method.MarketAccounts;
import bisq.offer.mu_sig.use_case.dependencies.AccountsProvider;
import bisq.offer.mu_sig.use_case.take_offer.payment_method.PaymentMethodSelectionService;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Optional;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PaymentMethodSelectionServiceTest {

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

    @Test
    public void loadAccountsForOfferKeepsOnlyOfferedMethodsWithCompatibleAccounts() {
        Market market = MarketRepository.getUSDBitcoinMarket();
        PaymentMethod<?> wiseMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.WISE);
        PaymentMethod<?> sepaMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA);
        PaymentMethod<?> achMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER);
        Account<?, ?> wiseAccount = createAccount(wiseMethod);
        Account<?, ?> sepaAccountDe = createCountryAccount(sepaMethod, "DE");
        Account<?, ?> sepaAccountFr = createCountryAccount(sepaMethod, "FR");
        Account<?, ?> achAccount = createAccount(achMethod);

        FakeAccountsProvider accountsProvider = new FakeAccountsProvider();
        accountsProvider.put(market, List.of(wiseAccount, sepaAccountDe, sepaAccountFr, achAccount));
        PaymentMethodSelectionService service = new PaymentMethodSelectionService(accountsProvider);

        MuSigOffer offer = offerWith(market,
                List.of(specOf(wiseMethod), specOf(sepaMethod)),
                List.of(accountOption(wiseMethod, List.of(), List.of()),
                        accountOption(sepaMethod, List.of("DE"), List.of())));

        OfferAccounts marketAccounts = service.loadAccountsForOffer(offer);

        assertEquals(List.of(wiseAccount, sepaAccountDe), marketAccounts.accountsForMarket());
        assertEquals(List.of(wiseAccount), marketAccounts.accountsByPaymentMethod().get(wiseMethod));
        assertEquals(List.of(sepaAccountDe), marketAccounts.accountsByPaymentMethod().get(sepaMethod));
    }

    @Test
    public void accountWithoutCountryIsNotEligibleWhenOfferRestrictsCountries() {
        Market market = MarketRepository.getUSDBitcoinMarket();
        PaymentMethod<?> sepaMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA);
        Account<?, ?> accountWithoutCountry = createAccount(sepaMethod);

        FakeAccountsProvider accountsProvider = new FakeAccountsProvider();
        accountsProvider.put(market, List.of(accountWithoutCountry));
        PaymentMethodSelectionService service = new PaymentMethodSelectionService(accountsProvider);

        MuSigOffer offer = offerWith(market,
                List.of(specOf(sepaMethod)),
                List.of(accountOption(sepaMethod, List.of("DE"), List.of())));

        assertTrue(service.loadAccountsForOffer(offer).accountsForMarket().isEmpty());
    }

    @Test
    public void bankRestrictionFiltersAccountsOfOtherBanks() {
        Market market = MarketRepository.getUSDBitcoinMarket();
        PaymentMethod<?> bankMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK);
        Account<?, ?> pskAccount = createBankAccount(bankMethod, "PSK");
        Account<?, ?> raikaAccount = createBankAccount(bankMethod, "Raika");

        FakeAccountsProvider accountsProvider = new FakeAccountsProvider();
        accountsProvider.put(market, List.of(pskAccount, raikaAccount));
        PaymentMethodSelectionService service = new PaymentMethodSelectionService(accountsProvider);

        MuSigOffer offer = offerWith(market,
                List.of(specOf(bankMethod)),
                List.of(accountOption(bankMethod, List.of(), List.of("PSK"))));

        assertEquals(List.of(pskAccount), service.loadAccountsForOffer(offer).accountsForMarket());
    }

    @Test
    public void accountOptionForNonOfferedMethodDoesNotMakeItsAccountsEligible() {
        Market market = MarketRepository.getUSDBitcoinMarket();
        PaymentMethod<?> wiseMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.WISE);
        PaymentMethod<?> achMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER);
        Account<?, ?> achAccount = createAccount(achMethod);

        FakeAccountsProvider accountsProvider = new FakeAccountsProvider();
        accountsProvider.put(market, List.of(achAccount));
        PaymentMethodSelectionService service = new PaymentMethodSelectionService(accountsProvider);

        MuSigOffer offer = offerWith(market,
                List.of(specOf(wiseMethod)),
                List.of(accountOption(wiseMethod, List.of(), List.of()),
                        accountOption(achMethod, List.of(), List.of())));

        assertTrue(service.loadAccountsForOffer(offer).accountsForMarket().isEmpty());
    }

    @Test
    public void incompatibleAccountsAreRecordedWithTheirMismatch() {
        Market market = MarketRepository.getUSDBitcoinMarket();
        PaymentMethod<?> sepaMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA);
        Account<?, ?> sepaAccountFr = createCountryAccount(sepaMethod, "FR");

        FakeAccountsProvider accountsProvider = new FakeAccountsProvider();
        accountsProvider.put(market, List.of(sepaAccountFr));
        PaymentMethodSelectionService service = new PaymentMethodSelectionService(accountsProvider);

        MuSigOffer offer = offerWith(market,
                List.of(specOf(sepaMethod)),
                List.of(accountOption(sepaMethod, List.of("DE"), List.of())));

        OfferAccounts offerAccounts = service.loadAccountsForOffer(offer);

        assertTrue(offerAccounts.accountsForMarket().isEmpty());
        List<AccountCompatibilityMismatch> mismatches =
                offerAccounts.incompatibleAccountsByPaymentMethod().get(sepaMethod);
        assertEquals(1, mismatches.size());
        AccountCompatibilityMismatch mismatch = mismatches.get(0);
        assertSame(sepaAccountFr, mismatch.account());
        assertEquals(AccountCompatibilityMismatch.Dimension.COUNTRY, mismatch.dimension());
        assertEquals("FR", mismatch.accountValue().orElseThrow());
        assertEquals(List.of("DE"), mismatch.acceptedValues());
    }

    @Test
    public void bankMismatchAndMissingValueAreRecorded() {
        Market market = MarketRepository.getUSDBitcoinMarket();
        PaymentMethod<?> bankMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK);
        Account<?, ?> raikaAccount = createBankAccount(bankMethod, "Raika");
        Account<?, ?> accountWithoutBank = createAccount(bankMethod);

        FakeAccountsProvider accountsProvider = new FakeAccountsProvider();
        accountsProvider.put(market, List.of(raikaAccount, accountWithoutBank));
        PaymentMethodSelectionService service = new PaymentMethodSelectionService(accountsProvider);

        MuSigOffer offer = offerWith(market,
                List.of(specOf(bankMethod)),
                List.of(accountOption(bankMethod, List.of(), List.of("PSK"))));

        List<AccountCompatibilityMismatch> mismatches =
                service.loadAccountsForOffer(offer).incompatibleAccountsByPaymentMethod().get(bankMethod);
        assertEquals(2, mismatches.size());
        AccountCompatibilityMismatch raikaMismatch = mismatches.get(0);
        assertSame(raikaAccount, raikaMismatch.account());
        assertEquals(AccountCompatibilityMismatch.Dimension.BANK, raikaMismatch.dimension());
        assertEquals("Raika", raikaMismatch.accountValue().orElseThrow());
        assertEquals(List.of("PSK"), raikaMismatch.acceptedValues());
        AccountCompatibilityMismatch missingBankMismatch = mismatches.get(1);
        assertSame(accountWithoutBank, missingBankMismatch.account());
        assertEquals(AccountCompatibilityMismatch.Dimension.BANK, missingBankMismatch.dimension());
        assertTrue(missingBankMismatch.accountValue().isEmpty());
        assertEquals(List.of("PSK"), missingBankMismatch.acceptedValues());
    }

    @Test
    public void compatibleAndNonOfferedAccountsRecordNoMismatch() {
        Market market = MarketRepository.getUSDBitcoinMarket();
        PaymentMethod<?> wiseMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.WISE);
        PaymentMethod<?> achMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER);
        Account<?, ?> wiseAccount = createAccount(wiseMethod);
        Account<?, ?> achAccount = createAccount(achMethod);

        FakeAccountsProvider accountsProvider = new FakeAccountsProvider();
        accountsProvider.put(market, List.of(wiseAccount, achAccount));
        PaymentMethodSelectionService service = new PaymentMethodSelectionService(accountsProvider);

        MuSigOffer offer = offerWith(market,
                List.of(specOf(wiseMethod)),
                List.of(accountOption(wiseMethod, List.of(), List.of())));

        OfferAccounts offerAccounts = service.loadAccountsForOffer(offer);

        assertEquals(List.of(wiseAccount), offerAccounts.accountsForMarket());
        assertTrue(offerAccounts.incompatibleAccountsByPaymentMethod().isEmpty());
    }

    private static MuSigOffer offerWith(Market market,
                                        List<PaymentMethodSpec<?>> quoteSideSpecs,
                                        List<OfferOption> offerOptions) {
        MuSigOffer offer = mock(MuSigOffer.class);
        when(offer.getMarket()).thenReturn(market);
        when(offer.getQuoteSidePaymentMethodSpecs()).thenReturn(quoteSideSpecs);
        when(offer.getOfferOptions()).thenReturn(offerOptions);
        return offer;
    }

    private static PaymentMethodSpec<?> specOf(PaymentMethod<?> paymentMethod) {
        PaymentMethodSpec<?> spec = mock(PaymentMethodSpec.class);
        when(spec.getPaymentMethod()).thenAnswer(invocation -> paymentMethod);
        return spec;
    }

    private static OfferOption accountOption(PaymentMethod<?> paymentMethod,
                                             List<String> acceptedCountryCodes,
                                             List<String> acceptedBanks) {
        AccountOption accountOption = mock(AccountOption.class);
        when(accountOption.getPaymentMethod()).thenAnswer(invocation -> paymentMethod);
        when(accountOption.getAcceptedCountryCodes()).thenReturn(acceptedCountryCodes);
        when(accountOption.getAcceptedBanks()).thenReturn(acceptedBanks);
        return accountOption;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Account<?, ?> createCountryAccount(PaymentMethod<?> paymentMethod, String countryCode) {
        Country country = mock(Country.class);
        when(country.getCode()).thenReturn(countryCode);
        CountryBasedAccountPayload payload = mock(CountryBasedAccountPayload.class);
        when(payload.getCountry()).thenReturn(country);
        Account account = mock(Account.class);
        when(account.getPaymentMethod()).thenReturn(paymentMethod);
        when(account.getAccountPayload()).thenAnswer(invocation -> payload);
        return account;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Account<?, ?> createBankAccount(PaymentMethod<?> paymentMethod, String bankId) {
        BankAccountPayload payload = mock(BankAccountPayload.class);
        when(payload.getBankId()).thenReturn(Optional.of(bankId));
        Account account = mock(Account.class);
        when(account.getPaymentMethod()).thenReturn(paymentMethod);
        when(account.getAccountPayload()).thenAnswer(invocation -> payload);
        return account;
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
