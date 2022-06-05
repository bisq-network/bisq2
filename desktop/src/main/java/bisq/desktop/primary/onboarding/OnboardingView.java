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

package bisq.desktop.primary.onboarding;

import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.NavigationView;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.primary.onboarding.onboardNewbie.OnboardNewbieView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OnboardingView extends NavigationView<VBox, OnboardingModel, OnboardingController> {
    public OnboardingView(OnboardingModel model, OnboardingController controller) {
        super(new VBox(), model, controller);

        root.getStyleClass().add("bisq-content-bg");

        Label step1 = new Label("1. " + Res.get("onboarding.step1").toUpperCase());
        step1.getStyleClass().add("bisq-small-light-label");

        Region line1 = new Region();
        line1.setMaxHeight(1);
        line1.setPrefWidth(50);
        line1.getStyleClass().add("bisq-grey-dimmed");
        HBox.setMargin(line1, new Insets(12, 0, 0, 0));

        Label step2 = new Label("2. " + Res.get("onboarding.step2").toUpperCase());
        step2.getStyleClass().add("bisq-small-light-label-dimmed");

        Region line2 = new Region();
        line2.setMaxHeight(1);
        line2.setPrefWidth(50);
        line2.getStyleClass().add("bisq-grey-dimmed");
        HBox.setMargin(line2, new Insets(12, 0, 0, 0));

        Label step3 = new Label("3. " + Res.get("onboarding.step3").toUpperCase());
        step3.getStyleClass().add("bisq-small-light-label-dimmed");

        HBox box = Layout.hBoxWith(Spacer.fillHBox(), step1, line1, step2, line2, step3, Spacer.fillHBox());
        box.getStyleClass().add("bisq-content-bg");
        box.setSpacing(15);
        VBox.setMargin(box, new Insets(47, 0, 0, 0));
        root.getChildren().add(box);

        model.getView().addListener((observable, oldValue, newValue) -> {
            root.getChildren().add(newValue.getRoot());
            if (oldValue != null) {
               /* if (newValue instanceof SelectUserTypeViewOld) {
                    step1.getStyleClass().remove("bisq-small-light-label");
                    step1.getStyleClass().add("bisq-small-light-label-dimmed");
                    step2.getStyleClass().remove("bisq-small-light-label-dimmed");
                    step2.getStyleClass().add("bisq-small-light-label");
                }*/
                if (newValue instanceof OnboardNewbieView) {
                    step2.getStyleClass().remove("bisq-small-light-label");
                    step2.getStyleClass().add("bisq-small-light-label-dimmed");
                    step3.getStyleClass().remove("bisq-small-light-label-dimmed");
                    step3.getStyleClass().add("bisq-small-light-label");
                }
                Transitions.transitHorizontal(newValue.getRoot(), oldValue.getRoot());
            } else {
                Transitions.fadeIn(newValue.getRoot());
            }
        });
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
