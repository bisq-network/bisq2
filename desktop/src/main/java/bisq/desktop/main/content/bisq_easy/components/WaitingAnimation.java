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

package bisq.desktop.main.content.bisq_easy.components;

import bisq.desktop.common.utils.ImageUtil;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WaitingAnimation extends StackPane {
    private ImageView waitingStateIcon;
    private WaitingState waitingState;
    private final RotateTransition rotate;
    private final FadeTransition fadeTransition;
    private Scene scene;
    private ChangeListener<Scene> sceneListener;
    private ChangeListener<Boolean> focusListener;

    public WaitingAnimation(WaitingState waitingState) {
        this();
        setState(waitingState);
    }

    public WaitingAnimation() {
        setAlignment(Pos.CENTER);
        ImageView spinningCircle = ImageUtil.getImageViewById("spinning-circle");

        spinningCircle.setFitHeight(78);
        spinningCircle.setFitWidth(78);
        spinningCircle.setPreserveRatio(true);

        getChildren().add(spinningCircle);

        fadeTransition = new FadeTransition(Duration.millis(1000), spinningCircle);
        fadeTransition.setFromValue(0);
        fadeTransition.setToValue(1);

        rotate = new RotateTransition(Duration.millis(1000), spinningCircle);
        rotate.setByAngle(360);

        sceneListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                focusListener = new ChangeListener<>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                        if (newValue) {
                            rotate.playFromStart();
                            spinningCircle.setOpacity(0);
                            fadeTransition.playFromStart();
                        }
                    }
                };
                scene = getScene();
                scene.getWindow().focusedProperty().addListener(focusListener);
            } else {
                if (scene != null) {
                    scene.getWindow().focusedProperty().removeListener(focusListener);
                    scene = null;
                }
                sceneProperty().removeListener(sceneListener);
            }
        };
        sceneProperty().addListener(sceneListener);
    }

    public void setState(WaitingState newWaitingState) {
        if (waitingState != newWaitingState) {
            waitingState = newWaitingState;
            updateWaitingStateIcon();
        }
    }

    private void updateWaitingStateIcon() {
        if (waitingStateIcon != null) {
            getChildren().remove(waitingStateIcon);
            waitingStateIcon = null;
        }

        if (waitingState != null) {
            waitingStateIcon = ImageUtil.getImageViewById(getIconId(waitingState));
            getChildren().add(waitingStateIcon);
        }
    }

    private String getIconId(WaitingState waitingState) {
        switch (waitingState) {
            case ACCOUNT_DATA:
                return "account-data";
            case FIAT_PAYMENT:
                return "fiat-payment";
            case FIAT_PAYMENT_CONFIRMATION:
                return "fiat-payment-confirmation";
            case BITCOIN_ADDRESS:
                return "bitcoin-address";
            case BITCOIN_PAYMENT:
                return "bitcoin-payment";
            case BITCOIN_CONFIRMATION:
                return "bitcoin-confirmation";
            default:
                throw new IllegalArgumentException("Unknown WaitingState: " + waitingState);
        }
    }

    public void play() {
        rotate.play();
        fadeTransition.play();
    }

    public void stop() {
        rotate.stop();
        fadeTransition.stop();
    }
}
