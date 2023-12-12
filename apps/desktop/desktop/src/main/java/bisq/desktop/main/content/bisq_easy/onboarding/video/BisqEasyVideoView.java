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

package bisq.desktop.main.content.bisq_easy.onboarding.video;

import bisq.common.data.Pair;
import bisq.common.util.ExceptionUtil;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.FillStageView;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.presentation.formatters.TimeFormatter;
import javafx.animation.*;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.stage.Window;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;
import org.fxmisc.easybind.monadic.MonadicBinding;

/**
 * This view is not following strictly the MVC patters (doing too much in the View). Reason is that lot of the relevant data
 * comes from the MediaPlayer and animations. But could be considered to clean that all up...
 */

@Slf4j
public class BisqEasyVideoView extends View<StackPane, BisqEasyVideoModel, BisqEasyVideoController> implements FillStageView {
    public static final double VIDEO_WIDTH = 1289;
    public static final double VIDEO_HEIGHT = 875;
    public static final double PADDING = 50;
    public static final double SLIDER_WIDTH = 59.5;

    private final MediaView mediaView;
    private MediaPlayer mediaPlayer;
    private final Label timeInfo;
    private final Slider positionSlider, volumeSlider;
    private final Pane volumePane;
    private final ImageView startImage, endImage, soundImage, soundOffImage, largePause, largePlay, largeReplay;
    private final HBox volumeHBox;
    private final ProgressBar positionProgressBar;
    private final StackPane progressPane;
    private final VBox controlsVBox, controlsWrapperVBox;
    private final Button playButton, pauseButton, soundButton, closeButton;
    private Subscription windowSizePin, currentTimePin, volumePin, statusPin;
    private Timeline showControlsTimeline, hideControlsTimeline, pausePlayTimeline;
    private long lastActivity;
    private UIScheduler checkActivityScheduler;

    public BisqEasyVideoView(BisqEasyVideoModel model, BisqEasyVideoController controller) {
        super(new StackPane(), model, controller);

        root.getStyleClass().add("video-bg");
        mediaView = new MediaView();

        closeButton = new Button(Res.get("action.close"));
        closeButton.getStyleClass().add("grey-transparent-outlined-button");

        positionProgressBar = new ProgressBar(0);
        positionProgressBar.getStyleClass().add("video-progress-bar");

        positionSlider = new Slider();
        positionSlider.setMin(0);
        positionSlider.setMax(1);
        positionSlider.getStyleClass().add("video-position-slider");

        StackPane.setMargin(positionProgressBar, new Insets(0, 6, 0, 6));
        progressPane = new StackPane(positionProgressBar, positionSlider);
        progressPane.setPadding(new Insets(0, 10, 0, 10));

        playButton = BisqIconButton.createIconButton("play");
        pauseButton = BisqIconButton.createIconButton("pause");

        soundImage = ImageUtil.getImageViewById("sound");
        soundOffImage = ImageUtil.getImageViewById("sound-off");
        soundButton = BisqIconButton.createIconButton(soundImage);

        volumeSlider = new Slider();
        volumeSlider.getStyleClass().add("video-volume-slider");
        volumeSlider.setPrefWidth(SLIDER_WIDTH);
        volumeSlider.setMin(0);
        volumeSlider.setMax(1);
        volumeSlider.setValue(1);
        volumeSlider.setTranslateX(-SLIDER_WIDTH);
        volumeSlider.setPadding(new Insets(0.5, 1, 0, 1));

        volumePane = new Pane(volumeSlider);
        volumePane.setPrefWidth(0);
        Rectangle clip = new Rectangle(SLIDER_WIDTH, 30);
        volumePane.setClip(clip);

        HBox.setMargin(volumePane, new Insets(3, -15, 0, 9));
        volumeHBox = new HBox(soundButton, volumePane);

        timeInfo = new Label();
        timeInfo.getStyleClass().add("video-time-info");

        HBox.setMargin(volumeHBox, new Insets(5.5, -5.5, 0, 0));
        HBox.setMargin(timeInfo, new Insets(-1, 0, 0, 0));
        HBox controlsHBox = new HBox(30, playButton, pauseButton, volumeHBox, timeInfo, Spacer.fillHBox(), closeButton);
        controlsHBox.setAlignment(Pos.CENTER);
        controlsHBox.setPadding(new Insets(15, 30, 15, 30));

        VBox.setMargin(progressPane, new Insets(10, 0, 0, 0));
        controlsVBox = new VBox(progressPane, controlsHBox);
        controlsVBox.getStyleClass().add("video-control-bg");

        startImage = ImageUtil.getImageViewById("bisq-easy-video-start-image");
        endImage = ImageUtil.getImageViewById("bisq-easy-video-end-image");
        endImage.setManaged(false);
        endImage.setVisible(false);
        largePause = ImageUtil.getImageViewById("large-pause");
        largePause.setOpacity(0);
        largePlay = ImageUtil.getImageViewById("large-play");
        largePlay.setOpacity(0);
        largeReplay = ImageUtil.getImageViewById("large-replay");
        largeReplay.setOpacity(0);

        StackPane.setAlignment(largePause, Pos.CENTER);
        StackPane.setAlignment(largePlay, Pos.CENTER);
        StackPane.setAlignment(largeReplay, Pos.CENTER);
        controlsWrapperVBox = new VBox(Spacer.fillVBox(), controlsVBox);
        root.getChildren().addAll(mediaView, startImage, endImage, largePause, largePlay, largeReplay, controlsWrapperVBox);
    }

    @Override
    protected void onViewAttached() {
        Window window = OverlayController.getInstance().getApplicationRoot().getScene().getWindow();
        MonadicBinding<Pair<Number, Number>> binding = EasyBind.combine(window.widthProperty(),
                window.heightProperty(), Pair::new);
        windowSizePin = EasyBind.subscribe(binding, this::resize);

        try {
            Media media = new Media(getClass().getClassLoader().getResource("video.mp4").toExternalForm());
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);

            mediaPlayer.setOnEndOfMedia(() -> {
                largeReplay.setScaleX(0.5);
                largeReplay.setScaleY(0.5);
                Transitions.fadeIn(largeReplay, Transitions.DEFAULT_DURATION / 2);
                endImage.setManaged(true);
                endImage.setVisible(true);
                Transitions.fadeIn(endImage, Transitions.DEFAULT_DURATION / 2);
                mediaPlayer.stop();

                controller.onCompleted();
            });
            root.setOnMouseClicked(e -> {
                onActivity();
                if (e.getY() < root.getHeight() - controlsVBox.getHeight()) {
                    if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                        pauseWithAnimation();
                    } else {
                        playWithAnimation();
                    }
                }
            });

            largeReplay.setOnMouseClicked(e -> replayWithAnimation());

            mediaPlayer.volumeProperty().bindBidirectional(volumeSlider.valueProperty());
            playButton.visibleProperty().bind(mediaPlayer.statusProperty().isEqualTo(MediaPlayer.Status.PLAYING).not());
            playButton.managedProperty().bind(playButton.visibleProperty());
            pauseButton.visibleProperty().bind(playButton.visibleProperty().not());
            pauseButton.managedProperty().bind(playButton.visibleProperty().not());
            volumeSlider.managedProperty().bind(volumeSlider.visibleProperty());

            volumeHBox.setOnMouseEntered(e -> showVolumeSlider());
            volumeHBox.setOnMouseExited(e -> hideVolumeSlider());
            soundButton.setOnMouseClicked(e -> toggleVolume());

            root.setOnMouseEntered(e -> maybeShowControls());
            root.setOnMouseExited(e -> maybeHideControls());
            root.setOnMouseMoved(e -> onActivity());

            playButton.setOnAction(e -> {
                play();
                largePlay.setOpacity(0);
            });
            pauseButton.setOnAction(e -> pause());

            positionProgressBar.progressProperty().bind(positionSlider.valueProperty());

            positionSlider.setOnMousePressed(e -> {
                if (!model.isMediaPlayerPausedBySeek() && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    pause();
                    model.setMediaPlayerPausedBySeek(true);
                }
                mediaPlayer.seek(Duration.millis(positionSlider.getValue() * getTotalDuration()));
                largePause.setOpacity(0);
                largePlay.setOpacity(0);
                largeReplay.setOpacity(0);
                startImage.setOpacity(0);
                startImage.setManaged(false);
                startImage.setVisible(false);
                endImage.setOpacity(0);
                endImage.setManaged(false);
                endImage.setVisible(false);
            });
            positionSlider.setOnMouseReleased(e -> {
                if (model.isMediaPlayerPausedBySeek()) {
                    play();
                    largePlay.setOpacity(0);
                    model.setMediaPlayerPausedBySeek(false);
                }
            });
            positionSlider.setOnMouseDragged(e ->
                    mediaPlayer.seek(Duration.millis(positionSlider.getValue() * getTotalDuration())));

            currentTimePin = EasyBind.subscribe(mediaPlayer.currentTimeProperty(), time -> {
                double totalDuration = getTotalDuration();
                double currentTime = time.toMillis();
                if (currentTime > 0) {
                    double progress = currentTime / totalDuration;
                    positionSlider.setValue(progress);
                }
                String current = TimeFormatter.formatVideoDuration(Math.round(currentTime));
                String total = TimeFormatter.formatVideoDuration(Math.round(totalDuration));
                timeInfo.setText(current + " / " + total);
            });
            volumePin = EasyBind.subscribe(volumeSlider.valueProperty(), volume -> {
                if (volume.doubleValue() == 0) {
                    soundButton.setGraphic(soundOffImage);
                } else {
                    soundButton.setGraphic(soundImage);
                }
            });

            closeButton.setOnAction(e -> controller.onClose());

            controlsVBox.setOpacity(1);

            if (model.getLastPositionBeforeClose() == 0) {
                largePlay.setOpacity(1);
                largePlay.setScaleX(0.5);
                largePlay.setScaleY(0.5);
                largePlay.setCursor(Cursor.HAND);

                startImage.setOpacity(1);
                startImage.setManaged(true);
                startImage.setVisible(true);
            } else {
                largePlay.setOpacity(0);
            }

            mediaView.setOpacity(0);
            statusPin = EasyBind.subscribe(mediaPlayer.statusProperty(), status -> {
                if (status == MediaPlayer.Status.READY) {
                    if (model.getLastPositionBeforeClose() != 0) {
                        mediaPlayer.seek(Duration.millis(model.getLastPositionBeforeClose()));
                    }
                    UIScheduler.run(() -> {
                        Transitions.fadeIn(mediaView, Transitions.DEFAULT_DURATION / 2);
                        statusPin.unsubscribe();
                        statusPin = null;
                    }).after(500);
                }
            });
        } catch (Exception e) {
            // If OS does not support mp4 we get an exception
            log.error(ExceptionUtil.print(e));
            new Popup().warning(Res.get("video.mp4NotSupported.warning", ExceptionUtil.print(e))).show();
        }
    }

    @Override
    protected void onViewDetached() {
        model.setMediaPlayerPausedBySeek(false);
        model.setLastPositionBeforeClose(mediaPlayer.getCurrentTime().toMillis());

        playButton.visibleProperty().unbind();
        playButton.managedProperty().unbind();
        pauseButton.visibleProperty().unbind();
        pauseButton.managedProperty().unbind();
        volumeSlider.managedProperty().unbind();
        positionProgressBar.progressProperty().unbind();

        positionSlider.setOnMousePressed(null);
        positionSlider.setOnMouseReleased(null);
        positionSlider.setOnMouseDragged(null);
        volumeHBox.setOnMouseEntered(null);
        volumeHBox.setOnMouseExited(null);
        soundButton.setOnMouseClicked(null);
        playButton.setOnAction(null);
        pauseButton.setOnAction(null);
        root.setOnMouseClicked(null);
        root.setOnMouseClicked(null);
        closeButton.setOnAction(null);

        windowSizePin.unsubscribe();

        if (mediaPlayer != null) {
            mediaPlayer.volumeProperty().unbindBidirectional(volumeSlider.valueProperty());
            mediaPlayer.setOnEndOfMedia(null);
            mediaPlayer.dispose();
            mediaPlayer = null;

            currentTimePin.unsubscribe();
            volumePin.unsubscribe();

            if (statusPin != null) {
                statusPin.unsubscribe();
            }
        }

        if (checkActivityScheduler != null) {
            checkActivityScheduler.stop();
        }
    }

    private void resize(Pair<Number, Number> appWindowSize) {
        double appWindowWidth = appWindowSize.getFirst().doubleValue();
        double appWindowHeight = appWindowSize.getSecond().doubleValue();
        double ration = VIDEO_WIDTH / VIDEO_HEIGHT;
        double width = appWindowWidth - 2 * PADDING;
        double height = width / ration;
        if (height > appWindowHeight) {
            height = appWindowHeight - 2 * PADDING;
            width = height * ration;
            mediaView.setFitHeight(height);
            mediaView.setFitWidth(0);
        } else {
            mediaView.setFitHeight(0);
            mediaView.setFitWidth(width);
        }
        root.setPrefWidth(width);
        root.setPrefHeight(height);
        startImage.setFitHeight(height);
        startImage.setFitWidth(width);
        endImage.setFitHeight(height);
        endImage.setFitWidth(width);
        positionProgressBar.setPrefWidth(width);
        positionSlider.setPrefWidth(width);
    }

    private void pauseWithAnimation() {
        pause();
        animatePlayOrPause(largePause);
    }

    private void playWithAnimation() {
        play();
        animatePlayOrPause(largePlay);
    }

    private void replayWithAnimation() {
        replay();
        animatePlayOrPause(largeReplay);
    }

    private void animatePlayOrPause(ImageView imageView) {
        if (pausePlayTimeline != null) {
            pausePlayTimeline.stop();
            largePause.setOpacity(0);
            largePlay.setOpacity(0);
            largeReplay.setOpacity(0);
        }
        pausePlayTimeline = new Timeline();
        if (Transitions.getUseAnimations()) {
            ObservableList<KeyFrame> keyFrames = pausePlayTimeline.getKeyFrames();
            if (imageView.getOpacity() == 0) {
                keyFrames.add(new KeyFrame(Duration.millis(0),
                        new KeyValue(imageView.opacityProperty(), 0, Interpolator.LINEAR),
                        new KeyValue(imageView.scaleXProperty(), 0.25, Interpolator.LINEAR),
                        new KeyValue(imageView.scaleYProperty(), 0.25, Interpolator.LINEAR)
                ));
            }
            keyFrames.add(new KeyFrame(Duration.millis(Transitions.DEFAULT_DURATION / 2d),
                    new KeyValue(imageView.opacityProperty(), 1, Interpolator.EASE_BOTH),
                    new KeyValue(imageView.scaleXProperty(), 0.5, Interpolator.LINEAR),
                    new KeyValue(imageView.scaleYProperty(), 0.5, Interpolator.LINEAR)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(Transitions.DEFAULT_DURATION),
                    new KeyValue(imageView.opacityProperty(), 0, Interpolator.EASE_BOTH),
                    new KeyValue(imageView.scaleXProperty(), 1, Interpolator.LINEAR),
                    new KeyValue(imageView.scaleYProperty(), 1, Interpolator.LINEAR)
            ));
            pausePlayTimeline.play();
        }
    }

    private void pause() {
        mediaPlayer.pause();
        if (checkActivityScheduler != null) {
            checkActivityScheduler.stop();
        }
    }

    private void play() {
        if (model.getLastPositionBeforeClose() != 0) {
            mediaPlayer.seek(Duration.millis(model.getLastPositionBeforeClose()));
            model.setLastPositionBeforeClose(0);
            largePlay.setOpacity(0);
        } else {
            Transitions.fadeOut(startImage, Transitions.DEFAULT_DURATION / 2, () -> {
                startImage.setManaged(false);
                startImage.setVisible(false);
            });
        }

        largeReplay.setOpacity(0);
        endImage.setOpacity(0);
        endImage.setManaged(false);
        endImage.setVisible(false);

        mediaPlayer.play();
        restartCheckActivityScheduler();
    }

    private void replay() {
        mediaPlayer.stop();
        play();
        largePlay.setOpacity(0);
    }

    private void toggleVolume() {
        volumeSlider.setValue(volumeSlider.getValue() == 0 ? 1 : 0);
    }

    private void maybeShowControls() {
        if (hideControlsTimeline != null && hideControlsTimeline.getStatus() == Animation.Status.RUNNING) {
            hideControlsTimeline.stop();
            controlsVBox.setOpacity(0);
        }
        if (controlsVBox.getOpacity() == 0) {
            restartCheckActivityScheduler();
            if (Transitions.getUseAnimations()) {
                showControlsTimeline = new Timeline();
                ObservableList<KeyFrame> keyFrames = showControlsTimeline.getKeyFrames();
                keyFrames.add(new KeyFrame(Duration.millis(0),
                        new KeyValue(controlsVBox.opacityProperty(), 0, Interpolator.LINEAR)
                ));
                keyFrames.add(new KeyFrame(Duration.millis(Transitions.DEFAULT_DURATION / 2d),
                        new KeyValue(controlsVBox.opacityProperty(), 1, Interpolator.EASE_BOTH)
                ));
                showControlsTimeline.play();
            } else {
                controlsVBox.setOpacity(1);
            }
        }
    }

    private void maybeHideControls() {
        if (showControlsTimeline != null && showControlsTimeline.getStatus() == Animation.Status.RUNNING) {
            showControlsTimeline.stop();
            controlsVBox.setOpacity(1);
        }
        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING && controlsVBox.getOpacity() > 0) {
            if (Transitions.getUseAnimations()) {
                hideControlsTimeline = new Timeline();
                ObservableList<KeyFrame> keyFrames = hideControlsTimeline.getKeyFrames();
                keyFrames.add(new KeyFrame(Duration.millis(0),
                        new KeyValue(controlsVBox.opacityProperty(), 1, Interpolator.LINEAR)
                ));
                keyFrames.add(new KeyFrame(Duration.millis(Transitions.DEFAULT_DURATION),
                        new KeyValue(controlsVBox.opacityProperty(), 0, Interpolator.EASE_BOTH)
                ));
                hideControlsTimeline.play();
            } else {
                controlsVBox.setOpacity(0);
            }
        }
    }

    private void onActivity() {
        lastActivity = System.currentTimeMillis();
        maybeShowControls();
    }

    private void restartCheckActivityScheduler() {
        if (checkActivityScheduler != null) {
            checkActivityScheduler.stop();
        }
        checkActivityScheduler = UIScheduler.run(() -> {
            // If no mouse movement has happened for 2 seconds we hide the control pane
            if (System.currentTimeMillis() - lastActivity > 2000 && controlsVBox.getOpacity() > 0) {
                maybeHideControls();
            }
        }).periodically(1000);
    }

    private void showVolumeSlider() {
        if (Transitions.getUseAnimations()) {
            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(volumeSlider.translateXProperty(), -SLIDER_WIDTH, Interpolator.LINEAR),
                    new KeyValue(volumePane.prefWidthProperty(), 0, Interpolator.LINEAR)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(Transitions.DEFAULT_DURATION / 2d),
                    new KeyValue(volumeSlider.translateXProperty(), 0, Interpolator.EASE_BOTH),
                    new KeyValue(volumePane.prefWidthProperty(), SLIDER_WIDTH, Interpolator.EASE_BOTH)
            ));
            timeline.play();
        } else {
            volumeSlider.setTranslateX(0);
        }
    }

    private void hideVolumeSlider() {
        if (Transitions.getUseAnimations()) {
            Timeline timeline = new Timeline();
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(volumeSlider.translateXProperty(), 0, Interpolator.LINEAR),
                    new KeyValue(volumePane.prefWidthProperty(), SLIDER_WIDTH, Interpolator.LINEAR)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(Transitions.DEFAULT_DURATION / 2d),
                    new KeyValue(volumeSlider.translateXProperty(), -SLIDER_WIDTH, Interpolator.EASE_BOTH),
                    new KeyValue(volumePane.prefWidthProperty(), 0, Interpolator.EASE_BOTH)
            ));
            timeline.play();
        } else {
            volumeSlider.setTranslateX(-SLIDER_WIDTH);
        }
    }

    private double getTotalDuration() {
        return mediaPlayer.getTotalDuration().toMillis();
    }
}