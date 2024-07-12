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
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebcamView extends StackPane {
    private static final VideoSize VIDEO_SIZE = VideoSize.SD;

    private final ImageView imageView;
    private final VBox vBox;
    private final WaitingAnimation waitingAnimation;
    private final Label headline, failedConnections;
    @Getter
    private final TextArea errorMessageTextArea;
    private final ImageView errorIcon;
    private int deviceNumber = -1;

    public WebcamView() {
        int videoWidth = VIDEO_SIZE.getWidth();
        int videoHeight = VIDEO_SIZE.getHeight();

        this.setPrefWidth(videoWidth);
        this.setPrefHeight(videoHeight + 23 + 20);
        this.getStyleClass().add("background");
        this.setPrefWidth(videoWidth);
        this.setPrefHeight(videoHeight);

        waitingAnimation = new WaitingAnimation();
        waitingAnimation.setAlignment(Pos.CENTER);

        headline = new Label(Res.get("connecting", deviceNumber));
        headline.setAlignment(Pos.CENTER);
        headline.setTextAlignment(TextAlignment.CENTER);
        headline.setWrapText(true);
        headline.getStyleClass().add("connection");

        failedConnections = new Label();
        failedConnections.setAlignment(Pos.CENTER);
        failedConnections.setTextAlignment(TextAlignment.CENTER);
        failedConnections.setWrapText(true);
        failedConnections.getStyleClass().add("failedConnections");

        errorIcon = new ImageView();
        errorIcon.setId("no-camera");
        errorIcon.setFitHeight(78);
        errorIcon.setFitWidth(78);
        errorIcon.setPreserveRatio(true);

        errorMessageTextArea = new TextArea();
        errorMessageTextArea.setEditable(false);
        errorMessageTextArea.setWrapText(true);
        errorMessageTextArea.getStyleClass().addAll("code-block");
        errorMessageTextArea.setMaxHeight(150);

        setupBindings();

        Region topSpacer = new Region();
        Region bottomSpacer = new Region();

        VBox.setMargin(waitingAnimation, new Insets(-VIDEO_SIZE.getHeight() / 8d, 0, 0, 0));
        VBox.setMargin(errorIcon, new Insets(-VIDEO_SIZE.getHeight() / 12d, 0, 0, 0));
        VBox.setMargin(errorMessageTextArea, new Insets(10, 40, -20, 40));
        VBox.setMargin(failedConnections, new Insets(-10, 0, 0, 0));
        VBox.setVgrow(topSpacer, Priority.ALWAYS);
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);
        vBox = new VBox(topSpacer, waitingAnimation, errorIcon, headline, failedConnections, errorMessageTextArea, bottomSpacer);
        vBox.setSpacing(20);
        vBox.setAlignment(Pos.TOP_CENTER);

        imageView = new ImageView();
        imageView.setFitWidth(videoWidth);
        imageView.setFitHeight(videoHeight);
        // Mirror image
        imageView.setScaleX(-1);

        this.getChildren().addAll(imageView, vBox);
    }

    public void setWebcamImage(Image webcamImage) {
        if (webcamImage != null) {
            waitingAnimation.stop();
        }

        imageView.setImage(webcamImage);

        BooleanBinding isImageNull = imageView.imageProperty().isNull();
        BooleanBinding isImageNotNull = isImageNull.not();
        imageView.visibleProperty().bind(isImageNotNull);
        imageView.managedProperty().bind(imageView.visibleProperty());

        vBox.visibleProperty().bind(isImageNull.or(errorMessageTextArea.textProperty().isEmpty().not()));
        vBox.managedProperty().bind(vBox.visibleProperty());
    }

    public void applyErrorMessage(String errorHeadline, String errorMessage) {
        errorMessageTextArea.setText(errorMessage);
        this.headline.setText(errorHeadline);
        this.headline.getStyleClass().setAll("all-connections-failed");
        vBox.getStyleClass().add("overlay-background");
    }

    public void applyDeviceNumber(int deviceNumber) {
        if (errorMessageTextArea.getText().isEmpty()) {
            String text = Res.get("connecting", deviceNumber);
            if (this.deviceNumber > -1) {
                String previous = failedConnections.getText();
                if (previous.isEmpty()) {
                    failedConnections.setText(Res.get("connectingFailed", deviceNumber - 1));
                } else {
                    failedConnections.setText(previous + "\n" + Res.get("connectingFailed", deviceNumber - 1));
                }
            }
            headline.setText(text);
        }
        this.deviceNumber = deviceNumber;
    }

    private void setupBindings() {
        errorMessageTextArea.visibleProperty().bind(errorMessageTextArea.textProperty().isEmpty().not());
        errorMessageTextArea.managedProperty().bind(errorMessageTextArea.visibleProperty());

        failedConnections.visibleProperty().bind(failedConnections.textProperty().isEmpty().not());
        failedConnections.managedProperty().bind(failedConnections.visibleProperty());

        errorIcon.visibleProperty().bind(errorMessageTextArea.visibleProperty());
        errorIcon.managedProperty().bind(errorIcon.visibleProperty());

        waitingAnimation.visibleProperty().bind(errorIcon.visibleProperty().not());
        waitingAnimation.managedProperty().bind(waitingAnimation.visibleProperty());
    }
}