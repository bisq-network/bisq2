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

package bisq.desktop.main.content.mu_sig.my_offers;

import bisq.desktop.common.view.View;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class MuSigMyOffersView extends View<VBox, MuSigMyOffersModel, MuSigMyOffersController> {

    public MuSigMyOffersView(MuSigMyOffersModel model, MuSigMyOffersController controller) {
        super(new VBox(), model, controller);

        VBox contentBox = new VBox(20);
        contentBox.getChildren().addAll(new Label(this.getClass().getSimpleName()));
        contentBox.getStyleClass().add("bisq-common-bg");
        root.getChildren().add(contentBox);
        root.setPadding(new Insets(0, 40, 500, 40));
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
