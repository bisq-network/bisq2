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

package bisq.desktop.main.content.user.accounts.create.summary;

import bisq.common.data.Triple;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
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
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
public class PaymentSummaryView extends View<VBox, PaymentSummaryModel, PaymentSummaryController> {
    private final static int FEEDBACK_WIDTH = 700;
    private static final double HEIGHT = 61;
    private final Label paymentMethod, risk, tradeLimit;
    private final GridPane gridPane;
    private int rowIndex = 0;

    public PaymentSummaryView(PaymentSummaryModel model, PaymentSummaryController controller) {
        super(new VBox(), model, controller);

        gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        GridPaneUtil.setGridPaneMultiColumnsConstraints(gridPane, 3);


        Label headline = new Label(Res.get("user.paymentAccounts.summary.review"));
        headline.getStyleClass().add("trade-wizard-review-headline");
        GridPane.setHalignment(headline, HPos.CENTER);
        GridPane.setMargin(headline, new Insets(10, 0, 30, 0));
        gridPane.add(headline, 0, rowIndex, 3, 1);

        rowIndex++;
        gridPane.add(getLine(), 0, rowIndex, 3, 1);

        rowIndex++;
        Triple<Text, Label, VBox> paymentMethodTriple = getDescriptionValueVBoxTriple(Res.get("user.paymentAccounts.summary.paymentMethod"));
        paymentMethod = paymentMethodTriple.getSecond();
        gridPane.add(paymentMethodTriple.getThird(), 0, rowIndex);

        Triple<Text, Label, VBox> riskTriple = getDescriptionValueVBoxTriple(Res.get("user.paymentAccounts.summary.risk"));
        risk = riskTriple.getSecond();
        gridPane.add(riskTriple.getThird(), 1, rowIndex);

        Triple<Text, Label, VBox> tradeLimitTriple = getDescriptionValueVBoxTriple(Res.get("user.paymentAccounts.summary.tradeLimit"));
        tradeLimit = tradeLimitTriple.getSecond();
        gridPane.add(tradeLimitTriple.getThird(), 2, rowIndex);

        rowIndex++;
        Label detailsHeadline = new Label(Res.get("user.paymentAccounts.summary.accountDetails").toUpperCase());
        detailsHeadline.getStyleClass().add("trade-wizard-review-details-headline");
        gridPane.add(detailsHeadline, 0, rowIndex, 3, 1);

        rowIndex++;
        Region line2 = getLine();
        GridPane.setMargin(line2, new Insets(-10, 0, -5, 0));
        gridPane.add(line2, 0, rowIndex, 3, 1);
        rowIndex++;

        VBox.setMargin(gridPane, new Insets(40));
        root.getChildren().addAll(gridPane);
    }

    @Override
    protected void onViewAttached() {
        paymentMethod.setText(model.getPaymentMethod().getDisplayString());
        paymentMethod.setTooltip(new BisqTooltip(model.getPaymentMethod().getDisplayString()));
        risk.setText(model.getRisk());
        tradeLimit.setText(model.getTradeLimit());

        gridPane.add(model.getDataDisplay().getViewRoot(), 0, rowIndex, 3, 1);
    }

    @Override
    protected void onViewDetached() {
        gridPane.getChildren().remove(model.getDataDisplay().getViewRoot());
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
        line.setStyle("-fx-background-color: -bisq-border-color-grey");
        line.setPadding(new Insets(9, 0, 8, 0));
        return line;
    }
}