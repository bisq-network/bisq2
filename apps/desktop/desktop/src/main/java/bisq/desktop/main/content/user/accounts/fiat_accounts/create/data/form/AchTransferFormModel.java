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

import bisq.account.accounts.fiat.AchTransferAccountPayload;
import bisq.account.accounts.fiat.BankAccountPayload;
import bisq.account.accounts.fiat.BankAccountType;
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
public class AchTransferFormModel extends FormModel {
    private final BooleanProperty runValidation = new SimpleBooleanProperty();

    private final StringProperty holderName = new SimpleStringProperty();
    private final TextMinMaxLengthValidator holderNameValidator = new TextMinMaxLengthValidator(BankAccountPayload.HOLDER_NAME_MIN_LENGTH,
            BankAccountPayload.HOLDER_NAME_MAX_LENGTH);

    private final StringProperty holderAddress = new SimpleStringProperty();
    private final TextMinMaxLengthValidator holderAddressValidator = new TextMinMaxLengthValidator(AchTransferAccountPayload.HOLDER_ADDRESS_MIN_LENGTH,
            AchTransferAccountPayload.HOLDER_ADDRESS_MAX_LENGTH);

    private final StringProperty bankName = new SimpleStringProperty();
    private final TextMinMaxLengthValidator bankNameValidator = new TextMinMaxLengthValidator(BankAccountPayload.BANK_NAME_MIN_LENGTH,
            BankAccountPayload.BANK_NAME_MAX_LENGTH);

    private final StringProperty bankId = new SimpleStringProperty();
    private final TextMinMaxLengthValidator bankIdValidator = new TextMinMaxLengthValidator(BankAccountPayload.BANK_ID_MIN_LENGTH,
            BankAccountPayload.BANK_ID_MAX_LENGTH);

    private final StringProperty accountNr = new SimpleStringProperty();
    private final StringProperty accountNrDescription = new SimpleStringProperty(Res.get("paymentAccounts.accountNr"));
    private final StringProperty accountNrPrompt = new SimpleStringProperty(Res.get("paymentAccounts.createAccount.prompt", Res.get("paymentAccounts.accountNr")));
    private final TextMinMaxLengthValidator accountNrValidator = new TextMinMaxLengthValidator(BankAccountPayload.ACCOUNT_NR_MIN_LENGTH,
            BankAccountPayload.ACCOUNT_NR_MAX_LENGTH);

    private final ObservableList<BankAccountType> bankAccountTypes;
    private final ObjectProperty<BankAccountType> selectedBankAccountType = new SimpleObjectProperty<>();
    private final BooleanProperty bankAccountTypeErrorVisible = new SimpleBooleanProperty();

    public AchTransferFormModel(String id, List<BankAccountType> bankAccountTypes) {
        super(id);
        this.bankAccountTypes = FXCollections.observableArrayList(bankAccountTypes);
    }
}
