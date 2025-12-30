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
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainView extends NavigationView<VBox, MainModel, MainController> {
    private static AnchorPane anchorPane;

    public MainView(MainModel model,
                    MainController controller,
                    AnchorPane leftNav,
                    StackPane bannerNotification,
                    HBox topPanel,
                    BorderPane notificationPanel,
                    BorderPane alertBanner) {
        super(new VBox(), model, controller);

        anchorPane = new AnchorPane();
        VBox.setVgrow(anchorPane, Priority.ALWAYS);
        VBox vBox = new VBox(topPanel, notificationPanel, alertBanner, anchorPane);
        vBox.setFillWidth(true);
        HBox.setHgrow(vBox, Priority.ALWAYS);
        HBox appContent = new HBox(leftNav, vBox);
        VBox.setVgrow(appContent, Priority.ALWAYS);
        root.getChildren().addAll(bannerNotification, appContent);

        // We only get created once after splashscreen and then never get removed, so we do not need to remove the 
        // listener.
        model.getView().addListener((observable, oldValue, newValue) -> {
            anchorPane.getChildren().setAll(newValue.getRoot());
            AnchorPane.setTopAnchor(newValue.getRoot(), 0.0);
            AnchorPane.setBottomAnchor(newValue.getRoot(), 0.0);
            AnchorPane.setLeftAnchor(newValue.getRoot(), 0.0);
            AnchorPane.setRightAnchor(newValue.getRoot(), 0.0);
        });

    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
