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

package network.misq.desktop.main.content.createoffer;

import javafx.beans.property.*;
import javafx.scene.Parent;
import network.misq.desktop.common.view.Controller;
import network.misq.desktop.common.view.Model;
import network.misq.desktop.common.view.View;

public class CreateOfferModel implements Model {
    final DoubleProperty minWidthProperty = new SimpleDoubleProperty(800);
    final DoubleProperty minHeightProperty = new SimpleDoubleProperty(600);
    final StringProperty titleProperty = new SimpleStringProperty("Create offer");
    final BooleanProperty backButtonVisible = new SimpleBooleanProperty(true);
    final BooleanProperty nextButtonVisible = new SimpleBooleanProperty(true);

    final ObjectProperty<View<Parent, Model, Controller>> view = new SimpleObjectProperty<>();

    public CreateOfferModel() {
    }

    public void selectView(View<Parent, Model, Controller> view) {
        this.view.set(view);
    }


}
