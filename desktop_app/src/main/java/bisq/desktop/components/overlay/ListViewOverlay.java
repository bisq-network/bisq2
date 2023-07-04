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
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polygon;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
public class ListViewOverlay<T> {
    private final Region owner;
    private final double prefWidth;
    private final double offsetX;
    private final double offsetY;
    private final Parent ownerRoot;
    private final Window rootWindow;
    private final Scene scene;
    private final Stage stage;
    private final ChangeListener<Number> positionListener;
    private final ChangeListener<Number> rootHeightListener;
    private final ChangeListener<T> selectedItemListener;
    private double width;
    private double height;
    private UIScheduler fixPositionsScheduler;
    private final ListView<T> listView;
    private final Pane root;
    protected final Polygon listBackground = new Polygon();

    public ListViewOverlay(Region owner,
                           ObservableList<T> items,
                           Callback<ListView<T>, ListCell<T>> cellFactory,
                           Consumer<T> selectionHandler,
                           double prefWidth) {
        this(owner, items, cellFactory, selectionHandler, prefWidth, 0, 0);
    }

    public ListViewOverlay(Region owner,
                           ObservableList<T> items,
                           Callback<ListView<T>, ListCell<T>> cellFactory,
                           Consumer<T> selectionHandler,
                           double prefWidth,
                           double offsetX,
                           double offsetY) {
        this.owner = owner;
        this.prefWidth = prefWidth;
        this.offsetX = offsetX;
        this.offsetY = offsetY;

        DropShadow dropShadow = new DropShadow();
        dropShadow.setBlurType(BlurType.GAUSSIAN);
        dropShadow.setRadius(25);
        dropShadow.setSpread(0.65);
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.4));

        listBackground.setFill(Paint.valueOf("#212121"));
        listBackground.setEffect(dropShadow);

        listView = new ListView<>(items);
        listView.setPrefWidth(400);
        listView.setPrefHeight(100);
        listView.setCellFactory(cellFactory);
        listView.setId("bisq-combo-box-list-view");
        selectedItemListener = (observable, oldValue, newValue) -> {
            selectionHandler.accept(newValue);
            close();
        };
        listView.getSelectionModel().selectedItemProperty().addListener(selectedItemListener);

        root = new Pane(listBackground, listView);
        root.setPrefWidth(prefWidth + 20);
        root.setStyle("-fx-background-color: transparent;");

        // Stage setup
        Scene rootScene = owner.getScene();
        ownerRoot = rootScene.getRoot();
        rootWindow = rootScene.getWindow();

        scene = new Scene(root);
        scene.getStylesheets().setAll(rootScene.getStylesheets());
        scene.setFill(Color.TRANSPARENT);

        stage = new Stage();
        stage.setScene(scene);
        stage.initOwner(rootWindow);
        stage.initModality(Modality.NONE);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.sizeToScene();


        // Listeners, handlers
        rootHeightListener = (observable, oldValue, newValue) -> doLayout();

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
        root.heightProperty().addListener(rootHeightListener);
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
        if (root.getHeight() > 0 && root.getWidth() > 0 && width > 0 && height > 0) {
            Bounds localToScene = owner.localToScene(owner.getBoundsInLocal());
            root.setLayoutX(localToScene.getMinX() - 31);
            root.setLayoutY(localToScene.getMinY() + 32);
            layoutListView();
        }
    }

    private void updatePosition() {
        Bounds boundsInLocal = ownerRoot.getBoundsInLocal();
        width = boundsInLocal.getWidth();
        height = boundsInLocal.getHeight();
        Bounds localToScreen = ownerRoot.localToScreen(boundsInLocal);
        stage.setX(localToScreen.getMinX() + offsetX);
        stage.setY(localToScreen.getMinY() + offsetY);
    }

    private void cleanup() {
        if (fixPositionsScheduler != null) {
            fixPositionsScheduler.stop();
        }
        rootWindow.xProperty().removeListener(positionListener);
        rootWindow.yProperty().removeListener(positionListener);
        rootWindow.widthProperty().removeListener(positionListener);
        root.heightProperty().removeListener(rootHeightListener);
        stage.setOnCloseRequest(null);
        scene.setOnKeyPressed(null);
        listView.getSelectionModel().selectedItemProperty().removeListener(selectedItemListener);

    }

    protected void layoutListView() {
        ObservableList<T> items = listView.getItems();
        if (items.isEmpty()) {
            listBackground.getPoints().clear();
        } else {
            double x = 5;
            double listOffset = 8;
            // relative to visible top-left point 
            double arrowX_l = 22;
            double arrowX_m = 31.5;
            double arrowX_r = 41;
            double height = Math.min(getVisibleRowCount(), items.size()) * getRowHeight() + listOffset;
            double width = prefWidth - 10;
            // double y = root.getHeight() - 25;
            double y = 0;
            double arrowY_m = y - 7.5;
            listBackground.getPoints().setAll(
                    x, y,
                    x + arrowX_l, y,
                    x + arrowX_m, arrowY_m,
                    x + arrowX_r, y,
                    x + width, y,
                    x + width, y + height,
                    x, y + height);

            listBackground.setLayoutX(0);
            listView.setLayoutX(x);
            listView.setLayoutY(y + listOffset);
            listView.setPrefWidth(width);
            listView.setPrefHeight(height - listOffset + 2);
            listView.autosize();
            root.setPrefHeight(listView.getHeight() + 32.5);
        }
    }

    private int getVisibleRowCount() {
        return 10;
    }

    protected int getRowHeight() {
        return 40;
    }
}