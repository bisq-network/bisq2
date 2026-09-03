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

package bisq.offer.mu_sig.use_case.create_offer.payment_method;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.observable.map.ReadOnlyObservableMap;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

public class CreateOfferPaymentMethodModel {
    private final ObservableHashMap<PaymentMethod<?>, List<Account<?, ?>>> accountsByPaymentMethod = new ObservableHashMap<>();
    private final ObservableHashMap<PaymentMethod<?>, Account<?, ?>> accountByPaymentMethod = new ObservableHashMap<>();


    //Fiat paymentRailBasedTradeLimitInUsd = MuSigTradeAmountLimits.getMaxTradeLimitInUsd(paymentRail);
    public CreateOfferPaymentMethodModel() {
    }



    /* --------------------------------------------------------------------- */
    // accountsByPaymentMethod
    /* --------------------------------------------------------------------- */

    void clearAccountsByPaymentMethod() {
        accountsByPaymentMethod.clear();
    }

    void putAccountsByPaymentMethod(PaymentMethod<?> paymentMethod, List<Account<?, ?>> account) {
        accountsByPaymentMethod.put(paymentMethod, account);
    }

    void removeAccountsByPaymentMethod(PaymentMethod<?> paymentMethod) {
        accountsByPaymentMethod.remove(paymentMethod);
    }

    void putAllAccountsByPaymentMethod(Map<PaymentMethod<?>, List<Account<?, ?>>> accountsByPaymentMethod) {
        this.accountsByPaymentMethod.putAll(accountsByPaymentMethod);
    }

    public ReadOnlyObservableMap<PaymentMethod<?>, List<Account<?, ?>>> accountsByPaymentMethodObservable() {
        return accountsByPaymentMethod;
    }

    public ImmutableMap<PaymentMethod<?>, List<Account<?, ?>>> getAccountsByPaymentMethod() {
        return ImmutableMap.copyOf(accountsByPaymentMethod);
    }

    /* --------------------------------------------------------------------- */
    // selectedAccountByPaymentMethod
    /* --------------------------------------------------------------------- */

    void clearAccountByPaymentMethod() {
        accountByPaymentMethod.clear();
    }

    void addAccountByPaymentMethodEntry(Map.Entry<PaymentMethod<?>, Account<?, ?>> entry) {
        accountByPaymentMethod.put(entry.getKey(), entry.getValue());
    }

    void removeAccountByPaymentMethodEntry(Map.Entry<PaymentMethod<?>, Account<?, ?>> entry) {
        accountByPaymentMethod.remove(entry.getKey(), entry.getValue());
    }

    void putAccountByPaymentMethod(PaymentMethod<?> paymentMethod, Account<?, ?> account) {
        accountByPaymentMethod.put(paymentMethod, account);
    }

    void removeAccountByPaymentMethod(PaymentMethod<?> paymentMethod) {
        accountByPaymentMethod.remove(paymentMethod);
    }

    void putAllAccountByPaymentMethod(Map<PaymentMethod<?>, Account<?, ?>> map) {
        this.accountByPaymentMethod.putAll(map);
    }

    public ReadOnlyObservableMap<PaymentMethod<?>, Account<?, ?>> accountByPaymentMethodObservable() {
        return accountByPaymentMethod;
    }

    public ImmutableMap<PaymentMethod<?>, Account<?, ?>> getAccountByPaymentMethod() {
        return ImmutableMap.copyOf(accountByPaymentMethod);
    }
}
