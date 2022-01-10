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

import bisq.desktop.common.view.View;
import bisq.desktop.primary.main.content.ContentView;
import bisq.desktop.primary.main.nav.LeftNavView;
import bisq.desktop.primary.main.top.TopPanelView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainView extends View<StackPane, MainModel, MainController> {

    public MainView(MainModel model,
                    MainController controller,
                    ContentView contentView,
                    LeftNavView leftNavView,
                    TopPanelView topPanelView) {
        super(new StackPane(), model, controller);

        root.getStyleClass().add("content-pane");
       
        // only for dev
       /* ImageView bgImage = ImageUtil.getImageView("/bisq2-layout.png");
        bgImage.setFitHeight(1087);
        bgImage.setFitWidth(1859);
        StackPane.setAlignment(bgImage, Pos.TOP_LEFT);
        bgImage.setOpacity(0);*/

        VBox rootContainer = new VBox();
        root.getChildren().addAll(/*bgImage,*/ rootContainer);

        HBox leftNavAndContentBox = new HBox();
        HBox.setHgrow(contentView.getRoot(), Priority.ALWAYS);
        leftNavAndContentBox.getChildren().addAll(leftNavView.getRoot(), contentView.getRoot());

        VBox.setVgrow(leftNavAndContentBox, Priority.ALWAYS);
        rootContainer.getChildren().addAll(topPanelView.getRoot(), leftNavAndContentBox);
    }
}
