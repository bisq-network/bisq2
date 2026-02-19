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

package bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary;

import bisq.account.accounts.MultiCurrencyAccountPayload;
import bisq.account.accounts.fiat.CountryBasedAccountPayload;
import bisq.common.data.Triple;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;

@Slf4j
public class PaymentSummaryView extends View<StackPane, PaymentSummaryModel, PaymentSummaryController> {
    private final static int FEEDBACK_WIDTH = 700;
    private static final double TOP_PANE_HEIGHT = 55;
    private static final double HEIGHT = 61;

    private final Label paymentMethod, currency, country;
    private final GridPane gridPane;
    private final Button accountNameButton;
    private final MaterialTextField accountNameField;
    private final VBox accountNameOverlay, currencyVBox, countryVBox;
    private Subscription showAccountNameOverlayPin;

    public PaymentSummaryView(PaymentSummaryModel model, PaymentSummaryController controller) {
        super(new StackPane(), model, controller);

        gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        GridPaneUtil.setGridPaneMultiColumnsConstraints(gridPane, 3);

        Label headline = new Label(Res.get("paymentAccounts.summary.headline"));
        headline.getStyleClass().add("trade-wizard-review-headline");
        GridPane.setHalignment(headline, HPos.CENTER);
        GridPane.setMargin(headline, new Insets(20, 0, 20, 0));
        int rowIndex = 0;
        gridPane.add(headline, 0, rowIndex, 3, 1);

        rowIndex++;
        gridPane.add(getLine(), 0, rowIndex, 3, 1);

        rowIndex++;
        Triple<Text, Label, VBox> paymentMethodTriple = getDescriptionValueVBoxTriple(Res.get("paymentAccounts.paymentMethod"));
        paymentMethod = paymentMethodTriple.getSecond();
        gridPane.add(paymentMethodTriple.getThird(), 0, rowIndex);

        Triple<Text, Label, VBox> currencyTriple = getDescriptionValueVBoxTriple(Res.get("paymentAccounts.currency"));
        currency = currencyTriple.getSecond();
        currencyVBox = currencyTriple.getThird();
        gridPane.add(currencyVBox, 1, rowIndex);

        Triple<Text, Label, VBox> countryTriple = getDescriptionValueVBoxTriple(Res.get("paymentAccounts.country"));
        country = countryTriple.getSecond();
        countryVBox = countryTriple.getThird();
        gridPane.add(countryVBox, 2, rowIndex);

        accountNameField = new MaterialTextField(Res.get("paymentAccounts.summary.accountNameOverlay.accountName.description"));
        accountNameField.setValidator(model.getAccountNameValidator());
        accountNameButton = new Button(Res.get("paymentAccounts.summary.accountNameOverlay.button"));
        accountNameOverlay = new VBox(20);
        configAccountNameOverlay();

        VBox.setMargin(gridPane, new Insets(40));
        StackPane.setMargin(accountNameOverlay, new Insets(-TOP_PANE_HEIGHT, 0, 0, 0));
        root.getChildren().addAll(gridPane, accountNameOverlay);
    }

    @Override
    protected void onViewAttached() {
        accountNameButton.disableProperty().bind(accountNameField.textProperty().isEmpty());

        accountNameButton.setOnAction(e -> {
            accountNameField.validate();
            controller.onCreateAccount(accountNameField.getText());
        });

        showAccountNameOverlayPin = EasyBind.subscribe(model.getShowAccountNameOverlay(),
                show -> {
                    accountNameOverlay.setVisible(show);
                    if (show) {
                        Transitions.blurStrong(gridPane, 0);
                        Transitions.slideInTop(accountNameOverlay, 450);
                        accountNameOverlay.requestFocus();
                    } else {
                        Transitions.removeEffect(gridPane);
                    }
                });

        accountNameField.setText(model.getDefaultAccountName());
        paymentMethod.setText(model.getPaymentMethod().getShortDisplayString());
        currency.setText(model.getCurrencyString());
        country.setText(model.getCountry());

        boolean showCurrency = !(model.getAccountPayload() instanceof MultiCurrencyAccountPayload);
        currencyVBox.setVisible(showCurrency);
        currencyVBox.setManaged(showCurrency);

        boolean isCountryBasedAccountPayload = model.getAccountPayload() instanceof CountryBasedAccountPayload;
        countryVBox.setVisible(isCountryBasedAccountPayload);
        countryVBox.setManaged(isCountryBasedAccountPayload);

        gridPane.add(model.getAccountDetailsGridPane(), 0, gridPane.getRowCount() + 1, 3, 1);
    }

    @Override
    protected void onViewDetached() {
        accountNameButton.disableProperty().unbind();

        accountNameButton.setOnAction(null);

        showAccountNameOverlayPin.unsubscribe();

        gridPane.getChildren().remove(model.getAccountDetailsGridPane());
    }

    private Triple<Label, Button, VBox> getAccountNameElements(@Nullable String description) {
        Text descriptionLabel = description == null ? new Text() : new Text(description.toUpperCase());
        descriptionLabel.getStyleClass().add("bisq-easy-trade-wizard-review-header-description");

        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("bisq-easy-trade-wizard-review-header-value");
        valueLabel.setMaxWidth(250);

        Button editButton = BisqIconButton.createIconButton(AwesomeIcon.EDIT_SIGN);
        HBox hBox = new HBox(valueLabel, Spacer.fillHBox(), editButton);

        VBox.setVgrow(valueLabel, Priority.ALWAYS);
        VBox.setMargin(valueLabel, new Insets(-2, 0, 0, 0));
        VBox vBox = new VBox(0, descriptionLabel, hBox);
        vBox.setFillWidth(true);
        vBox.setAlignment(Pos.TOP_LEFT);
        vBox.setMinHeight(HEIGHT);
        vBox.setMaxHeight(HEIGHT);
        return new Triple<>(valueLabel, editButton, vBox);
    }

    private Triple<Text, Label, VBox> getDescriptionValueVBoxTriple(@Nullable String description) {
        Text descriptionLabel = description == null ? new Text() : new Text(description.toUpperCase());
        descriptionLabel.getStyleClass().add("bisq-easy-trade-wizard-review-header-description");
        Label valueLabel = new Label();
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

    private Region getLine() {
        Region line = new Region();
        line.setMinHeight(1);
        line.setMaxHeight(1);
        line.getStyleClass().add("separator-line");
        line.setPadding(new Insets(9, 0, 8, 0));
        return line;
    }

    private void configAccountNameOverlay() {
        VBox contentBox = getFeedbackContentBox();

        accountNameOverlay.setVisible(false);
        accountNameOverlay.setAlignment(Pos.TOP_CENTER);

        Label headlineLabel = new Label(Res.get("paymentAccounts.summary.accountNameOverlay.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("paymentAccounts.summary.accountNameOverlay.subTitle"));
        configFeedbackSubtitleLabel(subtitleLabel);

        accountNameButton.setDefaultButton(true);
        VBox.setMargin(accountNameButton, new Insets(10, 0, 0, 0));
        contentBox.getChildren().addAll(headlineLabel, subtitleLabel, accountNameField, accountNameButton);
        accountNameOverlay.getChildren().addAll(contentBox, Spacer.fillVBox());
    }

    private VBox getFeedbackContentBox() {
        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.getStyleClass().setAll("trade-wizard-feedback-bg");
        contentBox.setPadding(new Insets(30));
        contentBox.setMaxWidth(FEEDBACK_WIDTH);
        return contentBox;
    }

    private void configFeedbackSubtitleLabel(Label subtitleLabel) {
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setMinWidth(FEEDBACK_WIDTH - 200);
        subtitleLabel.setMaxWidth(subtitleLabel.getMinWidth());
        subtitleLabel.setWrapText(true);
        subtitleLabel.getStyleClass().add("bisq-text-21");
    }
}