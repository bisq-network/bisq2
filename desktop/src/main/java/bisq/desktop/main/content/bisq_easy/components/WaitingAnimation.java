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
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class WaitingAnimation extends StackPane {
    private final RotateTransition rotate;
    private ImageView waitingStateIcon;
    private WaitingState waitingState;

    public WaitingAnimation(WaitingState waitingState) {
        this();
        setState(waitingState);
    }

    public WaitingAnimation() {
        setAlignment(Pos.CENTER);
        ImageView spinningCircle = ImageUtil.getImageViewById("spinning-circle");
        getChildren().add(spinningCircle);
        rotate = new RotateTransition(Duration.seconds(1), spinningCircle);
        rotate.setByAngle(360);
        rotate.setCycleCount(Animation.INDEFINITE);
        rotate.setInterpolator(Interpolator.LINEAR);
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
            waitingStateIcon.setScaleX(0.85);
            waitingStateIcon.setScaleY(0.85);
            getChildren().add(waitingStateIcon);
        }
    }

    private String getIconId(WaitingState waitingState) {
        switch (waitingState) {
            case ACCOUNT_DATA:
               return "account-data";
            case FIAT_PAYMENT:
            case FIAT_PAYMENT_CONFIRMATION:
                return "fiat-payment";
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
    }

    public void stop() {
        rotate.stop();
    }
}
