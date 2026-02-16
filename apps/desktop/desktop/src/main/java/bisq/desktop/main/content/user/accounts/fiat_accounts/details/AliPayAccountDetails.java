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

import bisq.account.accounts.fiat.AliPayAccount;
import bisq.account.accounts.fiat.AliPayAccountPayload;
import bisq.account.timestamp.AccountTimestampService;
import bisq.i18n.Res;

public class AliPayAccountDetails extends FiatAccountDetails<AliPayAccount> {
    public AliPayAccountDetails(AliPayAccount account, AccountTimestampService accountTimestampService) {
        super(account, accountTimestampService);
    }

    @Override
    protected void addDetails() {
        AliPayAccountPayload accountPayload = account.getAccountPayload();

        addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.accountNr"),
                accountPayload.getAccountNr());

        super.addDetails();
    }
}
