package bisq.network_lab;

import bisq.application.Executable;
import bisq.common.threading.ThreadName;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NetworkLabApp extends Executable<NetworkLabApplicationService> {
    public static void main(String[] args) {
        ThreadName.set(NetworkLabApp.class, "main");
        new NetworkLabApp(args);
    }

    public NetworkLabApp(String[] args) {
        super(args);
    }

    @Override
    protected NetworkLabApplicationService createApplicationService(String[] args) {
        return new NetworkLabApplicationService(args);
    }
}

