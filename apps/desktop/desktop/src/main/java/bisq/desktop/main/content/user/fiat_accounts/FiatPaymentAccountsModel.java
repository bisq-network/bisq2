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

package bisq.desktop.main.content.user.fiat_accounts;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.desktop.common.view.Model;
import bisq.desktop.main.content.user.fiat_accounts.details.FiatBaseAccountDetails;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class FiatPaymentAccountsModel implements Model {
    private final BooleanProperty noAccountsAvailable = new SimpleBooleanProperty();
    private final BooleanProperty deleteButtonDisabled = new SimpleBooleanProperty();
    private final BooleanProperty saveButtonDisabled = new SimpleBooleanProperty();
    private final BooleanProperty saveButtonVisible = new SimpleBooleanProperty();

    private final ObservableList<Account<? extends PaymentMethod<?>, ?>> accounts = FXCollections.observableArrayList();
    private final SortedList<Account<? extends PaymentMethod<?>, ?>> sortedAccounts = new SortedList<>(accounts);
    private final ObjectProperty<Account<? extends PaymentMethod<?>, ?>> selectedAccount = new SimpleObjectProperty<>();
    private final ObjectProperty<FiatBaseAccountDetails<?, ?>> accountDetails = new SimpleObjectProperty<>();
}
