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

import bisq.account.accounts.fiat.AdvancedCashAccountPayload;
import bisq.account.payment_method.fiat.FiatPaymentRailUtil;
import bisq.common.asset.FiatCurrency;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;

import java.util.List;

public class AdvancedCashFormController extends FormController<AdvancedCashFormView, AdvancedCashFormModel, AdvancedCashAccountPayload> {
    public AdvancedCashFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected AdvancedCashFormView createView() {
        return new AdvancedCashFormView(model, this);
    }

    @Override
    protected AdvancedCashFormModel createModel() {
        return new AdvancedCashFormModel(StringUtils.createUid(), FiatPaymentRailUtil.getAdvancedCashCurrencies());
    }

    @Override
    public void onActivate() {
        super.onActivate();
        model.getRunValidation().set(false);
        model.getCurrencyErrorVisible().set(false);
    }

    @Override
    public boolean validate() {
        boolean hasSelectedCurrencies = !model.getSelectedCurrencies().isEmpty();
        model.getCurrencyErrorVisible().set(!hasSelectedCurrencies);

        boolean isAccountNrValid = model.getAccountNrValidator().validateAndGet();
        model.getRunValidation().set(true);
        return hasSelectedCurrencies && isAccountNrValid;
    }

    @Override
    public AdvancedCashAccountPayload createAccountPayload() {
        List<String> selectedCurrencyCodes = model.getSelectedCurrencies().stream()
                .map(FiatCurrency::getCode)
                .toList();
        return new AdvancedCashAccountPayload(model.getId(),
                selectedCurrencyCodes,
                model.getAccountNr().get());
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }

    void onSelectCurrency(FiatCurrency currency, boolean selected) {
        if (selected) {
            if (!model.getSelectedCurrencies().contains(currency)) {
                model.getSelectedCurrencies().add(currency);
            }
        } else {
            model.getSelectedCurrencies().remove(currency);
        }
        model.getCurrencyErrorVisible().set(model.getSelectedCurrencies().isEmpty());
    }
}
