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

import bisq.account.accounts.fiat.CashByMailAccount;
import bisq.account.accounts.fiat.CashByMailAccountPayload;
import bisq.account.timestamp.AccountTimestampService;
import bisq.common.util.StringUtils;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import javafx.scene.control.Label;

public class CashByMailAccountDetails extends FiatAccountDetails<CashByMailAccount> {
    public CashByMailAccountDetails(CashByMailAccount account, AccountTimestampService accountTimestampService) {
        super(account, accountTimestampService);
    }

    @Override
    protected void addDetails() {
        CashByMailAccountPayload accountPayload = account.getAccountPayload();

        Label postalAddress = addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.postalAddress"),
                accountPayload.getPostalAddress());
        postalAddress.setTooltip(new BisqTooltip(accountPayload.getPostalAddress()));

        addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.cashByMail.contact"),
                accountPayload.getContact());

        String extraInfo = accountPayload.getExtraInfo();
        if (StringUtils.isNotEmpty(extraInfo)) {
            Label extraInfoLabel = addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.cashByMail.extraInfo"),
                    extraInfo);
            extraInfoLabel.setTooltip(new BisqTooltip(extraInfo));
        }

        super.addDetails();
    }
}
