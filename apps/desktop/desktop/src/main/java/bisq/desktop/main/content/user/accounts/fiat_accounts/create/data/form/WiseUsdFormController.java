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

import bisq.account.accounts.fiat.WiseUsdAccountPayload;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;

public class WiseUsdFormController extends FormController<WiseUsdFormView, WiseUsdFormModel, WiseUsdAccountPayload> {
    public WiseUsdFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected WiseUsdFormView createView() {
        return new WiseUsdFormView(model, this);
    }

    @Override
    protected WiseUsdFormModel createModel() {
        return new WiseUsdFormModel(StringUtils.createUid());
    }

    @Override
    public void onActivate() {
        super.onActivate();
        model.getRunValidation().set(false);
    }

    @Override
    public boolean validate() {
        boolean isHolderNameValid = model.getHolderNameValidator().validateAndGet();
        boolean isEmailValid = model.getEmailValidator().validateAndGet();
        boolean isBeneficiaryAddressValid = model.getBeneficiaryAddressValidator().validateAndGet();
        model.getRunValidation().set(true);
        return isHolderNameValid && isEmailValid && isBeneficiaryAddressValid;
    }

    @Override
    public WiseUsdAccountPayload createAccountPayload() {
        return new WiseUsdAccountPayload(model.getId(),
                "US",
                model.getHolderName().get(),
                model.getEmail().get(),
                model.getBeneficiaryAddress().get());
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }
}
