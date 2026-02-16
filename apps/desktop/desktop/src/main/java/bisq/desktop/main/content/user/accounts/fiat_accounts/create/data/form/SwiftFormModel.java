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

import bisq.account.accounts.fiat.SwiftAccountPayload;
import bisq.common.asset.FiatCurrency;
import bisq.common.locale.Country;
import bisq.desktop.components.controls.validator.OptionalTextMinMaxLengthValidator;
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
public class SwiftFormModel extends FormModel {
    private final ObservableList<Country> bankCountries;
    private final ObjectProperty<Country> selectedBankCountry = new SimpleObjectProperty<>();
    private final BooleanProperty bankCountryErrorVisible = new SimpleBooleanProperty();

    private final ObservableList<FiatCurrency> currencies;
    private final ObjectProperty<FiatCurrency> selectedCurrency = new SimpleObjectProperty<>();
    private final BooleanProperty currencyErrorVisible = new SimpleBooleanProperty();

    private final BooleanProperty currencyCountryMismatch = new SimpleBooleanProperty();
    private final BooleanProperty runValidation = new SimpleBooleanProperty();

    private final StringProperty beneficiaryName = new SimpleStringProperty();
    private final TextMinMaxLengthValidator beneficiaryNameValidator = new TextMinMaxLengthValidator(
            SwiftAccountPayload.NAME_MIN_LENGTH,
            SwiftAccountPayload.NAME_MAX_LENGTH);

    private final StringProperty beneficiaryAccountNr = new SimpleStringProperty();
    private final TextMinMaxLengthValidator beneficiaryAccountNrValidator = new TextMinMaxLengthValidator(
            SwiftAccountPayload.ACCOUNT_NR_MIN_LENGTH,
            SwiftAccountPayload.ACCOUNT_NR_MAX_LENGTH);

    private final StringProperty beneficiaryPhone = new SimpleStringProperty();
    private final OptionalTextMinMaxLengthValidator beneficiaryPhoneValidator = new OptionalTextMinMaxLengthValidator(
            SwiftAccountPayload.PHONE_MIN_LENGTH,
            SwiftAccountPayload.PHONE_MAX_LENGTH);

    private final StringProperty beneficiaryAddress = new SimpleStringProperty();
    private final TextMinMaxLengthValidator beneficiaryAddressValidator = new TextMinMaxLengthValidator(
            SwiftAccountPayload.ADDRESS_MIN_LENGTH,
            SwiftAccountPayload.ADDRESS_MAX_LENGTH);

    private final StringProperty bankSwiftCode = new SimpleStringProperty();
    private final TextMinMaxLengthValidator bankSwiftCodeValidator = new TextMinMaxLengthValidator(
            SwiftAccountPayload.SWIFT_CODE_MIN_LENGTH,
            SwiftAccountPayload.SWIFT_CODE_MAX_LENGTH);

    private final StringProperty bankName = new SimpleStringProperty();
    private final TextMinMaxLengthValidator bankNameValidator = new TextMinMaxLengthValidator(
            SwiftAccountPayload.NAME_MIN_LENGTH,
            SwiftAccountPayload.NAME_MAX_LENGTH);

    private final StringProperty bankBranch = new SimpleStringProperty();
    private final OptionalTextMinMaxLengthValidator bankBranchValidator = new OptionalTextMinMaxLengthValidator(
            SwiftAccountPayload.NAME_MIN_LENGTH,
            SwiftAccountPayload.NAME_MAX_LENGTH);

    private final StringProperty bankAddress = new SimpleStringProperty();
    private final TextMinMaxLengthValidator bankAddressValidator = new TextMinMaxLengthValidator(
            SwiftAccountPayload.ADDRESS_MIN_LENGTH,
            SwiftAccountPayload.ADDRESS_MAX_LENGTH);

    private final BooleanProperty useIntermediaryBank = new SimpleBooleanProperty();

    private final ObjectProperty<Country> intermediaryBankCountry = new SimpleObjectProperty<>();
    private final BooleanProperty intermediaryBankCountryErrorVisible = new SimpleBooleanProperty();

    private final StringProperty intermediaryBankSwiftCode = new SimpleStringProperty();
    private final TextMinMaxLengthValidator intermediaryBankSwiftCodeValidator = new TextMinMaxLengthValidator(
            SwiftAccountPayload.SWIFT_CODE_MIN_LENGTH,
            SwiftAccountPayload.SWIFT_CODE_MAX_LENGTH);

    private final StringProperty intermediaryBankName = new SimpleStringProperty();
    private final TextMinMaxLengthValidator intermediaryBankNameValidator = new TextMinMaxLengthValidator(
            SwiftAccountPayload.NAME_MIN_LENGTH,
            SwiftAccountPayload.NAME_MAX_LENGTH);

    private final StringProperty intermediaryBankBranch = new SimpleStringProperty();
    private final OptionalTextMinMaxLengthValidator intermediaryBankBranchValidator = new OptionalTextMinMaxLengthValidator(
            SwiftAccountPayload.NAME_MIN_LENGTH,
            SwiftAccountPayload.NAME_MAX_LENGTH);

    private final StringProperty intermediaryBankAddress = new SimpleStringProperty();
    private final TextMinMaxLengthValidator intermediaryBankAddressValidator = new TextMinMaxLengthValidator(
            SwiftAccountPayload.ADDRESS_MIN_LENGTH,
            SwiftAccountPayload.ADDRESS_MAX_LENGTH);

    private final StringProperty additionalInstructions = new SimpleStringProperty();
    private final OptionalTextMinMaxLengthValidator additionalInstructionsValidator = new OptionalTextMinMaxLengthValidator(
            SwiftAccountPayload.INSTRUCTIONS_MIN_LENGTH,
            SwiftAccountPayload.INSTRUCTIONS_MAX_LENGTH);

    public SwiftFormModel(String id, List<Country> bankCountries, List<FiatCurrency> allCurrencies) {
        super(id);
        this.bankCountries = FXCollections.observableArrayList(bankCountries);
        this.currencies = FXCollections.observableArrayList(allCurrencies);
    }
}
