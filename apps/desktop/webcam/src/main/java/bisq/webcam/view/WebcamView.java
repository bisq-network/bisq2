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

import bisq.i18n.Res;
import bisq.webcam.service.VideoSize;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebcamView extends BorderPane {
    private static final VideoSize VIDEO_SIZE = VideoSize.SD;

    private final ImageView imageView;
    private final VBox waitingInfo;
    private final WaitingAnimation waitingAnimation;
    private final Label headline;
    @Getter
    private final TextArea errorMessageTextArea;
    private final ImageView errorIcon;

    public WebcamView() {
        int videoWidth = VIDEO_SIZE.getWidth();
        int videoHeight = VIDEO_SIZE.getHeight();

        this.setPrefWidth(videoWidth);
        this.setPrefHeight(videoHeight + 23 + 20);
        this.getStyleClass().add("background");

        waitingAnimation = new WaitingAnimation();
        waitingAnimation.setAlignment(Pos.CENTER);

        headline = new Label(Res.get("connecting"));
        headline.setAlignment(Pos.CENTER);
        headline.setTextAlignment(TextAlignment.CENTER);
        headline.getStyleClass().add("connection");

        errorIcon = new ImageView();
        errorIcon.setId("no-camera");
        errorIcon.setFitHeight(78);
        errorIcon.setFitWidth(78);
        errorIcon.setPreserveRatio(true);

        errorMessageTextArea = new TextArea();
        errorMessageTextArea.setEditable(false);
        errorMessageTextArea.setWrapText(true);
        errorMessageTextArea.getStyleClass().addAll("code-block");

        setupBindings();

        VBox.setMargin(waitingAnimation, new Insets(VIDEO_SIZE.getHeight() / 4d, 0, 0, 0));
        VBox.setMargin(errorIcon, new Insets(VIDEO_SIZE.getHeight() / 6d, 0, 0, 0));
        VBox.setMargin(errorMessageTextArea, new Insets(0, 40, 40, 40));
        waitingInfo = new VBox(waitingAnimation, errorIcon, headline, errorMessageTextArea);
        waitingInfo.setSpacing(20);
        waitingInfo.setAlignment(Pos.TOP_CENTER);

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

    public void setHeadline(String headline) {
        this.headline.setText(headline);
    }

    public void setErrorMessage(String errorMessage) {
        errorMessageTextArea.setText(errorMessage);
    }

    private void setupBindings() {
        errorMessageTextArea.visibleProperty().bind(errorMessageTextArea.textProperty().isEmpty().not());
        errorMessageTextArea.managedProperty().bind(errorMessageTextArea.visibleProperty());

        errorIcon.visibleProperty().bind(errorMessageTextArea.visibleProperty());
        errorIcon.managedProperty().bind(errorIcon.visibleProperty());

        waitingAnimation.visibleProperty().bind(errorIcon.visibleProperty().not());
        waitingAnimation.managedProperty().bind(waitingAnimation.visibleProperty());
    }

}