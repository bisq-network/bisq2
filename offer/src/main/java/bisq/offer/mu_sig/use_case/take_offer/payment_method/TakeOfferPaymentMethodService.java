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
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentRail;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.mu_sig.use_case.create_offer.payment_method.PaymentMethodAccountSelection;
import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class TakeOfferPaymentMethodService {
    @Getter(AccessLevel.PACKAGE)
    @Delegate
    private final TakeOfferPaymentMethodModel model;
    private final PaymentMethodSelectionService paymentMethodSelectionService;
    // Invoked AFTER a selection mutation completed, so a recomputation never observes the
    // transient empty state of the clear-before-put sequence.
    @Setter
    private Runnable tradeAmountConstraintsRecalculationHandler = () -> {
    };

    public enum PaymentMethodSelectionStatus {
        NO_ACCOUNT_AVAILABLE,
        SINGLE_ACCOUNT_SELECTED,
        ACCOUNT_SELECTION_REQUIRED
    }

    public record PaymentMethodSelectionResult(PaymentMethodSelectionStatus status,
                                               List<Account<?, ?>> accountsRequiringSelection) {
        public PaymentMethodSelectionResult {
            checkNotNull(status, "status must not be null");
            checkNotNull(accountsRequiringSelection, "accountsRequiringSelection must not be null");
            accountsRequiringSelection = List.copyOf(accountsRequiringSelection);
        }

        public static PaymentMethodSelectionResult noAccountAvailable() {
            return new PaymentMethodSelectionResult(PaymentMethodSelectionStatus.NO_ACCOUNT_AVAILABLE, List.of());
        }

        public static PaymentMethodSelectionResult singleAccountSelected() {
            return new PaymentMethodSelectionResult(PaymentMethodSelectionStatus.SINGLE_ACCOUNT_SELECTED, List.of());
        }

        public static PaymentMethodSelectionResult accountSelectionRequired(List<Account<?, ?>> accountsRequiringSelection) {
            checkNotNull(accountsRequiringSelection, "accountsRequiringSelection must not be null");
            return new PaymentMethodSelectionResult(PaymentMethodSelectionStatus.ACCOUNT_SELECTION_REQUIRED, accountsRequiringSelection);
        }
    }

    public TakeOfferPaymentMethodService(PaymentMethodSelectionService paymentMethodSelectionService) {
        this.model = new TakeOfferPaymentMethodModel();
        this.paymentMethodSelectionService = checkNotNull(paymentMethodSelectionService, "paymentMethodSelectionService must not be null");
    }

    public void putAccountsByPaymentMethod(PaymentMethod<?> paymentMethod, List<Account<?, ?>> account) {
        checkNotNull(paymentMethod, "paymentMethod must not be null");
        checkNotNull(account, "account must not be null");
        model.putAccountsByPaymentMethod(paymentMethod, account);
    }

    public void removeAccountsByPaymentMethod(PaymentMethod<?> paymentMethod) {
        model.removeAccountsByPaymentMethod(paymentMethod);
    }

    public void putAllAccountsByPaymentMethod(Map<PaymentMethod<?>, List<Account<?, ?>>> selectedAccountByPaymentMethod) {
        checkNotNull(selectedAccountByPaymentMethod, "selectedAccountByPaymentMethod must not be null");
        model.putAllAccountsByPaymentMethod(selectedAccountByPaymentMethod);
    }

    public void clearAccountsByPaymentMethod() {
        model.clearAccountsByPaymentMethod();
    }

    public void putSelectedAccountByPaymentMethod(PaymentMethod<?> paymentMethod, Account<?, ?> account) {
        checkNotNull(paymentMethod, "paymentMethod must not be null");
        checkNotNull(account, "account must not be null");
        putSelectedAccountByPaymentMethod(paymentMethod, account, true);
    }

    public void removeSelectedAccountByPaymentMethod(PaymentMethod<?> paymentMethod) {
        removeSelectedAccountByPaymentMethod(paymentMethod, true);
    }

    public void clearSelectedAccountByPaymentMethod() {
        clearSelectedAccountByPaymentMethod(true);
    }

    public PaymentMethodSelectionResult onPaymentMethodSelected(PaymentMethod<?> paymentMethod) {
        checkNotNull(paymentMethod, "paymentMethod must not be null");

        PaymentMethodAccountSelection selection = paymentMethodSelectionService.findAccountsSelection(
                getAccountsByPaymentMethod(),
                paymentMethod);
        if (selection.accountToAutoSelect().isPresent()) {
            putSelectedAccountByPaymentMethod(paymentMethod, selection.accountToAutoSelect().get());
            return PaymentMethodSelectionResult.singleAccountSelected();
        }

        if (!selection.accountsRequiringSelection().isEmpty()) {
            return PaymentMethodSelectionResult.accountSelectionRequired(selection.accountsRequiringSelection());
        }

        return PaymentMethodSelectionResult.noAccountAvailable();
    }

    /* --------------------------------------------------------------------- */
    // Package scope helpers used by workflow/state engine callbacks
    /* --------------------------------------------------------------------- */

    public void updatePaymentMethods(MuSigOffer offer) {
        // Called once per take process at initialization; selection state from a previous
        // offer must not survive into a new one.
        clearSelectedAccountByPaymentMethod(false);
        model.setTakerSidePaymentMethodSpecs(getTakerSidePaymentMethodSpecs(offer));
        OfferAccounts offerAccounts = paymentMethodSelectionService.loadAccountsForOffer(offer);
        List<Account<?, ?>> accountsForMarket = offerAccounts.accountsForMarket();
        Map<PaymentMethod<?>, List<Account<?, ?>>> map = offerAccounts.accountsByPaymentMethod();
        // Refreshed unconditionally: two offers can both yield zero eligible accounts (equal maps)
        // while restricting differently, so the reasons must never survive an update.
        model.setIncompatibleAccountsByPaymentMethod(offerAccounts.incompatibleAccountsByPaymentMethod());
        if (!getAccountsByPaymentMethod().equals(map)) {
            clearAccountsByPaymentMethod();
            putAllAccountsByPaymentMethod(map);
        }

        // Remove payment methods which are not present in the eligible accounts
        ImmutableMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod = getSelectedAccountByPaymentMethod();
        List<? extends PaymentMethod<?>> paymentMethodsToRemove = paymentMethodSelectionService.findSelectedPaymentMethodsToRemove(selectedAccountByPaymentMethod,
                accountsForMarket);
        paymentMethodsToRemove.forEach(paymentMethod -> removeSelectedAccountByPaymentMethod(paymentMethod, false));

        // If we have only one, we pre-select
        paymentMethodSelectionService.findAccountToAutoSelect(accountsForMarket, getSelectedAccountByPaymentMethod())
                .ifPresent(account -> putSelectedAccountByPaymentMethod(account.getPaymentMethod(), account, false));
    }


    private static List<PaymentMethodSpec<?>> getTakerSidePaymentMethodSpecs(MuSigOffer offer) {
        return offer.getMarket().isBaseCurrencyBitcoin()
                ? offer.getQuoteSidePaymentMethodSpecs()
                : offer.getBaseSidePaymentMethodSpecs();
    }

    public void reset() {
        model.setTakerSidePaymentMethodSpecs(List.of());
        model.setIncompatibleAccountsByPaymentMethod(Map.of());
        clearAccountsByPaymentMethod();
        clearSelectedAccountByPaymentMethod(false);
    }

    public PaymentRail getSelectedPaymentRail() {
        return paymentMethodSelectionService.findMostRestrictiveSelectedPaymentRail(getSelectedAccountByPaymentMethod());
    }

    private boolean putSelectedAccountByPaymentMethod(PaymentMethod<?> paymentMethod,
                                                      Account<?, ?> account,
                                                      boolean recalculateTradeAmountConstraints) {
        checkNotNull(paymentMethod, "paymentMethod must not be null");
        checkNotNull(account, "account must not be null");
        // The use case is the enforcement point for a consistent selection: getSelectedPaymentMethodSpec()
        // derives the spec from the map key while getSelectedAccount() returns the value, so the pair must
        // agree. The eligible map is built only from offered, compatible accounts, so requiring the account
        // to be in it also guarantees the method is offered.
        checkArgument(paymentMethod.equals(account.getPaymentMethod()),
                "account payment method must match the selected payment method. paymentMethod=%s; account.paymentMethod=%s",
                paymentMethod, account.getPaymentMethod());
        checkArgument(getAccountsByPaymentMethod().getOrDefault(paymentMethod, List.of()).contains(account),
                "account must be one of the eligible accounts for the payment method. paymentMethod=%s",
                paymentMethod);
        // The take flow holds at most one selection, enforced here so every caller inherits it.
        ImmutableMap<PaymentMethod<?>, Account<?, ?>> existing = getSelectedAccountByPaymentMethod();
        if (existing.size() == 1 && account.equals(existing.get(paymentMethod))) {
            return false;
        }
        model.clearSelectedAccountByPaymentMethod();
        model.putSelectedAccountByPaymentMethod(paymentMethod, account);
        if (recalculateTradeAmountConstraints) {
            tradeAmountConstraintsRecalculationHandler.run();
        }
        return true;
    }

    private boolean removeSelectedAccountByPaymentMethod(PaymentMethod<?> paymentMethod,
                                                         boolean recalculateTradeAmountConstraints) {
        if (!getSelectedAccountByPaymentMethod().containsKey(paymentMethod)) {
            return false;
        }
        model.removeSelectedAccountByPaymentMethod(paymentMethod);
        if (recalculateTradeAmountConstraints) {
            tradeAmountConstraintsRecalculationHandler.run();
        }
        return true;
    }

    private boolean clearSelectedAccountByPaymentMethod(boolean recalculateTradeAmountConstraints) {
        if (getSelectedAccountByPaymentMethod().isEmpty()) {
            return false;
        }
        model.clearSelectedAccountByPaymentMethod();
        if (recalculateTradeAmountConstraints) {
            tradeAmountConstraintsRecalculationHandler.run();
        }
        return true;
    }
}
