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

import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public abstract class AccountDetailsVBox extends VBox {
    protected static final String DESCRIPTION_STYLE = "trade-wizard-review-description";
    protected static final String VALUE_STYLE = "trade-wizard-review-value";
    protected static final String DETAILS_STYLE = "trade-wizard-review-details";
    protected static final double DESCRIPTION_WIDTH = 200;
    protected static final double HBOX_SPACE = 10;

    public AccountDetailsVBox() {
        super(10);

        setPadding(new Insets(20));
        getStyleClass().add("bisq-content-bg");
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

    protected MaterialTextArea addTextAreaValueWithCopyButton(String value) {
        MaterialTextArea valueTextArea = new MaterialTextArea(Res.get("user.paymentAccounts.createAccount.accountData.userDefined.accountData"));
        valueTextArea.setText(value);
        valueTextArea.setFixedHeight(180);
        valueTextArea.setEditable(false);
        valueTextArea.showCopyIcon();
        getChildren().add(valueTextArea);
        return valueTextArea;
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

    protected Region getLine() {
        Region line = new Region();
        line.setMinHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: -bisq-border-color-grey");
        line.setPadding(new Insets(9, 0, 8, 0));
        return line;
    }

}
