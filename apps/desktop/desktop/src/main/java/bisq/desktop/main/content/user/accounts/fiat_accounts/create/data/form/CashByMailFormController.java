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

import bisq.account.accounts.fiat.CashByMailAccountPayload;
import bisq.common.asset.FiatCurrencyRepository;
import bisq.common.locale.LocaleRepository;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;

public class CashByMailFormController extends FormController<CashByMailFormView, CashByMailFormModel, CashByMailAccountPayload> {
    public CashByMailFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected CashByMailFormView createView() {
        return new CashByMailFormView(model, this);
    }

    @Override
    protected CashByMailFormModel createModel() {
        return new CashByMailFormModel(StringUtils.createUid(),
                FiatCurrencyRepository.getAllCurrencies());
    }

    @Override
    public void onActivate() {
        super.onActivate();
        model.getRunValidation().set(false);
        model.getCurrencyErrorVisible().set(false);

        if (model.getSelectedCurrency().get() == null) {
            String countryCode = LocaleRepository.getDefaultLocale().getCountry();
            model.getSelectedCurrency().set(FiatCurrencyRepository.getCurrencyByCountryCode(countryCode));
        }
        showOverlay();
    }

    @Override
    public boolean validate() {
        boolean isCurrencySet = model.getSelectedCurrency().get() != null;
        model.getCurrencyErrorVisible().set(!isCurrencySet);
        boolean postalAddressValid = model.getPostalAddressValidator().validateAndGet();
        boolean contactValid = model.getContactValidator().validateAndGet();
        boolean extraInfoValid = model.getExtraInfoValidator().validateAndGet();
        model.getRunValidation().set(true);
        return isCurrencySet && postalAddressValid && contactValid && extraInfoValid;
    }

    @Override
    public CashByMailAccountPayload createAccountPayload() {
        return new CashByMailAccountPayload(model.getId(),
                model.getSelectedCurrency().get().getCode(),
                model.getPostalAddress().get(),
                model.getContact().get(),
                model.getExtraInfo().get());
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }

    void onSelectCurrency() {
        model.getCurrencyErrorVisible().set(false);
    }
}
