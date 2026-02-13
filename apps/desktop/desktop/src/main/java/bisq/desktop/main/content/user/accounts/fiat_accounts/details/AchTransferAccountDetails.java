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

import bisq.account.accounts.fiat.AchTransferAccount;
import bisq.account.accounts.fiat.AchTransferAccountPayload;
import bisq.account.timestamp.AccountTimestampService;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import javafx.scene.control.Label;

public class AchTransferAccountDetails extends BankAccountDetails<AchTransferAccountPayload, AchTransferAccount> {
    public AchTransferAccountDetails(AchTransferAccount account, AccountTimestampService accountTimestampService) {
        super(account, accountTimestampService);
    }

    @Override
    protected void addBankAccountDetails() {
        AchTransferAccountPayload accountPayload = account.getAccountPayload();
        String countryCode = accountPayload.getCountryCode();

        addHolderName(accountPayload);
        addHolderAddress(accountPayload);
        addAccountNr(countryCode, accountPayload);
        addBankAccountType(accountPayload);

        addBankName(accountPayload);
        addBankId(accountPayload, countryCode);
        addBranchId(accountPayload, countryCode);
    }

    private void addHolderAddress(AchTransferAccountPayload accountPayload) {
        Label label = addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.holderAddress"),
                accountPayload.getHolderAddressAsSingleLine(), accountPayload.getHolderAddress());
        label.setTooltip(new BisqTooltip(accountPayload.getHolderAddress()));
    }
}
