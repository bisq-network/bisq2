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
import bisq.desktop.common.utils.Icons;
import bisq.i18n.Res;
import bisq.settings.DisplaySettings;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Camera;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.transform.Rotate;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class Notification extends Overlay<Notification> {
    private static Region owner;
    private static DisplaySettings displaySettings;

    protected final static double DEFAULT_WIDTH = 668;

    public static void init(Region owner, DisplaySettings displaySettings) {
        Notification.owner = owner;
        Notification.displaySettings = displaySettings;
    }


    private boolean hasBeenDisplayed;
    private boolean autoClose;
    private UIScheduler autoCloseTimer;

    public Notification() {
        width = 413; // 320 visible bg because of insets
        type = Type.Notification;
    }

    void onReadyForDisplay() {
        super.display();

        if (autoClose && autoCloseTimer == null) {
            autoCloseTimer = UIScheduler.run(this::doClose).after(5000);
        }

        stage.addEventHandler(MouseEvent.MOUSE_PRESSED, (event) -> doClose());
    }

    @Override
    public void hide() {
        animateHide();
    }

    @Override
    protected void onShow() {
        Manager.queueForDisplay(this);
    }

    @Override
    protected void onHidden() {
        Manager.onHidden(this);
    }

    public Notification tradeHeadLine(String tradeId) {
        return headLine(Res.get("notification.trade.headline", tradeId));
    }

    public Notification disputeHeadLine(String tradeId) {
        return headLine(Res.get("notification.ticket.headline", tradeId));
    }

    @Override
    public void show() {
        super.show();
        hasBeenDisplayed = true;
    }


    public Notification autoClose() {
        autoClose = true;
        return this;
    }

    @Override
    protected void animateHide(Runnable onFinishedHandler) {
        if (autoCloseTimer != null) {
            autoCloseTimer.stop();
            autoCloseTimer = null;
        }

        if (displaySettings.isUseAnimations()) {
            double duration = getDuration(400);
            Interpolator interpolator = Interpolator.SPLINE(0.25, 0.1, 0.25, 1);

            gridPane.setRotationAxis(Rotate.X_AXIS);
            Camera camera = gridPane.getScene().getCamera();
            gridPane.getScene().setCamera(new PerspectiveCamera());

            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(gridPane.rotateProperty(), 0, interpolator),
                    new KeyValue(gridPane.opacityProperty(), 1, interpolator)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(gridPane.rotateProperty(), -90, interpolator),
                    new KeyValue(gridPane.opacityProperty(), 0, interpolator)
            ));
            timeline.setOnFinished(event -> {
                gridPane.setRotate(0);
                gridPane.setRotationAxis(Rotate.Z_AXIS);
                gridPane.getScene().setCamera(camera);
                onFinishedHandler.run();
            });
            timeline.play();
        } else {
            onFinishedHandler.run();
        }
    }

    @Override
    protected void animateDisplay() {
        if (displaySettings.isUseAnimations()) {
            double startX = 320;
            double duration = getDuration(600);
            Interpolator interpolator = Interpolator.SPLINE(0.25, 0.1, 0.25, 1);

            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(gridPane.opacityProperty(), 0, interpolator),
                    new KeyValue(gridPane.translateXProperty(), startX, interpolator)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(gridPane.opacityProperty(), 1, interpolator),
                    new KeyValue(gridPane.translateXProperty(), 0, interpolator)
            ));
            timeline.play();
        }
    }


    @Override
    protected void createGridPane() {
        super.createGridPane();
        gridPane.setPadding(new Insets(62, 62, 62, 62));
    }

    @Override
    protected void addButtons() {
        buttonDistance = 10;
        super.addButtons();
    }

    @Override
    protected void applyStyles() {
        gridPane.getStyleClass().add("notification-popup-bg");
        if (headLineLabel != null)
            headLineLabel.getStyleClass().add("notification-popup-headline");

        headlineIcon.getStyleClass().add("popup-icon-information");
        headlineIcon.setManaged(true);
        headlineIcon.setVisible(true);
        headlineIcon.setPadding(new Insets(1));
        Icons.getIconForLabel(AwesomeIcon.INFO_SIGN, headlineIcon, "1em");
        if (actionButton != null)
            actionButton.getStyleClass().add("compact-button");
    }

    @Override
    protected void setModality() {
        stage.initOwner(owner.getScene().getWindow());
        stage.initModality(Modality.NONE);
    }

    @Override
    protected void layout() {
        Window window = owner.getScene().getWindow();
        double titleBarHeight = window.getHeight() - owner.getScene().getHeight();
        double shadowInset = 44;
        stage.setX(Math.round(window.getX() + window.getWidth() + shadowInset - stage.getWidth()));
        stage.setY(Math.round(window.getY() + titleBarHeight - shadowInset));
    }

    @Override
    protected void addEffectToBackground() {
    }

    @Override
    protected void removeEffectFromBackground() {
    }

    public boolean isHasBeenDisplayed() {
        return hasBeenDisplayed;
    }

    @Slf4j
    private static class Manager {
        private static final Queue<Notification> popups = new LinkedBlockingQueue<>(5);
        private static Notification displayedPopup;

        private static void queueForDisplay(Notification popup) {
            boolean result = popups.offer(popup);
            if (!result) {
                log.warn("The capacity is full with popups in the queue.\n\t" +
                        "Not added new popup=" + popup);
            }
            displayNext();
        }

        private static void onHidden(Notification popup) {
            if (displayedPopup == null || displayedPopup == popup) {
                displayedPopup = null;
                displayNext();
            } else {
                log.warn("We got a isHidden called with a wrong popup.\n\t" +
                        "popup (argument)=" + popup + "\n\tdisplayedPopup=" + displayedPopup);
            }
        }

        private static void displayNext() {
            if (displayedPopup == null) {
                if (!popups.isEmpty()) {
                    displayedPopup = popups.poll();
                    displayedPopup.onReadyForDisplay();
                }
            }
        }
    }
}
