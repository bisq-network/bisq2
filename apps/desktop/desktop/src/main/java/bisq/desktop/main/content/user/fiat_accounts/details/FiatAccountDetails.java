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

import bisq.account.accounts.Account;
import bisq.account.accounts.fiat.CountryBasedAccount;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.timestamp.AccountTimestampService;
import bisq.common.data.Triple;
import bisq.desktop.main.content.user.AccountDetails;
import bisq.i18n.Res;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class FiatAccountDetails<A extends Account<?, ?>> extends AccountDetails<A, FiatPaymentRail> {
    public FiatAccountDetails(A account, AccountTimestampService accountTimestampService) {
        super(account, accountTimestampService);
    }

    @Override
    protected void addHeader() {
        super.addHeader();

        if (account instanceof CountryBasedAccount<?> countryBasedAccount) {
            Triple<Text, Label, VBox> currencyTriple = getDescriptionValueVBoxTriple(Res.get("paymentAccounts.country"),
                    countryBasedAccount.getCountry().getName());
            gridPane.add(currencyTriple.getThird(), 2, rowIndex);
        }
    }

    @Override
    protected void addRestrictions() {
        if (account.getPaymentMethod().getPaymentRail() instanceof FiatPaymentRail fiatPaymentRail) {
            addDescriptionAndValue(Res.get("paymentAccounts.chargebackRisk"),
                    fiatPaymentRail.getChargebackRisk().toString());
        }
        super.addRestrictions();
    }
}
