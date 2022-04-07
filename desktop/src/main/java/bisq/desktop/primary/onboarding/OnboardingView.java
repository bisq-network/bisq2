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

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.NavigationView;
import javafx.geometry.Insets;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OnboardingView extends NavigationView<VBox, OnboardingModel, OnboardingController> {
    public OnboardingView(OnboardingModel model, OnboardingController controller) {
        super(new VBox(), model, controller);

        root.getStyleClass().add("content-pane");

        ImageView logo = ImageUtil.getImageViewById("logo-small");
        VBox.setMargin(logo, new Insets(20, 20, 10, 20));

        Pane pane = new Pane();
        root.getChildren().addAll(logo, pane);

        model.getView().addListener((observable, oldValue, newValue) -> {
            newValue.getRoot().setPadding(new Insets(0, 20, 20, 20));
            pane.getChildren().add(newValue.getRoot());
            if (oldValue != null) {
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
