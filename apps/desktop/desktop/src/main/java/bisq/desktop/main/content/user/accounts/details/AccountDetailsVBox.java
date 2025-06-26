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

package bisq.desktop.main.content.user.accounts.details;

import bisq.account.accounts.Account;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.common.data.Triple;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AccountDetailsVBox extends VBox {
    protected static final String DESCRIPTION_STYLE = "trade-wizard-review-description";
    protected static final String VALUE_STYLE = "trade-wizard-review-value";
    protected static final String DETAILS_STYLE = "trade-wizard-review-details";
    protected static final double DESCRIPTION_WIDTH = 200;
    protected static final double HBOX_SPACE = 10;
    private static final double HEIGHT = 61;

    public AccountDetailsVBox(Account<?, ?> account) {
        super(10);

        setPadding(new Insets(20));
        getStyleClass().add("bisq-content-bg");

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        GridPaneUtil.setGridPaneMultiColumnsConstraints(gridPane, 3);
        int rowIndex = 0;

        Triple<Text, Label, VBox> paymentMethodTriple = getDescriptionValueVBoxTriple(Res.get("user.paymentAccounts.summary.paymentMethod"),
                account.getPaymentMethod().getDisplayString());
        Label paymentMethod = paymentMethodTriple.getSecond();
        gridPane.add(paymentMethodTriple.getThird(), 0, rowIndex);

        if (account.getPaymentMethod().getPaymentRail() instanceof FiatPaymentRail fiatPaymentRail) {

            Triple<Text, Label, VBox> riskTriple = getDescriptionValueVBoxTriple(Res.get("user.paymentAccounts.summary.risk"),
                    fiatPaymentRail.getChargebackRisk().getDisplayString());
            Label risk = riskTriple.getSecond();
            gridPane.add(riskTriple.getThird(), 1, rowIndex);

            Triple<Text, Label, VBox> tradeLimitTriple = getDescriptionValueVBoxTriple(Res.get("user.paymentAccounts.summary.tradeLimit"),
                    fiatPaymentRail.getTradeLimit());
            Label tradeLimit = tradeLimitTriple.getSecond();
            gridPane.add(tradeLimitTriple.getThird(), 2, rowIndex);
        }


        rowIndex++;
        Label detailsHeadline = new Label(Res.get("user.paymentAccounts.summary.accountDetails").toUpperCase());
        detailsHeadline.getStyleClass().add("trade-wizard-review-details-headline");
        gridPane.add(detailsHeadline, 0, rowIndex, 3, 1);

        rowIndex++;
        Region line2 = getLine();
        GridPane.setMargin(line2, new Insets(-10, 0, -5, 0));
        gridPane.add(line2, 0, rowIndex, 3, 1);

        getChildren().add(gridPane);

        String currency = account.getSupportedCurrencyDisplayNameAndCodeAsDisplayString();

        Label label = addDescriptionAndValue(Res.get("user.paymentAccounts.currency"), account.getSupportedCurrencyCodesAsDisplayString());
        if (currency.length() > 90) {
            label.setTooltip(new BisqTooltip(currency));
        }
    }

    protected Label addDescriptionAndValue(String description, String value) {
        Label descriptionLabel = getDescriptionLabel(description);
        Label valueLabel = getValueLabel(value);
        getChildren().add(new HBox(HBOX_SPACE, descriptionLabel, valueLabel));
        return valueLabel;
    }

    protected Label addDescriptionAndValueWithCopyButton(String description, String value) {
        Label descriptionLabel = getDescriptionLabel(description);
        Label valueLabel = getValueLabel(value);
        BisqMenuItem copyButton = getBisqMenuItem();
        HBox hBox = new HBox(HBOX_SPACE, descriptionLabel, valueLabel, Spacer.fillHBox(), copyButton);
        hBox.setAlignment(Pos.BASELINE_LEFT);
        getChildren().add(hBox);
        return valueLabel;
    }

    protected MaterialTextArea addTextAreaValueWithCopyButton(String description, String value) {
        Label descriptionLabel = getDescriptionLabel(description);
        MaterialTextArea valueLabel = new MaterialTextArea();
        valueLabel.setText(value);
        valueLabel.setFixedHeight(180);
        valueLabel.setEditable(false);
        valueLabel.showCopyIcon();
        HBox hBox = new HBox(HBOX_SPACE, descriptionLabel, valueLabel);
        hBox.setAlignment(Pos.BASELINE_LEFT);
        getChildren().add(hBox);
        return valueLabel;
    }

    protected Label getDescriptionLabel(String description) {
        Label label = new Label(description);
        label.getStyleClass().add(DESCRIPTION_STYLE);
        label.setMinWidth(DESCRIPTION_WIDTH);
        label.setMaxWidth(DESCRIPTION_WIDTH);
        return label;
    }

    protected Label getValueLabel(String value) {
        Label label = new Label(value);
        label.getStyleClass().add(VALUE_STYLE);
        return label;
    }

    protected BisqMenuItem getBisqMenuItem() {
        return getBisqMenuItem(Res.get("action.copyToClipboard"));
    }

    protected BisqMenuItem getBisqMenuItem(String tooltip) {
        BisqMenuItem bisqMenuItem = new BisqMenuItem("copy-grey", "copy-white");
        bisqMenuItem.setTooltip(tooltip);
        bisqMenuItem.useIconOnly(17);
        return bisqMenuItem;
    }

    private Triple<Text, Label, VBox> getDescriptionValueVBoxTriple(String description, String value) {
        Text descriptionLabel = description == null ? new Text() : new Text(description.toUpperCase());
        descriptionLabel.getStyleClass().add("bisq-easy-trade-wizard-review-header-description");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("bisq-easy-trade-wizard-review-header-value");
        valueLabel.setMaxWidth(250);

        VBox.setVgrow(valueLabel, Priority.ALWAYS);
        VBox.setMargin(valueLabel, new Insets(-2, 0, 0, 0));
        VBox vBox = new VBox(0, descriptionLabel, valueLabel);
        vBox.setFillWidth(true);
        vBox.setAlignment(Pos.TOP_LEFT);
        vBox.setMinHeight(HEIGHT);
        vBox.setMaxHeight(HEIGHT);
        return new Triple<>(descriptionLabel, valueLabel, vBox);
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
