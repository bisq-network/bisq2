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
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class ProfileCardOverviewView extends View<VBox, ProfileCardOverviewModel, ProfileCardOverviewController> {
    private final Label numOffersAndMessagesLabel, totalBaseOfferAmountToBuyAndSellLabel,
            profileAgeLabel, lastUserActivityLabel, statementLabel,tradeTermsTextArea;

    public ProfileCardOverviewView(ProfileCardOverviewModel model,
                                   ProfileCardOverviewController controller) {
        super(new VBox(), model, controller);

        lastUserActivityLabel = new Label();
        HBox lastUserActivityBox = createAndGetTitleAndDetailsBox("user.profileCard.details.lastUserActivity", lastUserActivityLabel);

        profileAgeLabel = new Label();
        HBox profileAgeBox = createAndGetTitleAndDetailsBox("user.profileCard.details.profileAge", profileAgeLabel);

        numOffersAndMessagesLabel = new Label();
        HBox numOffersAndMessagesBox = createAndGetTitleAndDetailsBox("user.profileCard.overview.numOffersAndMessages", numOffersAndMessagesLabel);

        totalBaseOfferAmountToBuyAndSellLabel = new Label();
        HBox totalBaseOfferAmountToBuyAndSellBox = createAndGetTitleAndDetailsBox("user.profileCard.overview.totalBaseOfferAmountToBuyAndSell", totalBaseOfferAmountToBuyAndSellLabel);

        statementLabel = new Label();
        HBox statementBox = createAndGetTitleAndDetailsBox("user.profileCard.overview.statement", statementLabel);

        tradeTermsTextArea = new Label();
        HBox tradeTermsBox = createAndGetTitleAndDetailsBox("user.profileCard.overview.tradeTerms", tradeTermsTextArea, Optional.empty());

        VBox contentBox = new VBox(20, lastUserActivityBox, profileAgeBox, numOffersAndMessagesBox,
                totalBaseOfferAmountToBuyAndSellBox, statementBox, tradeTermsBox);
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
        numOffersAndMessagesLabel.setText(model.getNumOffers() + " / " + model.getNumPublicTextMessages());
        totalBaseOfferAmountToBuyAndSellLabel.setText(model.getTotalBaseOfferAmountToBuy() + " / " + model.getTotalBaseOfferAmountToSell());
        statementLabel.setText(model.getStatement());
        tradeTermsTextArea.setText(model.getTradeTerms());

        lastUserActivityLabel.textProperty().bind(model.getLastUserActivity());

        root.requestFocus();

    }

    @Override
    protected void onViewDetached() {
        lastUserActivityLabel.textProperty().unbind();
    }

    private HBox createAndGetTitleAndDetailsBox(String title, Label detailsLabel) {
        return createAndGetTitleAndDetailsBox(title, detailsLabel, Optional.empty());
    }

    private HBox createAndGetTitleAndDetailsBox(String title, Label detailsLabel, Optional<BisqMenuItem> button) {
        Label titleLabel = new Label(Res.get(title));
        double width = 200;
        titleLabel.setMaxWidth(width);
        titleLabel.setMinWidth(width);
        titleLabel.setPrefWidth(width);
        titleLabel.getStyleClass().addAll("text-fill-grey-dimmed", "medium-text");

        detailsLabel.getStyleClass().addAll("text-fill-white", "normal-text");

        HBox hBox = new HBox(titleLabel, detailsLabel);
        hBox.setAlignment(Pos.BASELINE_LEFT);

        if (button.isPresent()) {
            button.get().useIconOnly(17);
            HBox.setMargin(button.get(), new Insets(0, 0, 0, 40));
            hBox.getChildren().addAll(Spacer.fillHBox(), button.get());
        }
        return hBox;
    }
}
