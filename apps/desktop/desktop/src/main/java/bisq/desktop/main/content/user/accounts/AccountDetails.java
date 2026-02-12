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

package bisq.desktop.main.content.user.accounts;

import bisq.account.accounts.Account;
import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.MultiCurrencyAccountPayload;
import bisq.account.accounts.SelectableCurrencyAccountPayload;
import bisq.account.accounts.SingleCurrencyAccountPayload;
import bisq.account.payment_method.PaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.timestamp.AccountTimestampService;
import bisq.common.asset.FiatCurrencyRepository;
import bisq.common.data.Triple;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.i18n.Res;
import bisq.presentation.formatters.TimeFormatter;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AccountDetails<A extends Account<?, ?>, R extends PaymentRail> extends VBox {
    protected static final String DESCRIPTION_STYLE = "trade-wizard-review-description";
    protected static final String VALUE_STYLE = "trade-wizard-review-value";
    protected static final String DETAILS_STYLE = "trade-wizard-review-details";
    protected static final double DESCRIPTION_WIDTH = 200;
    protected static final double HBOX_SPACE = 10;
    protected static final double HEIGHT = 61;

    protected final A account;
    protected final AccountTimestampService accountTimestampService;
    protected final GridPane gridPane;
    protected int rowIndex = 0;
    protected Pin accountTimestampByHashPin;

    public AccountDetails(A account, AccountTimestampService accountTimestampService) {
        super(10);
        this.account = account;
        this.accountTimestampService = accountTimestampService;

        gridPane = createGridPane();
        setupRoot();

        addHeader();

        addDetailsHeadline();
        addDetails();

        addRestrictionsHeadline();
        addRestrictions();
    }

    public void dispose() {
        if (accountTimestampByHashPin != null) {
            accountTimestampByHashPin.unbind();
            accountTimestampByHashPin = null;
        }
    }

    protected GridPane createGridPane() {
        GridPane gridPane = new GridPane(10, 10);
        GridPaneUtil.setGridPaneMultiColumnsConstraints(gridPane, 3);
        return gridPane;
    }

    protected void setupRoot() {
        setPadding(new Insets(20));
        getStyleClass().add("bisq-content-bg");
        getChildren().add(gridPane);
    }

    protected void addHeader() {
        if (account.getPaymentMethod().getPaymentRail() instanceof FiatPaymentRail fiatPaymentRail) {
            Triple<Text, Label, VBox> paymentMethodTriple = getDescriptionValueVBoxTriple(Res.get("paymentAccounts.paymentMethod"),
                    account.getPaymentMethod().getDisplayString());
            gridPane.add(paymentMethodTriple.getThird(), 0, rowIndex);

            if (!(account.getAccountPayload() instanceof MultiCurrencyAccountPayload)) {
                addCurrencyDisplay();
            }
        }
    }

    protected void addDetailsHeadline() {
        Label detailsHeadline = new Label(Res.get("paymentAccounts.accountDetails").toUpperCase());
        detailsHeadline.getStyleClass().add("trade-wizard-review-details-headline");
        gridPane.add(detailsHeadline, 0, ++rowIndex, 3, 1);

        Region detailsLine = getLine();
        GridPane.setMargin(detailsLine, new Insets(-10, 0, -5, 0));
        gridPane.add(detailsLine, 0, ++rowIndex, 3, 1);
    }

    protected void addDetails() {
        Label accountAgeLabel = addDescriptionAndValue(Res.get("paymentAccounts.accountAge"), Res.get("data.na"));
        accountTimestampByHashPin = accountTimestampService.getAccountTimestampByHash().addObserver(() -> {
            accountTimestampService.findAccountTimestamp(account)
                    .ifPresent(date -> UIThread.run(() -> {
                        String accountAge = TimeFormatter.formatAgeInDays(date);
                        accountAgeLabel.setText(accountAge);
                    }));
        });
    }

    protected void addRestrictionsHeadline() {
        Label restrictionsHeadline = new Label(Res.get("paymentAccounts.restrictions").toUpperCase());
        restrictionsHeadline.getStyleClass().add("trade-wizard-review-details-headline");
        GridPane.setMargin(restrictionsHeadline, new Insets(20, 0, 0, 0));
        gridPane.add(restrictionsHeadline, 0, ++rowIndex, 3, 1);

        Region restrictionsLine = getLine();
        GridPane.setMargin(restrictionsLine, new Insets(-10, 0, -5, 0));
        gridPane.add(restrictionsLine, 0, ++rowIndex, 3, 1);
    }

    protected void addRestrictions() {
        addDescriptionAndValue(Res.get("paymentAccounts.tradeLimit"),
                account.getPaymentMethod().getPaymentRail().getTradeLimit());
        addDescriptionAndValue(Res.get("paymentAccounts.tradeDuration"),
                account.getPaymentMethod().getPaymentRail().getTradeDuration());
    }

    protected void addCurrencyDisplay() {
        AccountPayload<?> accountPayload = account.getAccountPayload();
        String currencyString = switch (accountPayload) {
            case MultiCurrencyAccountPayload multiCurrencyAccountPayload ->
                    FiatCurrencyRepository.getCodeAndDisplayNames(multiCurrencyAccountPayload.getSelectedCurrencyCodes());
            case SelectableCurrencyAccountPayload selectableCurrencyAccountPayload ->
                    FiatCurrencyRepository.getCodeAndDisplayName(selectableCurrencyAccountPayload.getSelectedCurrencyCode());
            case SingleCurrencyAccountPayload singleCurrencyAccountPayload ->
                    FiatCurrencyRepository.getCodeAndDisplayName(singleCurrencyAccountPayload.getCurrencyCode());
            case null, default -> {
                String type = accountPayload != null ? accountPayload.getClass().getSimpleName() : "null";
                throw new UnsupportedOperationException("accountPayload of unexpected type: " + type);
            }
        };

        Triple<Text, Label, VBox> currencyTriple = getDescriptionValueVBoxTriple(Res.get("paymentAccounts.currency"),
                currencyString);
        gridPane.add(currencyTriple.getThird(), 1, rowIndex);
    }

    protected Label addDescriptionAndValue(String description, String value) {
        addDescriptionLabel(description);
        return addValueLabel(value);
    }

    protected Label addDescriptionAndValueWithCopyButton(String description, String value) {
        return addDescriptionAndValueWithCopyButton(description, value, value);
    }

    protected Label addDescriptionAndValueWithCopyButton(String description, String value, String valueForCopy) {
        addDescriptionLabel(description);
        Label valueLabel = addValueLabel(value);
        GridPane.setMargin(valueLabel, new Insets(0, 20, 0, 0));
        addCopyButton(valueForCopy);
        return valueLabel;
    }

    protected Label addDescriptionLabel(String description) {
        Label label = new Label(description);
        label.getStyleClass().add(DESCRIPTION_STYLE);
        label.setMinWidth(DESCRIPTION_WIDTH);
        label.setMaxWidth(DESCRIPTION_WIDTH);
        gridPane.add(label, 0, ++rowIndex);
        return label;
    }

    protected Label addValueLabel(String value) {
        Label label = new Label(value);
        label.getStyleClass().add(VALUE_STYLE);
        gridPane.add(label, 1, rowIndex, 2, 1);
        return label;
    }

    protected BisqMenuItem addCopyButton(String value) {
        BisqMenuItem copyButton = new BisqMenuItem("copy-grey", "copy-white");
        copyButton.setTooltip(Res.get("action.copyToClipboard"));
        copyButton.useIconOnly(17);
        copyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(value));
        GridPane.setHalignment(copyButton, HPos.RIGHT);
        GridPane.setValignment(copyButton, VPos.CENTER);
        gridPane.add(copyButton, 1, rowIndex, 3, 1);
        return copyButton;
    }

    protected Triple<Text, Label, VBox> getDescriptionValueVBoxTriple(String description, String value) {
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
