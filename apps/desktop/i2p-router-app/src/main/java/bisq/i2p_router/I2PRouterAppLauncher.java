package bisq.i2p_router;

import javafx.application.Application;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class I2PRouterAppLauncher {
    public static void main(String[] args) {
        Thread.currentThread().setName("I2PRouterAppLauncher.main");
        new Thread(() -> {
            System.setProperty("javafx.sg.warn", "false");
            Thread.currentThread().setName("I2PRouterAppLauncher.JavaFXApplication.launch");
            Application.launch(I2PRouterApp.class, args); //blocks until app is closed
        }).start();
    }
}
