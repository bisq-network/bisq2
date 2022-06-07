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

package bisq.desktop.primary.overlay.onboarding.bisqeasy.tab2;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.primary.overlay.onboarding.Utils;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqEasyOnboardingTab2View extends View<VBox, BisqEasyOnboardingTab2Model, BisqEasyOnboardingTab2Controller> {
    private final Button nextButton, skipButton;

    public BisqEasyOnboardingTab2View(BisqEasyOnboardingTab2Model model, BisqEasyOnboardingTab2Controller controller) {
        super(new VBox(), model, controller);

        root.setSpacing(20);
        root.setAlignment(Pos.CENTER_LEFT);

        Label headLine = new Label(Res.get("bisqEasy.onBoarding.tab2.headline"));
        headLine.setId("bisq-easy-onboarding-headline-label");
        VBox.setMargin(headLine, new Insets(25, 0, 0, 0));

        nextButton = new Button(Res.get("next"));
        nextButton.setId("bisq-easy-next-button");
        skipButton = new Button(Res.get("bisqEasy.onBoarding.skipIntro"));
        skipButton.setId("bisq-easy-skip-button");
        HBox buttons = new HBox(0, nextButton, skipButton, Spacer.fillHBox());
        VBox.setMargin(buttons, new Insets(40, 0, 0, 0));

        root.getChildren().addAll(headLine,
                Utils.getIconAndText(Res.get("bisqEasy.onBoarding.tab2.line1"),"onboarding-2-offer"),
                Utils.getIconAndText(Res.get("bisqEasy.onBoarding.tab2.line2"),"onboarding-2-chat"),
                Utils.getIconAndText(Res.get("bisqEasy.onBoarding.tab2.line3"),"onboarding-2-payment"),
                buttons);
    }

    @Override
    protected void onViewAttached() {
        nextButton.setOnAction(e -> controller.onNext());
        skipButton.setOnAction(e -> controller.onSkip());
    }

    @Override
    protected void onViewDetached() {
        nextButton.setOnAction(null);
        skipButton.setOnAction(null);
    }
}
