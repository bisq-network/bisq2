package bisq.webcam;

import bisq.common.threading.ThreadName;
import javafx.application.Application;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebcamAppLauncher {
    public static void main(String[] args) {
        ThreadName.set(WebcamAppLauncher.class, "main");
        new Thread(() -> {
            ThreadName.setName("WebcamAppLauncher.JavaFXApplication.launch");
            Application.launch(WebcamApp.class, args); //blocks until app is closed
        }).start();
    }
}
