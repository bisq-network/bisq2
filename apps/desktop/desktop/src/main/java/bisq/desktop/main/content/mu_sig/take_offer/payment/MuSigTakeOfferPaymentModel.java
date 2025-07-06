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

package bisq.desktop.main.content.mu_sig.take_offer.payment;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.currency.Market;
import bisq.desktop.common.view.Model;
import bisq.offer.Direction;
import bisq.offer.payment_method.PaymentMethodSpec;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.ToggleGroup;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class MuSigTakeOfferPaymentModel implements Model {
    @Setter
    private Direction direction;
    @Setter
    private Market market;
    @Setter
    private String headline;
    @Setter
    private String subtitle;
    @Setter
    private String singlePaymentMethodAccountSelectionDescription;
    @Setter
    private boolean isSinglePaymentMethod;

    private final ToggleGroup toggleGroup = new ToggleGroup();
    private final ObservableList<PaymentMethod<?>> offeredPaymentMethods = FXCollections.observableArrayList();
    private final SortedList<PaymentMethod<?>> sortedPaymentMethods = new SortedList<>(offeredPaymentMethods);
    private final ObjectProperty<PaymentMethodSpec<?>> selectedPaymentMethodSpec = new SimpleObjectProperty<>();
    private final ObjectProperty<PaymentMethod<?>> paymentMethodWithoutAccount = new SimpleObjectProperty<>();
    private final ObjectProperty<PaymentMethod<?>> paymentMethodWithMultipleAccounts = new SimpleObjectProperty<>();

    private final Map<PaymentMethod<?>, List<Account<?, ?>>> accountsByPaymentMethod = new HashMap<>();
    private final ObjectProperty<Account<?, ?>> selectedAccount = new SimpleObjectProperty<>();
    private final ObservableList<Account<? extends PaymentMethod<?>, ?>> accountsForPaymentMethod = FXCollections.observableArrayList();
    private final SortedList<Account<? extends PaymentMethod<?>, ?>> sortedAccountsForPaymentMethod = new SortedList<>(accountsForPaymentMethod);

    public MuSigTakeOfferPaymentModel() {
    }

    void reset() {
        direction = null;
        market = null;
        headline = null;
        subtitle = null;
        singlePaymentMethodAccountSelectionDescription = null;
        isSinglePaymentMethod = false;
        toggleGroup.selectToggle(null);
        offeredPaymentMethods.clear();
        selectedPaymentMethodSpec.set(null);
        paymentMethodWithoutAccount.set(null);
        paymentMethodWithMultipleAccounts.set(null);
        accountsByPaymentMethod.clear();
        selectedAccount.set(null);
        accountsForPaymentMethod.clear();
        sortedAccountsForPaymentMethod.clear();
    }
}
