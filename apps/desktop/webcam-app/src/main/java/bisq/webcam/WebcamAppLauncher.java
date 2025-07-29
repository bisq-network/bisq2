package bisq.webcam;

import javafx.application.Application;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebcamAppLauncher {
    public static void main(String[] args) {
        Thread.currentThread().setName("WebcamAppLauncher.main");
        new Thread(() -> {
            System.setProperty("javafx.sg.warn", "false");
            Thread.currentThread().setName("WebcamAppLauncher.JavaFXApplication.launch");
            Application.launch(WebcamApp.class, args); //blocks until app is closed
        }).start();
    }
}
