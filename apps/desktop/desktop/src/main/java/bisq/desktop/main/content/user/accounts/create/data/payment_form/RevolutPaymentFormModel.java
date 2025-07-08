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

package bisq.desktop.main.content.user.accounts.create.data.payment_form;

import bisq.account.accounts.RevolutAccountPayload;
import bisq.common.currency.FiatCurrency;
import bisq.desktop.components.controls.validator.TextMinMaxLengthValidator;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class RevolutPaymentFormModel extends PaymentFormModel {
    private final ObservableList<FiatCurrency> revolutCurrencies;
    private final List<FiatCurrency> selectedCurrencies = new ArrayList<>();
    private final BooleanProperty selectedCurrenciesErrorVisible = new SimpleBooleanProperty();
    private final BooleanProperty runValidation = new SimpleBooleanProperty();
    private final StringProperty userName = new SimpleStringProperty();

    private final TextMinMaxLengthValidator userNameValidator = new TextMinMaxLengthValidator(RevolutAccountPayload.USER_NAME_MIN_LENGTH,
            RevolutAccountPayload.USER_NAME_MAX_LENGTH);

    public RevolutPaymentFormModel(String id, List<FiatCurrency> revolutCurrencies) {
        super(id);
        this.revolutCurrencies = FXCollections.observableArrayList(revolutCurrencies);
        selectedCurrencies.addAll(revolutCurrencies);
    }
}
