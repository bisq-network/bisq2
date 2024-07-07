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

package bisq.desktop.common.qr.webcam.app;


import bisq.desktop.common.qr.webcam.VideoSize;
import bisq.desktop.common.qr.webcam.WebcamService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebcamApp extends Application {
    private static final VideoSize VIDEO_SIZE = VideoSize.SD;

    private final WebcamService webcamService = new WebcamService();
    private final ImageView imageView = new ImageView();
    private final Label qrCodeLabel = new Label();

    @Override
    public void start(Stage primaryStage) {
        webcamService.setVideoSize(VIDEO_SIZE);
        VBox pane = new VBox(20, imageView, qrCodeLabel);
        pane.setPadding(new Insets(20));
        pane.setAlignment(Pos.CENTER);
        primaryStage.setScene(new Scene(pane, VIDEO_SIZE.getWidth(), 2 * VIDEO_SIZE.getHeight()));
        primaryStage.show();

        startWebcam();
    }

    private void startWebcam() {
        webcamService.getException().addObserver(exception -> {
            if (exception != null) {
                log.error(exception.toString());
            }
        });
        webcamService.getCapturedImage().addObserver(image -> {
            if (image != null) {
                Platform.runLater(() -> imageView.setImage(image));
            }
        });
        webcamService.getQrCode().addObserver(qrCode -> {
            if (qrCode != null) {
                Platform.runLater(() -> qrCodeLabel.setText("QR code: " + qrCode));
            }
        });
        webcamService.initialize();
    }
}

