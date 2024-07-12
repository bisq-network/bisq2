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

package bisq.webcam;


import bisq.common.logging.LogSetup;
import bisq.common.util.FileUtils;
import bisq.common.util.OsUtils;
import bisq.common.webcam.ControlSignals;
import bisq.i18n.Res;
import bisq.webcam.service.VideoSize;
import bisq.webcam.service.WebcamException;
import bisq.webcam.service.WebcamService;
import bisq.webcam.service.network.QrCodeSender;
import bisq.webcam.view.WebcamView;
import bisq.webcam.view.util.KeyHandlerUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FrameGrabber;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import static bisq.common.webcam.ControlSignals.*;
import static java.util.Objects.requireNonNull;

@Slf4j
public class WebcamApp extends Application {
    private static final VideoSize VIDEO_SIZE = VideoSize.SD;

    private final WebcamService webcamService;
    private QrCodeSender qrCodeSender;
    private WebcamView webcamView;
    private boolean imageDetected;

    public WebcamApp() {
        webcamService = new WebcamService();
        webcamService.setVideoSize(VIDEO_SIZE);
    }

    @Override
    public void init() {
        Parameters parameters = getParameters();
        try {
            int port = 8000;
            String portParam = parameters.getNamed().get("port");
            if (portParam != null) {
                port = Integer.parseInt(portParam);
            }

            String logFile = OsUtils.getUserDataDir().resolve("Bisq-webcam-app").toAbsolutePath() + FileUtils.FILE_SEP + "webcam-app";
            String logFileParam = parameters.getNamed().get("logFile");
            if (logFileParam != null) {
                logFile = URLDecoder.decode(logFileParam, StandardCharsets.UTF_8);
            }
            LogSetup.setup(logFile);
            log.info("Webcam app logging to {}", logFile);

            String language = "en";
            String languageParam = parameters.getNamed().get("language");
            if (languageParam != null) {
                language = URLDecoder.decode(languageParam, StandardCharsets.UTF_8);
            }
            Res.setLanguage(language);

            qrCodeSender = new QrCodeSender(port);
        } catch (Exception e) {
            log.error("init failed", e);
        }
    }
    @Override
    public void start(Stage primaryStage) {
        setupStage(primaryStage);
        startWebcam();
        // qrCodeSender.startSendingHeartBeat();
    }

    private void shutdown() {
        qrCodeSender.send(ControlSignals.SHUTDOWN)
                .whenComplete((nil, throwable) -> {
                    qrCodeSender.shutdown();
                    webcamService.shutdown()
                            .whenComplete((result, throwable1) -> Platform.exit());
                });
    }


    private void setupStage(Stage primaryStage) {
        webcamView = new WebcamView();
        Scene scene = new Scene(webcamView, VIDEO_SIZE.getWidth(), VIDEO_SIZE.getHeight());
        scene.getStylesheets().add(requireNonNull(this.getClass().getResource("/css/webapp.css")).toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.sizeToScene();
        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            shutdown();
        });
        primaryStage.show();

        scene.addEventHandler(KeyEvent.KEY_PRESSED,
                event -> KeyHandlerUtil.handleShutDownKeyEvent(event, this::shutdown));
    }

    private void handleError(Throwable throwable) {
        String errorMessage = throwable.getMessage();
        Platform.runLater(() -> {
            webcamView.setHeadline(Res.get("errorHeadline"));
            webcamView.setErrorMessage(errorMessage);
        });
    }

    private void startWebcam() {
        webcamService.getException().addObserver(exception -> {
            if (exception != null) {
                log.error(exception.toString());
                handleError(exception);
                String errorMessage = getErrorMessage(exception);
                int endIndex = Math.min(1000 - ERROR_MESSAGE_PREFIX.name().length(), errorMessage.length());
                errorMessage = errorMessage.substring(0, endIndex);
                // qrCodeSender.send(ERROR_MESSAGE_PREFIX, errorMessage);
            }
        });
        webcamService.getCapturedImage().addObserver(image -> {
            if (image != null) {
                if (!imageDetected) {
                    imageDetected = true;
                    qrCodeSender.send(IMAGE_RECOGNIZED)
                            .whenComplete((nil, throwable) -> handleError(throwable));
                }
                Platform.runLater(() -> webcamView.setWebcamImage(image));
            }
        });
        webcamService.getQrCode().addObserver(qrCode -> {
            if (qrCode != null) {
                qrCodeSender.send(QR_CODE_PREFIX, qrCode)
                        .whenComplete((nil, throwable) -> handleError(throwable));
                ;
            }
        });
        webcamService.initialize();
    }

    private static String getErrorMessage(Throwable throwable) {
        if (throwable instanceof WebcamException) {
          /*  CameraDevicceLookupException exception = (CameraDevicceLookupException) throwable;
            if (exception.getFrameGrabberException() !=null) {
                log.error("exception.getCause()==FrameGrabber.Exception {}", exception.getFrameGrabberException().toString());
                return exception.getFrameGrabberException().getMessage();
            }else{
                log.error("exception==CameraDevicceLookupException {}", throwable.toString());
                return throwable.getMessage();
            }*/
        } else if (throwable instanceof TimeoutException) {

        }

        if (throwable instanceof FrameGrabber.Exception) {
            log.error("exception==FrameGrabber.Exception {}", throwable.toString());
            return throwable.getMessage();
        }
        if (throwable.getCause() instanceof FrameGrabber.Exception) {
            log.error("exception.getCause()==FrameGrabber.Exception {}", throwable.getCause().toString());
            return throwable.getCause().getMessage();
        }

        return throwable.getMessage();
    }
}

