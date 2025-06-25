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

import bisq.account.accounts.SepaAccountPayload;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import bisq.desktop.components.controls.validator.SepaBicValidator;
import bisq.desktop.components.controls.validator.SepaIbanValidator;
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

import java.util.ArrayList;
import java.util.List;

@Getter
public class SepaPaymentFormModel extends PaymentFormModel {
    private final ObservableList<Country> allSepaCountries;
    private final List<Country> allEuroCountries;
    private final List<Country> allNonEuroCountries;
    private final List<Country> acceptedEuroCountries = new ArrayList<>();
    private final List<Country> acceptedNonEuroCountries = new ArrayList<>();
    private final ObjectProperty<Country> selectedCountryOfBank = new SimpleObjectProperty<>();
    private final BooleanProperty countryErrorVisible = new SimpleBooleanProperty();
    private final BooleanProperty acceptedCountriesErrorVisible = new SimpleBooleanProperty();
    private final BooleanProperty requireValidation = new SimpleBooleanProperty();
    private final StringProperty holderName = new SimpleStringProperty();
    private final StringProperty iban = new SimpleStringProperty();
    private final StringProperty bic = new SimpleStringProperty();


    private final TextMinMaxLengthValidator holderNameValidator = new TextMinMaxLengthValidator(SepaAccountPayload.HOLDER_NAME_MIN_LENGTH, SepaAccountPayload.HOLDER_NAME_MAX_LENGTH);
    private final SepaIbanValidator sepaIbanValidator = new SepaIbanValidator();
    private final SepaBicValidator sepaBicValidator = new SepaBicValidator();

    public SepaPaymentFormModel(String id,
                                List<Country> allSepaCountries,
                                List<Country> allEuroCountries,
                                List<Country> allNonEuroCountries) {
        super(id);
        this.allSepaCountries = FXCollections.observableArrayList(allSepaCountries);
        this.allEuroCountries = allEuroCountries;
        this.allNonEuroCountries = allNonEuroCountries;
        acceptedEuroCountries.addAll(allEuroCountries);
        acceptedNonEuroCountries.addAll(allNonEuroCountries);
    }
}
