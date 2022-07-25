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

package bisq.desktop.primary.main.content.settings.reputation.bond.tab1;

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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BondedReputationTab1View extends View<VBox, BondedReputationTab1Model, BondedReputationTab1Controller> {
    private final Button nextButton;
    private final Hyperlink learnMore;

    public BondedReputationTab1View(BondedReputationTab1Model model,
                                    BondedReputationTab1Controller controller) {
        super(new VBox(), model, controller);

        root.setSpacing(20);
        root.setAlignment(Pos.TOP_LEFT);

        Label headline = new Label(Res.get("reputation.bond.infoHeadline"));
        headline.getStyleClass().add("bisq-text-headline-2");

        Label info = new Label(Res.get("reputation.bond.info"));
        info.getStyleClass().addAll("bisq-text-13", "wrap-text");

        Label headline2 = new Label(Res.get("reputation.bond.infoHeadline2"));
        headline2.getStyleClass().add("bisq-text-headline-2");

        Label info2 = new Label(Res.get("reputation.bond.info2"));
        info2.getStyleClass().addAll("bisq-text-13", "wrap-text");

        nextButton = new Button(Res.get("next"));
        nextButton.setDefaultButton(true);

        learnMore = new Hyperlink(Res.get("reputation.learnMore"));

        HBox buttons = new HBox(20, nextButton, Spacer.fillHBox(), learnMore);
        buttons.setAlignment(Pos.BOTTOM_RIGHT);

        VBox.setMargin(headline, new Insets(10, 0, 0, 0));
        VBox.setMargin(headline2, new Insets(20, 0, 0, 0));
        VBox.setMargin(buttons, new Insets(10, 0, 0, 0));
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
