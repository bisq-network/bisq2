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

import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.crypto.CryptoAssetAccount;
import bisq.account.accounts.crypto.CryptoAssetAccountPayload;
import bisq.common.data.Triple;
import bisq.common.monetary.Coin;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.BooleanFormatter;
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
public abstract class AccountDetails<A extends CryptoAssetAccount<?>> extends VBox {
    protected static final String DESCRIPTION_STYLE = "trade-wizard-review-description";
    protected static final String VALUE_STYLE = "trade-wizard-review-value";
    protected static final String DETAILS_STYLE = "trade-wizard-review-details";
    protected static final double DESCRIPTION_WIDTH = 200;
    protected static final double HBOX_SPACE = 10;
    protected static final double HEIGHT = 61;

    protected final GridPane gridPane;
    protected int rowIndex = 0;
    protected Label addressDescriptionLabel;

    public AccountDetails(A account) {
        super(10);

        setPadding(new Insets(20));
        getStyleClass().add("bisq-content-bg");

        gridPane = new GridPane(10, 10);
        GridPaneUtil.setGridPaneMultiColumnsConstraints(gridPane, 3);

        addHeader(account);

        Label detailsHeadline = new Label(Res.get("paymentAccounts.accountDetails").toUpperCase());
        detailsHeadline.getStyleClass().add("trade-wizard-review-details-headline");
        gridPane.add(detailsHeadline, 0, ++rowIndex, 3, 1);

        Region detailsLine = getLine();
        GridPane.setMargin(detailsLine, new Insets(-10, 0, -5, 0));
        gridPane.add(detailsLine, 0, ++rowIndex, 3, 1);

        addDetails(account);

        Label limitsHeadline = new Label(Res.get("paymentAccounts.restrictions").toUpperCase());
        limitsHeadline.getStyleClass().add("trade-wizard-review-details-headline");
        GridPane.setMargin(limitsHeadline, new Insets(20, 0, 0, 0));
        gridPane.add(limitsHeadline, 0, ++rowIndex, 3, 1);
        Region limitsLine = getLine();
        GridPane.setMargin(limitsLine, new Insets(-10, 0, -5, 0));
        gridPane.add(limitsLine, 0, ++rowIndex, 3, 1);

        addRestrictions(account);

        getChildren().add(gridPane);
    }

    protected void addDetails(A account) {
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
    }

    protected void addRestrictions(A account) {
        addDescriptionAndValue(Res.get("paymentAccounts.tradeLimit"),
                account.getPaymentMethod().getPaymentRail().getTradeLimit());
        addDescriptionAndValue(Res.get("paymentAccounts.tradeDuration"),
                account.getPaymentMethod().getPaymentRail().getTradeDuration());
    }

    protected void addHeader(A account) {
        AccountPayload<?> accountPayload = account.getAccountPayload();

        Triple<Text, Label, VBox> currencyTriple = getDescriptionValueVBoxTriple(Res.get("paymentAccounts.currency"),
                account.getPaymentMethod().getDisplayString());
        gridPane.add(currencyTriple.getThird(), 0, rowIndex);
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
