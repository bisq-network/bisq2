package bisq.desktop.common.qr.webcam.app;

import javafx.application.Application;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebcamAppLauncher {
    public static void main(String[] args) {
        Application.launch(WebcamApp.class);
    }
}
