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

package bisq.desktop.main.content.mu_sig.create_offer.payment;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.market.Market;
import bisq.common.observable.map.ObservableHashMap;
import bisq.desktop.common.view.Model;
import bisq.offer.Direction;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class MuSigCreateOfferPaymentModel implements Model {
    @Setter
    private Direction direction;
    @Setter
    private String paymentMethodCurrencyCode;

    private final Map<PaymentMethod<?>, List<Account<?, ?>>> accountsByPaymentMethod = new HashMap<>();
    private final ObservableList<PaymentMethod<?>> paymentMethods = FXCollections.observableArrayList();
    private final SortedList<PaymentMethod<?>> sortedPaymentMethods = new SortedList<>(paymentMethods);
    private final ObservableList<PaymentMethod<?>> selectedPaymentMethods = FXCollections.observableArrayList();
    private final ObjectProperty<Market> market = new SimpleObjectProperty<>();

    private final ObjectProperty<PaymentMethod<?>> paymentMethodWithoutAccount = new SimpleObjectProperty<>();
    private final ObjectProperty<PaymentMethod<?>> paymentMethodWithMultipleAccounts = new SimpleObjectProperty<>();
    private final BooleanProperty shouldShowNoAccountOverlay = new SimpleBooleanProperty();
    private final StringProperty noAccountOverlayHeadlineText = new SimpleStringProperty();
    private final StringProperty multipleAccountsOverlayHeadlineText = new SimpleStringProperty();

    private final ObservableList<Account<? extends PaymentMethod<?>, ?>> accountsForPaymentMethod = FXCollections.observableArrayList();
    private final SortedList<Account<? extends PaymentMethod<?>, ?>> sortedAccountsForPaymentMethod = new SortedList<>(accountsForPaymentMethod);
    private final ObservableHashMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod = new ObservableHashMap<>();

    public MuSigCreateOfferPaymentModel() {
    }

    void reset() {
        direction = null;
        paymentMethodCurrencyCode = null;
        accountsByPaymentMethod.clear();
        paymentMethods.clear();
        selectedPaymentMethods.clear();
        market.set(null);
        paymentMethodWithoutAccount.set(null);
        paymentMethodWithMultipleAccounts.set(null);
        shouldShowNoAccountOverlay.set(false);
        noAccountOverlayHeadlineText.set("");
        multipleAccountsOverlayHeadlineText.set("");
        accountsForPaymentMethod.clear();
        sortedAccountsForPaymentMethod.clear();
        selectedAccountByPaymentMethod.clear();
    }
}
