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

package bisq.desktop.main.content.user.accounts;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.desktop.common.view.Model;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Collection;

@Slf4j
public class PaymentAccountsModel implements Model {
    private final StringProperty accountData = new SimpleStringProperty("");
    private final BooleanProperty saveButtonDisabled = new SimpleBooleanProperty();
    private final BooleanProperty deleteButtonDisabled = new SimpleBooleanProperty();
    private final ObjectProperty<Account<?, ? extends PaymentMethod<?>>> selectedAccount = new SimpleObjectProperty<>();
    private final ObservableList<Account<?, ? extends PaymentMethod<?>>> accounts = FXCollections.observableArrayList();
    private final SortedList<Account<?, ? extends PaymentMethod<?>>> sortedAccounts = new SortedList<>(accounts);

    // selectedAccount
    @Nullable
    public Account<?, ? extends PaymentMethod<?>> getSelectedAccount() {
        return selectedAccount.get();
    }

    public ObjectProperty<Account<?, ? extends PaymentMethod<?>>> selectedAccountProperty() {
        return selectedAccount;
    }

    public void setSelectedAccount(Account<?, ? extends PaymentMethod<?>> selectedAccount) {
        this.selectedAccount.set(selectedAccount);
    }

    // accountData
    @Nullable
    public String getAccountData() {
        return accountData.get();
    }

    public StringProperty accountDataProperty() {
        return accountData;
    }

    public void setAccountData(String accountData) {
        this.accountData.set(accountData);
    }

    // saveButtonDisabled
    public boolean isSaveButtonDisabled() {
        return saveButtonDisabled.get();
    }

    public BooleanProperty saveButtonDisabledProperty() {
        return saveButtonDisabled;
    }

    public void setSaveButtonDisabled(boolean saveButtonDisabled) {
        this.saveButtonDisabled.set(saveButtonDisabled);
    }

    public boolean isDeleteButtonDisabled() {
        return deleteButtonDisabled.get();
    }

    public BooleanProperty deleteButtonDisabledProperty() {
        return deleteButtonDisabled;
    }

    public void setDeleteButtonDisabled(boolean deleteButtonDisabled) {
        this.deleteButtonDisabled.set(deleteButtonDisabled);
    }

    public void setAllAccounts(Collection<Account<?, ? extends PaymentMethod<?>>> collection) {
        accounts.setAll(collection);
    }

    public SortedList<Account<?, ? extends PaymentMethod<?>>> getSortedAccounts() {
        return sortedAccounts;
    }
}
