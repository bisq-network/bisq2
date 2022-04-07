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

package bisq.desktop.primary.main;

import bisq.desktop.common.view.NavigationView;
import bisq.desktop.primary.main.left.LeftNavView;
import bisq.desktop.primary.main.top.TopPanelView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class MainView extends NavigationView<VBox, MainModel, MainController> {
    public MainView(MainModel model,
                    MainController controller,
                    LeftNavView leftNavView,
                    TopPanelView topPanelView) {
        super(new VBox(), model, controller);

        root.getStyleClass().add("content-pane");
        
        HBox leftNavAndContentBox = new HBox();
        leftNavAndContentBox.getChildren().add(leftNavView.getRoot());
        model.getView().addListener((observable, oldValue, contentView) -> {
            HBox.setHgrow(contentView.getRoot(), Priority.ALWAYS);
            leftNavAndContentBox.getChildren().add(contentView.getRoot());
        });


        VBox.setVgrow(leftNavAndContentBox, Priority.ALWAYS);
        root.getChildren().addAll(topPanelView.getRoot(), leftNavAndContentBox);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
