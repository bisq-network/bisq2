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

package bisq.desktop.main.content.user.crypto_accounts.details;

import bisq.account.accounts.crypto.MoneroAccount;
import bisq.account.accounts.crypto.MoneroAccountPayload;
import bisq.account.timestamp.AccountTimestampService;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import bisq.presentation.formatters.BooleanFormatter;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

public class MoneroAccountDetails extends CryptoAccountDetails<MoneroAccount> {
    public MoneroAccountDetails(MoneroAccount account, AccountTimestampService accountTimestampService) {
        super(account, accountTimestampService);
    }

    @Override
    protected void addDetails(MoneroAccount account) {
        super.addDetails(account);

        MoneroAccountPayload accountPayload = account.getAccountPayload();

        Label subAddressesHeadline = new Label(Res.get("paymentAccounts.crypto.address.xmr.useSubAddresses").toUpperCase());
        subAddressesHeadline.getStyleClass().add("trade-wizard-review-details-headline");
        GridPane.setMargin(subAddressesHeadline, new Insets(20, 0, 0, 0));
        gridPane.add(subAddressesHeadline, 0, ++rowIndex, 3, 1);
        Region subAddressesLine = getLine();
        GridPane.setMargin(subAddressesLine, new Insets(-10, 0, -5, 0));
        gridPane.add(subAddressesLine, 0, ++rowIndex, 3, 1);

        boolean isUseSubAddresses = accountPayload.isUseSubAddresses();
        String isUseSubAddressesString = BooleanFormatter.toEnabledDisabled(isUseSubAddresses);
        addDescriptionAndValue(Res.get("state.enabled"), isUseSubAddressesString);
        if (isUseSubAddresses) {
            addressDescriptionLabel.setText(Res.get("paymentAccounts.crypto.address.xmr.mainAddresses"));
            String privateViewKey = accountPayload.getPrivateViewKey().orElseThrow();
            Label privateViewKeyLabel = addDescriptionAndValue(Res.get("paymentAccounts.crypto.address.xmr.privateViewKey"), privateViewKey);
            if (privateViewKey.length() > 70) {
                privateViewKeyLabel.setTooltip(new BisqTooltip(privateViewKey));
            }
            String accountIndex = String.valueOf(accountPayload.getAccountIndex().orElseThrow());
            addDescriptionAndValue(Res.get("paymentAccounts.crypto.address.xmr.accountIndex"), accountIndex);

            String initialSubAddressIndex = String.valueOf(accountPayload.getInitialSubAddressIndex().orElseThrow());
            addDescriptionAndValue(Res.get("paymentAccounts.crypto.address.xmr.initialSubAddressIndex"), initialSubAddressIndex);
        }
    }
}
