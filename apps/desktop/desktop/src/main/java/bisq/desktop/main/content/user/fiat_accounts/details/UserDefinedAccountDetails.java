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
import bisq.account.timestamp.AccountTimestampService;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.i18n.Res;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import lombok.Getter;

import javax.annotation.Nullable;

public class UserDefinedAccountDetails extends FiatBaseAccountDetails<UserDefinedFiatAccount, FiatPaymentRail> {
    // If true we use the design from 2.1.7 and the option to save the data
    public final static boolean USE_LEGACY_DESIGN = true;
    @Getter
    @Nullable
    private ReadOnlyStringProperty textAreaTextProperty;

    public UserDefinedAccountDetails(UserDefinedFiatAccount account, AccountTimestampService accountTimestampService) {
        super(account, accountTimestampService);

        if (USE_LEGACY_DESIGN) {
            String accountData = account.getAccountPayload().getAccountData();
            MaterialTextArea textArea = new MaterialTextArea(Res.get("paymentAccounts.userDefined.accountData"));
            textArea.setText(accountData);
            textArea.setEditable(true);
            textArea.showCopyIcon();
            textArea.getIconButton().setOpacity(0.3);
            textArea.setFixedHeight(300);

            textAreaTextProperty = textArea.textProperty();

            getChildren().setAll(textArea);
        }
    }

    @Override
    protected void setupRoot() {
        if (!USE_LEGACY_DESIGN) {
            super.setupRoot();
        }
    }

    @Override
    protected void addHeader() {
        if (!USE_LEGACY_DESIGN) {
            super.addHeader();
        }
    }

    protected void addCurrencyDisplay() {
        // We don't show currency display
    }

    @Override
    protected void addDetailsHeadline() {
        if (!USE_LEGACY_DESIGN) {
            super.addDetailsHeadline();
        }
    }

    @Override
    protected void addDetails() {
        if (!USE_LEGACY_DESIGN) {
            Label descriptionLabel = addDescriptionLabel(Res.get("paymentAccounts.userDefined.accountData"));
            GridPane.setValignment(descriptionLabel, VPos.TOP);
            String accountData = account.getAccountPayload().getAccountData();
            Label valueLabel = addValueLabel(accountData);
            GridPane.setMargin(valueLabel, new Insets(0, 20, 0, 0));
            BisqMenuItem copyButton = addCopyButton(accountData);
            GridPane.setValignment(copyButton, VPos.TOP);

            super.addDetails();
        }
    }

    @Override
    protected void addRestrictionsHeadline() {
        if (!USE_LEGACY_DESIGN) {
            super.addRestrictionsHeadline();
        }
    }

    @Override
    protected void addRestrictions() {
        if (!USE_LEGACY_DESIGN) {
            addDescriptionAndValue(Res.get("paymentAccounts.userDefined.note"),
                    Res.get("paymentAccounts.userDefined.note.deprecated"));
        }
    }
}
