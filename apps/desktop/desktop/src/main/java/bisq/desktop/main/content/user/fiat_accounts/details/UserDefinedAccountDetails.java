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

import bisq.account.accounts.fiat.UserDefinedFiatAccount;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class UserDefinedAccountDetails extends AccountDetails<UserDefinedFiatAccount, FiatPaymentRail> {
    public UserDefinedAccountDetails(UserDefinedFiatAccount account) {
        super(account);
    }

    @Override
    protected void addDetails(UserDefinedFiatAccount account) {
        Label descriptionLabel = addDescriptionLabel(Res.get("paymentAccounts.userDefined.accountData"));
        GridPane.setValignment(descriptionLabel, VPos.TOP);
        String accountData = account.getAccountPayload().getAccountData();
        Label valueLabel = addValueLabel(accountData);
        GridPane.setMargin(valueLabel, new Insets(0, 20, 0, 0));
        BisqMenuItem copyButton = addCopyButton(accountData);
        GridPane.setValignment(copyButton, VPos.TOP);
    }

    @Override
    protected void addRestrictions(UserDefinedFiatAccount account) {
        addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.userDefined.note"),
                Res.get("paymentAccounts.userDefined.note.deprecated"));
    }
}
