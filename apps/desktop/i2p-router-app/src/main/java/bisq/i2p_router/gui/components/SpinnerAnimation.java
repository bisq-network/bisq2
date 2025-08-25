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

package bisq.i2p_router.gui.components;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SpinnerAnimation extends StackPane {
    private final Timeline timeline;
    private final ImageView i2pLogo;

    public SpinnerAnimation(double spinnerSize) {
        setAlignment(Pos.CENTER);

        ImageView spinningCircle = new ImageView();
        spinningCircle.setId("spinning-circle");
        spinningCircle.setFitWidth(spinnerSize);
        spinningCircle.setFitHeight(spinnerSize);

        i2pLogo = new ImageView();
        i2pLogo.setId("connecting");
        i2pLogo.setFitWidth(spinnerSize);
        i2pLogo.setFitHeight(spinnerSize);

        getChildren().addAll(spinningCircle, i2pLogo);

        Duration second = Duration.seconds(1);
        timeline = new Timeline();
        ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
        keyFrames.add(new KeyFrame(Duration.ZERO, new KeyValue(spinningCircle.opacityProperty(), 0, Interpolator.LINEAR)));
        keyFrames.add(new KeyFrame(second, new KeyValue(spinningCircle.opacityProperty(), 1, Interpolator.LINEAR)));
        keyFrames.add(new KeyFrame(second.multiply(2), new KeyValue(spinningCircle.opacityProperty(), 0, Interpolator.LINEAR)));
        keyFrames.add(new KeyFrame(second.multiply(3), new KeyValue(spinningCircle.opacityProperty(), 1, Interpolator.LINEAR)));

        keyFrames.add(new KeyFrame(Duration.ZERO, new KeyValue(spinningCircle.rotateProperty(), 0, Interpolator.LINEAR)));
        keyFrames.add(new KeyFrame(second, new KeyValue(spinningCircle.rotateProperty(), 360, Interpolator.LINEAR)));
        keyFrames.add(new KeyFrame(second.multiply(2), new KeyValue(spinningCircle.rotateProperty(), 360, Interpolator.LINEAR)));
        keyFrames.add(new KeyFrame(second.multiply(3), new KeyValue(spinningCircle.rotateProperty(), 720, Interpolator.LINEAR)));

        keyFrames.add(new KeyFrame(Duration.ZERO, new KeyValue(i2pLogo.opacityProperty(), 0.2, Interpolator.LINEAR)));
        keyFrames.add(new KeyFrame(second, new KeyValue(i2pLogo.opacityProperty(), 1, Interpolator.LINEAR)));
        keyFrames.add(new KeyFrame(second.multiply(4), new KeyValue(i2pLogo.opacityProperty(), 1, Interpolator.LINEAR)));

        timeline.setOnFinished(e -> timeline.play());
        timeline.setCycleCount(Integer.MAX_VALUE);
    }

    public void play() {
        timeline.play();
    }

    public void stop() {
        timeline.stop();
    }

    public void setI2PIconId(String id) {
        i2pLogo.setId(id);
    }
}
