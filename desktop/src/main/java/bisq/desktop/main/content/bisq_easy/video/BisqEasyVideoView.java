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

package bisq.desktop.main.content.bisq_easy.video;

import bisq.common.util.ExceptionUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class BisqEasyVideoView extends View<VBox, BisqEasyVideoModel, BisqEasyVideoController> {
    public static final double VIDEO_WIDTH = 1920;
    public static final double VIDEO_HEIGHT = 1080;
    public static final double MENU_HEIGHT = 51;
    public static final double PADDING = 50;

    private final MediaView mediaView;
    private final Label errorLabel;
    private MediaPlayer mediaPlayer;
    private final Button playButton, pauseButton, stopButton, closeButton;

    public BisqEasyVideoView(BisqEasyVideoModel model, BisqEasyVideoController controller) {
        super(new VBox(), model, controller);

        mediaView = new MediaView();

        playButton = BisqIconButton.createIconButton(AwesomeIcon.PLAY);
        pauseButton = BisqIconButton.createIconButton(AwesomeIcon.PAUSE);
        stopButton = BisqIconButton.createIconButton(AwesomeIcon.STOP);
        closeButton = new Button(Res.get("action.close"));
        closeButton.setDefaultButton(true);
        closeButton.setMinWidth(100);

        HBox.setMargin(closeButton, new Insets(0, 10, 0, -110));
        HBox buttonBox = new HBox(20, Spacer.fillHBox(), playButton, pauseButton, stopButton, Spacer.fillHBox(), closeButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        errorLabel = new Label();

        root.getChildren().addAll(mediaView, buttonBox, errorLabel);
    }

    @Override
    protected void onViewAttached() {
        double stageWidth = OverlayController.getInstance().getApplicationRoot().getWidth();
        double stageHeight = OverlayController.getInstance().getApplicationRoot().getHeight() - MENU_HEIGHT;
        double ration = VIDEO_WIDTH / VIDEO_HEIGHT;
        double videoWidth = stageWidth - 2 * PADDING;
        double videoHeight = videoWidth / ration;
        double width = videoWidth;
        double height = videoHeight + MENU_HEIGHT;
        if (videoHeight > stageHeight) {
            videoHeight = stageHeight - 2 * PADDING;
            videoWidth = videoHeight * ration;
            width = videoWidth;
            height = videoHeight + MENU_HEIGHT;
            mediaView.setFitHeight(videoHeight);
            mediaView.setFitWidth(0);
        } else {
            mediaView.setFitHeight(0);
            mediaView.setFitWidth(width);
        }

        root.setPrefWidth(width);
        root.setPrefHeight(height);

        try {
            Media media = new Media(getClass().getClassLoader().getResource("video.mp4").toExternalForm());
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.setOnEndOfMedia(() -> {
                mediaPlayer.stop();
                controller.onCompleted();
            });
            root.setOnMouseClicked(e -> {
                if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    mediaPlayer.pause();
                } else {
                    mediaPlayer.play();
                }
            });
            playButton.setOnAction(e -> mediaPlayer.play());
            pauseButton.setOnAction(e -> mediaPlayer.pause());
            stopButton.setOnAction(e -> mediaPlayer.stop());

            /*playButton.defaultButtonProperty().bind(mediaPlayer.statusProperty().isEqualTo(MediaPlayer.Status.PAUSED)
                    .or(mediaPlayer.statusProperty().isEqualTo(MediaPlayer.Status.STOPPED)));*/
            playButton.disableProperty().bind(mediaPlayer.statusProperty().isEqualTo(MediaPlayer.Status.PLAYING));
            pauseButton.disableProperty().bind(mediaPlayer.statusProperty().isEqualTo(MediaPlayer.Status.PAUSED)
                    .or(mediaPlayer.statusProperty().isEqualTo(MediaPlayer.Status.STOPPED)));
            stopButton.disableProperty().bind(mediaPlayer.statusProperty().isEqualTo(MediaPlayer.Status.STOPPED));
        } catch (Exception e) {
            // If OS does not support mp4 we get an exception
            log.error(root.toString());
            errorLabel.setText(ExceptionUtil.print(e));
        }

        closeButton.setOnAction(e -> controller.onClose());
    }

    @Override
    protected void onViewDetached() {
        if (mediaPlayer != null) {
            mediaPlayer.setOnEndOfMedia(null);
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;

            playButton.disableProperty().unbind();
            pauseButton.disableProperty().unbind();
            stopButton.disableProperty().unbind();

            playButton.setOnAction(null);
            pauseButton.setOnAction(null);
            stopButton.setOnAction(null);
            root.setOnMouseClicked(null);
        }
        errorLabel.setText(null);
        closeButton.setOnAction(null);
    }
}