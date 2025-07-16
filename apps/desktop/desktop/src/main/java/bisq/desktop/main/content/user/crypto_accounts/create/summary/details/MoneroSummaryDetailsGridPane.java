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

package bisq.desktop.main.content.user.crypto_accounts.create.summary.details;

import bisq.account.accounts.crypto.MoneroAccountPayload;
import bisq.i18n.Res;

public class MoneroSummaryDetailsGridPane extends CryptoCurrencySummaryDetailsGridPane<MoneroAccountPayload> {
    public MoneroSummaryDetailsGridPane(MoneroAccountPayload accountPayload) {
        super(accountPayload);
    }

    protected void addDetails(MoneroAccountPayload accountPayload) {
        super.addDetails(accountPayload);

        if (accountPayload.isUseSubAddresses()) {
            String privateViewKey = accountPayload.getPrivateViewKey().orElseThrow();
            addDescriptionAndValue(Res.get("paymentAccounts.crypto.address.xmr.privateViewKey"), privateViewKey);

            String accountIndex = String.valueOf(accountPayload.getAccountIndex().orElseThrow());
            String initialSubAddressIndex = String.valueOf(accountPayload.getInitialSubAddressIndex().orElseThrow());
            String combined = Res.get("paymentAccounts.crypto.summary.details.xmr.indices.value", accountIndex, initialSubAddressIndex);
            addDescriptionAndValue(Res.get("paymentAccounts.crypto.summary.details.xmr.indices.description"), combined);
        }
    }
}
