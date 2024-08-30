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

package bisq.webcam.view;

import javafx.animation.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WaitingAnimation extends StackPane {
    public static final int INTERVAL = 1000;

    private final ImageView spinningCircle;
    private ImageView waitingStateIcon;
    private final RotateTransition rotate;
    private final FadeTransition fadeTransition;
    private Scene scene;
    private ChangeListener<Scene> sceneListener;
    private ChangeListener<Boolean> focusListener;
    private final BooleanProperty startAnimationProperty = new SimpleBooleanProperty();
    private final Timeline timeline = new Timeline();

    public WaitingAnimation() {

        setAlignment(Pos.CENTER);

        spinningCircle = new ImageView();
        spinningCircle.setId("spinning-circle");
        spinningCircle.setFitHeight(78);
        spinningCircle.setFitWidth(78);
        spinningCircle.setPreserveRatio(true);

        waitingStateIcon = new ImageView();
        waitingStateIcon.setId("scan-with-camera");

        getChildren().addAll(spinningCircle, waitingStateIcon);

        fadeTransition = new FadeTransition(Duration.millis(INTERVAL), spinningCircle);
        fadeTransition.setFromValue(0);
        fadeTransition.setToValue(1);

        rotate = new RotateTransition(Duration.millis(INTERVAL), spinningCircle);
        rotate.setByAngle(360);

        timeline.setCycleCount(Integer.MAX_VALUE);
        ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
        keyFrames.add(new KeyFrame(Duration.millis(0), new KeyValue(startAnimationProperty, true, Interpolator.LINEAR)));
        keyFrames.add(new KeyFrame(Duration.millis(4000), new KeyValue(startAnimationProperty, false, Interpolator.EASE_BOTH)));
        startAnimationProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                rotate.playFromStart();
                fadeTransition.playFromStart();
            }
        });

        focusListener = (observable, oldValue, newValue) -> {
            if (newValue) {
                playFromStart();
            }
        };

        sceneListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                focusListener = (observable12, oldValue12, focus) -> {
                    if (focus) {
                        playFromStart();
                    }
                };
                scene = newValue;
                scene.windowProperty().addListener((observable1, oldValue1, newValue1) -> {
                    if (newValue1 != null) {
                        scene.getWindow().focusedProperty().addListener(focusListener);
                    }
                });

            } else {
                if (scene != null) {
                    scene.getWindow().focusedProperty().removeListener(focusListener);
                    scene = null;
                }
                sceneProperty().removeListener(sceneListener);
            }
        };
        sceneProperty().addListener(sceneListener);

        timeline.play();
    }

    private void playFromStart() {
        startAnimationProperty.set(false);
        spinningCircle.setOpacity(0);
        timeline.playFromStart();
    }

    public void stop() {
        rotate.stop();
        fadeTransition.stop();
        timeline.stop();
    }
}
