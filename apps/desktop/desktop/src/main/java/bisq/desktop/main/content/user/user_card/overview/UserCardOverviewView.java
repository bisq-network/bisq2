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

package bisq.desktop.main.content.user.user_card.overview;

import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserCardOverviewView extends View<VBox, UserCardOverviewModel, UserCardOverviewController> {
    private final Label statementLabel, tradeTermsLabel;

    public UserCardOverviewView(UserCardOverviewModel model,
                                UserCardOverviewController controller) {
        super(new VBox(), model, controller);

        // Statement
        statementLabel = new Label();
        VBox statementBox = createAndGetTitleAndDetailsBox("user.userCard.overview.statement", statementLabel, 30);

        // Trade terms
        tradeTermsLabel = new Label();
        VBox tradeTermsBox = createAndGetTitleAndDetailsBox("user.userCard.overview.tradeTerms", tradeTermsLabel, 100);

        VBox contentBox = new VBox(20, statementBox, tradeTermsBox);
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
        statementLabel.textProperty().bind(model.getStatement());
        tradeTermsLabel.textProperty().bind(model.getTradeTerms());
    }

    @Override
    protected void onViewDetached() {
        statementLabel.textProperty().unbind();
        tradeTermsLabel.textProperty().unbind();
    }

    private VBox createAndGetTitleAndDetailsBox(String title, Label detailsLabel, double height) {
        Label titleLabel = new Label(Res.get(title));
        titleLabel.getStyleClass().addAll("text-fill-grey-dimmed", "title");
        detailsLabel.getStyleClass().addAll("text-fill-white", "normal-text", "details");
        detailsLabel.setWrapText(true);
        detailsLabel.setMinHeight(height);
        detailsLabel.setPrefHeight(height);
        detailsLabel.setMaxHeight(height);
        detailsLabel.setAlignment(Pos.TOP_LEFT);
        VBox vBox = new VBox(10, titleLabel, detailsLabel);
        return vBox;
    }
}
