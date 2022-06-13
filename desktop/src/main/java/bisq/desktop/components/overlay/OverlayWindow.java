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

package bisq.desktop.components.overlay;

import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.components.containers.Spacer;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class OverlayWindow extends Pane {
    private final static double MARGIN = 66;
    private final static double TOP_MARGIN = 57;

    private final Region content;
    private Optional<Runnable> closeHandler = java.util.Optional.empty();
    private final Parent ownerRoot;
    private final Window rootWindow;
    private final Scene scene;
    private final Stage stage;
    private final HBox topBox;
    private final ImageView closeButton;
    private final ChangeListener<Number> positionListener;
    private final ChangeListener<Number> contentHeightListener;
    private final ScrollPane scrollPane;
    private double width;
    private double height;
    private UIScheduler fixPositionsScheduler;

    public OverlayWindow(Region owner, Region content) {
        this(owner, content, null);
    }

    public OverlayWindow(Region owner, Region content, Runnable closeHandler) {
        this.content = content;
        this.closeHandler = Optional.ofNullable(closeHandler);

        // Stage setup
        Scene rootScene = owner.getScene();
        ownerRoot = rootScene.getRoot();
        rootWindow = rootScene.getWindow();

        scene = new Scene(this);
        scene.getStylesheets().setAll(rootScene.getStylesheets());
        scene.setFill(Color.TRANSPARENT);

        stage = new Stage();
        stage.setScene(scene);
        stage.initOwner(rootWindow);
        stage.initModality(Modality.NONE);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.sizeToScene();


        // Content
        setStyle("-fx-background-color: transparent");

        closeButton = ImageUtil.getImageViewById("close");
        closeButton.setCursor(Cursor.HAND);
        closeButton.setOnMouseClicked(e -> close());
        HBox.setMargin(closeButton, new Insets(48, MARGIN + 7, 0, 0));

        topBox = new HBox();
        topBox.setFillHeight(true);
        topBox.getChildren().addAll(Spacer.fillHBox(), closeButton);
        topBox.setStyle("-fx-background-color: transparent");

        scrollPane = new ScrollPane();
        scrollPane.setContent(content);

        getChildren().addAll(scrollPane, topBox);

        Transitions.blur(ownerRoot, 1000, -0.5, false, 2d);

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
        closeHandler.ifPresent(Runnable::run);
    }

    private void doLayout() {
        if (content.getHeight() > 0 && content.getWidth() > 0 && width > 0 && height > 0) {
            double left = Math.max(0, ((width - content.getWidth()) / 2));
            double top = Math.max(0, Math.min(TOP_MARGIN, ((height - content.getHeight()) / 2)));
            double right = left;
            double bottom = MARGIN;
            double prefWidth = width - left - right;

            scrollPane.setLayoutX(left);
            scrollPane.setLayoutY(top);
            scrollPane.setPrefWidth(prefWidth);
            scrollPane.setPrefHeight(height - top - bottom);

            topBox.setPrefWidth(prefWidth);
            topBox.setLayoutX(left);
            topBox.setLayoutY(top);
        }
    }

    private void updatePosition() {
        Bounds boundsInLocal = ownerRoot.getBoundsInLocal();
        width = boundsInLocal.getWidth();
        height = boundsInLocal.getHeight();
        Bounds localToScreen = ownerRoot.localToScreen(boundsInLocal);
        stage.setX(localToScreen.getMinX());
        stage.setY(localToScreen.getMinY());
        setPrefWidth(width);
        setPrefHeight(height);
    }

    private void cleanup() {
        if (fixPositionsScheduler != null) {
            fixPositionsScheduler.stop();
        }
        rootWindow.xProperty().removeListener(positionListener);
        rootWindow.yProperty().removeListener(positionListener);
        rootWindow.widthProperty().removeListener(positionListener);
        content.heightProperty().removeListener(contentHeightListener);
        closeButton.setOnMouseClicked(null);
        stage.setOnCloseRequest(null);
        scene.setOnKeyPressed(null);
        Transitions.removeEffect(ownerRoot);
    }
}