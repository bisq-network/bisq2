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

package bisq.desktop.primary.main.content.settings.reputation.burn.tab2;

import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BurnBsqTab2View extends View<VBox, BurnBsqTab2Model, BurnBsqTab2Controller> {
    private final Button backButton, nextButton, learnMoreButton;

    public BurnBsqTab2View(BurnBsqTab2Model model,
                           BurnBsqTab2Controller controller) {
        super(new VBox(), model, controller);

        root.setSpacing(20);
        root.setAlignment(Pos.TOP_LEFT);

        Label headline = new Label(Res.get("reputation.burnedBsq.score.headline"));
        headline.getStyleClass().add("bisq-text-headline-2");

        Label info = new Label(Res.get("reputation.burnedBsq.score.info"));
        info.getStyleClass().addAll("bisq-text-13", "wrap-text");

        backButton = new Button(Res.get("back"));

        nextButton = new Button(Res.get("next"));
        nextButton.setDefaultButton(true);

        HBox buttons = new HBox(20, backButton, nextButton);

        learnMoreButton = new Button(Res.get("reputation.learnMore"));
        learnMoreButton.getStyleClass().add("bisq-text-button");

        VBox.setMargin(headline, new Insets(10, 0, 0, 0));
        VBox.setMargin(learnMoreButton, new Insets(0, 0, 10, 0));
        root.getChildren().addAll(headline, info, learnMoreButton, buttons);
    }

    @Override
    protected void onViewAttached() {
        backButton.setOnAction(e -> controller.onBack());
        nextButton.setOnAction(e -> controller.onNext());
        learnMoreButton.setOnAction(e -> controller.onLearnMore());
    }

    @Override
    protected void onViewDetached() {
        backButton.setOnAction(null);
        nextButton.setOnAction(null);
        learnMoreButton.setOnAction(null);
    }
}
