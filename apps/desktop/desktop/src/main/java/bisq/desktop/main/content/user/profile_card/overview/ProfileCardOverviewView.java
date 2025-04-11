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

package bisq.desktop.main.content.user.profile_card.overview;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BitcoinAmountDisplay;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProfileCardOverviewView extends View<VBox, ProfileCardOverviewModel, ProfileCardOverviewController> {
    private final Label profileAgeLabel, lastUserActivityLabel, statementLabel, sellingLimitLabel, tradeTermsTextArea;
    private final BitcoinAmountDisplay totalBaseToBuyBitcoinAmountDisplay, totalBaseToSellBitcoinAmountDisplay;

    public ProfileCardOverviewView(ProfileCardOverviewModel model,
                                   ProfileCardOverviewController controller) {
        super(new VBox(), model, controller);

        lastUserActivityLabel = new Label();
        VBox lastUserActivityBox = createAndGetTitleAndMetricBox("user.profileCard.details.lastUserActivity", lastUserActivityLabel);

        profileAgeLabel = new Label();
        VBox profileAgeBox = createAndGetTitleAndMetricBox("user.profileCard.details.profileAge", profileAgeLabel);

        totalBaseToBuyBitcoinAmountDisplay = new BitcoinAmountDisplay();
        configureBitcoinAmountDisplay(totalBaseToBuyBitcoinAmountDisplay);
        VBox totalBaseToBuyBox = createAndGetTitleAndBtcMetricBox("user.profileCard.overview.totalBuying", totalBaseToBuyBitcoinAmountDisplay);

        totalBaseToSellBitcoinAmountDisplay = new BitcoinAmountDisplay();
        configureBitcoinAmountDisplay(totalBaseToSellBitcoinAmountDisplay);
        VBox totalBaseToSellBox = createAndGetTitleAndBtcMetricBox("user.profileCard.overview.totalSelling", totalBaseToSellBitcoinAmountDisplay);

        sellingLimitLabel = new Label();
        Label sellingLimitUnitLabel = new Label("USD");
        VBox sellingLimitBox = createAndGetTitleAndMetricBox(sellingLimitLabel, sellingLimitUnitLabel);

        HBox metricsHBox = new HBox(
                lastUserActivityBox,
                Spacer.fillHBox(),
                profileAgeBox,
                Spacer.fillHBox(),
                totalBaseToBuyBox,
                Spacer.fillHBox(),
                totalBaseToSellBox,
                Spacer.fillHBox(),
                sellingLimitBox);
        metricsHBox.setAlignment(Pos.BASELINE_CENTER);

        statementLabel = new Label();
        VBox statementBox = createAndGetTitleAndDetailsBox("user.profileCard.overview.statement", statementLabel, 20);

        tradeTermsTextArea = new Label();
        VBox tradeTermsBox = createAndGetTitleAndDetailsBox("user.profileCard.overview.tradeTerms", tradeTermsTextArea, 80);

        VBox contentBox = new VBox(20, metricsHBox, getLine(), statementBox, tradeTermsBox);
        contentBox.getStyleClass().add("bisq-common-bg");
        contentBox.setAlignment(Pos.TOP_LEFT);
        contentBox.setMinHeight(307);
        contentBox.setPrefHeight(307);
        contentBox.setMaxHeight(307);

        root.getChildren().add(contentBox);
        root.setPadding(new Insets(20, 0, 20, 0));
        root.getStyleClass().add("overview");
    }

    @Override
    protected void onViewAttached() {
        profileAgeLabel.setText(model.getProfileAge());
        totalBaseToBuyBitcoinAmountDisplay.setBtcAmount(model.getTotalBaseOfferAmountToBuy());
        totalBaseToSellBitcoinAmountDisplay.setBtcAmount(model.getTotalBaseOfferAmountToSell());
        sellingLimitLabel.setText(model.getSellingLimit());
        statementLabel.setText(model.getStatement());
        tradeTermsTextArea.setText(model.getTradeTerms());

        lastUserActivityLabel.textProperty().bind(model.getLastUserActivity());

        root.requestFocus();
    }

    @Override
    protected void onViewDetached() {
        lastUserActivityLabel.textProperty().unbind();
    }

    private void configureBitcoinAmountDisplay(BitcoinAmountDisplay bitcoinAmountDisplay) {
        bitcoinAmountDisplay.getBtcCode().getStyleClass().clear();
        bitcoinAmountDisplay.getBtcCode().setStyle("-fx-fill: -fx-mid-text-color;\n" +
                "-fx-text-fill: -fx-mid-text-color;");
        bitcoinAmountDisplay.getBtcCode().getStyleClass().addAll("text-fill-grey-dimmed", "medium-text");
        bitcoinAmountDisplay.getLeadingZeros().getStyleClass().addAll("text-fill-grey-dimmed", "metric");
        bitcoinAmountDisplay.getSignificantDigits().getStyleClass().addAll("text-fill-white", "metric");
        bitcoinAmountDisplay.getIntegerPart().getStyleClass().addAll("text-fill-grey-dimmed", "metric");
        bitcoinAmountDisplay.applySmallCompactConfig();
    }

    private VBox createAndGetTitleAndMetricBox(String title, Label detailsLabel) {
        Label titleLabel = new Label(Res.get(title).toUpperCase());
        titleLabel.getStyleClass().addAll("text-fill-grey-dimmed", "compact-text", "font-light");
        detailsLabel.getStyleClass().addAll("text-fill-white", "metric");
        VBox vBox = new VBox(titleLabel, detailsLabel);
        vBox.setAlignment(Pos.CENTER);
        return vBox;
    }

    private VBox createAndGetTitleAndBtcMetricBox(String title, BitcoinAmountDisplay bitcoinAmountDisplay) {
        Label titleLabel = new Label(Res.get(title).toUpperCase());
        titleLabel.getStyleClass().addAll("text-fill-grey-dimmed", "compact-text", "font-light");

        VBox vBox = new VBox(titleLabel, bitcoinAmountDisplay);
        vBox.setAlignment(Pos.CENTER);
        return vBox;
    }

    private VBox createAndGetTitleAndMetricBox(Label detailsLabel, Label unitLabel) {
        Label titleLabel = new Label(Res.get("user.profileCard.overview.sellingLimit").toUpperCase());
        titleLabel.getStyleClass().addAll("text-fill-grey-dimmed", "compact-text", "font-light");
        detailsLabel.getStyleClass().addAll("text-fill-white", "metric");
        unitLabel.getStyleClass().addAll("text-fill-grey-dimmed", "medium-text");
        HBox detailsAndUnitHBox = new HBox(3, detailsLabel, unitLabel);
        detailsAndUnitHBox.setAlignment(Pos.BASELINE_CENTER);
        VBox vBox = new VBox(titleLabel, detailsAndUnitHBox);
        vBox.setAlignment(Pos.CENTER);
        return vBox;
    }

    private Region getLine() {
        Region line = new Region();
        line.setMinHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: -bisq-border-color-grey");
        line.setPadding(new Insets(9, 0, 8, 0));
        return line;
    }

    private VBox createAndGetTitleAndDetailsBox(String title, Label detailsLabel, double height) {
        Label titleLabel = new Label(Res.get(title).toUpperCase());
        titleLabel.getStyleClass().addAll("text-fill-grey-dimmed", "compact-text", "font-light");
        detailsLabel.getStyleClass().addAll("text-fill-white", "normal-text");
        detailsLabel.setWrapText(true);
        detailsLabel.setMinHeight(Label.USE_PREF_SIZE);
        detailsLabel.setAlignment(Pos.TOP_LEFT);
        ScrollPane detailsScrollPane = new ScrollPane(detailsLabel);
        detailsScrollPane.setMinHeight(height);
        detailsScrollPane.setMaxHeight(height);
        detailsScrollPane.setPrefHeight(height);
        detailsScrollPane.setFitToWidth(true);
        detailsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        return new VBox(3, titleLabel, detailsScrollPane);
    }
}