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

package network.misq.desktop;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.desktop.common.utils.KeyCodeUtils;
import network.misq.desktop.common.view.View;
import network.misq.desktop.main.MainView;

import static java.util.Objects.requireNonNull;

@Slf4j
public class StageView extends View<AnchorPane, StageModel, StageController> {
    private final Stage stage;
    @Getter
    private final Scene scene;

    public StageView(StageModel model, StageController controller, Stage stage) {
        super(new AnchorPane(), model, controller);
        this.stage = stage;

        scene = new Scene(root);
        this.stage.setScene(scene);

        try {
            scene.getStylesheets().setAll(requireNonNull(getClass().getResource("/misq.css")).toExternalForm(),
                    requireNonNull(getClass().getResource("/bisq.css")).toExternalForm(),
                    requireNonNull(getClass().getResource("/images.css")).toExternalForm(),
                    requireNonNull(getClass().getResource("/theme-dark.css")).toExternalForm());

            root.prefWidthProperty().bind(model.prefWidthProperty);
            root.prefHeightProperty().bind(model.prefHeightProperty);

            Preloader preloader = new Preloader();
            AnchorPane.setLeftAnchor(preloader, 0d);
            AnchorPane.setRightAnchor(preloader, 0d);
            AnchorPane.setTopAnchor(preloader, 0d);
            AnchorPane.setBottomAnchor(preloader, 0d);
            root.getChildren().add(preloader);

            stage.minHeightProperty().bind(model.minHeightProperty);
            stage.minWidthProperty().bind(model.minWidthProperty);
            stage.titleProperty().bind(model.titleProperty);
            stage.setOnCloseRequest(event -> {
                event.consume();
                controller.onQuit();
            });
            scene.addEventHandler(KeyEvent.KEY_RELEASED, keyEvent -> {
                if (KeyCodeUtils.isCtrlPressed(KeyCode.W, keyEvent) ||
                        KeyCodeUtils.isCtrlPressed(KeyCode.Q, keyEvent)) {
                    controller.onQuit();
                }
            });

            stage.show();
            controller.onViewAdded();
        } catch (Exception exception) {
            exception.printStackTrace();
            controller.onQuit();
        }
    }

    void addMainView(MainView mainView) {
        StackPane mainViewRoot = mainView.getRoot();
        AnchorPane.setLeftAnchor(mainViewRoot, 0d);
        AnchorPane.setRightAnchor(mainViewRoot, 0d);
        AnchorPane.setTopAnchor(mainViewRoot, 0d);
        AnchorPane.setBottomAnchor(mainViewRoot, 0d);
        root.getChildren().setAll(mainViewRoot);
    }
}
