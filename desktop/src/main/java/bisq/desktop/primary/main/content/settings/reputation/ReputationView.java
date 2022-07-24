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

package bisq.desktop.primary.main.content.settings.reputation;

import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReputationView extends View<VBox, ReputationModel, ReputationController> {
    private final Button burnBsqButton, bsqBondButton;
    private final Hyperlink learnMore;

    public ReputationView(ReputationModel model,
                          ReputationController controller) {
        super(new VBox(), model, controller);

        Label headlineLabel = new Label(Res.get("reputation.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-5");

        Label infoLabel = new Label(Res.get("reputation.info"));
        infoLabel.getStyleClass().addAll("bisq-text-13", "wrap-text");

        burnBsqButton = new Button(Res.get("reputation.burnBsq"));

        bsqBondButton = new Button(Res.get("reputation.bond"));

        learnMore = new Hyperlink(Res.get("reputation.learnMore"));

        HBox buttons = new HBox(20, burnBsqButton, bsqBondButton);

        VBox.setVgrow(infoLabel, Priority.ALWAYS);
        VBox.setMargin(headlineLabel, new Insets(-10, 0, 0, 0));
        VBox.setMargin(learnMore, new Insets(0, 0, 10, 0));
        VBox vBox = new VBox(16, headlineLabel, infoLabel, learnMore, buttons);
        vBox.getStyleClass().add("bisq-box-1");
        vBox.setPadding(new Insets(30));
        vBox.setAlignment(Pos.TOP_LEFT);

        VBox.setMargin(vBox, new Insets(30, 0, 0, 0));
        root.getChildren().addAll(vBox);
    }

    @Override
    protected void onViewAttached() {
        burnBsqButton.setOnAction(e -> controller.onBurnBsq());
        bsqBondButton.setOnAction(e -> controller.onBsqBond());
        learnMore.setOnAction(e -> controller.onLearnMore());
    }

    @Override
    protected void onViewDetached() {
        burnBsqButton.setOnAction(null);
        bsqBondButton.setOnAction(null);
        learnMore.setOnAction(null);
    }
}
