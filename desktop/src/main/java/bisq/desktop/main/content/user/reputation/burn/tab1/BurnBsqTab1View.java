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

package bisq.desktop.main.content.user.reputation.burn.tab1;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BurnBsqTab1View extends View<VBox, BurnBsqTab1Model, BurnBsqTab1Controller> {
    private final Button nextButton;
    private final Hyperlink learnMore;

    public BurnBsqTab1View(BurnBsqTab1Model model,
                           BurnBsqTab1Controller controller) {
        super(new VBox(20), model, controller);

        root.setAlignment(Pos.TOP_LEFT);

        Label headline = new Label(Res.get("user.reputation.burnedBsq.infoHeadline"));
        headline.getStyleClass().add("bisq-text-headline-2");

        Text infoLabelText = new Text(Res.get("user.reputation.burnedBsq.info"));
        infoLabelText.getStyleClass().add("bisq-text-13");
        TextFlow info = new TextFlow(infoLabelText);

        Label headline2 = new Label(Res.get("user.reputation.burnedBsq.infoHeadline2"));
        headline2.getStyleClass().add("bisq-text-headline-2");

        Text info2LabelText = new Text(Res.get("user.reputation.burnedBsq.info2"));
        info2LabelText.getStyleClass().add("bisq-text-13");
        TextFlow info2 = new TextFlow(info2LabelText);

        nextButton = new Button(Res.get("action.next"));
        nextButton.setDefaultButton(true);

        learnMore = new Hyperlink(Res.get("action.learnMore"));

        HBox buttons = new HBox(20, nextButton, Spacer.fillHBox(), learnMore);
        buttons.setAlignment(Pos.BOTTOM_RIGHT);

        VBox.setMargin(headline, new Insets(10, 0, 0, 0));
        VBox.setMargin(buttons, new Insets(10, 0, 0, 0));
        VBox.setMargin(headline2, new Insets(20, 0, 0, 0));
        root.getChildren().addAll(headline, info, headline2, info2, buttons);
    }

    @Override
    protected void onViewAttached() {
        nextButton.setOnAction(e -> controller.onNext());
        learnMore.setOnAction(e -> controller.onLearnMore());
    }

    @Override
    protected void onViewDetached() {
        nextButton.setOnAction(null);
        learnMore.setOnAction(null);
    }
}
