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

package bisq.desktop.main.content.user.accounts.crypto_accounts.details;

import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.crypto.CryptoAssetAccount;
import bisq.account.accounts.crypto.CryptoAssetAccountPayload;
import bisq.account.payment_method.crypto.CryptoPaymentRail;
import bisq.account.timestamp.AccountTimestampService;
import bisq.common.data.Triple;
import bisq.common.monetary.Coin;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.user.accounts.AccountDetails;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.BooleanFormatter;
import bisq.presentation.formatters.TimeFormatter;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class CryptoAccountDetails<A extends CryptoAssetAccount<?>> extends AccountDetails<A, CryptoPaymentRail> {
    protected Label addressDescriptionLabel;

    public CryptoAccountDetails(A account, AccountTimestampService accountTimestampService) {
        super(account, accountTimestampService);
    }

    @Override
    protected void addHeader() {
        Triple<Text, Label, VBox> currencyTriple = getDescriptionValueVBoxTriple(Res.get("paymentAccounts.currency"),
                account.getPaymentMethod().getDisplayString());
        gridPane.add(currencyTriple.getThird(), 0, rowIndex);
    }

    @Override
    protected void addDetails() {
        CryptoAssetAccountPayload accountPayload = account.getAccountPayload();
        String address = accountPayload.getAddress();
        addressDescriptionLabel = addDescriptionLabel(Res.get("paymentAccounts.crypto.address.address"));
        addValueLabel(address);
        if (address.length() > 70) {
            addressDescriptionLabel.setTooltip(new BisqTooltip(address));
        }
        String isInstant = BooleanFormatter.toEnabledDisabled(accountPayload.isInstant());
        addDescriptionAndValue(Res.get("paymentAccounts.crypto.isInstant"), isInstant);

        if (accountPayload.getIsAutoConf().isPresent()) {
            Label autoConfHeadline = new Label(Res.get("paymentAccounts.crypto.address.autoConf").toUpperCase());
            autoConfHeadline.getStyleClass().add("trade-wizard-review-details-headline");
            GridPane.setMargin(autoConfHeadline, new Insets(20, 0, 0, 0));
            gridPane.add(autoConfHeadline, 0, ++rowIndex, 3, 1);
            Region autoConfLine = getLine();
            GridPane.setMargin(autoConfLine, new Insets(-10, 0, -5, 0));
            gridPane.add(autoConfLine, 0, ++rowIndex, 3, 1);

            Boolean isAutoConf = accountPayload.getIsAutoConf().get();
            String autoConfString = BooleanFormatter.toEnabledDisabled(isAutoConf);
            addDescriptionAndValue(Res.get("state.enabled"), autoConfString);
            if (isAutoConf) {
                String autoConfNumConfirmations = String.valueOf(accountPayload.getAutoConfNumConfirmations().orElseThrow());
                addDescriptionAndValue(Res.get("paymentAccounts.crypto.address.autoConf.numConfirmations"), autoConfNumConfirmations);

                String autoConfMaxTradeAmount = AmountFormatter.formatAmountWithCode(Coin.fromValue(accountPayload.getAutoConfMaxTradeAmount().orElseThrow(), "BTC"), true);
                addDescriptionAndValue(Res.get("paymentAccounts.crypto.address.autoConf.maxTradeAmount"), autoConfMaxTradeAmount);

                String autoConfExplorerUrls = accountPayload.getAutoConfExplorerUrls().orElseThrow();
                Label autoConfExplorerUrlsLabel = addDescriptionAndValue(Res.get("paymentAccounts.crypto.address.autoConf.explorerUrls"), autoConfExplorerUrls);
                if (autoConfExplorerUrls.length() > 70) {
                    autoConfExplorerUrlsLabel.setTooltip(new BisqTooltip(autoConfExplorerUrls));
                }
            }
        }

        accountTimestampService.findAccountTimestamp(account)
                .ifPresent(date -> {
                    String accountAge = TimeFormatter.formatAgeInDays(date);
                    addDescriptionAndValue(Res.get("paymentAccounts.accountAge"), accountAge);
                });
    }


}
