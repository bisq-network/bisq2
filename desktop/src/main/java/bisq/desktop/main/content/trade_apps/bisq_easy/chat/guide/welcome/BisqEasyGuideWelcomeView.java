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

package bisq.desktop.main.content.trade_apps.bisq_easy.chat.guide.welcome;

import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqEasyGuideWelcomeView extends View<VBox, BisqEasyGuideWelcomeModel, BisqEasyGuideWelcomeController> {
    private final Button nextButton;
    private final Label content;

    public BisqEasyGuideWelcomeView(BisqEasyGuideWelcomeModel model, BisqEasyGuideWelcomeController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(20);
        root.setFillWidth(true);
        root.setAlignment(Pos.TOP_LEFT);

        Label headline = new Label(Res.get("tradeGuide.welcome.headline"));
        headline.getStyleClass().add("bisq-text-headline-2");

        content = new Label();
        content.setWrapText(true);
        content.getStyleClass().addAll("bisq-text-13", "bisq-line-spacing-01");

        nextButton = new Button(Res.get("action.next"));
        nextButton.setDefaultButton(true);

        VBox.setMargin(headline, new Insets(10, 0, 0, 0));
        VBox.setMargin(nextButton, new Insets(10, 0, 0, 0));
        root.getChildren().addAll(headline, content, nextButton);
    }

    @Override
    protected void onViewAttached() {
        content.textProperty().bind(model.getContentText());
        nextButton.setOnAction(e -> controller.onNext());
    }

    @Override
    protected void onViewDetached() {
        content.textProperty().unbind();
        nextButton.setOnAction(null);
    }
}
