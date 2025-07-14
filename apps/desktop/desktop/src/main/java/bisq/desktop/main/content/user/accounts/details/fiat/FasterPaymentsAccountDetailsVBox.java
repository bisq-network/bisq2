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

package bisq.desktop.main.content.user.accounts.details.fiat;

import bisq.account.accounts.fiat.FasterPaymentsAccount;
import bisq.account.accounts.fiat.FasterPaymentsAccountPayload;
import bisq.i18n.Res;

public class FasterPaymentsAccountDetailsVBox extends FiatAccountDetailsVBox<FasterPaymentsAccount> {
    public FasterPaymentsAccountDetailsVBox(FasterPaymentsAccount account) {
        super(account);
    }

    @Override
    protected void addDetails(FasterPaymentsAccount account) {
        FasterPaymentsAccountPayload accountPayload = account.getAccountPayload();
        addDescriptionAndValue(Res.get("user.paymentAccounts.holderName"),
                accountPayload.getHolderName());
        addDescriptionAndValueWithCopyButton(Res.get("user.paymentAccounts.fasterPayments.sortCode"),
                accountPayload.getSortCode());
        addDescriptionAndValueWithCopyButton(Res.get("user.paymentAccounts.accountNr"),
                accountPayload.getAccountNr());
    }
}
