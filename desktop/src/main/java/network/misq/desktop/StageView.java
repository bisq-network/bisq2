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

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.desktop.common.utils.KeyCodeUtils;
import network.misq.desktop.main.MainView;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class StageView extends Application {
    public static final CompletableFuture<StageView> LAUNCH_APP_FUTURE = new CompletableFuture<>();
    private StageController controller;
    private StageModel model;

    private Stage stage;
    @Getter
    private Scene scene;
    private Pane root;

    public StageView() {
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        LAUNCH_APP_FUTURE.complete(this);
    }

    public void initialize(StageModel model, StageController controller) {
        this.controller = controller;
        this.model = model;
        try {
            root = new AnchorPane();
            root.prefWidthProperty().bind(model.prefWidthProperty);
            root.prefHeightProperty().bind(model.prefHeightProperty);

            Preloader preloader = new Preloader();
            AnchorPane.setLeftAnchor(preloader, 0d);
            AnchorPane.setRightAnchor(preloader, 0d);
            AnchorPane.setTopAnchor(preloader, 0d);
            AnchorPane.setBottomAnchor(preloader, 0d);
            root.getChildren().add(preloader);
            scene = new Scene(root);
            scene.getStylesheets().setAll(getClass().getResource("/misq.css").toExternalForm(),
                    getClass().getResource("/bisq.css").toExternalForm(),
                    getClass().getResource("/theme-dark.css").toExternalForm());

            stage.setScene(scene);
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
        }
    }

    public void activate(MainView mainView) {
        StackPane mainViewRoot = mainView.getRoot();
        root.getChildren().setAll(mainViewRoot);
        AnchorPane.setLeftAnchor(mainViewRoot, 0d);
        AnchorPane.setRightAnchor(mainViewRoot, 0d);
        AnchorPane.setTopAnchor(mainViewRoot, 0d);
        AnchorPane.setBottomAnchor(mainViewRoot, 0d);
    }
}
