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
import bisq.account.accounts.fiat.BankAccountType;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;

import java.util.List;

public class AchTransferFormController extends FormController<AchTransferFormView, AchTransferFormModel, AchTransferAccountPayload> {
    public AchTransferFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected AchTransferFormView createView() {
        return new AchTransferFormView(model, this);
    }

    @Override
    protected AchTransferFormModel createModel() {
        return new AchTransferFormModel(StringUtils.createUid(),
                List.of(BankAccountType.CHECKING, BankAccountType.SAVINGS));
    }

    @Override
    public void onActivate() {
        super.onActivate();
        model.getRunValidation().set(false);
        model.getBankAccountTypeErrorVisible().set(false);
    }

    @Override
    public boolean validate() {
        boolean isHolderNameValid = model.getHolderNameValidator().validateAndGet();
        boolean isHolderAddressValid = model.getHolderAddressValidator().validateAndGet();
        boolean isBankNameValid = model.getBankNameValidator().validateAndGet();
        boolean isBankIdValid = model.getBankIdValidator().validateAndGet();
        boolean isAccountNrValid = model.getAccountNrValidator().validateAndGet();

        boolean isBankAccountTypeSet = model.getSelectedBankAccountType().get() != null;
        model.getBankAccountTypeErrorVisible().set(!isBankAccountTypeSet);

        model.getRunValidation().set(true);
        return isHolderNameValid &&
                isHolderAddressValid &&
                isBankNameValid &&
                isBankIdValid &&
                isAccountNrValid &&
                isBankAccountTypeSet;
    }

    @Override
    public AchTransferAccountPayload createAccountPayload() {
        return new AchTransferAccountPayload(model.getId(),
                model.getHolderName().get(),
                model.getHolderAddress().get(),
                model.getBankName().get(),
                model.getBankId().get(),
                model.getAccountNr().get(),
                model.getSelectedBankAccountType().get());
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }

    void onSelectBankAccountType(BankAccountType selectedBankAccountType) {
        model.getSelectedBankAccountType().set(selectedBankAccountType);
        model.getBankAccountTypeErrorVisible().set(false);
    }
}
