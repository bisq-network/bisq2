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

package bisq.offer.mu_sig.use_case.take_offer.payment_method;

import bisq.account.accounts.Account;
import bisq.account.accounts.util.AccountUtils;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentRail;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.AccountOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.mu_sig.use_case.create_offer.amount.limits.PaymentMethodBasedAmountLimitsProvider;
import bisq.offer.mu_sig.use_case.create_offer.payment_method.PaymentMethodAccountSelection;
import bisq.offer.mu_sig.use_case.dependencies.AccountsProvider;
import com.google.common.collect.ImmutableMap;

import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Encapsulates payment-method/account selection rules for a market.
 * <p>
 * Design: keeps all account eligibility, grouping, stale-selection cleanup, and rail-restriction
 * lookups in one pure service so workflow orchestration stays deterministic and readable.
 */
public class PaymentMethodSelectionService {
    private final AccountsProvider accountsProvider;

    public PaymentMethodSelectionService(AccountsProvider accountsProvider) {
        this.accountsProvider = checkNotNull(accountsProvider, "accountsProvider must not be null");
    }

    /* --------------------------------------------------------------------- */
    // Account loading and grouping
    /* --------------------------------------------------------------------- */

    public OfferAccounts loadAccountsForOffer(MuSigOffer offer) {
        checkNotNull(offer, "offer must not be null");
        List<Account<?, ?>> accountsForMarket = checkNotNull(accountsProvider.findAccountsForMarket(offer.getMarket()),
                "accountsForMarket must not be null");
        Map<PaymentMethod<?>, AccountOption> accountOptionByPaymentMethod =
                OfferOptionUtil.findAccountOptions(offer.getOfferOptions()).stream()
                        .collect(Collectors.toMap(AccountOption::getPaymentMethod, accountOption -> accountOption,
                                (first, second) -> first));
        List<PaymentMethodSpec<?>> takerSideSpecs = offer.getMarket().isBaseCurrencyBitcoin()
                ? offer.getQuoteSidePaymentMethodSpecs()
                : offer.getBaseSidePaymentMethodSpecs();
        Set<PaymentMethod<?>> offeredPaymentMethods = takerSideSpecs.stream()
                .map(spec -> (PaymentMethod<?>) spec.getPaymentMethod())
                .collect(Collectors.toSet());
        // The trust-boundary validation rejects AccountOptions for non-offered methods; the
        // offered-method check here keeps this service safe also for unvalidated input.
        List<Account<?, ?>> eligibleAccounts = new ArrayList<>();
        Map<PaymentMethod<?>, List<AccountCompatibilityMismatch>> incompatibleAccountsByPaymentMethod = new HashMap<>();
        for (Account<?, ?> account : accountsForMarket) {
            if (!offeredPaymentMethods.contains(account.getPaymentMethod())) {
                continue;
            }
            AccountOption accountOption = accountOptionByPaymentMethod.get(account.getPaymentMethod());
            if (accountOption == null) {
                continue;
            }
            List<AccountCompatibilityMismatch> mismatches = findIncompatibilities(account, accountOption);
            if (mismatches.isEmpty()) {
                eligibleAccounts.add(account);
            } else {
                incompatibleAccountsByPaymentMethod
                        .computeIfAbsent(account.getPaymentMethod(), key -> new ArrayList<>())
                        .addAll(mismatches);
            }
        }
        Map<PaymentMethod<?>, List<Account<?, ?>>> accountsByPaymentMethod = eligibleAccounts.stream()
                .collect(Collectors.groupingBy(Account::getPaymentMethod, Collectors.toList()));
        return new OfferAccounts(List.copyOf(eligibleAccounts), accountsByPaymentMethod, incompatibleAccountsByPaymentMethod);
    }

    // A compatibility dimension applies only when the offer's AccountOption carries entries for
    // it; an empty list imposes no restriction (non-country and non-bank payment methods store
    // empty lists by construction).
    static List<AccountCompatibilityMismatch> findIncompatibilities(Account<?, ?> account, AccountOption accountOption) {
        List<AccountCompatibilityMismatch> mismatches = new ArrayList<>();
        List<String> acceptedCountryCodes = accountOption.getAcceptedCountryCodes();
        if (!acceptedCountryCodes.isEmpty()) {
            Optional<String> countryCode = AccountUtils.getCountryCode(account.getAccountPayload());
            if (countryCode.isEmpty() || !acceptedCountryCodes.contains(countryCode.get())) {
                mismatches.add(new AccountCompatibilityMismatch(account,
                        AccountCompatibilityMismatch.Dimension.COUNTRY,
                        countryCode,
                        acceptedCountryCodes));
            }
        }
        List<String> acceptedBanks = accountOption.getAcceptedBanks();
        if (!acceptedBanks.isEmpty()) {
            Optional<String> bankId = AccountUtils.getBankId(account.getAccountPayload());
            if (bankId.isEmpty() || !acceptedBanks.contains(bankId.get())) {
                mismatches.add(new AccountCompatibilityMismatch(account,
                        AccountCompatibilityMismatch.Dimension.BANK,
                        bankId,
                        acceptedBanks));
            }
        }
        return mismatches;
    }

    /* --------------------------------------------------------------------- */
    // Selection
    /* --------------------------------------------------------------------- */

    public List<? extends PaymentMethod<?>> findSelectedPaymentMethodsToRemove(ImmutableMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod,
                                                                               List<Account<?, ?>> accountsForMarket) {
        checkNotNull(selectedAccountByPaymentMethod, "selectedAccountByPaymentMethod must not be null");
        checkNotNull(accountsForMarket, "accountsForMarket must not be null");
        return selectedAccountByPaymentMethod.entrySet().stream()
                .filter(entry -> !accountsForMarket.contains(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

    public Optional<Account<?, ?>> findAccountToAutoSelect(List<Account<?, ?>> accountsForMarket,
                                                           ImmutableMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod) {
        checkNotNull(accountsForMarket, "accountsForMarket must not be null");
        checkNotNull(selectedAccountByPaymentMethod, "selectedAccountByPaymentMethod must not be null");

        if (accountsForMarket.size() != 1) {
            return Optional.empty();
        }

        Account<?, ?> account = accountsForMarket.getFirst();
        Account<?, ?> existing = selectedAccountByPaymentMethod.get(account.getPaymentMethod());
        return account.equals(existing) ? Optional.empty() : Optional.of(account);
    }

    public PaymentMethodAccountSelection findAccountsSelection(Map<PaymentMethod<?>, List<Account<?, ?>>> accountsByPaymentMethod,
                                                               PaymentMethod<?> paymentMethod) {
        checkNotNull(accountsByPaymentMethod, "accountsByPaymentMethod must not be null");
        checkNotNull(paymentMethod, "paymentMethod must not be null");

        List<Account<?, ?>> accountsForPaymentMethod = accountsByPaymentMethod.get(paymentMethod);
        if (accountsForPaymentMethod == null || accountsForPaymentMethod.isEmpty()) {
            return PaymentMethodAccountSelection.noAccount();
        }

        if (accountsForPaymentMethod.size() == 1) {
            return PaymentMethodAccountSelection.singleAccount(accountsForPaymentMethod.getFirst());
        }

        return PaymentMethodAccountSelection.multipleAccounts(accountsForPaymentMethod);
    }

    /* --------------------------------------------------------------------- */
    // Restriction lookup
    /* --------------------------------------------------------------------- */

    public PaymentRail findMostRestrictiveSelectedPaymentRail(ImmutableMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod) {
        checkNotNull(selectedAccountByPaymentMethod, "selectedAccountByPaymentMethod must not be null");
        return selectedAccountByPaymentMethod.values().stream()
                .map(Account::getPaymentMethod)
                .map(PaymentMethod::getPaymentRail)
                .min(Comparator.comparing(PaymentMethodBasedAmountLimitsProvider::evaluateLimitInUsd))
                .orElse(null);
    }

}
