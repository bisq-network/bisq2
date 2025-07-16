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

package bisq.desktop.main.content.user.fiat_accounts.details;

import bisq.account.accounts.fiat.RevolutAccount;
import bisq.account.accounts.fiat.RevolutAccountPayload;
import bisq.account.payment_method.FiatPaymentRailUtil;
import bisq.common.currency.FiatCurrencyRepository;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import javafx.scene.control.Label;

import java.util.List;
import java.util.stream.Collectors;

public class RevolutAccountDetails extends FiatAccountDetails<RevolutAccount> {
    public RevolutAccountDetails(RevolutAccount account) {
        super(account);
    }

    @Override
    protected void addDetails(RevolutAccount account) {
        RevolutAccountPayload accountPayload = account.getAccountPayload();

        addDescriptionAndValue(Res.get("paymentAccounts.userName"),
                accountPayload.getUserName());

        List<String> allRevolutCurrencyCodes = FiatPaymentRailUtil.getRevolutCurrencyCodes().stream()
                .sorted()
                .collect(Collectors.toList());

        List<String> selectedCurrencyCodes = accountPayload.getSelectedCurrencyCodes().stream()
                .sorted()
                .collect(Collectors.toList());

        String allRevolutCurrencies = Res.get("paymentAccounts.createAccount.accountData.revolut.allRevolutCurrencies");
        boolean matchAllCurrencies = allRevolutCurrencyCodes.equals(selectedCurrencyCodes);
        String selectedCurrencies = selectedCurrencyCodes.stream()
                .map(FiatCurrencyRepository::getDisplayNameAndCode)
                .sorted()
                .collect(Collectors.joining(", "));
        Label selectedCurrenciesLabel = addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.revolut.selectedCurrencies"),
                matchAllCurrencies ? allRevolutCurrencies : selectedCurrencies,
                selectedCurrencies);
        if (matchAllCurrencies || selectedCurrencies.length() > 70) {
            selectedCurrenciesLabel.setTooltip(new BisqTooltip(selectedCurrencies));
        }
    }
}
