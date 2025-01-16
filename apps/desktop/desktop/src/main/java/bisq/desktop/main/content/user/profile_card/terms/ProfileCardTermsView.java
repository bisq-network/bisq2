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

package bisq.desktop.main.content.user.profile_card.terms;

import bisq.desktop.common.view.View;
import bisq.desktop.main.content.user.profile_card.ProfileCardView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProfileCardTermsView extends View<VBox, ProfileCardTermsModel, ProfileCardTermsController> {
    private final Label tradeTermsLabel;

    public ProfileCardTermsView(ProfileCardTermsModel model,
                                ProfileCardTermsController controller) {
        super(new VBox(), model, controller);

        tradeTermsLabel = new Label();
        tradeTermsLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        tradeTermsLabel.setWrapText(true);
        tradeTermsLabel.setAlignment(Pos.TOP_LEFT);

        VBox contentBox = new VBox(20, tradeTermsLabel);
        contentBox.getStyleClass().add("bisq-common-bg");
        contentBox.setAlignment(Pos.TOP_LEFT);
        contentBox.setMinHeight(ProfileCardView.SUB_VIEWS_CONTENT_HEIGHT);
        contentBox.setPrefHeight(ProfileCardView.SUB_VIEWS_CONTENT_HEIGHT);
        contentBox.setMaxHeight(ProfileCardView.SUB_VIEWS_CONTENT_HEIGHT);

        root.getChildren().add(contentBox);
        root.setPadding(new Insets(20, 0, 20, 0));
        root.getStyleClass().add("overview");
    }

    @Override
    protected void onViewAttached() {
        tradeTermsLabel.textProperty().bind(model.getTradeTerms());
    }

    @Override
    protected void onViewDetached() {
        tradeTermsLabel.textProperty().unbind();
    }
}
