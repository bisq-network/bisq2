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
import bisq.desktop.layout.Layout;
import bisq.desktop.primary.onboarding.onboardNewbie.OnboardNewbieView;
import bisq.desktop.primary.onboarding.selectUserType.SelectUserTypeView;
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

        // root.getStyleClass().add("content-pane");

     /*   ImageView logo = ImageUtil.getImageViewById("logo-small");
        VBox.setMargin(logo, new Insets(20, 20, 10, 20));

        Pane pane = new Pane();
        root.getChildren().addAll(logo, pane);
*/
        // root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("content-pane");

        Label step1 = new Label("1. " + Res.get("onboarding.step1").toUpperCase());
        step1.setStyle("-fx-text-fill: white;-fx-font-size: 1.4em;-fx-font-family: \"IBM Plex Sans Light\";");

        Region line1 = new Region();
        line1.setMaxHeight(1);
        line1.setPrefWidth(50);
        line1.setStyle("-fx-background-color: #4E4E4E");
        HBox.setMargin(line1, new Insets(12, 0, 0, 0));

        Label step2 = new Label("2. " + Res.get("onboarding.step2").toUpperCase());
        step2.setStyle("-fx-text-fill: #4E4E4E;-fx-font-size: 1.4em;-fx-font-family: \"IBM Plex Sans Light\";");

        Region line2 = new Region();
        line2.setMaxHeight(1);
        line2.setPrefWidth(50);
        line2.setStyle("-fx-background-color: #4E4E4E");
        HBox.setMargin(line2, new Insets(12, 0, 0, 0));

        Label step3 = new Label("3. " + Res.get("onboarding.step3").toUpperCase());
        step3.setStyle("-fx-text-fill: #4E4E4E;-fx-font-size: 1.4em;-fx-font-family: \"IBM Plex Sans Light\";");

        HBox box = Layout.hBoxWith(Spacer.fillHBox(), step1, line1, step2, line2, step3, Spacer.fillHBox());
        box.getStyleClass().add("content-pane");
        box.setSpacing(15);
        VBox.setMargin(box, new Insets(70, 0, 0, 0));
        root.getChildren().add(box);

        model.getView().addListener((observable, oldValue, newValue) -> {
            // newValue.getRoot().setPadding(new Insets(0, 20, 20, 20));
            root.getChildren().add(newValue.getRoot());
            if (oldValue != null) {
                if (newValue instanceof SelectUserTypeView) {
                    step1.setStyle("-fx-text-fill: #4E4E4E");
                    step2.setStyle("-fx-text-fill: white");
                }
                if (newValue instanceof OnboardNewbieView) {
                    step2.setStyle("-fx-text-fill: #4E4E4E");
                    step3.setStyle("-fx-text-fill: white");
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
