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

package bisq.desktop.popups;

import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.i18n.Res;
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
public class ComboBoxOverlay<T> {
    private final static double PADDING = 10;
    private final Region owner;
    private final double prefWidth;
    private final double offsetX;
    private final double offsetY;

    private final Parent ownerRoot;
    private final Window rootWindow;
    private final Scene scene;
    private final Stage stage;
    private final ChangeListener<Number> positionListener;
    private final AutoCompleteComboBox<T> comboBox;
    private final ChangeListener<Number> heightListener;
    private double width;
    private double height;
    private UIScheduler fixPositionsScheduler;
    private final Pane root;
    protected final Polygon listBackground = new Polygon();

    public ComboBoxOverlay(Region owner,
                           ObservableList<T> items,
                           Callback<ListView<T>, ListCell<T>> cellFactory,
                           Consumer<T> selectionHandler,
                           double prefWidth) {
        this(owner, items, cellFactory, selectionHandler, prefWidth, 0, 0);
    }

    public ComboBoxOverlay(Region owner,
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

        comboBox = new AutoCompleteComboBox<>(items, Res.get("tradeChat.addMarketChannel").toUpperCase(), Res.get("tradeChat.addMarketChannel.prompt"));
        comboBox.setCellFactory(cellFactory);
        comboBox.setPrefWidth(prefWidth - 2 * PADDING);
        comboBox.setLayoutX(PADDING);
        comboBox.setLayoutY(PADDING);
        comboBox.getAutoCompleteComboBoxSkin().setDropShadowColor(Color.rgb(0, 0, 0, 0.2));
        comboBox.setOnChangeConfirmed(e -> {
            selectionHandler.accept(comboBox.getSelectionModel().getSelectedItem());
            close();
        });
        UIThread.runOnNextRenderFrame(() -> comboBox.getEditorTextField().requestFocus());

        root = new Pane(listBackground, comboBox);
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
        heightListener = (observable, oldValue, newValue) -> doLayout();

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
        comboBox.getAutoCompleteComboBoxSkin().getListView().heightProperty().addListener(heightListener);
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
        comboBox.getAutoCompleteComboBoxSkin().getListView().heightProperty().removeListener(heightListener);
        stage.setOnCloseRequest(null);
        scene.setOnKeyPressed(null);
        comboBox.setOnChangeConfirmed(null);
    }

    protected void layoutListView() {
        ObservableList<T> items = comboBox.getItems();
        if (items.isEmpty()) {
            listBackground.getPoints().clear();
        } else {
            double x = 0;
            double listOffset = 8;
            // relative to visible top-left point 
            double arrowX_l = 22;
            double arrowX_m = 31.5;
            double arrowX_r = 41;
            double height = 33 + 2 * PADDING + comboBox.getHeight() + listOffset + Math.min(comboBox.getVisibleRowCount(), items.size()) * getRowHeight();
            double width = prefWidth;
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

            root.setPrefHeight(height + 25);
        }
    }

    protected int getRowHeight() {
        return 40;
    }
}