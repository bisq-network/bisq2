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

import bisq.account.accounts.fiat.ZelleAccount;
import bisq.account.accounts.fiat.ZelleAccountPayload;
import bisq.i18n.Res;

public class ZelleAccountDetailsVBox extends FiatAccountDetailsVBox<ZelleAccount> {
    public ZelleAccountDetailsVBox(ZelleAccount account) {
        super(account);
    }

    @Override
    protected void addDetails(ZelleAccount account) {
        ZelleAccountPayload accountPayload = account.getAccountPayload();

        addDescriptionAndValue(Res.get("user.paymentAccounts.holderName"),
                accountPayload.getHolderName());

        addDescriptionAndValueWithCopyButton(Res.get("user.paymentAccounts.emailOrMobileNr"),
                accountPayload.getEmailOrMobileNr());
    }
}
