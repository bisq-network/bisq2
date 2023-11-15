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

import bisq.desktop.common.Layout;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.NavigationView;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OverlayView extends NavigationView<AnchorPane, OverlayModel, OverlayController> {
    private static final Interpolator INTERPOLATOR = Interpolator.SPLINE(0.25, 0.1, 0.25, 1);

    private final Region owner;
    private final Scene ownerScene;
    private final Stage stage;
    private final Window window;
    private final ChangeListener<Number> positionListener;
    private final Scene scene;
    private UIScheduler centerTime;
    private final EventHandler<KeyEvent> onKeyPressedHandler = controller::onKeyPressed;

    public OverlayView(OverlayModel model, OverlayController controller, Region owner) {
        super(new AnchorPane(), model, controller);

        this.owner = owner;
        ownerScene = owner.getScene();

        scene = new Scene(root);
        scene.getStylesheets().setAll(ownerScene.getStylesheets());
        scene.setFill(Color.TRANSPARENT);

        stage = new Stage();
        stage.setScene(scene);
        stage.sizeToScene();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initOwner(owner.getScene().getWindow());
        stage.initModality(Modality.NONE);
        stage.setOnCloseRequest(event -> {
            event.consume();
            hide();
        });
        window = ownerScene.getWindow();

        // On Linux the owner stage does not move the child stage as it does on Mac
        // So we need to apply centerPopup. Further, with fast movements the handler loses
        // the latest position, with a delay it fixes that.
        // Also, on Mac sometimes the popups are positioned outside the main app, so keep it for all OS
        positionListener = (observable, oldValue, newValue) -> {
            layout();
            UIThread.runOnNextRenderFrame(this::layout);

            if (centerTime != null) {
                centerTime.stop();
            }

            centerTime = UIScheduler.run(this::layout).after(3000);
        };

        model.getView().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Region childRoot = newValue.getRoot();
                Layout.pinToAnchorPane(childRoot, 0, 0, 0, 0);
                root.getChildren().setAll(childRoot);
                show(childRoot.getPrefWidth(), childRoot.getPrefHeight());
                Transitions.transitContentViews(oldValue, newValue);
            } else {
                hide();
            }
        });
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }

    private void show(double prefWidth, double prefHeight) {
        scene.addEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);
        ownerScene.addEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);

        prefWidth = Math.min(prefWidth, owner.getWidth());
        prefHeight = Math.min(prefHeight, owner.getHeight());

        root.setMinWidth(prefWidth);
        root.setMinHeight(prefHeight);
        root.setMaxWidth(prefWidth);
        root.setMaxHeight(prefHeight);

        window.xProperty().addListener(positionListener);
        window.yProperty().addListener(positionListener);
        window.widthProperty().addListener(positionListener);
        window.heightProperty().addListener(positionListener);

        model.getTransitionsType().apply(owner);
        animateDisplay(controller::onShown);
        UIScheduler.run(() -> {
                    stage.show();
                    layout();
                })
                .after(150);
    }

    void hide() {
        scene.removeEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);
        ownerScene.removeEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);

        Transitions.removeEffect(owner);
        animateHide(() -> {
            if (stage != null) {
                stage.hide();
            }

            if (centerTime != null) {
                centerTime.stop();
            }

            if (window != null && positionListener != null) {
                window.xProperty().removeListener(positionListener);
                window.yProperty().removeListener(positionListener);
                window.widthProperty().removeListener(positionListener);
            }

            root.getChildren().clear();
            controller.onHidden();
        });
    }

    private void animateDisplay(Runnable onFinishedHandler) {
        double duration = model.getDuration(400);
        Timeline timeline = new Timeline();
        ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();

        double startScale = 0.25;
        keyFrames.add(new KeyFrame(Duration.millis(0),
                new KeyValue(root.opacityProperty(), 0, INTERPOLATOR),
                new KeyValue(root.scaleXProperty(), startScale, INTERPOLATOR),
                new KeyValue(root.scaleYProperty(), startScale, INTERPOLATOR)

        ));
        keyFrames.add(new KeyFrame(Duration.millis(duration),
                new KeyValue(root.opacityProperty(), 1, INTERPOLATOR),
                new KeyValue(root.scaleXProperty(), 1, INTERPOLATOR),
                new KeyValue(root.scaleYProperty(), 1, INTERPOLATOR)
        ));

        timeline.setOnFinished(e -> onFinishedHandler.run());
        timeline.play();
    }

    private void layout() {
        Bounds ownerBoundsInLocal = owner.getBoundsInLocal();
        Point2D ownerInLocalTopLeft = new Point2D(ownerBoundsInLocal.getMinX(), ownerBoundsInLocal.getMinY());
        Point2D ownerToScreenTopLeft = owner.localToScreen(ownerInLocalTopLeft);
        double titleBarHeight = ownerToScreenTopLeft.getY() - ownerScene.getWindow().getY();
        stage.setX(Math.round(window.getX() + (owner.getWidth() - stage.getWidth()) / 2));
        stage.setY(Math.round(window.getY() + titleBarHeight + (owner.getHeight() - stage.getHeight()) / 2));
    }

    private void animateHide(Runnable onFinishedHandler) {
        double duration = model.getDuration(Transitions.DEFAULT_DURATION / 2d);
        Timeline timeline = new Timeline();
        ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
        double endScale = 0.25;
        keyFrames.add(new KeyFrame(Duration.millis(0),
                new KeyValue(root.opacityProperty(), 1, INTERPOLATOR),
                new KeyValue(root.scaleXProperty(), 1, INTERPOLATOR),
                new KeyValue(root.scaleYProperty(), 1, INTERPOLATOR)
        ));
        keyFrames.add(new KeyFrame(Duration.millis(duration),
                new KeyValue(root.opacityProperty(), 0, INTERPOLATOR),
                new KeyValue(root.scaleXProperty(), endScale, INTERPOLATOR),
                new KeyValue(root.scaleYProperty(), endScale, INTERPOLATOR)
        ));

        timeline.setOnFinished(e -> onFinishedHandler.run());
        timeline.play();
    }
}