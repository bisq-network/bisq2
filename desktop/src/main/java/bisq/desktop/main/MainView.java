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

package bisq.desktop.main;

import bisq.desktop.common.view.NavigationView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainView extends NavigationView<HBox, MainModel, MainController> {
    private static ScrollPane scrollPane;

    // Hack to adjust the fitToHeight property in case a chatView is displayed
    public static void setFitToHeight(boolean value) {
        scrollPane.setFitToHeight(value);
    }

    public MainView(MainModel model,
                    MainController controller,
                    AnchorPane leftNavView,
                    HBox topPanelView) {
        super(new HBox(), model, controller);

        root.setFillHeight(true);
        scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);

        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        VBox vBox = new VBox(topPanelView, scrollPane);
        vBox.setFillWidth(true);

        HBox.setHgrow(vBox, Priority.ALWAYS);
        root.getChildren().addAll(leftNavView, vBox);

        model.getView().addListener((observable, oldValue, newValue) -> {
            scrollPane.setContent(newValue.getRoot());
        });
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
