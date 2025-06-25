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

import bisq.account.accounts.F2FAccountPayload;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.common.locale.CountryRepository;
import bisq.desktop.ServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class F2FPaymentFormController extends PaymentFormController<F2FPaymentFormView, F2FPaymentFormModel, F2FAccountPayload> {
    public F2FPaymentFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected F2FPaymentFormView createView() {
        return new F2FPaymentFormView(model, this);
    }

    @Override
    protected F2FPaymentFormModel createModel() {
        return new F2FPaymentFormModel(UUID.randomUUID().toString(), CountryRepository.getCountries());
    }

    @Override
    public void onActivate() {
        model.getRequireValidation().set(false);
        model.getCountryErrorVisible().set(false);
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    public boolean validate() {
        boolean isCountrySet = model.getSelectedCountry().get() != null;
        model.getCountryErrorVisible().set(!isCountrySet);
        boolean isValid = isCountrySet &&
                model.getCityValidator().validateAndGet() &&
                model.getContactValidator().validateAndGet() &&
                model.getExtraInfoValidator().validateAndGet();
        model.getRequireValidation().set(true);
        model.getRequireValidation().set(false);
        return isValid;
    }

    @Override
    public F2FAccountPayload getAccountPayload() {
        return new F2FAccountPayload(model.getId(),
                FiatPaymentRail.F2F.name(),
                model.getSelectedCountry().get().getCode(),
                model.getCity().get(),
                model.getContact().get(),
                model.getExtraInfo().get());
    }

}