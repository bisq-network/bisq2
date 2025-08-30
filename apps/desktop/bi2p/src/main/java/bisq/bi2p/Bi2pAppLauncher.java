package bisq.bi2p;

import javafx.application.Application;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Bi2pAppLauncher {
    public static void main(String[] args) {
        Thread.currentThread().setName("Bi2pAppLauncher.main");
        new Thread(() -> {
            System.setProperty("javafx.sg.warn", "false");
            Thread.currentThread().setName("Bi2pAppLauncher.JavaFXApplication.launch");
            Application.launch(Bi2pApp.class, args); //blocks until app is closed
        }).start();
    }
}
