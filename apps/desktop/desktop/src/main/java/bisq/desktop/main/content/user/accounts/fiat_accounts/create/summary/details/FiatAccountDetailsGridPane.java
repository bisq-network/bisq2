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

import bisq.account.accounts.AccountPayload;
import bisq.account.payment_method.fiat.FiatPaymentRail;

public abstract class FiatAccountDetailsGridPane<A extends AccountPayload<?>> extends AccountDetailsGridPane<A, FiatPaymentRail> {
    public FiatAccountDetailsGridPane(A accountPayload, FiatPaymentRail fiatPaymentRail) {
        super(accountPayload, fiatPaymentRail);
    }

  /*  @Override
    protected void addRestrictions(FiatPaymentRail fiatPaymentRail) {
        addDescriptionAndValue(Res.get("paymentAccounts.chargebackRisk"), fiatPaymentRail.getChargebackRisk().toString());
        super.addRestrictions(fiatPaymentRail);
    }*/
}
