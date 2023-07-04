package bisq.desktop_app;

import bisq.desktop.JavaFxExecutable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DesktopApp {
    public static void main(String[] args) {
        new JavaFxExecutable(args);
    }
}
