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

package bisq.desktop.primary.main.content.settings.accounts.create;

import bisq.desktop.common.view.Model;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class CreatePaymentAccountModel implements Model {
    private final StringProperty accountData = new SimpleStringProperty();
    private final StringProperty accountName = new SimpleStringProperty();
    private final BooleanProperty saveButtonDisabled = new SimpleBooleanProperty();
    private final BooleanProperty isEditable = new SimpleBooleanProperty();

    public String getAccountData() {
        return accountData.get();
    }

    public StringProperty accountDataProperty() {
        return accountData;
    }

    public void setAccountData(String accountData) {
        this.accountData.set(accountData);
    }

    public String getAccountName() {
        return accountName.get();
    }

    public StringProperty accountNameProperty() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName.set(accountName);
    }

    public boolean isSaveButtonDisabled() {
        return saveButtonDisabled.get();
    }

    public BooleanProperty saveButtonDisabledProperty() {
        return saveButtonDisabled;
    }

    public void setSaveButtonDisabled(boolean saveButtonDisabled) {
        this.saveButtonDisabled.set(saveButtonDisabled);
    }

    public boolean isIsEditable() {
        return isEditable.get();
    }

    public BooleanProperty isEditableProperty() {
        return isEditable;
    }

    public void setIsEditable(boolean isEditable) {
        this.isEditable.set(isEditable);
    }
}