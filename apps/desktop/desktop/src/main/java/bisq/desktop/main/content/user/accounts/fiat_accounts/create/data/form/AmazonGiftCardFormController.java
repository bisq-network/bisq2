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

import bisq.account.accounts.fiat.AmazonGiftCardAccountPayload;
import bisq.account.payment_method.fiat.FiatPaymentRailUtil;
import bisq.common.asset.FiatCurrencyRepository;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;

public class AmazonGiftCardFormController extends FormController<AmazonGiftCardFormView, AmazonGiftCardFormModel, AmazonGiftCardAccountPayload> {
    public AmazonGiftCardFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected AmazonGiftCardFormView createView() {
        return new AmazonGiftCardFormView(model, this);
    }

    @Override
    protected AmazonGiftCardFormModel createModel() {
        return new AmazonGiftCardFormModel(StringUtils.createUid(), FiatPaymentRailUtil.getAmazonGiftCardCountries());
    }

    @Override
    public void onActivate() {
        super.onActivate();
        model.getRunValidation().set(false);
        model.getCountryErrorVisible().set(false);

        Country defaultCountry = CountryRepository.getDefaultCountry();
        if (model.getSelectedCountry().get() == null && model.getCountries().contains(defaultCountry)) {
            model.getSelectedCountry().set(defaultCountry);
        }
        showOverlay();
    }

    @Override
    public boolean validate() {
        boolean isCountrySet = model.getSelectedCountry().get() != null;
        model.getCountryErrorVisible().set(!isCountrySet);
        boolean emailOrMobileValid = model.getEmailOrMobileNrValidator().validateAndGet();
        model.getRunValidation().set(true);
        return isCountrySet && emailOrMobileValid;
    }

    @Override
    public AmazonGiftCardAccountPayload createAccountPayload() {
        String countryCode = model.getSelectedCountry().get().getCode();
        String selectedCurrencyCode = FiatCurrencyRepository.getCurrencyByCountryCode(countryCode).getCode();
        return new AmazonGiftCardAccountPayload(model.getId(),
                countryCode,
                selectedCurrencyCode,
                model.getEmailOrMobileNr().get());
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }

    void onCountrySelected(Country country) {
        model.getSelectedCountry().set(country);
        model.getCountryErrorVisible().set(false);
    }
}
