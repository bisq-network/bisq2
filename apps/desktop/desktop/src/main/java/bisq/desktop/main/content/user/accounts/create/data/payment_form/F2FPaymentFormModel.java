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

import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
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

@Getter
public class F2FPaymentFormModel extends PaymentFormModel {
    private final ObservableList<Country> countries = FXCollections.observableArrayList(CountryRepository.getCountries());
    private final ObjectProperty<Country> country = new SimpleObjectProperty<>();
    private final BooleanProperty countryErrorVisible = new SimpleBooleanProperty();
    private final BooleanProperty requireValidation = new SimpleBooleanProperty();
    private final StringProperty city = new SimpleStringProperty("Paris");
    private final StringProperty contact = new SimpleStringProperty("contact...");
    private final StringProperty extraInfo = new SimpleStringProperty("extraInfo...");

    private final TextMinMaxLengthValidator cityValidator = new TextMinMaxLengthValidator(2, 50);
    private final TextMinMaxLengthValidator contactValidator = new TextMinMaxLengthValidator(5, 150);
    private final TextMaxLengthValidator extraInfoValidator = new TextMaxLengthValidator(30);

    public F2FPaymentFormModel(String id) {
       super(id);
    }
}
