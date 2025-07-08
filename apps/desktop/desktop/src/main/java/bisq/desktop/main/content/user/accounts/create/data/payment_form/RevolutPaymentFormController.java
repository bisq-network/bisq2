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

import bisq.account.accounts.RevolutAccountPayload;
import bisq.account.payment_method.FiatPaymentRailUtil;
import bisq.common.currency.FiatCurrency;
import bisq.common.currency.TradeCurrency;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class RevolutPaymentFormController extends PaymentFormController<RevolutPaymentFormView, RevolutPaymentFormModel, RevolutAccountPayload> {
    public RevolutPaymentFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected RevolutPaymentFormView createView() {
        return new RevolutPaymentFormView(model, this);
    }

    @Override
    protected RevolutPaymentFormModel createModel() {
        List<FiatCurrency> revolutCurrencies = FiatPaymentRailUtil.getRevolutCurrencies().stream()
                .sorted(Comparator.comparing(TradeCurrency::getName))
                .collect(Collectors.toList());
        return new RevolutPaymentFormModel(StringUtils.createUid(), revolutCurrencies);
    }

    @Override
    public void onActivate() {
        model.getRunValidation().set(false);
        model.getSelectedCurrenciesErrorVisible().set(false);
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    public RevolutAccountPayload createAccountPayload() {
        return new RevolutAccountPayload(model.getId(),
                model.getUserName().get(),
                model.getSelectedCurrencies().stream()
                        .map(TradeCurrency::getCode)
                        .collect(Collectors.toList()));
    }

    @Override
    public boolean validate() {
        boolean selectedCurrenciesValid = !model.getSelectedCurrencies().isEmpty();
        model.getSelectedCurrenciesErrorVisible().set(!selectedCurrenciesValid);
        boolean holderNameValid = model.getUserNameValidator().validateAndGet();
        boolean isValid = selectedCurrenciesValid && holderNameValid;
        model.getRunValidation().set(true);
        return isValid;
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }

    void onSelectCurrency(FiatCurrency currency, boolean selected) {
        if (selected) {
            model.getSelectedCurrencies().add(currency);
        } else {
            model.getSelectedCurrencies().remove(currency);
        }
        model.getSelectedCurrenciesErrorVisible().set(model.getSelectedCurrencies().isEmpty());
    }
}