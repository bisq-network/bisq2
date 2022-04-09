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

package bisq.desktop.primary.main.content.education;

import bisq.desktop.common.view.View;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class EducationView extends View<VBox, EducationModel, EducationController> {
    public EducationView(EducationModel model, EducationController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.CENTER);
        Label label = new Label("WIP");
        label.setStyle("-fx-text-fill: -bisq-text-dark; -fx-font-size: 30em");
        Label small = new Label(getClass().getSimpleName());
        small.setStyle("-fx-text-fill: -bisq-text-dark; -fx-font-size: 2em");
        root.getChildren().addAll(label, small);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
