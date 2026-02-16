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

package bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details;

import bisq.account.accounts.fiat.PayseraAccountPayload;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentRailUtil;
import bisq.common.asset.FiatCurrencyRepository;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import javafx.scene.control.Label;

import java.util.List;
import java.util.stream.Collectors;

public class PayseraAccountDetailsGridPane extends FiatAccountDetailsGridPane<PayseraAccountPayload> {
    public PayseraAccountDetailsGridPane(PayseraAccountPayload accountPayload, FiatPaymentRail fiatPaymentRail) {
        super(accountPayload, fiatPaymentRail);
    }

    @Override
    protected void addDetails(PayseraAccountPayload accountPayload) {
        addDescriptionAndValue(Res.get("paymentAccounts.email"),
                accountPayload.getEmail());

        List<String> allCurrencyCodes = FiatPaymentRailUtil.getPayseraCurrencyCodes().stream()
                .sorted()
                .collect(Collectors.toList());
        List<String> selectedCurrencyCodes = accountPayload.getSelectedCurrencyCodes().stream()
                .sorted()
                .collect(Collectors.toList());
        String selectedCurrencies;
        if (allCurrencyCodes.equals(selectedCurrencyCodes)) {
            selectedCurrencies = Res.get("paymentAccounts.createAccount.accountData.paysera.allPayseraCurrencies");
        } else {
            selectedCurrencies = selectedCurrencyCodes.stream()
                    .map(FiatCurrencyRepository::getDisplayNameAndCode)
                    .sorted()
                    .collect(Collectors.joining(", "));
        }
        Label selectedCurrenciesLabel = addDescriptionAndValue(Res.get("paymentAccounts.paysera.selectedCurrencies"),
                selectedCurrencies);
        if (selectedCurrencies.length() > 70) {
            selectedCurrenciesLabel.setTooltip(new BisqTooltip(selectedCurrencies));
        }
    }
}
