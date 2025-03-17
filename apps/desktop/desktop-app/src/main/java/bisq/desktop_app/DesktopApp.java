package bisq.desktop_app;

import bisq.common.threading.ThreadName;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DesktopApp {
    public static void main(String[] args) {
        ThreadName.set(DesktopApp.class, "main");
        new DesktopExecutable(args);
    }
}


