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

package bisq.desktop.primary.main.content.settings.reputation.burn.tab1;

import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BurnBsqTab1View extends View<VBox, BurnBsqTab1Model, BurnBsqTab1Controller> {
    private final Button nextButton, learnMoreButton;

    public BurnBsqTab1View(BurnBsqTab1Model model,
                           BurnBsqTab1Controller controller) {
        super(new VBox(), model, controller);

        root.setSpacing(20);
        root.setAlignment(Pos.TOP_LEFT);

        Label headline = new Label(Res.get("reputation.burnedBsq.infoHeadline"));
        headline.getStyleClass().add("bisq-text-headline-2");

        Label info = new Label(Res.get("reputation.burnedBsq.info"));
        info.getStyleClass().addAll("bisq-text-13", "wrap-text");

        Label headline2 = new Label(Res.get("reputation.burnedBsq.infoHeadline2"));
        headline2.getStyleClass().add("bisq-text-headline-2");

        Label info2 = new Label(Res.get("reputation.burnedBsq.info2"));
        info2.getStyleClass().addAll("bisq-text-13", "wrap-text");

        nextButton = new Button(Res.get("next"));
        nextButton.setDefaultButton(true);

        learnMoreButton = new Button(Res.get("reputation.learnMore"));
        learnMoreButton.getStyleClass().add("bisq-text-button");

        VBox.setMargin(headline, new Insets(10, 0, 0, 0));
        VBox.setMargin(headline2, new Insets(20, 0, 0, 0));
        VBox.setMargin(learnMoreButton, new Insets(0, 0, 10, 0));
        root.getChildren().addAll(headline, info, headline2, info2, learnMoreButton, nextButton);
    }

    @Override
    protected void onViewAttached() {
        nextButton.setOnAction(e -> controller.onNext());
        learnMoreButton.setOnAction(e -> controller.onLearnMore());
    }

    @Override
    protected void onViewDetached() {
        nextButton.setOnAction(null);
        learnMoreButton.setOnAction(null);
    }
}
