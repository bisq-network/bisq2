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

package bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form;

import bisq.account.accounts.fiat.F2FAccountPayload;
import bisq.common.asset.FiatCurrency;
import bisq.common.validation.PaymentAccountValidation;
import bisq.desktop.components.controls.validator.TextMaxLengthValidator;
import bisq.desktop.components.controls.validator.TextMinMaxLengthValidator;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

import java.util.List;

@Getter
public class CashByMailFormModel extends FormModel {
    private final ObservableList<FiatCurrency> currencies;
    private final ObjectProperty<FiatCurrency> selectedCurrency = new SimpleObjectProperty<>();
    private final BooleanProperty currencyErrorVisible = new SimpleBooleanProperty();

    private final BooleanProperty runValidation = new SimpleBooleanProperty();
    private final StringProperty postalAddress = new SimpleStringProperty();
    private final StringProperty contact = new SimpleStringProperty();
    private final StringProperty extraInfo = new SimpleStringProperty();

    private final TextMinMaxLengthValidator postalAddressValidator = new TextMinMaxLengthValidator(
            PaymentAccountValidation.ADDRESS_MIN_LENGTH,
            PaymentAccountValidation.ADDRESS_MAX_LENGTH);
    private final TextMinMaxLengthValidator contactValidator = new TextMinMaxLengthValidator(
            F2FAccountPayload.CONTACT_MIN_LENGTH,
            F2FAccountPayload.CONTACT_MAX_LENGTH);
    private final TextMaxLengthValidator extraInfoValidator = new TextMaxLengthValidator(F2FAccountPayload.EXTRA_INFO_MAX_LENGTH);

    public CashByMailFormModel(String id, List<FiatCurrency> currencies) {
        super(id);
        this.currencies = FXCollections.observableArrayList(currencies);
    }
}
