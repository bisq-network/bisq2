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

package bisq.desktop.main.content.user.accounts.fiat_accounts.details;

import bisq.account.accounts.fiat.UpholdAccount;
import bisq.account.accounts.fiat.UpholdAccountPayload;
import bisq.account.payment_method.fiat.FiatPaymentRailUtil;
import bisq.account.timestamp.AccountTimestampService;
import bisq.common.asset.FiatCurrencyRepository;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import javafx.scene.control.Label;

import java.util.List;
import java.util.stream.Collectors;

public class UpholdAccountDetails extends FiatAccountDetails<UpholdAccount> {
    public UpholdAccountDetails(UpholdAccount account, AccountTimestampService accountTimestampService) {
        super(account, accountTimestampService);
    }

    @Override
    protected void addDetails() {
        UpholdAccountPayload accountPayload = account.getAccountPayload();

        addDescriptionAndValue(Res.get("paymentAccounts.holderName"),
                accountPayload.getHolderName());

        addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.uphold.accountId"),
                accountPayload.getAccountId());

        List<String> allCurrencyCodes = FiatPaymentRailUtil.getUpholdCurrencyCodes().stream()
                .sorted()
                .collect(Collectors.toList());
        List<String> selectedCurrencyCodes = accountPayload.getSelectedCurrencyCodes().stream()
                .sorted()
                .collect(Collectors.toList());
        boolean matchAllCurrencies = allCurrencyCodes.equals(selectedCurrencyCodes);
        String allCurrencies = Res.get("paymentAccounts.createAccount.accountData.uphold.allUpholdCurrencies");
        String selectedCurrencies = selectedCurrencyCodes.stream()
                .map(FiatCurrencyRepository::getDisplayNameAndCode)
                .sorted()
                .collect(Collectors.joining(", "));
        Label selectedCurrenciesLabel = addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.uphold.selectedCurrencies"),
                matchAllCurrencies ? allCurrencies : selectedCurrencies,
                selectedCurrencies);
        if (matchAllCurrencies || selectedCurrencies.length() > 70) {
            selectedCurrenciesLabel.setTooltip(new BisqTooltip(selectedCurrencies));
        }

        super.addDetails();
    }
}
