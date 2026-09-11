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
import bisq.common.observable.collection.ObservableArray;
import bisq.common.observable.collection.ReadOnlyObservableArray;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.observable.map.ReadOnlyObservableMap;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TakeOfferPaymentMethodModel implements TakeOfferPaymentMethodReadOnlyModel {
    protected final ObservableHashMap<PaymentMethod<?>, List<Account<?, ?>>> accountsByPaymentMethod = new ObservableHashMap<>();
    protected final ObservableHashMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod = new ObservableHashMap<>();
    protected final ObservableHashMap<PaymentMethod<?>, List<AccountCompatibilityMismatch>> incompatibleAccountsByPaymentMethod = new ObservableHashMap<>();

    protected final ObservableArray<PaymentMethodSpec<?>> takerSidePaymentMethodSpecs = new ObservableArray<>();

    public TakeOfferPaymentMethodModel() {
    }

    /* --------------------------------------------------------------------- */
    // takerSidePaymentMethodSpecs
    /* --------------------------------------------------------------------- */

    void setTakerSidePaymentMethodSpecs(List<PaymentMethodSpec<?>> specs) {
        takerSidePaymentMethodSpecs.setAll(specs);
    }

    @Override
    public ReadOnlyObservableArray<PaymentMethodSpec<?>> takerSidePaymentMethodSpecsObservable() {
        return takerSidePaymentMethodSpecs;
    }

    @Override
    public List<PaymentMethodSpec<?>> getTakerSidePaymentMethodSpecs() {
        return List.copyOf(takerSidePaymentMethodSpecs);
    }

    @Override
    public Optional<PaymentMethodSpec<?>> findTakerSidePaymentMethodSpec(PaymentMethod<?> paymentMethod) {
        return takerSidePaymentMethodSpecs.stream()
                .filter(spec -> spec.getPaymentMethod().equals(paymentMethod))
                .findFirst();
    }

    /* --------------------------------------------------------------------- */
    // accountsByPaymentMethod
    /* --------------------------------------------------------------------- */

    void clearAccountsByPaymentMethod() {
        accountsByPaymentMethod.clear();
    }

    void putAccountsByPaymentMethod(PaymentMethod<?> paymentMethod, List<Account<?, ?>> account) {
        accountsByPaymentMethod.put(paymentMethod, List.copyOf(account));
    }

    void removeAccountsByPaymentMethod(PaymentMethod<?> paymentMethod) {
        accountsByPaymentMethod.remove(paymentMethod);
    }

    void putAllAccountsByPaymentMethod(Map<PaymentMethod<?>, List<Account<?, ?>>> accountsByPaymentMethod) {
        accountsByPaymentMethod.forEach(this::putAccountsByPaymentMethod);
    }

    @Override
    public ReadOnlyObservableMap<PaymentMethod<?>, List<Account<?, ?>>> accountsByPaymentMethodObservable() {
        return accountsByPaymentMethod;
    }

    @Override
    public ImmutableMap<PaymentMethod<?>, List<Account<?, ?>>> getAccountsByPaymentMethod() {
        return ImmutableMap.copyOf(accountsByPaymentMethod);
    }

    /* --------------------------------------------------------------------- */
    // selectedAccountByPaymentMethod
    /* --------------------------------------------------------------------- */

    void clearSelectedAccountByPaymentMethod() {
        selectedAccountByPaymentMethod.clear();
    }

    void putSelectedAccountByPaymentMethod(PaymentMethod<?> paymentMethod, Account<?, ?> account) {
        selectedAccountByPaymentMethod.put(paymentMethod, account);
    }

    void removeSelectedAccountByPaymentMethod(PaymentMethod<?> paymentMethod) {
        selectedAccountByPaymentMethod.remove(paymentMethod);
    }

    @Override
    public ReadOnlyObservableMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethodObservable() {
        return selectedAccountByPaymentMethod;
    }

    @Override
    public ImmutableMap<PaymentMethod<?>, Account<?, ?>> getSelectedAccountByPaymentMethod() {
        return ImmutableMap.copyOf(selectedAccountByPaymentMethod);
    }

    /* --------------------------------------------------------------------- */
    // incompatibleAccountsByPaymentMethod
    /* --------------------------------------------------------------------- */

    // Stored list values are copied immutably so no holder of a getter result can mutate
    // the state behind the model's back.
    void setIncompatibleAccountsByPaymentMethod(Map<PaymentMethod<?>, List<AccountCompatibilityMismatch>> value) {
        incompatibleAccountsByPaymentMethod.clear();
        value.forEach((paymentMethod, mismatches) ->
                incompatibleAccountsByPaymentMethod.put(paymentMethod, List.copyOf(mismatches)));
    }

    @Override
    public ReadOnlyObservableMap<PaymentMethod<?>, List<AccountCompatibilityMismatch>> incompatibleAccountsByPaymentMethodObservable() {
        return incompatibleAccountsByPaymentMethod;
    }

    @Override
    public ImmutableMap<PaymentMethod<?>, List<AccountCompatibilityMismatch>> getIncompatibleAccountsByPaymentMethod() {
        return ImmutableMap.copyOf(incompatibleAccountsByPaymentMethod);
    }
}
