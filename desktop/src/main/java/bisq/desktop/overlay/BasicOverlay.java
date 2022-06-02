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

import bisq.common.util.OsUtils;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.Transitions;
import bisq.settings.DisplaySettings;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BasicOverlay {
    @Getter
    public static Region owner;
    public static DisplaySettings displaySettings;

    public static void init(Region owner, DisplaySettings displaySettings) {
        BasicOverlay.owner = owner;
        BasicOverlay.displaySettings = displaySettings;
    }

    protected Stage stage;
    @Getter
    protected VBox root;
    protected double width = 668;

    protected ChangeListener<Number> positionListener;
    protected UIScheduler centerTime;


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void show() {
        createMainContent();
        addContent();
        getRootContainer().getStyleClass().add("popup-bg");
        display();
        onShow();
    }

    public void hide() {
        if (root != null) {
            animateHide();
        }
    }

    protected void onShow() {
    }

    protected void onHidden() {
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Protected
    ///////////////////////////////////////////////////////////////////////////////////////////

    protected void animateHide() {
        animateHide(() -> {
            Transitions.removeEffect(owner);

            if (stage != null) {
                stage.hide();
            }

            if (centerTime != null) {
                centerTime.stop();
            }

            Scene rootScene = owner.getScene();
            if (rootScene != null) {
                Window window = rootScene.getWindow();
                if (window != null && positionListener != null) {
                    window.xProperty().removeListener(positionListener);
                    window.yProperty().removeListener(positionListener);
                    window.widthProperty().removeListener(positionListener);
                }
            }

            onHidden();
        });
    }

    protected void createMainContent() {
        root = new VBox();
        root.setPadding(new Insets(0));
        root.setPrefWidth(width);
    }

    protected void display() {
        if (owner != null) {
            Scene rootScene = owner.getScene();
            if (rootScene != null) {
                Scene scene = new Scene(getRootContainer());
                scene.getStylesheets().setAll(rootScene.getStylesheets());
                scene.setFill(Color.TRANSPARENT);

                setupKeyHandler(scene);

                stage = new Stage();
                stage.setScene(scene);
                Window window = rootScene.getWindow();
                setModality();
                stage.initStyle(StageStyle.TRANSPARENT);
                stage.setOnCloseRequest(event -> {
                    event.consume();
                    hide();
                });
                stage.sizeToScene();
                stage.show();

                layout();

                Transitions.darken(owner, Transitions.DEFAULT_DURATION, false);

                // On Linux the owner stage does not move the child stage as it does on Mac
                // So we need to apply centerPopup. Further, with fast movements the handler loses
                // the latest position, with a delay it fixes that.
                // Also, on Mac sometimes the popups are positioned outside the main app, so keep it for all OS
                positionListener = (observable, oldValue, newValue) -> {
                    if (stage != null) {
                        layout();

                        if (centerTime != null)
                            centerTime.stop();

                        centerTime = UIScheduler.run(this::layout).after(3000);
                    }
                };
                window.xProperty().addListener(positionListener);
                window.yProperty().addListener(positionListener);
                window.widthProperty().addListener(positionListener);

                animateDisplay();
            }
        }
    }

    protected Region getRootContainer() {
        return root;
    }

    protected void setupKeyHandler(Scene scene) {
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.ENTER) {
                e.consume();
                hide();
            }
        });
    }

    protected void animateDisplay() {
        Region rootContainer = this.getRootContainer();

        rootContainer.setOpacity(0);
        Interpolator interpolator = Interpolator.SPLINE(0.25, 0.1, 0.25, 1);
        double duration = getDuration(400);
        Timeline timeline = new Timeline();
        ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();

        double startScale = 0.25;
        keyFrames.add(new KeyFrame(Duration.millis(0),
                new KeyValue(rootContainer.opacityProperty(), 0, interpolator),
                new KeyValue(rootContainer.scaleXProperty(), startScale, interpolator),
                new KeyValue(rootContainer.scaleYProperty(), startScale, interpolator)

        ));
        keyFrames.add(new KeyFrame(Duration.millis(duration),
                new KeyValue(rootContainer.opacityProperty(), 1, interpolator),
                new KeyValue(rootContainer.scaleXProperty(), 1, interpolator),
                new KeyValue(rootContainer.scaleYProperty(), 1, interpolator)
        ));

        timeline.play();
    }

    protected void animateHide(Runnable onFinishedHandler) {
        Interpolator interpolator = Interpolator.SPLINE(0.25, 0.1, 0.25, 1);
        double duration = getDuration(200);
        Timeline timeline = new Timeline();
        ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();

        Region rootContainer = getRootContainer();
        double endScale = 0.25;
        keyFrames.add(new KeyFrame(Duration.millis(0),
                new KeyValue(rootContainer.opacityProperty(), 1, interpolator),
                new KeyValue(rootContainer.scaleXProperty(), 1, interpolator),
                new KeyValue(rootContainer.scaleYProperty(), 1, interpolator)
        ));
        keyFrames.add(new KeyFrame(Duration.millis(duration),
                new KeyValue(rootContainer.opacityProperty(), 0, interpolator),
                new KeyValue(rootContainer.scaleXProperty(), endScale, interpolator),
                new KeyValue(rootContainer.scaleYProperty(), endScale, interpolator)
        ));

        timeline.setOnFinished(e -> onFinishedHandler.run());
        timeline.play();
    }

    protected void layout() {
        Scene rootScene = owner.getScene();
        if (rootScene != null) {
            Window window = rootScene.getWindow();
            double titleBarHeight = window.getHeight() - rootScene.getHeight();

            if (OsUtils.isWindows()) {
                titleBarHeight -= 9;
            }
            stage.setX(Math.round(window.getX() + (owner.getWidth() - stage.getWidth()) / 2));
            stage.setY(Math.round(window.getY() + titleBarHeight + (owner.getHeight() - stage.getHeight()) / 2));
        }
    }

    protected void setModality() {
        stage.initOwner(owner.getScene().getWindow());
        stage.initModality(Modality.WINDOW_MODAL);
    }

    protected void addContent() {
    }

    protected double getDuration(double duration) {
        return displaySettings.isUseAnimations() ? duration : 1;
    }
}
