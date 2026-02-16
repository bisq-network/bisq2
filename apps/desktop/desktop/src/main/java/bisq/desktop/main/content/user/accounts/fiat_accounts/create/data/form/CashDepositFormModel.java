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

import bisq.account.accounts.fiat.BankAccountPayload;
import bisq.account.accounts.fiat.BankAccountType;
import bisq.account.accounts.fiat.CashDepositAccountPayload;
import bisq.common.asset.FiatCurrency;
import bisq.common.locale.Country;
import bisq.desktop.components.controls.validator.TextMaxLengthValidator;
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
public class CashDepositFormModel extends FormModel {
    private final ObservableList<Country> allCountries;
    private final ObjectProperty<Country> selectedCountry = new SimpleObjectProperty<>();
    private final BooleanProperty countryErrorVisible = new SimpleBooleanProperty();

    private final ObservableList<FiatCurrency> currencies;
    private final ObjectProperty<FiatCurrency> selectedCurrency = new SimpleObjectProperty<>();
    private final BooleanProperty currencyErrorVisible = new SimpleBooleanProperty();
    private final BooleanProperty currencyCountryMismatch = new SimpleBooleanProperty();

    private final ObservableList<BankAccountType> bankAccountTypes;
    private final ObjectProperty<BankAccountType> selectedBankAccountType = new SimpleObjectProperty<>();
    private final BooleanProperty bankAccountTypeErrorVisible = new SimpleBooleanProperty();
    private final BooleanProperty isBankAccountTypesVisible = new SimpleBooleanProperty();

    private final BooleanProperty runValidation = new SimpleBooleanProperty();
    private final BooleanProperty useValidation = new SimpleBooleanProperty();

    private final StringProperty holderName = new SimpleStringProperty();
    private final TextMinMaxLengthValidator holderNameValidator = new TextMinMaxLengthValidator(BankAccountPayload.HOLDER_NAME_MIN_LENGTH,
            BankAccountPayload.HOLDER_NAME_MAX_LENGTH);

    private final StringProperty holderId = new SimpleStringProperty();
    private final BooleanProperty isHolderIdVisible = new SimpleBooleanProperty();
    private final StringProperty holderIdDescription = new SimpleStringProperty();
    private final StringProperty holderIdPrompt = new SimpleStringProperty();
    private final TextMinMaxLengthValidator holderIdValidator = new TextMinMaxLengthValidator(BankAccountPayload.HOLDER_ID_MIN_LENGTH,
            BankAccountPayload.HOLDER_ID_MAX_LENGTH);

    private final StringProperty bankName = new SimpleStringProperty();
    private final BooleanProperty isBankNameVisible = new SimpleBooleanProperty();
    private final TextMinMaxLengthValidator bankNameValidator = new TextMinMaxLengthValidator(BankAccountPayload.BANK_NAME_MIN_LENGTH,
            BankAccountPayload.BANK_NAME_MAX_LENGTH);

    private final StringProperty bankId = new SimpleStringProperty();
    private final BooleanProperty isBankIdVisible = new SimpleBooleanProperty();
    private final StringProperty bankIdDescription = new SimpleStringProperty();
    private final StringProperty bankIdPrompt = new SimpleStringProperty();
    private final TextMinMaxLengthValidator bankIdValidator = new TextMinMaxLengthValidator(BankAccountPayload.BANK_ID_MIN_LENGTH,
            BankAccountPayload.BANK_ID_MAX_LENGTH);

    private final StringProperty branchId = new SimpleStringProperty();
    private final BooleanProperty isBranchIdVisible = new SimpleBooleanProperty();
    private final StringProperty branchIdDescription = new SimpleStringProperty();
    private final StringProperty branchIdPrompt = new SimpleStringProperty();
    private final TextMinMaxLengthValidator branchIdValidator = new TextMinMaxLengthValidator(BankAccountPayload.BRANCH_ID_MIN_LENGTH,
            BankAccountPayload.BRANCH_ID_MAX_LENGTH);

    private final StringProperty accountNr = new SimpleStringProperty();
    private final StringProperty accountNrDescription = new SimpleStringProperty(Res.get("paymentAccounts.accountNr"));
    private final StringProperty accountNrPrompt = new SimpleStringProperty(Res.get("paymentAccounts.createAccount.prompt", Res.get("paymentAccounts.accountNr")));
    private final TextMinMaxLengthValidator accountNrValidator = new TextMinMaxLengthValidator(BankAccountPayload.ACCOUNT_NR_MIN_LENGTH,
            BankAccountPayload.ACCOUNT_NR_MAX_LENGTH);

    private final StringProperty nationalAccountId = new SimpleStringProperty();
    private final BooleanProperty isNationalAccountIdVisible = new SimpleBooleanProperty();
    private final StringProperty nationalAccountIdDescription = new SimpleStringProperty();
    private final StringProperty nationalAccountIdPrompt = new SimpleStringProperty();
    private final TextMinMaxLengthValidator nationalAccountIdValidator = new TextMinMaxLengthValidator(BankAccountPayload.NATIONAL_ACCOUNT_ID_MIN_LENGTH,
            BankAccountPayload.NATIONAL_ACCOUNT_ID_MAX_LENGTH);

    private final StringProperty requirements = new SimpleStringProperty();
    private final TextMaxLengthValidator requirementsValidator = new TextMaxLengthValidator(CashDepositAccountPayload.REQUIREMENTS_MAX_LENGTH);

    public CashDepositFormModel(String id,
                                List<Country> allCountries,
                                List<FiatCurrency> allCurrencies,
                                List<BankAccountType> bankAccountTypes) {
        super(id);
        this.allCountries = FXCollections.observableArrayList(allCountries);
        this.currencies = FXCollections.observableArrayList(allCurrencies);
        this.bankAccountTypes = FXCollections.observableArrayList(bankAccountTypes);
    }
}
