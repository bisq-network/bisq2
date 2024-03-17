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

import bisq.desktop.common.threading.UIScheduler;
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

import java.util.concurrent.TimeUnit;

@Slf4j
public class WaitingAnimation extends StackPane {
    public static final int INTERVAL = 1000;

    private final ImageView spinningCircle;
    private ImageView waitingStateIcon;
    private WaitingState waitingState;
    private final RotateTransition rotate;
    private final FadeTransition fadeTransition;
    private Scene scene;
    private ChangeListener<Scene> sceneListener;
    private ChangeListener<Boolean> focusListener;
    private UIScheduler uiScheduler;

    public WaitingAnimation(WaitingState waitingState) {
        setState(waitingState);

        setAlignment(Pos.CENTER);

        spinningCircle = ImageUtil.getImageViewById(getSpinningCircleIconId(waitingState));
        spinningCircle.setFitHeight(78);
        spinningCircle.setFitWidth(78);
        spinningCircle.setPreserveRatio(true);

        getChildren().add(spinningCircle);

        fadeTransition = new FadeTransition(Duration.millis(INTERVAL), spinningCircle);
        fadeTransition.setFromValue(0);
        fadeTransition.setToValue(1);

        rotate = new RotateTransition(Duration.millis(INTERVAL), spinningCircle);
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
            waitingStateIcon = ImageUtil.getImageViewById(getWaitingStateIconId(waitingState));
            getChildren().add(waitingStateIcon);
        }
    }

    private String getWaitingStateIconId(WaitingState waitingState) {
        switch (waitingState) {
            case TAKE_BISQ_EASY_OFFER:
                return "take-bisq-easy-offer";
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

    private String getSpinningCircleIconId(WaitingState waitingState) {
        return waitingState == WaitingState.TAKE_BISQ_EASY_OFFER ? "take-bisq-easy-offer-circle" : "spinning-circle";
    }

    public void play() {
        rotate.play();
        fadeTransition.play();
    }

    public void playIndefinitely() {
        playRepeated(0, 4 * INTERVAL, TimeUnit.MILLISECONDS, Long.MAX_VALUE);
    }

    public void playRepeated(long initialDelay, long delay, TimeUnit timeUnit, long cycles) {
        stop();
        uiScheduler = UIScheduler.run((this::play)).repeated(initialDelay, delay, timeUnit, cycles);
    }

    public void stop() {
        rotate.stop();
        fadeTransition.stop();
        if (uiScheduler != null) {
            uiScheduler.stop();
            uiScheduler = null;
        }
    }
}
