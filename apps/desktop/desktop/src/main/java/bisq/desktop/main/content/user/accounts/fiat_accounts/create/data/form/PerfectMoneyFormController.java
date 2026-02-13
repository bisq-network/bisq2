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

import bisq.account.accounts.fiat.PerfectMoneyAccountPayload;
import bisq.account.payment_method.fiat.FiatPaymentRailUtil;
import bisq.common.asset.Asset;
import bisq.common.asset.FiatCurrency;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PerfectMoneyFormController extends FormController<PerfectMoneyFormView, PerfectMoneyFormModel, PerfectMoneyAccountPayload> {
    public PerfectMoneyFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected PerfectMoneyFormView createView() {
        return new PerfectMoneyFormView(model, this);
    }

    @Override
    protected PerfectMoneyFormModel createModel() {
        List<FiatCurrency> currencies = FiatPaymentRailUtil.getPerfectMoneyCurrencies().stream()
                .sorted(Comparator.comparing(Asset::getName))
                .collect(Collectors.toList());
        return new PerfectMoneyFormModel(StringUtils.createUid(), currencies);
    }

    @Override
    public void onActivate() {
        super.onActivate();
        model.getRunValidation().set(false);
        model.getCurrencyErrorVisible().set(false);
    }

    @Override
    public boolean validate() {
        boolean currencySet = model.getSelectedCurrency().get() != null;
        model.getCurrencyErrorVisible().set(!currencySet);
        boolean accountNrValid = model.getAccountNrValidator().validateAndGet();
        model.getRunValidation().set(true);
        return currencySet && accountNrValid;
    }

    @Override
    public PerfectMoneyAccountPayload createAccountPayload() {
        return new PerfectMoneyAccountPayload(model.getId(),
                model.getSelectedCurrency().get().getCode(),
                model.getAccountNr().get());
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }

    void onSelectCurrency() {
        model.getCurrencyErrorVisible().set(false);
    }
}
