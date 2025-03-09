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

package bisq.desktop.main.content.bisq_musig.onboarding;

import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqMusigOnboardingView extends View<VBox, BisqMusigOnboardingModel, BisqMusigOnboardingController> {
    public BisqMusigOnboardingView(BisqMusigOnboardingModel model, BisqMusigOnboardingController controller) {
        super(new VBox(), model, controller);

        Label headlineLabel = new Label(Res.get("bisqMusig.onboarding.top.headline"));
        headlineLabel.getStyleClass().addAll("font-light", "font-size-25", "text-color-light");

        VBox contentBox = new VBox(20);
        contentBox.getChildren().add(headlineLabel);
        contentBox.getStyleClass().add("bisq-common-bg");
        root.getChildren().addAll(contentBox);
        root.setPadding(new Insets(0, 40, 20, 40));
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
