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

package bisq.desktop.common.utils;

import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.settings.DisplaySettings;
import javafx.animation.*;
import javafx.collections.ObservableList;
import javafx.scene.Camera;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Transitions {
    public static final int DEFAULT_DURATION = 600;
    public static final int CROSS_FADE_IN_DURATION = 1500;
    public static final int CROSS_FADE_OUT_DURATION = 1000;
    private static final Interpolator DEFAULT_INTERPOLATOR = Interpolator.SPLINE(0.25, 0.1, 0.25, 1);
    @Setter
    private static DisplaySettings displaySettings;

    private static Timeline removeEffectTimeLine;

    public static void fadeIn(Node node) {
        fadeIn(node, DEFAULT_DURATION);
    }

    public static void fadeIn(Node node, int duration) {
        FadeTransition fade = new FadeTransition(Duration.millis(getDuration(duration)), node);
        fade.setFromValue(0);
        fade.setToValue(1.0);
        fade.play();
    }

    public static FadeTransition fadeOut(Node node) {
        return fadeOut(node, DEFAULT_DURATION, null);
    }

    public static FadeTransition fadeOut(Node node, int duration) {
        return fadeOut(node, duration, null);
    }

    public static FadeTransition fadeOut(Node node, int duration, Runnable finishedHandler) {
        FadeTransition fade = new FadeTransition(Duration.millis(getDuration(duration)), node);
        fade.setFromValue(node.getOpacity());
        fade.setToValue(0.0);
        fade.play();
        if (finishedHandler != null) {
            fade.setOnFinished(actionEvent -> finishedHandler.run());
        }
        return fade;
    }

    public static void fadeOutAndRemove(Node node) {
        fadeOutAndRemove(node, DEFAULT_DURATION);
    }

    public static void fadeOutAndRemove(Node node, int duration) {
        fadeOutAndRemove(node, duration, null);
    }

    public static void fadeOutAndRemove(Node node, int duration, Runnable finishedHandler) {
        FadeTransition fade = fadeOut(node, getDuration(duration), finishedHandler);
        fade.setInterpolator(Interpolator.EASE_IN);
        fade.setOnFinished(actionEvent -> {
            if (node.getParent() != null) {
                ((Pane) (node.getParent())).getChildren().remove(node);
            } else {
                log.error("parent is null at {}", node);
            }
            if (finishedHandler != null) {
                finishedHandler.run();
            }
        });
    }

    public static void blur(Node node) {
        blur(node, DEFAULT_DURATION, -0.1, false, 15);
    }

    public static void blur(Node node, int duration, double brightness, boolean removeNode, double blurRadius) {
        if (removeEffectTimeLine != null)
            removeEffectTimeLine.stop();

        node.setMouseTransparent(true);
        GaussianBlur blur = new GaussianBlur(0.0);
        Timeline timeline = new Timeline();
        KeyValue kv1 = new KeyValue(blur.radiusProperty(), blurRadius);
        KeyFrame kf1 = new KeyFrame(Duration.millis(getDuration(duration)), kv1);
        ColorAdjust darken = new ColorAdjust();
        darken.setBrightness(0.0);
        blur.setInput(darken);
        KeyValue kv2 = new KeyValue(darken.brightnessProperty(), brightness);
        KeyFrame kf2 = new KeyFrame(Duration.millis(getDuration(duration)), kv2);
        timeline.getKeyFrames().addAll(kf1, kf2);
        node.setEffect(blur);
        if (removeNode)
            timeline.setOnFinished(actionEvent -> UIThread.runOnNextRenderFrame(() -> ((Pane) (node.getParent()))
                    .getChildren().remove(node)));
        timeline.play();
    }

    public static void darken(Node node, int duration, boolean removeNode) {
        blur(node, duration, -0.2, removeNode, 0);
    }

    public static void removeEffect(Node node) {
        removeEffect(node, DEFAULT_DURATION);
    }

    private static void removeEffect(Node node, int duration) {
        if (node != null) {
            node.setMouseTransparent(false);
            removeEffectTimeLine = new Timeline();
            if (node.getEffect() instanceof GaussianBlur gaussianBlur) {
                KeyValue kv1 = new KeyValue(gaussianBlur.radiusProperty(), 0.0);
                KeyFrame kf1 = new KeyFrame(Duration.millis(getDuration(duration)), kv1);
                removeEffectTimeLine.getKeyFrames().add(kf1);

                ColorAdjust darken = (ColorAdjust) gaussianBlur.getInput();
                KeyValue kv2 = new KeyValue(darken.brightnessProperty(), 0.0);
                KeyFrame kf2 = new KeyFrame(Duration.millis(getDuration(duration)), kv2);
                removeEffectTimeLine.getKeyFrames().add(kf2);
                removeEffectTimeLine.setOnFinished(actionEvent -> {
                    node.setEffect(null);
                    removeEffectTimeLine = null;
                });
                removeEffectTimeLine.play();
            } else {
                node.setEffect(null);
                removeEffectTimeLine = null;
            }
        }
    }

    private static int getDuration(int duration) {
        return displaySettings.isUseAnimations() ? duration : 1;
    }

    public static void crossFade(Node node1, Node node2) {
        fadeOut(node1, CROSS_FADE_OUT_DURATION)
                .setOnFinished(e -> Transitions.fadeIn(node2, CROSS_FADE_IN_DURATION));
    }


    public static void flapOut(Node node, Runnable onFinishedHandler) {
        if (displaySettings.isUseAnimations()) {
            double duration = getDuration(DEFAULT_DURATION);
            Interpolator interpolator = Interpolator.SPLINE(0.25, 0.1, 0.25, 1);

            node.setRotationAxis(Rotate.X_AXIS);
            Camera camera = node.getScene().getCamera();
            node.getScene().setCamera(new PerspectiveCamera());

            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(node.rotateProperty(), 0, interpolator),
                    new KeyValue(node.opacityProperty(), 1, interpolator)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(node.rotateProperty(), -90, interpolator),
                    new KeyValue(node.opacityProperty(), 0, interpolator)
            ));
            timeline.setOnFinished(event -> {
                node.setRotate(0);
                node.setRotationAxis(Rotate.Z_AXIS);
                node.getScene().setCamera(camera);
                onFinishedHandler.run();
            });
            timeline.play();
        } else {
            onFinishedHandler.run();
        }
    }

    public static void transitHorizontal(Region nodeIn, Region nodeOut) {
        nodeIn.setOpacity(0);
        UIScheduler.run(() -> slideInLeft(nodeIn, () -> {
        })).after(DEFAULT_DURATION / 4);
        slideOutRight(nodeOut, () -> {
            if (nodeOut.getParent() != null) {
                if (nodeOut.getParent() instanceof Pane pane) {
                    pane.getChildren().remove(nodeOut);
                }
            }
        });
    }

    public static void transitInNewTab(Region nodeIn) {
        nodeIn.setOpacity(0);
        fadeIn(nodeIn, DEFAULT_DURATION / 2);
    }

    public static void transitContentViews(Region nodeIn, Region nodeOut) {
        nodeIn.setOpacity(0);
        UIScheduler.run(() -> fadeIn(nodeIn)).after(DEFAULT_DURATION / 2);
        slideOutRight(nodeOut, () -> {
            if (nodeOut.getParent() != null) {
                if (nodeOut.getParent() instanceof Pane pane) {
                    pane.getChildren().remove(nodeOut);
                }
            }
        });
    }

    public static void slideInTop(Region node, int duration) {
        slideInTop(node, duration, () -> {
        });
    }

    public static void slideInTop(Region node, int duration, Runnable onFinishedHandler) {
        if (displaySettings.isUseAnimations()) {
            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            log.error("node.getHeight() " + node.getHeight());
            double start = node.getLayoutY() - node.getHeight();
            double end = node.getLayoutY();
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(node.opacityProperty(), 0, Interpolator.LINEAR),
                    new KeyValue(node.translateYProperty(), start, Interpolator.LINEAR)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(node.opacityProperty(), 1, Interpolator.EASE_OUT),
                    new KeyValue(node.translateYProperty(), end, Interpolator.EASE_OUT)
            ));

            timeline.setOnFinished(e -> onFinishedHandler.run());
            timeline.play();
        } else {
            onFinishedHandler.run();
        }
    }

    public static void slideInLeft(Region node, Runnable onFinishedHandler) {
        if (displaySettings.isUseAnimations()) {
            double duration = getDuration(DEFAULT_DURATION);
            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            double start = node.getLayoutX() - node.getWidth();
            double end = node.getLayoutX();
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(node.opacityProperty(), 0, Interpolator.LINEAR),
                    new KeyValue(node.translateXProperty(), start, Interpolator.LINEAR)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(node.opacityProperty(), 1, Interpolator.EASE_OUT),
                    new KeyValue(node.translateXProperty(), end, Interpolator.EASE_OUT)
            ));

            timeline.setOnFinished(e -> onFinishedHandler.run());
            timeline.play();
        } else {
            onFinishedHandler.run();
        }
    }

    public static void slideOutBottom(Region node, Runnable onFinishedHandler) {
        if (displaySettings.isUseAnimations()) {
            double duration = getDuration(DEFAULT_DURATION / 2);
            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            double start = node.getLayoutY();
            double end = node.getHeight();
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(node.opacityProperty(), 1, Interpolator.LINEAR),
                    new KeyValue(node.translateYProperty(), start, Interpolator.LINEAR)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(node.opacityProperty(), 0, Interpolator.EASE_OUT),
                    new KeyValue(node.translateYProperty(), end, Interpolator.EASE_OUT)
            ));

            timeline.setOnFinished(e -> onFinishedHandler.run());
            timeline.play();
        } else {
            onFinishedHandler.run();
        }
    }

    public static void slideOutRight(Region node, Runnable onFinishedHandler) {
        if (displaySettings.isUseAnimations()) {
            double duration = getDuration(DEFAULT_DURATION / 2);
            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            double start = node.getLayoutX();
            double end = node.getWidth();
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(node.opacityProperty(), 1, Interpolator.LINEAR),
                    new KeyValue(node.translateXProperty(), start, Interpolator.LINEAR)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(node.opacityProperty(), 0, Interpolator.EASE_OUT),
                    new KeyValue(node.translateXProperty(), end, Interpolator.EASE_OUT)
            ));

            timeline.setOnFinished(e -> onFinishedHandler.run());
            timeline.play();
        } else {
            onFinishedHandler.run();
        }
    }

    public static void slideDownFromCenterTop(Region node, Runnable onFinishedHandler) {
        if (displaySettings.isUseAnimations()) {
            double duration = getDuration(DEFAULT_DURATION);
            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            double endY = -node.getHeight();
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(node.opacityProperty(), 1, DEFAULT_INTERPOLATOR),
                    new KeyValue(node.translateYProperty(), -10, DEFAULT_INTERPOLATOR)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(node.opacityProperty(), 0, DEFAULT_INTERPOLATOR),
                    new KeyValue(node.translateYProperty(), endY, DEFAULT_INTERPOLATOR)
            ));

            timeline.setOnFinished(e -> onFinishedHandler.run());
            timeline.play();
        } else {
            onFinishedHandler.run();
        }
    }

    public static void slideHorizontal(Region node, double targetWidth, double targetX) {
        if (displaySettings.isUseAnimations()) {
            double duration = getDuration(DEFAULT_DURATION / 2);
            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            double startWidth = node.getWidth();
            double startX = node.getLayoutX();
            log.error("startWidth {}",startWidth);
            log.error("startX {}",startX);
            log.error("targetWidth {}",targetWidth);
            log.error("targetX {}",targetX);
           // node.setLayoutX(startX);
           // node.setPrefWidth(startWidth);
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(node.layoutXProperty(), startX, Interpolator.LINEAR),
                    new KeyValue(node.prefWidthProperty(), startWidth, Interpolator.LINEAR)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(node.layoutXProperty(), targetX, Interpolator.EASE_OUT),
                    new KeyValue(node.prefWidthProperty(), targetWidth, Interpolator.EASE_OUT)
            ));

            timeline.play();
        } else {
            node.setLayoutX(targetX);
            node.setPrefWidth(targetWidth);
        }
    }
}
