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

package bisq.desktop.components.controls;

import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
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

import javax.annotation.Nullable;
import java.util.function.Consumer;

@Slf4j
public class ComboBoxWithSearch<T> {
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
    private final Label placeHolder;
    private double width;
    private double height;
    private UIScheduler fixPositionsScheduler;
    private final Pane root;
    protected final Polygon listBackground = new Polygon();
    protected double arrowOffset = 31.5;

    public ComboBoxWithSearch(Region owner,
                              ObservableList<T> items,
                              Callback<ListView<T>, ListCell<T>> cellFactory,
                              Consumer<T> selectionHandler,
                              String description,
                              @Nullable String prompt,
                              double prefWidth) {
        this(owner, items, cellFactory, selectionHandler, description, prompt, prefWidth, 0, 0, 31.5);
    }

    public ComboBoxWithSearch(Region owner,
                              ObservableList<T> items,
                              Callback<ListView<T>, ListCell<T>> cellFactory,
                              Consumer<T> selectionHandler,
                              String description,
                              @Nullable String prompt,
                              double prefWidth,
                              double offsetX,
                              double offsetY,
                              double arrowOffset) {
        this.owner = owner;
        this.prefWidth = prefWidth;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.arrowOffset = arrowOffset;

        DropShadow dropShadow = new DropShadow();
        dropShadow.setBlurType(BlurType.GAUSSIAN);
        dropShadow.setRadius(25);
        dropShadow.setSpread(0.65);
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.4));

        listBackground.setFill(Paint.valueOf("#212121"));
        listBackground.setEffect(dropShadow);

        comboBox = new AutoCompleteComboBox<>(items, description, prompt);
        comboBox.setCellFactory(cellFactory);
        comboBox.setPrefWidth(prefWidth - 2 * PADDING);
        comboBox.setLayoutX(PADDING);
        comboBox.setLayoutY(PADDING);
        comboBox.getAutoCompleteComboBoxSkin().setDropShadowColor(Color.rgb(0, 0, 0, 0.2));
        comboBox.getAutoCompleteComboBoxSkin().setHideArrow(true);
        comboBox.setOnChangeConfirmed(e -> {
            T selectedItem = comboBox.getSelectionModel().getSelectedItem();
            selectionHandler.accept(selectedItem);
            if (selectedItem != null) {
                close();
            }
        });
        UIThread.runOnNextRenderFrame(comboBox::forceRedraw);

        placeHolder = new Label(Res.get("data.noDataAvailable"));
        placeHolder.setVisible(false);
        placeHolder.setManaged(false);
        placeHolder.getStyleClass().add("bisq-text-3");
        placeHolder.setLayoutX(25);
        placeHolder.setLayoutY(80);
        root = new Pane(listBackground, comboBox, placeHolder);
        root.setPrefWidth(prefWidth + 2 * PADDING);
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

        scene.setOnMousePressed(e -> close());
        ownerRoot.setOnMousePressed(e -> close());

        stage.focusedProperty().addListener((observable, hadFocus, hasFocus) -> {
            if (!hasFocus) {
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
        scene.setOnMousePressed(null);
        ownerRoot.setOnMousePressed(null);
    }

    protected void layoutListView() {
        ObservableList<T> items = comboBox.getItems();

        double x = 0;
        double listOffset = items.isEmpty() ? 0 : -17.5;
        // relative to visible top-left point 
        double arrowX_l = -9.5;
        double arrowX_m = 0;
        double arrowX_r = 9.5;
        double height = 33 + 2 * PADDING + comboBox.getHeight() + listOffset + Math.min(comboBox.getVisibleRowCount(), items.size()) * getRowHeight();
        double width = prefWidth;
        double y = 0;
        double arrowY_m = y - 7.5;
        listBackground.getPoints().setAll(
                x, y,
                x + arrowX_l + arrowOffset, y,
                x + arrowX_m + arrowOffset, arrowY_m,
                x + arrowX_r + arrowOffset, y,
                x + width, y,
                x + width, y + height,
                x, y + height);

        root.setPrefHeight(height + 25);

        placeHolder.setManaged(items.isEmpty());
        placeHolder.setVisible(items.isEmpty());
    }

    protected int getRowHeight() {
        return 40;
    }
}