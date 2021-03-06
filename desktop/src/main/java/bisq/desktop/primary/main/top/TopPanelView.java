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

package bisq.desktop.primary.main.top;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.primary.main.content.components.UserProfileSelection;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

public class TopPanelView extends View<HBox, TopPanelModel, TopPanelController> {
    public static final int HEIGHT = 57;

    public TopPanelView(TopPanelModel model,
                        TopPanelController controller,
                        UserProfileSelection userProfileSelection,
                        Pane marketPriceBox) {
        super(new HBox(), model, controller);

        root.setMinHeight(HEIGHT);
        root.setMaxHeight(HEIGHT);
        root.setSpacing(28);
        root.setFillHeight(true);
        root.setStyle("-fx-background-color: -bisq-dark-grey;");
        HBox.setMargin(marketPriceBox, new Insets(-10, 10, 0, 0));
        userProfileSelection.setIsLeftAligned(true);
        root.setPadding(new Insets(7, 30, 0, 0));
        root.getChildren().addAll(Spacer.fillHBox(), marketPriceBox, userProfileSelection.getRoot());
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
