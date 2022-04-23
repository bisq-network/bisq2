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

package bisq.desktop.overlay;

import bisq.desktop.common.threading.UIScheduler;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EmptyOverlay {
    private final Region owner;
    private final Region content;
    private final double offsetX;
    private final double offsetY;
    private final Parent ownerRoot;
    private final Window rootWindow;
    private final Scene scene;
    private final Stage stage;
    private final ChangeListener<Number> positionListener;
    private final ChangeListener<Number> contentHeightListener;
    private double width;
    private double height;
    private UIScheduler fixPositionsScheduler;

    public EmptyOverlay(Region owner, Region content, double offsetX, double offsetY) {
        this.owner = owner;
        this.content = content;
        this.offsetX = offsetX;
        this.offsetY = offsetY;

        // Stage setup
        Scene rootScene = owner.getScene();
        ownerRoot = rootScene.getRoot();
        rootWindow = rootScene.getWindow();

        scene = new Scene(content);
        scene.getStylesheets().setAll(rootScene.getStylesheets());
        scene.setFill(Color.TRANSPARENT);

        stage = new Stage();
        stage.setScene(scene);
        stage.initOwner(rootWindow);
        stage.initModality(Modality.NONE);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.sizeToScene();


        // Listeners, handlers
        contentHeightListener = (observable, oldValue, newValue) -> doLayout();

        // On Linux the owner stage does not move the child stage as it does on Mac
        // So we need to apply centerPopup. Further, with fast movements the handler loses
        // the latest position, with a delay it fixes that.
        // Also, on Mac sometimes the popups are positioned outside the main app, so keep it for all OS
        positionListener = (observable, oldValue, newValue) -> {
            updatePosition();
            doLayout();
            if (fixPositionsScheduler != null) {
                fixPositionsScheduler.stop();
            }
            fixPositionsScheduler = UIScheduler.run(this::updatePosition).after(300);
        };

        stage.setOnCloseRequest(event -> {
            event.consume();
            close();
        });

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                close();
            }
        });

        rootWindow.xProperty().addListener(positionListener);
        rootWindow.yProperty().addListener(positionListener);
        rootWindow.widthProperty().addListener(positionListener);
        content.heightProperty().addListener(contentHeightListener);
    }

    public void show() {
        updatePosition();
        doLayout();
        stage.show();
    }

    public void close() {
        stage.hide();
        cleanup();
    }

    private void doLayout() {
        if (content.getHeight() > 0 && content.getWidth() > 0 && width > 0 && height > 0) {
            Bounds localToScene = owner.localToScene(owner.getBoundsInLocal());
            content.setLayoutX(localToScene.getMinX() + offsetX);
            content.setLayoutY(localToScene.getMinY() + offsetY);
        }
    }

    private void updatePosition() {
        Bounds boundsInLocal = ownerRoot.getBoundsInLocal();
        width = boundsInLocal.getWidth();
        height = boundsInLocal.getHeight();
        Bounds localToScreen = ownerRoot.localToScreen(boundsInLocal);
        stage.setX(localToScreen.getMinX());
        stage.setY(localToScreen.getMinY());
    }

    private void cleanup() {
        if (fixPositionsScheduler != null) {
            fixPositionsScheduler.stop();
        }
        rootWindow.xProperty().removeListener(positionListener);
        rootWindow.yProperty().removeListener(positionListener);
        rootWindow.widthProperty().removeListener(positionListener);
        content.heightProperty().removeListener(contentHeightListener);
        stage.setOnCloseRequest(null);
        scene.setOnKeyPressed(null);
    }
}