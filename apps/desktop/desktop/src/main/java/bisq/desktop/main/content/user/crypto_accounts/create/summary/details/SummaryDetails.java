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

import bisq.account.accounts.crypto.CryptoCurrencyAccountPayload;
import bisq.account.payment_method.CryptoPaymentRail;
import bisq.common.monetary.Coin;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.BooleanFormatter;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

public class SummaryDetails<P extends CryptoCurrencyAccountPayload> extends GridPane {
    protected static final String DESCRIPTION_STYLE = "trade-wizard-review-description";
    protected static final String VALUE_STYLE = "trade-wizard-review-value";
    protected static final String DETAILS_STYLE = "trade-wizard-review-details";

    protected final Label headline;
    protected final Region line;

    int rowIndex = 0;

    public SummaryDetails(P accountPayload) {
        super(10, 10);

        GridPaneUtil.setGridPaneMultiColumnsConstraints(this, 3);

        headline = new Label(Res.get("paymentAccounts.crypto.summary.details").toUpperCase());
        headline.getStyleClass().add("trade-wizard-review-details-headline");
        GridPane.setMargin(headline, new Insets(10, 0, 0, 0));
        add(headline, 0, rowIndex, 3, 1);
        line = getLine();
        GridPane.setMargin(line, new Insets(-10, 0, -5, 0));
        add(line, 0, ++rowIndex, 3, 1);

        addDetails(accountPayload);

        addRestrictions(accountPayload);
    }

    protected void addDetails(P accountPayload) {
        String isInstant = BooleanFormatter.toEnabledDisabled(accountPayload.isInstant());
        addDescriptionAndValue(Res.get("paymentAccounts.crypto.isInstant"), isInstant);

        if (accountPayload.getIsAutoConf().isPresent()) {
            if (accountPayload.getIsAutoConf().get()) {
                String autoConfNumConfirmations = String.valueOf(accountPayload.getAutoConfNumConfirmations().orElseThrow());
                String autoConfMaxTradeAmount = AmountFormatter.formatAmountWithCode(Coin.fromValue(accountPayload.getAutoConfMaxTradeAmount().orElseThrow(), "BTC"), true);
                String autoConfExplorerUrls = accountPayload.getAutoConfExplorerUrls().orElseThrow();
                String autoConfValue = Res.get("paymentAccounts.crypto.summary.details.autoConf.value", autoConfNumConfirmations, autoConfMaxTradeAmount, autoConfExplorerUrls);
                Label label = addDescriptionAndValue(Res.get("paymentAccounts.crypto.address.autoConf"), autoConfValue);
                label.setTooltip(new BisqTooltip(autoConfValue));
            } else {
                addDescriptionAndValue(Res.get("paymentAccounts.crypto.address.autoConf"), Res.get("state.disabled"));
            }
        }
    }

    protected void addRestrictions(P accountPayload) {
        CryptoPaymentRail paymentRail = accountPayload.getPaymentMethod().getPaymentRail();
        String restrictions = Res.get("paymentAccounts.summary.tradeLimit", paymentRail.getTradeLimit()) + " / " +
                Res.get("paymentAccounts.summary.tradeDuration", paymentRail.getTradeDuration());
        addDescriptionAndValue(Res.get("paymentAccounts.restrictions"), restrictions);
    }

    protected Label addDescriptionAndValue(String description, String value) {
        addDescriptionLabel(description);
        return addValueLabel(value);
    }

    protected Label addDescriptionLabel(String description) {
        Label label = new Label(description);
        label.getStyleClass().add(DESCRIPTION_STYLE);
        add(label, 0, ++rowIndex);
        return label;
    }

    protected Label addValueLabel(String value) {
        Label label = new Label(value);
        label.getStyleClass().add(VALUE_STYLE);
        add(label, 1, rowIndex, 2, 1);
        return label;
    }

    protected Region getLine() {
        Region line = new Region();
        line.setMinHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: -bisq-border-color-grey");
        line.setPadding(new Insets(9, 0, 8, 0));
        return line;
    }
}