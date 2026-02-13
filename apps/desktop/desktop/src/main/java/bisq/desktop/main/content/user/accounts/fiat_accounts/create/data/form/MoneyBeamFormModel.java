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

import bisq.common.asset.FiatCurrency;
import bisq.common.locale.Country;
import bisq.common.validation.PaymentAccountValidation;
import bisq.desktop.components.controls.validator.EmailOrPhoneNumberValidator;
import bisq.desktop.components.controls.validator.TextMinMaxLengthValidator;
import bisq.i18n.Res;
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
public class MoneyBeamFormModel extends FormModel {
    private final ObservableList<Country> countries;
    private final ObjectProperty<Country> selectedCountry = new SimpleObjectProperty<>();
    private final BooleanProperty countryErrorVisible = new SimpleBooleanProperty();

    private final ObservableList<FiatCurrency> currencies;
    private final ObjectProperty<FiatCurrency> selectedCurrency = new SimpleObjectProperty<>();
    private final BooleanProperty currencyErrorVisible = new SimpleBooleanProperty();

    private final BooleanProperty runValidation = new SimpleBooleanProperty();

    private final StringProperty holderName = new SimpleStringProperty();
    private final TextMinMaxLengthValidator holderNameValidator = new TextMinMaxLengthValidator(
            PaymentAccountValidation.HOLDER_NAME_MIN_LENGTH,
            PaymentAccountValidation.HOLDER_NAME_MAX_LENGTH);

    private final StringProperty emailOrMobileNr = new SimpleStringProperty();
    private final EmailOrPhoneNumberValidator emailOrMobileNrValidator = new EmailOrPhoneNumberValidator(Res.get("validation.empty"));

    public MoneyBeamFormModel(String id,
                              List<Country> countries,
                              List<FiatCurrency> currencies) {
        super(id);
        this.countries = FXCollections.observableArrayList(countries);
        this.currencies = FXCollections.observableArrayList(currencies);
    }
}
