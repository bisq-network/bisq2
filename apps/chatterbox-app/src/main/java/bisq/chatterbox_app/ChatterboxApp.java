package bisq.chatterbox_app;

import bisq.application.Executable;
import bisq.common.threading.ThreadName;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatterboxApp extends Executable<ChatterboxApplicationService> {
    public static void main(String[] args) {
        ThreadName.set(ChatterboxApp.class, "main");
        new ChatterboxApp(args);
    }

    public ChatterboxApp(String[] args) {
        super(args);
    }

    @Override
    protected ChatterboxApplicationService createApplicationService(String[] args) {
        return new ChatterboxApplicationService(args);
    }
}

