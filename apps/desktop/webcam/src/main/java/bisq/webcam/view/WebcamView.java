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

import bisq.webcam.service.VideoSize;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebcamView extends BorderPane {
    private static final VideoSize VIDEO_SIZE = VideoSize.SD;

    private final ImageView imageView;
    private final VBox waitingInfo;
    private final WaitingAnimation waitingAnimation;

    public WebcamView() {
        int videoWidth = VIDEO_SIZE.getWidth();
        int videoHeight = VIDEO_SIZE.getHeight();

        this.setPrefWidth(videoWidth);
        this.setPrefHeight(videoHeight + 23 + 20);
        this.getStyleClass().add("background");

        waitingAnimation = new WaitingAnimation();
        waitingInfo = createWaitingInfo(waitingAnimation, new Label("Connecting to camera..."));

        imageView = new ImageView();
        imageView.setFitWidth(videoWidth);
        imageView.setFitHeight(videoHeight);
        // Mirror image
        imageView.setScaleX(-1);

        StackPane.setAlignment(waitingInfo, Pos.TOP_CENTER);
        StackPane stackPane = new StackPane(waitingInfo, imageView);
        stackPane.setPrefWidth(videoWidth);
        stackPane.setPrefHeight(videoHeight);
        this.setBottom(stackPane);
    }

    public void setWebcamImage(Image webcamImage) {
        boolean isConnecting = webcamImage == null;
        waitingInfo.setVisible(isConnecting);
        waitingInfo.setManaged(isConnecting);
        imageView.setVisible(!isConnecting);
        imageView.setManaged(!isConnecting);
        if (!isConnecting) {
            imageView.setImage(webcamImage);
            waitingAnimation.stop();
        }
    }


    private VBox createWaitingInfo(WaitingAnimation animation, Label headline) {
        animation.setAlignment(Pos.CENTER);
        headline.setAlignment(Pos.CENTER);
        headline.setTextAlignment(TextAlignment.CENTER);
        headline.getStyleClass().add("connection");
        VBox.setMargin(animation, new Insets(VIDEO_SIZE.getHeight() / 4d, 0, 0, 0));
        VBox waitingInfo = new VBox(animation, headline);
        waitingInfo.setSpacing(20);
        waitingInfo.setAlignment(Pos.TOP_CENTER);
        return waitingInfo;
    }
}