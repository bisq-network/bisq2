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

package bisq.desktop.common;

import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.TransitionedView;
import bisq.desktop.common.view.View;
import bisq.settings.SettingsService;
import javafx.animation.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Camera;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class Transitions {

    public enum Type {
        BLACK(node -> darken(node, -1)),
        VERY_DARK(node -> darken(node, -0.85)),
        DARK(node -> darken(node, -0.7)),
        MEDIUM(node -> darken(node, -0.5)),
        LIGHT(node -> darken(node, -0.25)),

        VERY_DARK_BLUR_LIGHT(node -> blurLight(node, -0.9)),
        DARK_BLUR_LIGHT(node -> blurLight(node, -0.7)),
        MEDIUM_BLUR_LIGHT(node -> blurLight(node, -0.5)),
        LIGHT_BLUR_LIGHT(node -> blurLight(node, -0.25)),

        VERY_DARK_BLUR_STRONG(node -> blurStrong(node, -0.9)),
        DARK_BLUR_STRONG(node -> blurStrong(node, -0.7)),
        MEDIUM_BLUR_STRONG(node -> blurStrong(node, -0.5)),
        LIGHT_BLUR_STRONG(node -> blurStrong(node, -0.25));

        private final Consumer<Node> handler;

        Type(Consumer<Node> handler) {
            this.handler = handler;
        }

        public void apply(Node node) {
            this.handler.accept(node);
        }
    }

    public static final Type DEFAULT_TYPE = Type.VERY_DARK;

    public static final int DEFAULT_DURATION = 600;
    public static final int CROSS_FADE_IN_DURATION = 1500;
    public static final int CROSS_FADE_OUT_DURATION = 1000;
    private static final Interpolator DEFAULT_INTERPOLATOR = Interpolator.SPLINE(0.25, 0.1, 0.25, 1);
    @Setter
    private static SettingsService settingsService;
    private static Map<String, Timeline> removeEffectTimeLineByNodeId = new HashMap<>();
    private static Map<String, ChangeListener<Effect>> effectChangeListenerByNodeId = new HashMap<>();

    public static void fadeIn(Node node) {
        fadeIn(node, DEFAULT_DURATION);
    }

    public static void fadeIn(Node node, int duration) {
        fadeIn(node, duration, null);
    }

    public static void fadeIn(Node node, int duration, @Nullable Runnable finishedHandler) {
        if (node == null) {
            return;
        }
        FadeTransition fade = new FadeTransition(Duration.millis(getDuration(duration)), node);
        node.setOpacity(0);
        fade.setFromValue(0);
        fade.setToValue(1.0);
        fade.play();
        if (finishedHandler != null) {
            fade.setOnFinished(actionEvent -> finishedHandler.run());
        }
    }

    public static void fadeIn(Node node, int duration, double targetOpacity, @Nullable Runnable finishedHandler) {
        if (node == null) {
            return;
        }
        FadeTransition fade = new FadeTransition(Duration.millis(getDuration(duration)), node);
        node.setOpacity(0);
        fade.setFromValue(0);
        fade.setToValue(targetOpacity);
        fade.play();
        if (finishedHandler != null) {
            fade.setOnFinished(actionEvent -> finishedHandler.run());
        }
    }

    public static void fade(Node node, double fromValue, double toValue, int duration) {
        if (node == null) {
            return;
        }
        FadeTransition fade = new FadeTransition(Duration.millis(getDuration(duration)), node);
        node.setOpacity(fromValue);
        fade.setFromValue(fromValue);
        fade.setToValue(toValue);
        fade.play();
    }

    public static FadeTransition fadeOut(Node node) {
        return fadeOut(node, DEFAULT_DURATION, null);
    }

    public static FadeTransition fadeOut(Node node, int duration) {
        return fadeOut(node, duration, null);
    }

    public static FadeTransition fadeOut(Node node, int duration, @Nullable Runnable finishedHandler) {
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
        blur(node, DEFAULT_DURATION / 2, -0.1, false, 15);
    }

    public static void darken(Node node, double brightness) {
        blur(node, DEFAULT_DURATION / 2, brightness, false, 0);
    }

    public static void blurLight(Node node, double brightness) {
        blur(node, DEFAULT_DURATION / 2, brightness, false, 10);
    }

    public static void blurStrong(Node node, double brightness) {
        blur(node, DEFAULT_DURATION / 2, brightness, false, 20);
    }

    public static void blur(Node node, int duration, double brightness, boolean removeNode, double blurRadius) {
        // If there was a transition already playing we stop it and apply again our blur. 
        if (node.getEffect() != null) {
            String nodeId = node.getId();
            if (nodeId == null) {
                node.setId(StringUtils.createUid());
            }
            if (!effectChangeListenerByNodeId.containsKey(nodeId)) {
                ChangeListener<Effect> effectChangeListener = (observable, oldValue, newValue) -> {
                    if (oldValue != null && newValue == null) {
                        blur(node, duration, brightness, removeNode, blurRadius);
                        node.effectProperty().removeListener(effectChangeListenerByNodeId.get(nodeId));
                        effectChangeListenerByNodeId.remove(nodeId);
                    }
                };
                node.effectProperty().addListener(effectChangeListener);
                effectChangeListenerByNodeId.put(nodeId, effectChangeListener);
            } else {
                return;
            }
            if (removeEffectTimeLineByNodeId.containsKey(nodeId)) {
                removeEffectTimeLineByNodeId.get(nodeId).stop();
                removeEffectTimeLineByNodeId.remove(nodeId);
                node.setEffect(null);
            }
        }

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

    public static void blurDark(Node node) {
        blur(node, DEFAULT_DURATION, -0.75, false, 5);
    }

    public static void darken(Node node) {
        blur(node, DEFAULT_DURATION, -0.75, false, 0);
    }

    public static void darken(Node node, int duration, boolean removeNode) {
        blur(node, duration, -0.75, removeNode, 0);
    }

    public static void removeEffect(Node node) {
        removeEffect(node, DEFAULT_DURATION / 2);
    }

    private static void removeEffect(Node node, int duration) {
        if (node != null) {
            node.setMouseTransparent(false);
            Effect effect = node.getEffect();
            if (effect instanceof GaussianBlur) {
                if (node.getId() == null) {
                    node.setId(StringUtils.createUid());
                }
                Timeline timeline = new Timeline();
                removeEffectTimeLineByNodeId.put(node.getId(), timeline);
                GaussianBlur gaussianBlur = (GaussianBlur) effect;
                KeyValue kv1 = new KeyValue(gaussianBlur.radiusProperty(), 0.0);
                KeyFrame kf1 = new KeyFrame(Duration.millis(getDuration(duration)), kv1);
                timeline.getKeyFrames().add(kf1);

                ColorAdjust darken = (ColorAdjust) gaussianBlur.getInput();
                KeyValue kv2 = new KeyValue(darken.brightnessProperty(), 0.0);
                KeyFrame kf2 = new KeyFrame(Duration.millis(getDuration(duration)), kv2);
                timeline.getKeyFrames().add(kf2);
                timeline.setOnFinished(actionEvent -> {
                    node.setEffect(null);
                    removeEffectTimeLineByNodeId.remove(node.getId());
                    node.setId(null);
                });
                timeline.play();
            } else {
                node.setEffect(null);
            }
        }
    }

    public static int getDuration(int duration) {
        return getUseAnimations() ? duration : 1;
    }

    public static void crossFade(Node node1, Node node2) {
        fadeOut(node1, CROSS_FADE_OUT_DURATION)
                .setOnFinished(e -> Transitions.fadeIn(node2, CROSS_FADE_IN_DURATION));
    }


    public static void flapOut(Node node, Runnable onFinishedHandler) {
        if (getUseAnimations()) {
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
            node.setOpacity(0);
            onFinishedHandler.run();
        }
    }

    public static void transitRightOut(Region nodeIn, Region nodeOut) {
        nodeIn.setOpacity(0);
        UIScheduler.run(() -> slideInLeft(nodeIn, () -> {
        })).after(DEFAULT_DURATION / 4);
        slideOutRight(nodeOut, () -> {
            Parent parent = nodeOut.getParent();
            if (parent != null) {
                if (parent instanceof Pane) {
                    Pane pane = (Pane) parent;
                    pane.getChildren().remove(nodeOut);
                }
            }
        });
    }

    public static void transitLeftOut(Region nodeIn, Region nodeOut) {
        nodeIn.setOpacity(0);
        UIScheduler.run(() -> slideInRight(nodeIn, () -> {
        })).after(DEFAULT_DURATION / 4);
        slideOutLeft(nodeOut, () -> {
            Parent parent = nodeOut.getParent();
            if (parent != null) {
                if (parent instanceof Pane) {
                    Pane pane = (Pane) parent;
                    pane.getChildren().remove(nodeOut);
                }
            }
        });
    }

    public static void transitInNewTab(Region nodeIn) {
        nodeIn.setOpacity(0);
        fadeIn(nodeIn, DEFAULT_DURATION / 2);
    }

    public static void transitContentViews(View<? extends Parent, ? extends Model, ? extends Controller> oldView,
                                           View<? extends Parent, ? extends Model, ? extends Controller> newView) {
        Region nodeIn = newView.getRoot();
        if (oldView != null) {
            Region nodeOut = oldView.getRoot();
            nodeIn.setOpacity(0);

            UIScheduler.run(() -> {
                        if (newView instanceof TransitionedView) {
                            ((TransitionedView) newView).onStartTransition();
                        }
                        fadeIn(nodeIn,
                                DEFAULT_DURATION / 2,
                                () -> {
                                    if (newView instanceof TransitionedView) {
                                        ((TransitionedView) newView).onTransitionCompleted();
                                    }
                                });
                    })
                    .after(DEFAULT_DURATION / 2);
            slideOutRight(nodeOut, () -> {
                Parent parent = nodeOut.getParent();
                if (parent != null) {
                    if (parent instanceof Pane) {
                        Pane pane = (Pane) parent;
                        pane.getChildren().remove(nodeOut);
                    }
                }
            });
        } else {
            if (newView instanceof TransitionedView) {
                ((TransitionedView) newView).onStartTransition();
            }
            Transitions.fadeIn(nodeIn,
                    DEFAULT_DURATION,
                    () -> {
                        if (newView instanceof TransitionedView) {
                            ((TransitionedView) newView).onTransitionCompleted();
                        }
                    });
        }
    }

    public static void slideInTop(Region node, int duration) {
        slideInTop(node, duration, () -> {
        });
    }

    public static void slideInTop(Region node, int duration, Runnable onFinishedHandler) {
        double end = 0;
        if (getUseAnimations()) {
            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            double start = -node.getHeight();
            node.setTranslateY(start);
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(node.translateYProperty(), start, Interpolator.LINEAR)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(node.translateYProperty(), end, Interpolator.EASE_BOTH)
            ));

            timeline.setOnFinished(e -> onFinishedHandler.run());
            timeline.play();
        } else {
            node.setTranslateY(end);
            onFinishedHandler.run();
        }
    }

    public static void slideInLeft(Region node, Runnable onFinishedHandler) {
        slideInHorizontal(node, onFinishedHandler, true);
    }

    public static void slideInRight(Region node, Runnable onFinishedHandler) {
        slideInHorizontal(node, onFinishedHandler, false);
    }

    public static void slideInHorizontal(Region node, Runnable onFinishedHandler, boolean slideLeft) {
        double end = node.getLayoutX();
        if (getUseAnimations()) {
            double duration = getDuration(DEFAULT_DURATION);
            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            double start = slideLeft ?
                    node.getLayoutX() - node.getWidth() :
                    node.getLayoutX() + node.getWidth();

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
            node.setTranslateX(end);
            node.setOpacity(1);
            onFinishedHandler.run();
        }
    }

    public static void slideOutBottom(Region node, Runnable onFinishedHandler) {
        double end = node.getHeight();
        if (getUseAnimations()) {
            double duration = getDuration(DEFAULT_DURATION / 2);
            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            node.setTranslateY(0);
            double start = node.getLayoutY();
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
            node.setTranslateY(end);
            node.setOpacity(0);
            onFinishedHandler.run();
        }
    }

    public static void slideOutRight(Region node, Runnable onFinishedHandler) {
        slideOutHorizontal(node, onFinishedHandler, true);
    }

    public static void slideOutLeft(Region node, Runnable onFinishedHandler) {
        slideOutHorizontal(node, onFinishedHandler, false);
    }

    public static void slideOutHorizontal(Region node, Runnable onFinishedHandler, boolean slideOutRight) {
        double start = node.getTranslateX();
        double end = slideOutRight ?
                node.getWidth() :
                -node.getWidth();
        if (getUseAnimations()) {
            double duration = getDuration(DEFAULT_DURATION / 2);
            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();


            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(node.opacityProperty(), 1, Interpolator.LINEAR),
                    new KeyValue(node.translateXProperty(), start, Interpolator.LINEAR)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(node.opacityProperty(), 0, Interpolator.EASE_OUT),
                    new KeyValue(node.translateXProperty(), end, Interpolator.EASE_OUT)
            ));
            timeline.setOnFinished(e -> {
                onFinishedHandler.run();
                node.setTranslateX(start);
            });
            timeline.play();
        } else {
            node.setTranslateX(start);
            node.setOpacity(0);
            onFinishedHandler.run();
        }
    }

    public static void slideDownFromCenterTop(Region node, Runnable onFinishedHandler) {
        double end = -node.getHeight();
        if (getUseAnimations()) {
            double duration = getDuration(DEFAULT_DURATION);
            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            node.setTranslateY(0);
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(node.opacityProperty(), 1, DEFAULT_INTERPOLATOR),
                    new KeyValue(node.translateYProperty(), -10, DEFAULT_INTERPOLATOR)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(node.opacityProperty(), 0, DEFAULT_INTERPOLATOR),
                    new KeyValue(node.translateYProperty(), end, DEFAULT_INTERPOLATOR)
            ));

            timeline.setOnFinished(e -> onFinishedHandler.run());
            timeline.play();
        } else {
            node.setTranslateY(end);
            node.setOpacity(0);
            onFinishedHandler.run();
        }
    }

    public static void moveLeft(Region node, int targetX, int duration) {
        if (getUseAnimations()) {
            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            double startX = node.getLayoutX();
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(node.layoutXProperty(), startX, Interpolator.LINEAR)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(node.layoutXProperty(), targetX, Interpolator.EASE_OUT)
            ));
            timeline.play();
        } else {
            node.setLayoutX(targetX);
        }
    }

    public static void animateLeftNavigationWidth(Region node, double targetWidth, int duration) {
        if (getUseAnimations()) {
            double startWidth = node.getWidth();
            Interpolator interpolator = startWidth > targetWidth ? Interpolator.EASE_IN : Interpolator.EASE_OUT;
            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(node.prefWidthProperty(), startWidth, Interpolator.LINEAR)
            ));

            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(node.prefWidthProperty(), targetWidth, interpolator)
            ));
            timeline.play();
        } else {
            node.setPrefWidth(targetWidth);
        }
    }

    public static void animateLeftSubNavigation(Region node, double targetX, int duration) {
        if (getUseAnimations()) {
            double startX = node.getLayoutX();
            Interpolator interpolator = startX > targetX ? Interpolator.EASE_IN : Interpolator.EASE_OUT;
            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(node.layoutXProperty(), startX, Interpolator.LINEAR)
            ));

            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(node.layoutXProperty(), targetX, interpolator)
            ));
            timeline.play();
        } else {
            node.setLayoutX(targetX);
        }
    }

    public static void animateNavigationButtonMarks(Region node, double targetHeight, double targetY) {
        double startY = node.getLayoutY();
        if (getUseAnimations() || startY == 0) {
            double duration = getDuration(DEFAULT_DURATION / 4);
            double startHeight = node.getHeight();
            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(node.layoutYProperty(), startY, Interpolator.LINEAR),
                    new KeyValue(node.prefHeightProperty(), startHeight, Interpolator.LINEAR)
            ));
            if (startY > targetY) {
                // animate upwards
                keyFrames.add(new KeyFrame(Duration.millis(duration / 2),
                        new KeyValue(node.layoutYProperty(), targetY, Interpolator.EASE_OUT),
                        new KeyValue(node.prefHeightProperty(), startY - targetY + startHeight, Interpolator.EASE_OUT)
                ));
                keyFrames.add(new KeyFrame(Duration.millis(duration),
                        new KeyValue(node.prefHeightProperty(), targetHeight, Interpolator.EASE_OUT)
                ));
            } else {
                // animate downwards
                keyFrames.add(new KeyFrame(Duration.millis(duration / 2),
                        new KeyValue(node.layoutYProperty(), startY, Interpolator.LINEAR),
                        new KeyValue(node.prefHeightProperty(), targetY - startY + targetHeight, Interpolator.EASE_OUT)
                ));
                keyFrames.add(new KeyFrame(Duration.millis(duration),
                        new KeyValue(node.layoutYProperty(), targetY, Interpolator.EASE_OUT),
                        new KeyValue(node.prefHeightProperty(), targetHeight, Interpolator.EASE_OUT)
                ));
            }
            timeline.play();
        } else {
            node.setLayoutY(targetY);
            node.setPrefHeight(targetHeight);
        }
    }

    public static void animateTabButtonMarks(Region node, double targetWidth, double targetX) {
        if (getUseAnimations()) {
            double startWidth = node.getWidth();
            double startX = node.getLayoutX();
            double duration = getDuration(DEFAULT_DURATION / 4);
            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(node.layoutXProperty(), startX, Interpolator.LINEAR),
                    new KeyValue(node.prefWidthProperty(), startWidth, Interpolator.LINEAR)
            ));
            if (startX == 0) {
                // start from left and x,w = 0
                keyFrames.add(new KeyFrame(Duration.millis(duration),
                        new KeyValue(node.layoutXProperty(), targetX, Interpolator.EASE_OUT),
                        new KeyValue(node.prefWidthProperty(), targetWidth, Interpolator.EASE_OUT)
                ));
            } else if (startX > targetX) {
                // animate to left
                keyFrames.add(new KeyFrame(Duration.millis(duration / 2),
                        new KeyValue(node.layoutXProperty(), targetX, Interpolator.EASE_OUT),
                        new KeyValue(node.prefWidthProperty(), startX - targetX + startWidth, Interpolator.EASE_OUT)
                ));
                keyFrames.add(new KeyFrame(Duration.millis(duration),
                        new KeyValue(node.prefWidthProperty(), targetWidth, Interpolator.EASE_OUT)
                ));
            } else {
                // animate to right
                keyFrames.add(new KeyFrame(Duration.millis(duration / 2),
                        new KeyValue(node.layoutXProperty(), startX, Interpolator.LINEAR),
                        new KeyValue(node.prefWidthProperty(), targetX - startX + targetWidth, Interpolator.EASE_OUT)
                ));
                keyFrames.add(new KeyFrame(Duration.millis(duration),
                        new KeyValue(node.layoutXProperty(), targetX, Interpolator.EASE_OUT),
                        new KeyValue(node.prefWidthProperty(), targetWidth, Interpolator.EASE_OUT)
                ));
            }
            timeline.play();
        } else {
            node.setLayoutX(targetX);
            node.setPrefWidth(targetWidth);
        }
    }

    public static void animateHeight(Region node, double targetHeight) {
        animateHeight(node, node.getPrefHeight(), targetHeight, getDuration(DEFAULT_DURATION / 2), null);
    }

    public static void animateHeight(Region node, double startHeight, double targetHeight, double duration, @Nullable Runnable onFinishedHandler) {
        if (getUseAnimations()) {
            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(node.prefHeightProperty(), startHeight, Interpolator.LINEAR)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(node.prefHeightProperty(), targetHeight, Interpolator.EASE_OUT)
            ));
            timeline.play();
            if (onFinishedHandler != null) {
                timeline.setOnFinished(e -> onFinishedHandler.run());
            }
        } else {
            node.setPrefHeight(targetHeight);
            if (onFinishedHandler != null) {
                onFinishedHandler.run();
            }
        }
    }

    public static void animateWidth(Region node, double targetWidth) {
        animateWidth(node, targetWidth, getDuration(DEFAULT_DURATION / 2), null);
    }

    public static void animateWidth(Region node, double targetWidth, double duration, @Nullable Runnable finishedHandler) {
        if (getUseAnimations()) {
            double startWidth = node.getPrefWidth();
            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(node.prefWidthProperty(), startWidth, Interpolator.LINEAR)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(node.prefWidthProperty(), targetWidth, Interpolator.EASE_OUT)
            ));
            timeline.play();
            if (finishedHandler != null) {
                timeline.setOnFinished(actionEvent -> finishedHandler.run());
            }
        } else {
            node.setPrefWidth(targetWidth);
        }
    }

    public static void animateLayoutY(Region node, double targetY) {
        animateLayoutY(node, targetY, getDuration(DEFAULT_DURATION / 2), null);
    }

    public static void animateLayoutY(Region node, double targetY, double duration, @Nullable Runnable finishedHandler) {
        if (getUseAnimations()) {
            double startY = node.getLayoutY();
            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(node.layoutYProperty(), startY, Interpolator.LINEAR)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(node.layoutYProperty(), targetY, Interpolator.EASE_OUT)
            ));
            timeline.play();
            if (finishedHandler != null) {
                timeline.setOnFinished(actionEvent -> finishedHandler.run());
            }
        } else {
            node.setLayoutY(targetY);
        }
    }

    public static Timeline pulse(Node node, double duration, double delay,
                                 double fromOpacity,
                                 double midOpacity,
                                 double toOpacity,
                                 double fromScale, double toScale) {
        Timeline timeline = new Timeline();
        if (getUseAnimations()) {
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(node.opacityProperty(), fromOpacity, Interpolator.LINEAR),
                    new KeyValue(node.scaleXProperty(), fromScale, Interpolator.LINEAR),
                    new KeyValue(node.scaleYProperty(), fromScale, Interpolator.LINEAR)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration / 2),
                    new KeyValue(node.opacityProperty(), midOpacity, Interpolator.EASE_OUT)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(node.opacityProperty(), toOpacity, Interpolator.EASE_OUT),
                    new KeyValue(node.scaleXProperty(), toScale, Interpolator.EASE_OUT),
                    new KeyValue(node.scaleYProperty(), toScale, Interpolator.EASE_OUT)
            ));
            timeline.setDelay(Duration.millis(delay));
            timeline.play();
        }
        return timeline;
    }

    private static boolean getUseAnimations() {
        return settingsService.getUseAnimations().get();
    }
}
