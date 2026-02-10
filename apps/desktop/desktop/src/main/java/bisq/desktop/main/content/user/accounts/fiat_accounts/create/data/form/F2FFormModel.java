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

import static bisq.account.accounts.fiat.F2FAccountPayload.CITY_MAX_LENGTH;
import static bisq.account.accounts.fiat.F2FAccountPayload.CITY_MIN_LENGTH;
import static bisq.account.accounts.fiat.F2FAccountPayload.CONTACT_MAX_LENGTH;
import static bisq.account.accounts.fiat.F2FAccountPayload.CONTACT_MIN_LENGTH;
import static bisq.account.accounts.fiat.F2FAccountPayload.EXTRA_INFO_MAX_LENGTH;

@Getter
public class F2FFormModel extends FormModel {
    private final ObservableList<Country> allCountries;
    private final ObjectProperty<Country> selectedCountry = new SimpleObjectProperty<>();
    private final BooleanProperty countryErrorVisible = new SimpleBooleanProperty();

    private final ObservableList<FiatCurrency> currencies;
    private final ObjectProperty<FiatCurrency> selectedCurrency = new SimpleObjectProperty<>();
    private final BooleanProperty currencyErrorVisible = new SimpleBooleanProperty();

    private final BooleanProperty currencyCountryMismatch = new SimpleBooleanProperty();
    private final BooleanProperty runValidation = new SimpleBooleanProperty();
    private final StringProperty city = new SimpleStringProperty();
    private final StringProperty contact = new SimpleStringProperty();
    private final StringProperty extraInfo = new SimpleStringProperty();

    private final TextMinMaxLengthValidator cityValidator = new TextMinMaxLengthValidator(CITY_MIN_LENGTH, CITY_MAX_LENGTH);
    private final TextMinMaxLengthValidator contactValidator = new TextMinMaxLengthValidator(CONTACT_MIN_LENGTH, CONTACT_MAX_LENGTH);
    private final TextMaxLengthValidator extraInfoValidator = new TextMaxLengthValidator(EXTRA_INFO_MAX_LENGTH);

    public F2FFormModel(String id, List<Country> allCountries, List<FiatCurrency> allCurrencies) {
        super(id);
        this.allCountries = FXCollections.observableArrayList(allCountries);
        this.currencies = FXCollections.observableArrayList(allCurrencies);
    }
}
