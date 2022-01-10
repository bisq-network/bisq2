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
        scene = new Scene(root); // takes about  50 ms
        try {
            scene.getStylesheets().setAll(requireNonNull(getClass().getResource("/bisq.css")).toExternalForm(),
                    requireNonNull(getClass().getResource("/bisq2.css")).toExternalForm(),
                    requireNonNull(getClass().getResource("/images.css")).toExternalForm(),
                    requireNonNull(getClass().getResource("/theme-dark.css")).toExternalForm());
            scene.addEventHandler(KeyEvent.KEY_RELEASED, keyEvent -> {
                if (KeyCodeUtils.isCtrlPressed(KeyCode.W, keyEvent) ||
                        KeyCodeUtils.isCtrlPressed(KeyCode.Q, keyEvent)) {
                    controller.onQuit();
                }
            });

            root.setPrefWidth(model.getPrefWidth());
            root.setPrefHeight(model.getPrefHeight());
            model.view.addListener((observable, oldValue, newValue) -> {
                Parent mainViewRoot = newValue.getRoot();
                AnchorPane.setLeftAnchor(mainViewRoot, 0d);
                AnchorPane.setRightAnchor(mainViewRoot, 0d);
                AnchorPane.setTopAnchor(mainViewRoot, 0d);
                AnchorPane.setBottomAnchor(mainViewRoot, 0d);
                root.getChildren().setAll(mainViewRoot);
            });

            stage.setTitle(model.getTitle());
            model.getStageX().ifPresent(stage::setX);
            model.getStageY().ifPresent(stage::setY);
            model.getStageWidth().ifPresent(stage::setWidth);
            model.getStageHeight().ifPresent(stage::setHeight);
            stage.setMinWidth(model.getMinWidth());
            stage.setMinHeight(model.getMinHeight());
            stage.xProperty().addListener((observable, oldValue, newValue) -> controller.onStageXChanged((double) newValue));
            stage.yProperty().addListener((observable, oldValue, newValue) -> controller.onStageYChanged((double) newValue));
            stage.widthProperty().addListener((observable, oldValue, newValue) -> controller.onStageWidthChanged((double) newValue));
            stage.heightProperty().addListener((observable, oldValue, newValue) -> controller.onStageHeightChanged((double) newValue));
            stage.setOnCloseRequest(event -> {
                event.consume();
                controller.onQuit();
            });

            stage.setScene(scene);
            stage.show(); // takes about 90 ms
        } catch (Exception exception) {
            exception.printStackTrace();
            controller.onQuit();
        }
    }
}
