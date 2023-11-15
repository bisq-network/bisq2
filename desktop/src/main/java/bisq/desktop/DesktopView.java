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

package bisq.desktop;

import bisq.desktop.common.Layout;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.NavigationView;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DesktopView extends NavigationView<AnchorPane, DesktopModel, DesktopController> {
    private final Stage stage;
    @Getter
    private final Scene scene;

    public DesktopView(DesktopModel model, DesktopController controller, Stage stage) {
        super(new AnchorPane(), model, controller);

        this.stage = stage;

        root.getStyleClass().add("bisq-content-bg");

        scene = new Scene(root); // takes about  50 ms
        try {
            stage.setTitle(model.getTitle());

            ImageUtil.addAppIcons(stage);

            configCss();
            configSizeAndPosition();
            configKeyEventHandlers();

            stage.setOnCloseRequest(event -> {
                event.consume();
                controller.onQuit();
            });


            model.getView().addListener((observable, oldValue, newValue) -> {
                Layout.pinToAnchorPane(newValue.getRoot(), 0, 0, 0, 0);
                root.getChildren().add(newValue.getRoot());
                if (oldValue != null) {
                    Transitions.fadeOutAndRemove(oldValue.getRoot());
                    Transitions.fadeIn(newValue.getRoot());
                }
            });
        } catch (Exception exception) {
            exception.printStackTrace();
            controller.onQuit();
        }
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }

    void showStage() {
        long ts = System.currentTimeMillis();
        stage.setScene(scene); // Here we trigger onViewAttached in controller
        stage.show(); // Scene must be set before show, otherwise render scale is not working properly on all OS.
        log.info("Attaching view to stage took {} ms", System.currentTimeMillis() - ts);
    }

    private void configCss() {
        CssConfig.addAllCss(scene);
    }

    private void configSizeAndPosition() {
        root.setPrefWidth(model.getStageWidth());
        root.setPrefHeight(model.getStageHeight());
        stage.setX(model.getStageX());
        stage.setY(model.getStageY());
        stage.setWidth(model.getStageWidth());
        stage.setHeight(model.getStageHeight());
        stage.setMinWidth(DesktopModel.MIN_WIDTH);
        stage.setMinHeight(DesktopModel.MIN_HEIGHT);
        stage.xProperty().addListener((observable, oldValue, newValue) -> controller.onStageXChanged((double) newValue));
        stage.yProperty().addListener((observable, oldValue, newValue) -> controller.onStageYChanged((double) newValue));
        stage.widthProperty().addListener((observable, oldValue, newValue) -> controller.onStageWidthChanged((double) newValue));
        stage.heightProperty().addListener((observable, oldValue, newValue) -> controller.onStageHeightChanged((double) newValue));
    }

    private void configKeyEventHandlers() {
        scene.addEventHandler(KeyEvent.KEY_PRESSED,
                event -> KeyHandlerUtil.handleShutDownKeyEvent(event, controller::onQuit));
    }
}
