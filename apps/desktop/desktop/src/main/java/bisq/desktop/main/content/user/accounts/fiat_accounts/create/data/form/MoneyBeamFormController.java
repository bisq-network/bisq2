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

import bisq.account.accounts.fiat.MoneyBeamAccountPayload;
import bisq.common.locale.CountryRepository;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;

public class MoneyBeamFormController extends FormController<MoneyBeamFormView, MoneyBeamFormModel, MoneyBeamAccountPayload> {
    public MoneyBeamFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected MoneyBeamFormView createView() {
        return new MoneyBeamFormView(model, this);
    }

    @Override
    protected MoneyBeamFormModel createModel() {
        return new MoneyBeamFormModel(StringUtils.createUid());
    }

    @Override
    public void onActivate() {
        super.onActivate();
        model.getRunValidation().set(false);
    }

    @Override
    public boolean validate() {
        boolean holderNameValid = model.getHolderNameValidator().validateAndGet();
        boolean isEmailOrMobileNrValid = model.getEmailOrMobileNrValidator().validateAndGet();
        model.getRunValidation().set(true);
        return holderNameValid && isEmailOrMobileNrValid;
    }

    @Override
    public MoneyBeamAccountPayload createAccountPayload() {
        String countryCode = CountryRepository.getDefaultCountry().getCode();
        return new MoneyBeamAccountPayload(model.getId(),
                countryCode,
                model.getHolderName().get(),
                model.getEmailOrMobileNr().get());
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }

}
