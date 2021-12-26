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

package network.misq.desktop.main;

import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import network.misq.desktop.common.utils.ImageUtil;
import network.misq.desktop.common.view.View;
import network.misq.desktop.main.content.ContentView;
import network.misq.desktop.main.left.NavigationView;
import network.misq.desktop.main.top.TopPanelView;

public class MainView extends View<StackPane, MainViewModel, MainViewController> {

    public MainView(MainViewModel model,
                    MainViewController controller,
                    ContentView contentView,
                    NavigationView navigationView,
                    TopPanelView topPanelView) {
        super(new StackPane(), model, controller);

        root.getStyleClass().add("content-pane");

        ImageView bgImage = ImageUtil.getImageView("/misq-layout.png");
        bgImage.setFitHeight(1087);
        bgImage.setFitWidth(1859);
        StackPane.setAlignment(bgImage, Pos.TOP_LEFT);
        bgImage.setOpacity(0);

        VBox rootContainer = new VBox();
        root.getChildren().addAll(bgImage, rootContainer);

        HBox leftNavAndContentBox = new HBox();
        VBox.setVgrow(leftNavAndContentBox, Priority.ALWAYS);
        rootContainer.getChildren().addAll(topPanelView.getRoot(), leftNavAndContentBox);

        HBox.setHgrow(contentView.getRoot(), Priority.ALWAYS);

        leftNavAndContentBox.getChildren().addAll(navigationView.getRoot(), contentView.getRoot());
    }
}
