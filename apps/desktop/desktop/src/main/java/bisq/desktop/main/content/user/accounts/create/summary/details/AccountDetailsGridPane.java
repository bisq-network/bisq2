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

package bisq.desktop.main.content.user.accounts.create.summary.details;

import bisq.account.accounts.AccountPayload;
import bisq.account.payment_method.PaymentRail;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

public abstract class AccountDetailsGridPane<A extends AccountPayload<?>, R extends PaymentRail> extends GridPane {
    protected static final String DESCRIPTION_STYLE = "trade-wizard-review-description";
    protected static final String VALUE_STYLE = "trade-wizard-review-value";
    protected static final String DETAILS_STYLE = "trade-wizard-review-details";

    protected final Label detailsHeadline;
    protected final Region detailsLine;

    int rowIndex = 0;

    public AccountDetailsGridPane(A accountPayload, R paymentRail) {
        super(10, 10);

        GridPaneUtil.setGridPaneMultiColumnsConstraints(this, 3);

        detailsHeadline = new Label(Res.get("user.paymentAccounts.accountDetails").toUpperCase());
        detailsHeadline.getStyleClass().add("trade-wizard-review-details-headline");
        GridPane.setMargin(detailsHeadline, new Insets(10, 0, 0, 0));
        add(detailsHeadline, 0, rowIndex, 3, 1);
        detailsLine = getLine();
        GridPane.setMargin(detailsLine, new Insets(-10, 0, -5, 0));
        add(detailsLine, 0, ++rowIndex, 3, 1);

        addDetails(accountPayload);

       /* Region restrictionsLine = getLine();
        GridPane.setMargin(restrictionsLine, new Insets(-10, 0, -5, 0));
        add(restrictionsLine, 0, ++rowIndex, 3, 1);*/

        addRestrictions(paymentRail);
    }

    protected abstract void addDetails(A accountPayload);

    protected void addRestrictions(R fiatPaymentRail) {
        String restrictions = Res.get("user.paymentAccounts.summary.tradeLimit", fiatPaymentRail.getTradeLimit()) + " / " +
                Res.get("user.paymentAccounts.summary.tradeDuration", fiatPaymentRail.getTradeDuration());
        addDescriptionAndValue(Res.get("user.paymentAccounts.restrictions"), restrictions);
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
        Label label = getValueLabel(value);
        add(label, 1, rowIndex, 2, 1);
        return label;
    }

    protected static Label getValueLabel(String value) {
        Label label = new Label(value);
        label.getStyleClass().add(VALUE_STYLE);
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