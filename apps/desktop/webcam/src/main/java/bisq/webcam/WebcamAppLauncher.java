package bisq.webcam;

import javafx.application.Application;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebcamAppLauncher {
    public static void main(String[] args) {
        new Thread(() -> {
            Thread.currentThread().setName("Java FX Application Launcher");
            Application.launch(WebcamApp.class, args); //blocks until app is closed
        }).start();


        //Application.launch(WebcamApp.class, args);
    }
}
