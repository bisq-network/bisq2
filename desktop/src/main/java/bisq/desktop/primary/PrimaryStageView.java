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

package bisq.desktop.primary;

import bisq.desktop.Preloader;
import bisq.desktop.common.utils.KeyCodeUtils;
import bisq.desktop.common.view.View;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.requireNonNull;

@Slf4j
public class PrimaryStageView extends View<AnchorPane, PrimaryStageModel, PrimaryStageController> {
    private final Stage stage;
    @Getter
    private final Scene scene;

    public PrimaryStageView(PrimaryStageModel model, PrimaryStageController controller, Stage stage) {
        super(new AnchorPane(), model, controller);

        this.stage = stage;
        scene = new Scene(root);

        try {
            scene.getStylesheets().setAll(requireNonNull(getClass().getResource("/bisq.css")).toExternalForm(),
                    requireNonNull(getClass().getResource("/bisq2.css")).toExternalForm(),
                    requireNonNull(getClass().getResource("/images.css")).toExternalForm(),
                    requireNonNull(getClass().getResource("/theme-dark.css")).toExternalForm());

            root.prefWidthProperty().bind(model.getPrefWidthProperty());
            root.prefHeightProperty().bind(model.getPrefHeightProperty());

            Preloader preloader = new Preloader();
            AnchorPane.setLeftAnchor(preloader, 0d);
            AnchorPane.setRightAnchor(preloader, 0d);
            AnchorPane.setTopAnchor(preloader, 0d);
            AnchorPane.setBottomAnchor(preloader, 0d);
            root.getChildren().add(preloader);

            stage.minHeightProperty().bind(model.getMinHeightProperty());
            stage.minWidthProperty().bind(model.getMinWidthProperty());
            stage.titleProperty().bind(model.getTitleProperty());
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
            show();
        } catch (Exception exception) {
            exception.printStackTrace();
            controller.onQuit();
        }

        model.view.addListener((observable, oldValue, newValue) -> {
            Parent mainViewRoot = newValue.getRoot();
            AnchorPane.setLeftAnchor(mainViewRoot, 0d);
            AnchorPane.setRightAnchor(mainViewRoot, 0d);
            AnchorPane.setTopAnchor(mainViewRoot, 0d);
            AnchorPane.setBottomAnchor(mainViewRoot, 0d);
            root.getChildren().setAll(mainViewRoot);
        });
    }

    private void show() {
        this.stage.setScene(scene);
        stage.show();
    }
}
