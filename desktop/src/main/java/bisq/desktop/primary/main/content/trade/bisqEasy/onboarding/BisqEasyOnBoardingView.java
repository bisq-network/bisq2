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

package bisq.desktop.primary.main.content.trade.bisqEasy.onboarding;

import bisq.desktop.common.view.TabViewChild;
import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

public class BisqEasyOnBoardingView extends View<ScrollPane, BisqEasyOnBoardingModel, BisqEasyOnBoardingController> implements TabViewChild {
    private final Button learnMoreButton, nextButton;
    private final VBox vBox;
    private final Label learnMore;
    private Label headlineLabel;
    private Label textLabel;

    public BisqEasyOnBoardingView(BisqEasyOnBoardingModel model, BisqEasyOnBoardingController controller) {
        super(new ScrollPane(), model, controller);

        vBox = new VBox();
        vBox.setSpacing(10);
        vBox.setPadding(new Insets(33, -67, 0, 67));

        root.setContent(vBox);

        Label headline = new Label(Res.get("bisqEasy.onBoarding.headline"));
        headline.getStyleClass().add("very-large-text");
        vBox.getChildren().add(headline);

        headlineLabel = new Label();
        headlineLabel.getStyleClass().add("large-text");
        VBox.setMargin(headlineLabel, new Insets(30, 0, 0, 0));
        textLabel = new Label();
        vBox.getChildren().addAll(headlineLabel, textLabel);
     
        nextButton = new Button(Res.get("bisqEasy.onBoarding.next.button"));
        VBox.setMargin(nextButton, new Insets(20, 0, 40, 0));
       
        learnMore = new Label(Res.get("bisqEasy.onBoarding.learnMore"));
        learnMoreButton = new Button(Res.get("bisqEasy.onBoarding.learnMore.button"));
      
        vBox.getChildren().addAll(nextButton, learnMore, learnMoreButton);
    }

    @Override
    protected void onViewAttached() {
        headlineLabel.textProperty().bind(model.getHeadline());
        textLabel.textProperty().bind(model.getText());
        learnMoreButton.setOnAction(e -> controller.onLearnMore());
        nextButton.setOnAction(e -> controller.onNext());
    }

    @Override
    protected void onViewDetached() {
        headlineLabel.textProperty().unbind();
        textLabel.textProperty().unbind();
        learnMoreButton.setOnAction(null);
        nextButton.setOnAction(null);
    }
}
