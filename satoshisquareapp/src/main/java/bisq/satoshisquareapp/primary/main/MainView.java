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

package bisq.satoshisquareapp.primary.main;

import bisq.desktop.common.view.View;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class MainView extends View<VBox, MainModel, MainController> {

    public MainView(MainModel model,
                    MainController controller,
                    Pane social,
                    Pane topPanel) {
        super(new VBox(), model, controller);

        root.getChildren().addAll(topPanel, social);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
